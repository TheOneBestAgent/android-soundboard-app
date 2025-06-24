package com.soundboard.android.diagnostics

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Data models for the DiagnosticsManager component
 * Defines all the core data structures used for system health monitoring,
 * performance analysis, and diagnostic reporting
 */

// =============================================================================
// COMPONENT TYPES AND ENUMS
// =============================================================================

/**
 * Types of system components that can be monitored
 */
@Serializable
enum class ComponentType {
    @SerialName("connection_pool") CONNECTION_POOL,
    @SerialName("cache") CACHE,
    @SerialName("compression") COMPRESSION,
    @SerialName("pipeline") PIPELINE,
    @SerialName("metrics") METRICS,
    @SerialName("network") NETWORK,
    @SerialName("system") SYSTEM
}

/**
 * Health status levels for components
 */
@Serializable
enum class ComponentStatus {
    @SerialName("healthy") HEALTHY,
    @SerialName("degraded") DEGRADED,
    @SerialName("critical") CRITICAL,
    @SerialName("offline") OFFLINE
}

/**
 * Severity levels for bottlenecks and issues
 */
@Serializable
enum class Severity(val priority: Int) {
    @SerialName("low") LOW(1),
    @SerialName("medium") MEDIUM(2),
    @SerialName("high") HIGH(3),
    @SerialName("critical") CRITICAL(4)
}

/**
 * Direction of trends in metrics
 */
@Serializable
enum class TrendDirection {
    @SerialName("improving") IMPROVING,
    @SerialName("decreasing") DECREASING,
    @SerialName("stable") STABLE,
    @SerialName("unknown") UNKNOWN
}

/**
 * Overall health trend assessment
 */
@Serializable
enum class HealthTrend {
    @SerialName("improving") IMPROVING,
    @SerialName("stable") STABLE,
    @SerialName("degrading") DEGRADING,
    @SerialName("critical") CRITICAL
}

/**
 * Types of performance bottlenecks
 */
@Serializable
enum class BottleneckType {
    @SerialName("component_degradation") COMPONENT_DEGRADATION,
    @SerialName("memory_pressure") MEMORY_PRESSURE,
    @SerialName("cpu_saturation") CPU_SATURATION,
    @SerialName("network_latency") NETWORK_LATENCY,
    @SerialName("disk_io") DISK_IO,
    @SerialName("memory_leak") MEMORY_LEAK,
    @SerialName("thread_contention") THREAD_CONTENTION,
    @SerialName("resource_exhaustion") RESOURCE_EXHAUSTION
}

/**
 * User impact levels
 */
@Serializable
enum class UserImpact {
    @SerialName("low") LOW,
    @SerialName("medium") MEDIUM,
    @SerialName("high") HIGH,
    @SerialName("critical") CRITICAL
}

// =============================================================================
// LOGGING SYSTEM DATA MODELS
// =============================================================================

/**
 * Log levels for filtering and categorization
 */
@Serializable
enum class LogLevel(val priority: Int) {
    @SerialName("trace") TRACE(1),
    @SerialName("debug") DEBUG(2),
    @SerialName("info") INFO(3),
    @SerialName("warn") WARN(4),
    @SerialName("error") ERROR(5)
}

/**
 * Categories for structured logging
 */
@Serializable
enum class LogCategory {
    @SerialName("performance") PERFORMANCE,
    @SerialName("network") NETWORK,
    @SerialName("cache") CACHE,
    @SerialName("compression") COMPRESSION,
    @SerialName("error") ERROR,
    @SerialName("user_action") USER_ACTION,
    @SerialName("user_interaction") USER_INTERACTION,
    @SerialName("alert") ALERT,
    @SerialName("system") SYSTEM
}

/**
 * Export formats for logs
 */
@Serializable
enum class ExportFormat {
    @SerialName("json") JSON,
    @SerialName("csv") CSV,
    @SerialName("text") TEXT,
    @SerialName("xml") XML
}

/**
 * Types of log anomalies
 */
@Serializable
enum class AnomalyType {
    @SerialName("error_spike") ERROR_SPIKE,
    @SerialName("unusual_silence") UNUSUAL_SILENCE,
    @SerialName("performance_degradation") PERFORMANCE_DEGRADATION,
    @SerialName("memory_spike") MEMORY_SPIKE,
    @SerialName("network_issues") NETWORK_ISSUES
}

/**
 * Log event structure for input
 */
@Serializable
data class LogEvent(
    val level: LogLevel,
    val category: LogCategory,
    val message: String,
    val metadata: Map<String, String> = emptyMap(),
    val component: ComponentType = ComponentType.SYSTEM,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Complete log entry with metadata
 */
@Serializable
data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val category: LogCategory,
    val component: ComponentType,
    val message: String,
    val metadata: Map<String, String>,
    val correlationId: String?,
    val threadName: String,
    val formattedMessage: String
)

/**
 * Log filtering options
 */
@Serializable
data class LogFilter(
    val level: LogLevel? = null,
    val category: LogCategory? = null,
    val component: ComponentType? = null,
    val correlationId: String? = null,
    val timeRange: TimeRange? = null,
    val searchText: String? = null
)

/**
 * Time range for log filtering
 */
@Serializable
data class TimeRange(
    val start: Long,
    val end: Long
)

/**
 * Detected log pattern
 */
