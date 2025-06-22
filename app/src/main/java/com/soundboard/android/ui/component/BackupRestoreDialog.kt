package com.soundboard.android.ui.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.soundboard.android.service.GoogleDriveService
import com.soundboard.android.service.BackupFile
import com.soundboard.android.service.SoundboardBackupService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.hilt.navigation.compose.hiltViewModel

data class BackupOption(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true
)

// Helper functions defined at top level
private suspend fun createBackupFile(
    context: Context,
    uri: Uri,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository,
    includeSettings: Boolean,
    includeLayouts: Boolean,
    includeSoundButtons: Boolean
) = withContext(Dispatchers.IO) {
    try {
        val backupData = JSONObject()
        
        // Add metadata
        backupData.put("version", "1.0")
        backupData.put("timestamp", System.currentTimeMillis())
        backupData.put("app_version", "6.2")
        
        // Add settings if requested
        if (includeSettings) {
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
            backupData.put("settings", settings)
        }
        
        // Add layouts if requested (placeholder)
        if (includeLayouts) {
            backupData.put("layouts", JSONObject())
        }
        
        // Add sound buttons if requested (placeholder)
        if (includeSoundButtons) {
            backupData.put("sound_buttons", JSONObject())
        }
        
        // Write to file
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(backupData.toString(2).toByteArray())
        } ?: throw IOException("Could not open output stream")
        
    } catch (e: Exception) {
        throw IOException("Failed to create backup: ${e.message}")
    }
}

