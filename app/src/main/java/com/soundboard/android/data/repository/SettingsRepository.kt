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
    
    // Reset all settings
    fun resetAllSettings() {
        prefs.edit().clear().apply()
        // Reload default values
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
    }
} 