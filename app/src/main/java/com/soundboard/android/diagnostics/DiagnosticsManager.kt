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
    @ApplicationContext private val context: Context
) {
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

    /**
     * Initialize diagnostic monitoring
     */
    suspend fun initialize() {
        scope.launch {
            startHealthMonitoring()
            startResourceMonitoring()
            startBottleneckDetection()
        }
    }

    /**
     * Shutdown diagnostic monitoring and cleanup resources
     */
    suspend fun shutdown() {
        isMonitoring = false
        monitoringJob?.cancel()
        scope.cancel()
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
        val resourceUsage = getResourceUsage()
        val trends = getResourceTrends()
        
        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            systemHealth = healthScore,
            bottlenecks = bottlenecks,
            resourceUsage = resourceUsage,
            resourceTrends = trends,
            recommendations = generateRecommendations(healthScore, bottlenecks, resourceUsage),
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
     * Get current resource usage snapshot
     */
    suspend fun getResourceUsage(): ResourceUsageSnapshot {
        val runtime = Runtime.getRuntime()
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
        } catch (e: Exception) {
            // Fallback to basic memory info
        }
        
        val resourceUsage = ResourceUsageSnapshot(
            timestamp = System.currentTimeMillis(),
            memoryUsed = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / 1024 / 1024, // MB
            memoryTotal = runtime.totalMemory().toDouble() / 1024 / 1024, // MB
            memoryAvailable = memoryInfo.availMem.toDouble() / 1024 / 1024, // MB
            cpuUsage = estimateCpuUsage(),
            networkLatency = estimateNetworkLatency(),
            batteryLevel = getBatteryLevel(),
            diskUsage = estimateDiskUsage(),
            threadCount = Thread.activeCount(),
            gcPressure = estimateGcPressure()
        )
        
        _resourceUsage.value = resourceUsage
        addToResourceTrends(resourceUsage)
        
        return resourceUsage
    }

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
                    getResourceUsage()
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
        val currentUsage = _resourceUsage.value
        
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
    ): List<HealthFactor> {
        val factors = mutableListOf<HealthFactor>()
        
        // Check component health factors
        componentHealth.forEach { (component, health) ->
            when {
                health.score < CRITICAL_HEALTH_THRESHOLD -> {
                    factors.add(HealthFactor.CRITICAL_COMPONENT_HEALTH(component))
                }
                health.score < WARNING_HEALTH_THRESHOLD -> {
                    factors.add(HealthFactor.WARNING_COMPONENT_HEALTH(component))
                }
            }
        }
        
        // Check resource health factors
        if (resourceHealth < CRITICAL_HEALTH_THRESHOLD) {
            factors.add(HealthFactor.CRITICAL_RESOURCE_USAGE)
        } else if (resourceHealth < WARNING_HEALTH_THRESHOLD) {
            factors.add(HealthFactor.HIGH_RESOURCE_USAGE)
        }
        
        // Check performance health factors
        if (performanceHealth < CRITICAL_HEALTH_THRESHOLD) {
            factors.add(HealthFactor.CRITICAL_PERFORMANCE)
        } else if (performanceHealth < WARNING_HEALTH_THRESHOLD) {
            factors.add(HealthFactor.DEGRADED_PERFORMANCE)
        }
        
        return factors
    }

    private fun calculateHealthTrend(): HealthTrend {
        // Analyze recent health scores to determine trend
        return HealthTrend.STABLE // Placeholder
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
            score = 0.9,
            status = ComponentStatus.HEALTHY,
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "activeConnections" to 5,
                "successRate" to 0.98,
                "avgLatency" to 45.0
            ),
            issues = emptyList()
        )
    }

    private suspend fun monitorCacheHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 CacheManager
        return ComponentHealth(
            score = 0.85,
            status = ComponentStatus.HEALTHY,
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "hitRate" to 0.82,
                "memoryUsage" to 65.0,
                "evictionRate" to 0.05
            ),
            issues = emptyList()
        )
    }

    private suspend fun monitorCompressionHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 CompressionManager
        return ComponentHealth(
            score = 0.92,
            status = ComponentStatus.HEALTHY,
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "compressionRatio" to 0.7,
                "processingTime" to 12.0,
                "bandwidthSavings" to 0.3
            ),
            issues = emptyList()
        )
    }

    private suspend fun monitorPipelineHealth(): ComponentHealth {
        // Will integrate with Phase 4.2 RequestPipelineManager
        return ComponentHealth(
            score = 0.88,
            status = ComponentStatus.HEALTHY,
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "throughput" to 150.0,
                "queueSize" to 3,
                "errorRate" to 0.02
            ),
            issues = emptyList()
        )
    }

    private suspend fun monitorMetricsHealth(): ComponentHealth {
        // Monitor the PerformanceMetrics component itself
        return ComponentHealth(
            score = 0.95,
            status = ComponentStatus.HEALTHY,
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "dataAccuracy" to 0.99,
                "processingLatency" to 8.0,
                "storageUsage" to 12.0
            ),
            issues = emptyList()
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
            score = score,
            status = if (score > 0.7) ComponentStatus.HEALTHY else ComponentStatus.DEGRADED,
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "latency" to latency,
                "availability" to 0.99,
                "bandwidth" to 25.0
            ),
            issues = if (score < 0.7) listOf("High network latency detected") else emptyList()
        )
    }

    private suspend fun monitorSystemHealth(): ComponentHealth {
        val resourceUsage = getResourceUsage()
        val memoryHealth = 1.0 - (resourceUsage.memoryUsed / resourceUsage.memoryTotal)
        val cpuHealth = 1.0 - (resourceUsage.cpuUsage / 100.0)
        val overallHealth = (memoryHealth + cpuHealth) / 2.0
        
        return ComponentHealth(
            score = overallHealth,
            status = when {
                overallHealth > 0.8 -> ComponentStatus.HEALTHY
                overallHealth > 0.6 -> ComponentStatus.DEGRADED
                else -> ComponentStatus.CRITICAL
            },
            lastUpdated = System.currentTimeMillis(),
            metrics = mapOf(
                "memoryUsage" to resourceUsage.memoryUsed,
                "cpuUsage" to resourceUsage.cpuUsage,
                "threadCount" to resourceUsage.threadCount
            ),
            issues = if (overallHealth < 0.6) listOf("High system resource usage") else emptyList()
        )
    }

    // =============================================================================
    // BOTTLENECK ANALYSIS
    // =============================================================================

    private suspend fun analyzeComponentBottlenecks(component: ComponentType): List<Bottleneck> {
        val health = monitorComponent(component)
        val bottlenecks = mutableListOf<Bottleneck>()
        
        if (health.score < CRITICAL_HEALTH_THRESHOLD) {
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
        val usage = getResourceUsage()
        val bottlenecks = mutableListOf<Bottleneck>()
        
        // Memory bottleneck
        if (usage.memoryUsed / usage.memoryTotal > 0.9) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.MEMORY_PRESSURE,
                    severity = Severity.CRITICAL,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = usage.memoryUsed / usage.memoryTotal,
                        affectedComponents = ComponentType.values().toList(),
                        userImpact = UserImpact.HIGH
                    ),
                    recommendations = listOf(
                        "Clear cache", "Reduce memory usage", "Enable compression"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // CPU bottleneck
        if (usage.cpuUsage > 80) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.CPU_SATURATION,
                    severity = Severity.HIGH,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = usage.cpuUsage / 100.0,
                        affectedComponents = listOf(ComponentType.PIPELINE, ComponentType.COMPRESSION),
                        userImpact = UserImpact.MEDIUM
                    ),
                    recommendations = listOf(
                        "Reduce processing intensity", "Optimize algorithms", "Use background threads"
                    ),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        return bottlenecks
    }

    private suspend fun analyzeTrendBottlenecks(): List<Bottleneck> {
        val trends = getResourceTrends()
        val bottlenecks = mutableListOf<Bottleneck>()
        
        if (trends.memoryTrend == TrendDirection.INCREASING) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.MEMORY_LEAK,
                    severity = Severity.MEDIUM,
                    component = ComponentType.SYSTEM,
                    impact = ImpactAssessment(
                        magnitude = 0.5,
                        affectedComponents = ComponentType.values().toList(),
                        userImpact = UserImpact.MEDIUM
                    ),
                    recommendations = listOf(
                        "Monitor for memory leaks", "Implement memory cleanup", "Profile memory usage"
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
            recentAvg > olderAvg * 1.1 -> TrendDirection.INCREASING
            recentAvg < olderAvg * 0.9 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun calculateOverallResourceTrend(trends: List<ResourceUsageSnapshot>): TrendDirection {
        if (trends.size < 6) return TrendDirection.STABLE
        
        val memoryTrend = calculateTrend(trends.map { it.memoryUsed })
        val cpuTrend = calculateTrend(trends.map { it.cpuUsage })
        val networkTrend = calculateTrend(trends.map { it.networkLatency })
        
        val increasingCount = listOf(memoryTrend, cpuTrend, networkTrend).count { it == TrendDirection.INCREASING }
        val decreasingCount = listOf(memoryTrend, cpuTrend, networkTrend).count { it == TrendDirection.DECREASING }
        
        return when {
            increasingCount >= 2 -> TrendDirection.INCREASING
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
        }
    }

    private fun generateRecommendations(
        healthScore: HealthScore,
        bottlenecks: List<Bottleneck>,
        resourceUsage: ResourceUsageSnapshot
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (healthScore.overall < CRITICAL_HEALTH_THRESHOLD) {
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
    private fun estimateCpuUsage(): Double = 25.0
    private fun estimateNetworkLatency(): Double = 45.0
    private fun getBatteryLevel(): Double = 75.0
    private fun estimateDiskUsage(): Double = 60.0
    private fun estimateGcPressure(): Double = 15.0

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
    fun getCurrentHealthScore(): Flow<HealthScore> = healthScore.asStateFlow()

    /**
     * Get component health status as Flow for reactive monitoring
     */
    fun getComponentHealth(): Flow<Map<ComponentType, ComponentHealth>> = flow {
        val health = _currentHealth.value
        emit(health.components)
    }

    /**
     * Get active bottlenecks as Flow for reactive monitoring
     */
    fun getActiveBottlenecks(): Flow<List<Bottleneck>> = flow {
        emit(analyzeBottlenecks())
    }

    /**
     * Perform comprehensive health check and return detailed results
     */
    suspend fun performHealthCheck(): DiagnosticReport {
        val health = performComprehensiveHealthCheck()
        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            healthScore = health.overallScore,
            componentHealth = health.components,
            bottlenecks = analyzeBottlenecks(),
            resourceUsage = getCurrentResourceUsage(),
            recommendations = generateRecommendations(health.overallScore, analyzeBottlenecks(), getCurrentResourceUsage()),
            systemInfo = getSystemInfo()
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
        return _currentHealth.value.components[component]
    }

    /**
     * Generate comprehensive diagnostic report
     */
    suspend fun generateReport(): DiagnosticReport {
        return performHealthCheck()
    }

    private fun getSystemInfo(): Map<String, String> {
        return mapOf(
            "platform" to "Android",
            "apiLevel" to android.os.Build.VERSION.SDK_INT.toString(),
            "device" to android.os.Build.MODEL,
            "manufacturer" to android.os.Build.MANUFACTURER
        )
    }
}
