package com.soundboard.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.soundboard.android.network.model.PlaySoundCommand
import com.soundboard.android.network.model.ServerResponse
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    data class Connected(val latencyMs: Long = 0) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

data class PlaySoundResponse(
    val status: String,
    val message: String,
    val buttonId: Int,
    val timestamp: String
)

data class ConnectionHealth(
    val isHealthy: Boolean,
    val latencyMs: Long,
    val consecutiveFailures: Int,
    val lastHealthCheck: Long
)

@Singleton
class SocketManager @Inject constructor() {
    
    private var socket: Socket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var analytics: ConnectionAnalytics? = null
    
    // Connection state management
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _playResponses = MutableStateFlow<PlaySoundResponse?>(null)
    val playResponses: StateFlow<PlaySoundResponse?> = _playResponses.asStateFlow()
    
    private val _serverInfo = MutableStateFlow<String?>(null)
    val serverInfo: StateFlow<String?> = _serverInfo.asStateFlow()
    
    // Connection health monitoring
    private val _connectionHealth = MutableStateFlow<ConnectionHealth?>(null)
    val connectionHealth: StateFlow<ConnectionHealth?> = _connectionHealth.asStateFlow()
    
    // Enhanced reconnection management for Phase 1
    private var reconnectionAttempts = 0
    private var maxReconnectionAttempts = 10
    private var baseReconnectionDelay = 1000L // 1 second
    private var maxReconnectionDelay = 30000L // 30 seconds
    private var reconnectionJob: Job? = null
    private var healthCheckJob: Job? = null
    private var isManualDisconnect = false
    
    // Phase 1: Smart reconnection strategy
    private var currentReconnectionStrategy = ReconnectionStrategy.EXPONENTIAL_BACKOFF
    private var disconnectionCause: String? = null
    private var lastConnectionQuality = ConnectionQuality.UNKNOWN
    
    enum class ReconnectionStrategy {
        IMMEDIATE_RETRY,
        EXPONENTIAL_BACKOFF,
        LINEAR_BACKOFF,
        ADAPTIVE_TIMING,
        TRANSPORT_SWITCH
    }
    
