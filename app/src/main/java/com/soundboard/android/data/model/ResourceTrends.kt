package com.soundboard.android.data.model

import kotlinx.serialization.Serializable

/**
 * Resource usage trends for dashboard metrics
 */
@Serializable
data class ResourceTrends(
    val memoryTrend: TrendDirection = TrendDirection.STABLE,
    val cpuTrend: TrendDirection = TrendDirection.STABLE,
    val networkTrend: TrendDirection = TrendDirection.STABLE,
    val batteryTrend: TrendDirection = TrendDirection.STABLE,
    val overallTrend: TrendDirection = TrendDirection.STABLE
)