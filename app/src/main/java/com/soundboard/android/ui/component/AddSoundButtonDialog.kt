package com.soundboard.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.soundboard.android.data.model.SoundButton
import com.soundboard.android.network.api.AudioFile

data class AudioFileBrowserUiState(
    val isInDirectoryBrowseMode: Boolean = false,
    val currentDirectoryPath: String? = null,
    val currentDirectoryFiles: List<AudioFile> = emptyList(),
    val currentDirectorySubdirectories: List<String> = emptyList(),
    val parentDirectoryPath: String? = null,
    val breadcrumbs: List<Pair<String, String>> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSoundButtonDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, filePath: String, positionX: Int, positionY: Int, color: String, iconName: String, isLocalFile: Boolean) -> Unit,
    availableFiles: List<AudioFile>,
    suggestedPosition: Pair<Int, Int>? = null,
    existingButton: SoundButton? = null,
    onRefreshFiles: () -> Unit = {},
    onRefreshLocalFiles: () -> Unit = {},
    localAudioFiles: List<AudioFile> = emptyList(),
    commonDirectories: List<String> = emptyList(),
    onBrowseDirectory: (String) -> Unit = {},
    isLoadingLocalFiles: Boolean = false,
    uiState: AudioFileBrowserUiState,
    onNavigateToParent: () -> Unit,
    onNavigateToBreadcrumb: (String) -> Unit,
    onExitDirectoryMode: () -> Unit
) {
    var name by remember { 
        mutableStateOf(TextFieldValue(existingButton?.name ?: ""))
    }
    var selectedFile by remember { mutableStateOf(existingButton?.filePath ?: "") }
    var selectedColor by remember { mutableStateOf(existingButton?.color ?: "#2196F3") }
    var selectedIcon by remember { mutableStateOf(existingButton?.iconName ?: "music_note") }
    var isLocalFile by remember { mutableStateOf(existingButton?.isLocalFile ?: false) }
    var positionX by remember { 
        mutableStateOf(TextFieldValue((existingButton?.positionX ?: suggestedPosition?.first ?: 0).toString()))
    }
    var positionY by remember { 
        mutableStateOf(TextFieldValue((existingButton?.positionY ?: suggestedPosition?.second ?: 0).toString()))
    }
    
    var showFileSelector by remember { mutableStateOf(false) }
    var showLocalFileSelector by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var positionError by remember { mutableStateOf<String?>(null) }
    
    val predefinedColors = listOf(
        "#2196F3", "#4CAF50", "#FF9800", "#F44336",
        "#9C27B0", "#607D8B", "#795548", "#E91E63",
        "#3F51B5", "#009688", "#CDDC39", "#FF5722"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingButton != null) "Edit Sound Button" else "Add Sound Button",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                // Button Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Button Name") },
                    placeholder = { Text("Enter button name...") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = if (nameError != null) {
                        { Text(nameError!!) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // File Source Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Audio File Source",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                onClick = { 
                                    isLocalFile = false
                                    selectedFile = ""
                                    fileError = null
                                },
                                label = { Text("Server Files") },
                                selected = !isLocalFile,
                                leadingIcon = if (!isLocalFile) {
                                    { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                            FilterChip(
                                onClick = { 
                                    isLocalFile = true
                                    selectedFile = ""
                                    fileError = null
                                },
                                label = { Text("Local Files") },
                                selected = isLocalFile,
                                leadingIcon = if (isLocalFile) {
                                    { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
                
                // Audio File Selection
                OutlinedTextField(
                    value = selectedFile,
                    onValueChange = { },
                    label = { Text(if (isLocalFile) "Local Audio File" else "Server Audio File") },
                    placeholder = { Text(if (isLocalFile) "Select a local audio file..." else "Select a server audio file...") },
                    singleLine = true,
                    readOnly = true,
                    isError = fileError != null,
                    supportingText = if (fileError != null) {
                        { Text(fileError!!) }
                    } else null,
                    trailingIcon = {
                        Row {
                            if (!isLocalFile) {
                                IconButton(onClick = onRefreshFiles) {
                                    Icon(Icons.Default.Refresh, "Refresh Server Files")
                                }
                            }
                            IconButton(onClick = { 
                                if (isLocalFile) {
                                    showLocalFileSelector = true
                                } else {
                                    showFileSelector = true
                                }
                            }) {
                                Icon(
                                    if (isLocalFile) Icons.Default.Storage else Icons.Default.Folder, 
                                    if (isLocalFile) "Browse Local Files" else "Browse Server Files"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Position Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = positionX,
                        onValueChange = { 
                            positionX = it
                            positionError = null
                        },
                        label = { Text("Column (X)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = positionError != null,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = positionY,
                        onValueChange = { 
                            positionY = it
                            positionError = null
                        },
                        label = { Text("Row (Y)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = positionError != null,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (positionError != null) {
                    Text(
                        text = positionError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Color Selection
                Text(
                    text = "Button Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(predefinedColors.chunked(4)) { colorRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorRow.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(color)))
                                        .clickable { selectedColor = color }
                                        .then(
                                            if (selectedColor == color) {
                                                Modifier.padding(2.dp)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == color) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Icon Selection
                Text(
                    text = "Button Icon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = selectedIcon,
                    onValueChange = { },
                    label = { Text("Icon") },
                    placeholder = { Text("Choose an icon...") },
                    singleLine = true,
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = getIconByName(selectedIcon),
                            contentDescription = "Selected Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showIconPicker = true }) {
                            Icon(Icons.Default.Edit, "Choose Icon")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                }
                
                // Action Buttons - Fixed at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Validate inputs
                            var hasError = false
                            
                            if (name.text.isBlank()) {
                                nameError = "Button name is required"
                                hasError = true
                            }
                            
                            if (selectedFile.isBlank()) {
                                fileError = "Please select an audio file"
                                hasError = true
                            }
                            
                            val x = positionX.text.toIntOrNull()
                            val y = positionY.text.toIntOrNull()
                            if (x == null || y == null || x < 0 || y < 0) {
                                positionError = "Position must be valid numbers (0 or greater)"
                                hasError = true
                            }
                            
                            if (!hasError) {
                                onSave(name.text.trim(), selectedFile, x!!, y!!, selectedColor, selectedIcon, isLocalFile)
                            }
                        }
                    ) {
                        Text(if (existingButton != null) "Update" else "Add Button")
                    }
                }
            }
        }
    }
    
    // File Selector Dialog
    if (showFileSelector) {
        AudioFileSelector(
            files = availableFiles,
            onFileSelected = { file ->
                selectedFile = file.path
                fileError = null
                showFileSelector = false
            },
            onDismiss = { showFileSelector = false },
            onRefresh = onRefreshFiles
        )
    }
    
    // Local File Selector Dialog
    if (showLocalFileSelector) {
        LocalAudioFileBrowser(
            onFileSelected = { file ->
                selectedFile = file.uri ?: file.path  // Use URI for local files, fallback to path
                fileError = null
                showLocalFileSelector = false
            },
            onDismiss = { showLocalFileSelector = false },
            availableFiles = localAudioFiles,
            isLoading = isLoadingLocalFiles,
            onRefreshFiles = onRefreshLocalFiles,
            onBrowseDirectory = onBrowseDirectory,
            commonDirectories = commonDirectories,
            isInDirectoryBrowseMode = uiState.isInDirectoryBrowseMode,
            currentDirectoryPath = uiState.currentDirectoryPath,
            currentDirectoryFiles = uiState.currentDirectoryFiles,
            currentDirectorySubdirectories = uiState.currentDirectorySubdirectories,
            parentDirectoryPath = uiState.parentDirectoryPath,
            breadcrumbs = uiState.breadcrumbs,
            onNavigateToParent = onNavigateToParent,
            onNavigateToBreadcrumb = onNavigateToBreadcrumb,
            onExitDirectoryMode = onExitDirectoryMode
        )
    }
    
    // Icon Picker Dialog
    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = selectedIcon,
            onDismiss = { showIconPicker = false },
            onIconSelected = { iconName ->
                selectedIcon = iconName
                showIconPicker = false
            }
        )
    }
}

@Composable
private fun AudioFileSelector(
    files: List<AudioFile>,
    onFileSelected: (AudioFile) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Audio File",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Row {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "No files",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No audio files found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Place audio files in the server/audio directory",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn {
                        items(files) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onFileSelected(file) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = "Audio file",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${file.format.uppercase()} • ${formatFileSize(file.size)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Select",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
} 