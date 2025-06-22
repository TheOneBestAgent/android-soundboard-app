package com.soundboard.android.network

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
    
    // Reconnection management
    private var reconnectionAttempts = 0
    private var maxReconnectionAttempts = 10
    private var baseReconnectionDelay = 1000L // 1 second
    private var maxReconnectionDelay = 30000L // 30 seconds
    private var reconnectionJob: Job? = null
    private var healthCheckJob: Job? = null
    private var isManualDisconnect = false
    
    // Transport error management
    private var transportErrorCount = 0
    private var lastTransportError = 0L
    private var transportErrorResetTime = 60000L // Reset count after 1 minute
    
    companion object {
        private const val TAG = "SocketManager"
        private const val CONNECTION_TIMEOUT = 30000L // Increased to 30 seconds
        private const val HEALTH_CHECK_INTERVAL = 15000L // 15 seconds
        private const val LATENCY_THRESHOLD_MS = 1000L // Consider unhealthy if latency > 1 second
        private const val PING_INTERVAL = 20000L // 20 seconds
        private const val PING_TIMEOUT = 60000L // 60 seconds
    }
    
    fun connectViaUSB(port: Int = 8080) {
        // Connect via USB using ADB port forwarding - always use localhost
        Log.d(TAG, "🔌 Connecting via USB with improved connection management...")
        _connectionStatus.value = ConnectionStatus.Connecting
        isManualDisconnect = false
        
        // Add delay to allow reverse port forwarding to establish
        scope.launch {
            delay(2000) // Wait 2 seconds
            connect("localhost", port)
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
} 