package com.soundboard.android.ui.monitoring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundboard.android.diagnostics.*
import com.soundboard.android.data.model.AlertEvent as UIAlertEvent
import com.soundboard.android.data.model.SystemStatus
import com.soundboard.android.data.model.AlertStatistics as UIAlertStatistics
import com.soundboard.android.data.model.HealthTrend
import com.soundboard.android.data.model.TrendDirection
import com.soundboard.android.data.model.ResourceTrends
import com.soundboard.android.data.model.ComponentHealth
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlin.math.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * MonitoringDashboard - Real-time system monitoring and diagnostics UI
 * 
 * Provides comprehensive real-time visualization of system health, performance metrics,
 * bottlenecks, and diagnostic data with interactive charts and status indicators.
 * 
 * Key Features:
 * - Real-time health monitoring with visual indicators
 * - Interactive performance charts and trend analysis
 * - Component health grid with detailed metrics
 * - Bottleneck detection and recommendations display
 * - Log pattern visualization and anomaly alerts
 * - Export capabilities and diagnostic actions
 * - Responsive Material Design 3 interface
 */
@Composable
fun MonitoringDashboard(
    modifier: Modifier = Modifier,
    viewModel: MonitoringViewModel = hiltViewModel()
) {
    val systemStatus by viewModel.systemStatus.collectAsState(SystemStatus())
    val alertUpdates by viewModel.alertUpdates.collectAsState(emptyList())
    val alertHistory by viewModel.alertHistory.collectAsState(emptyList())
    val alertStatistics by viewModel.alertStatistics.collectAsState(UIAlertStatistics())
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // System Status Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "System Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusItem(
                        title = "Active Alerts",
                        value = systemStatus.activeAlertCount.toString(),
                        color = if (systemStatus.activeAlertCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    
                    StatusItem(
                        title = "Last 24h",
                        value = alertStatistics.totalAlertsLast24h.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    StatusItem(
                        title = "Resolution Time",
                        value = "${alertStatistics.averageResolutionTimeMinutes}m",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        
        // Alert History
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Alert History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn {
                    items(alertHistory) { event ->
                        AlertHistoryItem(event = event)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AlertHistoryItem(
    event: UIAlertEvent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = event.alert.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "${event.alert.type} - ${event.alert.severity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = formatTimestamp(event.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // TODO: Implement proper timestamp formatting
    return timestamp.toString()
}

// =============================================================================
// HEADER COMPONENTS
// =============================================================================

@Composable
private fun MonitoringHeader(
    healthScore: HealthScore,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Health Monitor",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time diagnostics and performance monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onRefresh
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                IconButton(
                    onClick = onExport
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Export Report",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// =============================================================================
// HEALTH OVERVIEW COMPONENTS
// =============================================================================

@Composable
private fun SystemHealthOverview(
    healthScore: HealthScore,
    resourceUsage: ResourceUsageSnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Health Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthScoreIndicator(
                    label = "Overall Health",
                    score = healthScore.overall,
                    trend = when(healthScore.trend) {
                        com.soundboard.android.diagnostics.TrendDirection.STABLE -> HealthTrend.STABLE
                        com.soundboard.android.diagnostics.TrendDirection.DECREASING -> HealthTrend.DEGRADING
                        else -> HealthTrend.IMPROVING
                    },
                    modifier = Modifier.weight(1f)
                )
                
                HealthScoreIndicator(
                    label = "Memory",
                    score = 1.0 - (resourceUsage.memoryUsed / resourceUsage.memoryTotal),
                    trend = HealthTrend.STABLE,
                    modifier = Modifier.weight(1f)
                )
                
                HealthScoreIndicator(
                    label = "CPU",
                    score = 1.0 - (resourceUsage.cpuUsage / 100.0),
                    trend = HealthTrend.STABLE,
                    modifier = Modifier.weight(1f)
                )
                
                HealthScoreIndicator(
                    label = "Network",
                    score = when {
                        resourceUsage.networkLatency < 50 -> 1.0
                        resourceUsage.networkLatency < 100 -> 0.8
                        resourceUsage.networkLatency < 200 -> 0.6
                        resourceUsage.networkLatency < 500 -> 0.4
                        else -> 0.2
                    },
                    trend = HealthTrend.STABLE,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HealthScoreIndicator(
    label: String,
    score: Double,
    trend: HealthTrend,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "HealthScore"
    )
    
    val color = when {
        score >= 0.8 -> Color(0xFF4CAF50) // Green
        score >= 0.6 -> Color(0xFFFF9800) // Orange
        score >= 0.3 -> Color(0xFFFF5722) // Red-Orange
        else -> Color(0xFFF44336) // Red
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(64.dp)
            ) {
                val strokeWidth = 8.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                
                // Background circle
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = radius,
                    style = Stroke(strokeWidth)
                )
                
                // Progress arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animatedScore * 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            Text(
                text = "${(animatedScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        
        TrendIndicator(trend = trend)
    }
}

@Composable
private fun TrendIndicator(
    trend: HealthTrend,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (trend) {
        HealthTrend.IMPROVING -> Icons.Default.TrendingUp to Color(0xFF4CAF50)
        HealthTrend.STABLE -> Icons.Default.TrendingFlat to Color(0xFF757575)
        HealthTrend.DEGRADING -> Icons.Default.TrendingDown to Color(0xFFFF9800)
        HealthTrend.CRITICAL -> Icons.Default.TrendingDown to Color(0xFFF44336)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = trend.name,
        tint = color,
        modifier = modifier.size(16.dp)
    )
}

// =============================================================================
// QUICK STATS COMPONENTS
// =============================================================================

@Composable
private fun QuickStatsRow(
    stats: QuickStats,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickStatCard(
                icon = Icons.Default.Speed,
                label = "Uptime",
                value = formatDuration(stats.uptime),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            QuickStatCard(
                icon = Icons.Default.Memory,
                label = "Memory",
                value = "${stats.memoryUsedMB}MB",
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        item {
            QuickStatCard(
                icon = Icons.Default.NetworkCheck,
                label = "Latency",
                value = "${stats.networkLatency.toInt()}ms",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        
        item {
            QuickStatCard(
                icon = Icons.Default.BatteryFull,
                label = "Battery",
                value = "${stats.batteryLevel.toInt()}%",
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            QuickStatCard(
                icon = Icons.Default.Error,
                label = "Errors",
                value = stats.errorCount.toString(),
                color = if (stats.errorCount > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// REAL-TIME METRICS COMPONENTS
// =============================================================================

@Composable
private fun RealTimeMetricsSection(
    resourceUsage: ResourceUsageSnapshot,
    trends: ResourceTrends,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Real-Time Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBar(
                    label = "Memory Usage",
                    value = resourceUsage.memoryUsed,
                    maxValue = resourceUsage.memoryTotal,
                    unit = "MB",
                    trend = trends.memoryTrend,
                    color = MaterialTheme.colorScheme.primary
                )
                
                MetricBar(
                    label = "CPU Usage",
                    value = resourceUsage.cpuUsage,
                    maxValue = 100.0,
                    unit = "%",
                    trend = trends.cpuTrend,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                MetricBar(
                    label = "Network Latency",
                    value = resourceUsage.networkLatency,
                    maxValue = 500.0,
                    unit = "ms",
                    trend = trends.networkTrend,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                MetricBar(
                    label = "Battery Level",
                    value = resourceUsage.batteryLevel,
                    maxValue = 100.0,
                    unit = "%",
                    trend = trends.batteryTrend,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun MetricBar(
    label: String,
    value: Double,
    maxValue: Double,
    unit: String,
    trend: TrendDirection,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = (value / maxValue).coerceIn(0.0, 1.0)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "MetricProgress"
    )
    
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${value.format(1)} $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                TrendIcon(trend = trend)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun TrendIcon(
    trend: TrendDirection,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (trend) {
        TrendDirection.INCREASING -> Icons.Default.ArrowUpward to Color(0xFFF44336)
        TrendDirection.DECREASING -> Icons.Default.ArrowDownward to Color(0xFF4CAF50)
        TrendDirection.STABLE -> Icons.Default.Remove to Color(0xFF757575)
        TrendDirection.UNKNOWN -> Icons.Default.Help to Color(0xFF757575)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = trend.name,
        tint = color,
        modifier = modifier.size(16.dp)
    )
}

// =============================================================================
// COMPONENT HEALTH GRID
// =============================================================================

@Composable
private fun ComponentHealthGrid(
    componentHealth: Map<ComponentType, ComponentHealth>,
    onComponentClick: (ComponentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Component Health",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ComponentType.values()) { component ->
                    val health = componentHealth[component]
                    ComponentHealthCard(
                        component = component,
                        health = health,
                        onClick = { onComponentClick(component) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentHealthCard(
    component: ComponentType,
    health: ComponentHealth?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val score = health?.score ?: 0.0
    val status = health?.status ?: ComponentStatus.OFFLINE
    
    val statusColor = when (status) {
        ComponentStatus.HEALTHY -> Color(0xFF4CAF50)
        ComponentStatus.DEGRADED -> Color(0xFFFF9800)
        ComponentStatus.CRITICAL -> Color(0xFFF44336)
        ComponentStatus.OFFLINE -> Color(0xFF757575)
    }
    
    Card(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Component icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(statusColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getComponentIcon(component),
                    contentDescription = component.name,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formatComponentName(component),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${(score * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            
            Text(
                text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
    }
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

private fun getComponentIcon(component: ComponentType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (component) {
        ComponentType.CONNECTION_POOL -> Icons.Default.Hub
        ComponentType.CACHE -> Icons.Default.Storage
        ComponentType.COMPRESSION -> Icons.Default.Compress
        ComponentType.PIPELINE -> Icons.Default.Timeline
        ComponentType.METRICS -> Icons.Default.Analytics
        ComponentType.NETWORK -> Icons.Default.NetworkCheck
        ComponentType.SYSTEM -> Icons.Default.Computer
        ComponentType.UI_MAIN -> Icons.Default.Home
        ComponentType.UI_SETTINGS -> Icons.Default.Settings
        ComponentType.UI_DIALOG -> Icons.Default.Chat
        ComponentType.UI_LAYOUT -> Icons.Default.ViewQuilt
        ComponentType.UI_SOUNDBOARD -> Icons.Default.VolumeUp
        ComponentType.UI_MONITORING -> Icons.Default.Monitor
    }
}

private fun formatComponentName(component: ComponentType): String {
    return when (component) {
        ComponentType.CONNECTION_POOL -> "Connection\nPool"
        ComponentType.CACHE -> "Cache"
        ComponentType.COMPRESSION -> "Compression"
        ComponentType.PIPELINE -> "Pipeline"
        ComponentType.METRICS -> "Metrics"
        ComponentType.NETWORK -> "Network"
        ComponentType.SYSTEM -> "System"
        ComponentType.UI_MAIN -> "Main UI"
        ComponentType.UI_SETTINGS -> "Settings"
        ComponentType.UI_DIALOG -> "Dialogs"
        ComponentType.UI_LAYOUT -> "Layout"
        ComponentType.UI_SOUNDBOARD -> "Soundboard"
        ComponentType.UI_MONITORING -> "Monitoring"
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

// =============================================================================
// PLACEHOLDER COMPONENTS FOR REMAINING SECTIONS
// =============================================================================

@Composable
private fun BottlenecksSection(
    bottlenecks: List<Bottleneck>,
    onBottleneckClick: (Bottleneck) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Bottlenecks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            bottlenecks.take(3).forEach { bottleneck ->
                BottleneckItem(
                    bottleneck = bottleneck,
                    onClick = { onBottleneckClick(bottleneck) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BottleneckItem(
    bottleneck: Bottleneck,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bottleneck.type.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Impact: ${bottleneck.impact.userImpact.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            AssistChip(
                onClick = onClick,
                label = { Text(bottleneck.severity.name) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = when (bottleneck.severity) {
                        Severity.CRITICAL -> Color(0xFFF44336)
                        Severity.HIGH -> Color(0xFFFF9800)
                        Severity.MEDIUM -> Color(0xFFFFEB3B)
                        Severity.LOW -> Color(0xFF4CAF50)
                    }.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun LogPatternsSection(
    patterns: List<LogPattern>,
    anomalies: List<LogAnomaly>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Log Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (patterns.isNotEmpty()) {
                Text(
                    text = "${patterns.size} patterns detected",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (anomalies.isNotEmpty()) {
                Text(
                    text = "${anomalies.size} anomalies found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun PerformanceTrendsChart(
    trends: List<Double>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Trends",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Performance chart visualization\n(${trends.size} data points)",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticActionsPanel(
    onRunDiagnostics: () -> Unit,
    onClearLogs: () -> Unit,
    onOptimizePerformance: () -> Unit,
    onGenerateReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Diagnostic Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(
                        onClick = onRunDiagnostics,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Diagnostics")
                    }
                }
                
                item {
                    OutlinedButton(onClick = onClearLogs) {
                        Icon(Icons.Default.ClearAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Logs")
                    }
                }
                
                item {
                    OutlinedButton(onClick = onOptimizePerformance) {
                        Icon(Icons.Default.Speed, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimize")
                    }
                }
                
                item {
                    OutlinedButton(onClick = onGenerateReport) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        enabled = enabled,
        modifier = modifier.padding(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.small,
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    minHeight: Dp = 32.dp,
    paddingValues: PaddingValues = PaddingValues(horizontal = 8.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        enabled = enabled,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (enabled) 2.dp else 0.dp,
        shadowElevation = if (enabled) 2.dp else 0.dp,
        border = border,
        interactionSource = interactionSource
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            labelColor = labelColor,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun ChipContent(
    label: String,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    paddingValues: PaddingValues
) {
    Row(
        Modifier.padding(paddingValues),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = labelTextStyle,
            color = labelColor
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            trailingIcon()
        }
    }
} 