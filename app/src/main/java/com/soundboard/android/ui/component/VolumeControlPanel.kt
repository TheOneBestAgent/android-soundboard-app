package com.soundboard.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboard.android.data.model.SoundButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeControlPanel(
    soundButtons: List<SoundButton>,
    onGlobalVolumeChange: (Float) -> Unit,
    onNormalizeVolumes: () -> Unit,
    onResetAllVolumes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPanel by remember { mutableStateOf(false) }
    var globalVolumeMultiplier by remember { mutableStateOf(1.0f) }
    
    // Calculate average volume
    val averageVolume = if (soundButtons.isNotEmpty()) {
        soundButtons.map { it.volume }.average().toFloat()
    } else 1.0f
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume Control",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Volume Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Avg: ${(averageVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showPanel = !showPanel }
                    ) {
                        Icon(
                            imageVector = if (showPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showPanel) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            if (showPanel) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Global volume multiplier
                Text(
                    text = "Global Volume Multiplier: ${(globalVolumeMultiplier * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = globalVolumeMultiplier,
                    onValueChange = { 
                        globalVolumeMultiplier = it
                        onGlobalVolumeChange(it)
                    },
                    valueRange = 0.1f..2.0f,
                    steps = 18, // 0.1 to 2.0 in 0.1 increments
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Volume level indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("50%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("100%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("150%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("200%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Volume presets
                    listOf(0.5f, 0.75f, 1.0f, 1.25f).forEach { preset ->
                        FilterChip(
                            onClick = { 
                                globalVolumeMultiplier = preset
                                onGlobalVolumeChange(preset)
                            },
                            label = { Text("${(preset * 100).toInt()}%") },
                            selected = globalVolumeMultiplier == preset
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = onNormalizeVolumes,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Normalize",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Normalize")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            globalVolumeMultiplier = 1.0f
                            onResetAllVolumes()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset All")
                    }
                }
                
                // Volume distribution info
                if (soundButtons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    VolumeDistributionInfo(soundButtons)
                }
            }
        }
    }
}

@Composable
private fun VolumeDistributionInfo(soundButtons: List<SoundButton>) {
    val volumeRanges = mapOf(
        "Low (0-25%)" to soundButtons.count { it.volume <= 0.25f },
        "Medium (26-50%)" to soundButtons.count { it.volume > 0.25f && it.volume <= 0.5f },
        "High (51-75%)" to soundButtons.count { it.volume > 0.5f && it.volume <= 0.75f },
        "Max (76-100%)" to soundButtons.count { it.volume > 0.75f }
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Volume Distribution",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            volumeRanges.forEach { (range, count) ->
                if (count > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = range,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$count buttons",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
} 