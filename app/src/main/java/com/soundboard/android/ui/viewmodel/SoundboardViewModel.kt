package com.soundboard.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundboard.android.data.model.ConnectionHistory
import com.soundboard.android.data.model.SoundButton
import com.soundboard.android.data.model.SoundboardLayout
import com.soundboard.android.data.model.LayoutPreset
import com.soundboard.android.data.repository.SoundboardRepository
import com.soundboard.android.network.ConnectionStatus
import com.soundboard.android.network.api.AudioFile
import com.soundboard.android.ui.component.LayoutTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val serverInfo: String? = null,
    val errorMessage: String? = null,
    val ipAddress: String = "",
    val port: Int = 8080
)

data class SoundboardUiState(
    val soundButtons: List<SoundButton> = emptyList(),
    val activeLayout: SoundboardLayout? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val availableAudioFiles: List<AudioFile> = emptyList(),
    val localAudioFiles: List<AudioFile> = emptyList(),
    val isLoadingLocalFiles: Boolean = false,
    val commonDirectories: List<String> = emptyList(),
    val lastPlayResponse: String? = null,
    val connectionHistory: List<ConnectionHistory> = emptyList(),
    val lastUsedIpAddress: String = "",
    val lastUsedPort: Int = 8080,
    // Directory navigation state
    val currentDirectoryPath: String? = null,
    val currentDirectoryFiles: List<AudioFile> = emptyList(),
    val currentDirectorySubdirectories: List<String> = emptyList(),
    val parentDirectoryPath: String? = null,
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val isInDirectoryBrowseMode: Boolean = false,
    // Playing state for enhanced UI feedback
    val currentlyPlayingButtonId: Int? = null
)

