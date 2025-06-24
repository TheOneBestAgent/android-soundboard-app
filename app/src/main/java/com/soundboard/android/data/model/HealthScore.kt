package com.soundboard.android.data.model

import com.soundboard.android.diagnostics.ComponentType
import kotlinx.serialization.Serializable

/**
 * Health score data for monitoring dashboard
 */
@Serializable
data class HealthScore(
    val overall: Double,
    val components: Map<ComponentType, Double> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val factors: List<String> = emptyList(),
    val trend: com.soundboard.android.diagnostics.TrendDirection = com.soundboard.android.diagnostics.TrendDirection.STABLE,
    val confidence: Double = 1.0
)