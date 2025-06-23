package com.soundboard.android.diagnostics

import kotlinx.serialization.Serializable

/**
 * Data models for the AlertingSystem
 */

// =============================================================================
// ALERT CORE MODELS
// =============================================================================

/**
 * Types of alerts that can be triggered
 */
enum class AlertType {
    HEALTH_SCORE_LOW,
    MEMORY_HIGH,
    CPU_HIGH,
    NETWORK_LATENCY_HIGH,
    BATTERY_LOW,
    PERFORMANCE_DEGRADATION,
    EFFICIENCY_LOW,
    CONNECTION_FAILURE,
    DISK_SPACE_LOW,
    ERROR_RATE_HIGH,
    RESPONSE_TIME_HIGH,
    CUSTOM
}

/**
 * Alert severity levels
 */
enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Alert status lifecycle
 */
enum class AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    AUTO_RESOLVED,
    SUPPRESSED
}

/**
 * Alert event types for tracking
 */
enum class AlertEventType {
    CREATED,
    UPDATED,
    ACKNOWLEDGED,
    ESCALATED,
    RESOLVED,
    AUTO_RESOLVED,
    SUPPRESSED
}

/**
 * Alert actions for UI updates
 */
enum class AlertAction {
    CREATED,
    ACKNOWLEDGED,
    RESOLVED,
    AUTO_RESOLVED,
    ESCALATED,
    SUPPRESSED
}

// =============================================================================
// ALERT DATA MODELS
// =============================================================================

/**
 * Core alert data structure
 */
@Serializable
data class Alert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val context: Map<String, @Serializable(with = kotlinx.serialization.json.JsonElementSerializer::class) kotlinx.serialization.json.JsonElement>,
    val timestamp: Long,
    val status: AlertStatus,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolution: String? = null,
    val escalatedAt: Long? = null,
    val lastUpdated: Long? = null,
    val occurrenceCount: Int = 1,
    val tags: Set<String> = emptySet()
)

/**
 * Alert threshold configuration
 */
@Serializable
data class AlertThreshold(
    val warningValue: Double,
    val criticalValue: Double,
    val enabled: Boolean = true,
    val suppressionTime: Long = 300_000L, // 5 minutes default
    val escalationTime: Long = 900_000L // 15 minutes default
)

/**
 * Alert event for history tracking
 */
