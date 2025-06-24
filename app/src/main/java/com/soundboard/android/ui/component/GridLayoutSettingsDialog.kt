package com.soundboard.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soundboard.android.data.model.LayoutPreset
import com.soundboard.android.data.model.SoundboardLayout
import com.soundboard.android.ui.theme.*
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridLayoutSettingsDialog(
    currentLayout: SoundboardLayout,
    onDismiss: () -> Unit,
    onSaveLayout: (SoundboardLayout) -> Unit
) {
    var selectedColumns by remember { mutableStateOf(currentLayout.gridColumns) }
    var selectedRows by remember { mutableStateOf(currentLayout.gridRows) }
    var buttonSpacing by remember { mutableStateOf(currentLayout.buttonSpacing) }
    var cornerRadius by remember { mutableStateOf(currentLayout.cornerRadius) }
    var enableGlowEffect by remember { mutableStateOf(currentLayout.enableGlowEffect) }
    var selectedPreset by remember { mutableStateOf(currentLayout.layoutPreset) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBlueSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header - Fixed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grid Layout Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ButtonSurface)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Scrollable content area
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Layout Presets
                    item {
                        Text(
                            text = "Layout Presets",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(LayoutPreset.values().filter { it != LayoutPreset.CUSTOM }) { preset ->
                                PresetCard(
                                    preset = preset,
                                    isSelected = selectedPreset == preset,
                                    onClick = {
                                        selectedPreset = preset
                                        selectedColumns = preset.columns
                                        selectedRows = preset.rows
                                    }
                                )
                            }
                        }
                    }
                    
                    // Custom Grid Size
                    item {
                        SettingsSection(
                            title = "Custom Grid Size",
                            subtitle = "Configure rows and columns"
                        ) {
                            // Grid size controls
                            Text(
                                text = "Columns: $selectedColumns",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            
                            Slider(
                                value = selectedColumns.toFloat(),
                                onValueChange = { 
                                    selectedColumns = it.toInt()
                                    selectedPreset = LayoutPreset.CUSTOM
                                },
                                valueRange = 2f..6f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Rows: $selectedRows",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            
                            Slider(
                                value = selectedRows.toFloat(),
                                onValueChange = { 
                                    selectedRows = it.toInt()
                                    selectedPreset = LayoutPreset.CUSTOM
                                },
                                valueRange = 3f..8f,
                                steps = 4,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // Spacing and Appearance
                    item {
                        SettingsSection(
                            title = "Spacing & Appearance",
                            subtitle = "Fine-tune the visual style"
                        ) {
                            Text(
                                text = "Button Spacing: ${buttonSpacing.toInt()}dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            
                            Slider(
                                value = buttonSpacing,
                                onValueChange = { buttonSpacing = it },
                                valueRange = 4f..16f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Corner Radius: ${cornerRadius.toInt()}dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            
                            Slider(
                                value = cornerRadius,
                                onValueChange = { cornerRadius = it },
                                valueRange = 4f..24f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable Glow Effect",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                
                                Switch(
                                    checked = enableGlowEffect,
                                    onCheckedChange = { enableGlowEffect = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                    
                    // Preview
                    item {
                        SettingsSection(
                            title = "Preview",
                            subtitle = "See how your layout will look"
                        ) {
                            GridPreview(
                                columns = selectedColumns,
                                rows = selectedRows,
                                spacing = buttonSpacing,
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons - Fixed at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, ButtonSurface)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val updatedLayout = currentLayout.copy(
                                gridColumns = selectedColumns,
                                gridRows = selectedRows,
                                buttonSpacing = buttonSpacing,
                                cornerRadius = cornerRadius,
                                enableGlowEffect = enableGlowEffect,
                                layoutPreset = selectedPreset
                            )
                            onSaveLayout(updatedLayout)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Layout")
                    }
                }
            }
        }
    }
}

@Composable
fun PresetCard(
    preset: LayoutPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonPink.copy(alpha = 0.2f) else ButtonSurface
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(2.dp, NeonPink) else null
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = if (isSelected) NeonPink.copy(alpha = 0.3f) else TextDisabled.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${preset.columns}×${preset.rows}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) NeonPink else TextDisabled,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) NeonPink else TextPrimary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun GridPreview(
    columns: Int,
    rows: Int,
    spacing: Float,
    cornerRadius: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBlueVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${columns}×${rows} Grid Preview",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Calculate preview size based on available space
            val maxWidth = 200.dp
            val buttonSize = (maxWidth - (spacing.dp * (columns - 1))) / columns
            
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                repeat(minOf(rows, 4)) { row -> // Limit preview to 4 rows max
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
                    ) {
                        repeat(columns) { col ->
                            Box(
                                modifier = Modifier
                                    .size(buttonSize)
                                    .clip(RoundedCornerShape(cornerRadius.dp))
                                    .background(
                                        if ((row + col) % 2 == 0) NeonPink.copy(alpha = 0.3f) 
                                        else CoralAccent.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if ((row + col) % 2 == 0) NeonPink.copy(alpha = 0.5f) 
                                        else CoralAccent.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(cornerRadius.dp)
                                    )
                            )
                        }
                    }
                }
                
                if (rows > 4) {
                    Text(
                        text = "... ${rows - 4} more rows",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${columns * rows} buttons total",
                style = MaterialTheme.typography.bodySmall,
                color = NeonPinkLight,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        content()
    }
} 