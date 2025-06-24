package com.soundboard.android.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.2: Connection Pool Manager
 * 
 * Manages multiple connection channels for improved performance and load distribution.
 * Provides connection pooling, load balancing, and automatic channel health monitoring.
 */
@Singleton
class ConnectionPoolManager @Inject constructor() {
    
    companion object {
        private const val TAG = "ConnectionPoolManager"
        private const val DEFAULT_POOL_SIZE = 4
        private const val MAX_POOL_SIZE = 8
        private const val MIN_POOL_SIZE = 2
        private const val HEALTH_CHECK_INTERVAL_MS = 30_000L // 30 seconds
        private const val CONNECTION_TIMEOUT_MS = 15_000L // 15 seconds
        private const val MAX_RETRIES = 3
    }
    
    // Connection channel representation
    data class ConnectionChannel(
        val channelId: String,
        val channelType: ChannelType,
        val priority: Int,
        val isActive: Boolean,
        val isHealthy: Boolean,
        val connectionCount: Int,
        val lastUsedTimestamp: Long,
        val createdTimestamp: Long,
        val latency: Long,
        val errorCount: Int,
        val successCount: Int,
        val currentLoad: Double // 0.0 to 1.0
    )
    
    enum class ChannelType(val description: String, val maxConnections: Int) {
        WEBSOCKET("WebSocket Channel", 1),
        HTTP_PERSISTENT("HTTP Persistent Channel", 2),
        HTTP_POOLED("HTTP Pooled Channel", 4),
        UDP_CHANNEL("UDP Channel", 1)
    }
    
    // Connection pool configuration
    data class PoolConfiguration(
        val poolSize: Int,
        val enableLoadBalancing: Boolean,
        val enableHealthChecking: Boolean,
        val connectionTimeout: Long,
        val maxRetries: Int,
        val preferredChannelTypes: List<ChannelType>
    )
    
    // Load balancing strategies
    enum class LoadBalancingStrategy {
        ROUND_ROBIN,        // Cycle through channels
        LEAST_CONNECTIONS,  // Use channel with fewest connections
        LEAST_LATENCY,      // Use channel with lowest latency
        WEIGHTED_ROUND_ROBIN, // Weighted by channel performance
        ADAPTIVE            // Dynamically choose best strategy
    }
    
    // Pool statistics
    data class PoolStatistics(
        val totalChannels: Int,
        val activeChannels: Int,
        val healthyChannels: Int,
        val totalConnections: Int,
        val averageLatency: Long,
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val poolEfficiency: Double, // Success rate
        val loadDistribution: Map<String, Double>
    )
    
    // Request routing information
    data class RoutingInfo(
        val requestId: String,
        val channelId: String,
        val priority: RequestPriority,
        val timestamp: Long,
        val estimatedLatency: Long
    )
    
    enum class RequestPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    // Pool state management
    private val poolMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionChannels = ConcurrentHashMap<String, ConnectionChannel>()
    private val roundRobinCounter = AtomicInteger(0)
    
    // Configuration and state
    private var poolConfiguration = PoolConfiguration(
        poolSize = DEFAULT_POOL_SIZE,
        enableLoadBalancing = true,
        enableHealthChecking = true,
        connectionTimeout = CONNECTION_TIMEOUT_MS,
        maxRetries = MAX_RETRIES,
        preferredChannelTypes = listOf(
            ChannelType.WEBSOCKET,
            ChannelType.HTTP_PERSISTENT,
            ChannelType.HTTP_POOLED
        )
    )
    
    private var loadBalancingStrategy = LoadBalancingStrategy.ADAPTIVE
    
    // State flows for monitoring
    private val _poolStatistics = MutableStateFlow(
        PoolStatistics(
            totalChannels = 0,
            activeChannels = 0,
            healthyChannels = 0,
            totalConnections = 0,
            averageLatency = 0L,
            totalRequests = 0L,
            successfulRequests = 0L,
            failedRequests = 0L,
            poolEfficiency = 1.0,
            loadDistribution = emptyMap()
        )
    )
    val poolStatistics: StateFlow<PoolStatistics> = _poolStatistics.asStateFlow()
    
    private val _channelHealth = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val channelHealth: StateFlow<Map<String, Boolean>> = _channelHealth.asStateFlow()
    
    // Performance tracking
    private val requestMetrics = ConcurrentHashMap<String, Long>()
    private var totalRequests = AtomicInteger(0)
    private var successfulRequests = AtomicInteger(0)
    private var failedRequests = AtomicInteger(0)
    
