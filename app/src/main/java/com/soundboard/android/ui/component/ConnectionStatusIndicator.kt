package com.soundboard.android.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboard.android.network.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedConnectionStatusIndicator(
    connectionStatus: ConnectionStatus,
    connectionLatency: Long? = null,
    networkType: String? = null,
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            is ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
            is ConnectionStatus.Connecting -> MaterialTheme.colorScheme.secondary
            is ConnectionStatus.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 300)
    )
    
    val pulseAnimation = rememberInfiniteTransition()
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionStatus) {
                is ConnectionStatus.Connected -> MaterialTheme.colorScheme.primaryContainer
                is ConnectionStatus.Connecting -> MaterialTheme.colorScheme.secondaryContainer
                is ConnectionStatus.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Icon and Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated status indicator
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulse background for connecting status
                    if (connectionStatus is ConnectionStatus.Connecting) {
                        Canvas(modifier = Modifier.size(32.dp)) {
                            drawCircle(
                                color = animatedColor.copy(alpha = pulseAlpha * 0.3f),
                                radius = size.minDimension / 2
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = getConnectionIcon(connectionStatus),
                        contentDescription = "Connection Status",
                        tint = animatedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = getConnectionStatusText(connectionStatus),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (connectionStatus) {
                            is ConnectionStatus.Connected -> MaterialTheme.colorScheme.onPrimaryContainer
                            is ConnectionStatus.Connecting -> MaterialTheme.colorScheme.onSecondaryContainer
                            is ConnectionStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    // Connection details
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Network type indicator
                        networkType?.let { type ->
                            Surface(
                                color = animatedColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = animatedColor
                                )
                            }
                        }
                        
                        // Latency indicator
                        if (connectionStatus is ConnectionStatus.Connected && connectionLatency != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Latency",
                                    modifier = Modifier.size(12.dp),
                                    tint = getLatencyColor(connectionLatency)
                                )
                                Text(
                                    text = "${connectionLatency}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getLatencyColor(connectionLatency)
                                )
                            }
                        }
                    }
                    
                    // Error message
                    if (connectionStatus is ConnectionStatus.Error) {
                        Text(
                            text = connectionStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2
                        )
                    }
                }
            }
            
            // Action buttons
            if (connectionStatus is ConnectionStatus.Error) {
                IconButton(
                    onClick = onRetryClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Connection"
                    )
                }
            }
        }
    }
}

@Composable
private fun getConnectionIcon(status: ConnectionStatus): ImageVector {
    return when (status) {
        is ConnectionStatus.Connected -> Icons.Default.CheckCircle
        is ConnectionStatus.Connecting -> Icons.Default.Sync
        is ConnectionStatus.Error -> Icons.Default.Error
        else -> Icons.Default.Circle
    }
}

private fun getConnectionStatusText(status: ConnectionStatus): String {
    return when (status) {
        is ConnectionStatus.Connected -> "Connected via USB"
        is ConnectionStatus.Connecting -> "Connecting..."
        is ConnectionStatus.Error -> "Connection Failed"
        else -> "Disconnected"
    }
}

@Composable
private fun getLatencyColor(latency: Long): Color {
    return when {
        latency < 50 -> Color(0xFF4CAF50) // Green - Excellent
        latency < 100 -> Color(0xFFFF9800) // Orange - Good
        latency < 200 -> Color(0xFFF44336) // Red - Poor
        else -> Color(0xFF9E9E9E) // Gray - Very Poor
    }
}

