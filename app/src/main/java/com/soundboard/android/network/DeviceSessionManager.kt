package com.soundboard.android.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.1: Multi-Device Session Management
 * 
 * Manages multiple connected Android devices with session coordination,
 * device roles, and conflict resolution for simultaneous connections.
 */
@Singleton
class DeviceSessionManager @Inject constructor() {
    
    companion object {
        private const val TAG = "DeviceSessionManager"
        private const val MAX_DEVICES = 10
        private const val SESSION_TIMEOUT_MS = 300_000L // 5 minutes
    }
    
    // Device role definitions
    enum class DeviceRole(val priority: Int, val description: String) {
        PRIMARY(1, "Full control with highest priority"),
        SECONDARY(2, "Limited control with medium priority"),  
        OBSERVER(3, "Read-only access with lowest priority")
    }
    
    // Device capabilities
    enum class DeviceCapability {
        AUDIO_PLAYBACK,
        SOUND_UPLOAD,
        LAYOUT_MODIFICATION,
        SETTINGS_ACCESS,
        ANALYTICS_ACCESS,
        BACKUP_RESTORE
    }
    
    // Session state definitions
    enum class SessionState {
        CONNECTING,
        CONNECTED,
        AUTHENTICATED,
        ACTIVE,
        SUSPENDED,
        DISCONNECTED,
        EXPIRED
    }
    
    // Device session data class
    data class DeviceSession(
        val deviceId: String,
        val deviceName: String,
        val deviceInfo: DeviceInfo,
        val role: DeviceRole,
        val capabilities: Set<DeviceCapability>,
        val sessionState: SessionState,
        val connectionTimestamp: Long,
        val lastActivityTimestamp: Long,
        val sessionToken: String,
        val transport: String, // WebSocket, HTTP, etc.
        val ipAddress: String,
        val userAgent: String
    )
    
