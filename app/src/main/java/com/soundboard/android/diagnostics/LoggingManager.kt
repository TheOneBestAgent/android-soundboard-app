package com.soundboard.android.diagnostics

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LoggingManager - Advanced logging and analysis system
 * 
 * Provides structured logging with categorization, filtering, pattern detection,
 * and automated analysis capabilities for comprehensive system monitoring.
 * 
 * Key Features:
 * - Structured logging with categorization and filtering
 * - Log aggregation from all components with correlation IDs
 * - Pattern detection and anomaly identification
 * - Configurable log levels with runtime adjustment
 * - Log export capabilities for external analysis
 * - Real-time log streaming and analysis
 * - Memory-efficient log storage and rotation
 */
@Singleton
class LoggingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_MEMORY_LOGS = 10_000
        private const val MAX_LOG_FILE_SIZE_MB = 10
        private const val LOG_ROTATION_COUNT = 5
        private const val PATTERN_ANALYSIS_INTERVAL_MS = 60_000L // 1 minute
        private const val ANOMALY_DETECTION_WINDOW_MINUTES = 10
        private const val LOG_CLEANUP_INTERVAL_HOURS = 24
        private val DEFAULT_LOG_LEVEL = LogLevel.INFO
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    // Logging configuration
    private var currentLogLevel = DEFAULT_LOG_LEVEL
    private var enableFileLogging = true
    private var enableConsoleLogging = true
    private var enablePatternDetection = true
    
    // In-memory log storage
    private val memoryLogs = ConcurrentLinkedQueue<LogEntry>()
    private val logsByCategory = ConcurrentHashMap<LogCategory, MutableList<LogEntry>>()
    private val logsByComponent = ConcurrentHashMap<ComponentType, MutableList<LogEntry>>()
    private val correlationGroups = ConcurrentHashMap<String, MutableList<LogEntry>>()
    
    // Pattern detection and analysis
    private val _detectedPatterns = MutableStateFlow<List<LogPattern>>(emptyList())
    val detectedPatterns: StateFlow<List<LogPattern>> = _detectedPatterns.asStateFlow()
    
    private val _detectedAnomalies = MutableStateFlow<List<LogAnomaly>>(emptyList())
    val detectedAnomalies: StateFlow<List<LogAnomaly>> = _detectedAnomalies.asStateFlow()
    
    private val _logStatistics = MutableStateFlow(LogStatistics.initial())
    val logStatistics: StateFlow<LogStatistics> = _logStatistics.asStateFlow()
    
    // Pattern tracking
    private val patternFrequency = ConcurrentHashMap<String, AtomicLong>()
    private val errorPatterns = ConcurrentHashMap<String, MutableList<LogEntry>>()
    private val performancePatterns = ConcurrentHashMap<String, MutableList<LogEntry>>()
    
    // File logging
    private val logDirectory: File by lazy { 
        File(context.filesDir, "logs").apply { mkdirs() }
    }
    private var currentLogFile: File? = null
    private var logFileWriter: FileWriter? = null
    
    // Monitoring jobs
    private var patternAnalysisJob: Job? = null
    private var logCleanupJob: Job? = null
    private var isInitialized = false

    /**
     * Initialize the logging manager
     */
    suspend fun initialize() {
        if (isInitialized) return
        
        setupFileLogging()
        startPatternAnalysis()
        startLogCleanup()
        
        isInitialized = true
        
        logEvent(LogEvent(
            level = LogLevel.INFO,
            category = LogCategory.SYSTEM,
            message = "LoggingManager initialized successfully",
            metadata = mapOf(
                "maxMemoryLogs" to MAX_MEMORY_LOGS,
                "fileLogging" to enableFileLogging,
                "patternDetection" to enablePatternDetection
            ),
            component = ComponentType.SYSTEM
        ))
    }

    /**
     * Shutdown the logging manager and cleanup resources
     */
    suspend fun shutdown() {
        isInitialized = false
        
        logEvent(LogEvent(
            level = LogLevel.INFO,
            category = LogCategory.SYSTEM,
            message = "LoggingManager shutting down",
            metadata = emptyMap(),
            component = ComponentType.SYSTEM
        ))
        
        patternAnalysisJob?.cancel()
        logCleanupJob?.cancel()
        
        logFileWriter?.close()
        scope.cancel()
    }

    // =============================================================================
    // CORE LOGGING API
    // =============================================================================

    /**
     * Log a structured event
     */
    suspend fun logEvent(event: LogEvent) {
        if (!shouldLog(event.level)) return
        
        val entry = LogEntry(
            id = generateLogId(),
            timestamp = System.currentTimeMillis(),
            level = event.level,
            category = event.category,
            component = event.component,
            message = event.message,
            metadata = event.metadata,
            correlationId = event.correlationId,
            threadName = Thread.currentThread().name,
            formattedMessage = formatLogMessage(event)
        )
        
        // Add to memory storage
        addToMemoryStorage(entry)
        
        // Add to categorized storage
        addToCategorizedStorage(entry)
        
        // Add to correlation group if applicable
        entry.correlationId?.let { correlationId ->
            correlationGroups.getOrPut(correlationId) { mutableListOf() }.add(entry)
        }
        
        // Write to file if enabled
        if (enableFileLogging) {
            writeToFile(entry)
        }
        
        // Write to console if enabled
        if (enableConsoleLogging) {
            writeToConsole(entry)
        }
        
        // Update statistics
        updateLogStatistics(entry)
        
        // Check for immediate patterns or anomalies
        if (enablePatternDetection) {
            checkForImmediatePatterns(entry)
        }
    }

    /**
     * Log with correlation ID for request tracking
     */
    suspend fun logWithCorrelation(correlationId: String, event: LogEvent) {
        logEvent(event.copy(correlationId = correlationId))
    }

    /**
     * Get logs with filtering options
     */
    suspend fun getLogs(filter: LogFilter): List<LogEntry> {
        return memoryLogs.filter { entry ->
            matchesFilter(entry, filter)
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Detect patterns in logs within time range
     */
    suspend fun detectPatterns(timeRange: TimeRange): List<LogPattern> {
        val relevantLogs = memoryLogs.filter { entry ->
            entry.timestamp >= timeRange.start && entry.timestamp <= timeRange.end
        }
        
        val patterns = analyzeLogsForPatterns(relevantLogs)
        _detectedPatterns.value = patterns
        
        return patterns
    }

    /**
     * Analyze logs for anomalies
     */
    suspend fun analyzeAnomalies(): List<LogAnomaly> {
        val recentLogs = memoryLogs.filter { entry ->
            entry.timestamp >= System.currentTimeMillis() - (ANOMALY_DETECTION_WINDOW_MINUTES * 60 * 1000)
        }
        
        val anomalies = detectAnomalies(recentLogs)
        _detectedAnomalies.value = anomalies
        
        return anomalies
    }

    // =============================================================================
    // LOG EXPORT AND MANAGEMENT
    // =============================================================================

    /**
     * Export logs in specified format
     */
    suspend fun exportLogs(format: ExportFormat): ByteArray {
        return when (format) {
            ExportFormat.JSON -> exportAsJson()
            ExportFormat.CSV -> exportAsCsv()
            ExportFormat.TEXT -> exportAsText()
            ExportFormat.XML -> exportAsXml()
        }
    }

    /**
     * Cleanup old logs based on retention policy
     */
    suspend fun cleanupOldLogs() {
        val cutoffTime = System.currentTimeMillis() - (LOG_CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000)
        
        // Clean memory logs
        val iterator = memoryLogs.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().timestamp < cutoffTime) {
                iterator.remove()
            }
        }
        
        // Clean categorized logs
        logsByCategory.values.forEach { logs ->
            logs.removeAll { it.timestamp < cutoffTime }
        }
        
        logsByComponent.values.forEach { logs ->
            logs.removeAll { it.timestamp < cutoffTime }
        }
        
        // Clean correlation groups
        correlationGroups.values.forEach { logs ->
            logs.removeAll { it.timestamp < cutoffTime }
        }
        
        // Clean old log files
        cleanupOldLogFiles()
        
        logEvent(LogEvent(
            level = LogLevel.INFO,
            category = LogCategory.SYSTEM,
            message = "Log cleanup completed",
            metadata = mapOf(
                "cutoffTime" to cutoffTime,
                "remainingLogs" to memoryLogs.size
            ),
            component = ComponentType.SYSTEM
        ))
    }

    /**
     * Get current log statistics
     */
    suspend fun getLogStatistics(): LogStatistics {
        return _logStatistics.value
    }

    // =============================================================================
    // CONFIGURATION API
    // =============================================================================

    /**
     * Set log level for filtering
     */
    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
        
        scope.launch {
            logEvent(LogEvent(
                level = LogLevel.INFO,
                category = LogCategory.SYSTEM,
                message = "Log level changed",
                metadata = mapOf("newLevel" to level.name),
                component = ComponentType.SYSTEM
            ))
        }
    }

    /**
     * Enable or disable file logging
     */
    fun setFileLoggingEnabled(enabled: Boolean) {
        enableFileLogging = enabled
        
        if (!enabled) {
            logFileWriter?.close()
            logFileWriter = null
        } else {
            scope.launch { setupFileLogging() }
        }
    }

    /**
     * Enable or disable pattern detection
     */
    fun setPatternDetectionEnabled(enabled: Boolean) {
        enablePatternDetection = enabled
        
        if (enabled && patternAnalysisJob == null) {
            startPatternAnalysis()
        } else if (!enabled) {
            patternAnalysisJob?.cancel()
            patternAnalysisJob = null
        }
    }

    // =============================================================================
    // INTERNAL IMPLEMENTATION
    // =============================================================================

    private fun shouldLog(level: LogLevel): Boolean {
        return level.priority >= currentLogLevel.priority
    }

    private fun generateLogId(): String {
        return UUID.randomUUID().toString().substring(0, 8)
    }

    private fun formatLogMessage(event: LogEvent): String {
        val timestamp = dateFormatter.format(Date(System.currentTimeMillis()))
        val levelStr = event.level.name.padEnd(5)
        val categoryStr = event.category.name.padEnd(12)
        val componentStr = event.component.name.padEnd(15)
        val threadStr = Thread.currentThread().name.padEnd(20)
        
        return "$timestamp [$levelStr] [$categoryStr] [$componentStr] [$threadStr] ${event.message}"
    }

    private fun addToMemoryStorage(entry: LogEntry) {
        memoryLogs.offer(entry)
        
        // Maintain memory limit
        while (memoryLogs.size > MAX_MEMORY_LOGS) {
            memoryLogs.poll()
        }
    }

    private fun addToCategorizedStorage(entry: LogEntry) {
        logsByCategory.getOrPut(entry.category) { mutableListOf() }.add(entry)
        logsByComponent.getOrPut(entry.component) { mutableListOf() }.add(entry)
    }

    private suspend fun writeToFile(entry: LogEntry) {
        try {
            ensureLogFileWriter()
            logFileWriter?.appendLine(entry.formattedMessage)
            logFileWriter?.flush()
            
            // Check file size and rotate if necessary
            currentLogFile?.let { file ->
                if (file.length() > MAX_LOG_FILE_SIZE_MB * 1024 * 1024) {
                    rotateLogFile()
                }
            }
        } catch (e: Exception) {
            // Fallback to console logging if file logging fails
            writeToConsole(entry)
        }
    }

    private fun writeToConsole(entry: LogEntry) {
        when (entry.level) {
            LogLevel.ERROR -> Log.e("Soundboard", entry.formattedMessage)
            LogLevel.WARN -> Log.w("Soundboard", entry.formattedMessage)
            LogLevel.INFO -> Log.i("Soundboard", entry.formattedMessage)
            LogLevel.DEBUG -> Log.d("Soundboard", entry.formattedMessage)
            LogLevel.TRACE -> Log.v("Soundboard", entry.formattedMessage)
        }
    }

    private suspend fun setupFileLogging() {
        if (!enableFileLogging) return
        
        try {
            currentLogFile = File(logDirectory, "soundboard_${System.currentTimeMillis()}.log")
            logFileWriter = FileWriter(currentLogFile, true)
        } catch (e: Exception) {
            enableFileLogging = false
            Log.e("LoggingManager", "Failed to setup file logging", e)
        }
    }

    private suspend fun rotateLogFile() {
        logFileWriter?.close()
        
        // Rename current file
        currentLogFile?.let { file ->
            val rotatedFile = File(logDirectory, "soundboard_${System.currentTimeMillis()}.log")
            file.renameTo(rotatedFile)
        }
        
        setupFileLogging()
        cleanupOldLogFiles()
    }

    private fun cleanupOldLogFiles() {
        val logFiles = logDirectory.listFiles { _, name -> name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        
        // Keep only the most recent files
        logFiles.drop(LOG_ROTATION_COUNT).forEach { file ->
            file.delete()
        }
    }

    private suspend fun ensureLogFileWriter() {
        if (logFileWriter == null && enableFileLogging) {
            setupFileLogging()
        }
    }

    private fun updateLogStatistics(entry: LogEntry) {
        val currentStats = _logStatistics.value
        val newStats = currentStats.copy(
            totalLogs = currentStats.totalLogs + 1,
            logsByLevel = currentStats.logsByLevel.toMutableMap().apply {
                this[entry.level] = (this[entry.level] ?: 0) + 1
            },
            logsByCategory = currentStats.logsByCategory.toMutableMap().apply {
                this[entry.category] = (this[entry.category] ?: 0) + 1
            },
            logsByComponent = currentStats.logsByComponent.toMutableMap().apply {
                this[entry.component] = (this[entry.component] ?: 0) + 1
            },
            lastUpdated = System.currentTimeMillis()
        )
        
        _logStatistics.value = newStats
    }

    private fun matchesFilter(entry: LogEntry, filter: LogFilter): Boolean {
        return (filter.level == null || entry.level == filter.level) &&
                (filter.category == null || entry.category == filter.category) &&
                (filter.component == null || entry.component == filter.component) &&
                (filter.correlationId == null || entry.correlationId == filter.correlationId) &&
                (filter.timeRange == null || (entry.timestamp >= filter.timeRange.start && entry.timestamp <= filter.timeRange.end)) &&
                (filter.searchText == null || entry.message.contains(filter.searchText, ignoreCase = true))
    }

    private fun checkForImmediatePatterns(entry: LogEntry) {
        // Check for error patterns
        if (entry.level == LogLevel.ERROR) {
            val pattern = extractErrorPattern(entry.message)
            val frequency = patternFrequency.getOrPut(pattern) { AtomicLong(0) }
            frequency.incrementAndGet()
            
            errorPatterns.getOrPut(pattern) { mutableListOf() }.add(entry)
        }
        
        // Check for performance patterns
        if (entry.category == LogCategory.PERFORMANCE) {
            val pattern = extractPerformancePattern(entry.message)
            performancePatterns.getOrPut(pattern) { mutableListOf() }.add(entry)
        }
    }

    private fun extractErrorPattern(message: String): String {
        // Extract common error patterns by removing specific details
        return message
            .replace(Regex("\\d+"), "[NUMBER]")
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[EMAIL]")
            .replace(Regex("\\b(?:https?|ftp)://[^\\s]+"), "[URL]")
            .take(100) // Limit pattern length
    }

    private fun extractPerformancePattern(message: String): String {
        // Extract performance patterns focusing on operation types
        return message
            .replace(Regex("\\d+\\.?\\d*ms"), "[DURATION]")
            .replace(Regex("\\d+\\.?\\d*%"), "[PERCENTAGE]")
            .replace(Regex("\\d+\\.?\\d*MB"), "[MEMORY]")
            .take(100)
    }

    private fun startPatternAnalysis() {
        patternAnalysisJob = scope.launch {
            while (isActive) {
                try {
                    analyzePatterns()
                    delay(PATTERN_ANALYSIS_INTERVAL_MS)
                } catch (e: Exception) {
                    delay(PATTERN_ANALYSIS_INTERVAL_MS)
                }
            }
        }
    }

    private fun startLogCleanup() {
        logCleanupJob = scope.launch {
            while (isActive) {
                try {
                    cleanupOldLogs()
                    delay(LOG_CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000L)
                } catch (e: Exception) {
                    delay(LOG_CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000L)
                }
            }
        }
    }

    private suspend fun analyzePatterns() {
        val patterns = mutableListOf<LogPattern>()
        
        // Analyze error patterns
        errorPatterns.forEach { (pattern, logs) ->
            if (logs.size >= 3) { // Minimum occurrences for a pattern
                patterns.add(
                    LogPattern(
                        pattern = pattern,
                        frequency = logs.size,
                        severity = when {
                            logs.size >= 10 -> Severity.CRITICAL
                            logs.size >= 5 -> Severity.HIGH
                            else -> Severity.MEDIUM
                        },
                        suggestedAction = generatePatternAction(pattern, logs),
                        firstOccurrence = logs.minOfOrNull { it.timestamp } ?: 0,
                        lastOccurrence = logs.maxOfOrNull { it.timestamp } ?: 0,
                        affectedComponents = logs.map { it.component }.distinct()
                    )
                )
            }
        }
        
        // Analyze performance patterns
        performancePatterns.forEach { (pattern, logs) ->
            if (logs.size >= 2) {
                patterns.add(
                    LogPattern(
                        pattern = pattern,
                        frequency = logs.size,
                        severity = Severity.MEDIUM,
                        suggestedAction = "Review performance optimization for this operation",
                        firstOccurrence = logs.minOfOrNull { it.timestamp } ?: 0,
                        lastOccurrence = logs.maxOfOrNull { it.timestamp } ?: 0,
                        affectedComponents = logs.map { it.component }.distinct()
                    )
                )
            }
        }
        
        _detectedPatterns.value = patterns.sortedByDescending { it.severity.priority }
    }

    private fun generatePatternAction(pattern: String, logs: List<LogEntry>): String {
        return when {
            pattern.contains("connection", ignoreCase = true) -> "Check network connectivity and server status"
            pattern.contains("memory", ignoreCase = true) -> "Review memory usage and optimize allocation"
            pattern.contains("timeout", ignoreCase = true) -> "Increase timeout values or optimize performance"
            pattern.contains("permission", ignoreCase = true) -> "Verify application permissions"
            pattern.contains("cache", ignoreCase = true) -> "Review cache configuration and size limits"
            else -> "Investigate recurring error pattern and implement fix"
        }
    }

    private fun analyzeLogsForPatterns(logs: List<LogEntry>): List<LogPattern> {
        val messagePatterns = mutableMapOf<String, MutableList<LogEntry>>()
        
        logs.forEach { entry ->
            val pattern = when {
                entry.level == LogLevel.ERROR -> extractErrorPattern(entry.message)
                entry.category == LogCategory.PERFORMANCE -> extractPerformancePattern(entry.message)
                else -> entry.message.take(50) // Simple pattern for other logs
            }
            
            messagePatterns.getOrPut(pattern) { mutableListOf() }.add(entry)
        }
        
        return messagePatterns.filter { it.value.size >= 2 }.map { (pattern, entries) ->
            LogPattern(
                pattern = pattern,
                frequency = entries.size,
                severity = when {
                    entries.any { it.level == LogLevel.ERROR } && entries.size >= 5 -> Severity.CRITICAL
                    entries.any { it.level == LogLevel.ERROR } -> Severity.HIGH
                    entries.size >= 10 -> Severity.MEDIUM
                    else -> Severity.LOW
                },
                suggestedAction = generatePatternAction(pattern, entries),
                firstOccurrence = entries.minOfOrNull { it.timestamp } ?: 0,
                lastOccurrence = entries.maxOfOrNull { it.timestamp } ?: 0,
                affectedComponents = entries.map { it.component }.distinct()
            )
        }.sortedByDescending { it.frequency }
    }

    private fun detectAnomalies(logs: List<LogEntry>): List<LogAnomaly> {
        val anomalies = mutableListOf<LogAnomaly>()
        
        // Detect sudden error spikes
        val errorLogs = logs.filter { it.level == LogLevel.ERROR }
        if (errorLogs.size > 10) { // Threshold for error spike
            anomalies.add(
                LogAnomaly(
                    type = AnomalyType.ERROR_SPIKE,
                    description = "Unusual number of errors detected",
                    severity = Severity.HIGH,
                    timestamp = System.currentTimeMillis(),
                    affectedLogs = errorLogs.size,
                    suggestedAction = "Investigate recent changes that may have caused error increase"
                )
            )
        }
        
        // Detect unusual silence (too few logs)
        val recentLogsCount = logs.count { 
            it.timestamp >= System.currentTimeMillis() - (5 * 60 * 1000) // Last 5 minutes
        }
        if (recentLogsCount < 5) { // Threshold for silence
            anomalies.add(
                LogAnomaly(
                    type = AnomalyType.UNUSUAL_SILENCE,
                    description = "Unusually low logging activity detected",
                    severity = Severity.MEDIUM,
                    timestamp = System.currentTimeMillis(),
                    affectedLogs = recentLogsCount,
                    suggestedAction = "Check if application components are functioning properly"
                )
            )
        }
        
        return anomalies
    }

    // Export implementations
    private suspend fun exportAsJson(): ByteArray {
        // Implementation for JSON export
        return "JSON export not implemented".toByteArray()
    }

    private suspend fun exportAsCsv(): ByteArray {
        // Implementation for CSV export
        return "CSV export not implemented".toByteArray()
    }

    private suspend fun exportAsText(): ByteArray {
        val logs = memoryLogs.toList()
        val content = logs.joinToString("\n") { it.formattedMessage }
        return content.toByteArray()
    }

    private suspend fun exportAsXml(): ByteArray {
        // Implementation for XML export
        return "XML export not implemented".toByteArray()
    }
} 