// Network quality indicator with detailed metrics
@Composable
fun ConnectionQualityCard(
    connectionStatus: ConnectionStatus,
    latency: Long?,
    packetLoss: Float?,
    connectionDuration: Long?,
    reconnectCount: Int?,
    modifier: Modifier = Modifier
) {
    if (connectionStatus !is ConnectionStatus.Connected) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connection Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Quality metrics
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    QualityMetric(
                        label = "Latency",
                        value = latency?.let { "${it}ms" } ?: "N/A",
                        quality = latency?.let { getConnectionQuality(it) } ?: ConnectionQuality.UNKNOWN
                    )
                }
                
                item {
                    QualityMetric(
                        label = "Stability",
                        value = packetLoss?.let { "${String.format("%.1f", it)}%" } ?: "N/A",
                        quality = packetLoss?.let { 
                            when {
                                it < 1f -> ConnectionQuality.EXCELLENT
                                it < 3f -> ConnectionQuality.GOOD
                                it < 10f -> ConnectionQuality.FAIR
                                else -> ConnectionQuality.POOR
                            }
                        } ?: ConnectionQuality.UNKNOWN
                    )
                }
                
                item {
                    QualityMetric(
                        label = "Uptime",
                        value = connectionDuration?.let { formatDuration(it) } ?: "N/A",
                        quality = ConnectionQuality.GOOD
                    )
                }
                
                reconnectCount?.let { count ->
                    item {
                        QualityMetric(
                            label = "Reconnects",
                            value = count.toString(),
                            quality = when {
                                count == 0 -> ConnectionQuality.EXCELLENT
                                count < 3 -> ConnectionQuality.GOOD
                                count < 10 -> ConnectionQuality.FAIR
                                else -> ConnectionQuality.POOR
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

@Composable
private fun QualityMetric(
    label: String,
    value: String,
    quality: ConnectionQuality
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (quality) {
                ConnectionQuality.EXCELLENT -> Color(0xFF4CAF50)
                ConnectionQuality.GOOD -> Color(0xFF8BC34A)
                ConnectionQuality.FAIR -> Color(0xFFFF9800)
                ConnectionQuality.POOR -> Color(0xFFF44336)
                ConnectionQuality.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getConnectionQuality(latency: Long): ConnectionQuality {
    return when {
        latency < 30 -> ConnectionQuality.EXCELLENT
        latency < 60 -> ConnectionQuality.GOOD
        latency < 120 -> ConnectionQuality.FAIR
        else -> ConnectionQuality.POOR
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

@Composable
fun ConnectionStatusIndicator(
    connectionStatus: ConnectionStatus,
    serverInfo: String?,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isConnecting = connectionStatus is ConnectionStatus.Connecting
    val isError = connectionStatus is ConnectionStatus.Error
    
    // Extract latency information
    val latencyMs = (connectionStatus as? ConnectionStatus.Connected)?.latencyMs ?: -1
    
    // Animated colors
    val statusColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            is ConnectionStatus.Connected -> Color(0xFF4CAF50) // Green
            is ConnectionStatus.Connecting -> Color(0xFFFF9800) // Orange
            is ConnectionStatus.Error -> Color(0xFFF44336) // Red
            is ConnectionStatus.Disconnected -> Color(0xFF9E9E9E) // Gray
        },
        animationSpec = tween(300),
        label = "statusColor"
    )
    
    // Pulsing animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "connectionPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator with animation
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing background for connecting state
            if (isConnecting) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(pulseScale)
                        .background(
                            statusColor.copy(alpha = pulseAlpha * 0.3f),
                            CircleShape
                        )
                )
            }
            
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
                    .scale(if (isConnecting) pulseScale * 0.8f else 1f)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Status text with enhanced styling
        Column {
            Text(
                text = when (connectionStatus) {
                    is ConnectionStatus.Connected -> "Connected"
                    is ConnectionStatus.Connecting -> "Connecting..."
                    is ConnectionStatus.Error -> "Connection Error"
                    is ConnectionStatus.Disconnected -> "Disconnected"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            
            // Server info, latency, or error message
            when {
                serverInfo != null && isConnected -> {
                    Column {
                        Text(
                            text = serverInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Show latency if available
                        if (latencyMs > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Latency status indicator dot
                                PulsingDot(
                                    color = when {
                                        latencyMs < 100 -> Color(0xFF4CAF50) // Green - Excellent
                                        latencyMs < 300 -> Color(0xFFFF9800) // Orange - Good  
                                        else -> Color(0xFFF44336) // Red - Poor
                                    },
                                    size = 6f,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                
                                Text(
                                    text = "${latencyMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        latencyMs < 100 -> Color(0xFF4CAF50)
                                        latencyMs < 300 -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Text(
                                    text = when {
                                        latencyMs < 100 -> "Excellent"
                                        latencyMs < 300 -> "Good"
                                        else -> "Poor"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                isError -> {
                    Text(
                        text = (connectionStatus as ConnectionStatus.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        maxLines = 2
                    )
                }
                isConnecting -> {
                    Text(
                        text = "Establishing connection...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedConnectionButton(
    connectionStatus: ConnectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionStatus is ConnectionStatus.Connected
    val isConnecting = connectionStatus is ConnectionStatus.Connecting
    
    // Animated button properties
    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> Color(0xFF4CAF50).copy(alpha = 0.8f)
            isConnecting -> Color(0xFFFF9800).copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300),
        label = "buttonColor"
    )
    
    val buttonScale by animateFloatAsState(
        targetValue = if (isConnecting) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    
    Button(
        onClick = onClick,
        enabled = !isConnecting,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        modifier = modifier
            .scale(buttonScale)
            .height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isConnecting) {
                // Loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.Wifi
                        else -> Icons.Default.WifiOff
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            
            Text(
                text = when {
                    isConnected -> "Connected"
                    isConnecting -> "Connecting"
                    else -> "Connect"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LoadingStateOverlay(
    isVisible: Boolean,
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingDot(
    color: Color,
    size: Float = 8f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    
    Canvas(
        modifier = modifier.size(size.dp)
    ) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = size.dp.toPx() / 2
        )
    }
} 