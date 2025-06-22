package com.soundboard.android.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundboard.android.data.model.SoundButton
import com.soundboard.android.network.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignShowcase(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Modern UI Components Showcase",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✨ Design Improvements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val improvements = listOf(
                        "Minimizable Connection Bar" to "Tap to expand/collapse, saves space when not needed",
                        "Modern Volume Control" to "Material 3 sliders with preset buttons and distribution analytics",
                        "Smooth Animations" to "Fluid transitions using modern easing curves",
                        "Better Visual Hierarchy" to "Clear information hierarchy with improved typography",
                        "Adaptive Colors" to "Dynamic colors that respond to connection state",
                        "Compact Mode" to "Essential info always visible, detailed controls on demand"
                    )
                    
                    improvements.forEach { (title, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Text(
                text = "Connection Bar (Minimizable)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MinimizableConnectionBar(
                connectionStatus = ConnectionStatus.Connected(latencyMs = 25L),
                serverInfo = "192.168.1.100:8080",
                onConnectionClick = { },
                onSettingsClick = { },
                onAddButtonClick = { },
                onDisconnectClick = { }
            )
        }
        
        item {
            Text(
                text = "Modern Volume Control",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ModernVolumeControl(
                soundButtons = listOf(
                    SoundButton(
                        name = "Sample 1",
                        filePath = "sample1.mp3",
                        isLocalFile = false,
                        positionX = 0,
                        positionY = 0,
                        volume = 0.8f,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    SoundButton(
                        name = "Sample 2",
                        filePath = "sample2.mp3",
                        isLocalFile = false,
                        positionX = 1,
                        positionY = 0,
                        volume = 0.6f,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    SoundButton(
                        name = "Sample 3",
                        filePath = "sample3.mp3",
                        isLocalFile = false,
                        positionX = 2,
                        positionY = 0,
                        volume = 0.3f,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                ),
                onGlobalVolumeChange = { },
                onNormalizeVolumes = { },
                onResetAllVolumes = { }
            )
        }
    }
} 