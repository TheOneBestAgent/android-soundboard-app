package com.soundboard.android.ui.screen

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundboard.android.data.model.*
import com.soundboard.android.ui.theme.*
import com.soundboard.android.ui.viewmodel.SoundboardViewModel
import com.soundboard.android.ui.component.CreateLayoutDialog
import com.soundboard.android.ui.component.TemplateDialog
import com.soundboard.android.ui.component.EditLayoutDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutManagerScreen(
    onNavigateBack: () -> Unit,
    onLayoutSelected: (SoundboardLayout) -> Unit,
    viewModel: SoundboardViewModel = hiltViewModel()
) {
    val layouts by viewModel.layouts.collectAsState()
    val currentLayout by viewModel.currentLayout.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedLayoutForEdit by remember { mutableStateOf<SoundboardLayout?>(null) }
    var selectedCategory by remember { mutableStateOf(LayoutCategory.GENERAL) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlueBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Header with icon-inspired styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ButtonSurface)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonPink
                )
            }
            
            Text(
                "Layout Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                // Template button
                IconButton(
                    onClick = { showTemplateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoralAccent.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = "Templates",
                        tint = CoralAccent
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Create new layout button
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPink.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Layout",
                        tint = NeonPink
                    )
                }
            }
        }
        
        // Current Layout Card
        currentLayout?.let { layout ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = DarkBlueSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Current Layout",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonPinkLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            color = NeonPink.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        layout.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "${layout.gridColumns}×${layout.gridRows} grid • ${layout.maxButtons} buttons",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    
                    layout.description?.let { desc ->
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Layout Categories
        LazyRow(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LayoutCategory.values()) { category ->
                CategoryChip(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { selectedCategory = category }
                )
            }
        }
        
        // Layouts List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val filteredLayouts = layouts.filter { layout ->
                when (selectedCategory) {
                    LayoutCategory.CUSTOM -> !layout.isTemplate
                    else -> layout.templateCategory == selectedCategory.name.lowercase() || 
                            (!layout.isTemplate && selectedCategory == LayoutCategory.GENERAL)
                }
            }
            
            items(filteredLayouts) { layout ->
                LayoutCard(
                    layout = layout,
                    isActive = layout.id == currentLayout?.id,
                    onSelect = { onLayoutSelected(layout) },
                    onEdit = { 
                        selectedLayoutForEdit = layout
                        showEditDialog = true 
                    },
                    onDelete = { viewModel.deleteLayout(layout) },
                    onDuplicate = { viewModel.duplicateLayout(layout) }
                )
            }
            
            if (filteredLayouts.isEmpty()) {
                item {
                    EmptyStateCard(
                        category = selectedCategory,
                        onCreateLayout = { showCreateDialog = true },
                        onBrowseTemplates = { showTemplateDialog = true }
                    )
                }
            }
        }
    }
    
    // Create Layout Dialog
    if (showCreateDialog) {
        CreateLayoutDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, preset ->
                viewModel.createLayout(name, preset)
                showCreateDialog = false
            }
        )
    }
    
    // Template Dialog
    if (showTemplateDialog) {
        TemplateDialog(
            onDismiss = { showTemplateDialog = false },
            onSelectTemplate = { template ->
                viewModel.createFromTemplate(template)
                showTemplateDialog = false
            }
        )
    }
    
    // Edit Layout Dialog
    if (showEditDialog && selectedLayoutForEdit != null) {
        EditLayoutDialog(
            layout = selectedLayoutForEdit!!,
            onDismiss = { 
                showEditDialog = false
                selectedLayoutForEdit = null
            },
            onSave = { updatedLayout ->
                viewModel.updateLayout(updatedLayout)
                showEditDialog = false
                selectedLayoutForEdit = null
            }
        )
    }
}

@Composable
fun CategoryChip(
    category: LayoutCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(20.dp)),
        color = if (isSelected) NeonPink.copy(alpha = 0.2f) else ButtonSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) NeonPink else TextSecondary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun LayoutCard(
    layout: SoundboardLayout,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .shadow(
                elevation = if (isActive) 12.dp else 4.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) DarkBlueVariant else DarkBlueSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        layout.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        "${layout.gridColumns}×${layout.gridRows} • ${layout.layoutPreset.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) NeonPinkLight else TextSecondary
                    )
                }
                
                Row {
                    if (isActive) {
                        Surface(
                            color = NeonPink.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (!isActive) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = CoralAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            layout.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    category: LayoutCategory,
    onCreateLayout: () -> Unit,
    onBrowseTemplates: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBlueSurface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Apps,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "No ${category.displayName} Layouts",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                "Create your first layout or browse templates",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDisabled,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CoralAccent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Templates")
                }
                
                Button(
                    onClick = onCreateLayout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPink
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Layout")
                }
            }
        }
    }
} 