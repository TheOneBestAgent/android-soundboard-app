package com.soundboard.android.diagnostics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
import dagger.Lazy

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
    @ApplicationContext private val context: Context,
    private val diagnosticsProvider: Lazy<DiagnosticsProvider>,
    private val loggingProvider: Lazy<LoggingProvider>,
    private val performanceProvider: Lazy<PerformanceProvider>
) : AlertingProvider {
    companion object {
        private const val MONITORING_INTERVAL_MS = 5_000L // 5 seconds
        private const val ALERT_CORRELATION_WINDOW_MS = 60_000L // 1 minute
        private const val MAX_ALERTS_PER_HOUR = 50
        private const val ALERT_HISTORY_SIZE = 1000
        private const val ESCALATION_TIMEOUT_MS = 300_000L // 5 minutes
        private const val AUTO_RESOLUTION_CHECK_INTERVAL_MS = 30_000L // 30 seconds
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val alertMutex = Mutex()
    private val isMonitoring = AtomicBoolean(false)
    
    // Alert tracking
    private val activeAlerts = ConcurrentHashMap<String, Alert>()
    private val alertHistory = mutableListOf<AlertEvent>()
    private val alertThresholds = ConcurrentHashMap<AlertType, AlertThreshold>()
    private val suppressedAlerts = ConcurrentHashMap<String, Long>()
    
    // Notification management
    private val notificationChannels = ConcurrentHashMap<NotificationChannel, NotificationConfig>()
    private val alertRateLimiter = AtomicLong(0)
    
    // Flow for real-time alert updates
    private val _alertUpdates = MutableStateFlow<AlertUpdate?>(null)
    val alertUpdates: StateFlow<AlertUpdate?> = _alertUpdates.asStateFlow()
    
    private val _systemStatus = MutableStateFlow(AlertingSystemStatus())
    val systemStatus: StateFlow<AlertingSystemStatus> = _systemStatus.asStateFlow()
    
    init {
        initializeDefaultThresholds()
        initializeNotificationChannels()
        scope.launch {
            startMonitoring()
        }
    }
    
    // =============================================================================
    // AlertingProvider Implementation
    // =============================================================================

    override suspend fun createAlert(
        type: AlertType,
        severity: AlertSeverity,
        message: String,
        context: Map<String, String>
    ): Alert {
        return alertMutex.withLock {
            val alert = Alert(
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

    override suspend fun getActiveAlerts(): List<Alert> {
        return activeAlerts.values.filter { it.status == AlertStatus.ACTIVE }.toList()
    }
    
    // =============================================================================
    // PUBLIC API
    // =============================================================================
    
    /**
     * Start the alerting system monitoring
     */
    suspend fun startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            loggingProvider.get().logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.ALERT,
                    message = "Alerting system monitoring started",
                    metadata = mapOf("system" to "alerting"),
                    component = ComponentType.SYSTEM
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
            loggingProvider.get().logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.ALERT,
                    message = "Alerting system monitoring stopped",
                    metadata = mapOf("system" to "alerting"),
                    component = ComponentType.SYSTEM
                )
            )
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
                
                _alertUpdates.value = AlertUpdate(
                    alert = updatedAlert,
                    action = AlertAction.ACKNOWLEDGED,
                    timestamp = System.currentTimeMillis()
                )
                
                loggingProvider.get().logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.ALERT,
                        message = "Alert acknowledged: $alertId by $acknowledgedBy",
                        metadata = mapOf("alertId" to alertId, "acknowledgedBy" to acknowledgedBy),
                        component = ComponentType.SYSTEM
                    )
                )
                
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Resolve an active or acknowledged alert
     */
    suspend fun resolveAlert(alertId: String, resolvedBy: String, resolution: String): Boolean {
        return alertMutex.withLock {
            val alert = activeAlerts[alertId]
            if (alert != null && (alert.status == AlertStatus.ACTIVE || alert.status == AlertStatus.ACKNOWLEDGED)) {
                val updatedAlert = alert.copy(
                    status = AlertStatus.RESOLVED,
                    resolvedBy = resolvedBy,
                    resolvedAt = System.currentTimeMillis(),
                    resolution = resolution
                )
                
                activeAlerts[alertId] = updatedAlert
                recordAlertEvent(AlertEventType.RESOLVED, updatedAlert)
                
                _alertUpdates.value = AlertUpdate(
                    alert = updatedAlert,
                    action = AlertAction.RESOLVED,
                    timestamp = System.currentTimeMillis()
                )
                
                loggingProvider.get().logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.ALERT,
                        message = "Alert resolved: $alertId by $resolvedBy",
                        metadata = mapOf(
                            "alertId" to alertId,
                            "resolvedBy" to resolvedBy,
                            "resolution" to resolution
                        ),
                        component = ComponentType.SYSTEM
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
    suspend fun updateThreshold(type: AlertType, threshold: AlertThreshold) {
        alertMutex.withLock {
            alertThresholds[type] = threshold
            
            loggingProvider.get().logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.ALERT,
                    message = "Alert threshold updated for $type",
                    metadata = mapOf("alertType" to type.toString(), "threshold" to threshold.toString()),
                    component = ComponentType.SYSTEM
                )
            )
        }
    }
    
    /**
     * Get alert history
     */
    fun getAlertHistory(limit: Int = 100): List<AlertEvent> {
        return alertHistory.takeLast(limit).sortedByDescending { it.timestamp }
    }
    
    /**
     * Get alert statistics
     */
    fun getAlertStatistics(): AlertStatistics {
        val now = System.currentTimeMillis()
        val last24h = now - 86_400_000L // 24 hours
        val last7d = now - 604_800_000L // 7 days
        
        val recent24h = alertHistory.filter { it.timestamp >= last24h }
        val recent7d = alertHistory.filter { it.timestamp >= last7d }
        
        return AlertStatistics(
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
                    loggingProvider.get().logError("Alert monitoring error", e)
                }
                
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }
    
    private fun startAutoResolutionLoop() {
        scope.launch {
            while (isMonitoring.get()) {
                try {
                    // Check each active alert for auto-resolution
                    val alertsToCheck = activeAlerts.values.filter { 
                        it.status == AlertStatus.ACTIVE || it.status == AlertStatus.ACKNOWLEDGED 
                    }
                    
                    for (alert in alertsToCheck) {
                        if (shouldAutoResolve(alert)) {
                            val updatedAlert = alert.copy(
                                status = AlertStatus.AUTO_RESOLVED,
                                resolvedAt = System.currentTimeMillis(),
                                resolution = "Auto-resolved: Condition no longer met"
                            )
                            
                            activeAlerts[alert.id] = updatedAlert
                            recordAlertEvent(AlertEventType.AUTO_RESOLVED, updatedAlert)
                            
                            _alertUpdates.value = AlertUpdate(
                                alert = updatedAlert,
                                action = AlertAction.AUTO_RESOLVED,
                                timestamp = System.currentTimeMillis()
                            )
                            
                            loggingProvider.get().logEvent(
                                LogEvent(
                                    level = LogLevel.INFO,
                                    category = LogCategory.ALERT,
                                    message = "Alert auto-resolved: ${alert.id}",
                                    metadata = mapOf("alertId" to alert.id, "alertType" to alert.type.toString()),
                                    component = ComponentType.SYSTEM
                                )
                            )
                        }
                    }
                    
                    // Cleanup expired suppressions
                    cleanupExpiredSuppressions()
                    
                } catch (e: Exception) {
                    loggingProvider.get().logError("Auto-resolution error", e)
                }
                
                delay(AUTO_RESOLUTION_CHECK_INTERVAL_MS)
            }
        }
    }
    
    // =============================================================================
    // HEALTH MONITORING
    // =============================================================================
    
    private suspend fun checkSystemHealth() {
        val healthScore = diagnosticsProvider.get().getCurrentReport().systemHealth.overall
        
        if (healthScore < 0.5) {
            triggerAlert(
                type = AlertType.HEALTH_SCORE_LOW,
                severity = if (healthScore < 0.3) AlertSeverity.CRITICAL else AlertSeverity.HIGH,
                message = "System health score is critically low: ${(healthScore * 100).toInt()}%",
                context = mapOf("healthScore" to healthScore.toString())
            )
        }
    }
    
    private suspend fun checkPerformanceMetrics() {
        val performanceMetrics = diagnosticsProvider.get().getCurrentReport().performanceMetrics
        val performanceScore = performanceMetrics["performance_score"] ?: 100L
        
        if (performanceScore < 60) {
            triggerAlert(
                type = AlertType.PERFORMANCE_DEGRADATION,
                severity = if (performanceScore < 40) AlertSeverity.CRITICAL else AlertSeverity.HIGH,
                message = "System performance degradation detected",
                context = mapOf(
                    "performanceScore" to performanceScore.toString(),
                    "frameRate" to (performanceMetrics["frame_rate"] ?: 0L).toString(),
                    "networkLatency" to (performanceMetrics["network_latency"] ?: 0L).toString(),
                    "audioBufferHealth" to (performanceMetrics["audio_buffer_health"] ?: 0L).toString()
                )
            )
        }
    }
    
    private suspend fun checkResourceUsage() {
        val resourceUsage = diagnosticsProvider.get().getCurrentReport().resourceUsage
        
        checkMemoryUsage(resourceUsage)
        checkCpuUsage(resourceUsage)
        checkBatteryLevel(resourceUsage)
        checkNetworkLatency(resourceUsage)
    }
    
    private suspend fun checkMemoryUsage(resourceUsage: ResourceUsageSnapshot) {
        val memoryUsagePercent = (resourceUsage.memoryUsed / resourceUsage.memoryTotal) * 100
        
        when {
            memoryUsagePercent >= 90 -> {
                createAlert(
                    type = AlertType.MEMORY_HIGH,
                    severity = AlertSeverity.CRITICAL,
                    message = "Critical memory usage: ${String.format("%.1f", memoryUsagePercent)}%",
                    context = mapOf(
                        "memoryUsed" to "${String.format("%.1f", resourceUsage.memoryUsed)}MB",
                        "memoryTotal" to "${String.format("%.1f", resourceUsage.memoryTotal)}MB"
                    )
                )
            }
            memoryUsagePercent >= 80 -> {
                createAlert(
                    type = AlertType.MEMORY_HIGH,
                    severity = AlertSeverity.MEDIUM,
                    message = "High memory usage: ${String.format("%.1f", memoryUsagePercent)}%",
                    context = mapOf(
                        "memoryUsed" to "${String.format("%.1f", resourceUsage.memoryUsed)}MB",
                        "memoryTotal" to "${String.format("%.1f", resourceUsage.memoryTotal)}MB"
                    )
                )
            }
        }
    }

    private suspend fun checkCpuUsage(resourceUsage: ResourceUsageSnapshot) {
        when {
            resourceUsage.cpuUsage >= 90 -> {
                createAlert(
                    type = AlertType.CPU_HIGH,
                    severity = AlertSeverity.CRITICAL,
                    message = "Critical CPU usage: ${String.format("%.1f", resourceUsage.cpuUsage)}%",
                    context = mapOf("cpuUsage" to "${String.format("%.1f", resourceUsage.cpuUsage)}%")
                )
            }
            resourceUsage.cpuUsage >= 80 -> {
                createAlert(
                    type = AlertType.CPU_HIGH,
                    severity = AlertSeverity.MEDIUM,
                    message = "High CPU usage: ${String.format("%.1f", resourceUsage.cpuUsage)}%",
                    context = mapOf("cpuUsage" to "${String.format("%.1f", resourceUsage.cpuUsage)}%")
                )
            }
        }
    }

    private suspend fun checkBatteryLevel(resourceUsage: ResourceUsageSnapshot) {
        when {
            resourceUsage.batteryLevel <= 10 -> {
                createAlert(
                    type = AlertType.BATTERY_LOW,
                    severity = AlertSeverity.CRITICAL,
                    message = "Critical battery level: ${String.format("%.1f", resourceUsage.batteryLevel)}%",
                    context = mapOf("batteryLevel" to "${String.format("%.1f", resourceUsage.batteryLevel)}%")
                )
            }
            resourceUsage.batteryLevel <= 20 -> {
                createAlert(
                    type = AlertType.BATTERY_LOW,
                    severity = AlertSeverity.MEDIUM,
                    message = "Low battery level: ${String.format("%.1f", resourceUsage.batteryLevel)}%",
                    context = mapOf("batteryLevel" to "${String.format("%.1f", resourceUsage.batteryLevel)}%")
                )
            }
        }
    }

    private suspend fun checkNetworkLatency(resourceUsage: ResourceUsageSnapshot) {
        when {
            resourceUsage.networkLatency >= 1000 -> {
                createAlert(
                    type = AlertType.NETWORK_LATENCY_HIGH,
                    severity = AlertSeverity.CRITICAL,
                    message = "Critical network latency: ${String.format("%.1f", resourceUsage.networkLatency)}ms",
                    context = mapOf("latency" to "${String.format("%.1f", resourceUsage.networkLatency)}ms")
                )
            }
            resourceUsage.networkLatency >= 500 -> {
                createAlert(
                    type = AlertType.NETWORK_LATENCY_HIGH,
                    severity = AlertSeverity.MEDIUM,
                    message = "High network latency: ${String.format("%.1f", resourceUsage.networkLatency)}ms",
                    context = mapOf("latency" to "${String.format("%.1f", resourceUsage.networkLatency)}ms")
                )
            }
        }
    }
    
    private suspend fun checkNetworkHealth() {
        val resourceUsage = diagnosticsProvider.get().getCurrentReport().resourceUsage
        
        // Check network latency
        if (resourceUsage.networkLatency > 200) {
            triggerAlert(
                type = AlertType.NETWORK_LATENCY_HIGH,
                severity = if (resourceUsage.networkLatency > 500) AlertSeverity.CRITICAL else AlertSeverity.HIGH,
                message = "High network latency detected: ${resourceUsage.networkLatency.toInt()}ms",
                context = mapOf("networkLatency" to resourceUsage.networkLatency.toString())
            )
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
                    val alert = Alert(
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
    
    private suspend fun processNewAlert(alert: Alert) {
        // Check suppression
        if (!isAlertSuppressed(alert)) {
            activeAlerts[alert.id] = alert
            recordAlertEvent(AlertEventType.CREATED, alert)
            
            // Send notifications
            sendNotifications(alert)
            
            // Update rate limiter
            alertRateLimiter.incrementAndGet()
            
            // Emit alert update
            _alertUpdates.value = AlertUpdate(
                alert = alert,
                action = AlertAction.CREATED,
                timestamp = System.currentTimeMillis()
            )
            
            loggingProvider.get().logEvent(
                LogEvent(
                    level = when (alert.severity) {
                        AlertSeverity.CRITICAL -> LogLevel.ERROR
                        AlertSeverity.HIGH -> LogLevel.WARN
                        AlertSeverity.MEDIUM -> LogLevel.WARN
                        AlertSeverity.LOW -> LogLevel.INFO
                    },
                    category = LogCategory.ALERT,
                    message = "Alert triggered: ${alert.type} - ${alert.message}",
                    metadata = mapOf("alertId" to alert.id, "alertType" to alert.type.toString(), "severity" to alert.severity.toString()),
                    component = ComponentType.SYSTEM
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
                
                _alertUpdates.value = AlertUpdate(
                    alert = updatedAlert,
                    action = AlertAction.AUTO_RESOLVED,
                    timestamp = System.currentTimeMillis()
                )
                
                loggingProvider.get().logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.ALERT,
                        message = "Alert auto-resolved: ${alert.id}",
                        metadata = mapOf("alertId" to alert.id, "alertType" to alert.type.toString()),
                        component = ComponentType.SYSTEM
                    )
                )
            }
        }
    }
    
    // =============================================================================
    // HELPER FUNCTIONS
    // =============================================================================
    
    private fun initializeDefaultThresholds() {
        alertThresholds[AlertType.HEALTH_SCORE_LOW] = AlertThreshold(
            warningValue = 0.7,
            criticalValue = 0.5,
            enabled = true
        )
        
        alertThresholds[AlertType.MEMORY_HIGH] = AlertThreshold(
            warningValue = 80.0,
            criticalValue = 90.0,
            enabled = true
        )
        
        alertThresholds[AlertType.CPU_HIGH] = AlertThreshold(
            warningValue = 80.0,
            criticalValue = 95.0,
            enabled = true
        )
        
        alertThresholds[AlertType.NETWORK_LATENCY_HIGH] = AlertThreshold(
            warningValue = 200.0,
            criticalValue = 500.0,
            enabled = true
        )
        
        alertThresholds[AlertType.BATTERY_LOW] = AlertThreshold(
            warningValue = 20.0,
            criticalValue = 10.0,
            enabled = true
        )
        
        alertThresholds[AlertType.PERFORMANCE_DEGRADATION] = AlertThreshold(
            warningValue = 10.0,
            criticalValue = 25.0,
            enabled = true
        )
        
        alertThresholds[AlertType.EFFICIENCY_LOW] = AlertThreshold(
            warningValue = 0.6,
            criticalValue = 0.4,
            enabled = true
        )
    }
    
    private fun initializeNotificationChannels() {
        notificationChannels[NotificationChannel.IN_APP] = NotificationConfig(
            enabled = true,
            severityFilter = setOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH, AlertSeverity.MEDIUM, AlertSeverity.LOW)
        )
        
        notificationChannels[NotificationChannel.SYSTEM] = NotificationConfig(
            enabled = true,
            severityFilter = setOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH)
        )
        
        notificationChannels[NotificationChannel.LOG] = NotificationConfig(
            enabled = true,
            severityFilter = setOf(AlertSeverity.CRITICAL, AlertSeverity.HIGH, AlertSeverity.MEDIUM, AlertSeverity.LOW)
        )
    }
    
    private fun generateAlertId(): String {
        return "alert_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
    
    private fun findSimilarActiveAlert(type: AlertType, context: Map<String, String>): Alert? {
        return activeAlerts.values.find { alert ->
            alert.type == type && isSimilarContext(alert.context, context)
        }
    }
    
    private fun isSimilarContext(context1: Map<String, String>, context2: Map<String, String>): Boolean {
        // Simple similarity check - can be enhanced with more sophisticated logic
        return context1.keys.intersect(context2.keys).size >= context1.keys.size * 0.5
    }
    
    private fun updateExistingAlert(alert: Alert, newContext: Map<String, String>) {
        val updatedAlert = alert.copy(
            context = alert.context + newContext,
            lastUpdated = System.currentTimeMillis(),
            occurrenceCount = alert.occurrenceCount + 1
        )
        
        activeAlerts[alert.id] = updatedAlert
        recordAlertEvent(AlertEventType.UPDATED, updatedAlert)
    }
    
    private fun isAlertSuppressed(alert: Alert): Boolean {
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
    
    private suspend fun sendNotifications(alert: Alert) {
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
    
    private suspend fun shouldAutoResolve(alert: Alert): Boolean {
        return when (alert.type) {
            AlertType.HEALTH_SCORE_LOW -> {
                val currentHealth = diagnosticsProvider.get().getCurrentReport().systemHealth.overall
                val threshold = alertThresholds[AlertType.HEALTH_SCORE_LOW]?.warningValue ?: 0.7
                currentHealth > threshold
            }
            AlertType.MEMORY_HIGH -> {
                val resourceUsage = diagnosticsProvider.get().getCurrentReport().resourceUsage
                val memoryUsagePercent = resourceUsage.memoryUsed / resourceUsage.memoryTotal * 100
                val threshold = alertThresholds[AlertType.MEMORY_HIGH]?.warningValue ?: 80.0
                memoryUsagePercent < threshold
            }
            AlertType.CPU_HIGH -> {
                val resourceUsage = diagnosticsProvider.get().getCurrentReport().resourceUsage
                val threshold = alertThresholds[AlertType.CPU_HIGH]?.warningValue ?: 80.0
                resourceUsage.cpuUsage < threshold
            }
            AlertType.NETWORK_LATENCY_HIGH -> {
                val resourceUsage = diagnosticsProvider.get().getCurrentReport().resourceUsage
                val threshold = alertThresholds[AlertType.NETWORK_LATENCY_HIGH]?.warningValue ?: 200.0
                resourceUsage.networkLatency < threshold
            }
            else -> false
        }
    }
    
    private fun recordAlertEvent(eventType: AlertEventType, alert: Alert) {
        val event = AlertEvent(
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
        _systemStatus.value = AlertingSystemStatus(
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
    
    private fun calculateAverageResolutionTime(events: List<AlertEvent>): Long {
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
    
    private fun getTopAlertTypes(events: List<AlertEvent>, limit: Int): List<Pair<AlertType, Int>> {
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

 