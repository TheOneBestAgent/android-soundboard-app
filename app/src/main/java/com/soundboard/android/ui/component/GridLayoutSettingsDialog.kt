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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.soundboard.android.data.model.LayoutPreset
import com.soundboard.android.data.model.SoundboardLayout
import com.soundboard.android.ui.theme.*

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
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBlueSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
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
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Current Layout Info
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkBlueVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Current Layout: ${currentLayout.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${selectedColumns}×${selectedRows} grid • ${selectedColumns * selectedRows} buttons max",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonPinkLight
                                )
                            }
                        }
                    }
                    
                    // Preset Selection
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
                        Text(
                            text = "Custom Grid Size",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Columns
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Columns",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { 
                                            if (selectedColumns > 2) {
                                                selectedColumns--
                                                selectedPreset = LayoutPreset.CUSTOM
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedColumns > 2) NeonPink.copy(alpha = 0.2f) else ButtonSurface.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Decrease columns",
                                            tint = if (selectedColumns > 2) NeonPink else TextDisabled,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = selectedColumns.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            if (selectedColumns < 8) {
                                                selectedColumns++
                                                selectedPreset = LayoutPreset.CUSTOM
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedColumns < 8) NeonPink.copy(alpha = 0.2f) else ButtonSurface.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Increase columns",
                                            tint = if (selectedColumns < 8) NeonPink else TextDisabled,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Rows
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rows",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { 
                                            if (selectedRows > 2) {
                                                selectedRows--
                                                selectedPreset = LayoutPreset.CUSTOM
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedRows > 2) NeonPink.copy(alpha = 0.2f) else ButtonSurface.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Decrease rows",
                                            tint = if (selectedRows > 2) NeonPink else TextDisabled,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = selectedRows.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            if (selectedRows < 10) {
                                                selectedRows++
                                                selectedPreset = LayoutPreset.CUSTOM
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedRows < 10) NeonPink.copy(alpha = 0.2f) else ButtonSurface.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Increase rows",
                                            tint = if (selectedRows < 10) NeonPink else TextDisabled,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Visual Preview
                    item {
                        Text(
                            text = "Grid Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        GridPreview(
                            columns = selectedColumns,
                            rows = selectedRows,
                            spacing = buttonSpacing,
                            cornerRadius = cornerRadius
                        )
                    }
                    
                    // Button Appearance
                    item {
                        Text(
                            text = "Button Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Button Spacing
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Button Spacing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${buttonSpacing.toInt()}dp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonPink,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Slider(
                                value = buttonSpacing,
                                onValueChange = { buttonSpacing = it },
                                valueRange = 4f..16f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonPink,
                                    activeTrackColor = NeonPink,
                                    inactiveTrackColor = ButtonSurface
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Corner Radius
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Corner Radius",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${cornerRadius.toInt()}dp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonPink,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Slider(
                                value = cornerRadius,
                                onValueChange = { cornerRadius = it },
                                valueRange = 4f..24f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonPink,
                                    activeTrackColor = NeonPink,
                                    inactiveTrackColor = ButtonSurface
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Glow Effect Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { enableGlowEffect = !enableGlowEffect }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = enableGlowEffect,
                                onCheckedChange = { enableGlowEffect = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonPink,
                                    checkedTrackColor = NeonPink.copy(alpha = 0.5f),
                                    uncheckedThumbColor = TextDisabled,
                                    uncheckedTrackColor = ButtonSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Neon Glow Effect",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Add glowing borders to sound buttons",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
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
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f))
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
                                layoutPreset = selectedPreset,
                                maxButtons = selectedColumns * selectedRows,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveLayout(updatedLayout)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Changes")
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