package com.soundboard.android.ui.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsDialog(
    onDismiss: () -> Unit,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository,
    onShowMessage: (String) -> Unit = {}
) {
    val enableDebugLogging by settingsRepository.debugLogging.collectAsState()
    val showConnectionStatus by settingsRepository.showConnectionStatus.collectAsState()
    val autoReconnect by settingsRepository.autoReconnect.collectAsState()
    val connectionTimeout by settingsRepository.connectionTimeout.collectAsState()
    val audioBufferSize by settingsRepository.bufferSize.collectAsState()
    val audioSampleRate by settingsRepository.sampleRate.collectAsState()
    val compressionQuality by settingsRepository.audioQuality.collectAsState()
    val maxConcurrentSounds by settingsRepository.maxConcurrentSounds.collectAsState()
    val enableHapticFeedback by settingsRepository.hapticFeedback.collectAsState()
    val keepScreenOn by settingsRepository.keepScreenOn.collectAsState()
    val lowLatencyMode by settingsRepository.lowLatencyMode.collectAsState()
    val enableAnalytics by settingsRepository.analyticsEnabled.collectAsState()
    val enableCrashReporting by settingsRepository.crashReporting.collectAsState()
    
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // File creation launcher for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isExporting = true
                    exportSettings(context, selectedUri, settingsRepository)
                    onShowMessage("Settings exported successfully!")
                } catch (e: Exception) {
                    onShowMessage("Failed to export settings: ${e.message}")
                } finally {
                    isExporting = false
                }
            }
        }
    }
    
    // File picker launcher for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isImporting = true
                    importSettings(context, selectedUri, settingsRepository)
                    onShowMessage("Settings imported successfully!")
                } catch (e: Exception) {
                    onShowMessage("Failed to import settings: ${e.message}")
                } finally {
                    isImporting = false
                }
            }
        }
    }
    
    val sampleRateOptions = listOf("22050", "44100", "48000", "96000")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Developer Options
                item {
                    AdvancedSettingsSection(title = "Developer Options") {
                        SwitchSetting(
                            title = "Debug Logging",
                            subtitle = "Enable detailed logging for troubleshooting",
                            checked = enableDebugLogging,
                            onCheckedChange = { settingsRepository.setDebugLogging(it) }
                        )
                        
                        SwitchSetting(
                            title = "Show Connection Status",
                            subtitle = "Display connection indicators in UI",
                            checked = showConnectionStatus,
                            onCheckedChange = { settingsRepository.setShowConnectionStatus(it) }
                        )
                        
                        SwitchSetting(
                            title = "Analytics",
                            subtitle = "Help improve the app with anonymous usage data",
                            checked = enableAnalytics,
                            onCheckedChange = { settingsRepository.setAnalyticsEnabled(it) }
                        )
                        
                        SwitchSetting(
                            title = "Crash Reporting",
                            subtitle = "Automatically report crashes to developers",
                            checked = enableCrashReporting,
                            onCheckedChange = { settingsRepository.setCrashReporting(it) }
                        )
                    }
                }
                
                // Connection Settings
                item {
                    AdvancedSettingsSection(title = "Connection Settings") {
                        SwitchSetting(
                            title = "Auto Reconnect",
                            subtitle = "Automatically reconnect when connection is lost",
                            checked = autoReconnect,
                            onCheckedChange = { settingsRepository.setAutoReconnect(it) }
                        )
                        
                        SliderSetting(
                            title = "Connection Timeout",
                            subtitle = "${connectionTimeout.toInt()} seconds",
                            value = connectionTimeout,
                            onValueChange = { settingsRepository.setConnectionTimeout(it) },
                            valueRange = 5f..30f,
                            steps = 24
                        )
                        
                        SwitchSetting(
                            title = "Low Latency Mode",
                            subtitle = "Reduce audio delay (may increase CPU usage)",
                            checked = lowLatencyMode,
                            onCheckedChange = { settingsRepository.setLowLatencyMode(it) }
                        )
                    }
                }
                
                // Audio Settings
                item {
                    AdvancedSettingsSection(title = "Audio Configuration") {
                        // Sample Rate Selection
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Sample Rate",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Audio quality setting (higher = better quality)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sampleRateOptions.forEach { rate ->
                                    FilterChip(
                                        onClick = { settingsRepository.setSampleRate(rate.toInt()) },
                                        label = { Text("${rate}Hz") },
                                        selected = audioSampleRate == rate.toInt(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        
                        // Buffer Size Selector (using string values)
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Audio Buffer Size",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Current: $audioBufferSize",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Small", "Medium", "Large").forEach { size ->
                                    FilterChip(
                                        onClick = { settingsRepository.setBufferSize(size) },
                                        label = { Text(size) },
                                        selected = audioBufferSize == size,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        
                        SliderSetting(
                            title = "Max Concurrent Sounds",
                            subtitle = "${maxConcurrentSounds} sounds",
                            value = maxConcurrentSounds.toFloat(),
                            onValueChange = { settingsRepository.setMaxConcurrentSounds(it.toInt()) },
                            valueRange = 1f..16f,
                            steps = 14
                        )
                        
                        SliderSetting(
                            title = "Audio Quality",
                            subtitle = "${compressionQuality.toInt()}%",
                            value = compressionQuality,
                            onValueChange = { settingsRepository.setAudioQuality(it) },
                            valueRange = 10f..100f,
                            steps = 17
                        )
                    }
                }
                
                // System Settings
                item {
                    AdvancedSettingsSection(title = "System Settings") {
                        SwitchSetting(
                            title = "Haptic Feedback",
                            subtitle = "Vibrate when buttons are pressed",
                            checked = enableHapticFeedback,
                            onCheckedChange = { settingsRepository.setHapticFeedback(it) }
                        )
                        
                        SwitchSetting(
                            title = "Keep Screen On",
                            subtitle = "Prevent screen from turning off during use",
                            checked = keepScreenOn,
                            onCheckedChange = { settingsRepository.setKeepScreenOn(it) }
                        )
                    }
                }
                
                // Data Management
                item {
                    AdvancedSettingsSection(title = "Data Management") {
                        // Export Settings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isExporting && !isImporting) { 
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("soundboard_settings_$timestamp.json")
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Configuration",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isExporting) "Exporting..." else "Save settings and layouts to file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Import Settings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isExporting && !isImporting) { 
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Import Configuration",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isImporting) "Importing..." else "Load settings and layouts from file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Reset Settings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetConfirmation = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Reset All Settings",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Red
                                )
                                Text(
                                    text = "Restore app to default configuration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Save Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Settings are automatically saved when changed
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Advanced Settings")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // Reset Confirmation Dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset All Settings?") },
            text = { 
                Text("This will reset all app settings to their default values. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsRepository.resetAllSettings()
                        showResetConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    

}

private suspend fun exportSettings(
    context: Context,
    uri: Uri,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository
) = withContext(Dispatchers.IO) {
    try {
        val settingsData = JSONObject().apply {
            put("version", "1.0")
            put("timestamp", System.currentTimeMillis())
            put("app_version", "6.2")
            put("type", "settings_only")
            
            val settings = JSONObject().apply {
                put("theme_mode", settingsRepository.getThemeMode())
                put("color_scheme", settingsRepository.getColorScheme())
                put("button_corner_radius", settingsRepository.getButtonCornerRadius())
                put("button_spacing", settingsRepository.getButtonSpacing())
                put("compact_layout", settingsRepository.getCompactLayout())
                put("animations_enabled", settingsRepository.getAnimationsEnabled())
                put("show_button_labels", settingsRepository.getShowButtonLabels())
                put("sample_rate", settingsRepository.getSampleRate())
                put("buffer_size", settingsRepository.getBufferSize())
                put("max_concurrent_sounds", settingsRepository.getMaxConcurrentSounds())
                put("audio_quality", settingsRepository.getAudioQuality())
                put("auto_reconnect", settingsRepository.getAutoReconnect())
                put("connection_timeout", settingsRepository.getConnectionTimeout())
                put("low_latency_mode", settingsRepository.getLowLatencyMode())
                put("debug_logging", settingsRepository.getDebugLogging())
                put("show_connection_status", settingsRepository.getShowConnectionStatus())
                put("analytics_enabled", settingsRepository.getAnalyticsEnabled())
                put("crash_reporting", settingsRepository.getCrashReporting())
                put("haptic_feedback", settingsRepository.getHapticFeedback())
                put("keep_screen_on", settingsRepository.getKeepScreenOn())
                put("download_location", settingsRepository.getDownloadLocation())
            }
            put("settings", settings)
        }
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(settingsData.toString(2).toByteArray())
        } ?: throw IOException("Could not open output stream")
        
    } catch (e: Exception) {
        throw IOException("Failed to export settings: ${e.message}")
    }
}

private suspend fun importSettings(
    context: Context,
    uri: Uri,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository
) = withContext(Dispatchers.IO) {
    try {
        val settingsContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        } ?: throw IOException("Could not read settings file")
        
        val settingsData = JSONObject(settingsContent)
        
        if (settingsData.has("settings")) {
            val settings = settingsData.getJSONObject("settings")
            
            if (settings.has("theme_mode")) settingsRepository.setThemeMode(settings.getString("theme_mode"))
            if (settings.has("color_scheme")) settingsRepository.setColorScheme(settings.getString("color_scheme"))
            if (settings.has("button_corner_radius")) settingsRepository.setButtonCornerRadius(settings.getDouble("button_corner_radius").toFloat())
            if (settings.has("button_spacing")) settingsRepository.setButtonSpacing(settings.getDouble("button_spacing").toFloat())
            if (settings.has("compact_layout")) settingsRepository.setCompactLayout(settings.getBoolean("compact_layout"))
            if (settings.has("animations_enabled")) settingsRepository.setAnimationsEnabled(settings.getBoolean("animations_enabled"))
            if (settings.has("show_button_labels")) settingsRepository.setShowButtonLabels(settings.getBoolean("show_button_labels"))
            if (settings.has("sample_rate")) settingsRepository.setSampleRate(settings.getInt("sample_rate"))
            if (settings.has("buffer_size")) settingsRepository.setBufferSize(settings.getInt("buffer_size"))
            if (settings.has("max_concurrent_sounds")) settingsRepository.setMaxConcurrentSounds(settings.getInt("max_concurrent_sounds"))
            if (settings.has("audio_quality")) settingsRepository.setAudioQuality(settings.getDouble("audio_quality").toFloat())
            if (settings.has("auto_reconnect")) settingsRepository.setAutoReconnect(settings.getBoolean("auto_reconnect"))
            if (settings.has("connection_timeout")) settingsRepository.setConnectionTimeout(settings.getDouble("connection_timeout").toFloat())
            if (settings.has("low_latency_mode")) settingsRepository.setLowLatencyMode(settings.getBoolean("low_latency_mode"))
            if (settings.has("debug_logging")) settingsRepository.setDebugLogging(settings.getBoolean("debug_logging"))
            if (settings.has("show_connection_status")) settingsRepository.setShowConnectionStatus(settings.getBoolean("show_connection_status"))
            if (settings.has("analytics_enabled")) settingsRepository.setAnalyticsEnabled(settings.getBoolean("analytics_enabled"))
            if (settings.has("crash_reporting")) settingsRepository.setCrashReporting(settings.getBoolean("crash_reporting"))
            if (settings.has("haptic_feedback")) settingsRepository.setHapticFeedback(settings.getBoolean("haptic_feedback"))
            if (settings.has("keep_screen_on")) settingsRepository.setKeepScreenOn(settings.getBoolean("keep_screen_on"))
            if (settings.has("download_location")) settingsRepository.setDownloadLocation(settings.getString("download_location"))
        }
        
    } catch (e: Exception) {
        throw IOException("Failed to import settings: ${e.message}")
    }
}

@Composable
fun AdvancedSettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
} 