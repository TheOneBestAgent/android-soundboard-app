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
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.2: Performance Metrics & Analytics
 * 
 * Comprehensive performance monitoring, analytics, and optimization recommendations
 * for all network operations and system components.
 */
@Singleton
class PerformanceMetrics @Inject constructor() {
    
    companion object {
        private const val TAG = "PerformanceMetrics"
        private const val METRICS_COLLECTION_INTERVAL_MS = 5_000L // 5 seconds
        private const val METRICS_RETENTION_WINDOW_MS = 3600_000L // 1 hour
        private const val PERFORMANCE_THRESHOLD_WARNING = 0.7 // 70%
        private const val PERFORMANCE_THRESHOLD_CRITICAL = 0.9 // 90%
        private val LATENCY_PERCENTILES = listOf(50.0, 90.0, 95.0, 99.0)
    }
    
    // Metric categories
    enum class MetricCategory(val description: String) {
        NETWORK("Network Performance"),
        CACHE("Cache Performance"),
        COMPRESSION("Compression Performance"),
        PIPELINE("Request Pipeline Performance"),
        CONNECTION("Connection Pool Performance"),
        OVERALL("Overall System Performance")
    }
    
    // Performance metric data point
    data class MetricDataPoint(
        val timestamp: Long,
        val category: MetricCategory,
        val name: String,
        val value: Double,
        val unit: String,
        val tags: Map<String, String> = emptyMap()
    )
    
    // Aggregated metric statistics
    data class MetricStatistics(
        val category: MetricCategory,
        val name: String,
        val unit: String,
        val count: Long,
        val sum: Double,
        val average: Double,
        val minimum: Double,
        val maximum: Double,
        val percentiles: Map<Double, Double>, // percentile -> value
        val standardDeviation: Double,
        val trend: TrendDirection,
        val lastUpdated: Long
    )
    
    enum class TrendDirection {
        IMPROVING,
        STABLE,
        DEGRADING,
        UNKNOWN
    }
    
    // Performance alert
    data class PerformanceAlert(
        val alertId: String,
        val severity: AlertSeverity,
        val category: MetricCategory,
        val metricName: String,
        val threshold: Double,
        val actualValue: Double,
        val message: String,
        val timestamp: Long,
        val acknowledged: Boolean = false
    )
    
    enum class AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    // Performance recommendation
    data class PerformanceRecommendation(
        val recommendationId: String,
        val category: MetricCategory,
        val priority: RecommendationPriority,
        val title: String,
        val description: String,
        val expectedImprovement: String,
        val implementationComplexity: ComplexityLevel,
        val relatedMetrics: List<String>,
        val timestamp: Long
    )
    
    enum class RecommendationPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    enum class ComplexityLevel {
        SIMPLE,
        MODERATE,
        COMPLEX,
        MAJOR
    }
    
    // System health status
    data class SystemHealthStatus(
        val overallHealth: HealthLevel,
        val networkHealth: HealthLevel,
        val cacheHealth: HealthLevel,
        val compressionHealth: HealthLevel,
        val pipelineHealth: HealthLevel,
        val connectionHealth: HealthLevel,
        val healthScore: Double, // 0.0 to 1.0
        val activeAlerts: Int,
        val criticalAlerts: Int,
        val lastChecked: Long
    )
    
    enum class HealthLevel {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        CRITICAL
    }
    
    // Performance dashboard data
    data class PerformanceDashboard(
        val systemHealth: SystemHealthStatus,
        val keyMetrics: Map<MetricCategory, List<MetricStatistics>>,
        val activeAlerts: List<PerformanceAlert>,
        val recommendations: List<PerformanceRecommendation>,
        val trendAnalysis: Map<String, TrendDirection>,
        val efficiencyScores: Map<MetricCategory, Double>,
        val lastUpdated: Long
    )
    
    // State management
    private val metricsMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Data storage
    private val metricsHistory = ConcurrentHashMap<String, ArrayDeque<MetricDataPoint>>()
    private val currentStatistics = ConcurrentHashMap<String, MetricStatistics>()
    private val activeAlerts = ConcurrentHashMap<String, PerformanceAlert>()
    private val recommendations = ConcurrentHashMap<String, PerformanceRecommendation>()
    private val metricThresholds = ConcurrentHashMap<String, Pair<Double, Double>>() // warning, critical
    
    // Performance counters
    private val metricCounts = ConcurrentHashMap<String, AtomicLong>()
    private val metricSums = ConcurrentHashMap<String, Double>()
    
