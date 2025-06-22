package com.soundboard.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class ThemeOption(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isDark: Boolean? = null // null for system default
)

data class ColorScheme(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsDialog(
    onDismiss: () -> Unit,
    settingsRepository: com.soundboard.android.data.repository.SettingsRepository
) {
    val selectedTheme by settingsRepository.themeMode.collectAsState()
    val selectedColorScheme by settingsRepository.colorScheme.collectAsState()
    val buttonCornerRadius by settingsRepository.buttonCornerRadius.collectAsState()
    val buttonSpacing by settingsRepository.buttonSpacing.collectAsState()
    val showButtonLabels by settingsRepository.showButtonLabels.collectAsState()
    val useCompactLayout by settingsRepository.compactLayout.collectAsState()
    val animationsEnabled by settingsRepository.animationsEnabled.collectAsState()
    
    val themeOptions = listOf(
        ThemeOption("System Default", "Follow system theme", Icons.Default.Brightness6, null),
        ThemeOption("Light", "Always use light theme", Icons.Default.LightMode, false),
        ThemeOption("Dark", "Always use dark theme", Icons.Default.DarkMode, true)
    )
    
    val colorSchemes = listOf(
        ColorScheme("Blue", Color(0xFF1976D2), Color(0xFF42A5F5)),
        ColorScheme("Purple", Color(0xFF7B1FA2), Color(0xFFBA68C8)),
        ColorScheme("Green", Color(0xFF388E3C), Color(0xFF81C784)),
        ColorScheme("Orange", Color(0xFFF57C00), Color(0xFFFFB74D)),
        ColorScheme("Red", Color(0xFFD32F2F), Color(0xFFEF5350))
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Appearance Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Theme Selection
                item {
                    AppearanceSettingsSection(title = "Theme") {
                        themeOptions.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedTheme == theme.name,
                                        onClick = { settingsRepository.setThemeMode(theme.name) }
                                    )
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTheme == theme.name,
                                    onClick = { settingsRepository.setThemeMode(theme.name) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = theme.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = theme.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = theme.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Color Scheme Selection
                item {
                    AppearanceSettingsSection(title = "Color Scheme") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorSchemes.forEach { scheme ->
                                ColorSchemeCard(
                                    colorScheme = scheme,
                                    isSelected = selectedColorScheme == scheme.name,
                                    onClick = { settingsRepository.setColorScheme(scheme.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Button Customization
                item {
                    AppearanceSettingsSection(title = "Button Appearance") {
                        // Corner Radius
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Corner Radius: ${buttonCornerRadius.toInt()}dp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = buttonCornerRadius,
                                onValueChange = { settingsRepository.setButtonCornerRadius(it) },
                                valueRange = 0f..24f,
                                steps = 23
                            )
                        }
                        
                        // Button Spacing
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Button Spacing: ${buttonSpacing.toInt()}dp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = buttonSpacing,
                                onValueChange = { settingsRepository.setButtonSpacing(it) },
                                valueRange = 2f..16f,
                                steps = 13
                            )
                        }
                        
                        // Show Button Labels
                                                    Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsRepository.setShowButtonLabels(!showButtonLabels) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = showButtonLabels,
                                onCheckedChange = { settingsRepository.setShowButtonLabels(it) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Show Button Labels",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Display text labels on sound buttons",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Layout Options
                item {
                    AppearanceSettingsSection(title = "Layout") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsRepository.setCompactLayout(!useCompactLayout) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = useCompactLayout,
                                onCheckedChange = { settingsRepository.setCompactLayout(it) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Compact Layout",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Fit more buttons on screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsRepository.setAnimationsEnabled(!animationsEnabled) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = animationsEnabled,
                                onCheckedChange = { settingsRepository.setAnimationsEnabled(it) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Animations",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Enable button press animations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Save Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Settings are automatically saved when changed
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Settings")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun ColorSchemeCard(
    colorScheme: ColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.primaryColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.secondaryColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = colorScheme.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
} 