    enum class ConnectionQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }
    
    // Transport error management
    private var transportErrorCount = 0
    private var lastTransportError = 0L
    private var transportErrorResetTime = 60000L // Reset count after 1 minute
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityManager: ConnectivityManager? = null
    private var lastNetworkType: NetworkType = NetworkType.UNKNOWN
    private var connectionAttempts = 0
    private val maxConnectionAttempts = 5
    private var lastConnectionTime = 0L
    private val minReconnectInterval = 1000L // 1 second minimum between reconnects
    
    enum class NetworkType {
        WIFI, MOBILE, ETHERNET, UNKNOWN
    }
    
    companion object {
        private const val TAG = "SocketManager"
        private const val CONNECTION_TIMEOUT = 30000L // Increased to 30 seconds
        private const val HEALTH_CHECK_INTERVAL = 15000L // 15 seconds
        private const val LATENCY_THRESHOLD_MS = 1000L // Consider unhealthy if latency > 1 second
        private const val PING_INTERVAL = 20000L // 20 seconds
        private const val PING_TIMEOUT = 60000L // 60 seconds
    }
    
    fun connectViaUSB(context: Context, serverUrl: String, onResult: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Starting USB connection to: $serverUrl")
        
        // Initialize analytics
        analytics = ConnectionAnalytics(context)
        
        // Reset connection attempts for new connection
        connectionAttempts = 0
        
        // Setup network monitoring
        setupNetworkMonitoring(context)
        
        try {
            disconnect()
            
            // WEBSOCKET-ONLY configuration for stable USB connections
            val options = IO.Options().apply {
                // WEBSOCKET ONLY - no polling transport at all
                transports = arrayOf("websocket")
                
                // WebSocket-optimized timeouts matching server
                timeout = 10000 // 10 seconds connection timeout
                
                // WebSocket-specific settings (no upgrade needed)
                upgrade = false // Already WebSocket, no upgrade
                rememberUpgrade = false // Not applicable
                
                // Heartbeat matching server configuration
                // Note: pingInterval and pingTimeout are not available in Socket.io client options
                // These are handled by the Socket.io client automatically
                
                // Manual reconnection control
                reconnection = false // We handle reconnection manually
                
                // WebSocket connection settings
                forceNew = true // Force new connection for reliability
                multiplex = false // Disable multiplexing for stability
                
                // WebSocket-specific query parameters
                query = "platform=android&version=${android.os.Build.VERSION.SDK_INT}&transport=websocket&client=android-soundboard"
            }
            
            socket = IO.socket(serverUrl, options)
            
            setupEnhancedEventHandlers(onResult, context)
            
            // Connect with timing check
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastConnectionTime < minReconnectInterval) {
                // Wait before connecting to avoid rapid reconnects
                Handler(Looper.getMainLooper()).postDelayed({
                    performConnection()
                }, minReconnectInterval - (currentTime - lastConnectionTime))
            } else {
                performConnection()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up USB connection", e)
            onResult(false, "Connection setup failed: ${e.message}")
        }
    }
    
    private fun setupNetworkMonitoring(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available: $network")
                    val networkType = getCurrentNetworkType()
                    if (networkType != lastNetworkType) {
                        lastNetworkType = networkType
                        handleNetworkChange(networkType)
                    }
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost: $network")
                    handleNetworkLost()
                }
                
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    Log.d(TAG, "Network capabilities changed")
                    // Check if we switched from mobile to wifi or vice versa
                    val newNetworkType = getCurrentNetworkType()
                    if (newNetworkType != lastNetworkType) {
                        lastNetworkType = newNetworkType
                        handleNetworkChange(newNetworkType)
                    }
                }
            }
            
            connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
        }
    }
    
    private fun getCurrentNetworkType(): NetworkType {
        connectivityManager?.let { cm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork ?: return NetworkType.UNKNOWN
                val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkType.UNKNOWN
                
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                    else -> NetworkType.UNKNOWN
                }
            }
        }
        return NetworkType.UNKNOWN
    }
    
    private fun handleNetworkChange(networkType: NetworkType) {
        Log.d(TAG, "Network changed to: $networkType")
        
        // If we're connected and network changed, verify connection health
        if (isConnected() && networkType == NetworkType.ETHERNET) {
            // Ethernet connection is preferred for USB debugging
            Log.d(TAG, "Switched to ethernet, connection should be more stable")
            // Reset connection attempts since we have a better connection
            connectionAttempts = 0
        } else if (isConnected() && networkType != NetworkType.ETHERNET) {
            // We might have lost the USB connection
            Log.d(TAG, "Network changed away from ethernet, may affect USB connection")
            // Perform a quick health check
            performConnectionHealthCheck()
        }
    }
    
    private fun handleNetworkLost() {
        Log.d(TAG, "Network connection lost")
        if (isConnected()) {
            // Network lost while connected - prepare for reconnection
            connectionAttempts = 0 // Reset attempts for better reconnection
        }
    }
    
    private fun performConnectionHealthCheck() {
        socket?.let { s ->
            // Send a ping to verify connection health
            s.emit("ping", JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("type", "health_check")
            })
            
            // Set a timeout to detect if the connection is actually dead
            Handler(Looper.getMainLooper()).postDelayed({
                if (!s.connected()) {
                    Log.d(TAG, "Health check failed - connection appears dead")
                    // Trigger reconnection
                    Log.d(TAG, "Socket is closed, attempting reconnection")
                    s.connect()
                }
            }, 3000) // 3 second timeout for health check
        }
    }
    
    private fun performConnection() {
        lastConnectionTime = System.currentTimeMillis()
        socket?.connect()
    }

    private fun setupEnhancedEventHandlers(onResult: (Boolean, String?) -> Unit, context: Context) {
        socket?.apply {
            // Connection successful
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "✅ Socket connected successfully via USB")
                connectionAttempts = 0 // Reset on successful connection
                _connectionStatus.value = ConnectionStatus.Connected()
                
                // Send connection metadata for server-side optimization
                emit("client_info", JSONObject().apply {
                    put("platform", "android")
                    put("version", android.os.Build.VERSION.SDK_INT)
                    put("connection_type", "usb")
                    put("timestamp", System.currentTimeMillis())
                })
                
                Handler(Looper.getMainLooper()).post {
                    onResult(true, "Connected successfully via USB")
                }
            }
            
            // Connection failed
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                connectionAttempts++
                val error = if (args.isNotEmpty()) args[0].toString() else "Unknown error"
                Log.e(TAG, "❌ Socket connection error (attempt $connectionAttempts/$maxConnectionAttempts): $error")
                
                _connectionStatus.value = ConnectionStatus.Error(error)
                
                // Enhanced error handling based on error type
                val errorMessage = when {
                    error.contains("timeout") -> "Connection timeout - check if server is running"
                    error.contains("ECONNREFUSED") -> "Connection refused - server may not be accessible"
                    error.contains("Network is unreachable") -> "Network unreachable - check USB debugging connection"
                    connectionAttempts >= maxConnectionAttempts -> "Connection failed after $maxConnectionAttempts attempts"
                    else -> "Connection error: $error"
                }
                
                if (connectionAttempts >= maxConnectionAttempts) {
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, errorMessage)
                    }
                }
            }
            
            // Disconnection handling
            on(Socket.EVENT_DISCONNECT) { args ->
                val reason = if (args.isNotEmpty()) args[0].toString() else "Unknown reason"
                Log.w(TAG, "⚠️ Socket disconnected: $reason")
                _connectionStatus.value = ConnectionStatus.Disconnected
                
                // Handle different disconnect reasons
                when (reason) {
                    "io server disconnect" -> {
                        Log.d(TAG, "Server initiated disconnect")
                        // Don't auto-reconnect on server disconnect
                    }
                    "io client disconnect" -> {
                        Log.d(TAG, "Client initiated disconnect")
                        // Normal disconnect, don't auto-reconnect
                    }
                    "ping timeout" -> {
                        Log.d(TAG, "Ping timeout - connection health issue")
                        // Reset attempts for better reconnection chance
                        connectionAttempts = kotlin.math.max(0, connectionAttempts - 1)
                    }
                    "transport close" -> {
                        Log.d(TAG, "Transport closed - likely network issue")
                        // This often happens with USB connection issues
                        performConnectionHealthCheck()
                    }
                    "transport error" -> {
                        Log.d(TAG, "Transport error - checking network state")
                        // Check if USB connection is still available
                        val networkType = getCurrentNetworkType()
                        if (networkType != NetworkType.ETHERNET) {
                            Log.w(TAG, "Transport error and no ethernet - USB connection may be lost")
                        }
                    }
                }
            }
            
            // These events are not available in Socket.io client 2.0.1
            // Reconnection is handled manually in our implementation
            
            // Enhanced ping/pong handling
            on("pong") { args ->
                Log.d(TAG, "📡 Received pong from server")
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val serverTime = data.optLong("timestamp", 0)
                        val roundTripTime = System.currentTimeMillis() - serverTime
                        Log.d(TAG, "Connection latency: ${roundTripTime}ms")
                        
                        // Update connection quality based on latency
                        _connectionStatus.value = if (roundTripTime < 100) {
                            ConnectionStatus.Connected() // Good connection
                        } else {
                            ConnectionStatus.Connected(roundTripTime) // Still connected but with higher latency
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Simple pong received")
                    }
                }
            }
            
            // Phase 1: Enhanced server communication
            on("health_prediction") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val stability = data.optDouble("stability", 1.0)
                        val quality = data.optString("quality", "unknown")
                        
                        Log.d(TAG, "🔮 Server health prediction: stability=$stability, quality=$quality")
                        
                        // Update connection quality based on server prediction
                        lastConnectionQuality = when (quality) {
                            "excellent" -> ConnectionQuality.EXCELLENT
                            "good" -> ConnectionQuality.GOOD
                            "fair" -> ConnectionQuality.FAIR
                            "poor" -> ConnectionQuality.POOR
                            else -> ConnectionQuality.UNKNOWN
                        }
                        
                        // Adjust reconnection strategy based on prediction
                        adjustReconnectionStrategy(stability, quality)
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing health prediction: ${e.message}")
                    }
                }
            }
            
            on("reconnection_guidance") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val strategy = data.optString("strategy", "exponential_backoff")
                        val estimatedDelay = data.optLong("estimatedDelay", baseReconnectionDelay)
                        val maxAttempts = data.optInt("maxAttempts", maxReconnectionAttempts)
                        
                        Log.d(TAG, "💡 Server reconnection guidance: strategy=$strategy, delay=$estimatedDelay, maxAttempts=$maxAttempts")
                        
                        // Apply server recommendations
                        currentReconnectionStrategy = when (strategy) {
                            "immediate_retry" -> ReconnectionStrategy.IMMEDIATE_RETRY
                            "linear_backoff" -> ReconnectionStrategy.LINEAR_BACKOFF
                            "adaptive_timing" -> ReconnectionStrategy.ADAPTIVE_TIMING
                            "transport_switch" -> ReconnectionStrategy.TRANSPORT_SWITCH
                            else -> ReconnectionStrategy.EXPONENTIAL_BACKOFF
                        }
                        
                        baseReconnectionDelay = estimatedDelay
                        maxReconnectionAttempts = maxAttempts
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing reconnection guidance: ${e.message}")
                    }
                }
            }
        }
    }
    
    fun connect(ipAddress: String, port: Int) {
        scope.launch {
            try {
                Log.d(TAG, "🚀 Attempting to connect to $ipAddress:$port")
                _connectionStatus.value = ConnectionStatus.Connecting
                
                disconnect() // Disconnect any existing connection
                
                // First test basic HTTP connectivity
                val httpSuccess = testHttpConnectionSuspend(ipAddress, port)
                if (httpSuccess) {
                    Log.d(TAG, "✅ HTTP connection test successful, proceeding with Socket.IO")
                    connectSocketIO(ipAddress, port)
                    startHealthMonitoring()
                } else {
                    Log.e(TAG, "❌ HTTP connection test failed")
                    _connectionStatus.value = ConnectionStatus.Error("Cannot reach server - check USB connection")
                    scheduleReconnection(ipAddress, port)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 Connection error", e)
                _connectionStatus.value = ConnectionStatus.Error("Connection failed: ${e.message}")
                scheduleReconnection(ipAddress, port)
            }
        }
    }
    
    private suspend fun testHttpConnectionSuspend(ipAddress: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Testing HTTP connection to: http://$ipAddress:$port/health")
                val startTime = System.currentTimeMillis()
                
                val url = java.net.URL("http://$ipAddress:$port/health")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                val latency = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "📊 HTTP test response: $responseCode (${latency}ms)")
                
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d(TAG, "📝 HTTP test response body: $response")
                    
                    // Update connection health
                    _connectionHealth.value = ConnectionHealth(
                        isHealthy = latency < LATENCY_THRESHOLD_MS,
                        latencyMs = latency,
                        consecutiveFailures = 0,
                        lastHealthCheck = System.currentTimeMillis()
                    )
                }
                
                responseCode == 200
            } catch (e: Exception) {
                Log.e(TAG, "❌ HTTP connection test failed: ${e.javaClass.simpleName} - ${e.message}")
                
                // Update connection health
                val currentHealth = _connectionHealth.value
                _connectionHealth.value = ConnectionHealth(
                    isHealthy = false,
                    latencyMs = -1,
                    consecutiveFailures = (currentHealth?.consecutiveFailures ?: 0) + 1,
                    lastHealthCheck = System.currentTimeMillis()
                )
                
                false
            }
        }
    }
    
    private fun connectSocketIO(ipAddress: String, port: Int) {
        try {
            val serverUrl = "http://$ipAddress:$port"
            val options = IO.Options().apply {
                timeout = CONNECTION_TIMEOUT
                forceNew = true
                reconnection = false // We'll handle reconnection manually
                
                // Enhanced transport configuration for better stability
                if (ipAddress == "localhost" || ipAddress == "127.0.0.1") {
                    // For localhost/USB connections, prefer websocket with polling fallback
                    transports = arrayOf("websocket", "polling")
                } else {
                    // For network connections, use both transports for better reliability
                    transports = arrayOf("polling", "websocket")
                }
                
                secure = false
                
                // Enhanced stability and timeout options
                randomizationFactor = 0.5
                timeout = CONNECTION_TIMEOUT
                
                // Additional transport stability options
                multiplex = false // Disable multiplexing for better stability
            }
            
            socket = IO.socket(serverUrl, options)
            
            setupSocketListeners(ipAddress, port)
            
            // Connect manually with better error handling
            try {
                socket?.connect()
                Log.d(TAG, "🔌 Socket.io connection initiated to $serverUrl")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initiate socket connection", e)
                throw e
            }
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "❌ Invalid server URL", e)
            _connectionStatus.value = ConnectionStatus.Error("Invalid server address")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Socket.IO connection error", e)
            _connectionStatus.value = ConnectionStatus.Error("Socket.IO failed: ${e.message}")
        }
    }
    
    private fun setupSocketListeners(ipAddress: String, port: Int) {
        socket?.apply {
            
            // Connection established
            on(Socket.EVENT_CONNECT, Emitter.Listener {
                Log.d(TAG, "✅ Connected to server")
                reconnectionAttempts = 0 // Reset reconnection attempts on successful connection
                _connectionStatus.value = ConnectionStatus.Connected()
                
                // Send authentication
                emit("authenticate", gson.toJson(mapOf(
                    "client_type" to "android",
                    "version" to "1.0.0",
                    "timestamp" to System.currentTimeMillis()
                )))
            })
            
            // Authentication response
            on("authenticated", Emitter.Listener { args ->
                try {
                    if (args.isNotEmpty()) {
                        val response = args[0].toString()
                        Log.d(TAG, "🔐 Authentication response: $response")
                        
                        val serverResponse = gson.fromJson(response, ServerResponse::class.java)
                        if (serverResponse.status == "success") {
                            _serverInfo.value = "Connected to ${serverResponse.message ?: "server"}"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing authentication response", e)
                }
            })
            
            // Play sound response
            on("play_response", Emitter.Listener { args ->
                try {
                    if (args.isNotEmpty()) {
                        val response = args[0].toString()
                        Log.d(TAG, "🎵 Play response: $response")
                        
                        val playResponse = gson.fromJson(response, PlaySoundResponse::class.java)
                        _playResponses.value = playResponse
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing play response", e)
                }
            })
            
            // Connection error
            on(Socket.EVENT_CONNECT_ERROR, Emitter.Listener { args ->
                val error = if (args.isNotEmpty()) {
                    val errorObj = args[0]
                    when (errorObj) {
                        is Exception -> {
                            Log.e(TAG, "💥 Connection exception: ${errorObj.message}", errorObj)
                            "Connection failed: ${errorObj.message}"
                        }
                        else -> {
                            val errorStr = errorObj.toString()
                            Log.e(TAG, "❌ Connection error: $errorStr")
                            if (errorStr.contains("xhr poll error")) {
                                "Network error - check USB connection and server"
                            } else {
                                "Connection failed: $errorStr"
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "❓ Unknown connection error")
                    "Unknown connection error"
                }
                _connectionStatus.value = ConnectionStatus.Error(error)
                
                // Schedule reconnection if not manually disconnected
                if (!isManualDisconnect) {
                    scheduleReconnection(ipAddress, port)
                }
            })
            
            // Handle pong response for latency calculation
            on("pong", Emitter.Listener { args ->
                try {
                    if (args.isNotEmpty()) {
                        val pongData = args[0].toString()
                        val pongResponse = gson.fromJson(pongData, Map::class.java)
                        val clientTimestamp = (pongResponse["clientTimestamp"] as? Double)?.toLong() ?: 0
                        val latency = System.currentTimeMillis() - clientTimestamp
                        
                        Log.d(TAG, "📡 Ping response received - latency: ${latency}ms")
                        
                        // Update connection status with latency
                        _connectionStatus.value = ConnectionStatus.Connected(latency)
                        
                        // Update connection health
                        _connectionHealth.value = ConnectionHealth(
                            isHealthy = latency < LATENCY_THRESHOLD_MS,
                            latencyMs = latency,
                            consecutiveFailures = 0,
                            lastHealthCheck = System.currentTimeMillis()
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing pong response", e)
                }
            })

            // Disconnection
            on(Socket.EVENT_DISCONNECT, Emitter.Listener { args ->
                val reason = if (args.isNotEmpty()) args[0].toString() else "Unknown reason"
                Log.d(TAG, "🔌 Disconnected: $reason")
                _connectionStatus.value = ConnectionStatus.Disconnected
                
                // Handle different disconnect reasons with improved strategy
                when (reason) {
                    "transport error" -> {
                        Log.w(TAG, "⚠️ Transport error detected - implementing exponential backoff")
                        // For transport errors, use exponential backoff to prevent connection spam
                        if (!isManualDisconnect) {
                            scope.launch {
                                val backoffDelay = calculateTransportErrorBackoff()
                                Log.d(TAG, "🔄 Waiting ${backoffDelay}ms before reconnecting after transport error")
                                delay(backoffDelay)
                                if (!isManualDisconnect) {
                                    scheduleReconnection(ipAddress, port)
                                }
                            }
                        }
                    }
                    "client namespace disconnect" -> {
                        Log.d(TAG, "ℹ️ Client initiated disconnect")
                        // Don't reconnect for client-initiated disconnects
                    }
                    "ping timeout" -> {
                        Log.w(TAG, "⏰ Ping timeout - connection may be unstable")
                        if (!isManualDisconnect) {
                            scheduleReconnection(ipAddress, port)
                        }
                    }
                    "io server disconnect" -> {
                        Log.w(TAG, "🔌 Server initiated disconnect")
                        if (!isManualDisconnect) {
                            // Wait a bit longer for server-initiated disconnects
                            scope.launch {
                                delay(3000)
                                if (!isManualDisconnect) {
                                    scheduleReconnection(ipAddress, port)
                                }
                            }
                        }
                    }
                    else -> {
                        Log.d(TAG, "ℹ️ Disconnect reason: $reason")
                        // Schedule reconnection for other reasons if not manual
                        if (!isManualDisconnect) {
                            scheduleReconnection(ipAddress, port)
                        }
                    }
                }
            })
            
            // Add transport error handling
            on("transport error", Emitter.Listener { args ->
                val error = if (args.isNotEmpty()) args[0].toString() else "Unknown transport error"
                Log.w(TAG, "🚨 Transport error: $error")
                // Don't immediately reconnect on transport errors - let the disconnect handler manage it
            })
            
            // Add connect timeout handling
            on("connect_timeout", Emitter.Listener {
                Log.w(TAG, "⏰ Connection timeout")
                _connectionStatus.value = ConnectionStatus.Error("Connection timeout")
                if (!isManualDisconnect) {
                    scheduleReconnection(ipAddress, port)
                }
            })
        }
    }
    
    private fun scheduleReconnection(ipAddress: String, port: Int) {
        if (isManualDisconnect || reconnectionAttempts >= maxReconnectionAttempts) {
            if (reconnectionAttempts >= maxReconnectionAttempts) {
                Log.w(TAG, "⚠️ Max reconnection attempts reached ($maxReconnectionAttempts)")
                _connectionStatus.value = ConnectionStatus.Error("Max reconnection attempts reached")
            }
            return
        }
        
        // Cancel any existing reconnection job
        reconnectionJob?.cancel()
        
        reconnectionAttempts++
        
        // Calculate exponential backoff delay with jitter
        val baseDelay = min(
            baseReconnectionDelay * (2.0.pow(reconnectionAttempts - 1)).toLong(),
            maxReconnectionDelay
        )
        val jitterDelay = baseDelay + (Math.random() * 1000).toLong() // Add up to 1 second jitter
        
        Log.d(TAG, "⏰ Scheduling reconnection attempt $reconnectionAttempts in ${jitterDelay}ms")
        
        reconnectionJob = scope.launch {
            delay(jitterDelay)
            if (!isManualDisconnect) {
                Log.d(TAG, "🔄 Reconnection attempt $reconnectionAttempts")
                _connectionStatus.value = ConnectionStatus.Connecting
                connect(ipAddress, port)
            }
        }
    }
    
    private fun startHealthMonitoring() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive && _connectionStatus.value is ConnectionStatus.Connected) {
                delay(HEALTH_CHECK_INTERVAL)
                
                // Perform a simple ping to check connection health
                val startTime = System.currentTimeMillis()
                socket?.emit("ping", System.currentTimeMillis())
                
                // Wait for pong response (implemented via server response time)
                delay(1000) // Give server time to respond
                
                val currentHealth = _connectionHealth.value
                val timeSinceLastCheck = System.currentTimeMillis() - (currentHealth?.lastHealthCheck ?: 0)
                
                if (timeSinceLastCheck > HEALTH_CHECK_INTERVAL * 2) {
                    Log.w(TAG, "⚠️ Connection health check indicates possible issues")
                    _connectionHealth.value = currentHealth?.copy(
                        isHealthy = false,
                        consecutiveFailures = (currentHealth.consecutiveFailures) + 1
                    ) ?: ConnectionHealth(false, -1, 1, System.currentTimeMillis())
                }
            }
        }
    }
    
    fun playSound(filePath: String, volume: Float = 1.0f, buttonId: Int) {
        val currentStatus = _connectionStatus.value
        if (currentStatus !is ConnectionStatus.Connected) {
            Log.w(TAG, "⚠️ Cannot play sound - not connected (status: $currentStatus)")
            return
        }
        
        try {
            val command = PlaySoundCommand(
                filePath = filePath,
                volume = volume,
                buttonId = buttonId
            )
            
            val commandJson = gson.toJson(command)
            Log.d(TAG, "🎵 Sending play command: $commandJson")
            
            socket?.emit("play_sound", commandJson)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending play command", e)
        }
    }
    
    fun disconnect() {
        try {
            Log.d(TAG, "🔌 Manually disconnecting from server")
            isManualDisconnect = true
            
            // Cancel reconnection and health monitoring
            reconnectionJob?.cancel()
            healthCheckJob?.cancel()
            reconnectionAttempts = 0
            
            socket?.disconnect()
            socket?.off() // Remove all listeners
            socket = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            _serverInfo.value = null
            _connectionHealth.value = null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during disconnect", e)
        }
    }
    
    fun isConnected(): Boolean {
        return _connectionStatus.value is ConnectionStatus.Connected
    }
    
    fun clearPlayResponse() {
        _playResponses.value = null
    }
    
    fun getConnectionLatency(): Long {
        return (_connectionStatus.value as? ConnectionStatus.Connected)?.latencyMs ?: -1
    }
    
    fun resetReconnectionAttempts() {
        reconnectionAttempts = 0
        isManualDisconnect = false
    }
    
    private fun calculateTransportErrorBackoff(): Long {
        val currentTime = System.currentTimeMillis()
        
        // Reset transport error count if enough time has passed
        if (currentTime - lastTransportError > transportErrorResetTime) {
            transportErrorCount = 0
        }
        
        transportErrorCount++
        lastTransportError = currentTime
        
        // Calculate exponential backoff for transport errors
        // Start at 5 seconds, double each time, max 30 seconds
        val backoffDelay = min(
            5000L * (2.0.pow(transportErrorCount - 1)).toLong(),
            30000L
        )
        
        Log.d(TAG, "🔄 Transport error #$transportErrorCount, backoff: ${backoffDelay}ms")
        return backoffDelay
    }

    fun cleanup() {
        // Cleanup network monitoring
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        disconnect()
    }
    
    // Phase 1: Enhanced Reconnection Strategy Methods
    private fun adjustReconnectionStrategy(stability: Double, quality: String) {
        when {
            stability < 0.3 -> {
                currentReconnectionStrategy = ReconnectionStrategy.IMMEDIATE_RETRY
                maxReconnectionAttempts = 3
                Log.d(TAG, "🚨 Low stability detected - switching to immediate retry strategy")
            }
            stability < 0.6 -> {
                currentReconnectionStrategy = ReconnectionStrategy.EXPONENTIAL_BACKOFF
                maxReconnectionAttempts = 8
                Log.d(TAG, "⚠️ Moderate stability - using exponential backoff")
            }
            stability < 0.8 -> {
                currentReconnectionStrategy = ReconnectionStrategy.LINEAR_BACKOFF
                maxReconnectionAttempts = 6
                Log.d(TAG, "📊 Fair stability - using linear backoff")
            }
            else -> {
                currentReconnectionStrategy = ReconnectionStrategy.ADAPTIVE_TIMING
                maxReconnectionAttempts = 10
                Log.d(TAG, "✅ Good stability - using adaptive timing")
            }
        }
        
        // Adjust delays based on quality
        baseReconnectionDelay = when (quality) {
            "excellent" -> 500L
            "good" -> 1000L
            "fair" -> 2000L
            "poor" -> 5000L
            else -> 1000L
        }
    }
    
    private fun calculateIntelligentReconnectionDelay(attempt: Int): Long {
        return when (currentReconnectionStrategy) {
            ReconnectionStrategy.IMMEDIATE_RETRY -> {
                if (attempt <= 3) 100L else baseReconnectionDelay
            }
            ReconnectionStrategy.EXPONENTIAL_BACKOFF -> {
                min(
                    baseReconnectionDelay * (2.0.pow(attempt - 1)).toLong(),
                    maxReconnectionDelay
                )
            }
            ReconnectionStrategy.LINEAR_BACKOFF -> {
                baseReconnectionDelay * attempt
            }
            ReconnectionStrategy.ADAPTIVE_TIMING -> {
                // Adapt based on connection quality and history
                val qualityMultiplier = when (lastConnectionQuality) {
                    ConnectionQuality.EXCELLENT -> 0.5
                    ConnectionQuality.GOOD -> 0.8
                    ConnectionQuality.FAIR -> 1.2
                    ConnectionQuality.POOR -> 2.0
                    ConnectionQuality.UNKNOWN -> 1.0
                }
                (baseReconnectionDelay * qualityMultiplier * attempt).toLong()
            }
            ReconnectionStrategy.TRANSPORT_SWITCH -> {
                // Quick retry with potential transport changes
                baseReconnectionDelay / 2
            }
        }
    }
    
    private fun analyzeDisconnectionAndAdjustStrategy(reason: String) {
        disconnectionCause = reason
        
        analytics?.recordDisconnection(reason)
        
        when {
            reason.contains("timeout") -> {
                currentReconnectionStrategy = ReconnectionStrategy.EXPONENTIAL_BACKOFF
                baseReconnectionDelay = 2000L
                Log.d(TAG, "📡 Timeout detected - using longer delays")
            }
            reason.contains("transport") -> {
                currentReconnectionStrategy = ReconnectionStrategy.TRANSPORT_SWITCH
                baseReconnectionDelay = 1000L
                Log.d(TAG, "🚀 Transport issue - will try different approach")
            }
            reason.contains("server") -> {
                currentReconnectionStrategy = ReconnectionStrategy.LINEAR_BACKOFF
                baseReconnectionDelay = 3000L
                maxReconnectionAttempts = 5
                Log.d(TAG, "🖥️ Server issue - using conservative approach")
            }
            else -> {
                currentReconnectionStrategy = ReconnectionStrategy.ADAPTIVE_TIMING
                Log.d(TAG, "🎯 Unknown cause - using adaptive strategy")
            }
        }
    }
    
    fun getConnectionQuality(): String {
        return when (lastConnectionQuality) {
            ConnectionQuality.EXCELLENT -> "excellent"
            ConnectionQuality.GOOD -> "good"
            ConnectionQuality.FAIR -> "fair"
            ConnectionQuality.POOR -> "poor"
            ConnectionQuality.UNKNOWN -> "unknown"
        }
    }
    
    fun getReconnectionStats(): Map<String, Any> {
        return mapOf(
            "attempts" to reconnectionAttempts,
            "maxAttempts" to maxReconnectionAttempts,
            "strategy" to currentReconnectionStrategy.name,
            "baseDelay" to baseReconnectionDelay,
            "lastCause" to (disconnectionCause ?: "unknown"),
            "quality" to getConnectionQuality()
        )
    }
} 