    // State flows for monitoring
    private val _performanceDashboard = MutableStateFlow(
        PerformanceDashboard(
            systemHealth = SystemHealthStatus(
                overallHealth = HealthLevel.GOOD,
                networkHealth = HealthLevel.GOOD,
                cacheHealth = HealthLevel.GOOD,
                compressionHealth = HealthLevel.GOOD,
                pipelineHealth = HealthLevel.GOOD,
                connectionHealth = HealthLevel.GOOD,
                healthScore = 0.8,
                activeAlerts = 0,
                criticalAlerts = 0,
                lastChecked = System.currentTimeMillis()
            ),
            keyMetrics = emptyMap(),
            activeAlerts = emptyList(),
            recommendations = emptyList(),
            trendAnalysis = emptyMap(),
            efficiencyScores = emptyMap(),
            lastUpdated = System.currentTimeMillis()
        )
    )
    val performanceDashboard: StateFlow<PerformanceDashboard> = _performanceDashboard.asStateFlow()
    
    private val _systemHealth = MutableStateFlow(HealthLevel.GOOD)
    val systemHealth: StateFlow<HealthLevel> = _systemHealth.asStateFlow()
    
    /**
     * Initialize the performance metrics system
     */
    fun initialize() {
        scope.launch {
            metricsMutex.withLock {
                initializeDefaultThresholds()
                startMetricsCollection()
                startAlertsProcessor()
                startRecommendationEngine()
            }
        }
        Log.i(TAG, "Performance metrics system initialized")
    }
    
    /**
     * Record a metric data point
     */
    suspend fun recordMetric(
        category: MetricCategory,
        name: String,
        value: Double,
        unit: String,
        tags: Map<String, String> = emptyMap()
    ) {
        val metricKey = "${category.name}_$name"
        val dataPoint = MetricDataPoint(
            timestamp = System.currentTimeMillis(),
            category = category,
            name = name,
            value = value,
            unit = unit,
            tags = tags
        )
        
        metricsMutex.withLock {
            // Store in history
            val history = metricsHistory.getOrPut(metricKey) { ArrayDeque() }
            history.offer(dataPoint)
            
            // Cleanup old data
            val cutoffTime = System.currentTimeMillis() - METRICS_RETENTION_WINDOW_MS
            while (history.isNotEmpty() && history.peek().timestamp < cutoffTime) {
                history.poll()
            }
            
            // Update counters
            metricCounts.getOrPut(metricKey) { AtomicLong(0) }.incrementAndGet()
            metricSums[metricKey] = (metricSums[metricKey] ?: 0.0) + value
            
            // Update statistics
            updateMetricStatistics(metricKey, history.toList())
            
            // Check for alerts
            checkMetricThresholds(metricKey, value)
        }
        
        Log.v(TAG, "Recorded metric: ${category.name}.$name = $value $unit")
    }
    
    /**
     * Get statistics for a specific metric
     */
    fun getMetricStatistics(category: MetricCategory, name: String): MetricStatistics? {
        val metricKey = "${category.name}_$name"
        return currentStatistics[metricKey]
    }
    
    /**
     * Get all statistics for a category
     */
    fun getCategoryStatistics(category: MetricCategory): List<MetricStatistics> {
        return currentStatistics.values.filter { it.category == category }
    }
    
    /**
     * Set threshold values for a metric
     */
    suspend fun setMetricThreshold(
        category: MetricCategory,
        name: String,
        warningThreshold: Double,
        criticalThreshold: Double
    ) {
        val metricKey = "${category.name}_$name"
        metricsMutex.withLock {
            metricThresholds[metricKey] = warningThreshold to criticalThreshold
        }
        Log.d(TAG, "Set thresholds for $metricKey: warning=$warningThreshold, critical=$criticalThreshold")
    }
    
    /**
     * Get active alerts
     */
    fun getActiveAlerts(): List<PerformanceAlert> {
        return activeAlerts.values.filter { !it.acknowledged }
    }
    
    /**
     * Get active recommendations
     */
    fun getActiveRecommendations(): List<PerformanceRecommendation> {
        return recommendations.values.toList()
    }
    
    /**
     * Acknowledge an alert
     */
    suspend fun acknowledgeAlert(alertId: String) {
        metricsMutex.withLock {
            activeAlerts[alertId]?.let { alert ->
                activeAlerts[alertId] = alert.copy(acknowledged = true)
                Log.d(TAG, "Acknowledged alert: $alertId")
            }
        }
    }
    
    /**
     * Get current system health status
     */
    fun getCurrentSystemHealth(): SystemHealthStatus {
        return _performanceDashboard.value.systemHealth
    }
    
