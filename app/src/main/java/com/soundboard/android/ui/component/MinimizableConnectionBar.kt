package com.soundboard.android.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboard.android.network.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimizableConnectionBar(
    connectionStatus: ConnectionStatus,
    serverInfo: String?,
    onConnectionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddButtonClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isConnecting = connectionStatus is ConnectionStatus.Connecting
    val isError = connectionStatus is ConnectionStatus.Error
    
    // Status color with modern Material 3 approach
    val statusColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            is ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
            is ConnectionStatus.Connecting -> MaterialTheme.colorScheme.tertiary
            is ConnectionStatus.Error -> MaterialTheme.colorScheme.error
            is ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(400, easing = EaseInOutCubic),
        label = "statusColor"
    )
    
    // Container color for modern elevated appearance
    val containerColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
            isConnecting -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(400, easing = EaseInOutCubic),
        label = "containerColor"
    )
    
    // Pulsing animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "connectionPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 8.dp else 4.dp
        )
    ) {
        Column {
            // Minimized header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status indicator with modern design
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background circle for status
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(if (isConnecting) pulseScale else 1f)
                                .background(
                                    statusColor.copy(alpha = 0.12f),
                                    CircleShape
                                )
                        )
                        
                        // Status icon
                        Icon(
                            imageVector = when (connectionStatus) {
                                is ConnectionStatus.Connected -> Icons.Default.Wifi
                                is ConnectionStatus.Connecting -> Icons.Default.WifiFind
                                is ConnectionStatus.Error -> Icons.Default.WifiOff
                                is ConnectionStatus.Disconnected -> Icons.Default.WifiOff
                            },
                            contentDescription = "Connection Status",
                            tint = statusColor,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(if (isConnecting) pulseScale * 0.9f else 1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Status text with modern typography
                    Column {
                        Text(
                            text = when (connectionStatus) {
                                is ConnectionStatus.Connected -> "Connected"
                                is ConnectionStatus.Connecting -> "Connecting..."
                                is ConnectionStatus.Error -> "Connection Error"
                                is ConnectionStatus.Disconnected -> "Disconnected"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (!isExpanded && serverInfo != null && isConnected) {
                            Text(
                                text = serverInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                // Expand/collapse indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Expanded content with smooth animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = EaseInOutCubic)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = EaseInOutCubic)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Detailed connection info
                    if (serverInfo != null && isConnected) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Server: $serverInfo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    if (isError) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = (connectionStatus as ConnectionStatus.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // Action buttons with modern Material 3 styling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Connection button
                        FilledTonalButton(
                            onClick = onConnectionClick,
                            enabled = !isConnecting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = statusColor.copy(alpha = 0.12f),
                                contentColor = statusColor
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = statusColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Connection Settings",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnecting) "Connecting..." else "Connect",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        
                        // Quick action buttons
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = onAddButtonClick,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Sound Button",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (isConnected) {
                            IconButton(
                                onClick = onDisconnectClick,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Disconnect",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 