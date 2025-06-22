package com.soundboard.android.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest

data class IconOption(
    val name: String,
    val icon: ImageVector,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    currentIcon: String?,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val context = LocalContext.current
    
    // File picker launcher for custom images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Convert URI to string and pass it as custom icon
            onIconSelected("custom:$selectedUri")
            onDismiss()
        }
    }
    
    val iconCategories = listOf(
        "All", "Custom", "Audio", "Gaming", "Social", "Effects", "Media", "Streaming", "General"
    )
    
    val availableIcons = remember {
        listOf(
            // Audio Icons
            IconOption("music_note", Icons.Default.MusicNote, "Audio"),
            IconOption("volume_up", Icons.Default.VolumeUp, "Audio"),
            IconOption("volume_down", Icons.Default.VolumeDown, "Audio"),
            IconOption("volume_off", Icons.Default.VolumeOff, "Audio"),
            IconOption("mic", Icons.Default.Mic, "Audio"),
            IconOption("mic_off", Icons.Default.MicOff, "Audio"),
            IconOption("headphones", Icons.Default.Headphones, "Audio"),
            IconOption("speaker", Icons.Default.Speaker, "Audio"),
            IconOption("queue_music", Icons.Default.QueueMusic, "Audio"),
            IconOption("library_music", Icons.Default.LibraryMusic, "Audio"),
            
            // Gaming Icons
            IconOption("sports_esports", Icons.Default.SportsEsports, "Gaming"),
            IconOption("gamepad", Icons.Default.Gamepad, "Gaming"),
            IconOption("casino", Icons.Default.Casino, "Gaming"),
            IconOption("rocket_launch", Icons.Default.RocketLaunch, "Gaming"),
            IconOption("military_tech", Icons.Default.MilitaryTech, "Gaming"),
            IconOption("emoji_events", Icons.Default.EmojiEvents, "Gaming"),
            
            // Social Icons
            IconOption("chat", Icons.Default.Chat, "Social"),
            IconOption("chat_bubble", Icons.Default.ChatBubble, "Social"),
            IconOption("forum", Icons.Default.Forum, "Social"),
            IconOption("people", Icons.Default.People, "Social"),
            IconOption("person", Icons.Default.Person, "Social"),
            IconOption("group", Icons.Default.Group, "Social"),
            IconOption("thumb_up", Icons.Default.ThumbUp, "Social"),
            IconOption("favorite", Icons.Default.Favorite, "Social"),
            
            // Effects Icons
            IconOption("flash_on", Icons.Default.FlashOn, "Effects"),
            IconOption("auto_awesome", Icons.Default.AutoAwesome, "Effects"),
            IconOption("celebration", Icons.Default.Celebration, "Effects"),
            IconOption("local_fire_department", Icons.Default.LocalFireDepartment, "Effects"),
            IconOption("bolt", Icons.Default.Bolt, "Effects"),
            IconOption("star", Icons.Default.Star, "Effects"),
            IconOption("diamond", Icons.Default.Diamond, "Effects"),
            IconOption("flare", Icons.Default.Flare, "Effects"),
            
            // Media Icons
            IconOption("play_arrow", Icons.Default.PlayArrow, "Media"),
            IconOption("pause", Icons.Default.Pause, "Media"),
            IconOption("stop", Icons.Default.Stop, "Media"),
            IconOption("skip_next", Icons.Default.SkipNext, "Media"),
            IconOption("skip_previous", Icons.Default.SkipPrevious, "Media"),
            IconOption("replay", Icons.Default.Replay, "Media"),
            IconOption("shuffle", Icons.Default.Shuffle, "Media"),
            IconOption("repeat", Icons.Default.Repeat, "Media"),
            
            // Streaming Icons
            IconOption("videocam", Icons.Default.Videocam, "Streaming"),
            IconOption("videocam_off", Icons.Default.VideocamOff, "Streaming"),
            IconOption("record_voice_over", Icons.Default.RecordVoiceOver, "Streaming"),
            IconOption("live_tv", Icons.Default.LiveTv, "Streaming"),
            IconOption("broadcast_on_home", Icons.Default.BroadcastOnHome, "Streaming"),
            IconOption("radio", Icons.Default.Radio, "Streaming"),
            
            // General Icons
            IconOption("home", Icons.Default.Home, "General"),
            IconOption("settings", Icons.Default.Settings, "General"),
            IconOption("info", Icons.Default.Info, "General"),
            IconOption("help", Icons.Default.Help, "General"),
            IconOption("notifications", Icons.Default.Notifications, "General"),
            IconOption("alarm", Icons.Default.Alarm, "General"),
            IconOption("timer", Icons.Default.Timer, "General"),
            IconOption("schedule", Icons.Default.Schedule, "General"),
            IconOption("event", Icons.Default.Event, "General"),
            IconOption("today", Icons.Default.Today, "General"),
            IconOption("folder", Icons.Default.Folder, "General"),
            IconOption("file_copy", Icons.Default.FileCopy, "General"),
            IconOption("download", Icons.Default.Download, "General"),
            IconOption("upload", Icons.Default.Upload, "General"),
            IconOption("cloud", Icons.Default.Cloud, "General"),
            IconOption("wifi", Icons.Default.Wifi, "General"),
            IconOption("bluetooth", Icons.Default.Bluetooth, "General"),
            IconOption("phone", Icons.Default.Phone, "General"),
            IconOption("email", Icons.Default.Email, "General"),
            IconOption("message", Icons.Default.Message, "General")
        )
    }
    
    val filteredIcons = if (selectedCategory == "All") {
        availableIcons
    } else if (selectedCategory == "Custom") {
        emptyList() // Custom category shows file picker button instead
    } else {
        availableIcons.filter { it.category == selectedCategory }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Icon",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Category tabs
                ScrollableTabRow(
                    selectedTabIndex = iconCategories.indexOf(selectedCategory),
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp
                ) {
                    iconCategories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = { Text(category) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content based on selected category
                if (selectedCategory == "Custom") {
                    // Custom file picker section
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Custom Image",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Choose Custom Icon",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select an image from your device to use as a custom icon",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Pick Image",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Browse Images")
                            }
                        }
                    }
                } else {
                    // Icon grid for predefined icons
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredIcons) { iconOption ->
                            IconItem(
                                iconOption = iconOption,
                                isSelected = currentIcon == iconOption.name,
                                onClick = { 
                                    onIconSelected(iconOption.name)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconItem(
    iconOption: IconOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconOption.icon,
            contentDescription = iconOption.name,
            modifier = Modifier.size(32.dp),
            tint = if (isSelected) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

 