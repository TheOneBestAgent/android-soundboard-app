package com.soundboard.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundboard.android.ui.component.ConnectionDialog
import com.soundboard.android.ui.component.UsbConnectionDialog
import com.soundboard.android.ui.component.MyInstantDownloader
import com.soundboard.android.ui.component.AppearanceSettingsDialog
import com.soundboard.android.ui.component.DownloadLocationDialog
import com.soundboard.android.ui.component.GridLayoutSettingsDialog
import com.soundboard.android.ui.component.AnalyticsDashboard
import com.soundboard.android.ui.viewmodel.SoundboardViewModel
import com.soundboard.android.data.repository.SettingsRepository
import com.soundboard.android.network.NetworkDiscoveryService
import com.soundboard.android.network.MultiTransportManager
import com.soundboard.android.network.ConnectionAnalytics
import com.soundboard.android.diagnostics.ComponentType
import com.soundboard.android.diagnostics.LogCategory
import com.soundboard.android.diagnostics.LogEvent
import com.soundboard.android.diagnostics.LogLevel
import com.soundboard.android.ui.viewmodel.logInteraction
import javax.inject.Inject

data class SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val onClick: () -> Unit
)

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLayoutManager: () -> Unit = {},
    viewModel: SoundboardViewModel = hiltViewModel(),
    settingsRepository: SettingsRepository
) {
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showUsbConnectionDialog by remember { mutableStateOf(false) }
    var showMyInstantDownloader by remember { mutableStateOf(false) }
    var showAppearanceSettings by remember { mutableStateOf(false) }
    var showAnalyticsDashboard by remember { mutableStateOf(false) }
    var showDownloadLocationDialog by remember { mutableStateOf(false) }
    var showGridLayoutSettings by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    val downloadLocation by settingsRepository.downloadLocation.collectAsState()
    
    // Phase 2: Network Discovery Service
    val context = LocalContext.current
    val networkDiscoveryService = remember { NetworkDiscoveryService(context) }
    val discoveredServers by networkDiscoveryService.discoveredServers.collectAsState()
    val isDiscovering by networkDiscoveryService.discoveryState.collectAsState()
    val isDiscoveringActive = isDiscovering == NetworkDiscoveryService.DiscoveryState.DISCOVERING
    
    // Phase 3: Multi-Transport & Analytics
    val connectionAnalytics = remember { ConnectionAnalytics(context) }
    val multiTransportManager = remember { MultiTransportManager(context, connectionAnalytics) }
    
    // Cleanup services
    DisposableEffect(Unit) {
        onDispose {
            networkDiscoveryService.cleanup()
            multiTransportManager.cleanup()
        }
    }
    
    // Helper function to format download location display
    fun formatDownloadLocation(location: String): String {
        return if (location.startsWith("custom:")) {
            "Custom folder"
        } else {
            location
        }
    }
    
    val settingsSections = listOf(
        SettingsSection(
            title = "Connection",
            items = listOf(
                SettingsItem(
                    title = "Connection Settings",
                    subtitle = "Configure server connection",
                    icon = Icons.Default.Wifi,
                    onClick = {
                        viewModel.logInteraction("Clicked on Connection Settings", ComponentType.UI_SETTINGS)
                        showConnectionDialog = true
                    }
                ),
                SettingsItem(
                    title = "USB Setup Guide",
                    subtitle = "Setup ADB connection over USB",
                    icon = Icons.Default.Usb,
                    onClick = {
                        viewModel.logInteraction("Clicked on USB Setup Guide", ComponentType.UI_SETTINGS)
                        showUsbConnectionDialog = true
                    }
                ),
                SettingsItem(
                    title = "Analytics Dashboard",
                    subtitle = "View connection analytics and performance metrics",
                    icon = Icons.Default.Analytics,
                    onClick = {
                        viewModel.logInteraction("Clicked on Analytics Dashboard", ComponentType.UI_SETTINGS)
                        showAnalyticsDashboard = true
                    }
                )
            )
        ),
        SettingsSection(
            title = "Content",
            items = listOf(
                SettingsItem(
                    title = "Download Sounds",
                    subtitle = "Browse and download from MyInstant.com",
                    icon = Icons.Default.Download,
                    onClick = {
                        viewModel.logInteraction("Clicked on Download Sounds", ComponentType.UI_SETTINGS)
                        showMyInstantDownloader = true
                    }
                ),
                SettingsItem(
                    title = "Download Location",
                    subtitle = "Currently: ${formatDownloadLocation(downloadLocation)}",
                    icon = Icons.Default.Folder,
                    onClick = {
                        viewModel.logInteraction("Clicked on Download Location", ComponentType.UI_SETTINGS)
                        showDownloadLocationDialog = true
                    }
                ),
                SettingsItem(
                    title = "Layout Manager",
                    subtitle = "Manage soundboard layouts",
                    icon = Icons.Default.GridView,
                    onClick = {
                        viewModel.logInteraction("Clicked on Layout Manager", ComponentType.UI_SETTINGS)
                        onNavigateToLayoutManager()
                    }
                )
            )
        ),
        SettingsSection(
            title = "Customization",
            items = listOf(
                SettingsItem(
                    title = "Appearance",
                    subtitle = "Themes, colors, and visual settings",
                    icon = Icons.Default.Palette,
                    onClick = {
                        viewModel.logInteraction("Clicked on Appearance", ComponentType.UI_SETTINGS)
                        showAppearanceSettings = true
                    }
                ),
                SettingsItem(
                    title = "Button Icons",
                    subtitle = "Customize button icons and styles",
                    icon = Icons.Default.Apps,
                    onClick = {
                        viewModel.logInteraction("Clicked on Button Icons", ComponentType.UI_SETTINGS)
                        /* TODO: Icon picker */
                    }
                ),
                SettingsItem(
                    title = "Grid Layout",
                    subtitle = "Adjust grid size and spacing",
                    icon = Icons.Default.Dashboard,
                    onClick = {
                        viewModel.logInteraction("Clicked on Grid Layout", ComponentType.UI_SETTINGS)
                        showGridLayoutSettings = true
                    }
                )
            )
        ),
        SettingsSection(
            title = "Audio",
            items = listOf(
                SettingsItem(
                    title = "Volume Settings",
                    subtitle = "Global volume and normalization",
                    icon = Icons.Default.VolumeUp,
                    onClick = {
                        viewModel.logInteraction("Clicked on Volume Settings", ComponentType.UI_SETTINGS)
                        /* TODO: Volume settings */
                    }
                ),
                SettingsItem(
                    title = "Audio Quality",
                    subtitle = "Playback quality and format settings",
                    icon = Icons.Default.HighQuality,
                    onClick = {
                        viewModel.logInteraction("Clicked on Audio Quality", ComponentType.UI_SETTINGS)
                        /* TODO: Audio quality settings */
                    }
                )
            )
        )
    )
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            snackbarMessage?.let { message ->
                SnackbarHost(
                    hostState = remember { SnackbarHostState() },
                    snackbar = { data ->
                        Snackbar(
                            action = {
                                TextButton(onClick = { snackbarMessage = null }) {
                                    Text("Dismiss")
                                }
                            }
                        ) {
                            Text(message)
                        }
                    }
                )
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    snackbarMessage = null
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(settingsSections) { section ->
                SettingsSectionCard(section = section)
            }
        }
    }
    
    // Dialogs
    if (showConnectionDialog) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Connection Dialog", ComponentType.UI_DIALOG)
        }
        ConnectionDialog(
            onDismiss = {
                viewModel.logInteraction("Connection Dialog dismissed", ComponentType.UI_DIALOG)
                showConnectionDialog = false
            },
            onConnect = { host, port ->
                viewModel.connectToServer(host, port)
                showConnectionDialog = false
            },
            onQRCodeScan = {
                // TODO: Implement QR code scanning
                snackbarMessage = "QR code scanning will be implemented in next update"
            },
            isConnecting = false, // TODO: Get from viewModel
            discoveredServers = discoveredServers.values.toList(),
            isDiscovering = isDiscoveringActive,
            onStartDiscovery = {
                networkDiscoveryService.startDiscovery()
            },
            onRefreshDiscovery = {
                networkDiscoveryService.stopDiscovery()
                networkDiscoveryService.startDiscovery()
            }
        )
    }
    
    if (showUsbConnectionDialog) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing USB Connection Dialog", ComponentType.UI_DIALOG)
        }
        val context = LocalContext.current
        UsbConnectionDialog(
            onDismiss = {
                viewModel.logInteraction("USB Connection Dialog dismissed", ComponentType.UI_DIALOG)
                showUsbConnectionDialog = false
            },
            onConnect = {
                viewModel.connectViaUSB(context)
                showUsbConnectionDialog = false
            }
        )
    }
    
    if (showMyInstantDownloader) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing MyInstantDownloader", ComponentType.UI_DIALOG)
        }
        MyInstantDownloader(
            onDismiss = {
                viewModel.logInteraction("MyInstantDownloader dismissed", ComponentType.UI_DIALOG)
                showMyInstantDownloader = false
            },
            onDownloadComplete = { fileName, filePath ->
                // Handle download completion
                snackbarMessage = "Downloaded $fileName"
            }
        )
    }
    
    if (showAppearanceSettings) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Appearance Settings Dialog", ComponentType.UI_DIALOG)
        }
        AppearanceSettingsDialog(
            onDismiss = {
                viewModel.logInteraction("Appearance Settings Dialog dismissed", ComponentType.UI_DIALOG)
                showAppearanceSettings = false
            },
            currentTheme = "default",
            currentAccentColor = "blue",
            isDarkTheme = false,
            onThemeChange = { /* TODO: Implement theme change */ },
            onAccentColorChange = { /* TODO: Implement accent color change */ },
            onDarkThemeToggle = { /* TODO: Implement dark theme toggle */ }
        )
    }
    
    if (showDownloadLocationDialog) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Download Location Dialog", ComponentType.UI_DIALOG)
        }
        DownloadLocationDialog(
            onDismiss = {
                viewModel.logInteraction("Download Location Dialog dismissed", ComponentType.UI_DIALOG)
                showDownloadLocationDialog = false
            },
            onLocationSelected = { location ->
                settingsRepository.setDownloadLocation(location)
                showDownloadLocationDialog = false
            },
            currentLocation = downloadLocation
        )
    }
    
    if (showGridLayoutSettings) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Grid Layout Settings Dialog", ComponentType.UI_DIALOG)
        }
        val currentLayout by viewModel.currentLayout.collectAsState()
        currentLayout?.let { layout ->
            GridLayoutSettingsDialog(
                currentLayout = layout,
                onDismiss = {
                    viewModel.logInteraction("Grid Layout Settings Dialog dismissed", ComponentType.UI_DIALOG)
                    showGridLayoutSettings = false
                },
                onSaveLayout = { updatedLayout ->
                    viewModel.updateLayout(updatedLayout)
                    showGridLayoutSettings = false
                }
            )
        }
    }
    
    if (showAnalyticsDashboard) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Analytics Dashboard", ComponentType.UI_MONITORING)
        }
        AnalyticsDashboard(
            multiTransportManager = multiTransportManager,
            connectionAnalytics = connectionAnalytics,
            onClose = {
                viewModel.logInteraction("Analytics Dashboard dismissed", ComponentType.UI_MONITORING)
                showAnalyticsDashboard = false
            }
        )
    }
}

@Composable
fun SettingsSectionCard(section: SettingsSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            section.items.forEachIndexed { index, item ->
                SettingsItemRow(
                    item = item,
                    showDivider = index < section.items.size - 1
                )
            }
        }
    }
}

@Composable
fun SettingsItemRow(
    item: SettingsItem,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { item.onClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 40.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
} 