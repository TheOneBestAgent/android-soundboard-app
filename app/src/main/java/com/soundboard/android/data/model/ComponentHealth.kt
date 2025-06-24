package com.soundboard.android.data.model

import com.soundboard.android.diagnostics.ComponentType
import com.soundboard.android.diagnostics.ComponentStatus
import kotlinx.serialization.Serializable

/**
 * Health data for individual components in monitoring dashboard
 */
@Serializable
data class ComponentHealth(
    val component: ComponentType,
    val score: Double,
    val status: ComponentStatus = ComponentStatus.HEALTHY,
    val metrics: Map<String, Double> = emptyMap(),
    val lastChecked: Long = System.currentTimeMillis(),
    val trend: TrendDirection = TrendDirection.STABLE
)