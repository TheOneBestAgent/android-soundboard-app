package com.soundboard.android.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.2: Request Pipeline Manager
 * 
 * Enables request pipelining for parallel processing, batch operations,
 * and improved throughput with intelligent request ordering and priority management.
 */
@Singleton
class RequestPipelineManager @Inject constructor() {
    
    companion object {
        private const val TAG = "RequestPipelineManager"
        private const val DEFAULT_PIPELINE_SIZE = 8
        private const val REQUEST_TIMEOUT_MS = 30_000L // 30 seconds
        private const val PIPELINE_FLUSH_INTERVAL_MS = 100L // 100ms
        private const val THROUGHPUT_WINDOW_MS = 10_000L // 10 seconds
    }
    
    // Request representation
    data class PipelineRequest<T>(
        val requestId: String,
        val priority: RequestPriority,
        val requestType: RequestType,
        val payload: Any?,
        val timeout: Long,
        val retryCount: Int,
        val maxRetries: Int,
        val dependencies: List<String>, // IDs of requests this depends on
        val tags: Set<String>,
        val createdTimestamp: Long,
        val estimatedDuration: Long,
        val requestBlock: suspend () -> T,
        val deferred: CompletableDeferred<Result<T>>
    )
    
    enum class RequestPriority(val weight: Int) {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4),
        IMMEDIATE(5)
    }
    
    enum class RequestType(val description: String, val allowParallel: Boolean) {
        GET("GET Request", true),
        POST("POST Request", true),
        PUT("PUT Request", false), // Order dependent
        DELETE("DELETE Request", false), // Order dependent
        PATCH("PATCH Request", false), // Order dependent
        WEBSOCKET("WebSocket Message", true),
        UPLOAD("File Upload", true),
        DOWNLOAD("File Download", true),
        CUSTOM("Custom Request", true)
    }
    
    enum class RequestStatus {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
    
    // Pipeline configuration
    data class PipelineConfiguration(
        val maxConcurrentRequests: Int,
        val enableBatching: Boolean,
        val enablePipelining: Boolean,
        val enableDependencyTracking: Boolean,
        val enableRetries: Boolean,
        val defaultTimeout: Long,
        val maxRetries: Int
    )
    
    // Pipeline statistics
    data class PipelineStatistics(
        val totalRequests: Long,
        val completedRequests: Long,
        val failedRequests: Long,
        val cancelledRequests: Long,
        val averageLatency: Long,
        val currentThroughput: Double, // requests per second
        val activeRequests: Int,
        val queuedRequests: Int,
        val pipelineEfficiency: Double,
        val requestTypeDistribution: Map<RequestType, Long>
    )
    
    // Request tracking
    data class RequestTrackingInfo(
        val requestId: String,
        val status: RequestStatus,
        val startTime: Long,
        val endTime: Long?,
        val duration: Long?,
        val retryCount: Int,
        val errorMessage: String?
    )
    
    // Batch operation
    enum class BatchType {
        PARALLEL,    // Execute all requests in parallel
        SEQUENTIAL,  // Execute requests in order
        PRIORITY,    // Execute by priority order
        DEPENDENCY   // Execute based on dependencies
    }
    
    // Pipeline state management
    private val pipelineMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Request queues and tracking
    private val requestQueue = Channel<PipelineRequest<*>>(Channel.UNLIMITED)
    private val activeRequests = ConcurrentHashMap<String, RequestTrackingInfo>()
    private val completedRequests = ConcurrentHashMap<String, RequestTrackingInfo>()
    private val dependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    private val requestResults = ConcurrentHashMap<String, Any>()
    
    // Performance tracking
    private val requestHistory = ArrayDeque<Pair<Long, String>>(1000) // timestamp, requestId
    private var totalRequests = AtomicLong(0)
    private var completedRequestCount = AtomicLong(0)
    private var failedRequestCount = AtomicLong(0)
    private var cancelledRequestCount = AtomicLong(0)
    private var totalLatency = AtomicLong(0)
    
    // Configuration
    private var pipelineConfiguration = PipelineConfiguration(
        maxConcurrentRequests = DEFAULT_PIPELINE_SIZE,
        enableBatching = true,
        enablePipelining = true,
        enableDependencyTracking = true,
        enableRetries = true,
        defaultTimeout = REQUEST_TIMEOUT_MS,
        maxRetries = 3
    )
    
    // Pipeline workers
    private val workers = mutableListOf<Job>()
    
    // State flows for monitoring
    private val _pipelineStatistics = MutableStateFlow(
        PipelineStatistics(
            totalRequests = 0L,
            completedRequests = 0L,
            failedRequests = 0L,
            cancelledRequests = 0L,
            averageLatency = 0L,
            currentThroughput = 0.0,
            activeRequests = 0,
            queuedRequests = 0,
            pipelineEfficiency = 1.0,
            requestTypeDistribution = emptyMap()
        )
    )
    val pipelineStatistics: StateFlow<PipelineStatistics> = _pipelineStatistics.asStateFlow()
    
    private val _activeRequestsFlow = MutableStateFlow<List<RequestTrackingInfo>>(emptyList())
    val activeRequestsFlow: StateFlow<List<RequestTrackingInfo>> = _activeRequestsFlow.asStateFlow()
    
    /**
     * Initialize the pipeline manager
     */
    fun initialize(configuration: PipelineConfiguration = pipelineConfiguration) {
        scope.launch {
            pipelineMutex.withLock {
                pipelineConfiguration = configuration
                startPipelineWorkers()
                startStatisticsUpdater()
                startDependencyResolver()
            }
        }
        Log.i(TAG, "Pipeline manager initialized with ${configuration.maxConcurrentRequests} concurrent workers")
    }
    
    /**
     * Submit a request to the pipeline
     */
    suspend fun <T> submitRequest(
        priority: RequestPriority = RequestPriority.NORMAL,
        requestType: RequestType = RequestType.CUSTOM,
        payload: Any? = null,
        timeout: Long = pipelineConfiguration.defaultTimeout,
        maxRetries: Int = pipelineConfiguration.maxRetries,
        dependencies: List<String> = emptyList(),
        tags: Set<String> = emptySet(),
        estimatedDuration: Long = 1000L,
        requestBlock: suspend () -> T
    ): Deferred<Result<T>> {
        val requestId = generateRequestId()
        val deferred = CompletableDeferred<Result<T>>()
        
        val request = PipelineRequest(
            requestId = requestId,
            priority = priority,
            requestType = requestType,
            payload = payload,
            timeout = timeout,
            retryCount = 0,
            maxRetries = maxRetries,
            dependencies = dependencies,
            tags = tags,
            createdTimestamp = System.currentTimeMillis(),
            estimatedDuration = estimatedDuration,
            requestBlock = requestBlock,
            deferred = deferred
        )
        
        // Track dependencies
        if (pipelineConfiguration.enableDependencyTracking && dependencies.isNotEmpty()) {
            dependencies.forEach { depId ->
                dependencyGraph.getOrPut(depId) { mutableSetOf() }.add(requestId)
            }
        }
        
        // Add to queue
        requestQueue.send(request)
        totalRequests.incrementAndGet()
        
        // Track request
        activeRequests[requestId] = RequestTrackingInfo(
            requestId = requestId,
            status = RequestStatus.QUEUED,
            startTime = System.currentTimeMillis(),
            endTime = null,
            duration = null,
            retryCount = 0,
            errorMessage = null
        )
        
        Log.d(TAG, "Submitted request: $requestId (${priority.name} priority, ${requestType.name})")
        
        return deferred
    }
    
    /**
     * Submit a batch of requests
     */
    suspend fun <T> submitBatch(
        requests: List<suspend () -> T>,
        batchType: BatchType = BatchType.PARALLEL,
        priority: RequestPriority = RequestPriority.NORMAL,
        requestType: RequestType = RequestType.CUSTOM
    ): List<Deferred<Result<T>>> {
        val batchId = generateBatchId()
        val deferredResults = mutableListOf<Deferred<Result<T>>>()
        
        val pipelineRequests = requests.mapIndexed { index, requestBlock ->
            val requestId = "${batchId}_$index"
            val deferred = CompletableDeferred<Result<T>>()
            deferredResults.add(deferred)
            
            PipelineRequest(
                requestId = requestId,
                priority = priority,
                requestType = requestType,
                payload = null,
                timeout = pipelineConfiguration.defaultTimeout,
                retryCount = 0,
                maxRetries = pipelineConfiguration.maxRetries,
                dependencies = if (batchType == BatchType.SEQUENTIAL && index > 0) {
                    listOf("${batchId}_${index - 1}")
                } else emptyList(),
                tags = setOf("batch:$batchId"),
                createdTimestamp = System.currentTimeMillis(),
                estimatedDuration = 1000L,
                requestBlock = requestBlock,
                deferred = deferred
            )
        }
        
        // Submit all requests in batch
        pipelineRequests.forEach { request ->
            requestQueue.send(request)
            totalRequests.incrementAndGet()
            
            activeRequests[request.requestId] = RequestTrackingInfo(
                requestId = request.requestId,
                status = RequestStatus.QUEUED,
                startTime = System.currentTimeMillis(),
                endTime = null,
                duration = null,
                retryCount = 0,
                errorMessage = null
            )
        }
        
        Log.d(TAG, "Submitted batch: $batchId with ${requests.size} requests (${batchType.name})")
        
        return deferredResults
    }
    
    /**
     * Cancel a request
     */
    suspend fun cancelRequest(requestId: String): Boolean {
        return pipelineMutex.withLock {
            val tracking = activeRequests[requestId]
            if (tracking != null && tracking.status in listOf(RequestStatus.QUEUED, RequestStatus.PROCESSING)) {
                activeRequests[requestId] = tracking.copy(
                    status = RequestStatus.CANCELLED,
                    endTime = System.currentTimeMillis()
                )
                
                cancelledRequestCount.incrementAndGet()
                Log.d(TAG, "Cancelled request: $requestId")
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Get request status
     */
    fun getRequestStatus(requestId: String): RequestTrackingInfo? {
        return activeRequests[requestId] ?: completedRequests[requestId]
    }
    
    /**
     * Get all active requests
     */
    fun getActiveRequests(): List<RequestTrackingInfo> {
        return activeRequests.values.toList()
    }
    
    // Private helper methods
    
    private fun startPipelineWorkers() {
        repeat(pipelineConfiguration.maxConcurrentRequests) { workerId ->
            val worker = scope.launch {
                while (isActive) {
                    try {
                        processNextRequest(workerId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in worker $workerId", e)
                    }
                }
            }
            workers.add(worker)
        }
        Log.d(TAG, "Started ${workers.size} pipeline workers")
    }
    
    private suspend fun processNextRequest(workerId: Int) {
        try {
            val request = withTimeoutOrNull(PIPELINE_FLUSH_INTERVAL_MS) {
                requestQueue.receive()
            } ?: return
            
            // Check dependencies
            if (pipelineConfiguration.enableDependencyTracking && !areDependenciesSatisfied(request.dependencies)) {
                requestQueue.send(request)
                delay(10)
                return
            }
            
            // Update status to processing
            activeRequests[request.requestId] = activeRequests[request.requestId]!!.copy(
                status = RequestStatus.PROCESSING,
                startTime = System.currentTimeMillis()
            )
            
            // Execute request with timeout
            val result = try {
                withTimeout(request.timeout) {
                    request.requestBlock()
                }
            } catch (e: TimeoutCancellationException) {
                handleRequestTimeout(request)
                return
            } catch (e: Exception) {
                handleRequestError(request, e)
                return
            }
            
            // Handle successful completion
            handleRequestSuccess(request, result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in worker $workerId", e)
        }
    }
    
    private suspend fun handleRequestSuccess(request: PipelineRequest<*>, result: Any?) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - (activeRequests[request.requestId]?.startTime ?: endTime)
        
        val trackingInfo = RequestTrackingInfo(
            requestId = request.requestId,
            status = RequestStatus.COMPLETED,
            startTime = activeRequests[request.requestId]?.startTime ?: endTime,
            endTime = endTime,
            duration = duration,
            retryCount = request.retryCount,
            errorMessage = null
        )
        
        activeRequests.remove(request.requestId)
        completedRequests[request.requestId] = trackingInfo
        
        // Store result for dependencies
        requestResults[request.requestId] = result ?: Unit
        
        // Complete the deferred
        @Suppress("UNCHECKED_CAST")
        (request.deferred as CompletableDeferred<Result<Any?>>).complete(Result.success(result))
        
        // Update metrics
        completedRequestCount.incrementAndGet()
        totalLatency.addAndGet(duration)
        recordRequestCompletion(request.requestId)
        
        // Notify dependent requests
        notifyDependentRequests(request.requestId)
        
        Log.d(TAG, "Completed request: ${request.requestId} in ${duration}ms")
    }
    
    private suspend fun handleRequestError(request: PipelineRequest<*>, error: Exception) {
        // Check if we should retry
        if (pipelineConfiguration.enableRetries && request.retryCount < request.maxRetries) {
            val retryRequest = request.copy(retryCount = request.retryCount + 1)
            requestQueue.send(retryRequest)
            
            Log.d(TAG, "Retrying request: ${request.requestId} (attempt ${retryRequest.retryCount}/${request.maxRetries})")
            return
        }
        
        // Handle final failure
        val endTime = System.currentTimeMillis()
        val duration = endTime - (activeRequests[request.requestId]?.startTime ?: endTime)
        
        val trackingInfo = RequestTrackingInfo(
            requestId = request.requestId,
            status = RequestStatus.FAILED,
            startTime = activeRequests[request.requestId]?.startTime ?: endTime,
            endTime = endTime,
            duration = duration,
            retryCount = request.retryCount,
            errorMessage = error.message
        )
        
        activeRequests.remove(request.requestId)
        completedRequests[request.requestId] = trackingInfo
        
        // Complete the deferred with error
        @Suppress("UNCHECKED_CAST")
        (request.deferred as CompletableDeferred<Result<Any?>>).complete(Result.failure(error))
        
        // Update metrics
        failedRequestCount.incrementAndGet()
        recordRequestCompletion(request.requestId)
        
        Log.e(TAG, "Failed request: ${request.requestId} after ${request.retryCount} retries", error)
    }
    
    private suspend fun handleRequestTimeout(request: PipelineRequest<*>) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - (activeRequests[request.requestId]?.startTime ?: endTime)
        
        val trackingInfo = RequestTrackingInfo(
            requestId = request.requestId,
            status = RequestStatus.TIMEOUT,
            startTime = activeRequests[request.requestId]?.startTime ?: endTime,
            endTime = endTime,
            duration = duration,
            retryCount = request.retryCount,
            errorMessage = "Request timeout after ${request.timeout}ms"
        )
        
        activeRequests.remove(request.requestId)
        completedRequests[request.requestId] = trackingInfo
        
        // Complete the deferred with timeout error
        @Suppress("UNCHECKED_CAST")
        (request.deferred as CompletableDeferred<Result<Any?>>).complete(Result.failure(Exception("Request timeout")))
        
        // Update metrics
        failedRequestCount.incrementAndGet()
        recordRequestCompletion(request.requestId)
        
        Log.w(TAG, "Request timeout: ${request.requestId} after ${request.timeout}ms")
    }
    
    private fun areDependenciesSatisfied(dependencies: List<String>): Boolean {
        return dependencies.all { depId ->
            completedRequests.containsKey(depId) && 
            completedRequests[depId]?.status == RequestStatus.COMPLETED
        }
    }
    
    private fun notifyDependentRequests(completedRequestId: String) {
        dependencyGraph.remove(completedRequestId)
    }
    
    private fun startStatisticsUpdater() {
        scope.launch {
            while (true) {
                try {
                    updateStatistics()
                    delay(1000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating statistics", e)
                }
            }
        }
    }
    
    private fun startDependencyResolver() {
        if (pipelineConfiguration.enableDependencyTracking) {
            scope.launch {
                while (true) {
                    try {
                        cleanupDependencyGraph()
                        delay(10_000L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in dependency resolver", e)
                    }
                }
            }
        }
    }
    
    private fun cleanupDependencyGraph() {
        val completedIds = completedRequests.keys
        dependencyGraph.keys.removeAll(completedIds)
    }
    
    private fun recordRequestCompletion(requestId: String) {
        val currentTime = System.currentTimeMillis()
        requestHistory.offer(currentTime to requestId)
        
        val cutoffTime = currentTime - THROUGHPUT_WINDOW_MS
        while (requestHistory.isNotEmpty() && requestHistory.peek().first < cutoffTime) {
            requestHistory.poll()
        }
    }
    
    private fun updateStatistics() {
        val totalReq = totalRequests.get()
        val completedReq = completedRequestCount.get()
        val failedReq = failedRequestCount.get()
        val cancelledReq = cancelledRequestCount.get()
        
        val avgLatency = if (completedReq > 0) {
            totalLatency.get() / completedReq
        } else 0L
        
        val currentTime = System.currentTimeMillis()
        val recentRequests = requestHistory.count { 
            currentTime - it.first <= THROUGHPUT_WINDOW_MS 
        }
        val throughput = recentRequests.toDouble() / (THROUGHPUT_WINDOW_MS / 1000.0)
        
        val activeReq = activeRequests.size
        val queuedReq = 0 // Simplified
        
        val efficiency = if (totalReq > 0) {
            completedReq.toDouble() / totalReq
        } else 1.0
        
        _activeRequestsFlow.value = activeRequests.values.toList()
        
        _pipelineStatistics.value = PipelineStatistics(
            totalRequests = totalReq,
            completedRequests = completedReq,
            failedRequests = failedReq,
            cancelledRequests = cancelledReq,
            averageLatency = avgLatency,
            currentThroughput = throughput,
            activeRequests = activeReq,
            queuedRequests = queuedReq,
            pipelineEfficiency = efficiency,
            requestTypeDistribution = emptyMap()
        )
    }
    
    private fun generateRequestId(): String {
        return "req_${System.currentTimeMillis()}_${Random().nextInt(10000)}"
    }
    
    private fun generateBatchId(): String {
        return "batch_${System.currentTimeMillis()}_${Random().nextInt(1000)}"
    }
}