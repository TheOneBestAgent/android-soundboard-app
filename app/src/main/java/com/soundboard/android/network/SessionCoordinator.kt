package com.soundboard.android.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.1: Session Coordinator
 * 
 * Handles real-time state synchronization across multiple connected devices.
 * Ensures all devices have consistent view of soundboard state and manages
 * state updates, conflict resolution, and session handoffs.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val deviceSessionManager: DeviceSessionManager,
    private val gson: Gson
) {
    
    companion object {
        private const val TAG = "SessionCoordinator"
        private const val SYNC_INTERVAL_MS = 1000L // 1 second
        private const val STATE_HISTORY_SIZE = 50
    }
    
    // State synchronization types
    enum class StateType {
        SOUNDBOARD_LAYOUT,
        SOUND_BUTTONS,
        CURRENT_VOLUME,
        CONNECTION_STATUS,
        USER_PREFERENCES,
        ACTIVE_LAYOUT,
        BACKUP_STATUS
    }
    
    // State change events
    data class StateChangeEvent(
        val stateType: StateType,
        val deviceId: String,
        val timestamp: Long,
        val newState: JsonElement,
        val previousState: JsonElement?,
        val changeId: String
    )
    
    // Synchronization status
    data class SyncStatus(
        val isInSync: Boolean,
        val lastSyncTimestamp: Long,
        val pendingSyncs: Int,
        val conflictCount: Int,
        val deviceSyncStates: Map<String, DeviceSyncState>
    )
    
    data class DeviceSyncState(
        val deviceId: String,
        val lastSyncTimestamp: Long,
        val pendingChanges: Int,
        val syncLatency: Long,
        val isConnected: Boolean
    )
    
    // Session handoff data
    data class SessionHandoff(
        val fromDeviceId: String,
        val toDeviceId: String,
        val handoffType: HandoffType,
        val stateSnapshot: JsonObject,
        val timestamp: Long,
        val completionCallback: (() -> Unit)?
    )
    
    enum class HandoffType {
        GRACEFUL_TRANSFER,      // User initiated
        FAILOVER_TRANSFER,      // Device disconnection
        ROLE_CHANGE_TRANSFER    // Role promotion
    }
    
    // State storage
    private val currentStates = ConcurrentHashMap<StateType, JsonElement>()
    private val stateHistory = ConcurrentHashMap<StateType, MutableList<StateChangeEvent>>()
    private val stateMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // State flows for observation
    private val _syncStatus = MutableStateFlow(
        SyncStatus(
            isInSync = true,
            lastSyncTimestamp = System.currentTimeMillis(),
            pendingSyncs = 0,
            conflictCount = 0,
            deviceSyncStates = emptyMap()
        )
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _stateChanges = MutableStateFlow<StateChangeEvent?>(null)
    val stateChanges: StateFlow<StateChangeEvent?> = _stateChanges.asStateFlow()
    
    private val _handoffEvents = MutableStateFlow<SessionHandoff?>(null)
    val handoffEvents: StateFlow<SessionHandoff?> = _handoffEvents.asStateFlow()
    
    // Pending operations tracking
    private val pendingOperations = ConcurrentHashMap<String, StateChangeEvent>()
    
    /**
     * Initialize the session coordinator
     */
    fun initialize() {
        scope.launch {
            startStateSynchronization()
        }
        
        // Listen to device session events
        scope.launch {
            deviceSessionManager.sessionEvents.collect { event ->
                event?.let { handleSessionEvent(it) }
            }
        }
        
        Log.i(TAG, "SessionCoordinator initialized")
    }
    
    /**
     * Update state and propagate to all connected devices
     */
    suspend fun updateState(
        stateType: StateType,
        newState: JsonElement,
        deviceId: String
    ): Result<String> = stateMutex.withLock {
        try {
            val changeId = generateChangeId()
            val previousState = currentStates[stateType]
            
            // Validate device has permission to make this change
            if (!canDeviceUpdateState(deviceId, stateType)) {
                return Result.failure(Exception("Device does not have permission to update $stateType"))
            }
            
            // Create state change event
            val stateChange = StateChangeEvent(
                stateType = stateType,
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                newState = newState,
                previousState = previousState,
                changeId = changeId
            )
            
            // Update current state
            currentStates[stateType] = newState
            
            // Add to history
            addToHistory(stateType, stateChange)
            
            // Propagate to other devices
            propagateStateChange(stateChange, excludeDeviceId = deviceId)
            
            // Emit state change event
            _stateChanges.value = stateChange
            
            Log.i(TAG, "State updated: $stateType by $deviceId (changeId: $changeId)")
            
            Result.success(changeId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update state: $stateType", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current state for a specific type
     */
    fun getCurrentState(stateType: StateType): JsonElement? {
        return currentStates[stateType]
    }
    
    /**
     * Get all current states
     */
    fun getAllCurrentStates(): Map<StateType, JsonElement> {
        return currentStates.toMap()
    }
    
    /**
     * Initiate session handoff between devices
     */
    suspend fun initiateSessionHandoff(
        fromDeviceId: String,
        toDeviceId: String,
        handoffType: HandoffType
    ): Result<Unit> = stateMutex.withLock {
        try {
            val fromSession = deviceSessionManager.getSession(fromDeviceId)
            val toSession = deviceSessionManager.getSession(toDeviceId)
            
            if (fromSession == null || toSession == null) {
                return Result.failure(Exception("One or both devices not found"))
            }
            
            // Create state snapshot
            val stateSnapshot = JsonObject().apply {
                currentStates.forEach { (stateType, state) ->
                    add(stateType.name, state)
                }
            }
            
            // Create handoff event
            val handoff = SessionHandoff(
                fromDeviceId = fromDeviceId,
                toDeviceId = toDeviceId,
                handoffType = handoffType,
                stateSnapshot = stateSnapshot,
                timestamp = System.currentTimeMillis(),
                completionCallback = null
            )
            
            // Execute handoff
            executeSessionHandoff(handoff)
            
            // Emit handoff event
            _handoffEvents.value = handoff
            
            Log.i(TAG, "Session handoff initiated: $fromDeviceId -> $toDeviceId ($handoffType)")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate session handoff", e)
            Result.failure(e)
        }
    }
    
    /**
     * Synchronize device with current state
     */
    suspend fun synchronizeDevice(deviceId: String): Result<Map<StateType, JsonElement>> {
        try {
            val session = deviceSessionManager.getSession(deviceId)
                ?: return Result.failure(Exception("Device not found"))
            
            val currentStatesMap = currentStates.toMap()
            
            // Send all current states to device
            sendStatesToDevice(deviceId, currentStatesMap)
            
            // Update sync status
            updateDeviceSyncState(deviceId, isConnected = true, pendingChanges = 0)
            
            Log.i(TAG, "Device synchronized: $deviceId with ${currentStatesMap.size} states")
            
            return Result.success(currentStatesMap)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synchronize device: $deviceId", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Handle state conflict resolution
     */
    suspend fun resolveStateConflict(
        conflictingChanges: List<StateChangeEvent>
    ): StateChangeEvent {
        // Sort by device priority and timestamp
        val sortedChanges = conflictingChanges.sortedWith(
            compareBy<StateChangeEvent> { change ->
                deviceSessionManager.getSession(change.deviceId)?.role?.priority ?: Int.MAX_VALUE
            }.thenBy { it.timestamp }
        )
        
        val winningChange = sortedChanges.first()
        
        // Apply winning change
        currentStates[winningChange.stateType] = winningChange.newState
        addToHistory(winningChange.stateType, winningChange)
        
        // Propagate resolution to all devices
        propagateStateChange(winningChange, excludeDeviceId = null)
        
        Log.i(TAG, "State conflict resolved: ${winningChange.stateType} won by ${winningChange.deviceId}")
        
        return winningChange
    }
    
    /**
     * Get state history for a specific type
     */
    fun getStateHistory(stateType: StateType): List<StateChangeEvent> {
        return stateHistory[stateType]?.toList() ?: emptyList()
    }
    
    /**
     * Validate state consistency across devices
     */
    suspend fun validateStateConsistency(): Map<StateType, Boolean> {
        val consistencyMap = mutableMapOf<StateType, Boolean>()
        
        StateType.values().forEach { stateType ->
            val currentState = currentStates[stateType]
            // In a real implementation, you would check this against each device
            // For now, we assume consistency based on recent successful syncs
            consistencyMap[stateType] = true
        }
        
        return consistencyMap
    }
    
    // Private helper methods
    
    private suspend fun startStateSynchronization() {
        while (true) {
            try {
                updateSyncStatus()
                kotlinx.coroutines.delay(SYNC_INTERVAL_MS)
            } catch (e: Exception) {
                Log.e(TAG, "Error in state synchronization", e)
            }
        }
    }
    
    private suspend fun handleSessionEvent(event: DeviceSessionManager.SessionEvent) {
        when (event.type) {
            DeviceSessionManager.SessionEventType.DEVICE_CONNECTED -> {
                // Synchronize new device with current state
                synchronizeDevice(event.deviceId)
            }
            DeviceSessionManager.SessionEventType.DEVICE_DISCONNECTED -> {
                // Update sync status to reflect disconnection
                updateDeviceSyncState(event.deviceId, isConnected = false, pendingChanges = 0)
            }
            DeviceSessionManager.SessionEventType.ROLE_CHANGED -> {
                // Handle role change implications for state management
                val newRole = event.data["newRole"] as? String
                Log.i(TAG, "Device role changed: ${event.deviceId} -> $newRole")
            }
            else -> {
                // Handle other session events as needed
            }
        }
    }
    
    private fun canDeviceUpdateState(deviceId: String, stateType: StateType): Boolean {
        val session = deviceSessionManager.getSession(deviceId) ?: return false
        
        return when (stateType) {
            StateType.SOUNDBOARD_LAYOUT -> session.capabilities.contains(
                DeviceSessionManager.DeviceCapability.LAYOUT_MODIFICATION
            )
            StateType.SOUND_BUTTONS -> session.capabilities.contains(
                DeviceSessionManager.DeviceCapability.SOUND_UPLOAD
            )
            StateType.USER_PREFERENCES -> session.capabilities.contains(
                DeviceSessionManager.DeviceCapability.SETTINGS_ACCESS
            )
            StateType.BACKUP_STATUS -> session.capabilities.contains(
                DeviceSessionManager.DeviceCapability.BACKUP_RESTORE
            )
            StateType.CURRENT_VOLUME, StateType.CONNECTION_STATUS, StateType.ACTIVE_LAYOUT -> {
                // These can be updated by any connected device
                true
            }
        }
    }
    
    private fun addToHistory(stateType: StateType, stateChange: StateChangeEvent) {
        val history = stateHistory.getOrPut(stateType) { mutableListOf() }
        history.add(stateChange)
        
        // Maintain history size limit
        if (history.size > STATE_HISTORY_SIZE) {
            history.removeFirst()
        }
    }
    
    private suspend fun propagateStateChange(
        stateChange: StateChangeEvent,
        excludeDeviceId: String?
    ) {
        val activeSessions = deviceSessionManager.getActiveSessions()
        
        activeSessions.forEach { session ->
            if (session.deviceId != excludeDeviceId) {
                try {
                    sendStateChangeToDevice(session.deviceId, stateChange)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to propagate state to device: ${session.deviceId}", e)
                }
            }
        }
    }
    
    private suspend fun sendStateChangeToDevice(deviceId: String, stateChange: StateChangeEvent) {
        // In a real implementation, this would send the state change through the appropriate transport
        // For now, we'll simulate the operation
        Log.d(TAG, "Sending state change to device $deviceId: ${stateChange.stateType}")
        
        // Update pending operations
        pendingOperations[deviceId + "_" + stateChange.changeId] = stateChange
        
        // Simulate network delay and then mark as completed
        scope.launch {
            kotlinx.coroutines.delay(100) // Simulate network latency
            pendingOperations.remove(deviceId + "_" + stateChange.changeId)
            updateDeviceSyncState(deviceId, isConnected = true, pendingChanges = pendingOperations.size)
        }
    }
    
    private suspend fun sendStatesToDevice(deviceId: String, states: Map<StateType, JsonElement>) {
        // In a real implementation, this would send all states through the appropriate transport
        Log.d(TAG, "Sending ${states.size} states to device $deviceId")
        
        // Simulate the operation
        scope.launch {
            kotlinx.coroutines.delay(200) // Simulate network latency
            updateDeviceSyncState(deviceId, isConnected = true, pendingChanges = 0)
        }
    }
    
    private suspend fun executeSessionHandoff(handoff: SessionHandoff) {
        // Transfer session control and synchronize state
        try {
            // Send complete state snapshot to target device
            val statesMap = mutableMapOf<StateType, JsonElement>()
            handoff.stateSnapshot.entrySet().forEach { (key, value) ->
                try {
                    val stateType = StateType.valueOf(key)
                    statesMap[stateType] = value
                } catch (e: Exception) {
                    Log.w(TAG, "Unknown state type in handoff: $key")
                }
            }
            
            sendStatesToDevice(handoff.toDeviceId, statesMap)
            
            // Update device roles if necessary
            if (handoff.handoffType == HandoffType.ROLE_CHANGE_TRANSFER) {
                deviceSessionManager.changeDeviceRole(
                    handoff.toDeviceId, 
                    DeviceSessionManager.DeviceRole.PRIMARY
                )
            }
            
            Log.i(TAG, "Session handoff executed: ${handoff.fromDeviceId} -> ${handoff.toDeviceId}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute session handoff", e)
        }
    }
    
    private fun updateDeviceSyncState(
        deviceId: String,
        isConnected: Boolean,
        pendingChanges: Int,
        latency: Long = 0L
    ) {
        val currentStatus = _syncStatus.value
        val updatedDeviceStates = currentStatus.deviceSyncStates.toMutableMap()
        
        updatedDeviceStates[deviceId] = DeviceSyncState(
            deviceId = deviceId,
            lastSyncTimestamp = System.currentTimeMillis(),
            pendingChanges = pendingChanges,
            syncLatency = latency,
            isConnected = isConnected
        )
        
        val newStatus = currentStatus.copy(
            lastSyncTimestamp = System.currentTimeMillis(),
            pendingSyncs = updatedDeviceStates.values.sumOf { it.pendingChanges },
            deviceSyncStates = updatedDeviceStates
        )
        
        _syncStatus.value = newStatus
    }
    
    private fun updateSyncStatus() {
        val currentStatus = _syncStatus.value
        val activeSessions = deviceSessionManager.getActiveSessions()
        
        // Update device states based on active sessions
        val updatedDeviceStates = mutableMapOf<String, DeviceSyncState>()
        
        activeSessions.forEach { session ->
            val existingState = currentStatus.deviceSyncStates[session.deviceId]
            updatedDeviceStates[session.deviceId] = existingState?.copy(
                isConnected = true
            ) ?: DeviceSyncState(
                deviceId = session.deviceId,
                lastSyncTimestamp = System.currentTimeMillis(),
                pendingChanges = 0,
                syncLatency = 0L,
                isConnected = true
            )
        }
        
        val newStatus = currentStatus.copy(
            isInSync = updatedDeviceStates.values.all { it.pendingChanges == 0 },
            lastSyncTimestamp = System.currentTimeMillis(),
            pendingSyncs = updatedDeviceStates.values.sumOf { it.pendingChanges },
            deviceSyncStates = updatedDeviceStates
        )
        
        _syncStatus.value = newStatus
    }
    
    private fun generateChangeId(): String {
        return "change_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
} 