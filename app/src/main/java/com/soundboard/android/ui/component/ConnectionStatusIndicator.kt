package com.soundboard.android.ui.component

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboard.android.network.ConnectionStatus

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