private suspend fun restoreFromBackupFile(
    context: Context,
    uri: Uri,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository,
    includeSettings: Boolean,
    includeLayouts: Boolean,
    includeSoundButtons: Boolean
) = withContext(Dispatchers.IO) {
    try {
        val backupContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        } ?: throw IOException("Could not read backup file")
        
        val backupData = JSONObject(backupContent)
        
        // Restore settings if requested
        if (includeSettings && backupData.has("settings")) {
            val settings = backupData.getJSONObject("settings")
            
            if (settings.has("theme_mode")) settingsRepository.setThemeMode(settings.getString("theme_mode"))
            if (settings.has("color_scheme")) settingsRepository.setColorScheme(settings.getString("color_scheme"))
            if (settings.has("button_corner_radius")) settingsRepository.setButtonCornerRadius(settings.getDouble("button_corner_radius").toFloat())
            if (settings.has("button_spacing")) settingsRepository.setButtonSpacing(settings.getDouble("button_spacing").toFloat())
            if (settings.has("compact_layout")) settingsRepository.setCompactLayout(settings.getBoolean("compact_layout"))
            if (settings.has("animations_enabled")) settingsRepository.setAnimationsEnabled(settings.getBoolean("animations_enabled"))
            if (settings.has("show_button_labels")) settingsRepository.setShowButtonLabels(settings.getBoolean("show_button_labels"))
            if (settings.has("sample_rate")) settingsRepository.setSampleRate(settings.getInt("sample_rate"))
            if (settings.has("buffer_size")) settingsRepository.setBufferSize(settings.getInt("buffer_size").toString())
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
        
        // TODO: Restore layouts and sound buttons if requested
        
    } catch (e: Exception) {
        throw IOException("Failed to restore backup: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    onDismiss: () -> Unit,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository,
    onShowMessage: (String) -> Unit = {},
    googleDriveService: GoogleDriveService? = null,
    backupService: SoundboardBackupService? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    var includeSettings by remember { mutableStateOf(true) }
    var includeLayouts by remember { mutableStateOf(true) }
    var includeSoundButtons by remember { mutableStateOf(true) }
    var backupLocation by remember { mutableStateOf("Local") } // "Local" or "Google Drive"
    var isSignedInToGoogleDrive by remember { mutableStateOf(false) }
    var availableBackups by remember { mutableStateOf<List<BackupFile>>(emptyList()) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // File creation launcher for backup
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isProcessing = true
                    
                    // Use new comprehensive backup service if available
                    val result = if (backupService != null) {
                        backupService.createCompleteBackup(
                            context = context,
                            uri = selectedUri,
                            includeSettings = includeSettings,
                            includeLayouts = includeLayouts,
                            includeSoundButtons = includeSoundButtons,
                            includeLocalFiles = settingsRepository.getBackupIncludeLocalFiles()
                        )
                    } else {
                        // Fallback to old method
                        createBackupFile(
                            context = context,
                            uri = selectedUri,
                            settingsRepository = settingsRepository,
                            includeSettings = includeSettings,
                            includeLayouts = includeLayouts,
                            includeSoundButtons = includeSoundButtons
                        )
                        Result.success("Backup created successfully!")
                    }
                    
                    result.fold(
                        onSuccess = { message ->
                            onShowMessage(message)
                            onDismiss()
                        },
                        onFailure = { error ->
                            onShowMessage("Failed to create backup: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    onShowMessage("Failed to create backup: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    
    // File picker launcher for restore
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                try {
                    isProcessing = true
                    
                    // Use new comprehensive backup service if available
                    val result = if (backupService != null) {
                        backupService.restoreCompleteBackup(
                            context = context,
                            uri = selectedUri,
                            restoreSettings = includeSettings,
                            restoreLayouts = includeLayouts,
                            restoreSoundButtons = includeSoundButtons,
                            mergeWithExisting = false // For now, replace existing data
                        )
                    } else {
                        // Fallback to old method
                        restoreFromBackupFile(
                            context = context,
                            uri = selectedUri,
                            settingsRepository = settingsRepository,
                            includeSettings = includeSettings,
                            includeLayouts = includeLayouts,
                            includeSoundButtons = includeSoundButtons
                        )
                        Result.success("Backup restored successfully!")
                    }
                    
                    result.fold(
                        onSuccess = { message ->
                            onShowMessage(message)
                            onDismiss()
                        },
                        onFailure = { error ->
                            onShowMessage("Failed to restore backup: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    onShowMessage("Failed to restore backup: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Backup") },
                        icon = { Icon(Icons.Default.Backup, contentDescription = "Backup") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Restore") },
                        icon = { Icon(Icons.Default.Restore, contentDescription = "Restore") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Google Drive") },
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Google Drive") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content based on selected tab
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (selectedTab) {
                                0, 1 -> {
                                    Text(
                                        text = if (selectedTab == 0) "Select what to include in your backup:" else "Select what to restore from backup:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    // Settings option
                                    BackupOptionCard(
                                        option = BackupOption(
                                            name = "App Settings",
                                            description = "Theme, audio settings, preferences",
                                            icon = Icons.Default.Settings
                                        ),
                                        isChecked = includeSettings,
                                        onCheckedChange = { includeSettings = it }
                                    )
                                    
                                    // Layouts option
                                    BackupOptionCard(
                                        option = BackupOption(
                                            name = "Soundboard Layouts", 
                                            description = "Grid layouts and configurations",
                                            icon = Icons.Default.GridView
                                        ),
                                        isChecked = includeLayouts,
                                        onCheckedChange = { includeLayouts = it }
                                    )
                                    
                                    // Sound buttons option
                                    BackupOptionCard(
                                        option = BackupOption(
                                            name = "Sound Buttons",
                                            description = "Button configurations and assignments",
                                            icon = Icons.Default.GraphicEq
                                        ),
                                        isChecked = includeSoundButtons,
                                        onCheckedChange = { includeSoundButtons = it }
                                    )
                                }
                                2 -> {
                                    Text(
                                        text = "Google Drive Backup",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    if (!isSignedInToGoogleDrive) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Not signed in to Google Drive",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Sign in to Google Drive to backup and restore your soundboard data to the cloud.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        if (selectedTab != 2) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTab == 0) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.Info else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (selectedTab == 0) 
                                            "Backups are saved as JSON files that can be shared and restored on any device."
                                        else 
                                            "Restoring will overwrite your current settings and configurations. This action cannot be undone.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            when (selectedTab) {
                                0 -> {
                                    // Create backup
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    createBackupLauncher.launch("soundboard_backup_$timestamp.json")
                                }
                                1 -> {
                                    // Restore backup
                                    restoreBackupLauncher.launch(arrayOf("application/json"))
                                }
                                2 -> {
                                    // Google Drive functionality - placeholder for now
                                    onShowMessage("Google Drive functionality coming soon!")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing && when (selectedTab) {
                            0, 1 -> includeSettings || includeLayouts || includeSoundButtons
                            2 -> isSignedInToGoogleDrive
                            else -> false
                        }
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = when (selectedTab) {
                                    0 -> Icons.Default.Backup
                                    1 -> Icons.Default.Restore
                                    2 -> Icons.Default.CloudUpload
                                    else -> Icons.Default.Backup
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(when (selectedTab) {
                            0 -> "Create Backup"
                            1 -> "Restore Backup"
                            2 -> "Manage Google Drive"
                            else -> "Action"
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun BackupOptionCard(
    option: BackupOption,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (option.isEnabled) onCheckedChange(!isChecked) },
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = if (option.isEnabled) onCheckedChange else null,
                enabled = option.isEnabled
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (option.isEnabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (option.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
