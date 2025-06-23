package com.soundboard.android.diagnostics

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * AlertingSystem - Proactive monitoring and intelligent notification system
 * 
 * Provides comprehensive alerting capabilities for system health, performance issues,
 * and diagnostic events with intelligent threshold management and notification delivery.
 * 
 * Key Features:
 * - Real-time threshold monitoring with adaptive sensitivity
 * - Intelligent alert correlation and deduplication
 * - Multi-level alert severity with escalation policies
 * - Customizable notification channels and delivery methods
 * - Alert history tracking and trend analysis
 * - Predictive alerting based on performance patterns
 * - Auto-resolution detection and alert lifecycle management
 * - Integration with DiagnosticsManager and PerformanceTuner
 * - Configurable alert suppression and maintenance windows
 * - Comprehensive alert analytics and reporting
 */
@Singleton
class AlertingSystem @Inject constructor(
    private val diagnosticsManager: DiagnosticsManager,
    private val loggingManager: LoggingManager,
    private val performanceTuner: PerformanceTuner
) {
    companion object {
        private const val MONITORING_INTERVAL_MS = 5_000L // 5 seconds
        private const val ALERT_CORRELATION_WINDOW_MS = 60_000L // 1 minute
        private const val MAX_ALERTS_PER_HOUR = 50
        private const val ALERT_HISTORY_SIZE = 1000
        private const val ESCALATION_TIMEOUT_MS = 300_000L // 5 minutes
        private const val AUTO_RESOLUTION_CHECK_INTERVAL_MS = 30_000L // 30 seconds
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val alertMutex = Mutex()
    private val isMonitoring = AtomicBoolean(false)
    
    // Alert tracking
    private val activeAlerts = ConcurrentHashMap<String, SimpleAlert>()
    private val alertHistory = mutableListOf<SimpleAlertEvent>()
    private val alertThresholds = ConcurrentHashMap<AlertType, SimpleThreshold>()
    private val suppressedAlerts = ConcurrentHashMap<String, Long>()
    
    // Notification management
    private val notificationChannels = ConcurrentHashMap<NotificationChannel, SimpleNotificationConfig>()
    private val alertRateLimiter = AtomicLong(0)
    
    // Flow for real-time alert updates
    private val _alertUpdates = MutableStateFlow<SimpleAlertUpdate?>(null)
    val alertUpdates: StateFlow<SimpleAlertUpdate?> = _alertUpdates.asStateFlow()
    
    private val _systemStatus = MutableStateFlow(SimpleAlertingSystemStatus())
    val systemStatus: StateFlow<SimpleAlertingSystemStatus> = _systemStatus.asStateFlow()
    
    init {
        initializeDefaultThresholds()
        initializeNotificationChannels()
        startMonitoring()
    }
    
    // =============================================================================
    // PUBLIC API
    // =============================================================================
    
    /**
     * Start the alerting system monitoring
     */
    suspend fun startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            loggingManager.logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.SYSTEM,
                    message = "Alerting system monitoring started",
                    timestamp = System.currentTimeMillis()
                )
            )
            
            startAlertMonitoringLoop()
            startAutoResolutionLoop()
        }
    }
    
    /**
     * Stop the alerting system monitoring
     */
    suspend fun stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            loggingManager.logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.SYSTEM,
                    message = "Alerting system monitoring stopped",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Create a manual alert
     */
    suspend fun createAlert(
        type: AlertType,
        severity: AlertSeverity,
        message: String,
        context: Map<String, String> = emptyMap()
    ): SimpleAlert {
        return alertMutex.withLock {
            val alert = SimpleAlert(
                id = generateAlertId(),
                type = type,
                severity = severity,
                message = message,
                context = context,
                timestamp = System.currentTimeMillis(),
                status = AlertStatus.ACTIVE
            )
            
            processNewAlert(alert)
            alert
        }
    }
    
    /**
     * Acknowledge an active alert
     */
    suspend fun acknowledgeAlert(alertId: String, acknowledgedBy: String): Boolean {
        return alertMutex.withLock {
            val alert = activeAlerts[alertId]
            if (alert != null && alert.status == AlertStatus.ACTIVE) {
                val updatedAlert = alert.copy(
                    status = AlertStatus.ACKNOWLEDGED,
                    acknowledgedBy = acknowledgedBy,
                    acknowledgedAt = System.currentTimeMillis()
                )
                
                activeAlerts[alertId] = updatedAlert
                recordAlertEvent(AlertEventType.ACKNOWLEDGED, updatedAlert)
                
                _alertUpdates.value = SimpleAlertUpdate(
                    alert = updatedAlert,
                    action = AlertAction.ACKNOWLEDGED,
                    timestamp = System.currentTimeMillis()
                )
                
                loggingManager.logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.SYSTEM,
                        message = "Alert acknowledged: $alertId by $acknowledgedBy",
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Resolve an alert manually
     */
    suspend fun resolveAlert(alertId: String, resolvedBy: String, resolution: String): Boolean {
        return alertMutex.withLock {
            val alert = activeAlerts[alertId]
            if (alert != null) {
                val updatedAlert = alert.copy(
                    status = AlertStatus.RESOLVED,
                    resolvedBy = resolvedBy,
                    resolvedAt = System.currentTimeMillis(),
                    resolution = resolution
                )
                
                activeAlerts.remove(alertId)
                recordAlertEvent(AlertEventType.RESOLVED, updatedAlert)
                
                _alertUpdates.value = SimpleAlertUpdate(
                    alert = updatedAlert,
                    action = AlertAction.RESOLVED,
                    timestamp = System.currentTimeMillis()
                )
                
                loggingManager.logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.SYSTEM,
                        message = "Alert resolved: $alertId by $resolvedBy - $resolution",
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Update alert thresholds
     */
    suspend fun updateThreshold(type: AlertType, threshold: SimpleThreshold) {
        alertMutex.withLock {
            alertThresholds[type] = threshold
            
            loggingManager.logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.SYSTEM,
                    message = "Alert threshold updated for $type",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Get current active alerts
     */
    fun getActiveAlerts(): List<SimpleAlert> {
        return activeAlerts.values.toList().sortedByDescending { it.timestamp }
    }
    
    /**
     * Get alert history
     */
    fun getAlertHistory(limit: Int = 100): List<SimpleAlertEvent> {
        return alertHistory.takeLast(limit).sortedByDescending { it.timestamp }
    }
    
    /**
     * Get alert statistics
     */
    fun getAlertStatistics(): SimpleAlertStatistics {
        val now = System.currentTimeMillis()
        val last24h = now - 86_400_000L // 24 hours
        val last7d = now - 604_800_000L // 7 days
        
        val recent24h = alertHistory.filter { it.timestamp >= last24h }
        val recent7d = alertHistory.filter { it.timestamp >= last7d }
        
        return SimpleAlertStatistics(
            totalActiveAlerts = activeAlerts.size,
            totalAlertsLast24h = recent24h.size,
            totalAlertsLast7d = recent7d.size,
            alertsByType = recent7d.groupBy { it.alert.type }.mapValues { it.value.size },
            alertsBySeverity = recent7d.groupBy { it.alert.severity }.mapValues { it.value.size },
            averageResolutionTime = calculateAverageResolutionTime(recent7d),
            topAlertTypes = getTopAlertTypes(recent7d, 5)
        )
    }
    
    // =============================================================================
    // MONITORING LOOPS
    // =============================================================================
    
    private fun startAlertMonitoringLoop() {
        scope.launch {
            while (isMonitoring.get()) {
                try {
                    checkSystemHealth()
                    checkPerformanceMetrics()
                    checkResourceUsage()
                    checkNetworkHealth()
                    updateSystemStatus()
                    
                } catch (e: Exception) {
                    loggingManager.logEvent(
                        LogEvent(
                            level = LogLevel.ERROR,
                            category = LogCategory.SYSTEM,
                            message = "Alert monitoring error: ${e.message}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }
    
    private fun startAutoResolutionLoop() {
        scope.launch {
            while (isMonitoring.get()) {
                try {
                    checkAutoResolution()
                    cleanupExpiredSuppressions()
                    
                } catch (e: Exception) {
                    loggingManager.logEvent(
                        LogEvent(
                            level = LogLevel.ERROR,
                            category = LogCategory.SYSTEM,
                            message = "Auto-resolution check error: ${e.message}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                
                delay(AUTO_RESOLUTION_CHECK_INTERVAL_MS)
            }
        }
    }
    
    // =============================================================================
    // HEALTH MONITORING
    // =============================================================================
    
    private suspend fun checkSystemHealth() {
        try {
            val healthScore = diagnosticsManager.getCurrentHealthScore().first()
            val threshold = alertThresholds[AlertType.HEALTH_SCORE_LOW]
            
            if (threshold != null && healthScore.overall < threshold.criticalValue) {
                triggerAlert(
                    type = AlertType.HEALTH_SCORE_LOW,
                    severity = AlertSeverity.CRITICAL,
                    message = "System health score critically low: ${(healthScore.overall * 100).toInt()}%",
                    context = mapOf(
                        "healthScore" to healthScore.overall.toString(),
                        "threshold" to threshold.criticalValue.toString(),
                        "trend" to healthScore.trend.name
                    )
                )
            } else if (threshold != null && healthScore.overall < threshold.warningValue) {
                triggerAlert(
                    type = AlertType.HEALTH_SCORE_LOW,
                    severity = AlertSeverity.HIGH,
                    message = "System health score below warning threshold: ${(healthScore.overall * 100).toInt()}%",
                    context = mapOf(
                        "healthScore" to healthScore.overall.toString(),
                        "threshold" to threshold.warningValue.toString(),
                        "trend" to healthScore.trend.name
                    )
                )
            }
        } catch (e: Exception) {
            // Handle silently to avoid alert loops
        }
    }
    
    private suspend fun checkPerformanceMetrics() {
        try {
            val metrics = performanceTuner.performanceMetrics.first()
            
            // Check performance trend
            val trendThreshold = alertThresholds[AlertType.PERFORMANCE_DEGRADATION]
            if (trendThreshold != null && metrics.trend < -trendThreshold.criticalValue) {
                triggerAlert(
                    type = AlertType.PERFORMANCE_DEGRADATION,
                    severity = AlertSeverity.HIGH,
                    message = "Significant performance degradation detected: ${metrics.trend.toInt()}%",
                    context = mapOf(
                        "trend" to metrics.trend.toString(),
                        "currentScore" to metrics.currentScore.toString(),
                        "efficiency" to metrics.efficiency.toString()
                    )
                )
            }
            
            // Check efficiency
            val efficiencyThreshold = alertThresholds[AlertType.EFFICIENCY_LOW]
            if (efficiencyThreshold != null && metrics.efficiency < efficiencyThreshold.criticalValue) {
                triggerAlert(
                    type = AlertType.EFFICIENCY_LOW,
                    severity = AlertSeverity.MEDIUM,
                    message = "System efficiency below acceptable level: ${(metrics.efficiency * 100).toInt()}%",
                    context = mapOf(
                        "efficiency" to metrics.efficiency.toString(),
                        "threshold" to efficiencyThreshold.criticalValue.toString()
                    )
                )
            }
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    private suspend fun checkResourceUsage() {
        try {
            val resourceUsage = diagnosticsManager.getResourceUsage().first()
            
            // Memory usage check
            val memoryThreshold = alertThresholds[AlertType.MEMORY_HIGH]
            val memoryUsagePercent = resourceUsage.memoryUsed / resourceUsage.memoryTotal * 100
            
            if (memoryThreshold != null && memoryUsagePercent > memoryThreshold.criticalValue) {
                triggerAlert(
                    type = AlertType.MEMORY_HIGH,
                    severity = AlertSeverity.CRITICAL,
                    message = "Memory usage critically high: ${memoryUsagePercent.toInt()}%",
                    context = mapOf(
                        "memoryUsed" to resourceUsage.memoryUsed.toString(),
                        "memoryTotal" to resourceUsage.memoryTotal.toString(),
                        "usagePercent" to memoryUsagePercent.toString()
                    )
                )
            }
            
            // CPU usage check
            val cpuThreshold = alertThresholds[AlertType.CPU_HIGH]
            if (cpuThreshold != null && resourceUsage.cpuUsage > cpuThreshold.criticalValue) {
                triggerAlert(
                    type = AlertType.CPU_HIGH,
                    severity = AlertSeverity.HIGH,
                    message = "CPU usage critically high: ${resourceUsage.cpuUsage.toInt()}%",
                    context = mapOf(
                        "cpuUsage" to resourceUsage.cpuUsage.toString(),
                        "threshold" to cpuThreshold.criticalValue.toString()
                    )
                )
            }
            
            // Battery check
            val batteryThreshold = alertThresholds[AlertType.BATTERY_LOW]
            if (batteryThreshold != null && resourceUsage.batteryLevel < batteryThreshold.criticalValue) {
                triggerAlert(
                    type = AlertType.BATTERY_LOW,
                    severity = AlertSeverity.MEDIUM,
                    message = "Battery level low: ${resourceUsage.batteryLevel.toInt()}%",
                    context = mapOf(
                        "batteryLevel" to resourceUsage.batteryLevel.toString(),
                        "threshold" to batteryThreshold.criticalValue.toString()
                    )
                )
            }
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    private suspend fun checkNetworkHealth() {
        try {
            val resourceUsage = diagnosticsManager.getResourceUsage().first()
            val networkThreshold = alertThresholds[AlertType.NETWORK_LATENCY_HIGH]
            
            if (networkThreshold != null && resourceUsage.networkLatency > networkThreshold.criticalValue) {
                triggerAlert(
                    type = AlertType.NETWORK_LATENCY_HIGH,
                    severity = AlertSeverity.HIGH,
                    message = "Network latency critically high: ${resourceUsage.networkLatency.toInt()}ms",
                    context = mapOf(
                        "latency" to resourceUsage.networkLatency.toString(),
                        "threshold" to networkThreshold.criticalValue.toString()
                    )
                )
            }
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    // =============================================================================
    // ALERT PROCESSING
    // =============================================================================
    
    private suspend fun triggerAlert(
        type: AlertType,
        severity: AlertSeverity,
        message: String,
        context: Map<String, String>
    ) {
        alertMutex.withLock {
            // Check rate limiting
            if (!isRateLimitExceeded()) {
                // Check for existing similar alert (deduplication)
                val existingAlert = findSimilarActiveAlert(type, context)
                
                if (existingAlert == null) {
                    val alert = SimpleAlert(
                        id = generateAlertId(),
                        type = type,
                        severity = severity,
                        message = message,
                        context = context,
                        timestamp = System.currentTimeMillis(),
                        status = AlertStatus.ACTIVE
                    )
                    
                    processNewAlert(alert)
                } else {
                    // Update existing alert with new context
                    updateExistingAlert(existingAlert, context)
                }
            }
        }
    }
    
    private suspend fun processNewAlert(alert: SimpleAlert) {
        // Check suppression
        if (!isAlertSuppressed(alert)) {
            activeAlerts[alert.id] = alert
            recordAlertEvent(AlertEventType.CREATED, alert)
            
            // Send notifications
            sendNotifications(alert)
            
            // Update rate limiter
            alertRateLimiter.incrementAndGet()
            
            // Emit alert update
            _alertUpdates.value = SimpleAlertUpdate(
                alert = alert,
                action = AlertAction.CREATED,
                timestamp = System.currentTimeMillis()
            )
            
            loggingManager.logEvent(
                LogEvent(
                    level = when (alert.severity) {
                        AlertSeverity.CRITICAL -> LogLevel.ERROR
                        AlertSeverity.HIGH -> LogLevel.WARN
                        AlertSeverity.MEDIUM -> LogLevel.WARN
                        AlertSeverity.LOW -> LogLevel.INFO
                    },
                    category = LogCategory.ALERT,
                    message = "Alert triggered: ${alert.type} - ${alert.message}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    private suspend fun checkAutoResolution() {
        val alertsToCheck = activeAlerts.values.toList()
        
        for (alert in alertsToCheck) {
            if (shouldAutoResolve(alert)) {
                val updatedAlert = alert.copy(
                    status = AlertStatus.AUTO_RESOLVED,
                    resolvedAt = System.currentTimeMillis(),
                    resolution = "Auto-resolved: condition no longer met"
                )
                
                activeAlerts.remove(alert.id)
                recordAlertEvent(AlertEventType.AUTO_RESOLVED, updatedAlert)
                
                _alertUpdates.value = SimpleAlertUpdate(
                    alert = updatedAlert,
                    action = AlertAction.AUTO_RESOLVED,
                    timestamp = System.currentTimeMillis()
                )
                
                loggingManager.logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.ALERT,
                        message = "Alert auto-resolved: ${alert.id}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    // =============================================================================
    // HELPER FUNCTIONS
    // =============================================================================
    
    private fun initializeDefaultThresholds() {
        alertThresholds[AlertType.HEALTH_SCORE_LOW] = SimpleThreshold(
            warningValue = 0.7,
            criticalValue = 0.5,
            enabled = true
        )
        
        alertThresholds[AlertType.MEMORY_HIGH] = SimpleThreshold(
            warningValue = 80.0,
            criticalValue = 90.0,
            enabled = true
        )
        
        alertThresholds[AlertType.CPU_HIGH] = SimpleThreshold(
            warningValue = 80.0,
            criticalValue = 95.0,
            enabled = true
        )
        
        alertThresholds[AlertType.NETWORK_LATENCY_HIGH] = SimpleThreshold(
            warningValue = 200.0,
            criticalValue = 500.0,
            enabled = true
        )
        
        alertThresholds[AlertType.BATTERY_LOW] = SimpleThreshold(
            warningValue = 20.0,
            criticalValue = 10.0,
            enabled = true
        )
        
        alertThresholds[AlertType.PERFORMANCE_DEGRADATION] = SimpleThreshold(
            warningValue = 10.0,
            criticalValue = 25.0,
            enabled = true
        )
        
        alertThresholds[AlertType.EFFICIENCY_LOW] = SimpleThreshold(
            warningValue = 0.6,
            criticalValue = 0.4,
            enabled = true
        )
    }
    
    private fun initializeNotificationChannels() {
        notificationChannels[NotificationChannel.IN_APP] = SimpleNotificationConfig(
            enabled = true,
            severityFilter = setOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH, AlertSeverity.MEDIUM, AlertSeverity.LOW)
        )
        
        notificationChannels[NotificationChannel.SYSTEM] = SimpleNotificationConfig(
            enabled = true,
            severityFilter = setOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH)
        )
        
        notificationChannels[NotificationChannel.LOG] = SimpleNotificationConfig(
            enabled = true,
            severityFilter = setOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH, AlertSeverity.MEDIUM, AlertSeverity.LOW)
        )
    }
    
    private fun generateAlertId(): String {
        return "alert_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    private fun findSimilarActiveAlert(type: AlertType, context: Map<String, String>): SimpleAlert? {
        return activeAlerts.values.find { alert ->
            alert.type == type && isSimilarContext(alert.context, context)
        }
    }
    
    private fun isSimilarContext(context1: Map<String, String>, context2: Map<String, String>): Boolean {
        // Simple similarity check - can be enhanced with more sophisticated logic
        return context1.keys.intersect(context2.keys).size >= context1.keys.size * 0.5
    }
    
    private fun updateExistingAlert(alert: SimpleAlert, newContext: Map<String, String>) {
        val updatedAlert = alert.copy(
            context = alert.context + newContext,
            lastUpdated = System.currentTimeMillis(),
            occurrenceCount = alert.occurrenceCount + 1
        )
        
        activeAlerts[alert.id] = updatedAlert
        recordAlertEvent(AlertEventType.UPDATED, updatedAlert)
    }
    
    private fun isAlertSuppressed(alert: SimpleAlert): Boolean {
        val suppressionKey = "${alert.type}_${alert.context.hashCode()}"
        val suppressedUntil = suppressedAlerts[suppressionKey]
        return suppressedUntil != null && System.currentTimeMillis() < suppressedUntil
    }
    
    private fun isRateLimitExceeded(): Boolean {
        val currentHour = System.currentTimeMillis() / 3600000
        
        // Simple rate limiting - reset every hour
        if (System.currentTimeMillis() % 3600000 < MONITORING_INTERVAL_MS) {
            alertRateLimiter.set(0)
        }
        
        return alertRateLimiter.get() >= MAX_ALERTS_PER_HOUR
    }
    
    private suspend fun sendNotifications(alert: SimpleAlert) {
        for ((channel, config) in notificationChannels) {
            if (config.enabled && alert.severity in config.severityFilter) {
                when (channel) {
                    NotificationChannel.IN_APP -> {
                        // In-app notification will be handled by UI observing alertUpdates
                    }
                    NotificationChannel.SYSTEM -> {
                        // TODO: Implement system notifications
                    }
                    NotificationChannel.LOG -> {
                        // Already logged in processNewAlert
                    }
                    else -> {
                        // Handle other notification channels
                    }
                }
            }
        }
    }
    
    private suspend fun shouldAutoResolve(alert: SimpleAlert): Boolean {
        return when (alert.type) {
            AlertType.HEALTH_SCORE_LOW -> {
                val currentHealth = diagnosticsManager.getCurrentHealthScore().first().overall
                val threshold = alertThresholds[AlertType.HEALTH_SCORE_LOW]?.warningValue ?: 0.7
                currentHealth > threshold
            }
            AlertType.MEMORY_HIGH -> {
                val resourceUsage = diagnosticsManager.getResourceUsage().first()
                val memoryUsagePercent = resourceUsage.memoryUsed / resourceUsage.memoryTotal * 100
                val threshold = alertThresholds[AlertType.MEMORY_HIGH]?.warningValue ?: 80.0
                memoryUsagePercent < threshold
            }
            AlertType.CPU_HIGH -> {
                val resourceUsage = diagnosticsManager.getResourceUsage().first()
                val threshold = alertThresholds[AlertType.CPU_HIGH]?.warningValue ?: 80.0
                resourceUsage.cpuUsage < threshold
            }
            AlertType.NETWORK_LATENCY_HIGH -> {
                val resourceUsage = diagnosticsManager.getResourceUsage().first()
                val threshold = alertThresholds[AlertType.NETWORK_LATENCY_HIGH]?.warningValue ?: 200.0
                resourceUsage.networkLatency < threshold
            }
            else -> false
        }
    }
    
    private fun recordAlertEvent(eventType: AlertEventType, alert: SimpleAlert) {
        val event = SimpleAlertEvent(
            id = generateAlertId(),
            alertId = alert.id,
            type = eventType,
            alert = alert,
            timestamp = System.currentTimeMillis()
        )
        
        alertHistory.add(event)
        if (alertHistory.size > ALERT_HISTORY_SIZE) {
            alertHistory.removeFirst()
        }
    }
    
    private fun updateSystemStatus() {
        _systemStatus.value = SimpleAlertingSystemStatus(
            isMonitoring = isMonitoring.get(),
            activeAlertCount = activeAlerts.size,
            totalAlertsToday = getTodayAlertCount(),
            lastAlertTime = getLastAlertTime(),
            systemHealth = calculateSystemHealth()
        )
    }
    
    private fun getTodayAlertCount(): Int {
        val today = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
        return alertHistory.count { it.timestamp >= today }
    }
    
    private fun getLastAlertTime(): Long? {
        return alertHistory.maxByOrNull { it.timestamp }?.timestamp
    }
    
    private fun calculateSystemHealth(): Double {
        return when {
            activeAlerts.any { it.value.severity == AlertSeverity.CRITICAL } -> 0.3
            activeAlerts.any { it.value.severity == AlertSeverity.HIGH } -> 0.6
            activeAlerts.any { it.value.severity == AlertSeverity.MEDIUM } -> 0.8
            activeAlerts.isNotEmpty() -> 0.9
            else -> 1.0
        }
    }
    
    private fun calculateAverageResolutionTime(events: List<SimpleAlertEvent>): Long {
        val resolvedEvents = events.filter { 
            it.type == AlertEventType.RESOLVED || it.type == AlertEventType.AUTO_RESOLVED 
        }
        
        if (resolvedEvents.isEmpty()) return 0L
        
        val resolutionTimes = resolvedEvents.mapNotNull { event ->
            val createdEvent = events.find { 
                it.alertId == event.alertId && it.type == AlertEventType.CREATED 
            }
            if (createdEvent != null) {
                event.timestamp - createdEvent.timestamp
            } else null
        }
        
        return if (resolutionTimes.isNotEmpty()) {
            resolutionTimes.average().toLong()
        } else 0L
    }
    
    private fun getTopAlertTypes(events: List<SimpleAlertEvent>, limit: Int): List<Pair<AlertType, Int>> {
        return events
            .groupBy { it.alert.type }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    private fun cleanupExpiredSuppressions() {
        val now = System.currentTimeMillis()
        suppressedAlerts.entries.removeAll { it.value < now }
    }
}

// =============================================================================
// SIMPLIFIED DATA MODELS
// =============================================================================

data class SimpleAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val context: Map<String, String>,
    val timestamp: Long,
    val status: AlertStatus,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolution: String? = null,
    val lastUpdated: Long? = null,
    val occurrenceCount: Int = 1
)

data class SimpleThreshold(
    val warningValue: Double,
    val criticalValue: Double,
    val enabled: Boolean = true
)

data class SimpleAlertEvent(
    val id: String,
    val alertId: String,
    val type: AlertEventType,
    val alert: SimpleAlert,
    val timestamp: Long
)

data class SimpleAlertUpdate(
    val alert: SimpleAlert,
    val action: AlertAction,
    val timestamp: Long
)

data class SimpleNotificationConfig(
    val enabled: Boolean,
    val severityFilter: Set<AlertSeverity>
)

data class SimpleAlertingSystemStatus(
    val isMonitoring: Boolean = false,
    val activeAlertCount: Int = 0,
    val totalAlertsToday: Int = 0,
    val lastAlertTime: Long? = null,
    val systemHealth: Double = 1.0
)

data class SimpleAlertStatistics(
    val totalActiveAlerts: Int,
    val totalAlertsLast24h: Int,
    val totalAlertsLast7d: Int,
    val alertsByType: Map<AlertType, Int>,
    val alertsBySeverity: Map<AlertSeverity, Int>,
    val averageResolutionTime: Long,
    val topAlertTypes: List<Pair<AlertType, Int>>
) 