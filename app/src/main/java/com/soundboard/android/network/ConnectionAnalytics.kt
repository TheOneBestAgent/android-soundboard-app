package com.soundboard.android.network

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

@Serializable
data class ConnectionEvent(
    val timestamp: Long,
    val eventType: ConnectionEventType,
    val details: String,
    val networkType: String? = null,
    val latency: Long? = null,
    val errorCode: String? = null
)

@Serializable
enum class ConnectionEventType {
    CONNECT_ATTEMPT,
    CONNECT_SUCCESS,
    CONNECT_FAILED,
    DISCONNECT,
    TRANSPORT_UPGRADE,
    TRANSPORT_ERROR,
    NETWORK_CHANGE,
    HEALTH_CHECK_FAILED,
    RECONNECT_ATTEMPT,
    RECONNECT_SUCCESS
}

@Serializable
data class ConnectionSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long? = null,
    val events: List<ConnectionEvent> = emptyList(),
    val averageLatency: Long? = null,
    val reconnectCount: Int = 0,
    val transportUpgrades: Int = 0,
    val errorCount: Int = 0
)

@Serializable
data class ConnectionMetrics(
    val totalSessions: Int = 0,
    val totalConnectionTime: Long = 0,
    val averageSessionDuration: Long = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val reconnectAttempts: Int = 0,
    val transportErrors: Int = 0,
    val networkChanges: Int = 0,
    val averageLatency: Long = 0,
    val connectionReliability: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)

