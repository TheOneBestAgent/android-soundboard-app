package com.soundboard.android.ui.component

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

data class ResetOption(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isDestructive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetAppDialog(
    onDismiss: () -> Unit,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository,
    onShowMessage: (String) -> Unit = {},
    onResetComplete: () -> Unit = {}
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var resetSettings by remember { mutableStateOf(true) }
    var resetLayouts by remember { mutableStateOf(true) }
    var resetSoundButtons by remember { mutableStateOf(true) }
    var resetDownloads by remember { mutableStateOf(false) }
    var resetConnections by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    val resetOptions = listOf(
        ResetOption(
            name = "App Settings",
            description = "Reset all preferences to defaults",
            icon = Icons.Default.Settings
        ),
        ResetOption(
            name = "Soundboard Layouts",
            description = "Remove all custom layouts",
            icon = Icons.Default.GridView
        ),
        ResetOption(
            name = "Sound Buttons",
            description = "Clear all sound button configurations",
            icon = Icons.Default.GraphicEq
        ),
        ResetOption(
            name = "Connection History",
            description = "Clear saved connection settings",
            icon = Icons.Default.Wifi,
            isDestructive = false
        ),
        ResetOption(
            name = "Downloaded Files",
            description = "Delete all downloaded audio files",
            icon = Icons.Default.AudioFile
        )
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (showConfirmation) {
                // Confirmation Dialog Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Are you absolutely sure?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "This action will permanently delete the selected data and cannot be undone. Make sure you have a backup if you want to restore your settings later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmation = false },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        isProcessing = true
                                        performReset(
                                            settingsRepository = settingsRepository,
                                            resetSettings = resetSettings,
                                            resetLayouts = resetLayouts,
                                            resetSoundButtons = resetSoundButtons,
                                            resetDownloads = resetDownloads,
                                            resetConnections = resetConnections
                                        )
                                        onShowMessage("App reset completed successfully!")
                                        onResetComplete()
                                        onDismiss()
                                    } catch (e: Exception) {
                                        onShowMessage("Reset failed: ${e.message}")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset App")
                        }
                    }
                }
            } else {
                // Main Dialog Content
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
                            text = "Reset App",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    // Content
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Select what to reset:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                resetOptions.forEach { option ->
                                    val isChecked = when (option.name) {
                                        "App Settings" -> resetSettings
                                        "Soundboard Layouts" -> resetLayouts
                                        "Sound Buttons" -> resetSoundButtons
                                        "Connection History" -> resetConnections
                                        "Downloaded Files" -> resetDownloads
                                        else -> false
                                    }
                                    
                                    val onCheckedChange: (Boolean) -> Unit = { checked ->
                                        when (option.name) {
                                            "App Settings" -> resetSettings = checked
                                            "Soundboard Layouts" -> resetLayouts = checked
                                            "Sound Buttons" -> resetSoundButtons = checked
                                            "Connection History" -> resetConnections = checked
                                            "Downloaded Files" -> resetDownloads = checked
                                        }
                                    }
                                    
                                    ResetOptionCard(
                                        option = option,
                                        isChecked = isChecked,
                                        onCheckedChange = onCheckedChange
                                    )
                                }
                            }
                        }
                        
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Warning: This action cannot be undone",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Consider creating a backup before resetting to preserve your configurations.",
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = { showConfirmation = true },
                            modifier = Modifier.weight(1f),
                            enabled = resetSettings || resetLayouts || resetSoundButtons || resetDownloads || resetConnections,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Selected")
                        }
                    }
                }
            }
        }
    }
}



private suspend fun performReset(
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository,
    resetSettings: Boolean,
    resetLayouts: Boolean,
    resetSoundButtons: Boolean,
    resetDownloads: Boolean,
    resetConnections: Boolean
) {
    try {
        if (resetSettings) {
            settingsRepository.resetAllSettings()
        }
        
        // TODO: Implement other reset operations
        if (resetLayouts) {
            // Reset layouts - would need LayoutRepository
        }
        
        if (resetSoundButtons) {
            // Reset sound buttons - would need SoundButtonRepository
        }
        
        if (resetConnections) {
            // Reset connection history - would need ConnectionRepository
        }
        
        if (resetDownloads) {
            // Delete downloaded files - would need file system operations
        }
        
    } catch (e: Exception) {
        throw Exception("Failed to reset app: ${e.message}")
    }
}

@Composable
fun ResetOptionCard(
    option: ResetOption,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        colors = CardDefaults.cardColors(
            containerColor = if (option.isDestructive && isChecked) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
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
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (option.isDestructive && isChecked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (option.isDestructive && isChecked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
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