    /**
     * Force metrics analysis and recommendation generation
     */
    suspend fun forceAnalysis() {
        scope.launch {
            analyzeSystemPerformance()
            generateRecommendations()
            updateDashboard()
        }
    }
    
    /**
     * Export metrics data for external analysis
     */
    fun exportMetricsData(
        category: MetricCategory? = null,
        startTime: Long = System.currentTimeMillis() - METRICS_RETENTION_WINDOW_MS,
        endTime: Long = System.currentTimeMillis()
    ): List<MetricDataPoint> {
        return metricsHistory.values
            .flatten()
            .filter { dataPoint ->
                dataPoint.timestamp in startTime..endTime &&
                (category == null || dataPoint.category == category)
            }
            .sortedBy { it.timestamp }
    }
    
    /**
     * Clear metrics history
     */
    suspend fun clearMetricsHistory() {
        metricsMutex.withLock {
            metricsHistory.clear()
            currentStatistics.clear()
            activeAlerts.clear()
            recommendations.clear()
            metricCounts.clear()
            metricSums.clear()
            Log.i(TAG, "Cleared all metrics history")
        }
    }
    
    // Private helper methods
    
    private fun initializeDefaultThresholds() {
        // Network metrics
        setDefaultThreshold(MetricCategory.NETWORK, "latency_ms", 500.0, 1000.0)
        setDefaultThreshold(MetricCategory.NETWORK, "error_rate", 0.05, 0.1)
        setDefaultThreshold(MetricCategory.NETWORK, "timeout_rate", 0.02, 0.05)
        
        // Cache metrics
        setDefaultThreshold(MetricCategory.CACHE, "hit_rate", 0.8, 0.6)
        setDefaultThreshold(MetricCategory.CACHE, "memory_usage", 0.8, 0.9)
        setDefaultThreshold(MetricCategory.CACHE, "eviction_rate", 0.1, 0.2)
        
        // Compression metrics
        setDefaultThreshold(MetricCategory.COMPRESSION, "compression_ratio", 0.7, 0.9)
        setDefaultThreshold(MetricCategory.COMPRESSION, "compression_time_ms", 100.0, 500.0)
        
        // Pipeline metrics
        setDefaultThreshold(MetricCategory.PIPELINE, "queue_size", 50.0, 100.0)
        setDefaultThreshold(MetricCategory.PIPELINE, "throughput_rps", 10.0, 5.0)
        setDefaultThreshold(MetricCategory.PIPELINE, "failure_rate", 0.05, 0.1)
        
        // Connection metrics
        setDefaultThreshold(MetricCategory.CONNECTION, "pool_utilization", 0.8, 0.95)
        setDefaultThreshold(MetricCategory.CONNECTION, "connection_errors", 0.02, 0.05)
    }
    
    private fun setDefaultThreshold(category: MetricCategory, name: String, warning: Double, critical: Double) {
        val metricKey = "${category.name}_$name"
        metricThresholds[metricKey] = warning to critical
    }
    
