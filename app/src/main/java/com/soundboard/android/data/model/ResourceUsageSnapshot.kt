package com.soundboard.android.data.model

import kotlinx.serialization.Serializable

/**
 * Resource usage snapshot for monitoring dashboard
 */
@Serializable
data class ResourceUsageSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val memoryUsed: Double,
    val memoryTotal: Double,
    val memoryAvailable: Double = memoryTotal - memoryUsed,
    val cpuUsage: Double,
    val diskUsage: Double = 0.0,
    val networkLatency: Double,
    val batteryLevel: Double,
    val threadCount: Int,
    val gcPressure: Double
)