    /**
     * Initialize the connection pool
     */
    fun initialize(configuration: PoolConfiguration = poolConfiguration) {
        scope.launch {
            poolMutex.withLock {
                poolConfiguration = configuration
                initializeChannels()
                startHealthMonitoring()
                startStatisticsUpdater()
            }
        }
        Log.i(TAG, "Connection pool initialized with ${configuration.poolSize} channels")
    }
    
    /**
     * Get an optimal connection channel for a request
     */
    suspend fun getOptimalChannel(priority: RequestPriority = RequestPriority.NORMAL): ConnectionChannel? {
        return poolMutex.withLock {
            val availableChannels = connectionChannels.values.filter { 
                it.isActive && it.isHealthy 
            }
            
            if (availableChannels.isEmpty()) {
                Log.w(TAG, "No healthy channels available")
                return null
            }
            
            val selectedChannel = when (loadBalancingStrategy) {
                LoadBalancingStrategy.ROUND_ROBIN -> selectRoundRobin(availableChannels)
                LoadBalancingStrategy.LEAST_CONNECTIONS -> selectLeastConnections(availableChannels)
                LoadBalancingStrategy.LEAST_LATENCY -> selectLeastLatency(availableChannels)
                LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN -> selectWeightedRoundRobin(availableChannels)
                LoadBalancingStrategy.ADAPTIVE -> selectAdaptive(availableChannels, priority)
            }
            
            selectedChannel?.let { channel ->
                // Update channel usage
                updateChannelUsage(channel.channelId)
                Log.d(TAG, "Selected channel ${channel.channelId} (${channel.channelType}) for ${priority.name} priority request")
            }
            
            selectedChannel
        }
    }
    
