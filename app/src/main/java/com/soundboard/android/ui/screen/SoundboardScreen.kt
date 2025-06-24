package com.soundboard.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundboard.android.data.model.SoundButton
import com.soundboard.android.data.model.SoundboardLayout
import com.soundboard.android.network.ConnectionStatus
import com.soundboard.android.ui.component.AddSoundButtonDialog
import com.soundboard.android.ui.component.AudioFileBrowserUiState
import com.soundboard.android.ui.component.ConnectionDialog
import com.soundboard.android.ui.component.UsbConnectionDialog
import com.soundboard.android.ui.component.SoundButtonComponent
import com.soundboard.android.ui.component.VolumeControlPanel
import com.soundboard.android.ui.component.MinimizableConnectionBar
import com.soundboard.android.ui.component.ModernVolumeControl
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.soundboard.android.ui.component.LoadingStateOverlay
import com.soundboard.android.ui.screen.SettingsScreen
import com.soundboard.android.ui.screen.LayoutManagerScreen
import com.soundboard.android.ui.viewmodel.SoundboardViewModel
import com.soundboard.android.data.repository.SettingsRepository
import com.soundboard.android.diagnostics.ComponentType
import com.soundboard.android.diagnostics.LogCategory
import com.soundboard.android.diagnostics.LogEvent
import com.soundboard.android.diagnostics.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundboardScreen(
    viewModel: SoundboardViewModel = hiltViewModel(),
    settingsRepository: SettingsRepository
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val serverInfo by viewModel.serverInfo.collectAsState()
    var showConnectionDialog by remember { mutableStateOf(false) }
    
    // Get minimized mode from settings
    val isMinimizedMode by settingsRepository.minimizedMode.collectAsState()
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showLayoutManagerScreen by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    var selectedButtonForEdit by remember { mutableStateOf<SoundButton?>(null) }
    var clickedPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    if (showAddButtonDialog || selectedButtonForEdit != null) {
        val isEditMode = selectedButtonForEdit != null
        LaunchedEffect(isEditMode) {
            viewModel.logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.USER_INTERACTION,
                    message = if (isEditMode) "Showing Edit Sound Button Dialog" else "Showing Add Sound Button Dialog",
                    component = ComponentType.UI_DIALOG
                )
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Modern minimizable connection bar - only show when not in minimized mode
            AnimatedVisibility(
                visible = !isMinimizedMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                MinimizableConnectionBar(
                    connectionStatus = connectionStatus,
                    serverInfo = serverInfo,
                    onConnectionClick = { showConnectionDialog = true },
                    onSettingsClick = { showSettingsScreen = true },
                    onAddButtonClick = { 
                        clickedPosition = findNextAvailablePosition(uiState.soundButtons, uiState.activeLayout)
                        showAddButtonDialog = true 
                    },
                    onDisconnectClick = { viewModel.disconnectFromServer() },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        
            // Error message
            uiState.errorMessage?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Modern Volume Control - only show when not in minimized mode
            AnimatedVisibility(
                visible = !isMinimizedMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ModernVolumeControl(
                    soundButtons = uiState.soundButtons,
                    onGlobalVolumeChange = { multiplier ->
                        viewModel.applyGlobalVolumeMultiplier(multiplier)
                    },
                    onNormalizeVolumes = {
                        viewModel.normalizeAllVolumes()
                    },
                    onResetAllVolumes = {
                        viewModel.resetAllVolumes()
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        
            // Add spacing only when not in minimized mode
            if (!isMinimizedMode) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Soundboard grid
            if (uiState.activeLayout != null) {
                val activeLayout = uiState.activeLayout!!
                LazyVerticalGrid(
                    columns = GridCells.Fixed(activeLayout.gridColumns),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = generateGridItems(
                            uiState.soundButtons,
                            activeLayout.gridColumns,
                            activeLayout.gridRows
                        ),
                        key = { "${it.positionX}-${it.positionY}" }
                    ) { gridItem ->
                        SoundButtonComponent(
                            soundButton = gridItem.soundButton,
                            onClick = { soundButton ->
                                if (soundButton != null) {
                                    viewModel.playSoundButton(soundButton)
                                } else {
                                    // Empty slot clicked - add new button
                                    clickedPosition = Pair(gridItem.positionX, gridItem.positionY)
                                    showAddButtonDialog = true
                                }
                            },
                            onLongClick = { soundButton ->
                                selectedButtonForEdit = soundButton
                            },
                            onEdit = { soundButton ->
                                selectedButtonForEdit = soundButton
                            },
                            onDelete = { soundButton ->
                                viewModel.deleteSoundButton(soundButton)
                            },
                            onVolumeChange = { soundButton, volume ->
                                viewModel.updateSoundButtonVolume(soundButton, volume)
                            },
                            onQuickVolumeAdjust = { soundButton, volume ->
                                viewModel.updateSoundButtonVolume(soundButton, volume)
                            },
                            isEnabled = connectionStatus is ConnectionStatus.Connected,
                            isPlaying = gridItem.soundButton?.id == uiState.currentlyPlayingButtonId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                }
            } else {
                // Loading or no layout state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading soundboard...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        // Floating Action Button for toggling minimized mode
        FloatingActionButton(
            onClick = { 
                settingsRepository.setMinimizedMode(!isMinimizedMode)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isMinimizedMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (isMinimizedMode) "Show UI" else "Hide UI",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    
    // USB Connection Dialog
    if (showConnectionDialog) {
        LaunchedEffect(Unit) {
            viewModel.logEvent(
                LogEvent(
                    level = LogLevel.INFO,
                    category = LogCategory.USER_INTERACTION,
                    message = "Showing USB Connection Dialog",
                    component = ComponentType.UI_DIALOG
                )
            )
        }
        val context = LocalContext.current
        UsbConnectionDialog(
            onDismiss = {
                viewModel.logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.USER_INTERACTION,
                        message = "USB Connection Dialog dismissed",
                        component = ComponentType.UI_DIALOG
                    )
                )
                showConnectionDialog = false
            },
            onConnect = {
                viewModel.connectViaUSB(context)
                showConnectionDialog = false
            },
            isConnecting = connectionStatus is ConnectionStatus.Connecting,
            connectionSuccess = connectionStatus is ConnectionStatus.Connected,
            errorMessage = when (val status = connectionStatus) {
                is ConnectionStatus.Error -> status.message
                else -> null
            }
        )
    }
    
    // Add Button Dialog
    if (showAddButtonDialog) {
        val suggestedPos = clickedPosition
        AddSoundButtonDialog(
            onDismiss = { 
                viewModel.logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.USER_INTERACTION,
                        message = "Add Sound Button Dialog dismissed",
                        component = ComponentType.UI_DIALOG
                    )
                )
                showAddButtonDialog = false
            },
            onSave = { name, filePath, positionX, positionY, color, iconName, isLocalFile ->
                viewModel.addSoundButton(name, filePath, positionX, positionY, color, iconName, isLocalFile)
                showAddButtonDialog = false
            },
            availableFiles = uiState.availableAudioFiles,
            suggestedPosition = suggestedPos,
            onRefreshFiles = { viewModel.refreshAudioFiles() },
            onRefreshLocalFiles = { viewModel.refreshLocalAudioFiles() },
            localAudioFiles = uiState.localAudioFiles,
            commonDirectories = uiState.commonDirectories,
            onBrowseDirectory = { directory -> viewModel.browseDirectory(directory) },
            isLoadingLocalFiles = uiState.isLoadingLocalFiles,
            uiState = AudioFileBrowserUiState(
                isInDirectoryBrowseMode = uiState.isInDirectoryBrowseMode,
                currentDirectoryPath = uiState.currentDirectoryPath,
                currentDirectoryFiles = uiState.currentDirectoryFiles,
                currentDirectorySubdirectories = uiState.currentDirectorySubdirectories,
                parentDirectoryPath = uiState.parentDirectoryPath,
                breadcrumbs = uiState.breadcrumbs
            ),
            onNavigateToParent = { viewModel.navigateToParentDirectory() },
            onNavigateToBreadcrumb = { path -> viewModel.navigateToBreadcrumb(path) },
            onExitDirectoryMode = { viewModel.exitDirectoryBrowseMode() }
        )
    }
    
    // Edit Button Dialog
    if (selectedButtonForEdit != null) {
        AddSoundButtonDialog(
            onDismiss = {
                viewModel.logEvent(
                    LogEvent(
                        level = LogLevel.INFO,
                        category = LogCategory.USER_INTERACTION,
                        message = "Edit SoundButton Dialog dismissed",
                        component = ComponentType.UI_DIALOG
                    )
                )
                selectedButtonForEdit = null
            },
            onSave = { name, filePath, positionX, positionY, color, iconName, isLocalFile ->
                viewModel.updateSoundButton(
                    selectedButtonForEdit!!.copy(
                        name = name,
                        filePath = filePath,
                        positionX = positionX,
                        positionY = positionY,
                        color = color,
                        iconName = iconName,
                        isLocalFile = isLocalFile
                    )
                )
                selectedButtonForEdit = null
            },
            availableFiles = uiState.availableAudioFiles,
            existingButton = selectedButtonForEdit,
            onRefreshFiles = { viewModel.refreshAudioFiles() },
            onRefreshLocalFiles = { viewModel.refreshLocalAudioFiles() },
            localAudioFiles = uiState.localAudioFiles,
            commonDirectories = uiState.commonDirectories,
            onBrowseDirectory = { directory -> viewModel.browseDirectory(directory) },
            isLoadingLocalFiles = uiState.isLoadingLocalFiles,
            uiState = AudioFileBrowserUiState(
                isInDirectoryBrowseMode = uiState.isInDirectoryBrowseMode,
                currentDirectoryPath = uiState.currentDirectoryPath,
                currentDirectoryFiles = uiState.currentDirectoryFiles,
                currentDirectorySubdirectories = uiState.currentDirectorySubdirectories,
                parentDirectoryPath = uiState.parentDirectoryPath,
                breadcrumbs = uiState.breadcrumbs
            ),
            onNavigateToParent = { viewModel.navigateToParentDirectory() },
            onNavigateToBreadcrumb = { path -> viewModel.navigateToBreadcrumb(path) },
            onExitDirectoryMode = { viewModel.exitDirectoryBrowseMode() }
        )
    }
    
    // Settings Screen
    if (showSettingsScreen) {
        SettingsScreen(
            onNavigateBack = { showSettingsScreen = false },
            onNavigateToLayoutManager = { 
                showSettingsScreen = false
                showLayoutManagerScreen = true 
            },
            viewModel = viewModel,
            settingsRepository = settingsRepository
        )
    }
    
    // Layout Manager Screen
    if (showLayoutManagerScreen) {
        LayoutManagerScreen(
            onNavigateBack = { showLayoutManagerScreen = false },
            viewModel = viewModel
        )
    }
    
    // Loading overlay for various loading states
    LoadingStateOverlay(
        isVisible = uiState.isLoading || uiState.isLoadingLocalFiles,
        message = when {
            uiState.isLoadingLocalFiles -> "Loading audio files..."
            uiState.isLoading -> "Loading soundboard..."
            else -> "Loading..."
        }
    )
}

data class GridItem(
    val positionX: Int,
    val positionY: Int,
    val soundButton: SoundButton?
)

fun generateGridItems(
    soundButtons: List<SoundButton>,
    columns: Int,
    rows: Int
): List<GridItem> {
    val buttonMap = soundButtons.associateBy { "${it.positionX}-${it.positionY}" }
    val gridItems = mutableListOf<GridItem>()
    
    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val key = "$x-$y"
            gridItems.add(
                GridItem(
                    positionX = x,
                    positionY = y,
                    soundButton = buttonMap[key]
                )
            )
        }
    }
    
    return gridItems
}

fun findNextAvailablePosition(
    soundButtons: List<SoundButton>,
    activeLayout: SoundboardLayout?
): Pair<Int, Int>? {
    if (activeLayout == null) return null
    
    val columns = activeLayout.gridColumns
    val rows = activeLayout.gridRows
    val occupiedPositions = soundButtons.map { "${it.positionX}-${it.positionY}" }.toSet()
    
    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val position = "$x-$y"
            if (!occupiedPositions.contains(position)) {
                return Pair(x, y)
            }
        }
    }
    
    return null
}
