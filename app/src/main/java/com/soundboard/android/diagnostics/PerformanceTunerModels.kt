package com.soundboard.android.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

/**
 * Data models for the PerformanceTuner system
 */

// =============================================================================
// OPTIMIZATION TYPES AND ENUMS
// =============================================================================

/**
 * Types of optimizations that can be performed
 */
enum class OptimizationType {
    MEMORY_OPTIMIZATION,
    CPU_OPTIMIZATION,
    NETWORK_OPTIMIZATION,
    CACHE_OPTIMIZATION,
    CONNECTION_POOL_OPTIMIZATION,
    COMPRESSION_OPTIMIZATION
}

/**
 * Priority levels for optimizations
 */
enum class OptimizationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Status of optimization execution
 */
enum class OptimizationExecutionStatus {
    PENDING,
    RUNNING,
    APPLIED,
    FAILED,
    ROLLED_BACK
}

/**
 * Status of optimization state
 */
enum class OptimizationStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

// =============================================================================
// PERFORMANCE MEASUREMENT MODELS
// =============================================================================

/**
 * Snapshot of system performance at a specific time
 */
@Serializable
data class PerformanceSnapshot(
    val timestamp: Long,
    val metrics: Map<String, Double>,
    @Contextual val resourceUsage: ResourceUsageSnapshot,
    val healthScore: Double,
    val componentScores: Map<String, Double>,
    val bottleneckCount: Int,
    val criticalBottlenecks: Int,
    val responseTime: Double,
    val throughput: Double,
    val errorRate: Double,
    val bottlenecks: List<String> = emptyList()
)

/**
 * Baseline performance metrics for comparison
 */
@Serializable
data class PerformanceBaseline(
    val timestamp: Long,
    val averageHealthScore: Double,
    val averageResponseTime: Double,
    val averageThroughput: Double,
    val averageErrorRate: Double,
    val resourceUtilization: Double
)

/**
 * Current performance metrics
 */
@Serializable
data class PerformanceMetrics(
    val currentScore: Double = 0.0,
    val trend: Double = 0.0,
    val efficiency: Double = 0.0,
    val stability: Double = 0.0,
    val lastUpdate: Long = System.currentTimeMillis(),
    val optimizationCount: Int = 0,
    val activeOptimizations: Int = 0
)

// =============================================================================
// OPTIMIZATION RECOMMENDATION MODELS
// =============================================================================

/**
 * Recommendation for a specific optimization
 */
@Serializable
data class OptimizationRecommendation(
    val type: OptimizationType,
    val priority: OptimizationPriority,
    val confidence: Double, // 0.0 to 1.0
    val expectedImprovement: Double, // Percentage improvement expected
    val parameters: Map<String, String>,
    val description: String = "",
    val rationale: String = ""
)

/**
 * Result of applying an optimization
 */
@Serializable
data class OptimizationExecution(
    val type: OptimizationType,
    val success: Boolean,
    val improvementPercentage: Double,
    val confidence: Double,
    val timestamp: Long,
    val beforeMetrics: PerformanceSnapshot? = null,
    val afterMetrics: PerformanceSnapshot? = null,
    val error: String? = null
)

/**
 * Overall optimization result containing multiple executions
 */
@Serializable
data class OptimizationResult(
    val timestamp: Long,
    val optimizations: List<OptimizationExecution>,
    val overallImprovement: Double,
    val confidence: Double
)

// =============================================================================
// OPTIMIZATION STATE MANAGEMENT
// =============================================================================

/**
 * Current state of an optimization
 */
@Serializable
data class OptimizationState(
    val type: OptimizationType,
    val startTime: Long,
    val parameters: Map<String, String>,
    val expectedImprovement: Double,
    val status: OptimizationStatus = OptimizationStatus.RUNNING
)

/**
 * Profile containing predefined optimization strategies
 */
@Serializable
data class OptimizationProfile(
    val name: String,
    val description: String,
    val optimizations: List<OptimizationRecommendation>
)