    private fun startMetricsCollection() {
        scope.launch {
            while (true) {
                try {
                    collectSystemMetrics()
                    kotlinx.coroutines.delay(METRICS_COLLECTION_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting metrics", e)
                }
            }
        }
    }
    
    private fun startAlertsProcessor() {
        scope.launch {
            while (true) {
                try {
                    processExpiredAlerts()
                    kotlinx.coroutines.delay(60_000L) // Check every minute
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing alerts", e)
                }
            }
        }
    }
    
    private fun startRecommendationEngine() {
        scope.launch {
            while (true) {
                try {
                    analyzeSystemPerformance()
                    generateRecommendations()
                    updateDashboard()
                    kotlinx.coroutines.delay(METRICS_COLLECTION_INTERVAL_MS * 6) // Every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in recommendation engine", e)
                }
            }
        }
    }
    
    private suspend fun collectSystemMetrics() {
        // Collect basic system metrics
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsage = usedMemory.toDouble() / totalMemory
        
        recordMetric(
            category = MetricCategory.OVERALL,
            name = "memory_usage",
            value = memoryUsage,
            unit = "percentage"
        )
        
        recordMetric(
            category = MetricCategory.OVERALL,
            name = "used_memory_mb",
            value = usedMemory.toDouble() / (1024 * 1024),
            unit = "megabytes"
        )
    }
    
    private fun updateMetricStatistics(metricKey: String, dataPoints: List<MetricDataPoint>) {
        if (dataPoints.isEmpty()) return
        
        val values = dataPoints.map { it.value }.sorted()
        val count = values.size.toLong()
        val sum = values.sum()
        val average = sum / count
        val minimum = values.minOrNull() ?: 0.0
        val maximum = values.maxOrNull() ?: 0.0
        
        // Calculate percentiles
        val percentiles = LATENCY_PERCENTILES.associateWith { percentile ->
            calculatePercentile(values, percentile)
        }
        
        // Calculate standard deviation
        val variance = values.map { (it - average) * (it - average) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        // Determine trend
        val trend = calculateTrend(dataPoints)
        
        val firstDataPoint = dataPoints.first()
        val statistics = MetricStatistics(
            category = firstDataPoint.category,
            name = firstDataPoint.name,
            unit = firstDataPoint.unit,
            count = count,
            sum = sum,
            average = average,
            minimum = minimum,
            maximum = maximum,
            percentiles = percentiles,
            standardDeviation = standardDeviation,
            trend = trend,
            lastUpdated = System.currentTimeMillis()
        )
        
        currentStatistics[metricKey] = statistics
    }
    
    private fun calculatePercentile(sortedValues: List<Double>, percentile: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        
        val index = (percentile / 100.0 * (sortedValues.size - 1)).toInt()
        return sortedValues[index.coerceIn(0, sortedValues.size - 1)]
    }
    
    private fun calculateTrend(dataPoints: List<MetricDataPoint>): TrendDirection {
        if (dataPoints.size < 10) return TrendDirection.UNKNOWN
        
        val recentPoints = dataPoints.takeLast(10)
        val olderPoints = dataPoints.takeLast(20).take(10)
        
        if (olderPoints.isEmpty()) return TrendDirection.UNKNOWN
        
        val recentAverage = recentPoints.map { it.value }.average()
        val olderAverage = olderPoints.map { it.value }.average()
        
        val changeRatio = (recentAverage - olderAverage) / olderAverage
        
        return when {
            changeRatio > 0.05 -> TrendDirection.DEGRADING // Assuming higher values are worse
            changeRatio < -0.05 -> TrendDirection.IMPROVING
            else -> TrendDirection.STABLE
        }
    }
    
    private suspend fun checkMetricThresholds(metricKey: String, value: Double) {
        val thresholds = metricThresholds[metricKey] ?: return
        val (warningThreshold, criticalThreshold) = thresholds
        
        val severity = when {
            value >= criticalThreshold -> AlertSeverity.CRITICAL
            value >= warningThreshold -> AlertSeverity.WARNING
            else -> return // No alert needed
        }
        
        val alertId = "${metricKey}_${System.currentTimeMillis()}"
        val alert = PerformanceAlert(
            alertId = alertId,
            severity = severity,
            category = MetricCategory.values().find { metricKey.startsWith(it.name) } ?: MetricCategory.OVERALL,
            metricName = metricKey.substringAfter("_"),
            threshold = if (severity == AlertSeverity.CRITICAL) criticalThreshold else warningThreshold,
            actualValue = value,
            message = "Metric $metricKey exceeded ${severity.name.lowercase()} threshold: $value vs ${if (severity == AlertSeverity.CRITICAL) criticalThreshold else warningThreshold}",
            timestamp = System.currentTimeMillis()
        )
        
        activeAlerts[alertId] = alert
        Log.w(TAG, "Performance alert: ${alert.message}")
    }
    
    private fun processExpiredAlerts() {
        val cutoffTime = System.currentTimeMillis() - 300_000L // 5 minutes
        val expiredAlerts = activeAlerts.values.filter { it.timestamp < cutoffTime }
        
        expiredAlerts.forEach { alert ->
            activeAlerts.remove(alert.alertId)
        }
        
        if (expiredAlerts.isNotEmpty()) {
            Log.d(TAG, "Removed ${expiredAlerts.size} expired alerts")
        }
    }
    
    private suspend fun analyzeSystemPerformance() {
        // Calculate health levels for each category
        val networkHealth = calculateCategoryHealth(MetricCategory.NETWORK)
        val cacheHealth = calculateCategoryHealth(MetricCategory.CACHE)
        val compressionHealth = calculateCategoryHealth(MetricCategory.COMPRESSION)
        val pipelineHealth = calculateCategoryHealth(MetricCategory.PIPELINE)
        val connectionHealth = calculateCategoryHealth(MetricCategory.CONNECTION)
        
        // Calculate overall health
        val healthLevels = listOf(networkHealth, cacheHealth, compressionHealth, pipelineHealth, connectionHealth)
        val overallHealth = healthLevels.minByOrNull { it.ordinal } ?: HealthLevel.GOOD
        
        // Calculate health score
        val healthScore = healthLevels.map { healthLevelToScore(it) }.average()
        
        // Count alerts
        val alerts = activeAlerts.values.filter { !it.acknowledged }
        val criticalAlerts = alerts.count { it.severity == AlertSeverity.CRITICAL }
        
        val systemHealth = SystemHealthStatus(
            overallHealth = overallHealth,
            networkHealth = networkHealth,
            cacheHealth = cacheHealth,
            compressionHealth = compressionHealth,
            pipelineHealth = pipelineHealth,
            connectionHealth = connectionHealth,
            healthScore = healthScore,
            activeAlerts = alerts.size,
            criticalAlerts = criticalAlerts,
            lastChecked = System.currentTimeMillis()
        )
        
        _systemHealth.value = overallHealth
    }
    
    private fun calculateCategoryHealth(category: MetricCategory): HealthLevel {
        val categoryStats = getCategoryStatistics(category)
        if (categoryStats.isEmpty()) return HealthLevel.GOOD
        
        // Simple health calculation based on trends and alert presence
        val degradingTrends = categoryStats.count { it.trend == TrendDirection.DEGRADING }
        val categoryAlerts = activeAlerts.values.count { it.category == category && !it.acknowledged }
        val criticalCategoryAlerts = activeAlerts.values.count { 
            it.category == category && it.severity == AlertSeverity.CRITICAL && !it.acknowledged 
        }
        
        return when {
            criticalCategoryAlerts > 0 -> HealthLevel.CRITICAL
            categoryAlerts > 2 -> HealthLevel.POOR
            degradingTrends > categoryStats.size / 2 -> HealthLevel.FAIR
            else -> HealthLevel.GOOD
        }
    }
    
    private fun healthLevelToScore(healthLevel: HealthLevel): Double {
        return when (healthLevel) {
            HealthLevel.EXCELLENT -> 1.0
            HealthLevel.GOOD -> 0.8
            HealthLevel.FAIR -> 0.6
            HealthLevel.POOR -> 0.4
            HealthLevel.CRITICAL -> 0.2
        }
    }
    
    private suspend fun generateRecommendations() {
        // Generate recommendations based on current metrics and trends
        val newRecommendations = mutableListOf<PerformanceRecommendation>()
        
        // Check cache performance
        val cacheStats = getCategoryStatistics(MetricCategory.CACHE)
        cacheStats.forEach { stat ->
            if (stat.name == "hit_rate" && stat.average < 0.7) {
                newRecommendations.add(
                    PerformanceRecommendation(
                        recommendationId = "cache_hit_rate_${System.currentTimeMillis()}",
                        category = MetricCategory.CACHE,
                        priority = RecommendationPriority.HIGH,
                        title = "Improve Cache Hit Rate",
                        description = "Cache hit rate is ${String.format("%.1f", stat.average * 100)}%. Consider increasing cache size or improving cache strategies.",
                        expectedImprovement = "10-20% reduction in response times",
                        implementationComplexity = ComplexityLevel.MODERATE,
                        relatedMetrics = listOf("cache_hit_rate", "response_time"),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        
        // Update recommendations
        metricsMutex.withLock {
            newRecommendations.forEach { recommendation ->
                recommendations[recommendation.recommendationId] = recommendation
            }
            
            // Remove old recommendations (older than 1 hour)
            val cutoffTime = System.currentTimeMillis() - 3600_000L
            val expiredRecommendations = recommendations.values.filter { it.timestamp < cutoffTime }
            expiredRecommendations.forEach { recommendation ->
                recommendations.remove(recommendation.recommendationId)
            }
        }
    }
    
    private suspend fun updateDashboard() {
        val systemHealth = getCurrentSystemHealth()
        val keyMetrics = MetricCategory.values().associateWith { category ->
            getCategoryStatistics(category)
        }
        val alerts = getActiveAlerts()
        val recs = getActiveRecommendations()
        
        val trendAnalysis = currentStatistics.mapValues { it.value.trend }
        val efficiencyScores = MetricCategory.values().associateWith { category ->
            healthLevelToScore(calculateCategoryHealth(category))
        }
        
        _performanceDashboard.value = PerformanceDashboard(
            systemHealth = systemHealth,
            keyMetrics = keyMetrics,
            activeAlerts = alerts,
            recommendations = recs,
            trendAnalysis = trendAnalysis,
            efficiencyScores = efficiencyScores,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
