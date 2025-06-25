package com.soundboard.android.diagnostics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.Lazy

/**
 * DiagnosticsManager - Core system health monitoring and diagnostics
 * 
 * Provides comprehensive system health monitoring, performance bottleneck detection,
 * and diagnostic reporting with deep integration to Phase 4.2 performance components.
 * 
 * Key Features:
 * - Real-time system health monitoring
 * - Performance bottleneck detection and analysis
 * - Component health tracking across all Phase 4.2 systems
 * - Resource usage monitoring (CPU, memory, network, battery)
 * - Diagnostic report generation with actionable insights
 * - Health scoring system with weighted metrics
 */
@Singleton
class DiagnosticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggingProvider: Lazy<LoggingProvider>
) : DiagnosticsProvider {
    companion object {
        private const val MONITORING_INTERVAL_MS = 30_000L // 30 seconds
        private const val HEALTH_HISTORY_RETENTION_HOURS = 24
        private const val BOTTLENECK_DETECTION_WINDOW_MINUTES = 5
        private const val CRITICAL_HEALTH_THRESHOLD = 0.3
        private const val WARNING_HEALTH_THRESHOLD = 0.6
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Health monitoring state
    private val _systemHealthScore = MutableStateFlow(HealthScore.initial())
    val systemHealthScore: StateFlow<HealthScore> = _systemHealthScore.asStateFlow()
    
    private val _componentHealth = MutableStateFlow<Map<ComponentType, ComponentHealth>>(emptyMap())
    val componentHealth: StateFlow<Map<ComponentType, ComponentHealth>> = _componentHealth.asStateFlow()
    
    private val _detectedBottlenecks = MutableStateFlow<List<Bottleneck>>(emptyList())
    val detectedBottlenecks: StateFlow<List<Bottleneck>> = _detectedBottlenecks.asStateFlow()
    
    private val _resourceUsage = MutableStateFlow(ResourceUsageSnapshot.initial())
    val resourceUsage: StateFlow<ResourceUsageSnapshot> = _resourceUsage.asStateFlow()

    // Internal monitoring data
    private val healthHistory = ConcurrentHashMap<ComponentType, MutableList<HealthSnapshot>>()
    private val resourceTrends = MutableList(100) { ResourceUsageSnapshot.initial() }
    private val bottleneckHistory = MutableList(50) { mutableListOf<Bottleneck>() }
    private val monitoringStartTime = System.currentTimeMillis()
    
    // Monitoring control
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    
    // Performance tracking
    private val diagnosticsOverhead = AtomicLong(0)
    private val healthComputations = AtomicLong(0)

    private val healthMetrics = ConcurrentHashMap<String, AtomicLong>()
    private val performanceMetrics = ConcurrentHashMap<String, AtomicLong>()
    private val diagnosticEvents = MutableSharedFlow<DiagnosticEvent>()
    
    // Health check intervals
    private val systemHealthInterval = 30.seconds
    private val performanceCheckInterval = 1.minutes
    private val diagnosticReportInterval = 5.minutes
    
    init {
        scope.launch {
            initialize()
        }
    }

    /**
     * Initialize diagnostic monitoring
     */
    private suspend fun initialize() {
        scope.launch {
            startHealthMonitoring()
            startResourceMonitoring()
            startBottleneckDetection()
            isMonitoring = true
            loggingProvider.get().logInfo(
                "DiagnosticsManager initialized successfully",
                mapOf(
                    "monitoringInterval" to MONITORING_INTERVAL_MS,
                    "healthRetention" to HEALTH_HISTORY_RETENTION_HOURS,
                    "bottleneckWindow" to BOTTLENECK_DETECTION_WINDOW_MINUTES
                )
            )
        }
    }

    /**
     * Shutdown diagnostic monitoring and cleanup resources
     */
    suspend fun shutdown() {
        isMonitoring = false
        monitoringJob?.cancel()
        scope.cancel()
        loggingProvider.get().logInfo("DiagnosticsManager shutdown completed", emptyMap())
    }

    // =============================================================================
    // DiagnosticsProvider Implementation
    // =============================================================================

    override suspend fun getHealthMetric(name: String): Long {
        return healthMetrics[name]?.get() ?: 0L
    }

    override suspend fun getPerformanceMetric(name: String): Long {
        return performanceMetrics[name]?.get() ?: 0L
    }

    override suspend fun getCurrentReport(): DiagnosticReport {
        val healthScore = getSystemHealthScore()
        val bottlenecks = detectPerformanceBottlenecks()
        val resource = resourceUsage.value
        val trends = resourceTrends.takeLast(50)
        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            systemHealth = healthScore,
            bottlenecks = bottlenecks,
            resourceUsage = resource,
            resourceTrends = trends,
            recommendations = generateRecommendations(healthScore, bottlenecks, resource),
            monitoringDuration = System.currentTimeMillis() - monitoringStartTime,
            diagnosticsOverhead = calculateDiagnosticsOverhead(),
            performanceMetrics = performanceMetrics.mapValues { it.value.get() },
            healthMetrics = healthMetrics.mapValues { it.value.get() }
        )
    }

    // =============================================================================
    // CORE MONITORING API
    // =============================================================================

    /**
     * Get current comprehensive system health score
     */
    suspend fun getSystemHealthScore(): HealthScore {
        val startTime = System.nanoTime()
        
        try {
            val componentHealthMap = getCurrentComponentHealth()
            val resourceHealth = calculateResourceHealth()
            val performanceHealth = calculatePerformanceHealth()
            
            val overallScore = calculateWeightedHealth(
                componentHealth = componentHealthMap.values.map { it.score },
                resourceHealth = resourceHealth,
                performanceHealth = performanceHealth
            )
            
            val healthFactors = identifyHealthFactors(componentHealthMap, resourceHealth, performanceHealth)
            
            val healthScore = HealthScore(
                overall = overallScore,
                components = componentHealthMap.mapValues { it.value.score },
                timestamp = System.currentTimeMillis(),
                factors = healthFactors,
                trend = calculateHealthTrend(),
                confidence = calculateConfidenceLevel(componentHealthMap.size)
            )
            
            _systemHealthScore.value = healthScore
            return healthScore
            
        } finally {
            diagnosticsOverhead.addAndGet(System.nanoTime() - startTime)
            healthComputations.incrementAndGet()
        }
    }

    /**
     * Detect current performance bottlenecks with detailed analysis
     */
    suspend fun detectPerformanceBottlenecks(): List<Bottleneck> {
        val currentBottlenecks = mutableListOf<Bottleneck>()
        
        // Analyze each component for bottlenecks
        ComponentType.values().forEach { component ->
            val componentBottlenecks = analyzeComponentBottlenecks(component)
            currentBottlenecks.addAll(componentBottlenecks)
        }
        
        // Analyze resource bottlenecks
        val resourceBottlenecks = analyzeResourceBottlenecks()
        currentBottlenecks.addAll(resourceBottlenecks)
        
        // Analyze performance trends
        val trendBottlenecks = analyzeTrendBottlenecks()
        currentBottlenecks.addAll(trendBottlenecks)
        
        // Sort by severity and impact
        val sortedBottlenecks = currentBottlenecks.sortedWith(
            compareByDescending<Bottleneck> { it.severity.priority }
                .thenByDescending { it.impact.magnitude }
        )
        
        _detectedBottlenecks.value = sortedBottlenecks
        return sortedBottlenecks
    }

    /**
     * Generate comprehensive diagnostic report
     */
    suspend fun generateDiagnosticReport(): DiagnosticReport {
        val healthScore = getSystemHealthScore()
        val bottlenecks = detectPerformanceBottlenecks()
        val resource = resourceUsage.value
        val trends = resourceTrends.takeLast(50)
        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            systemHealth = healthScore,
            bottlenecks = bottlenecks,
            resourceUsage = resource,
            resourceTrends = trends,
            recommendations = generateRecommendations(healthScore, bottlenecks, resource),
            monitoringDuration = System.currentTimeMillis() - monitoringStartTime,
            diagnosticsOverhead = calculateDiagnosticsOverhead()
        )
    }

    // =============================================================================
    // COMPONENT HEALTH TRACKING
    // =============================================================================

    /**
     * Monitor specific component health
     */
    suspend fun monitorComponent(component: ComponentType): ComponentHealth {
        val health = when (component) {
            ComponentType.CONNECTION_POOL -> monitorConnectionPoolHealth()
            ComponentType.CACHE -> monitorCacheHealth()
            ComponentType.COMPRESSION -> monitorCompressionHealth()
            ComponentType.PIPELINE -> monitorPipelineHealth()
            ComponentType.METRICS -> monitorMetricsHealth()
            ComponentType.NETWORK -> monitorNetworkHealth()
            ComponentType.SYSTEM -> monitorSystemHealth()
            else -> monitorSystemHealth()
        }
        
        // Update component health map
        val currentHealth = _componentHealth.value.toMutableMap()
        currentHealth[component] = health
        _componentHealth.value = currentHealth
        
        // Add to history
        addToHealthHistory(component, HealthSnapshot(health.score, System.currentTimeMillis()))
        
        return health
    }

    /**
     * Get component health history
     */
    suspend fun getComponentHealthHistory(component: ComponentType): List<HealthSnapshot> {
        return healthHistory[component]?.toList() ?: emptyList()
    }

    // =============================================================================
    // RESOURCE MONITORING
    // =============================================================================

    /**
     * Track resource usage trends
     */
    suspend fun trackResourceTrends(): ResourceTrends {
        val recentTrends = resourceTrends.takeLast(50)
        
        return ResourceTrends(
            memoryTrend = calculateTrend(recentTrends.map { it.memoryUsed }),
            cpuTrend = calculateTrend(recentTrends.map { it.cpuUsage }),
            networkTrend = calculateTrend(recentTrends.map { it.networkLatency }),
            batteryTrend = calculateTrend(recentTrends.map { it.batteryLevel }),
            overallTrend = calculateOverallResourceTrend(recentTrends)
        )
    }

    /**
     * Get resource usage trends
     */
    fun getResourceTrends(): ResourceTrends {
        val recentTrends = resourceTrends.takeLast(50)
        
        return ResourceTrends(
            memoryTrend = calculateTrend(recentTrends.map { it.memoryUsed }),
            cpuTrend = calculateTrend(recentTrends.map { it.cpuUsage }),
            networkTrend = calculateTrend(recentTrends.map { it.networkLatency }),
            batteryTrend = calculateTrend(recentTrends.map { it.batteryLevel }),
            overallTrend = calculateOverallResourceTrend(recentTrends)
        )
    }

    // =============================================================================
    // PHASE 4.2 INTEGRATION
    // =============================================================================

    /**
     * Analyze Phase 4.2 performance metrics for diagnostic insights
     */
    suspend fun analyzePerformanceMetrics(): PerformanceAnalysis {
        // This will integrate with Phase 4.2 PerformanceMetrics component
        // For now, return placeholder analysis
        return PerformanceAnalysis(
            timestamp = System.currentTimeMillis(),
            connectionPoolAnalysis = analyzeConnectionPoolMetrics(),
            cacheAnalysis = analyzeCacheMetrics(),
            compressionAnalysis = analyzeCompressionMetrics(),
            pipelineAnalysis = analyzePipelineMetrics(),
            overallEfficiency = calculateOverallEfficiency(),
            performanceScore = calculatePerformanceScore()
        )
    }

    /**
     * Correlate metrics with health status
     */
    suspend fun correlateMetricsWithHealth(): CorrelationReport {
        val healthScore = getSystemHealthScore()
        val performanceAnalysis = analyzePerformanceMetrics()
        
        return CorrelationReport(
            timestamp = System.currentTimeMillis(),
            healthScore = healthScore.overall,
            performanceScore = performanceAnalysis.performanceScore,
            correlation = calculateHealthPerformanceCorrelation(healthScore, performanceAnalysis),
            insights = generateCorrelationInsights(healthScore, performanceAnalysis),
            recommendations = generateCorrelationRecommendations(healthScore, performanceAnalysis)
        )
    }

    // =============================================================================
    // INTERNAL MONITORING IMPLEMENTATION
    // =============================================================================

    private fun startHealthMonitoring() {
        monitoringJob = scope.launch {
            isMonitoring = true
            while (isMonitoring) {
                try {
                    ComponentType.values().forEach { component ->
                        monitorComponent(component)
                    }
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    // Log error but continue monitoring
                    delay(MONITORING_INTERVAL_MS)
                }
            }
        }
    }

    private fun startResourceMonitoring() {
        scope.launch {
            while (isMonitoring) {
                try {
                    updateResourceUsage()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    delay(MONITORING_INTERVAL_MS)
                }
            }
        }
    }

    private fun startBottleneckDetection() {
        scope.launch {
            while (isMonitoring) {
                try {
                    detectPerformanceBottlenecks()
                    delay(BOTTLENECK_DETECTION_WINDOW_MINUTES * 60 * 1000L) // 5 minutes
                } catch (e: Exception) {
                    delay(BOTTLENECK_DETECTION_WINDOW_MINUTES * 60 * 1000L)
                }
            }
        }
    }

    // =============================================================================
    // HEALTH CALCULATION METHODS
    // =============================================================================

    private suspend fun getCurrentComponentHealth(): Map<ComponentType, ComponentHealth> {
        val healthMap = mutableMapOf<ComponentType, ComponentHealth>()
        
        ComponentType.values().forEach { component ->
            healthMap[component] = monitorComponent(component)
        }
        
        return healthMap
    }

    private fun calculateResourceHealth(): Double {
        val currentUsage = resourceUsage.value
        
        val memoryHealth = 1.0 - (currentUsage.memoryUsed / currentUsage.memoryTotal.coerceAtLeast(1.0))
        val cpuHealth = 1.0 - (currentUsage.cpuUsage / 100.0)
        val networkHealth = when {
            currentUsage.networkLatency < 50 -> 1.0
            currentUsage.networkLatency < 100 -> 0.8
            currentUsage.networkLatency < 200 -> 0.6
            currentUsage.networkLatency < 500 -> 0.4
            else -> 0.2
        }
        val batteryHealth = currentUsage.batteryLevel / 100.0
        
        return (memoryHealth + cpuHealth + networkHealth + batteryHealth) / 4.0
    }

    private fun calculatePerformanceHealth(): Double {
        // Placeholder - will integrate with Phase 4.2 PerformanceMetrics
        return 0.85
    }

    private fun calculateWeightedHealth(
        componentHealth: List<Double>,
        resourceHealth: Double,
        performanceHealth: Double
    ): Double {
        val componentWeight = 0.5
        val resourceWeight = 0.3
        val performanceWeight = 0.2
        
        val avgComponentHealth = if (componentHealth.isNotEmpty()) {
            componentHealth.average()
        } else 1.0
        
        return (avgComponentHealth * componentWeight) +
                (resourceHealth * resourceWeight) +
                (performanceHealth * performanceWeight)
    }

    private fun identifyHealthFactors(
        componentHealth: Map<ComponentType, ComponentHealth>,
        resourceHealth: Double,
        performanceHealth: Double
    ): List<String> {
        val factors = mutableListOf<String>()
        
        // Check component health factors
        componentHealth.forEach { (component, health) ->
            when {
                health.score < 0.3 -> {
                    factors.add("Critical: $component")
                }
                health.score < 0.7 -> {
                    factors.add("High: $component")
                }
                health.score < 0.9 -> {
                    factors.add("Medium: $component")
                }
            }
        }
        
        // Check resource health factors
        if (resourceHealth < 0.3) {
            factors.add("Critical: Resource usage")
        } else if (resourceHealth < 0.7) {
            factors.add("High: Resource usage")
        }
        
        // Check performance health factors
        if (performanceHealth < 0.3) {
            factors.add("Critical: Performance")
        } else if (performanceHealth < 0.7) {
            factors.add("High: Performance")
        }
        
        return factors
    }

    private fun calculateHealthTrend(): TrendDirection {
        // Analyze recent health scores to determine trend
        return TrendDirection.STABLE // Placeholder
    }

    private fun calculateConfidenceLevel(componentCount: Int): Double {
        // Confidence based on number of monitored components and data quality
        return (componentCount / ComponentType.values().size.toDouble()).coerceAtMost(1.0)
    }

    // =============================================================================
    // COMPONENT-SPECIFIC HEALTH MONITORING
    // =============================================================================

    private suspend fun monitorConnectionPoolHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 ConnectionPoolManager
        return ComponentHealth(
            component = ComponentType.CONNECTION_POOL,
            score = 0.9,
            metrics = mapOf(
                "activeConnections" to 5.0,
                "successRate" to 0.98,
                "avgLatency" to 45.0
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorCacheHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 CacheManager
        return ComponentHealth(
            component = ComponentType.CACHE,
            score = 0.85,
            metrics = mapOf(
                "hitRate" to 0.82,
                "memoryUsage" to 65.0,
                "evictionRate" to 0.05
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorCompressionHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 CompressionManager
        return ComponentHealth(
            component = ComponentType.COMPRESSION,
            score = 0.92,
            metrics = mapOf(
                "compressionRatio" to 0.7,
                "processingTime" to 12.0,
                "bandwidthSavings" to 0.3
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorPipelineHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 RequestPipelineManager
        return ComponentHealth(
            component = ComponentType.PIPELINE,
            score = 0.88,
            metrics = mapOf(
                "throughput" to 150.0,
                "queueSize" to 3.0,
                "errorRate" to 0.02
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorMetricsHealth(): ComponentHealth {
        // Monitor the PerformanceMetrics component itself
        return ComponentHealth(
            component = ComponentType.METRICS,
            score = 0.95,
            metrics = mapOf(
                "dataAccuracy" to 0.99,
                "processingLatency" to 8.0,
                "storageUsage" to 12.0
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorNetworkHealth(): ComponentHealth {
        val latency = estimateNetworkLatency()
        val score = when {
            latency < 50 -> 1.0
            latency < 100 -> 0.9
            latency < 200 -> 0.7
            latency < 500 -> 0.5
            else -> 0.3
        }
        
        return ComponentHealth(
            component = ComponentType.NETWORK,
            score = score,
            metrics = mapOf(
                "latency" to latency,
                "availability" to 0.99,
                "bandwidth" to 25.0
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorSystemHealth(): ComponentHealth {
        val resourceUsage = resourceUsage.value
        val memoryHealth = 1.0 - (resourceUsage.memoryUsed / resourceUsage.memoryTotal)
        val cpuHealth = 1.0 - (resourceUsage.cpuUsage / 100.0)
        val overallHealth = (memoryHealth + cpuHealth) / 2.0
        
        return ComponentHealth(
            component = ComponentType.SYSTEM,
            score = overallHealth,
            metrics = mapOf(
                "memoryUsage" to resourceUsage.memoryUsed,
                "cpuUsage" to resourceUsage.cpuUsage,
                "threadCount" to resourceUsage.threadCount.toDouble()
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun monitorUIHealth(): ComponentHealth {
        // Placeholder for UI health monitoring
        return ComponentHealth(
            component = ComponentType.UI_MAIN,
            score = 0.95,
            metrics = mapOf(
                "responsiveness" to 0.95,
                "render_time" to 16.0
            )
        )
    }

    // =============================================================================
    // BOTTLENECK ANALYSIS
    // =============================================================================

    private suspend fun analyzeComponentBottlenecks(component: ComponentType): List<Bottleneck> {
        val health = monitorComponent(component)
        val bottlenecks = mutableListOf<Bottleneck>()
        
        if (health.score < 0.3) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.COMPONENT_DEGRADATION,
                    severity = Severity.CRITICAL,
                    component = component,
                    impact = ImpactAssessment(
                        magnitude = 1.0 - health.score,
                        affectedComponents = listOf(component),
                        userImpact = UserImpact.HIGH
                    ),
                    recommendations = generateComponentRecommendations(component, health),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        return bottlenecks
    }

    private suspend fun analyzeResourceBottlenecks(): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()
        val resourceUsage = resourceUsage.value
        
        // Check memory bottlenecks
        if (resourceUsage.memoryUsed / resourceUsage.memoryTotal > 0.8) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.MEMORY_PRESSURE,
                    severity = if (resourceUsage.memoryUsed / resourceUsage.memoryTotal > 0.9) 
                        Severity.CRITICAL else Severity.HIGH,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = resourceUsage.memoryUsed / resourceUsage.memoryTotal,
                        affectedComponents = listOf(ComponentType.SYSTEM),
                        userImpact = UserImpact.HIGH
                    ),
                    recommendations = listOf(
                        "Clear memory caches",
                        "Review memory allocation patterns",
                        "Consider increasing memory limits"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check CPU bottlenecks
        if (resourceUsage.cpuUsage > 75) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.CPU_SATURATION,
                    severity = if (resourceUsage.cpuUsage > 90) Severity.CRITICAL else Severity.HIGH,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = resourceUsage.cpuUsage / 100.0,
                        affectedComponents = listOf(ComponentType.SYSTEM),
                        userImpact = UserImpact.HIGH
                    ),
                    recommendations = listOf(
                        "Optimize CPU-intensive operations",
                        "Review thread pool configuration",
                        "Consider scaling resources"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check network bottlenecks
        if (resourceUsage.networkLatency > 100) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.NETWORK_LATENCY,
                    severity = if (resourceUsage.networkLatency > 200) Severity.CRITICAL else Severity.HIGH,
                    component = ComponentType.NETWORK,
                    impact = ImpactAssessment(
                        magnitude = resourceUsage.networkLatency / 1000.0,
                        affectedComponents = listOf(ComponentType.NETWORK),
                        userImpact = UserImpact.HIGH
                    ),
                    recommendations = listOf(
                        "Check network connectivity",
                        "Review network configuration",
                        "Consider connection pooling"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        return bottlenecks
    }

    private suspend fun analyzeTrendBottlenecks(): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()
        val trends = getResourceTrends()
        
        // Check memory trend
        if (trends.memoryTrend == TrendDirection.IMPROVING) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.MEMORY_LEAK,
                    severity = Severity.HIGH,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = 0.5,
                        affectedComponents = listOf(ComponentType.SYSTEM),
                        userImpact = UserImpact.MEDIUM
                    ),
                    recommendations = listOf(
                        "Investigate potential memory leaks",
                        "Monitor object lifecycles",
                        "Review memory allocation patterns"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check thread contention trend
        if (trends.cpuTrend == TrendDirection.IMPROVING) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.THREAD_CONTENTION,
                    severity = Severity.MEDIUM,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = 0.3,
                        affectedComponents = listOf(ComponentType.SYSTEM),
                        userImpact = UserImpact.MEDIUM
                    ),
                    recommendations = listOf(
                        "Review thread synchronization",
                        "Optimize lock contention",
                        "Consider thread pool tuning"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check resource exhaustion trend
        if (trends.overallTrend == TrendDirection.IMPROVING) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.RESOURCE_EXHAUSTION,
                    severity = Severity.HIGH,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = 0.4,
                        affectedComponents = listOf(ComponentType.SYSTEM),
                        userImpact = UserImpact.HIGH
                    ),
                    recommendations = listOf(
                        "Review resource allocation",
                        "Implement resource pooling",
                        "Consider scaling resources"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        return bottlenecks
    }

    // =============================================================================
    // HELPER METHODS
    // =============================================================================

    private fun addToHealthHistory(component: ComponentType, snapshot: HealthSnapshot) {
        val history = healthHistory.getOrPut(component) { mutableListOf() }
        history.add(snapshot)
        
        // Keep only recent history
        val cutoffTime = System.currentTimeMillis() - (HEALTH_HISTORY_RETENTION_HOURS * 60 * 60 * 1000)
        history.removeAll { it.timestamp < cutoffTime }
    }

    private fun addToResourceTrends(usage: ResourceUsageSnapshot) {
        if (resourceTrends.size >= 100) {
            resourceTrends.removeAt(0)
        }
        resourceTrends.add(usage)
    }

    private fun calculateTrend(values: List<Double>): TrendDirection {
        if (values.size < 3) return TrendDirection.STABLE
        
        val recent = values.takeLast(5)
        val older = values.dropLast(5).takeLast(5)
        
        if (recent.isEmpty() || older.isEmpty()) return TrendDirection.STABLE
        
        val recentAvg = recent.average()
        val olderAvg = older.average()
        
        return when {
            recentAvg > olderAvg * 1.1 -> TrendDirection.IMPROVING
            recentAvg < olderAvg * 0.9 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun calculateOverallResourceTrend(trends: List<ResourceUsageSnapshot>): TrendDirection {
        if (trends.size < 6) return TrendDirection.STABLE
        
        val memoryTrend = calculateTrend(trends.map { it.memoryUsed })
        val cpuTrend = calculateTrend(trends.map { it.cpuUsage })
        val networkTrend = calculateTrend(trends.map { it.networkLatency })
        
        val improvingCount = listOf(memoryTrend, cpuTrend, networkTrend).count { it == TrendDirection.IMPROVING }
        val decreasingCount = listOf(memoryTrend, cpuTrend, networkTrend).count { it == TrendDirection.DECREASING }
        
        return when {
            improvingCount >= 2 -> TrendDirection.IMPROVING
            decreasingCount >= 2 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun generateComponentRecommendations(component: ComponentType, health: ComponentHealth): List<String> {
        return when (component) {
            ComponentType.CONNECTION_POOL -> listOf(
                "Check connection pool configuration",
                "Verify network connectivity",
                "Review load balancing strategy"
            )
            ComponentType.CACHE -> listOf(
                "Clear cache if full",
                "Adjust cache size limits",
                "Review eviction strategy"
            )
            ComponentType.COMPRESSION -> listOf(
                "Check compression algorithm efficiency",
                "Verify network conditions",
                "Review compression settings"
            )
            ComponentType.PIPELINE -> listOf(
                "Monitor request queue size",
                "Check for request timeouts",
                "Review parallel processing configuration"
            )
            ComponentType.METRICS -> listOf(
                "Verify metrics collection accuracy",
                "Check storage usage",
                "Review processing latency"
            )
            ComponentType.NETWORK -> listOf(
                "Check network connectivity",
                "Verify server accessibility",
                "Review network configuration"
            )
            ComponentType.SYSTEM -> listOf(
                "Monitor system resources",
                "Check for memory leaks",
                "Review thread usage"
            )
            ComponentType.UI_MAIN,
            ComponentType.UI_SETTINGS,
            ComponentType.UI_DIALOG,
            ComponentType.UI_LAYOUT,
            ComponentType.UI_SOUNDBOARD,
            ComponentType.UI_MONITORING -> listOf(
                "Check for UI freezes or jank",
                "Review layout complexity",
                "Optimize resource usage in UI components"
            )
        }
    }

    private fun generateRecommendations(
        healthScore: HealthScore,
        bottlenecks: List<Bottleneck>,
        resourceUsage: ResourceUsageSnapshot
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (healthScore.overall < 0.3) {
            recommendations.add("System health is critical - immediate attention required")
        }
        
        bottlenecks.forEach { bottleneck ->
            recommendations.addAll(bottleneck.recommendations)
        }
        
        if (resourceUsage.memoryUsed / resourceUsage.memoryTotal > 0.8) {
            recommendations.add("Consider reducing memory usage or clearing caches")
        }
        
        if (resourceUsage.cpuUsage > 70) {
            recommendations.add("High CPU usage detected - consider optimizing processing")
        }
        
        return recommendations.distinct()
    }

    private fun calculateDiagnosticsOverhead(): Double {
        val totalOverheadNs = diagnosticsOverhead.get()
        val computationCount = healthComputations.get()
        
        if (computationCount == 0L) return 0.0
        
        val avgOverheadMs = (totalOverheadNs / computationCount) / 1_000_000.0
        return avgOverheadMs
    }

    // Placeholder implementations for resource monitoring
    private suspend fun estimateCpuUsage(): Double = withContext(Dispatchers.IO) {
        // Implementation for CPU usage estimation
        0.0 // Placeholder
    }

    private suspend fun estimateNetworkLatency(): Double = withContext(Dispatchers.IO) {
        // Implementation for network latency estimation
        0.0 // Placeholder
    }

    private suspend fun estimateDiskUsage(): Double = withContext(Dispatchers.IO) {
        // Implementation for disk usage estimation
        0.0 // Placeholder
    }

    private suspend fun getBatteryLevel(): Double = withContext(Dispatchers.IO) {
        // Implementation for battery level estimation
        100.0 // Placeholder
    }

    private suspend fun estimateGcPressure(): Double = withContext(Dispatchers.IO) {
        // Implementation for GC pressure estimation
        0.0 // Placeholder
    }

    private suspend fun getMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0) // Convert to MB
        val totalMemory = runtime.maxMemory() / (1024.0 * 1024.0) // Convert to MB
        val availableMemory = totalMemory - usedMemory
        MemoryInfo(used = usedMemory, total = totalMemory, available = availableMemory)
    }

    private suspend fun getCpuInfo(): CpuInfo = withContext(Dispatchers.IO) {
        val cpuUsage = estimateCpuUsage()
        CpuInfo(usage = cpuUsage)
    }

    private suspend fun getDiskInfo(): DiskInfo = withContext(Dispatchers.IO) {
        val diskUsage = estimateDiskUsage()
        DiskInfo(usage = diskUsage)
    }

    private suspend fun getNetworkLatency(): Double = withContext(Dispatchers.IO) {
        estimateNetworkLatency()
    }

    // Placeholder implementations for Phase 4.2 integration
    private fun analyzeConnectionPoolMetrics(): String = "Connection pool healthy"
    private fun analyzeCacheMetrics(): String = "Cache performance good"
    private fun analyzeCompressionMetrics(): String = "Compression efficiency optimal"
    private fun analyzePipelineMetrics(): String = "Pipeline throughput stable"
    private fun calculateOverallEfficiency(): Double = 0.87
    private fun calculatePerformanceScore(): Double = 0.89
    private fun calculateHealthPerformanceCorrelation(health: HealthScore, performance: PerformanceAnalysis): Double = 0.82
    private fun generateCorrelationInsights(health: HealthScore, performance: PerformanceAnalysis): List<String> = 
        listOf("Health and performance are positively correlated")
    private fun generateCorrelationRecommendations(health: HealthScore, performance: PerformanceAnalysis): List<String> = 
        listOf("Continue current optimization strategies")

    // =============================================================================
    // MISSING METHODS FOR EXTERNAL API
    // =============================================================================

    /**
     * Get current health score as Flow for reactive monitoring
     */
    fun getCurrentHealthScore(): StateFlow<HealthScore> = systemHealthScore

    /**
     * Get component health status as Flow for reactive monitoring
     */
    fun getComponentHealthFlow(): StateFlow<Map<ComponentType, ComponentHealth>> = componentHealth

    /**
     * Get active bottlenecks as Flow for reactive monitoring
     */
    fun getActiveBottlenecks(): StateFlow<List<Bottleneck>> = detectedBottlenecks

    /**
     * Get resource usage as Flow for reactive monitoring
     */
    fun getResourceUsageFlow(): StateFlow<ResourceUsageSnapshot> = resourceUsage

    /**
     * Perform comprehensive health check and return detailed results
     */
    suspend fun performHealthCheck(): DiagnosticReport {
        val health = getSystemHealthScore()
        val bottlenecks = analyzeBottlenecks()
        val resource = resourceUsage.value
        val trends = resourceTrends.takeLast(50)
        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            systemHealth = health,
            bottlenecks = bottlenecks,
            resourceUsage = resource,
            resourceTrends = trends,
            recommendations = generateRecommendations(health, bottlenecks, resource),
            monitoringDuration = System.currentTimeMillis() - monitoringStartTime,
            diagnosticsOverhead = calculateDiagnosticsOverhead()
        )
    }

    /**
     * Analyze current bottlenecks and return results
     */
    suspend fun analyzeBottlenecks(): List<Bottleneck> {
        return analyzeResourceBottlenecks() + analyzeTrendBottlenecks()
    }

    /**
     * Get component details for specific component
     */
    suspend fun getComponentDetails(component: ComponentType): ComponentHealth? {
        return componentHealth.value[component]
    }

    private fun getSystemInfo(): Map<String, String> {
        return mapOf(
            "platform" to "Android",
            "apiLevel" to android.os.Build.VERSION.SDK_INT.toString(),
            "device" to android.os.Build.MODEL,
            "manufacturer" to android.os.Build.MANUFACTURER
        )
    }

    // =============================================================================
    // RESOURCE MONITORING IMPLEMENTATION
    // =============================================================================

    /**
     * Update resource usage metrics
     */
    private suspend fun updateResourceUsage() {
        val memoryInfo = getMemoryInfo()
        val cpuInfo = getCpuInfo()
        val diskInfo = getDiskInfo()
        
        _resourceUsage.value = ResourceUsageSnapshot(
            timestamp = System.currentTimeMillis(),
            memoryUsed = memoryInfo.used,
            memoryTotal = memoryInfo.total,
            memoryAvailable = memoryInfo.available,
            cpuUsage = cpuInfo.usage,
            diskUsage = diskInfo.usage,
            networkLatency = getNetworkLatency(),
            batteryLevel = getBatteryLevel(),
            threadCount = Thread.activeCount(),
            gcPressure = estimateGcPressure()
        )
        
        // Update resource trends
        resourceTrends.add(_resourceUsage.value)
        if (resourceTrends.size > 100) {
            resourceTrends.removeAt(0)
        }
    }

    private data class MemoryInfo(
        val used: Double,
        val total: Double,
        val available: Double
    )

    private data class CpuInfo(
        val usage: Double
    )

    private data class DiskInfo(
        val usage: Double
    )

    private fun startPerformanceMonitoring() {
        scope.launch {
            while (isMonitoring) {
                try {
                    checkPerformanceMetrics()
                    delay(performanceCheckInterval)
                        } catch (e: Exception) {
            scope.launch {
                loggingProvider.get().logError("Performance monitoring error", e)
            }
                }
            }
        }
    }

    private suspend fun checkPerformanceMetrics() {
        withContext(Dispatchers.Default) {
            // Frame rate analysis
            val frameRate = getFrameRate()
            
            // Network latency check
            val networkLatency = getNetworkLatency()
            
            // Audio buffer analysis
            val audioBufferHealth = getAudioBufferHealth()
            
            // Update performance metrics
            performanceMetrics["frame_rate"] = AtomicLong(frameRate)
            performanceMetrics["network_latency"] = AtomicLong(networkLatency.toLong())
            performanceMetrics["audio_buffer_health"] = AtomicLong(audioBufferHealth)
            
            // Log performance check
            loggingProvider.get().logInfo("Performance check completed", mapOf(
                "frame_rate" to frameRate.toDouble(),
                "network_latency" to networkLatency,
                "audio_buffer_health" to audioBufferHealth.toDouble()
            ))
        }
    }

    private fun getFrameRate(): Long {
        // Simplified frame rate calculation
        return try {
            val choreographer = android.view.Choreographer.getInstance()
            val frameRate = 60L // Default frame rate
            frameRate
        } catch (e: Exception) {
            scope.launch {
                loggingProvider.get().logError("Frame rate calculation error", e)
            }
            0L
        }
    }

    private fun getAudioBufferHealth(): Long {
        // Simplified audio buffer health check
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val bufferSize = android.media.AudioTrack.getMinBufferSize(
                44100,
                android.media.AudioFormat.CHANNEL_OUT_STEREO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            bufferSize.toLong()
        } catch (e: Exception) {
            scope.launch {
                loggingProvider.get().logError("Audio buffer health check error", e)
            }
            0L
        }
    }

    fun observeDiagnosticEvents(): Flow<DiagnosticEvent> = diagnosticEvents.asSharedFlow()
}

sealed class DiagnosticEvent {
    data class ReportGenerated(val report: DiagnosticReport) : DiagnosticEvent()
    data class HealthAlert(val metric: String, val value: Long, val threshold: Long) : DiagnosticEvent()
    data class PerformanceAlert(val metric: String, val value: Long, val threshold: Long) : DiagnosticEvent()
}


