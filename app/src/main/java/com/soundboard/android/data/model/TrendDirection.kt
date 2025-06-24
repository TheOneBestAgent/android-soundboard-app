package com.soundboard.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Direction of trends in metrics for UI indicators
 */
@Serializable
enum class TrendDirection {
    @SerialName("increasing") INCREASING,
    @SerialName("decreasing") DECREASING,
    @SerialName("stable") STABLE,
    @SerialName("unknown") UNKNOWN
}