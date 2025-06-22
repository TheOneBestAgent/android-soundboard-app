package com.soundboard.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.*
import java.util.concurrent.ConcurrentHashMap

/**
 * NetworkDiscoveryService - Phase 2: Discovery & Automation
 * 
 * Provides automatic server discovery via network scanning, QR code connection parsing,
 * and network topology analysis for seamless connection setup.
 */
class NetworkDiscoveryService(
    private val context: Context
) {
    companion object {
        private const val TAG = "NetworkDiscoveryService"
        private const val DISCOVERY_TIMEOUT = 30000L // 30 seconds
        private const val SCAN_INTERVAL = 10000L // 10 seconds
        private const val DEFAULT_PORT = 8080
    }

    @Serializable
    data class DiscoveredServer(
        val name: String,
        val address: String,
        val port: Int,
        val hostname: String? = null,
        val version: String? = null,
        val platform: String? = null,
        val capabilities: Map<String, String> = emptyMap(),
        val token: String? = null,
        val discoveredAt: Long = System.currentTimeMillis(),
        val isLocal: Boolean = true,
        val quality: NetworkQuality = NetworkQuality.UNKNOWN,
        val lastSeen: Long = System.currentTimeMillis()
    )

    @Serializable
    data class QRConnectionData(
        val type: String,
        val version: String,
        val server: ServerInfo,
        val connection: ConnectionInfo,
        val capabilities: Map<String, String>,
        val timestamp: String,
        val expires: String
    )

    @Serializable
    data class ServerInfo(
        val name: String,
        val address: String,
        val port: Int,
        val hostname: String
    )

    @Serializable
    data class ConnectionInfo(
        val token: String,
        val methods: List<String>,
        val endpoints: Map<String, String>
    )

    @Serializable
    data class NetworkTopology(
        val localAddress: String?,
        val networkName: String?,
        val connectionType: ConnectionType,
        val isMetered: Boolean,
        val signalStrength: Int = -1,
        val linkSpeed: Int = -1,
        val frequency: Int = -1,
        val capabilities: List<String> = emptyList()
    )

    enum class ConnectionType {
        WIFI, CELLULAR, ETHERNET, VPN, UNKNOWN
    }

    enum class NetworkQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }

    enum class DiscoveryState {
        IDLE, DISCOVERING, DISCOVERED, ERROR
    }

    // Service state
    private var discoveryJob: Job? = null
    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Discovered servers
    private val _discoveredServers = MutableStateFlow<Map<String, DiscoveredServer>>(emptyMap())
    val discoveredServers: StateFlow<Map<String, DiscoveredServer>> = _discoveredServers.asStateFlow()
    
    private val _discoveryState = MutableStateFlow(DiscoveryState.IDLE)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()
    
    private val _networkTopology = MutableStateFlow<NetworkTopology?>(null)
    val networkTopology: StateFlow<NetworkTopology?> = _networkTopology.asStateFlow()
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    // Internal state
    private val serversMap = ConcurrentHashMap<String, DiscoveredServer>()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Start network discovery service
     */
    fun startDiscovery() {
        if (discoveryJob?.isActive == true) {
            Log.d(TAG, "Discovery already active")
            return
        }

        Log.d(TAG, "🔍 Starting network discovery service...")
        _discoveryState.value = DiscoveryState.DISCOVERING

        discoveryJob = discoveryScope.launch {
            try {
                // Update network topology
                updateNetworkTopology()
                
                // Start network scanning
                startNetworkScanning()
                
                // Start periodic server quality updates
                startQualityMonitoring()
                
                Log.d(TAG, "✅ Network discovery service started")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start discovery service", e)
                _lastError.value = "Failed to start discovery: ${e.message}"
                _discoveryState.value = DiscoveryState.ERROR
            }
        }
    }

    /**
     * Stop network discovery service
     */
    fun stopDiscovery() {
        Log.d(TAG, "🛑 Stopping network discovery service...")
        
        discoveryJob?.cancel()
        
        _discoveryState.value = DiscoveryState.IDLE
        Log.d(TAG, "✅ Network discovery service stopped")
    }

    /**
     * Start network scanning for soundboard servers
     */
    private suspend fun startNetworkScanning() {
        try {
            val networkInfo = getNetworkTopology()
            val localAddress = networkInfo?.localAddress ?: return
            
            Log.d(TAG, "📡 Starting network scan from $localAddress")
            
            // Extract network base (e.g., 192.168.1.x)
            val parts = localAddress.split(".")
            if (parts.size == 4) {
                val networkBase = "${parts[0]}.${parts[1]}.${parts[2]}"
                
                // Scan common IP ranges
                for (i in 1..254) {
                    if (!discoveryJob?.isActive!!) break
                    
                    val targetIP = "$networkBase.$i"
                    discoveryScope.launch {
                        scanServerAt(targetIP, DEFAULT_PORT)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Network scanning error", e)
            _lastError.value = "Network scanning failed: ${e.message}"
        }
    }

    /**
     * Scan for soundboard server at specific address
     */
    private suspend fun scanServerAt(address: String, port: Int) {
        try {
            withContext(Dispatchers.IO) {
                val socket = Socket()
                socket.connect(InetSocketAddress(address, port), 3000) // 3 second timeout
                socket.close()
                
                // Server found, try to get info
                val server = DiscoveredServer(
                    name = "Soundboard Server",
                    address = address,
                    port = port,
                    hostname = address,
                    version = "Unknown",
                    platform = "Unknown",
                    isLocal = isLocalAddress(address),
                    quality = assessNetworkQuality(address),
                    discoveredAt = System.currentTimeMillis()
                )
                
                addDiscoveredServer(server)
            }
        } catch (e: Exception) {
            // Server not found at this address, continue silently
        }
    }

    /**
     * Update network topology information
     */
    private fun updateNetworkTopology() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            val connectionType = when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> ConnectionType.VPN
                else -> ConnectionType.UNKNOWN
            }
            
            val localAddress = getLocalIPAddress()
            val networkName = if (connectionType == ConnectionType.WIFI) {
                wifiManager.connectionInfo.ssid?.removeSurrounding("\"")
            } else null
            
            val topology = NetworkTopology(
                localAddress = localAddress,
                networkName = networkName,
                connectionType = connectionType,
                isMetered = connectivityManager.isActiveNetworkMetered,
                signalStrength = if (connectionType == ConnectionType.WIFI) wifiManager.connectionInfo.rssi else -1,
                linkSpeed = if (connectionType == ConnectionType.WIFI) wifiManager.connectionInfo.linkSpeed else -1,
                frequency = if (connectionType == ConnectionType.WIFI) wifiManager.connectionInfo.frequency else -1,
                capabilities = networkCapabilities?.let { caps ->
                    mutableListOf<String>().apply {
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) add("internet")
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) add("validated")
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) add("unmetered")
                    }
                } ?: emptyList()
            )
            
            _networkTopology.value = topology
            Log.d(TAG, "📊 Network topology updated: $topology")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update network topology", e)
            _lastError.value = "Failed to get network info: ${e.message}"
        }
    }

    /**
     * Get local IP address
     */
    private fun getLocalIPAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get local IP address", e)
        }
        return null
    }

    /**
     * Check if an address is local/private
     */
    private fun isLocalAddress(address: String?): Boolean {
        if (address == null) return false
        
        return try {
            val inet = InetAddress.getByName(address)
            inet.isSiteLocalAddress || inet.isLinkLocalAddress || inet.isLoopbackAddress
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Assess network quality to an address
     */
    private fun assessNetworkQuality(address: String?): NetworkQuality {
        if (address == null) return NetworkQuality.UNKNOWN
        
        return try {
            val startTime = System.currentTimeMillis()
            val isReachable = InetAddress.getByName(address).isReachable(3000)
            val latency = System.currentTimeMillis() - startTime
            
            when {
                !isReachable -> NetworkQuality.POOR
                latency < 50 -> NetworkQuality.EXCELLENT
                latency < 150 -> NetworkQuality.GOOD
                latency < 300 -> NetworkQuality.FAIR
                else -> NetworkQuality.POOR
            }
        } catch (e: Exception) {
            NetworkQuality.UNKNOWN
        }
    }

    /**
     * Add discovered server to the list
     */
    private fun addDiscoveredServer(server: DiscoveredServer) {
        val key = "${server.address}:${server.port}"
        serversMap[key] = server
        _discoveredServers.value = serversMap.toMap()
        
        if (_discoveryState.value == DiscoveryState.DISCOVERING) {
            _discoveryState.value = DiscoveryState.DISCOVERED
        }
        
        Log.d(TAG, "➕ Added server: ${server.name} at ${server.address}:${server.port}")
    }

    /**
     * Parse QR code connection data
     */
    fun parseQRConnectionData(qrContent: String): QRConnectionData? {
        return try {
            val connectionData = json.decodeFromString<QRConnectionData>(qrContent)
            
            // Validate QR code data
            if (connectionData.type != "soundboard_connection") {
                Log.w(TAG, "⚠️ Invalid QR code type: ${connectionData.type}")
                return null
            }
            
            // Check expiration
            val expiresAt = java.time.Instant.parse(connectionData.expires)
            if (java.time.Instant.now().isAfter(expiresAt)) {
                Log.w(TAG, "⚠️ QR code has expired")
                return null
            }
            
            Log.d(TAG, "✅ Valid QR connection data parsed")
            connectionData
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse QR code data", e)
            _lastError.value = "Invalid QR code format: ${e.message}"
            null
        }
    }

    /**
     * Create server from QR connection data
     */
    fun createServerFromQR(qrData: QRConnectionData): DiscoveredServer {
        return DiscoveredServer(
            name = qrData.server.name,
            address = qrData.server.address,
            port = qrData.server.port,
            hostname = qrData.server.hostname,
            version = qrData.version,
            token = qrData.connection.token,
            capabilities = qrData.capabilities,
            isLocal = isLocalAddress(qrData.server.address),
            quality = assessNetworkQuality(qrData.server.address),
            discoveredAt = System.currentTimeMillis()
        )
    }

    /**
     * Get network topology information
     */
    fun getNetworkTopology(): NetworkTopology? {
        return _networkTopology.value
    }

    /**
     * Start quality monitoring for discovered servers
     */
    private fun startQualityMonitoring() {
        discoveryScope.launch {
            while (isActive) {
                delay(SCAN_INTERVAL)
                
                // Update quality for all discovered servers
                val updatedServers = serversMap.mapValues { (_, server) ->
                    server.copy(
                        quality = assessNetworkQuality(server.address),
                        lastSeen = System.currentTimeMillis()
                    )
                }
                
                serversMap.clear()
                serversMap.putAll(updatedServers)
                _discoveredServers.value = serversMap.toMap()
            }
        }
    }

    /**
     * Get best available server
     */
    fun getBestServer(): DiscoveredServer? {
        return discoveredServers.value.values
            .filter { it.quality != NetworkQuality.POOR }
            .maxByOrNull { 
                when (it.quality) {
                    NetworkQuality.EXCELLENT -> 4
                    NetworkQuality.GOOD -> 3
                    NetworkQuality.FAIR -> 2
                    NetworkQuality.POOR -> 1
                    NetworkQuality.UNKNOWN -> 0
                }
            }
    }

    /**
     * Force refresh discovery
     */
    fun refreshDiscovery() {
        Log.d(TAG, "🔄 Refreshing network discovery...")
        
        discoveryScope.launch {
            updateNetworkTopology()
            
            // Clear old servers and restart discovery
            serversMap.clear()
            _discoveredServers.value = emptyMap()
            
            delay(1000) // Brief pause
            startNetworkScanning()
        }
    }

    /**
     * Get discovery status
     */
    fun getDiscoveryStatus(): Map<String, Any> {
        return mapOf(
            "state" to _discoveryState.value.name,
            "serversFound" to serversMap.size,
            "networkTopology" to (_networkTopology.value ?: "unknown"),
            "lastError" to (_lastError.value ?: "none"),
            "isActive" to (discoveryJob?.isActive == true)
        )
    }

    /**
     * Add manually discovered server
     */
    fun addManualServer(name: String, address: String, port: Int) {
        val server = DiscoveredServer(
            name = name,
            address = address,
            port = port,
            hostname = address,
            version = "Manual",
            platform = "Unknown",
            isLocal = isLocalAddress(address),
            quality = assessNetworkQuality(address),
            discoveredAt = System.currentTimeMillis()
        )
        
        addDiscoveredServer(server)
        Log.d(TAG, "➕ Manually added server: $name at $address:$port")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up NetworkDiscoveryService...")
        
        stopDiscovery()
        discoveryScope.cancel()
        serversMap.clear()
        _discoveredServers.value = emptyMap()
        
        Log.d(TAG, "✅ NetworkDiscoveryService cleanup complete")
    }
} 