/**
 * Status information for the optimization system
 */
@Serializable
data class OptimizationSystemStatus(
    val isRunning: Boolean,
    val activeOptimizations: List<OptimizationState>,
    val lastOptimization: OptimizationResult?,
    val baseline: PerformanceBaseline?,
    val currentMetrics: PerformanceMetrics
)

/**
 * Real-time update about optimization progress
 */
@Serializable
data class OptimizationUpdate(
    val type: OptimizationType,
    val status: OptimizationExecutionStatus,
    val improvement: Double,
    val timestamp: Long,
    val message: String = ""
)

// =============================================================================
// ANALYSIS AND REPORTING MODELS
// =============================================================================

/**
 * Analysis of current performance bottlenecks
 */
@Serializable
data class PerformanceAnalysisResult(
    val timestamp: Long,
    val overallScore: Double,
    val bottlenecks: List<PerformanceBottleneck>,
    val recommendations: List<OptimizationRecommendation>,
    val trendAnalysis: TrendAnalysis
)

/**
 * Specific performance bottleneck identified
 */
@Serializable
data class PerformanceBottleneck(
    val component: String,
    val type: String,
    val severity: Severity,
    val impact: Double,
    val description: String,
    val suggestedOptimizations: List<OptimizationType>
)

/**
 * Analysis of performance trends over time
 */
@Serializable
data class TrendAnalysis(
    val shortTermTrend: Double, // Last hour
    val mediumTermTrend: Double, // Last day
    val longTermTrend: Double, // Last week
    val volatility: Double,
    val prediction: TrendPrediction
)

/**
 * Prediction of future performance trends
 */
@Serializable
data class TrendPrediction(
    val direction: TrendDirection,
    val confidence: Double,
    val timeToThreshold: Long?, // Time until critical threshold reached
    val recommendedActions: List<String>
)

// =============================================================================
// CONFIGURATION MODELS
// =============================================================================

/**
 * Configuration for the performance tuner
 */
@Serializable
data class PerformanceTunerConfig(
    val enabled: Boolean = true,
    val optimizationInterval: Long = 30_000L,
    val maxConcurrentOptimizations: Int = 3,
    val minConfidence: Double = 0.7,
    val rollbackThreshold: Double = 0.85,
    val profiles: Map<String, OptimizationProfile> = emptyMap()
)

/**
 * Thresholds for triggering optimizations
 */
@Serializable
data class OptimizationThresholds(
    val memoryUsageThreshold: Double = 80.0,
    val cpuUsageThreshold: Double = 75.0,
    val networkLatencyThreshold: Double = 100.0,
    val errorRateThreshold: Double = 5.0,
    val responseTimeThreshold: Double = 1000.0
)

// =============================================================================
// REPORTING MODELS
// =============================================================================

/**
 * Comprehensive optimization report
 */
@Serializable
data class OptimizationReport(
    val timestamp: Long,
    val timeRange: OptimizationTimeRange,
    val summary: OptimizationSummary,
    val detailedResults: List<OptimizationResult>,
    val performanceImpact: PerformanceImpact,
    val recommendations: List<OptimizationRecommendation>
)

/**
 * Time range for reports
 */
@Serializable
data class OptimizationTimeRange(
    val startTime: Long,
    val endTime: Long,
    val description: String
)

/**
 * Summary of optimization activities
 */
@Serializable
data class OptimizationSummary(
    val totalOptimizations: Int,
    val successfulOptimizations: Int,
    val averageImprovement: Double,
    val totalImprovement: Double,
    val mostEffectiveOptimization: OptimizationType?,
    val leastEffectiveOptimization: OptimizationType?
)

/**
 * Impact of optimizations on performance
 */
@Serializable
data class PerformanceImpact(
    val beforeBaseline: PerformanceBaseline,
    val afterBaseline: PerformanceBaseline,
    val improvementPercentage: Double,
    val impactByCategory: Map<String, Double>,
    val regressions: List<String>
) 