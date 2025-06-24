package com.soundboard.android.diagnostics

/**
 * Common interfaces and types for the diagnostics system
 */

interface DiagnosticsProvider {
    suspend fun getHealthMetric(name: String): Long
    suspend fun getPerformanceMetric(name: String): Long
    suspend fun getCurrentReport(): DiagnosticReport
}

interface LoggingProvider {
    suspend fun logError(message: String, error: Throwable? = null)
    suspend fun logInfo(message: String, metadata: Map<String, Any> = emptyMap())
    suspend fun logEvent(event: LogEvent)
}

interface PerformanceProvider {
    suspend fun getOptimizationStatus(): OptimizationStatus
    suspend fun getCurrentParameters(): Map<String, Any>
}

interface AlertingProvider {
    suspend fun createAlert(
        type: AlertType,
        severity: AlertSeverity,
        message: String,
        context: Map<String, String>
    ): Alert
    suspend fun getActiveAlerts(): List<Alert>
} 