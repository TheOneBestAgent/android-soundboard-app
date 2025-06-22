package com.soundboard.android.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboard.android.network.MultiTransportManager
import com.soundboard.android.network.ConnectionAnalytics
import com.soundboard.android.network.ConnectionMetrics
import java.text.SimpleDateFormat
import java.util.*

/**
 * AnalyticsDashboard - Phase 3: Real-Time Analytics Dashboard
 * 
 * Provides comprehensive monitoring interface for connection analytics,
 * transport performance, and system health metrics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboard(
    multiTransportManager: MultiTransportManager,
    connectionAnalytics: ConnectionAnalytics,
    onClose: () -> Unit
) {
    val connectionState by multiTransportManager.connectionState.collectAsState()
    val transportMetrics by multiTransportManager.transportMetrics.collectAsState()
    val analyticsMetrics by connectionAnalytics.currentMetrics.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Transports", "Performance")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Analytics Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Dashboard"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Connection Status Banner
        ConnectionStatusBanner(connectionState)

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> OverviewTab(connectionState, analyticsMetrics, transportMetrics)
            1 -> TransportsTab(transportMetrics, connectionState)
            2 -> PerformanceTab(analyticsMetrics)
        }
    }
}

@Composable
private fun ConnectionStatusBanner(connectionState: MultiTransportManager.ConnectionState) {
    val statusColor = when {
        !connectionState.isConnected -> MaterialTheme.colorScheme.error
        connectionState.connectionQuality == MultiTransportManager.ConnectionQuality.EXCELLENT -> Color(0xFF4CAF50)
        connectionState.connectionQuality == MultiTransportManager.ConnectionQuality.GOOD -> Color(0xFF8BC34A)
        connectionState.connectionQuality == MultiTransportManager.ConnectionQuality.FAIR -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
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
                    text = if (connectionState.isConnected) "Connected" else "Disconnected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = statusColor
                )
                if (connectionState.currentTransport != null) {
                    Text(
                        text = "via ${connectionState.currentTransport}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QualityIndicator(connectionState.connectionQuality)
                ConnectionIcon(connectionState.isConnected)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    connectionState: MultiTransportManager.ConnectionState,
    analyticsMetrics: ConnectionMetrics,
    transportMetrics: Map<MultiTransportManager.TransportType, MultiTransportManager.TransportMetrics>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Total Connections",
                    value = connectionState.totalConnections.toString(),
                    icon = Icons.Default.Link,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Failovers",
                    value = connectionState.failoverCount.toString(),
                    icon = Icons.Default.SwapHoriz,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Avg Latency",
                    value = "${analyticsMetrics.averageLatency}ms",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Success Rate",
                    value = "${analyticsMetrics.connectionReliability.toInt()}%",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // Transport Health Overview
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Transport Health",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    transportMetrics.forEach { (transport, metrics) ->
                        TransportRow(
                            transport = transport,
                            metrics = metrics,
                            isActive = connectionState.currentTransport == transport
                        )
                        if (transport != transportMetrics.keys.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportsTab(
    transportMetrics: Map<MultiTransportManager.TransportType, MultiTransportManager.TransportMetrics>,
    connectionState: MultiTransportManager.ConnectionState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Transport Status",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    transportMetrics.forEach { (transport, metrics) ->
                        TransportRow(
                            transport = transport,
                            metrics = metrics,
                            isActive = connectionState.currentTransport == transport
                        )
                        if (transport != transportMetrics.keys.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTab(analyticsMetrics: ConnectionMetrics) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Performance Metrics",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Total Sessions", "${analyticsMetrics.totalSessions}")
                        MetricItem("Failed Connections", "${analyticsMetrics.failedConnections}")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Avg Latency", "${analyticsMetrics.averageLatency}ms")
                        MetricItem("Transport Errors", "${analyticsMetrics.transportErrors}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransportRow(
    transport: MultiTransportManager.TransportType,
    metrics: MultiTransportManager.TransportMetrics,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getTransportIcon(transport),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (metrics.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Text(
                text = transport.name,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
            if (isActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(metrics.successRate * 100).toInt()}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${metrics.averageLatency}ms",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QualityIndicator(quality: MultiTransportManager.ConnectionQuality) {
    val (color, icon) = when (quality) {
        MultiTransportManager.ConnectionQuality.EXCELLENT -> Color(0xFF4CAF50) to Icons.Default.SignalWifi4Bar
        MultiTransportManager.ConnectionQuality.GOOD -> Color(0xFF8BC34A) to Icons.Default.SignalWifi4Bar
        MultiTransportManager.ConnectionQuality.FAIR -> Color(0xFFFF9800) to Icons.Default.Wifi
        MultiTransportManager.ConnectionQuality.POOR -> Color(0xFFF44336) to Icons.Default.WifiOff
        MultiTransportManager.ConnectionQuality.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.SignalWifiOff
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Signal Quality: $quality",
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun ConnectionIcon(isConnected: Boolean) {
    Icon(
        imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
        contentDescription = if (isConnected) "Connected" else "Disconnected",
        tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
        modifier = Modifier.size(24.dp)
    )
}

private fun getTransportIcon(transport: MultiTransportManager.TransportType): ImageVector {
    return when (transport) {
        MultiTransportManager.TransportType.WEBSOCKET -> Icons.Default.Wifi
        MultiTransportManager.TransportType.HTTP_POLLING -> Icons.Default.Http
        MultiTransportManager.TransportType.USB_ADB -> Icons.Default.Usb
        MultiTransportManager.TransportType.WIFI_DIRECT -> Icons.Default.WifiTethering
        MultiTransportManager.TransportType.BLUETOOTH -> Icons.Default.Bluetooth
        MultiTransportManager.TransportType.CLOUD_RELAY -> Icons.Default.Cloud
    }
} 