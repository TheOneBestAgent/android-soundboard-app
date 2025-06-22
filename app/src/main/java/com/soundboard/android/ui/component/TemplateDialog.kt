package com.soundboard.android.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soundboard.android.data.model.*
import com.soundboard.android.ui.theme.*

// Predefined templates for quick layout creation
data class LayoutTemplate(
    val name: String,
    val description: String,
    val category: LayoutCategory,
    val preset: LayoutPreset,
    val icon: ImageVector,
    val previewButtons: List<String>, // Sample button names for preview
    val accentColor: String = "#FF4081"
)

val predefinedTemplates = listOf(
    LayoutTemplate(
        name = "Streamer Essentials",
        description = "Essential sounds for live streaming - applause, alerts, music stings",
        category = LayoutCategory.STREAMING,
        preset = LayoutPreset.STANDARD,
        icon = Icons.Default.Stream,
        previewButtons = listOf("Applause", "Airhorn", "Ding", "Drumroll", "Laugh", "Notification")
    ),
    LayoutTemplate(
        name = "Podcast Producer",
        description = "Professional podcast sounds - intro/outro music, transitions, effects",
        category = LayoutCategory.PODCASTING,
        preset = LayoutPreset.PODCAST,
        icon = Icons.Default.Mic,
        previewButtons = listOf("Intro Music", "Outro Music", "Transition", "Commercial Break")
    ),
    LayoutTemplate(
        name = "Music Producer",
        description = "Sample pads and beats for music production",
        category = LayoutCategory.MUSIC,
        preset = LayoutPreset.MUSIC,
        icon = Icons.Default.MusicNote,
        previewButtons = listOf("Kick", "Snare", "Hi-Hat", "Bass", "Synth", "Vocal")
    ),
    LayoutTemplate(
        name = "Gaming Soundboard",
        description = "Gaming alerts, victory sounds, and funny effects",
        category = LayoutCategory.GAMING,
        preset = LayoutPreset.WIDE,
        icon = Icons.Default.SportsEsports,
        previewButtons = listOf("Victory", "Defeat", "Level Up", "Achievement", "Fail", "Epic")
    ),
    LayoutTemplate(
        name = "StreamDeck Style",
        description = "Clean 5x3 layout similar to Elgato StreamDeck",
        category = LayoutCategory.GENERAL,
        preset = LayoutPreset.STREAMDECK,
        icon = Icons.Default.GridView,
        previewButtons = listOf("Button 1", "Button 2", "Button 3", "Button 4", "Button 5")
    ),
    LayoutTemplate(
        name = "Compact Mobile",
        description = "Perfect for phone screens with essential controls",
        category = LayoutCategory.GENERAL,
        preset = LayoutPreset.COMPACT,
        icon = Icons.Default.PhoneAndroid,
        previewButtons = listOf("Sound 1", "Sound 2", "Sound 3", "Sound 4")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (LayoutTemplate) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(LayoutCategory.STREAMING) }
    
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
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkBlueSurface
            ),
            shape = RoundedCornerShape(20.dp)
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
                    Column {
                        Text(
                            "Layout Templates",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Choose from professional templates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ButtonSurface)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Category Filter
                LazyRow(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LayoutCategory.values().filter { it != LayoutCategory.CUSTOM }) { category ->
                        TemplateCategoryChip(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
                
                // Templates List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val filteredTemplates = predefinedTemplates.filter { template ->
                        selectedCategory == LayoutCategory.GENERAL || template.category == selectedCategory
                    }
                    
                    items(filteredTemplates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onSelectTemplate(template) }
                        )
                    }
                    
                    if (filteredTemplates.isEmpty()) {
                        item {
                            EmptyTemplatesMessage(selectedCategory)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateCategoryChip(
    category: LayoutCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp)),
        color = if (isSelected) NeonPink.copy(alpha = 0.2f) else ButtonSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            category.displayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) NeonPink else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun TemplateCard(
    template: LayoutTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = ButtonSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = NeonPink.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                template.icon,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            template.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            template.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                Surface(
                    color = CoralAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "${template.preset.columns}×${template.preset.rows}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            Text(
                template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Preview Buttons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(template.previewButtons.take(6)) { buttonName ->
                    Surface(
                        color = DarkBlueVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            buttonName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                
                if (template.previewButtons.size > 6) {
                    item {
                        Surface(
                            color = TextDisabled.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "+${template.previewButtons.size - 6}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDisabled
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTemplatesMessage(category: LayoutCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            tint = TextDisabled,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "No ${category.displayName} Templates",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            "More templates coming soon!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDisabled,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
} 