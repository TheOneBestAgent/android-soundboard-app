package com.soundboard.android.diagnostics

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
enum class ComponentType {
    CONNECTION_POOL,
    CACHE,
    COMPRESSION,
    PIPELINE,
    METRICS,
    NETWORK,
    SYSTEM
}

/**
 * Health status levels for components
 */
enum class ComponentStatus {
    HEALTHY,
    DEGRADED,
    CRITICAL,
    OFFLINE
}

/**
 * Severity levels for bottlenecks and issues
 */
enum class Severity(val priority: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4)
}

/**
 * Direction of trends in metrics
 */
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
    UNKNOWN
}

/**
 * Overall health trend assessment
 */
enum class HealthTrend {
    IMPROVING,
    STABLE,
    DEGRADING,
    CRITICAL
}

/**
 * Types of performance bottlenecks
 */
enum class BottleneckType {
    COMPONENT_DEGRADATION,
    MEMORY_PRESSURE,
    CPU_SATURATION,
    NETWORK_LATENCY,
    DISK_IO,
    MEMORY_LEAK,
    THREAD_CONTENTION,
    RESOURCE_EXHAUSTION
}

/**
 * User impact levels
 */
enum class UserImpact {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

// =============================================================================
// LOGGING SYSTEM DATA MODELS
// =============================================================================

/**
 * Log levels for filtering and categorization
 */
enum class LogLevel(val priority: Int) {
    TRACE(1),
    DEBUG(2),
    INFO(3),
    WARN(4),
    ERROR(5)
}

/**
 * Categories for structured logging
 */
enum class LogCategory {
    PERFORMANCE,
    NETWORK,
    CACHE,
    COMPRESSION,
    ERROR,
    USER_ACTION,
    SYSTEM
}

/**
 * Export formats for logs
 */
enum class ExportFormat {
    JSON,
    CSV,
    TEXT,
    XML
}

/**
 * Types of log anomalies
 */
enum class AnomalyType {
    ERROR_SPIKE,
    UNUSUAL_SILENCE,
    PERFORMANCE_DEGRADATION,
    MEMORY_SPIKE,
    NETWORK_ISSUES
}

/**
 * Log event structure for input
 */
data class LogEvent(
    val level: LogLevel,
    val category: LogCategory,
    val message: String,
    val metadata: Map<String, Any>,
    val correlationId: String? = null,
    val component: ComponentType
)

/**
 * Complete log entry with metadata
 */
data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val category: LogCategory,
    val component: ComponentType,
    val message: String,
    val metadata: Map<String, Any>,
    val correlationId: String?,
    val threadName: String,
    val formattedMessage: String
)

/**
 * Log filtering options
 */
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
data class TimeRange(
    val start: Long,
    val end: Long
)

/**
 * Detected log pattern
 */
data class LogPattern(
    val pattern: String,
    val frequency: Int,
    val severity: Severity,
    val suggestedAction: String,
    val firstOccurrence: Long,
    val lastOccurrence: Long,
    val affectedComponents: List<ComponentType>
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

// =============================================================================
// HEALTH MONITORING DATA MODELS
// =============================================================================

/**
 * System health score with detailed breakdown
 */
data class HealthScore(
    val overall: Double,
    val components: Map<ComponentType, Double>,
    val timestamp: Long,
    val factors: List<HealthFactor>,
    val trend: HealthTrend,
    val confidence: Double
) {
    companion object {
        fun initial(): HealthScore = HealthScore(
            overall = 1.0,
            components = ComponentType.values().associateWith { 1.0 },
            timestamp = System.currentTimeMillis(),
            factors = emptyList(),
            trend = HealthTrend.STABLE,
            confidence = 1.0
        )
    }
}

/**
 * Factors contributing to health score
 */
sealed class HealthFactor {
    object CRITICAL_RESOURCE_USAGE : HealthFactor()
    object HIGH_RESOURCE_USAGE : HealthFactor()
    object CRITICAL_PERFORMANCE : HealthFactor()
    object DEGRADED_PERFORMANCE : HealthFactor()
    object NETWORK_ISSUES : HealthFactor()
    object MEMORY_PRESSURE : HealthFactor()
    object CPU_SATURATION : HealthFactor()
    object DISK_ISSUES : HealthFactor()
    
    data class CRITICAL_COMPONENT_HEALTH(val component: ComponentType) : HealthFactor()
    data class WARNING_COMPONENT_HEALTH(val component: ComponentType) : HealthFactor()
}

/**
 * Health data for individual components
 */
data class ComponentHealth(
    val score: Double,
    val status: ComponentStatus,
    val lastUpdated: Long,
    val metrics: Map<String, Any>,
    val issues: List<String>
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
    val timestamp: Long,
    val systemHealth: HealthScore,
    val bottlenecks: List<Bottleneck>,
    val resourceUsage: ResourceUsageSnapshot,
    val resourceTrends: ResourceTrends,
    val recommendations: List<String>,
    val monitoringDuration: Long,
    val diagnosticsOverhead: Double
) 