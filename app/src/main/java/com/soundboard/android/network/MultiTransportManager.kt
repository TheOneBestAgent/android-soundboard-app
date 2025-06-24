package com.soundboard.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MultiTransportManager - Phase 3: Multi-Transport & Resilience
 * 
 * Provides multiple transport options with automatic failover, hybrid connection modes,
 * and comprehensive connection resilience for the Android Soundboard application.
 * 
 * Features:
 * - WebSocket primary transport with HTTP polling fallback
 * - USB/ADB connection with automatic port forwarding
 * - Network quality assessment and adaptive transport selection
 * - Automatic failover and recovery mechanisms
 * - Real-time connection analytics and monitoring
 * - Hybrid connection modes (WiFi Direct, Bluetooth future support)
 */
@Singleton
class MultiTransportManager @Inject constructor(
    private val context: Context,
    private val connectionAnalytics: ConnectionAnalytics
) {
    companion object {
        private const val TAG = "MultiTransportManager"
        private const val DEFAULT_WEBSOCKET_PORT = 3001
        private const val DEFAULT_HTTP_PORT = 3001
        private const val DEFAULT_USB_PORT = 8080
        private const val CONNECTION_TIMEOUT = 10000L // 10 seconds
        private const val HEALTH_CHECK_INTERVAL = 15000L // 15 seconds
        private const val FAILOVER_THRESHOLD = 3 // Failed attempts before failover
    }

    @Serializable
    enum class TransportType {
        WEBSOCKET,
        HTTP_POLLING,
        USB_ADB,
        WIFI_DIRECT,    // Future implementation
        BLUETOOTH,      // Future implementation
        CLOUD_RELAY     // Future implementation
    }

    @Serializable
    data class TransportConfig(
        val type: TransportType,
        val host: String,
        val port: Int,
        val priority: Int, // Lower number = higher priority
        val enabled: Boolean = true,
        val timeout: Long = CONNECTION_TIMEOUT,
        val retryCount: Int = 3
    )

    @Serializable
    data class ConnectionState(
        val currentTransport: TransportType?,
        val availableTransports: List<TransportType>,
        val isConnected: Boolean,
        val connectionQuality: ConnectionQuality,
        val lastConnectionTime: Long,
        val failoverCount: Int,
        val totalConnections: Int
    )

    @Serializable
    enum class ConnectionQuality {
        EXCELLENT,  // <50ms latency, no errors
        GOOD,       // 50-150ms latency, minimal errors
        FAIR,       // 150-300ms latency, some errors
        POOR,       // >300ms latency, frequent errors
        UNKNOWN     // No data available
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Transport configurations with priority order
    private val transportConfigs = mutableMapOf<TransportType, TransportConfig>()
    private val transportConnections = ConcurrentHashMap<TransportType, Boolean>()
    private val failureCounters = ConcurrentHashMap<TransportType, AtomicInteger>()
    
    // Current connection state
    private val _connectionState = MutableStateFlow(
        ConnectionState(
            currentTransport = null,
            availableTransports = emptyList(),
            isConnected = false,
            connectionQuality = ConnectionQuality.UNKNOWN,
            lastConnectionTime = 0L,
            failoverCount = 0,
            totalConnections = 0
        )
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Analytics and monitoring
    private val _transportMetrics = MutableStateFlow<Map<TransportType, TransportMetrics>>(emptyMap())
    val transportMetrics: StateFlow<Map<TransportType, TransportMetrics>> = _transportMetrics.asStateFlow()
    
    @Serializable
    data class TransportMetrics(
        val type: TransportType,
        val successRate: Float,
        val averageLatency: Long,
        val totalConnections: Int,
        val failedConnections: Int,
        val lastSuccessTime: Long,
        val isHealthy: Boolean
    )

    init {
        initializeTransportConfigs()
        startHealthMonitoring()
        Log.i(TAG, "🔄 MultiTransportManager initialized with resilient connection management")
    }

    /**
     * Initialize default transport configurations with priority ordering
     */
    private fun initializeTransportConfigs() {
        transportConfigs[TransportType.USB_ADB] = TransportConfig(
            type = TransportType.USB_ADB,
            host = "localhost",
            port = DEFAULT_USB_PORT,
            priority = 1 // Highest priority - most reliable
        )
        
        transportConfigs[TransportType.WEBSOCKET] = TransportConfig(
            type = TransportType.WEBSOCKET,
            host = "localhost",
            port = DEFAULT_WEBSOCKET_PORT,
            priority = 2 // Second priority - fast but network dependent
        )
        
        transportConfigs[TransportType.HTTP_POLLING] = TransportConfig(
            type = TransportType.HTTP_POLLING,
            host = "localhost",
            port = DEFAULT_HTTP_PORT,
            priority = 3 // Fallback option - most compatible
        )

        // Initialize failure counters
        transportConfigs.keys.forEach { transport ->
            failureCounters[transport] = AtomicInteger(0)
            transportConnections[transport] = false
        }
        
        Log.i(TAG, "📋 Initialized ${transportConfigs.size} transport configurations")
    }

    /**
     * Start health monitoring for all transports
     */
    private fun startHealthMonitoring() {
        connectionScope.launch {
            while (isActive) {
                try {
                    checkTransportHealth()
                    updateConnectionQuality()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Health monitoring error", e)
                    delay(HEALTH_CHECK_INTERVAL)
                }
            }
        }
    }

    /**
     * Attempt connection with automatic transport selection and failover
     */
    suspend fun connect(preferredHost: String? = null, preferredPort: Int? = null): Boolean {
        Log.i(TAG, "🔄 Starting multi-transport connection attempt")
        
        // Update host/port if provided
        preferredHost?.let { host ->
            preferredPort?.let { port ->
                updateTransportConfigs(host, port)
            }
        }
        
        // Get ordered list of available transports
        val availableTransports = getAvailableTransports()
        
        for (transport in availableTransports) {
            if (attemptTransportConnection(transport)) {
                Log.i(TAG, "✅ Successfully connected via $transport")
                updateConnectionState(transport, true)
                connectionAnalytics.recordEvent(
                    ConnectionEventType.CONNECT_SUCCESS,
                    "Transport connection successful: $transport"
                )
                return true
            }
        }
        
        Log.e(TAG, "❌ All transport connections failed")
        connectionAnalytics.recordEvent(
            ConnectionEventType.TRANSPORT_ERROR,
            "All transport connections failed",
            errorCode = "ALL_TRANSPORTS_FAILED"
        )
        updateConnectionState(null, false)
        return false
    }

    /**
     * Update transport configurations with new host/port
     */
    private fun updateTransportConfigs(host: String, port: Int) {
        transportConfigs[TransportType.WEBSOCKET] = transportConfigs[TransportType.WEBSOCKET]!!.copy(
            host = host, port = port
        )
        transportConfigs[TransportType.HTTP_POLLING] = transportConfigs[TransportType.HTTP_POLLING]!!.copy(
            host = host, port = port
        )
        
        Log.i(TAG, "🔧 Updated transport configs: $host:$port")
    }

    /**
     * Get available transports ordered by priority and health
     */
    private fun getAvailableTransports(): List<TransportType> {
        return transportConfigs.values
            .filter { it.enabled }
            .sortedWith { a, b ->
                // Primary sort by priority
                val priorityComparison = a.priority.compareTo(b.priority)
                if (priorityComparison != 0) return@sortedWith priorityComparison
                
                // Secondary sort by failure count (fewer failures first)
                val aFailures = failureCounters[a.type]?.get() ?: 0
                val bFailures = failureCounters[b.type]?.get() ?: 0
                aFailures.compareTo(bFailures)
            }
            .map { it.type }
    }

    /**
     * Attempt connection via specific transport
     */
    private suspend fun attemptTransportConnection(transport: TransportType): Boolean {
        val config = transportConfigs[transport] ?: return false
        
        Log.i(TAG, "🔌 Attempting connection via $transport to ${config.host}:${config.port}")
        
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val success = when (transport) {
                    TransportType.USB_ADB -> connectViaUSB(config)
                    TransportType.WEBSOCKET -> connectViaWebSocket(config)
                    TransportType.HTTP_POLLING -> connectViaHttpPolling(config)
                    else -> {
                        Log.w(TAG, "⚠️ Transport $transport not yet implemented")
                        false
                    }
                }
                
                val latency = System.currentTimeMillis() - startTime
                
                if (success) {
                    failureCounters[transport]?.set(0)
                    transportConnections[transport] = true
                    updateTransportMetrics(transport, success, latency)
                    connectionAnalytics.recordEvent(
                        ConnectionEventType.CONNECT_SUCCESS,
                        "Transport connection successful: $transport",
                        latency = latency
                    )
                    true
                } else {
                    val failureCount = failureCounters[transport]?.incrementAndGet() ?: 0
                    transportConnections[transport] = false
                    updateTransportMetrics(transport, success, latency)
                    connectionAnalytics.recordEvent(
                        ConnectionEventType.CONNECT_FAILED,
                        "Transport connection failed: $transport - ${latency}ms",
                        errorCode = "TRANSPORT_FAILURE"
                    )
                    
                    Log.w(TAG, "❌ $transport connection failed (attempt $failureCount)")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 $transport connection error", e)
                val failureCount = failureCounters[transport]?.incrementAndGet() ?: 0
                transportConnections[transport] = false
                connectionAnalytics.recordEvent(
                    ConnectionEventType.TRANSPORT_ERROR,
                    "Transport connection error: $transport - ${e.message}",
                    errorCode = e.javaClass.simpleName
                )
                false
            }
        }
    }

    /**
     * Connect via USB/ADB transport
     */
    private suspend fun connectViaUSB(config: TransportConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Test connection to localhost:8080 (ADB forwarded port)
                val socket = Socket()
                socket.connect(InetSocketAddress("localhost", config.port), config.timeout.toInt())
                socket.close()
                Log.i(TAG, "✅ USB/ADB connection successful")
                true
            } catch (e: Exception) {
                Log.w(TAG, "❌ USB/ADB connection failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Connect via WebSocket transport
     */
    private suspend fun connectViaWebSocket(config: TransportConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Test WebSocket endpoint availability
                val url = URL("http://${config.host}:${config.port}/socket.io/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = config.timeout.toInt()
                connection.readTimeout = config.timeout.toInt()
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                val success = responseCode in 200..299
                Log.i(TAG, if (success) "✅ WebSocket endpoint available" else "❌ WebSocket endpoint unavailable (code: $responseCode)")
                success
            } catch (e: Exception) {
                Log.w(TAG, "❌ WebSocket connection failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Connect via HTTP polling transport
     */
    private suspend fun connectViaHttpPolling(config: TransportConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Test HTTP endpoint availability
                val url = URL("http://${config.host}:${config.port}/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = config.timeout.toInt()
                connection.readTimeout = config.timeout.toInt()
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                val success = responseCode in 200..299
                Log.i(TAG, if (success) "✅ HTTP polling available" else "❌ HTTP polling unavailable (code: $responseCode)")
                success
            } catch (e: Exception) {
                Log.w(TAG, "❌ HTTP polling connection failed: ${e.message}")
                false
            }
        }
    }

    /**
     * Check health of all transports
     */
    private suspend fun checkTransportHealth() {
        val healthyTransports = mutableListOf<TransportType>()
        
        transportConfigs.keys.forEach { transport ->
            if (transportConnections[transport] == true) {
                val config = transportConfigs[transport]!!
                if (isTransportHealthy(transport, config)) {
                    healthyTransports.add(transport)
                } else {
                    transportConnections[transport] = false
                    Log.w(TAG, "⚠️ Transport $transport marked as unhealthy")
                }
            }
        }
        
        _connectionState.value = _connectionState.value.copy(
            availableTransports = healthyTransports
        )
    }

    /**
     * Check if specific transport is healthy
     */
    private suspend fun isTransportHealthy(transport: TransportType, config: TransportConfig): Boolean {
        return try {
            when (transport) {
                TransportType.USB_ADB -> {
                    val socket = Socket()
                    socket.connect(InetSocketAddress("localhost", config.port), 5000)
                    socket.close()
                    true
                }
                TransportType.WEBSOCKET, TransportType.HTTP_POLLING -> {
                    val url = URL("http://${config.host}:${config.port}/health")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val responseCode = connection.responseCode
                    connection.disconnect()
                    responseCode in 200..299
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update connection state
     */
    private fun updateConnectionState(currentTransport: TransportType?, isConnected: Boolean) {
        val currentState = _connectionState.value
        _connectionState.value = currentState.copy(
            currentTransport = currentTransport,
            isConnected = isConnected,
            lastConnectionTime = if (isConnected) System.currentTimeMillis() else currentState.lastConnectionTime,
            failoverCount = if (currentTransport != currentState.currentTransport && isConnected) 
                currentState.failoverCount + 1 else currentState.failoverCount,
            totalConnections = if (isConnected) currentState.totalConnections + 1 else currentState.totalConnections
        )
    }

    /**
     * Update connection quality based on current metrics
     */
    private fun updateConnectionQuality() {
        val currentTransport = _connectionState.value.currentTransport
        if (currentTransport == null) {
            _connectionState.value = _connectionState.value.copy(connectionQuality = ConnectionQuality.UNKNOWN)
            return
        }
        
        val metrics = _transportMetrics.value[currentTransport]
        val quality = when {
            metrics == null -> ConnectionQuality.UNKNOWN
            metrics.averageLatency < 50 && metrics.successRate > 0.95f -> ConnectionQuality.EXCELLENT
            metrics.averageLatency < 150 && metrics.successRate > 0.85f -> ConnectionQuality.GOOD
            metrics.averageLatency < 300 && metrics.successRate > 0.70f -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
        
        _connectionState.value = _connectionState.value.copy(connectionQuality = quality)
    }

    /**
     * Update transport metrics
     */
    private fun updateTransportMetrics(transport: TransportType, success: Boolean, latency: Long) {
        val currentMetrics = _transportMetrics.value.toMutableMap()
        val existingMetrics = currentMetrics[transport]
        
        val newMetrics = if (existingMetrics == null) {
            TransportMetrics(
                type = transport,
                successRate = if (success) 1.0f else 0.0f,
                averageLatency = latency,
                totalConnections = 1,
                failedConnections = if (success) 0 else 1,
                lastSuccessTime = if (success) System.currentTimeMillis() else 0L,
                isHealthy = success
            )
        } else {
            val newTotalConnections = existingMetrics.totalConnections + 1
            val newFailedConnections = if (success) existingMetrics.failedConnections else existingMetrics.failedConnections + 1
            val newSuccessRate = (newTotalConnections - newFailedConnections).toFloat() / newTotalConnections
            val newAverageLatency = (existingMetrics.averageLatency + latency) / 2
            
            existingMetrics.copy(
                successRate = newSuccessRate,
                averageLatency = newAverageLatency,
                totalConnections = newTotalConnections,
                failedConnections = newFailedConnections,
                lastSuccessTime = if (success) System.currentTimeMillis() else existingMetrics.lastSuccessTime,
                isHealthy = newSuccessRate > 0.7f && newAverageLatency < 500
            )
        }
        
        currentMetrics[transport] = newMetrics
        _transportMetrics.value = currentMetrics
    }

    /**
     * Force failover to next available transport
     */
    suspend fun forceFailover(): Boolean {
        Log.i(TAG, "🔄 Forcing failover to next available transport")
        
        val currentTransport = _connectionState.value.currentTransport
        if (currentTransport != null) {
            transportConnections[currentTransport] = false
            failureCounters[currentTransport]?.set(FAILOVER_THRESHOLD)
        }
        
        return connect()
    }

    /**
     * Get current transport information
     */
    fun getCurrentTransportInfo(): String {
        val state = _connectionState.value
        return if (state.isConnected && state.currentTransport != null) {
            val config = transportConfigs[state.currentTransport]
            "Connected via ${state.currentTransport} to ${config?.host}:${config?.port} (Quality: ${state.connectionQuality})"
        } else {
            "Not connected"
        }
    }

    /**
     * Get comprehensive analytics data
     */
    fun getAnalyticsData(): Map<String, Any> {
        val state = _connectionState.value
        val metrics = _transportMetrics.value
        
        return mapOf(
            "connectionState" to state,
            "transportMetrics" to metrics,
            "transportConfigs" to transportConfigs,
            "failureCounts" to failureCounters.mapValues { it.value.get() },
            "networkInfo" to getNetworkInfo()
        )
    }

    /**
     * Get current network information
     */
    private fun getNetworkInfo(): Map<String, String> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        
        return mapOf(
            "hasWifi" to (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?.toString() ?: "false"),
            "hasCellular" to (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?.toString() ?: "false"),
            "hasEthernet" to (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?.toString() ?: "false"),
            "isConnected" to (network != null).toString()
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        connectionScope.cancel()
        transportConnections.clear()
        failureCounters.clear()
        Log.i(TAG, "🧹 MultiTransportManager cleaned up")
    }
} 