    /**
     * Execute a request through the optimal channel
     */
    suspend fun <T> executeRequest(
        priority: RequestPriority = RequestPriority.NORMAL,
        requestBlock: suspend (ConnectionChannel) -> T
    ): Result<T> {
        val requestId = generateRequestId()
        totalRequests.incrementAndGet()
        
        return try {
            val channel = getOptimalChannel(priority)
                ?: return Result.failure(Exception("No available channels"))
            
            val startTime = System.currentTimeMillis()
            val result = requestBlock(channel)
            val endTime = System.currentTimeMillis()
            
            // Record success metrics
            recordRequestSuccess(requestId, channel.channelId, endTime - startTime)
            successfulRequests.incrementAndGet()
            
            Result.success(result)
            
        } catch (e: Exception) {
            // Record failure metrics
            recordRequestFailure(requestId, e)
            failedRequests.incrementAndGet()
            
            Log.e(TAG, "Request execution failed: $requestId", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods (abbreviated for brevity)
    private suspend fun initializeChannels() {
        connectionChannels.clear()
        val channelsPerType = poolConfiguration.poolSize / poolConfiguration.preferredChannelTypes.size
        var remainingChannels = poolConfiguration.poolSize
        
        poolConfiguration.preferredChannelTypes.forEach { channelType ->
            val channelCount = minOf(channelsPerType, remainingChannels)
            repeat(channelCount) {
                val channelId = generateChannelId(channelType)
                connectionChannels[channelId] = ConnectionChannel(
                    channelId = channelId,
                    channelType = channelType,
                    priority = channelType.ordinal + 1,
                    isActive = true,
                    isHealthy = true,
                    connectionCount = 0,
                    lastUsedTimestamp = System.currentTimeMillis(),
                    createdTimestamp = System.currentTimeMillis(),
                    latency = 0L,
                    errorCount = 0,
                    successCount = 0,
                    currentLoad = 0.0
                )
            }
            remainingChannels -= channelCount
        }
        updateStatistics()
    }
    
    private fun startHealthMonitoring() {
        if (poolConfiguration.enableHealthChecking) {
            scope.launch {
                while (true) {
                    try {
                        performHealthCheck()
                        kotlinx.coroutines.delay(HEALTH_CHECK_INTERVAL_MS)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in health monitoring", e)
                    }
                }
            }
        }
    }
    
    private fun startStatisticsUpdater() {
        scope.launch {
            while (true) {
                try {
                    updateStatistics()
                    kotlinx.coroutines.delay(5000) // Update every 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating statistics", e)
                }
            }
        }
    }
    
    private suspend fun performHealthCheck() {
        val healthMap = mutableMapOf<String, Boolean>()
        connectionChannels.values.forEach { channel ->
            val isHealthy = simulateHealthCheck(channel)
            healthMap[channel.channelId] = isHealthy
        }
        _channelHealth.value = healthMap
    }
    
    private fun simulateHealthCheck(channel: ConnectionChannel): Boolean {
        val errorRate = if (channel.successCount > 0) {
            channel.errorCount.toDouble() / (channel.successCount + channel.errorCount)
        } else 0.0
        return errorRate < 0.3 && channel.currentLoad < 0.9
    }
    
    private fun selectRoundRobin(channels: List<ConnectionChannel>): ConnectionChannel? {
        if (channels.isEmpty()) return null
        val index = roundRobinCounter.getAndIncrement() % channels.size
        return channels[index]
    }
    
    private fun selectLeastConnections(channels: List<ConnectionChannel>): ConnectionChannel? {
        return channels.minByOrNull { it.connectionCount }
    }
    
    private fun selectLeastLatency(channels: List<ConnectionChannel>): ConnectionChannel? {
        return channels.minByOrNull { it.latency }
    }
    
    private fun selectWeightedRoundRobin(channels: List<ConnectionChannel>): ConnectionChannel? {
        val weightedChannels = channels.map { channel ->
            val latencyWeight = if (channel.latency > 0) 1.0 / channel.latency else 1.0
            val errorRate = if (channel.successCount > 0) {
                channel.errorCount.toDouble() / (channel.successCount + channel.errorCount)
            } else 0.0
            val errorWeight = 1.0 - errorRate
            channel to (latencyWeight * errorWeight)
        }
        
        val totalWeight = weightedChannels.sumOf { it.second }
        if (totalWeight <= 0) return channels.firstOrNull()
        
        val random = Random().nextDouble() * totalWeight
        var currentWeight = 0.0
        
        for ((channel, weight) in weightedChannels) {
            currentWeight += weight
            if (random <= currentWeight) {
                return channel
            }
        }
        return channels.firstOrNull()
    }
    
    private fun selectAdaptive(channels: List<ConnectionChannel>, priority: RequestPriority): ConnectionChannel? {
        return when (priority) {
            RequestPriority.CRITICAL -> selectLeastLatency(channels)
            RequestPriority.HIGH -> selectLeastConnections(channels)
            RequestPriority.NORMAL -> selectWeightedRoundRobin(channels)
            RequestPriority.LOW -> selectRoundRobin(channels)
        }
    }
    
    private fun updateChannelUsage(channelId: String) {
        connectionChannels[channelId]?.let { channel ->
            connectionChannels[channelId] = channel.copy(
                lastUsedTimestamp = System.currentTimeMillis(),
                connectionCount = channel.connectionCount + 1
            )
        }
    }
    
    private fun recordRequestSuccess(requestId: String, channelId: String, latency: Long) {
        requestMetrics[requestId] = latency
        connectionChannels[channelId]?.let { channel ->
            connectionChannels[channelId] = channel.copy(
                successCount = channel.successCount + 1,
                latency = (channel.latency + latency) / 2
            )
        }
    }
    
    private fun recordRequestFailure(requestId: String, error: Exception) {
        Log.w(TAG, "Request failed: $requestId - ${error.message}")
    }
    
    private fun updateStatistics() {
        val channels = connectionChannels.values
        val activeChannels = channels.count { it.isActive }
        val healthyChannels = channels.count { it.isHealthy }
        val totalConnections = channels.sumOf { it.connectionCount }
        val averageLatency = if (channels.isNotEmpty()) {
            channels.mapNotNull { if (it.latency > 0) it.latency else null }.average().toLong()
        } else 0L
        
        val totalReq = totalRequests.get().toLong()
        val successReq = successfulRequests.get().toLong()
        val failedReq = failedRequests.get().toLong()
        val efficiency = if (totalReq > 0) successReq.toDouble() / totalReq else 1.0
        
        val loadDistribution = channels.associate { 
            it.channelId to it.currentLoad 
        }
        
        _poolStatistics.value = PoolStatistics(
            totalChannels = channels.size,
            activeChannels = activeChannels,
            healthyChannels = healthyChannels,
            totalConnections = totalConnections,
            averageLatency = averageLatency,
            totalRequests = totalReq,
            successfulRequests = successReq,
            failedRequests = failedReq,
            poolEfficiency = efficiency,
            loadDistribution = loadDistribution
        )
    }
    
    private fun generateChannelId(channelType: ChannelType): String {
        return "${channelType.name.lowercase()}_${System.currentTimeMillis()}_${Random().nextInt(1000)}"
    }
    
    private fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${Random().nextInt(10000)}"
    }
}