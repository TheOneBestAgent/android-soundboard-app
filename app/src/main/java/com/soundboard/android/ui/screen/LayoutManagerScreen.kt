package com.soundboard.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundboard.android.data.model.*
import com.soundboard.android.ui.component.*
import com.soundboard.android.ui.theme.*
import com.soundboard.android.ui.viewmodel.SoundboardViewModel
import com.soundboard.android.ui.viewmodel.logInteraction
import com.soundboard.android.diagnostics.ComponentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutManagerScreen(
    onNavigateBack: () -> Unit,
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
    ) {
        // Header with icon-inspired styling - Fixed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ButtonSurface)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Layout Manager",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create and manage your soundboard layouts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.logInteraction("Clicked on Templates", ComponentType.UI_LAYOUT)
                        showTemplateDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonPink.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = "Templates",
                        tint = NeonPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        viewModel.logInteraction("Clicked on Create Layout", ComponentType.UI_LAYOUT)
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Layout",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Scrollable content area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Current Layout Info
            item {
                currentLayout?.let { layout ->
                    CurrentLayoutCard(
                        layout = layout,
                        onEditLayout = {
                            selectedLayoutForEdit = layout
                            showEditDialog = true
                        }
                    )
                }
            }
            
            // Category Filter
            item {
                Text(
                    text = "Browse Layouts",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(LayoutCategory.values()) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
            
            // Layouts Grid
            val filteredLayouts = layouts.filter { layout ->
                selectedCategory == LayoutCategory.GENERAL || layout.templateCategory == selectedCategory.name.lowercase()
            }
            
            if (filteredLayouts.isNotEmpty()) {
                items(filteredLayouts) { layout ->
                    LayoutCard(
                        layout = layout,
                        isActive = layout.id == currentLayout?.id,
                        onSelectLayout = { /* TODO: viewModel.setCurrentLayout(layout) */ },
                        onEditLayout = {
                            selectedLayoutForEdit = layout
                            showEditDialog = true
                        },
                        onDeleteLayout = { viewModel.deleteLayout(layout) }
                    )
                }
            } else {
                item {
                    EmptyLayoutsCard(
                        category = selectedCategory,
                        onCreateLayout = { showCreateDialog = true },
                        onBrowseTemplates = { showTemplateDialog = true }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showCreateDialog) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Create Layout Dialog", ComponentType.UI_DIALOG)
        }
        CreateLayoutDialog(
            onDismiss = {
                viewModel.logInteraction("Create Layout Dialog dismissed", ComponentType.UI_DIALOG)
                showCreateDialog = false
            },
            onCreate = { name, preset ->
                viewModel.createLayout(name, preset)
                showCreateDialog = false
            }
        )
    }
    
    if (showTemplateDialog) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Template Dialog", ComponentType.UI_DIALOG)
        }
        TemplateDialog(
            onDismiss = {
                viewModel.logInteraction("Template Dialog dismissed", ComponentType.UI_DIALOG)
                showTemplateDialog = false
            },
            onSelectTemplate = { template ->
                viewModel.createLayoutFromTemplate(template.name, template)
                showTemplateDialog = false
            }
        )
    }
    
    if (showEditDialog && selectedLayoutForEdit != null) {
        LaunchedEffect(Unit) {
            viewModel.logInteraction("Showing Edit Layout Dialog", ComponentType.UI_DIALOG)
        }
        EditLayoutDialog(
            layout = selectedLayoutForEdit!!,
            onDismiss = {
                viewModel.logInteraction("Edit Layout Dialog dismissed", ComponentType.UI_DIALOG)
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
    onSelectLayout: () -> Unit,
    onEditLayout: () -> Unit,
    onDeleteLayout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectLayout() }
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
                        onClick = onEditLayout,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (!isActive) {
                        IconButton(
                            onClick = onDeleteLayout,
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
fun EmptyLayoutsCard(
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

@Composable
private fun CurrentLayoutCard(
    layout: SoundboardLayout,
    onEditLayout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                "${layout.gridColumns}×${layout.gridRows} grid • ${layout.gridColumns * layout.gridRows} buttons max",
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEditLayout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Layout")
                }
            }
        }
    }
} 