@HiltViewModel
class SoundboardViewModel @Inject constructor(
    private val repository: SoundboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SoundboardUiState())
    val uiState: StateFlow<SoundboardUiState> = _uiState.asStateFlow()
    
    // Get real-time connection status from SocketManager
    val connectionStatus: StateFlow<ConnectionStatus> = repository.getSocketManager().connectionStatus
    val serverInfo: StateFlow<String?> = repository.getSocketManager().serverInfo
    
    // Phase 3.3: Layout Management - LiveData for compatibility with existing screens
    val layouts: StateFlow<List<SoundboardLayout>> = repository.getAllLayouts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val currentLayout: StateFlow<SoundboardLayout?> = flow {
        while (true) {
            emit(repository.getActiveLayout())
            kotlinx.coroutines.delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    
    init {
        initializeData()
        observeSoundButtons()
        observeActiveLayout()
        observePlayResponses()
        observeConnectionHistory()
        loadLastUsedConnection()
        loadCommonDirectories()
    }
    
    private fun initializeData() {
        viewModelScope.launch {
            try {
                repository.initializeDefaultLayout()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to initialize: ${e.message}") }
            }
        }
    }
    
    private fun observeSoundButtons() {
        viewModelScope.launch {
            repository.getAllSoundButtons()
                .catch { e -> 
                    _uiState.update { it.copy(errorMessage = "Failed to load sound buttons: ${e.message}") }
                }
                .collect { buttons ->
                    _uiState.update { it.copy(soundButtons = buttons) }
                }
        }
    }
    
    private fun observeActiveLayout() {
        viewModelScope.launch {
            // Poll for active layout changes
            flow {
                while (true) {
                    emit(repository.getActiveLayout())
                    kotlinx.coroutines.delay(1000) // Check every second
                }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = "Failed to load layout: ${e.message}") }
            }
            .collect { layout ->
                _uiState.update { it.copy(activeLayout = layout) }
            }
        }
    }
    
    fun connectToServer(ipAddress: String, port: Int) {
        viewModelScope.launch {
            try {
                // Connect via repository
                repository.connectToServer(ipAddress, port)
                
                // Add to connection history
                repository.addOrUpdateConnection("Computer", ipAddress, port)
                
                // Test the connection and load audio files
                refreshAudioFiles()
                
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
            }
        }
    }
    
    fun connectViaUSB(context: android.content.Context) {
        viewModelScope.launch {
            try {
                // Connect via USB using ADB reverse port forwarding
                repository.getSocketManager().connectViaUSB(
                    context = context,
                    serverUrl = "http://localhost:8080",
                    onResult = { success, message ->
                        if (success) {
                            android.util.Log.d("SoundboardViewModel", "✅ USB connection successful")
                        } else {
                            android.util.Log.e("SoundboardViewModel", "❌ USB connection failed: $message")
                            _uiState.update { it.copy(errorMessage = message ?: "USB connection failed") }
                        }
                    }
                )
                
                // Set up the API service for HTTP requests
                repository.connectToServer("localhost", 8080)
                
                // Add to connection history
                repository.addOrUpdateConnection("USB Device", "localhost", 8080)
                
                // Wait a moment for connection to establish, then load audio files
                kotlinx.coroutines.delay(1000)
                refreshAudioFiles()
                
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "USB connection failed: ${e.message}") }
            }
        }
    }
    
    fun disconnectFromServer() {
        repository.disconnectFromServer()
    }
    
    fun playSoundButton(soundButton: SoundButton) {
        android.util.Log.d("SoundboardViewModel", "🎵 PLAY BUTTON CLICKED: ID=${soundButton.id}, name=${soundButton.name}, isLocalFile=${soundButton.isLocalFile}")
        
        if (connectionStatus.value !is ConnectionStatus.Connected) {
            android.util.Log.e("SoundboardViewModel", "❌ NOT CONNECTED: Connection status = ${connectionStatus.value}")
            _uiState.update { it.copy(errorMessage = "Not connected to server") }
            return
        }
        
        android.util.Log.d("SoundboardViewModel", "✅ CONNECTION OK: Proceeding with sound playback")
        
        // Set playing state immediately for visual feedback
        _uiState.update { it.copy(currentlyPlayingButtonId = soundButton.id) }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("SoundboardViewModel", "🔄 CALLING REPOSITORY.playSound()")
                val result = repository.playSound(soundButton)
                android.util.Log.d("SoundboardViewModel", "✅ REPOSITORY RESULT: $result")
                _uiState.update { it.copy(errorMessage = null) }
                
                // Clear playing state after a short duration (typical sound duration)
                kotlinx.coroutines.delay(2000) // 2 seconds visual feedback
                _uiState.update { 
                    if (it.currentlyPlayingButtonId == soundButton.id) {
                        it.copy(currentlyPlayingButtonId = null)
                    } else it
                }
            } catch (e: Exception) {
                android.util.Log.e("SoundboardViewModel", "❌ VIEWMODEL ERROR: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        errorMessage = "Failed to play sound: ${e.message}",
                        currentlyPlayingButtonId = null
                    ) 
                }
            }
        }
    }
    
    fun addSoundButton(name: String, filePath: String, positionX: Int, positionY: Int, color: String = "#2196F3", iconName: String = "music_note", isLocalFile: Boolean = false) {
        viewModelScope.launch {
            try {
                val soundButton = SoundButton(
                    name = name,
                    filePath = filePath,
                    isLocalFile = isLocalFile,
                    positionX = positionX,
                    positionY = positionY,
                    color = color,
                    iconName = iconName,
                    volume = 1.0f
                )
                repository.insertSoundButton(soundButton)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add sound button: ${e.message}") }
            }
        }
    }
    
    fun updateSoundButton(soundButton: SoundButton) {
        viewModelScope.launch {
            try {
                repository.updateSoundButton(soundButton.copy(updatedAt = System.currentTimeMillis()))
                
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update sound button: ${e.message}") }
            }
        }
    }
    
    fun updateSoundButtonVolume(soundButton: SoundButton, volume: Float) {
        viewModelScope.launch {
            try {
                val updatedButton = soundButton.copy(
                    volume = volume.coerceIn(0f, 1f),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateSoundButton(updatedButton)
                
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update volume: ${e.message}") }
            }
        }
    }
    
    fun applyGlobalVolumeMultiplier(multiplier: Float) {
        viewModelScope.launch {
            try {
                val currentButtons = _uiState.value.soundButtons
                currentButtons.forEach { button ->
                    val newVolume = (button.volume * multiplier).coerceIn(0f, 1f)
                    val updatedButton = button.copy(
                        volume = newVolume,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateSoundButton(updatedButton)
                }
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to apply global volume: ${e.message}") }
            }
        }
    }
    
    fun normalizeAllVolumes() {
        viewModelScope.launch {
            try {
                val currentButtons = _uiState.value.soundButtons
                if (currentButtons.isEmpty()) return@launch
                
                // Calculate normalization based on filename patterns and current volumes
                currentButtons.forEach { button ->
                    val normalizedVolume = calculateNormalizedVolume(button)
                    val updatedButton = button.copy(
                        volume = normalizedVolume,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateSoundButton(updatedButton)
                }
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to normalize volumes: ${e.message}") }
            }
        }
    }
    
    fun resetAllVolumes() {
        viewModelScope.launch {
            try {
                val currentButtons = _uiState.value.soundButtons
                currentButtons.forEach { button ->
                    val updatedButton = button.copy(
                        volume = 1.0f,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateSoundButton(updatedButton)
                }
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to reset volumes: ${e.message}") }
            }
        }
    }
    
    private fun calculateNormalizedVolume(button: SoundButton): Float {
        val fileName = button.name.lowercase()
        val filePath = button.filePath.lowercase()
        
        return when {
            // Loud files - reduce volume
            fileName.contains("airhorn") || fileName.contains("loud") || 
            fileName.contains("scream") || fileName.contains("explosion") -> 0.4f
            
            // Quiet files - increase volume
            fileName.contains("whisper") || fileName.contains("quiet") || 
            fileName.contains("soft") || fileName.contains("ambient") -> 0.8f
            
            // Voice/speech files - moderate boost
            fileName.contains("voice") || fileName.contains("speech") || 
            fileName.contains("talk") || filePath.contains("voice") -> 0.7f
            
            // Music files - moderate level
            fileName.contains("music") || fileName.contains("song") || 
            fileName.contains("melody") || filePath.contains("music") -> 0.6f
            
            // Effect files - standard level
            fileName.contains("effect") || fileName.contains("sound") || 
            fileName.contains("fx") -> 0.65f
            
            // Alert/notification files - higher level
            fileName.contains("alert") || fileName.contains("notification") || 
            fileName.contains("alarm") || fileName.contains("bell") -> 0.75f
            
            // Default normalization
            else -> 0.65f
        }
    }
    
    fun deleteSoundButton(soundButton: SoundButton) {
        viewModelScope.launch {
            try {
                repository.deleteSoundButton(soundButton)
                
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete sound button: ${e.message}") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun refreshAudioFiles() {
        viewModelScope.launch {
            try {
                val result = repository.getAvailableAudioFiles()
                result.fold(
                    onSuccess = { audioFiles ->
                        _uiState.update { it.copy(availableAudioFiles = audioFiles) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = "Failed to load audio files: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error loading audio files: ${e.message}") }
            }
        }
    }
    
    fun refreshLocalAudioFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocalFiles = true) }
            try {
                val result = repository.getLocalAudioFiles()
                result.fold(
                    onSuccess = { audioFiles ->
                        _uiState.update { 
                            it.copy(
                                localAudioFiles = audioFiles,
                                isLoadingLocalFiles = false
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                errorMessage = "Failed to load local audio files: ${error.message}",
                                isLoadingLocalFiles = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        errorMessage = "Error loading local audio files: ${e.message}",
                        isLoadingLocalFiles = false
                    ) 
                }
            }
        }
    }
    
    fun browseDirectory(directoryPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocalFiles = true) }
            try {
                val result = repository.getDirectoryContent(directoryPath)
                result.fold(
                    onSuccess = { content ->
                        val breadcrumbs = repository.getBreadcrumbs(directoryPath)
                        _uiState.update { 
                            it.copy(
                                currentDirectoryPath = directoryPath,
                                currentDirectoryFiles = content.audioFiles,
                                currentDirectorySubdirectories = content.subdirectories,
                                parentDirectoryPath = content.parentDirectory,
                                breadcrumbs = breadcrumbs,
                                isInDirectoryBrowseMode = true,
                                localAudioFiles = content.audioFiles, // For backward compatibility
                                isLoadingLocalFiles = false
                            ) 
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                errorMessage = "Failed to browse directory: ${error.message}",
                                isLoadingLocalFiles = false
                            ) 
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        errorMessage = "Error browsing directory: ${e.message}",
                        isLoadingLocalFiles = false
                    ) 
                }
            }
        }
    }
    
    fun navigateToParentDirectory() {
        val parentPath = _uiState.value.parentDirectoryPath
        if (parentPath != null) {
            browseDirectory(parentPath)
        }
    }
    
    fun navigateToBreadcrumb(directoryPath: String) {
        browseDirectory(directoryPath)
    }
    
    fun exitDirectoryBrowseMode() {
        _uiState.update { 
            it.copy(
                isInDirectoryBrowseMode = false,
                currentDirectoryPath = null,
                currentDirectoryFiles = emptyList(),
                currentDirectorySubdirectories = emptyList(),
                parentDirectoryPath = null,
                breadcrumbs = emptyList()
            ) 
        }
        // Refresh the main file list
        refreshLocalAudioFiles()
    }
    
    private fun loadCommonDirectories() {
        viewModelScope.launch {
            try {
                val directories = repository.getCommonAudioDirectories()
                _uiState.update { it.copy(commonDirectories = directories) }
            } catch (e: Exception) {
                // Don't show error for this, just use empty list
                _uiState.update { it.copy(commonDirectories = emptyList()) }
            }
        }
    }
    
    private fun observePlayResponses() {
        viewModelScope.launch {
            repository.getSocketManager().playResponses
                .filterNotNull()
                .collect { response ->
                    val message = if (response.status == "success") {
                        "Playing ${response.buttonId}"
                    } else {
                        "Error: ${response.message}"
                    }
                    _uiState.update { it.copy(lastPlayResponse = message) }
                    
                    // Clear response after showing it
                    kotlinx.coroutines.delay(3000)
                    repository.getSocketManager().clearPlayResponse()
                    _uiState.update { it.copy(lastPlayResponse = null) }
                }
        }
    }
    
    private fun observeConnectionHistory() {
        viewModelScope.launch {
            repository.getAllConnectionHistory()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load connection history: ${e.message}") }
                }
                .collect { history ->
                    _uiState.update { it.copy(connectionHistory = history) }
                }
        }
    }
    
    private fun loadLastUsedConnection() {
        viewModelScope.launch {
            val lastUsedConnection = repository.getLastUsedConnection()
            if (lastUsedConnection != null) {
                _uiState.update { it.copy(lastUsedIpAddress = lastUsedConnection.ipAddress, lastUsedPort = lastUsedConnection.port) }
            }
        }
    }
    
    // ========================================
    // Phase 3.3: Layout Management Functions
    // ========================================
    
    fun createLayout(name: String, preset: LayoutPreset) {
        viewModelScope.launch {
            try {
                val layout = SoundboardLayout(
                    name = name,
                    description = preset.description,
                    gridColumns = preset.columns,
                    gridRows = preset.rows,
                    layoutPreset = preset,
                    isTemplate = false,
                    maxButtons = preset.columns * preset.rows
                )
                repository.insertLayout(layout)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to create layout: ${e.message}") }
            }
        }
    }
    
    fun createFromTemplate(template: LayoutTemplate) {
        viewModelScope.launch {
            try {
                val layout = SoundboardLayout(
                    name = template.name,
                    description = template.description,
                    gridColumns = template.preset.columns,
                    gridRows = template.preset.rows,
                    layoutPreset = template.preset,
                    isTemplate = false,
                    templateCategory = template.category.name.lowercase(),
                    accentColor = template.accentColor,
                    maxButtons = template.preset.columns * template.preset.rows
                )
                repository.insertLayout(layout)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to create layout from template: ${e.message}") }
            }
        }
    }
    
    fun switchToLayout(layout: SoundboardLayout) {
        viewModelScope.launch {
            try {
                // Use the atomic transaction method to switch layouts
                repository.switchActiveLayout(layout.id)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to switch layout: ${e.message}") }
            }
        }
    }
    
    fun duplicateLayout(layout: SoundboardLayout) {
        viewModelScope.launch {
            try {
                val duplicatedLayout = layout.copy(
                    id = 0, // Let Room auto-generate new ID
                    name = "${layout.name} (Copy)",
                    isActive = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertLayout(duplicatedLayout)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to duplicate layout: ${e.message}") }
            }
        }
    }
    
    fun deleteLayout(layout: SoundboardLayout) {
        viewModelScope.launch {
            try {
                if (layout.isActive) {
                    _uiState.update { it.copy(errorMessage = "Cannot delete active layout") }
                    return@launch
                }
                
                // TODO: Delete sound buttons for layout when layout relationship is implemented
                repository.deleteLayout(layout)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete layout: ${e.message}") }
            }
        }
    }
    
    fun updateLayout(layout: SoundboardLayout) {
        viewModelScope.launch {
            try {
                repository.updateLayout(layout.copy(updatedAt = System.currentTimeMillis()))
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update layout: ${e.message}") }
            }
        }
    }
    
    fun exportLayout(layout: SoundboardLayout): String? {
        return try {
            // TODO: Implement JSON export functionality
            // This would serialize the layout and associated buttons to JSON
            // For now, return a placeholder
            "Layout export functionality coming soon"
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Failed to export layout: ${e.message}") }
            null
        }
    }
    
    fun importLayout(jsonData: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement JSON import functionality
                // This would parse JSON and create layout + buttons
                _uiState.update { it.copy(errorMessage = "Layout import functionality coming soon") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to import layout: ${e.message}") }
            }
        }
    }
    
} 