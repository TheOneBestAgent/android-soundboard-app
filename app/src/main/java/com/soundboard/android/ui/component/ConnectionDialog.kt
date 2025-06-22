package com.soundboard.android.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.soundboard.android.data.model.ConnectionHistory
import com.soundboard.android.network.NetworkDiscoveryService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    onDismiss: () -> Unit,
    onConnect: (ipAddress: String, port: Int) -> Unit,
    onQRCodeScan: () -> Unit = {},
    isConnecting: Boolean = false,
    currentIpAddress: String = "",
    currentPort: Int = 8080,
    connectionHistory: List<ConnectionHistory> = emptyList(),
    discoveredServers: List<NetworkDiscoveryService.DiscoveredServer> = emptyList(),
    isDiscovering: Boolean = false,
    onStartDiscovery: () -> Unit = {},
    onRefreshDiscovery: () -> Unit = {}
) {
    var ipAddress by remember { mutableStateOf(TextFieldValue(currentIpAddress)) }
    var port by remember { mutableStateOf(TextFieldValue(currentPort.toString())) }
    var ipError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(if (discoveredServers.isNotEmpty()) 0 else 1) }
    
    val tabs = listOf("Discovery", "Manual", "History", "QR Code")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 20.dp, 24.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connect to Server",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                
                // Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    when (selectedTab) {
                        0 -> AutoDiscoveryContent(
                            discoveredServers = discoveredServers,
                            isDiscovering = isDiscovering,
                            onStartDiscovery = onStartDiscovery,
                            onRefreshDiscovery = onRefreshDiscovery,
                            onConnect = onConnect,
                            isConnecting = isConnecting
                        )
                        
                        1 -> ManualConnectionContent(
                            ipAddress = ipAddress,
                            port = port,
                            ipError = ipError,
                            portError = portError,
                            isConnecting = isConnecting,
                            onIpAddressChange = { 
                                ipAddress = it
                                ipError = null
                            },
                            onPortChange = { 
                                port = it
                                portError = null
                            },
                            onConnect = { 
                                var hasError = false
                                
                                if (ipAddress.text.isBlank()) {
                                    ipError = "IP address is required"
                                    hasError = true
                                }
                                
                                val portInt = port.text.toIntOrNull()
                                if (portInt == null || portInt !in 1..65535) {
                                    portError = "Port must be between 1 and 65535"
                                    hasError = true
                                }
                                
                                if (!hasError) {
                                    onConnect(ipAddress.text.trim(), portInt!!)
                                }
                            }
                        )
                        
                        2 -> HistoryContent(
                            connectionHistory = connectionHistory,
                            onConnect = onConnect,
                            isConnecting = isConnecting
                        )
                        
                        3 -> QRCodeContent(
                            onQRCodeScan = onQRCodeScan,
                            isConnecting = isConnecting
                        )
                    }
                }
                
                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 0.dp, 24.dp, 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isConnecting
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoDiscoveryContent(
    discoveredServers: List<NetworkDiscoveryService.DiscoveredServer>,
    isDiscovering: Boolean,
    onStartDiscovery: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onConnect: (String, Int) -> Unit,
    isConnecting: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Discovery header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Network Discovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (discoveredServers.isNotEmpty()) {
                IconButton(onClick = onRefreshDiscovery, enabled = !isConnecting) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
        
        // Discovery status
        AnimatedVisibility(visible = isDiscovering) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Searching for soundboard servers...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Results
        if (discoveredServers.isNotEmpty()) {
            Text(
                text = "Found ${discoveredServers.size} server${if (discoveredServers.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(discoveredServers) { server ->
                    ServerCard(
                        server = server,
                        onConnect = { onConnect(server.address, server.port) },
                        isConnecting = isConnecting
                    )
                }
            }
        } else if (!isDiscovering) {
            EmptyStateCard(
                icon = Icons.Default.NetworkCheck,
                title = "No servers found",
                subtitle = "Tap 'Start Discovery' to search for servers on your network"
            ) {
                Button(
                    onClick = onStartDiscovery,
                    enabled = !isConnecting
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Discovery")
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: NetworkDiscoveryService.DiscoveredServer,
    onConnect: () -> Unit,
    isConnecting: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onConnect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${server.address}:${server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                QualityBadge(server.quality)
            }
        }
    }
}

@Composable
private fun QualityBadge(quality: NetworkDiscoveryService.NetworkQuality) {
    val (color, text) = when (quality) {
        NetworkDiscoveryService.NetworkQuality.EXCELLENT -> Color(0xFF4CAF50) to "Excellent"
        NetworkDiscoveryService.NetworkQuality.GOOD -> Color(0xFF8BC34A) to "Good"
        NetworkDiscoveryService.NetworkQuality.FAIR -> Color(0xFFFF9800) to "Fair"
        NetworkDiscoveryService.NetworkQuality.POOR -> Color(0xFFf44336) to "Poor"
        NetworkDiscoveryService.NetworkQuality.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant to "Unknown"
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ManualConnectionContent(
    ipAddress: TextFieldValue,
    port: TextFieldValue,
    ipError: String?,
    portError: String?,
    isConnecting: Boolean,
    onIpAddressChange: (TextFieldValue) -> Unit,
    onPortChange: (TextFieldValue) -> Unit,
    onConnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Manual Connection",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedTextField(
            value = ipAddress,
            onValueChange = onIpAddressChange,
            label = { Text("Server IP Address") },
            placeholder = { Text("192.168.1.100") },
            singleLine = true,
            enabled = !isConnecting,
            isError = ipError != null,
            supportingText = if (ipError != null) {
                { Text(ipError) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("Port") },
            placeholder = { Text("8080") },
            singleLine = true,
            enabled = !isConnecting,
            isError = portError != null,
            supportingText = if (portError != null) {
                { Text(portError) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Quick Connect",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onIpAddressChange(TextFieldValue("192.168.3.39"))
                    onPortChange(TextFieldValue("8080"))
                },
                enabled = !isConnecting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Mac Server")
            }
            
            OutlinedButton(
                onClick = {
                    onIpAddressChange(TextFieldValue("10.0.2.2"))
                    onPortChange(TextFieldValue("8080"))
                },
                enabled = !isConnecting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Emulator")
            }
        }
        
        Button(
            onClick = onConnect,
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connecting...")
            } else {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun HistoryContent(
    connectionHistory: List<ConnectionHistory>,
    onConnect: (String, Int) -> Unit,
    isConnecting: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Connection History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        if (connectionHistory.isNotEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(connectionHistory) { history ->
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isConnecting) {
                                onConnect(history.ipAddress, history.port)
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${history.ipAddress}:${history.port}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = "Connection History",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = "Last used: ${dateFormat.format(Date(history.lastConnected))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = if (history.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (history.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            EmptyStateCard(
                icon = Icons.Default.History,
                title = "No connection history",
                subtitle = "Your previous connections will appear here"
            )
        }
    }
}

@Composable
private fun QRCodeContent(
    onQRCodeScan: () -> Unit,
    isConnecting: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "QR Code Pairing",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Instant Connection Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Scan a QR code from your soundboard server to instantly connect with all the correct settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onQRCodeScan,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QR Code")
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            action?.invoke()
        }
    }
} 