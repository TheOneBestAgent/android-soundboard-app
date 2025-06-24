package com.soundboard.android.data.model

import kotlinx.serialization.Serializable

/**
 * Overall system status for monitoring dashboard
 */
@Serializable
data class SystemStatus(
    val activeAlertCount: Int = 0,
    val healthScore: Double = 1.0,
    val healthTrend: HealthTrend = HealthTrend.STABLE,
    val healthFactors: List<String> = emptyList(),
    val isRunningDiagnostics: Boolean = false,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val uptime: Long = 0L,
    val systemHealthy: Boolean = true
)