    // Device information
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val appVersion: String,
        val screenResolution: String,
        val density: Float
    )
    
    // Session event for notifications
    data class SessionEvent(
        val type: SessionEventType,
        val deviceId: String,
        val timestamp: Long,
        val data: Map<String, Any> = emptyMap()
    )
    
    enum class SessionEventType {
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        ROLE_CHANGED,
        SESSION_HANDOFF,
        CONFLICT_RESOLVED,
        SESSION_EXPIRED,
        AUTHENTICATION_FAILED
    }
    
    // Action conflict resolution
    data class ActionConflict(
        val conflictId: String,
        val actions: List<DeviceAction>,
        val timestamp: Long,
        val resolution: ConflictResolution? = null
    )
    
    data class DeviceAction(
        val deviceId: String,
        val actionType: String,
        val actionData: Map<String, Any>,
        val timestamp: Long,
        val priority: Int
    )
    
    data class ConflictResolution(
        val winningAction: DeviceAction,
        val reason: String,
        val resolvedTimestamp: Long
    )
    
    // Thread-safe storage
    private val activeSessions = ConcurrentHashMap<String, DeviceSession>()
    private val sessionMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // State flows for UI observation
    private val _sessionsState = MutableStateFlow<List<DeviceSession>>(emptyList())
    val sessionsState: StateFlow<List<DeviceSession>> = _sessionsState.asStateFlow()
    
    private val _sessionEvents = MutableStateFlow<SessionEvent?>(null)
    val sessionEvents: StateFlow<SessionEvent?> = _sessionEvents.asStateFlow()
    
    private val _conflictsState = MutableStateFlow<List<ActionConflict>>(emptyList())
    val conflictsState: StateFlow<List<ActionConflict>> = _conflictsState.asStateFlow()
    
    // Active conflicts tracking
    private val activeConflicts = ConcurrentHashMap<String, ActionConflict>()
    
    /**
     * Register a new device session
     */
    suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceInfo: DeviceInfo,
        requestedRole: DeviceRole,
        transport: String,
        ipAddress: String,
        userAgent: String
    ): Result<DeviceSession> = sessionMutex.withLock {
        try {
            // Check session limits
            if (activeSessions.size >= MAX_DEVICES) {
                return Result.failure(Exception("Maximum device limit reached"))
            }
            
            // Check if device already exists
            if (activeSessions.containsKey(deviceId)) {
                return Result.failure(Exception("Device already registered"))
            }
            
            // Determine actual role based on current sessions
            val actualRole = determineDeviceRole(requestedRole)
            val capabilities = getCapabilitiesForRole(actualRole)
            
            // Generate session token
            val sessionToken = generateSessionToken()
            
            // Create device session
            val session = DeviceSession(
                deviceId = deviceId,
                deviceName = deviceName,
                deviceInfo = deviceInfo,
                role = actualRole,
                capabilities = capabilities,
                sessionState = SessionState.CONNECTED,
                connectionTimestamp = System.currentTimeMillis(),
                lastActivityTimestamp = System.currentTimeMillis(),
                sessionToken = sessionToken,
                transport = transport,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            
            // Store session
            activeSessions[deviceId] = session
            updateSessionsState()
            
            // Emit session event
            emitSessionEvent(SessionEventType.DEVICE_CONNECTED, deviceId)
            
            Log.i(TAG, "Device registered: $deviceName ($deviceId) as ${actualRole.name}")
            
            Result.success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device: $deviceId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unregister a device session
     */
    suspend fun unregisterDevice(deviceId: String): Result<Unit> = sessionMutex.withLock {
        try {
            val session = activeSessions.remove(deviceId)
            if (session != null) {
                updateSessionsState()
                emitSessionEvent(SessionEventType.DEVICE_DISCONNECTED, deviceId)
                
                // If primary device disconnected, promote secondary
                if (session.role == DeviceRole.PRIMARY) {
                    promoteSecondaryToPrimary()
                }
                
                Log.i(TAG, "Device unregistered: ${session.deviceName} ($deviceId)")
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister device: $deviceId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update device activity timestamp
     */
    suspend fun updateDeviceActivity(deviceId: String) {
        sessionMutex.withLock {
            activeSessions[deviceId]?.let { session ->
                activeSessions[deviceId] = session.copy(
                    lastActivityTimestamp = System.currentTimeMillis()
                )
                updateSessionsState()
            }
        }
    }
    
    /**
     * Change device role
     */
    suspend fun changeDeviceRole(
        deviceId: String, 
        newRole: DeviceRole
    ): Result<DeviceSession> = sessionMutex.withLock {
        try {
            val session = activeSessions[deviceId]
                ?: return Result.failure(Exception("Device not found"))
            
            // Check if role change is allowed
            if (!isRoleChangeAllowed(session, newRole)) {
                return Result.failure(Exception("Role change not allowed"))
            }
            
            val updatedSession = session.copy(
                role = newRole,
                capabilities = getCapabilitiesForRole(newRole)
            )
            
            activeSessions[deviceId] = updatedSession
            updateSessionsState()
            
            emitSessionEvent(SessionEventType.ROLE_CHANGED, deviceId, 
                mapOf("oldRole" to session.role.name, "newRole" to newRole.name))
            
            Log.i(TAG, "Device role changed: $deviceId from ${session.role.name} to ${newRole.name}")
            
            Result.success(updatedSession)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change device role: $deviceId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle action conflict resolution
     */
    suspend fun resolveActionConflict(
        actions: List<DeviceAction>
    ): ConflictResolution {
        val conflictId = UUID.randomUUID().toString()
        val conflict = ActionConflict(
            conflictId = conflictId,
            actions = actions,
            timestamp = System.currentTimeMillis()
        )
        
        activeConflicts[conflictId] = conflict
        updateConflictsState()
        
        // Resolve based on device priority and role
        val winningAction = actions.minByOrNull { action ->
            val session = activeSessions[action.deviceId]
            session?.role?.priority ?: Int.MAX_VALUE
        } ?: actions.first()
        
        val resolution = ConflictResolution(
            winningAction = winningAction,
            reason = "Resolved by device role priority",
            resolvedTimestamp = System.currentTimeMillis()
        )
        
        // Update conflict with resolution
        activeConflicts[conflictId] = conflict.copy(resolution = resolution)
        updateConflictsState()
        
        emitSessionEvent(SessionEventType.CONFLICT_RESOLVED, winningAction.deviceId,
            mapOf("conflictId" to conflictId, "actionType" to winningAction.actionType))
        
        Log.i(TAG, "Action conflict resolved: ${winningAction.actionType} from ${winningAction.deviceId}")
        
        return resolution
    }
    
    /**
     * Get all active sessions
     */
    fun getActiveSessions(): List<DeviceSession> {
        return activeSessions.values.toList()
    }
    
    /**
     * Get session by device ID
     */
    fun getSession(deviceId: String): DeviceSession? {
        return activeSessions[deviceId]
    }
    
    /**
     * Check if device has capability
     */
    fun hasCapability(deviceId: String, capability: DeviceCapability): Boolean {
        return activeSessions[deviceId]?.capabilities?.contains(capability) ?: false
    }
    
    /**
     * Session cleanup - remove expired sessions
     */
    suspend fun cleanupExpiredSessions() = sessionMutex.withLock {
        val currentTime = System.currentTimeMillis()
        val expiredSessions = activeSessions.values.filter { session ->
            currentTime - session.lastActivityTimestamp > SESSION_TIMEOUT_MS
        }
        
        expiredSessions.forEach { session ->
            activeSessions.remove(session.deviceId)
            emitSessionEvent(SessionEventType.SESSION_EXPIRED, session.deviceId)
            Log.i(TAG, "Session expired: ${session.deviceName} (${session.deviceId})")
        }
        
        if (expiredSessions.isNotEmpty()) {
            updateSessionsState()
        }
    }
    
    // Private helper methods
    
    private fun determineDeviceRole(requestedRole: DeviceRole): DeviceRole {
        val hasPrimary = activeSessions.values.any { it.role == DeviceRole.PRIMARY }
        
        return when (requestedRole) {
            DeviceRole.PRIMARY -> if (hasPrimary) DeviceRole.SECONDARY else DeviceRole.PRIMARY
            DeviceRole.SECONDARY -> DeviceRole.SECONDARY
            DeviceRole.OBSERVER -> DeviceRole.OBSERVER
        }
    }
    
    private fun getCapabilitiesForRole(role: DeviceRole): Set<DeviceCapability> {
        return when (role) {
            DeviceRole.PRIMARY -> setOf(
                DeviceCapability.AUDIO_PLAYBACK,
                DeviceCapability.SOUND_UPLOAD,
                DeviceCapability.LAYOUT_MODIFICATION,
                DeviceCapability.SETTINGS_ACCESS,
                DeviceCapability.ANALYTICS_ACCESS,
                DeviceCapability.BACKUP_RESTORE
            )
            DeviceRole.SECONDARY -> setOf(
                DeviceCapability.AUDIO_PLAYBACK,
                DeviceCapability.SOUND_UPLOAD,
                DeviceCapability.ANALYTICS_ACCESS
            )
            DeviceRole.OBSERVER -> setOf(
                DeviceCapability.ANALYTICS_ACCESS
            )
        }
    }
    
    private fun isRoleChangeAllowed(session: DeviceSession, newRole: DeviceRole): Boolean {
        // Don't allow changing to primary if one already exists
        if (newRole == DeviceRole.PRIMARY) {
            val hasPrimary = activeSessions.values.any { 
                it.deviceId != session.deviceId && it.role == DeviceRole.PRIMARY 
            }
            return !hasPrimary
        }
        
        return true
    }
    
    private suspend fun promoteSecondaryToPrimary() {
        val secondarySession = activeSessions.values.find { it.role == DeviceRole.SECONDARY }
        if (secondarySession != null) {
            changeDeviceRole(secondarySession.deviceId, DeviceRole.PRIMARY)
            Log.i(TAG, "Promoted secondary device to primary: ${secondarySession.deviceName}")
        }
    }
    
    private fun generateSessionToken(): String {
        return UUID.randomUUID().toString()
    }
    
    private fun updateSessionsState() {
        _sessionsState.value = activeSessions.values.toList()
    }
    
    private fun updateConflictsState() {
        _conflictsState.value = activeConflicts.values.toList()
    }
    
    private fun emitSessionEvent(
        type: SessionEventType, 
        deviceId: String, 
        data: Map<String, Any> = emptyMap()
    ) {
        scope.launch {
            _sessionEvents.value = SessionEvent(
                type = type,
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                data = data
            )
        }
    }
    
    /**
     * Start session monitoring (call from application initialization)
     */
    fun startSessionMonitoring() {
        scope.launch {
            while (true) {
                try {
                    cleanupExpiredSessions()
                    kotlinx.coroutines.delay(60_000) // Check every minute
                } catch (e: Exception) {
                    Log.e(TAG, "Error in session monitoring", e)
                }
            }
        }
    }
} 