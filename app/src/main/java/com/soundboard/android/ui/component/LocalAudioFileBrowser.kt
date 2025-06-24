package com.soundboard.android.ui.component

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.soundboard.android.network.api.AudioFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocalAudioFileBrowser(
    onFileSelected: (AudioFile) -> Unit,
    onDismiss: () -> Unit,
    availableFiles: List<AudioFile>,
    isLoading: Boolean = false,
    onRefreshFiles: () -> Unit = {},
    onBrowseDirectory: (String) -> Unit = {},
    commonDirectories: List<String> = emptyList(),
    // Enhanced navigation state
    isInDirectoryBrowseMode: Boolean = false,
    currentDirectoryPath: String? = null,
    currentDirectoryFiles: List<AudioFile> = emptyList(),
    currentDirectorySubdirectories: List<String> = emptyList(),
    parentDirectoryPath: String? = null,
    breadcrumbs: List<Pair<String, String>> = emptyList(),
    onNavigateToParent: () -> Unit = {},
    onNavigateToBreadcrumb: (String) -> Unit = {},
    onExitDirectoryMode: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Request appropriate storage permissions based on API level
    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        listOf(android.Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)
    
    // System file picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Convert URI to file path and create AudioFile
                try {
                    val path = uri.path ?: return@let
                    val fileName = uri.lastPathSegment ?: "audio_file"
                    
                    val audioFile = AudioFile(
                        name = fileName,
                        path = path,
                        format = fileName.substringAfterLast('.', ""),
                        size = 0, // Size will be determined later
                        uri = uri.toString()
                    )
                    onFileSelected(audioFile)
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    // Automatically refresh files when permissions are granted
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onRefreshFiles()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and actions
                HeaderSection(
                    isInDirectoryMode = isInDirectoryBrowseMode,
                    currentPath = currentDirectoryPath,
                    onSystemFilePicker = {
                        // Launch system file picker for audio files
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "audio/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        documentPickerLauncher.launch(intent)
                    },
                    onRefresh = onRefreshFiles,
                    onExitDirectoryMode = onExitDirectoryMode,
                    onDismiss = onDismiss
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Check permissions
                if (!permissionsState.allPermissionsGranted) {
                    PermissionRequestCard(
                        onPermissionRequest = { permissionsState.launchMultiplePermissionRequest() }
                    )
                } else {
                    if (isInDirectoryBrowseMode) {
                        // Directory browse mode with navigation
                        DirectoryNavigationSection(
                            currentPath = currentDirectoryPath ?: "",
                            breadcrumbs = breadcrumbs,
                            parentPath = parentDirectoryPath,
                            onNavigateToParent = onNavigateToParent,
                            onNavigateToBreadcrumb = onNavigateToBreadcrumb
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DirectoryContentSection(
                            isLoading = isLoading,
                            audioFiles = currentDirectoryFiles,
                            subdirectories = currentDirectorySubdirectories,
                            onFileSelected = onFileSelected,
                            onDirectorySelected = onBrowseDirectory
                        )
                    } else {
                        // Main browse mode with tabs
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("All Music") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Folders") }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        when (selectedTab) {
                            0 -> {
                                // All music files
                                AudioFileListSection(
                                    isLoading = isLoading,
                                    audioFiles = availableFiles,
                                    onFileSelected = onFileSelected,
                                    onRefresh = onRefreshFiles
                                )
                            }
                            1 -> {
                                // Directory browser
                                CommonDirectoriesSection(
                                    directories = commonDirectories,
                                    onDirectorySelected = onBrowseDirectory
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    isInDirectoryMode: Boolean,
    currentPath: String?,
    onSystemFilePicker: () -> Unit,
    onRefresh: () -> Unit,
    onExitDirectoryMode: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isInDirectoryMode) "Browse Directory" else "Select Local Audio File",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (isInDirectoryMode && currentPath != null) {
                Text(
                    text = currentPath.substringAfterLast('/'),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Row {
            // System file picker button
            IconButton(onClick = onSystemFilePicker) {
                Icon(Icons.Default.FolderOpen, "System File Picker")
            }
            
            // Back/Exit button for directory mode
            if (isInDirectoryMode) {
                IconButton(onClick = onExitDirectoryMode) {
                    Icon(Icons.Default.ArrowBack, "Exit Directory")
                }
            }
            
            // Refresh button
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
            
            // Close button
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close")
            }
        }
    }
}

@Composable
private fun DirectoryNavigationSection(
    currentPath: String,
    breadcrumbs: List<Pair<String, String>>,
    parentPath: String?,
    onNavigateToParent: () -> Unit,
    onNavigateToBreadcrumb: (String) -> Unit
) {
    Column {
        // Parent directory button
        if (parentPath != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToParent() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Parent Directory",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = ".. (Parent Directory)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Breadcrumbs
        if (breadcrumbs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Path:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(breadcrumbs.size) { index ->
                            val (name, path) = breadcrumbs[index]
                            val isLast = index == breadcrumbs.size - 1
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { if (!isLast) onNavigateToBreadcrumb(path) },
                                    enabled = !isLast
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isLast) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                if (!isLast) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Separator",
                                        modifier = Modifier.size(16.dp),
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

@Composable
private fun DirectoryContentSection(
    isLoading: Boolean,
    audioFiles: List<AudioFile>,
    subdirectories: List<String>,
    onFileSelected: (AudioFile) -> Unit,
    onDirectorySelected: (String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn {
            // Subdirectories first
            items(subdirectories) { directory ->
                SubdirectoryItem(
                    directory = directory,
                    onClick = { onDirectorySelected(directory) }
                )
            }
            
            // Audio files
            items(audioFiles) { file ->
                AudioFileItem(
                    file = file,
                    onClick = { onFileSelected(file) }
                )
            }
            
            // Empty state
            if (subdirectories.isEmpty() && audioFiles.isEmpty()) {
                item {
                    EmptyDirectoryCard()
                }
            }
        }
    }
}

@Composable
private fun AudioFileListSection(
    isLoading: Boolean,
    audioFiles: List<AudioFile>,
    onFileSelected: (AudioFile) -> Unit,
    onRefresh: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (audioFiles.isEmpty()) {
        EmptyStateCard(
            title = "No Audio Files Found",
            description = "No audio files were found on your device. Try browsing specific folders or check your storage permissions.",
            onRefresh = onRefresh
        )
    } else {
        LazyColumn {
            items(audioFiles) { file ->
                AudioFileItem(
                    file = file,
                    onClick = { onFileSelected(file) }
                )
            }
        }
    }
}

@Composable
private fun CommonDirectoriesSection(
    directories: List<String>,
    onDirectorySelected: (String) -> Unit
) {
    if (directories.isEmpty()) {
        EmptyStateCard(
            title = "No Directories Found",
            description = "No common audio directories were found on your device.",
            onRefresh = { /* No refresh action for directories */ }
        )
    } else {
        LazyColumn {
            items(directories) { directory ->
                DirectoryItem(
                    directory = directory,
                    onClick = { onDirectorySelected(directory) }
                )
            }
        }
    }
}

@Composable
private fun SubdirectoryItem(
    directory: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Subdirectory",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directory.substringAfterLast('/'),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun AudioFileItem(
    file: AudioFile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AudioFile,
                contentDescription = "Audio file",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.format.uppercase()} • ${formatFileSize(file.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.path.isNotEmpty()) {
                    Text(
                        text = file.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DirectoryItem(
    directory: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Directory",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directory.substringAfterLast('/'),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = directory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Browse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRequestCard(
    onPermissionRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = "Storage permission",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To browse and select audio files from your device, we need access to your storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPermissionRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun EmptyDirectoryCard() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FolderOff,
                    contentDescription = "Empty directory",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Empty Directory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This directory contains no audio files or subdirectories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
} 