@Serializable
data class AlertEvent(
    val id: String,
    val alertId: String,
    val type: AlertEventType,
    val alert: Alert,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Real-time alert update
 */
@Serializable
data class AlertUpdate(
    val alert: Alert,
    val action: AlertAction,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap()
)

// =============================================================================
// NOTIFICATION MODELS
// =============================================================================

/**
 * Available notification channels
 */
enum class NotificationChannel {
    IN_APP,
    SYSTEM,
    EMAIL,
    SMS,
    WEBHOOK,
    LOG
}

/**
 * Notification configuration
 */
@Serializable
data class NotificationConfig(
    val enabled: Boolean,
    val severityFilter: Set<AlertSeverity>,
    val quietHours: TimeRange? = null,
    val rateLimitPerHour: Int = 10,
    val template: String? = null
)

// TimeRange is defined in DiagnosticsModels.kt - using that definition

// =============================================================================
// SYSTEM STATUS MODELS
// =============================================================================

/**
 * Overall alerting system status
 */
@Serializable
data class AlertingSystemStatus(
    val isMonitoring: Boolean = false,
    val activeAlertCount: Int = 0,
    val totalAlertsToday: Int = 0,
    val lastAlertTime: Long? = null,
    val systemHealth: Double = 1.0, // 0.0 to 1.0
    val uptime: Long = 0L,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

/**
 * Alert statistics and analytics
 */
@Serializable
data class AlertStatistics(
    val totalActiveAlerts: Int,
    val totalAlertsLast24h: Int,
    val totalAlertsLast7d: Int,
    val alertsByType: Map<AlertType, Int>,
    val alertsBySeverity: Map<AlertSeverity, Int>,
    val averageResolutionTime: Long, // milliseconds
    val topAlertTypes: List<Pair<AlertType, Int>>,
    val resolutionRate: Double = 0.0, // percentage
    val escalationRate: Double = 0.0  // percentage
)

// =============================================================================
// CONFIGURATION MODELS
// =============================================================================

/**
 * Alert correlation configuration
 */
@Serializable
data class AlertCorrelationConfig(
    val enabled: Boolean = true,
    val timeWindow: Long = 60_000L, // 1 minute
    val similarityThreshold: Double = 0.8,
    val maxCorrelatedAlerts: Int = 5
)

/**
 * Alert escalation policy
 */
@Serializable
data class AlertEscalationPolicy(
    val enabled: Boolean = true,
    val escalationLevels: List<EscalationLevel>,
    val maxEscalations: Int = 3
)

/**
 * Individual escalation level
 */
@Serializable
data class EscalationLevel(
    val level: Int,
    val delayMinutes: Int,
    val notificationChannels: Set<NotificationChannel>,
    val recipients: List<String> = emptyList()
)

/**
 * Maintenance window configuration
 */
@Serializable
data class MaintenanceWindow(
    val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val suppressedAlertTypes: Set<AlertType> = emptySet(),
    val description: String = "",
    val recurring: Boolean = false,
    val recurrencePattern: String? = null
)

// =============================================================================
// ALERTING RULE MODELS
// =============================================================================

/**
 * Custom alerting rule
 */
@Serializable
data class AlertingRule(
    val id: String,
    val name: String,
    val description: String,
    val condition: AlertCondition,
    val severity: AlertSeverity,
    val enabled: Boolean = true,
    val tags: Set<String> = emptySet(),
    val notificationChannels: Set<NotificationChannel> = emptySet()
)

/**
 * Alert condition for custom rules
 */
@Serializable
data class AlertCondition(
    val metric: String,
    val operator: ComparisonOperator,
    val threshold: Double,
    val evaluationWindow: Long = 300_000L, // 5 minutes
    val dataPoints: Int = 1
)

/**
 * Comparison operators for alert conditions
 */
enum class ComparisonOperator {
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    NOT_EQUAL
}

// =============================================================================
// DASHBOARD MODELS
// =============================================================================

/**
 * Alert dashboard summary
 */
@Serializable
data class AlertDashboardSummary(
    val totalActiveAlerts: Int,
    val criticalAlerts: Int,
    val highSeverityAlerts: Int,
    val newAlertsLast24h: Int,
    val resolvedAlertsLast24h: Int,
    val averageResolutionTime: Long,
    val systemHealthScore: Double,
    val topAlertTypes: List<AlertTypeSummary>,
    val alertTrend: List<AlertTrendPoint>
)

/**
 * Alert type summary for dashboard
 */
@Serializable
data class AlertTypeSummary(
    val type: AlertType,
    val count: Int,
    val lastOccurrence: Long?,
    val trend: TrendDirection
)

/**
 * Alert trend data point
 */
@Serializable
data class AlertTrendPoint(
    val timestamp: Long,
    val alertCount: Int,
    val severity: AlertSeverity? = null
)

// =============================================================================
// WEBHOOK MODELS
// =============================================================================

/**
 * Webhook notification payload
 */
@Serializable
data class WebhookPayload(
    val alert: Alert,
    val action: AlertAction,
    val timestamp: Long,
    val systemInfo: Map<String, String> = emptyMap()
)

/**
 * Webhook configuration
 */
@Serializable
data class WebhookConfig(
    val url: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val timeout: Long = 30_000L,
    val retryCount: Int = 3,
    val retryDelay: Long = 5_000L
)

// =============================================================================
// ALERT TEMPLATES
// =============================================================================

/**
 * Alert message template
 */
@Serializable
data class AlertTemplate(
    val id: String,
    val name: String,
    val alertType: AlertType,
    val channel: NotificationChannel,
    val subject: String,
    val body: String,
    val variables: Set<String> = emptySet()
)

/**
 * Template variable for dynamic content
 */
@Serializable
data class TemplateVariable(
    val name: String,
    val description: String,
    val type: VariableType,
    val defaultValue: String? = null
)

/**
 * Template variable types
 */
enum class VariableType {
    STRING,
    NUMBER,
    BOOLEAN,
    TIMESTAMP,
    DURATION
} 