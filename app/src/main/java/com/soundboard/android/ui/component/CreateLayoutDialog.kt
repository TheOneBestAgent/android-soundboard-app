package com.soundboard.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
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
import com.soundboard.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLayoutDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, preset: LayoutPreset) -> Unit
) {
    var layoutName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(LayoutPreset.STANDARD) }
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
                Text(
                    "Create New Layout",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "Design your custom soundboard layout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Layout Name Input
                OutlinedTextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    label = { Text("Layout Name") },
                    placeholder = { Text("Enter layout name...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        focusedLabelColor = NeonPink,
                        cursorColor = NeonPink,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        unfocusedBorderColor = TextDisabled,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preset Selector
                Text(
                    "Layout Preset",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPresetPicker = !showPresetPicker },
                    colors = CardDefaults.cardColors(
                        containerColor = ButtonSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
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
                                selectedPreset.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                selectedPreset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        
                        Surface(
                            color = NeonPink.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${selectedPreset.columns}×${selectedPreset.rows}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonPink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Preset Options (when expanded)
                if (showPresetPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(LayoutPreset.values()) { preset ->
                            PresetOption(
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
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (layoutName.isNotBlank()) {
                                onCreate(layoutName.trim(), selectedPreset)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = layoutName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPink,
                            disabledContainerColor = ButtonSurface
                        )
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun PresetOption(
    preset: LayoutPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonPink.copy(alpha = 0.1f) else ButtonSurface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) NeonPink else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) NeonPinkLight else TextSecondary
                )
            }
            
            Surface(
                color = if (isSelected) NeonPink.copy(alpha = 0.2f) else TextDisabled.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "${preset.columns}×${preset.rows}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) NeonPink else TextDisabled,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 