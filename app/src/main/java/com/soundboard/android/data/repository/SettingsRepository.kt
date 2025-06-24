package com.soundboard.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("soundboard_settings", Context.MODE_PRIVATE)
    
    // Theme Settings
    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    
    private val _colorScheme = MutableStateFlow(getColorScheme())
    val colorScheme: StateFlow<String> = _colorScheme.asStateFlow()
    
    // Appearance Settings
    private val _buttonCornerRadius = MutableStateFlow(getButtonCornerRadius())
    val buttonCornerRadius: StateFlow<Float> = _buttonCornerRadius.asStateFlow()
    
    private val _buttonSpacing = MutableStateFlow(getButtonSpacing())
    val buttonSpacing: StateFlow<Float> = _buttonSpacing.asStateFlow()
    
    private val _compactLayout = MutableStateFlow(getCompactLayout())
    val compactLayout: StateFlow<Boolean> = _compactLayout.asStateFlow()
    
    private val _animationsEnabled = MutableStateFlow(getAnimationsEnabled())
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled.asStateFlow()
    
    private val _showButtonLabels = MutableStateFlow(getShowButtonLabels())
    val showButtonLabels: StateFlow<Boolean> = _showButtonLabels.asStateFlow()
    
    // Audio Settings
    private val _sampleRate = MutableStateFlow(getSampleRate())
    val sampleRate: StateFlow<Int> = _sampleRate.asStateFlow()
    
    private val _bufferSize = MutableStateFlow(getBufferSize())
    val bufferSize: StateFlow<String> = _bufferSize.asStateFlow()
    
    private val _maxConcurrentSounds = MutableStateFlow(getMaxConcurrentSounds())
    val maxConcurrentSounds: StateFlow<Int> = _maxConcurrentSounds.asStateFlow()
    
    private val _audioQuality = MutableStateFlow(getAudioQuality())
    val audioQuality: StateFlow<Float> = _audioQuality.asStateFlow()
    
    // Connection Settings
    private val _autoReconnect = MutableStateFlow(getAutoReconnect())
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()
    
    private val _connectionTimeout = MutableStateFlow(getConnectionTimeout())
    val connectionTimeout: StateFlow<Float> = _connectionTimeout.asStateFlow()
    
    private val _lowLatencyMode = MutableStateFlow(getLowLatencyMode())
    val lowLatencyMode: StateFlow<Boolean> = _lowLatencyMode.asStateFlow()
    
    // Developer Settings
    private val _debugLogging = MutableStateFlow(getDebugLogging())
    val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()
    
    private val _showConnectionStatus = MutableStateFlow(getShowConnectionStatus())
    val showConnectionStatus: StateFlow<Boolean> = _showConnectionStatus.asStateFlow()
    
    private val _analyticsEnabled = MutableStateFlow(getAnalyticsEnabled())
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()
    
    private val _crashReporting = MutableStateFlow(getCrashReporting())
    val crashReporting: StateFlow<Boolean> = _crashReporting.asStateFlow()
    
    // Download Settings
    private val _downloadLocation = MutableStateFlow(getDownloadLocation())
    val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()
    
    // System Settings
    private val _hapticFeedback = MutableStateFlow(getHapticFeedback())
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()
    
    private val _keepScreenOn = MutableStateFlow(getKeepScreenOn())
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()
    
    // UI Settings
    private val _minimizedMode = MutableStateFlow(getMinimizedMode())
    val minimizedMode: StateFlow<Boolean> = _minimizedMode.asStateFlow()
    
    // NEW: Server Connection Settings
    private val _defaultServerHost = MutableStateFlow(getDefaultServerHost())
    val defaultServerHost: StateFlow<String> = _defaultServerHost.asStateFlow()
    
    private val _defaultServerPort = MutableStateFlow(getDefaultServerPort())
    val defaultServerPort: StateFlow<Int> = _defaultServerPort.asStateFlow()
    
    private val _rememberServerConnections = MutableStateFlow(getRememberServerConnections())
    val rememberServerConnections: StateFlow<Boolean> = _rememberServerConnections.asStateFlow()
    
    private val _autoConnectToLastServer = MutableStateFlow(getAutoConnectToLastServer())
    val autoConnectToLastServer: StateFlow<Boolean> = _autoConnectToLastServer.asStateFlow()
    
    private val _serverConnectionHistory = MutableStateFlow(getServerConnectionHistory())
    val serverConnectionHistory: StateFlow<List<String>> = _serverConnectionHistory.asStateFlow()
    
    // NEW: Path Management Settings
    private val _defaultLocalAudioPath = MutableStateFlow(getDefaultLocalAudioPath())
    val defaultLocalAudioPath: StateFlow<String> = _defaultLocalAudioPath.asStateFlow()
    
    private val _preserveFilePaths = MutableStateFlow(getPreserveFilePaths())
    val preserveFilePaths: StateFlow<Boolean> = _preserveFilePaths.asStateFlow()
    
    private val _autoResolveLocalPaths = MutableStateFlow(getAutoResolveLocalPaths())
    val autoResolveLocalPaths: StateFlow<Boolean> = _autoResolveLocalPaths.asStateFlow()
    
    private val _pathResolutionStrategy = MutableStateFlow(getPathResolutionStrategy())
    val pathResolutionStrategy: StateFlow<String> = _pathResolutionStrategy.asStateFlow()
    
    // NEW: Backup & Sync Settings
    private val _autoBackupEnabled = MutableStateFlow(getAutoBackupEnabled())
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()
    
    private val _autoBackupFrequency = MutableStateFlow(getAutoBackupFrequency())
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency.asStateFlow()
    
    private val _cloudBackupEnabled = MutableStateFlow(getCloudBackupEnabled())
    val cloudBackupEnabled: StateFlow<Boolean> = _cloudBackupEnabled.asStateFlow()
    
    private val _backupLocation = MutableStateFlow(getBackupLocation())
    val backupLocation: StateFlow<String> = _backupLocation.asStateFlow()
    
    private val _lastBackupTimestamp = MutableStateFlow(getLastBackupTimestamp())
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()
    
    private val _backupIncludeSettings = MutableStateFlow(getBackupIncludeSettings())
    val backupIncludeSettings: StateFlow<Boolean> = _backupIncludeSettings.asStateFlow()
    
    private val _backupIncludeLayouts = MutableStateFlow(getBackupIncludeLayouts())
    val backupIncludeLayouts: StateFlow<Boolean> = _backupIncludeLayouts.asStateFlow()
    
    private val _backupIncludeSoundButtons = MutableStateFlow(getBackupIncludeSoundButtons())
    val backupIncludeSoundButtons: StateFlow<Boolean> = _backupIncludeSoundButtons.asStateFlow()
    
    private val _backupIncludeLocalFiles = MutableStateFlow(getBackupIncludeLocalFiles())
    val backupIncludeLocalFiles: StateFlow<Boolean> = _backupIncludeLocalFiles.asStateFlow()
    
    // NEW: Profile Settings
    private val _currentProfileName = MutableStateFlow(getCurrentProfileName())
    val currentProfileName: StateFlow<String> = _currentProfileName.asStateFlow()
    
    private val _profileSyncEnabled = MutableStateFlow(getProfileSyncEnabled())
    val profileSyncEnabled: StateFlow<Boolean> = _profileSyncEnabled.asStateFlow()

    // Theme Settings
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }
    
    fun getThemeMode(): String = prefs.getString("theme_mode", "System Default") ?: "System Default"
    
    fun setColorScheme(scheme: String) {
        prefs.edit().putString("color_scheme", scheme).apply()
        _colorScheme.value = scheme
    }
    
    fun getColorScheme(): String = prefs.getString("color_scheme", "Default Blue") ?: "Default Blue"
    
    // Appearance Settings
    fun setButtonCornerRadius(radius: Float) {
        prefs.edit().putFloat("button_corner_radius", radius).apply()
        _buttonCornerRadius.value = radius
    }
    
    fun getButtonCornerRadius(): Float = prefs.getFloat("button_corner_radius", 12f)
    
    fun setButtonSpacing(spacing: Float) {
        prefs.edit().putFloat("button_spacing", spacing).apply()
        _buttonSpacing.value = spacing
    }
    
    fun getButtonSpacing(): Float = prefs.getFloat("button_spacing", 8f)
    
    fun setCompactLayout(enabled: Boolean) {
        prefs.edit().putBoolean("compact_layout", enabled).apply()
        _compactLayout.value = enabled
    }
    
    fun getCompactLayout(): Boolean = prefs.getBoolean("compact_layout", false)
    
    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("animations_enabled", enabled).apply()
        _animationsEnabled.value = enabled
    }
    
    fun getAnimationsEnabled(): Boolean = prefs.getBoolean("animations_enabled", true)
    
    fun setShowButtonLabels(show: Boolean) {
        prefs.edit().putBoolean("show_button_labels", show).apply()
        _showButtonLabels.value = show
    }
    
    fun getShowButtonLabels(): Boolean = prefs.getBoolean("show_button_labels", true)
    
    // Audio Settings
    fun setSampleRate(rate: Int) {
        prefs.edit().putInt("sample_rate", rate).apply()
        _sampleRate.value = rate
    }
    
    fun getSampleRate(): Int = prefs.getInt("sample_rate", 44100)
    
    fun setBufferSize(size: String) {
        prefs.edit().putString("buffer_size", size).apply()
        _bufferSize.value = size
    }
    
    fun getBufferSize(): String = prefs.getString("buffer_size", "Medium") ?: "Medium"
    
    fun setMaxConcurrentSounds(max: Int) {
        prefs.edit().putInt("max_concurrent_sounds", max).apply()
        _maxConcurrentSounds.value = max
    }
    
    fun getMaxConcurrentSounds(): Int = prefs.getInt("max_concurrent_sounds", 4)
    
    fun setAudioQuality(quality: Float) {
        prefs.edit().putFloat("audio_quality", quality).apply()
        _audioQuality.value = quality
    }
    
    fun getAudioQuality(): Float = prefs.getFloat("audio_quality", 80f)
    
    // Connection Settings
    fun setAutoReconnect(enabled: Boolean) {
        prefs.edit().putBoolean("auto_reconnect", enabled).apply()
        _autoReconnect.value = enabled
    }
    
    fun getAutoReconnect(): Boolean = prefs.getBoolean("auto_reconnect", true)
    
    fun setConnectionTimeout(timeout: Float) {
        prefs.edit().putFloat("connection_timeout", timeout).apply()
        _connectionTimeout.value = timeout
    }
    
    fun getConnectionTimeout(): Float = prefs.getFloat("connection_timeout", 10f)
    
    fun setLowLatencyMode(enabled: Boolean) {
        prefs.edit().putBoolean("low_latency_mode", enabled).apply()
        _lowLatencyMode.value = enabled
    }
    
    fun getLowLatencyMode(): Boolean = prefs.getBoolean("low_latency_mode", false)
    
    // Developer Settings
    fun setDebugLogging(enabled: Boolean) {
        prefs.edit().putBoolean("debug_logging", enabled).apply()
        _debugLogging.value = enabled
    }
    
    fun getDebugLogging(): Boolean = prefs.getBoolean("debug_logging", false)
    
    fun setShowConnectionStatus(show: Boolean) {
        prefs.edit().putBoolean("show_connection_status", show).apply()
        _showConnectionStatus.value = show
    }
    
    fun getShowConnectionStatus(): Boolean = prefs.getBoolean("show_connection_status", false)
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("analytics_enabled", enabled).apply()
        _analyticsEnabled.value = enabled
    }
    
    fun getAnalyticsEnabled(): Boolean = prefs.getBoolean("analytics_enabled", true)
    
    fun setCrashReporting(enabled: Boolean) {
        prefs.edit().putBoolean("crash_reporting", enabled).apply()
        _crashReporting.value = enabled
    }
    
    fun getCrashReporting(): Boolean = prefs.getBoolean("crash_reporting", true)
    
    // System Settings
    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _hapticFeedback.value = enabled
    }
    
    fun getHapticFeedback(): Boolean = prefs.getBoolean("haptic_feedback", true)
    
    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean("keep_screen_on", enabled).apply()
        _keepScreenOn.value = enabled
    }
    
    fun getKeepScreenOn(): Boolean = prefs.getBoolean("keep_screen_on", false)
    
    // Download Settings
    fun setDownloadLocation(location: String) {
        prefs.edit().putString("download_location", location).apply()
        _downloadLocation.value = location
    }
    
    fun getDownloadLocation(): String = prefs.getString("download_location", "Downloads") ?: "Downloads"
    
    // UI Settings
    fun setMinimizedMode(enabled: Boolean) {
        prefs.edit().putBoolean("minimized_mode", enabled).apply()
        _minimizedMode.value = enabled
    }
    
    fun getMinimizedMode(): Boolean = prefs.getBoolean("minimized_mode", false)
    
    // NEW: Server Connection Settings
    fun setDefaultServerHost(host: String) {
        prefs.edit().putString("default_server_host", host).apply()
        _defaultServerHost.value = host
    }
    
    fun getDefaultServerHost(): String = prefs.getString("default_server_host", "localhost") ?: "localhost"
    
    fun setDefaultServerPort(port: Int) {
        prefs.edit().putInt("default_server_port", port).apply()
        _defaultServerPort.value = port
    }
    
    fun getDefaultServerPort(): Int = prefs.getInt("default_server_port", 8080)
    
    fun setRememberServerConnections(remember: Boolean) {
        prefs.edit().putBoolean("remember_server_connections", remember).apply()
        _rememberServerConnections.value = remember
    }
    
    fun getRememberServerConnections(): Boolean = prefs.getBoolean("remember_server_connections", true)
    
    fun setAutoConnectToLastServer(autoConnect: Boolean) {
        prefs.edit().putBoolean("auto_connect_to_last_server", autoConnect).apply()
        _autoConnectToLastServer.value = autoConnect
    }
    
    fun getAutoConnectToLastServer(): Boolean = prefs.getBoolean("auto_connect_to_last_server", true)
    
    fun setServerConnectionHistory(history: List<String>) {
        val historyString = history.joinToString(";")
        prefs.edit().putString("server_connection_history", historyString).apply()
        _serverConnectionHistory.value = history
    }
    
    fun getServerConnectionHistory(): List<String> {
        val historyString = prefs.getString("server_connection_history", "") ?: ""
        return if (historyString.isBlank()) emptyList() else historyString.split(";")
    }
    
    fun addServerToHistory(serverUrl: String) {
        val currentHistory = getServerConnectionHistory().toMutableList()
        currentHistory.remove(serverUrl) // Remove if already exists
        currentHistory.add(0, serverUrl) // Add to beginning
        val limitedHistory = currentHistory.take(10) // Keep only last 10
        setServerConnectionHistory(limitedHistory)
    }
    
    // NEW: Path Management Settings
    fun setDefaultLocalAudioPath(path: String) {
        prefs.edit().putString("default_local_audio_path", path).apply()
        _defaultLocalAudioPath.value = path
    }
    
    fun getDefaultLocalAudioPath(): String = prefs.getString("default_local_audio_path", "Music") ?: "Music"
    
    fun setPreserveFilePaths(preserve: Boolean) {
        prefs.edit().putBoolean("preserve_file_paths", preserve).apply()
        _preserveFilePaths.value = preserve
    }
    
    fun getPreserveFilePaths(): Boolean = prefs.getBoolean("preserve_file_paths", true)
    
    fun setAutoResolveLocalPaths(autoResolve: Boolean) {
        prefs.edit().putBoolean("auto_resolve_local_paths", autoResolve).apply()
        _autoResolveLocalPaths.value = autoResolve
    }
    
    fun getAutoResolveLocalPaths(): Boolean = prefs.getBoolean("auto_resolve_local_paths", true)
    
    fun setPathResolutionStrategy(strategy: String) {
        prefs.edit().putString("path_resolution_strategy", strategy).apply()
        _pathResolutionStrategy.value = strategy
    }
    
    fun getPathResolutionStrategy(): String = prefs.getString("path_resolution_strategy", "Smart") ?: "Smart"
    
    // NEW: Backup & Sync Settings
    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
        _autoBackupEnabled.value = enabled
    }
    
    fun getAutoBackupEnabled(): Boolean = prefs.getBoolean("auto_backup_enabled", true)
    
    fun setAutoBackupFrequency(frequency: String) {
        prefs.edit().putString("auto_backup_frequency", frequency).apply()
        _autoBackupFrequency.value = frequency
    }
    
    fun getAutoBackupFrequency(): String = prefs.getString("auto_backup_frequency", "Daily") ?: "Daily"
    
    fun setCloudBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_backup_enabled", enabled).apply()
        _cloudBackupEnabled.value = enabled
    }
    
    fun getCloudBackupEnabled(): Boolean = prefs.getBoolean("cloud_backup_enabled", false)
    
    fun setBackupLocation(location: String) {
        prefs.edit().putString("backup_location", location).apply()
        _backupLocation.value = location
    }
    
    fun getBackupLocation(): String = prefs.getString("backup_location", "Local") ?: "Local"
    
    fun setLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong("last_backup_timestamp", timestamp).apply()
        _lastBackupTimestamp.value = timestamp
    }
    
    fun getLastBackupTimestamp(): Long = prefs.getLong("last_backup_timestamp", 0L)
    
    fun setBackupIncludeSettings(include: Boolean) {
        prefs.edit().putBoolean("backup_include_settings", include).apply()
        _backupIncludeSettings.value = include
    }
    
    fun getBackupIncludeSettings(): Boolean = prefs.getBoolean("backup_include_settings", true)
    
    fun setBackupIncludeLayouts(include: Boolean) {
        prefs.edit().putBoolean("backup_include_layouts", include).apply()
        _backupIncludeLayouts.value = include
    }
    
    fun getBackupIncludeLayouts(): Boolean = prefs.getBoolean("backup_include_layouts", true)
    
    fun setBackupIncludeSoundButtons(include: Boolean) {
        prefs.edit().putBoolean("backup_include_sound_buttons", include).apply()
        _backupIncludeSoundButtons.value = include
    }
    
    fun getBackupIncludeSoundButtons(): Boolean = prefs.getBoolean("backup_include_sound_buttons", true)
    
    fun setBackupIncludeLocalFiles(include: Boolean) {
        prefs.edit().putBoolean("backup_include_local_files", include).apply()
        _backupIncludeLocalFiles.value = include
    }
    
    fun getBackupIncludeLocalFiles(): Boolean = prefs.getBoolean("backup_include_local_files", false)
    
    // NEW: Profile Settings
    fun setCurrentProfileName(name: String) {
        prefs.edit().putString("current_profile_name", name).apply()
        _currentProfileName.value = name
    }
    
    fun getCurrentProfileName(): String = prefs.getString("current_profile_name", "Default") ?: "Default"
    
    fun setProfileSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("profile_sync_enabled", enabled).apply()
        _profileSyncEnabled.value = enabled
    }
    
    fun getProfileSyncEnabled(): Boolean = prefs.getBoolean("profile_sync_enabled", false)
    
    // Get all settings as a map for backup/export
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            // Theme Settings
            "theme_mode" to getThemeMode(),
            "color_scheme" to getColorScheme(),
            
            // Appearance Settings
            "button_corner_radius" to getButtonCornerRadius(),
            "button_spacing" to getButtonSpacing(),
            "compact_layout" to getCompactLayout(),
            "animations_enabled" to getAnimationsEnabled(),
            "show_button_labels" to getShowButtonLabels(),
            
            // Audio Settings
            "sample_rate" to getSampleRate(),
            "buffer_size" to getBufferSize(),
            "max_concurrent_sounds" to getMaxConcurrentSounds(),
            "audio_quality" to getAudioQuality(),
            
            // Connection Settings
            "auto_reconnect" to getAutoReconnect(),
            "connection_timeout" to getConnectionTimeout(),
            "low_latency_mode" to getLowLatencyMode(),
            
            // Developer Settings
            "debug_logging" to getDebugLogging(),
            "show_connection_status" to getShowConnectionStatus(),
            "analytics_enabled" to getAnalyticsEnabled(),
            "crash_reporting" to getCrashReporting(),
            
            // System Settings
            "haptic_feedback" to getHapticFeedback(),
            "keep_screen_on" to getKeepScreenOn(),
            "download_location" to getDownloadLocation(),
            "minimized_mode" to getMinimizedMode(),
            
            // Server Connection Settings
            "default_server_host" to getDefaultServerHost(),
            "default_server_port" to getDefaultServerPort(),
            "remember_server_connections" to getRememberServerConnections(),
            "auto_connect_to_last_server" to getAutoConnectToLastServer(),
            "server_connection_history" to getServerConnectionHistory(),
            
            // Path Management Settings
            "default_local_audio_path" to getDefaultLocalAudioPath(),
            "preserve_file_paths" to getPreserveFilePaths(),
            "auto_resolve_local_paths" to getAutoResolveLocalPaths(),
            "path_resolution_strategy" to getPathResolutionStrategy(),
            
            // Backup & Sync Settings
            "auto_backup_enabled" to getAutoBackupEnabled(),
            "auto_backup_frequency" to getAutoBackupFrequency(),
            "cloud_backup_enabled" to getCloudBackupEnabled(),
            "backup_location" to getBackupLocation(),
            "last_backup_timestamp" to getLastBackupTimestamp(),
            "backup_include_settings" to getBackupIncludeSettings(),
            "backup_include_layouts" to getBackupIncludeLayouts(),
            "backup_include_sound_buttons" to getBackupIncludeSoundButtons(),
            "backup_include_local_files" to getBackupIncludeLocalFiles(),
            
            // Profile Settings
            "current_profile_name" to getCurrentProfileName(),
            "profile_sync_enabled" to getProfileSyncEnabled()
        )
    }
    
    // Apply settings from a map (for restore/import)
    fun applySettings(settings: Map<String, Any>) {
        settings.forEach { (key, value) ->
            when (key) {
                // Theme Settings
                "theme_mode" -> if (value is String) setThemeMode(value)
                "color_scheme" -> if (value is String) setColorScheme(value)
                
                // Appearance Settings
                "button_corner_radius" -> when (value) {
                    is Double -> setButtonCornerRadius(value.toFloat())
                    is Float -> setButtonCornerRadius(value)
                    is Int -> setButtonCornerRadius(value.toFloat())
                }
                "button_spacing" -> when (value) {
                    is Double -> setButtonSpacing(value.toFloat())
                    is Float -> setButtonSpacing(value)
                    is Int -> setButtonSpacing(value.toFloat())
                }
                "compact_layout" -> if (value is Boolean) setCompactLayout(value)
                "animations_enabled" -> if (value is Boolean) setAnimationsEnabled(value)
                "show_button_labels" -> if (value is Boolean) setShowButtonLabels(value)
                
                // Audio Settings
                "sample_rate" -> if (value is Int) setSampleRate(value)
                "buffer_size" -> if (value is String) setBufferSize(value)
                "max_concurrent_sounds" -> if (value is Int) setMaxConcurrentSounds(value)
                "audio_quality" -> when (value) {
                    is Double -> setAudioQuality(value.toFloat())
                    is Float -> setAudioQuality(value)
                    is Int -> setAudioQuality(value.toFloat())
                }
                
                // Connection Settings
                "auto_reconnect" -> if (value is Boolean) setAutoReconnect(value)
                "connection_timeout" -> when (value) {
                    is Double -> setConnectionTimeout(value.toFloat())
                    is Float -> setConnectionTimeout(value)
                    is Int -> setConnectionTimeout(value.toFloat())
                }
                "low_latency_mode" -> if (value is Boolean) setLowLatencyMode(value)
                
                // Developer Settings
                "debug_logging" -> if (value is Boolean) setDebugLogging(value)
                "show_connection_status" -> if (value is Boolean) setShowConnectionStatus(value)
                "analytics_enabled" -> if (value is Boolean) setAnalyticsEnabled(value)
                "crash_reporting" -> if (value is Boolean) setCrashReporting(value)
                
                // System Settings
                "haptic_feedback" -> if (value is Boolean) setHapticFeedback(value)
                "keep_screen_on" -> if (value is Boolean) setKeepScreenOn(value)
                "download_location" -> if (value is String) setDownloadLocation(value)
                "minimized_mode" -> if (value is Boolean) setMinimizedMode(value)
                
                // Server Connection Settings
                "default_server_host" -> if (value is String) setDefaultServerHost(value)
                "default_server_port" -> if (value is Int) setDefaultServerPort(value)
                "remember_server_connections" -> if (value is Boolean) setRememberServerConnections(value)
                "auto_connect_to_last_server" -> if (value is Boolean) setAutoConnectToLastServer(value)
                "server_connection_history" -> if (value is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    setServerConnectionHistory(value as List<String>)
                }
                
                // Path Management Settings
                "default_local_audio_path" -> if (value is String) setDefaultLocalAudioPath(value)
                "preserve_file_paths" -> if (value is Boolean) setPreserveFilePaths(value)
                "auto_resolve_local_paths" -> if (value is Boolean) setAutoResolveLocalPaths(value)
                "path_resolution_strategy" -> if (value is String) setPathResolutionStrategy(value)
                
                // Backup & Sync Settings
                "auto_backup_enabled" -> if (value is Boolean) setAutoBackupEnabled(value)
                "auto_backup_frequency" -> if (value is String) setAutoBackupFrequency(value)
                "cloud_backup_enabled" -> if (value is Boolean) setCloudBackupEnabled(value)
                "backup_location" -> if (value is String) setBackupLocation(value)
                "last_backup_timestamp" -> if (value is Long) setLastBackupTimestamp(value)
                "backup_include_settings" -> if (value is Boolean) setBackupIncludeSettings(value)
                "backup_include_layouts" -> if (value is Boolean) setBackupIncludeLayouts(value)
                "backup_include_sound_buttons" -> if (value is Boolean) setBackupIncludeSoundButtons(value)
                "backup_include_local_files" -> if (value is Boolean) setBackupIncludeLocalFiles(value)
                
                // Profile Settings
                "current_profile_name" -> if (value is String) setCurrentProfileName(value)
                "profile_sync_enabled" -> if (value is Boolean) setProfileSyncEnabled(value)
            }
        }
    }

    // Reset all settings
    fun resetAllSettings() {
        prefs.edit().clear().apply()
        // Reload default values for all settings
        _themeMode.value = getThemeMode()
        _colorScheme.value = getColorScheme()
        _buttonCornerRadius.value = getButtonCornerRadius()
        _buttonSpacing.value = getButtonSpacing()
        _compactLayout.value = getCompactLayout()
        _animationsEnabled.value = getAnimationsEnabled()
        _showButtonLabels.value = getShowButtonLabels()
        _sampleRate.value = getSampleRate()
        _bufferSize.value = getBufferSize()
        _maxConcurrentSounds.value = getMaxConcurrentSounds()
        _audioQuality.value = getAudioQuality()
        _autoReconnect.value = getAutoReconnect()
        _connectionTimeout.value = getConnectionTimeout()
        _lowLatencyMode.value = getLowLatencyMode()
        _debugLogging.value = getDebugLogging()
        _showConnectionStatus.value = getShowConnectionStatus()
        _analyticsEnabled.value = getAnalyticsEnabled()
        _crashReporting.value = getCrashReporting()
        _hapticFeedback.value = getHapticFeedback()
        _keepScreenOn.value = getKeepScreenOn()
        _downloadLocation.value = getDownloadLocation()
        _minimizedMode.value = getMinimizedMode()
        
        // NEW: Reset new settings
        _defaultServerHost.value = getDefaultServerHost()
        _defaultServerPort.value = getDefaultServerPort()
        _rememberServerConnections.value = getRememberServerConnections()
        _autoConnectToLastServer.value = getAutoConnectToLastServer()
        _serverConnectionHistory.value = getServerConnectionHistory()
        _defaultLocalAudioPath.value = getDefaultLocalAudioPath()
        _preserveFilePaths.value = getPreserveFilePaths()
        _autoResolveLocalPaths.value = getAutoResolveLocalPaths()
        _pathResolutionStrategy.value = getPathResolutionStrategy()
        _autoBackupEnabled.value = getAutoBackupEnabled()
        _autoBackupFrequency.value = getAutoBackupFrequency()
        _cloudBackupEnabled.value = getCloudBackupEnabled()
        _backupLocation.value = getBackupLocation()
        _lastBackupTimestamp.value = getLastBackupTimestamp()
        _backupIncludeSettings.value = getBackupIncludeSettings()
        _backupIncludeLayouts.value = getBackupIncludeLayouts()
        _backupIncludeSoundButtons.value = getBackupIncludeSoundButtons()
        _backupIncludeLocalFiles.value = getBackupIncludeLocalFiles()
        _currentProfileName.value = getCurrentProfileName()
        _profileSyncEnabled.value = getProfileSyncEnabled()
    }
} 