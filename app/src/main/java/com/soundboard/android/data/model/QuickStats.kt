package com.soundboard.android.data.model

import kotlinx.serialization.Serializable

/**
 * Quick stats for monitoring dashboard
 */
@Serializable
data class QuickStats(
    val uptime: Long,
    val memoryUsedMB: Int,
    val networkLatency: Double,
    val batteryLevel: Double,
    val errorCount: Int
)