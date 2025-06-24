package com.soundboard.android.data.model

import com.soundboard.android.diagnostics.Alert
import com.soundboard.android.diagnostics.AlertEventType
import kotlinx.serialization.Serializable

/**
 * Alert event for monitoring dashboard with resolution tracking
 */
@Serializable
data class AlertEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val alertId: String,
    val type: AlertEventType,
    val alert: Alert,
    val timestamp: Long,
    val resolvedAt: Long? = null,
    val metadata: Map<String, String> = emptyMap()
)