class ConnectionAnalytics(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("connection_analytics", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _currentMetrics = MutableStateFlow(loadMetrics())
    val currentMetrics: StateFlow<ConnectionMetrics> = _currentMetrics.asStateFlow()
    
    private val _currentSession = MutableStateFlow<ConnectionSession?>(null)
    val currentSession: StateFlow<ConnectionSession?> = _currentSession.asStateFlow()
    
    private val eventQueue = ConcurrentLinkedQueue<ConnectionEvent>()
    private val maxEventsToStore = 1000 // Limit memory usage
    
    companion object {
        private const val METRICS_KEY = "connection_metrics"
        private const val SESSIONS_KEY = "connection_sessions"
        private const val MAX_STORED_SESSIONS = 50
    }
    
    fun startSession(): String {
        val sessionId = "session_${System.currentTimeMillis()}"
        val session = ConnectionSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis()
        )
        _currentSession.value = session
        
        recordEvent(ConnectionEventType.CONNECT_ATTEMPT, "New connection session started")
        return sessionId
    }
    
    fun endSession() {
        _currentSession.value?.let { session ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - session.startTime
            
            val completedSession = session.copy(
                endTime = endTime,
                duration = duration,
                events = eventQueue.toList(),
                averageLatency = calculateAverageLatency(),
                reconnectCount = countEvents(ConnectionEventType.RECONNECT_ATTEMPT),
                transportUpgrades = countEvents(ConnectionEventType.TRANSPORT_UPGRADE),
                errorCount = countErrors()
            )
            
            saveSession(completedSession)
            updateMetrics(completedSession)
            
            // Clear current session and events
            _currentSession.value = null
            eventQueue.clear()
        }
    }
    
    fun recordEvent(
        eventType: ConnectionEventType,
        details: String,
        networkType: String? = null,
        latency: Long? = null,
        errorCode: String? = null
    ) {
        val event = ConnectionEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            details = details,
            networkType = networkType,
            latency = latency,
            errorCode = errorCode
        )
        
        // Add to current session events
        if (eventQueue.size >= maxEventsToStore) {
            eventQueue.poll() // Remove oldest event to maintain size limit
        }
        eventQueue.offer(event)
        
        // Update current session
        _currentSession.value?.let { session ->
            _currentSession.value = session.copy(
                events = eventQueue.toList()
            )
        }
        
        // Predictive health analysis for Phase 1 enhancements
        performPredictiveAnalysis()
    }
    
    fun recordConnectionSuccess(latency: Long? = null, networkType: String? = null) {
        recordEvent(
            eventType = ConnectionEventType.CONNECT_SUCCESS,
            details = "Connection established successfully",
            networkType = networkType,
            latency = latency
        )
    }
    
    fun recordConnectionFailure(error: String, errorCode: String? = null) {
        recordEvent(
            eventType = ConnectionEventType.CONNECT_FAILED,
            details = error,
            errorCode = errorCode
        )
    }
    
    fun recordDisconnection(reason: String) {
        recordEvent(
            eventType = ConnectionEventType.DISCONNECT,
            details = reason
        )
    }
    
    fun recordTransportUpgrade(fromTransport: String, toTransport: String) {
        recordEvent(
            eventType = ConnectionEventType.TRANSPORT_UPGRADE,
            details = "Transport upgraded from $fromTransport to $toTransport"
        )
    }
    
    fun recordNetworkChange(newNetworkType: String) {
        recordEvent(
            eventType = ConnectionEventType.NETWORK_CHANGE,
            details = "Network changed to $newNetworkType",
            networkType = newNetworkType
        )
    }
    
    private fun calculateAverageLatency(): Long {
        val latencyEvents = eventQueue.filter { it.latency != null }
        return if (latencyEvents.isNotEmpty()) {
            latencyEvents.mapNotNull { it.latency }.average().roundToInt().toLong()
        } else 0L
    }
    
    private fun countEvents(eventType: ConnectionEventType): Int {
        return eventQueue.count { it.eventType == eventType }
    }
    
    private fun countErrors(): Int {
        return eventQueue.count { 
            it.eventType in listOf(
                ConnectionEventType.CONNECT_FAILED,
                ConnectionEventType.TRANSPORT_ERROR,
                ConnectionEventType.HEALTH_CHECK_FAILED
            )
        }
    }
    
    private fun updateMetrics(session: ConnectionSession) {
        val currentMetrics = _currentMetrics.value
        val isSuccessful = session.events.any { it.eventType == ConnectionEventType.CONNECT_SUCCESS }
        
        val updatedMetrics = currentMetrics.copy(
            totalSessions = currentMetrics.totalSessions + 1,
            totalConnectionTime = currentMetrics.totalConnectionTime + (session.duration ?: 0),
            averageSessionDuration = if (currentMetrics.totalSessions > 0) {
                (currentMetrics.totalConnectionTime + (session.duration ?: 0)) / (currentMetrics.totalSessions + 1)
            } else (session.duration ?: 0),
            successfulConnections = if (isSuccessful) currentMetrics.successfulConnections + 1 else currentMetrics.successfulConnections,
            failedConnections = if (!isSuccessful) currentMetrics.failedConnections + 1 else currentMetrics.failedConnections,
            reconnectAttempts = currentMetrics.reconnectAttempts + session.reconnectCount,
            transportErrors = currentMetrics.transportErrors + session.errorCount,
            averageLatency = if (session.averageLatency != null && session.averageLatency > 0) {
                if (currentMetrics.averageLatency > 0) {
                    (currentMetrics.averageLatency + session.averageLatency) / 2
                } else session.averageLatency
            } else currentMetrics.averageLatency,
            connectionReliability = if (currentMetrics.totalSessions + 1 > 0) {
                (if (isSuccessful) currentMetrics.successfulConnections + 1 else currentMetrics.successfulConnections).toFloat() / 
                (currentMetrics.totalSessions + 1).toFloat() * 100f
            } else 0f,
            lastUpdated = System.currentTimeMillis()
        )
        
        _currentMetrics.value = updatedMetrics
        saveMetrics(updatedMetrics)
    }
    
    private fun saveSession(session: ConnectionSession) {
        try {
            val existingSessions = loadSessions().toMutableList()
            existingSessions.add(session)
            
            // Keep only the most recent sessions
            if (existingSessions.size > MAX_STORED_SESSIONS) {
                existingSessions.sortByDescending { it.startTime }
                existingSessions.subList(MAX_STORED_SESSIONS, existingSessions.size).clear()
            }
            
            val sessionsJson = json.encodeToString(existingSessions)
            prefs.edit().putString(SESSIONS_KEY, sessionsJson).apply()
        } catch (e: Exception) {
            // Handle serialization errors gracefully
        }
    }
    
    private fun saveMetrics(metrics: ConnectionMetrics) {
        try {
            val metricsJson = json.encodeToString(metrics)
            prefs.edit().putString(METRICS_KEY, metricsJson).apply()
        } catch (e: Exception) {
            // Handle serialization errors gracefully
        }
    }
    
    private fun loadMetrics(): ConnectionMetrics {
        return try {
            val metricsJson = prefs.getString(METRICS_KEY, null)
            if (metricsJson != null) {
                json.decodeFromString<ConnectionMetrics>(metricsJson)
            } else {
                ConnectionMetrics()
            }
        } catch (e: Exception) {
            ConnectionMetrics()
        }
    }
    
    private fun loadSessions(): List<ConnectionSession> {
        return try {
            val sessionsJson = prefs.getString(SESSIONS_KEY, null)
            if (sessionsJson != null) {
                json.decodeFromString<List<ConnectionSession>>(sessionsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getConnectionHealthScore(): Int {
        val metrics = _currentMetrics.value
        if (metrics.totalSessions == 0) return 100 // No data yet
        
        val reliabilityScore = metrics.connectionReliability.toInt()
        val latencyScore = when {
            metrics.averageLatency < 50 -> 100
            metrics.averageLatency < 100 -> 80
            metrics.averageLatency < 200 -> 60
            else -> 40
        }
        val errorScore = when {
            metrics.transportErrors == 0 -> 100
            metrics.transportErrors < 5 -> 80
            metrics.transportErrors < 10 -> 60
            else -> 40
        }
        
        return (reliabilityScore + latencyScore + errorScore) / 3
    }
    
    fun getRecommendations(): List<String> {
        val metrics = _currentMetrics.value
        val recommendations = mutableListOf<String>()
        
        if (metrics.connectionReliability < 80f) {
            recommendations.add("Connection reliability is low. Check USB cable and ADB setup.")
        }
        
        if (metrics.averageLatency > 100) {
            recommendations.add("High latency detected. Consider restarting the connection or checking network conditions.")
        }
        
        if (metrics.transportErrors > 5) {
            recommendations.add("Multiple transport errors detected. Try reconnecting or restarting the server.")
        }
        
        if (metrics.reconnectAttempts > metrics.successfulConnections * 2) {
            recommendations.add("Frequent reconnections detected. Check for network stability issues.")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Connection performance looks good!")
        }
        
        return recommendations
    }
    
    fun clearAnalytics() {
        prefs.edit().clear().apply()
        _currentMetrics.value = ConnectionMetrics()
        _currentSession.value = null
        eventQueue.clear()
    }
    
    fun exportAnalyticsData(): String {
        val metrics = _currentMetrics.value
        val sessions = loadSessions()
        
        return json.encodeToString(
            mapOf(
                "metrics" to metrics,
                "sessions" to if (sessions is List<*>) sessions.takeLast(10) else emptyList<Any>(), // Export last 10 sessions
                "exportTime" to System.currentTimeMillis()
            )
        )
    }
    
    // Phase 1 Enhancement: Predictive Health Analysis
    private fun performPredictiveAnalysis() {
        _currentSession.value?.let { session ->
            val healthPrediction = predictConnectionHealth(session)
            // Store prediction for future reference
            recordHealthPrediction(healthPrediction)
        }
    }
    
    private fun predictConnectionHealth(session: ConnectionSession): ConnectionHealthPrediction {
        val recentEvents = eventQueue.toList().takeLast(20) // Last 20 events
        val latencyTrend = calculateLatencyTrend(recentEvents)
        val errorRate = calculateRecentErrorRate(recentEvents)
        val stabilityScore = calculateStabilityScore(session)
        
        val riskFactors = mutableListOf<String>()
        
        // Analyze risk factors
        if (latencyTrend > 10) riskFactors.add("increasing_latency")
        if (errorRate > 0.1) riskFactors.add("high_error_rate")
        if (stabilityScore < 0.8) riskFactors.add("connection_instability")
        
        val predictedStability = kotlin.math.max(0.0, kotlin.math.min(1.0, 
            stabilityScore - (latencyTrend * 0.01) - (errorRate * 0.5)
        ))
        
        return ConnectionHealthPrediction(
            predictedStability = predictedStability,
            riskFactors = riskFactors,
            recommendations = generateHealthRecommendations(riskFactors, latencyTrend, errorRate),
            confidenceLevel = calculatePredictionConfidence(recentEvents.size)
        )
    }
    
    private fun calculateLatencyTrend(events: List<ConnectionEvent>): Double {
        val latencyEvents = events.filter { it.latency != null }.takeLast(10)
        if (latencyEvents.size < 3) return 0.0
        
        val halfPoint = latencyEvents.size / 2
        val firstHalf = latencyEvents.take(halfPoint).mapNotNull { it.latency }.average()
        val secondHalf = latencyEvents.drop(halfPoint).mapNotNull { it.latency }.average()
        
        return secondHalf - firstHalf
    }
    
    private fun calculateRecentErrorRate(events: List<ConnectionEvent>): Double {
        if (events.isEmpty()) return 0.0
        val errorEvents = events.count { 
            it.eventType in listOf(
                ConnectionEventType.CONNECT_FAILED,
                ConnectionEventType.TRANSPORT_ERROR,
                ConnectionEventType.HEALTH_CHECK_FAILED
            )
        }
        return errorEvents.toDouble() / events.size
    }
    
    private fun calculateStabilityScore(session: ConnectionSession): Double {
        val totalEvents = session.events.size
        if (totalEvents == 0) return 1.0
        
        val successfulEvents = session.events.count { 
            it.eventType in listOf(
                ConnectionEventType.CONNECT_SUCCESS,
                ConnectionEventType.TRANSPORT_UPGRADE
            )
        }
        val errorEvents = session.events.count { 
            it.eventType in listOf(
                ConnectionEventType.CONNECT_FAILED,
                ConnectionEventType.TRANSPORT_ERROR,
                ConnectionEventType.HEALTH_CHECK_FAILED
            )
        }
        
        return (successfulEvents.toDouble() - errorEvents.toDouble()) / totalEvents.toDouble()
    }
    
    private fun generateHealthRecommendations(
        riskFactors: List<String>, 
        latencyTrend: Double, 
        errorRate: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if ("increasing_latency" in riskFactors) {
            recommendations.add("Latency is increasing. Consider checking USB connection or server performance.")
        }
        
        if ("high_error_rate" in riskFactors) {
            recommendations.add("High error rate detected. Restart connection or check server status.")
        }
        
        if ("connection_instability" in riskFactors) {
            recommendations.add("Connection appears unstable. Check USB cable and ADB setup.")
        }
        
        if (latencyTrend > 20) {
            recommendations.add("Significant latency increase detected. Consider reconnecting.")
        }
        
        if (errorRate > 0.2) {
            recommendations.add("Critical error rate. Immediate reconnection recommended.")
        }
        
        return recommendations
    }
    
    private fun calculatePredictionConfidence(eventCount: Int): Double {
        return when {
            eventCount >= 20 -> 0.9
            eventCount >= 10 -> 0.7
            eventCount >= 5 -> 0.5
            else -> 0.3
        }
    }
    
    private fun recordHealthPrediction(prediction: ConnectionHealthPrediction) {
        // Store prediction in current session or log for future analysis
        recordEvent(
            eventType = ConnectionEventType.HEALTH_CHECK_FAILED, // Reuse existing type or add new one
            details = "Health prediction: stability=${prediction.predictedStability}, risks=${prediction.riskFactors.joinToString()}"
        )
    }
}

@Serializable
data class ConnectionHealthPrediction(
    val predictedStability: Double,
    val riskFactors: List<String>,
    val recommendations: List<String>,
    val confidenceLevel: Double
) 