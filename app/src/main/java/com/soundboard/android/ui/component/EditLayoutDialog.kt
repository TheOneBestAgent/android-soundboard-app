package com.soundboard.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soundboard.android.data.model.LayoutPreset
import com.soundboard.android.data.model.SoundboardLayout
import com.soundboard.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLayoutDialog(
    layout: SoundboardLayout,
    onDismiss: () -> Unit,
    onSave: (SoundboardLayout) -> Unit
) {
    var layoutName by remember { mutableStateOf(layout.name) }
    var layoutDescription by remember { mutableStateOf(layout.description ?: "") }
    var selectedPreset by remember { mutableStateOf(layout.layoutPreset) }
    var showPresetPicker by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkBlueSurface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Edit Layout",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            "Modify layout properties and settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
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
                
                // Layout Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBlueVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${layout.gridColumns}×${layout.gridRows} grid • ${layout.gridColumns * layout.gridRows} buttons max",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonPinkLight
                        )
                        Text(
                            text = "Created: ${java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(layout.createdAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Layout Name Input
                OutlinedTextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    label = { Text("Layout Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        focusedLabelColor = NeonPink,
                        cursorColor = NeonPink
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Layout Description Input
                OutlinedTextField(
                    value = layoutDescription,
                    onValueChange = { layoutDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        focusedLabelColor = NeonPink,
                        cursorColor = NeonPink
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Layout Preset Selector
                Text(
                    text = "Layout Preset",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = selectedPreset.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Current Preset") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPresetPicker = !showPresetPicker },
                    trailingIcon = {
                        Icon(
                            if (showPresetPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle preset picker"
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoralAccent,
                        focusedLabelColor = CoralAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (showPresetPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(LayoutPreset.values()) { preset ->
                            EditPresetOption(
                                preset = preset,
                                isSelected = preset == selectedPreset,
                                onClick = {
                                    selectedPreset = preset
                                    showPresetPicker = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Warning Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CoralAccent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = CoralAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Note: Changing the preset will update the grid dimensions and may affect button positions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val updatedLayout = layout.copy(
                                name = layoutName.trim(),
                                description = if (layoutDescription.isBlank()) null else layoutDescription.trim(),
                                layoutPreset = selectedPreset,
                                gridColumns = selectedPreset.columns,
                                gridRows = selectedPreset.rows,
                                maxButtons = selectedPreset.columns * selectedPreset.rows,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updatedLayout)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = layoutName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink,
                            disabledContainerColor = ButtonSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun EditPresetOption(
    preset: LayoutPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonPink.copy(alpha = 0.2f) else ButtonSurface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.GridView,
                contentDescription = "Preset",
                tint = if (isSelected) NeonPink else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) NeonPink else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    "${preset.columns}×${preset.rows} grid",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) NeonPinkLight else TextSecondary
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = NeonPink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 