@Serializable
data class LogPattern(
    val pattern: String,
    val category: LogCategory,
    val severity: LogLevel,
    val occurrences: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Log anomaly detection result
 */
data class LogAnomaly(
    val type: AnomalyType,
    val description: String,
    val severity: Severity,
    val timestamp: Long,
    val affectedLogs: Int,
    val suggestedAction: String
)

/**
 * Log statistics summary
 */
data class LogStatistics(
    val totalLogs: Long,
    val logsByLevel: Map<LogLevel, Long>,
    val logsByCategory: Map<LogCategory, Long>,
    val logsByComponent: Map<ComponentType, Long>,
    val lastUpdated: Long
) {
    companion object {
        fun initial(): LogStatistics = LogStatistics(
            totalLogs = 0,
            logsByLevel = LogLevel.values().associateWith { 0L },
            logsByCategory = LogCategory.values().associateWith { 0L },
            logsByComponent = ComponentType.values().associateWith { 0L },
            lastUpdated = System.currentTimeMillis()
        )
    }
}

/**
 * Comprehensive logging report
 */
data class LoggingReport(
    val timestamp: Long,
    val totalLogs: Int,
    val errorCount: Int,
    val warningCount: Int,
    val patterns: List<LogPattern>,
    val anomalies: List<LogAnomaly>,
    val logsByCategory: Map<LogCategory, Int>,
    val logsByComponent: Map<ComponentType, Int>
)

// =============================================================================
// HEALTH MONITORING DATA MODELS
// =============================================================================

/**
 * System health score with detailed breakdown
 */
@Serializable
data class HealthScore(
    val overall: Double,
    val components: Map<ComponentType, Double>,
    val timestamp: Long,
    val factors: List<String>,
    val trend: TrendDirection,
    val confidence: Double
) {
    companion object {
        fun initial() = HealthScore(
            overall = 1.0,
            components = emptyMap(),
            timestamp = System.currentTimeMillis(),
            factors = emptyList(),
            trend = TrendDirection.STABLE,
            confidence = 1.0
        )
    }
}

/**
 * Factors contributing to health score
 */
sealed class HealthFactor {
    data class Critical(val component: ComponentType) : HealthFactor()
    data class High(val component: ComponentType) : HealthFactor()
    data class Medium(val component: ComponentType) : HealthFactor()
}

/**
 * Health data for individual components
 */
@Serializable
data class ComponentHealth(
    val component: ComponentType,
    val score: Double,
    val metrics: Map<String, Double>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Historical health snapshot
 */
data class HealthSnapshot(
    val score: Double,
    val timestamp: Long
)

// =============================================================================
// RESOURCE MONITORING DATA MODELS
// =============================================================================

/**
 * Current resource usage snapshot
 */
data class ResourceUsageSnapshot(
    val timestamp: Long,
    val memoryUsed: Double, // MB
    val memoryTotal: Double, // MB
    val memoryAvailable: Double, // MB
    val cpuUsage: Double, // Percentage
    val networkLatency: Double, // ms
    val batteryLevel: Double, // Percentage
    val diskUsage: Double, // Percentage
    val threadCount: Int,
    val gcPressure: Double // Percentage
) {
    companion object {
        fun initial(): ResourceUsageSnapshot = ResourceUsageSnapshot(
            timestamp = System.currentTimeMillis(),
            memoryUsed = 0.0,
            memoryTotal = 1024.0,
            memoryAvailable = 1024.0,
            cpuUsage = 0.0,
            networkLatency = 0.0,
            batteryLevel = 100.0,
            diskUsage = 0.0,
            threadCount = 1,
            gcPressure = 0.0
        )
    }
}

/**
 * Resource usage trends
 */
data class ResourceTrends(
    val memoryTrend: TrendDirection,
    val cpuTrend: TrendDirection,
    val networkTrend: TrendDirection,
    val batteryTrend: TrendDirection,
    val overallTrend: TrendDirection
)

// =============================================================================
// BOTTLENECK ANALYSIS DATA MODELS
// =============================================================================

/**
 * Performance bottleneck detection result
 */
data class Bottleneck(
    val type: BottleneckType,
    val severity: Severity,
    val component: ComponentType,
    val impact: ImpactAssessment,
    val recommendations: List<String>,
    val timestamp: Long
)

/**
 * Impact assessment for bottlenecks
 */
data class ImpactAssessment(
    val magnitude: Double,
    val affectedComponents: List<ComponentType>,
    val userImpact: UserImpact
)

// =============================================================================
// PERFORMANCE ANALYSIS DATA MODELS
// =============================================================================

/**
 * Performance analysis results from Phase 4.2 integration
 */
data class PerformanceAnalysis(
    val timestamp: Long,
    val connectionPoolAnalysis: String,
    val cacheAnalysis: String,
    val compressionAnalysis: String,
    val pipelineAnalysis: String,
    val overallEfficiency: Double,
    val performanceScore: Double
)

/**
 * Correlation between health and performance metrics
 */
data class CorrelationReport(
    val timestamp: Long,
    val healthScore: Double,
    val performanceScore: Double,
    val correlation: Double,
    val insights: List<String>,
    val recommendations: List<String>
)

// =============================================================================
// DIAGNOSTIC REPORTING DATA MODELS
// =============================================================================

/**
 * Comprehensive diagnostic report
 */
data class DiagnosticReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long,
    val systemHealth: HealthScore,
    val bottlenecks: List<Bottleneck>,
    val resourceUsage: ResourceUsageSnapshot,
    val resourceTrends: List<ResourceUsageSnapshot>,
    val recommendations: List<String>,
    val monitoringDuration: Long,
    val diagnosticsOverhead: Double,
    val performanceMetrics: Map<String, Long> = emptyMap(),
    val healthMetrics: Map<String, Long> = emptyMap()
) 