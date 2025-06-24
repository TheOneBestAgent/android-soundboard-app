package com.soundboard.android.data.model

import com.soundboard.android.diagnostics.AlertSeverity
import com.soundboard.android.diagnostics.AlertType
import kotlinx.serialization.Serializable

/**
 * Alert statistics for monitoring dashboard
 */
@Serializable
data class AlertStatistics(
    val totalAlertsLast24h: Int = 0,
    val totalAlertsLast7d: Int = 0,
    val averageResolutionTimeMinutes: Int = 0,
    val criticalAlertCount: Int = 0,
    val warningAlertCount: Int = 0,
    val totalActiveAlerts: Int = 0,
    val alertsByType: Map<AlertType, Int> = emptyMap(),
    val alertsBySeverity: Map<AlertSeverity, Int> = emptyMap(),
    val resolutionRate: Double = 0.0,
    val escalationRate: Double = 0.0,
    val topAlertTypes: List<Pair<AlertType, Int>> = emptyList(),
    val averageResolutionTime: Long = 0L
)