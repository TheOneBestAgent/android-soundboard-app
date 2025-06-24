package com.soundboard.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Overall health trend assessment for UI display
 */
@Serializable
enum class HealthTrend {
    @SerialName("improving") IMPROVING,
    @SerialName("stable") STABLE,
    @SerialName("degrading") DEGRADING,
    @SerialName("critical") CRITICAL
}