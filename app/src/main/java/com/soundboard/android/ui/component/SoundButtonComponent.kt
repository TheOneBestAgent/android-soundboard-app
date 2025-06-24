package com.soundboard.android.ui.component

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundboard.android.data.model.SoundButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SoundButtonComponent(
    soundButton: SoundButton?,
    onClick: (SoundButton?) -> Unit,
    onLongClick: (SoundButton?) -> Unit = {},
    onEdit: (SoundButton) -> Unit = {},
    onDelete: (SoundButton) -> Unit = {},
    onVolumeChange: (SoundButton, Float) -> Unit = { _, _ -> },
    onQuickVolumeAdjust: (SoundButton, Float) -> Unit = { _, _ -> },
    isEnabled: Boolean = true,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )
    
    val playingScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playingScale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val elevation by animateFloatAsState(
        targetValue = when {
            isPlaying -> 12.dp.value
            isPressed -> 2.dp.value
            else -> 6.dp.value
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "elevation"
    )
    
    val buttonColor = if (soundButton != null) {
        try {
            Color(android.graphics.Color.parseColor(soundButton.color))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val animatedButtonColor by animateColorAsState(
        targetValue = when {
            isPlaying -> buttonColor.copy(alpha = 1f)
            isPressed -> buttonColor.copy(alpha = 0.8f)
            else -> buttonColor.copy(alpha = 0.9f)
        },
        animationSpec = tween(300),
        label = "buttonColor"
    )
    
    Card(
        modifier = modifier
            .scale(pressScale * playingScale)
            .graphicsLayer {
                if (isPlaying) {
                    shadowElevation = elevation.dp.toPx()
                    alpha = 1f
                }
            }
            .combinedClickable(
                enabled = isEnabled,
                onClick = { 
                    onClick(soundButton)
                    isPressed = true
                    haptic.performHapticFeedback(
                        if (soundButton != null) HapticFeedbackType.TextHandleMove 
                        else HapticFeedbackType.LongPress
                    )
                },
                onLongClick = {
                    if (soundButton != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showContextMenu = true
                        onLongClick(soundButton)
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = animatedButtonColor
        ),
        border = if (isPlaying) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
            )
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                )
            }
            
            if (soundButton != null) {
                val isCustom = isCustomIcon(soundButton.iconName)
                
                if (isCustom) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        getCustomIconUri(soundButton.iconName)?.let { uri ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uri.toUri())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = soundButton.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .graphicsLayer {
                                        scaleX = if (isPlaying) 1.02f else 1f
                                        scaleY = if (isPlaying) 1.02f else 1f
                                    }
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(
                                        alpha = if (isPlaying) 0.2f else 0.4f
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isPlaying) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Playing",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .graphicsLayer {
                                                alpha = glowAlpha
                                                rotationZ = if (isPlaying) 
                                                    (System.currentTimeMillis() / 50) % 360f 
                                                else 0f
                                            }
                                    )
                                }
                                
                                Text(
                                    text = "${(soundButton.volume * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            Color.Black.copy(alpha = 0.6f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = soundButton.name,
                                    color = if (isPlaying) 
                                        MaterialTheme.colorScheme.primary 
                                    else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color.Black.copy(alpha = 0.6f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .graphicsLayer {
                                            if (isPlaying) {
                                                shadowElevation = 4.dp.toPx()
                                            }
                                        }
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isPlaying) 4.dp else 3.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.3f),
                                            RoundedCornerShape(2.dp)
                                        )
                                ) {
                                    val volumeColor = when {
                                        soundButton.volume >= 0.8f -> Color.Red.copy(alpha = 0.9f)
                                        soundButton.volume >= 0.6f -> Color.Yellow.copy(alpha = 0.9f)
                                        soundButton.volume >= 0.3f -> Color.Green.copy(alpha = 0.9f)
                                        else -> Color.White.copy(alpha = 0.7f)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(soundButton.volume)
                                            .fillMaxHeight()
                                            .background(
                                                if (isPlaying) 
                                                    volumeColor.copy(alpha = glowAlpha)
                                                else volumeColor,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Regular icon layout with enhanced animations
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // Top row with playing indicator and volume percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Playing indicator
                            if (isPlaying) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Playing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .graphicsLayer {
                                            alpha = glowAlpha
                                            scaleX = 1.2f
                                            scaleY = 1.2f
                                        }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(12.dp))
                            }
                            
                            Text(
                                text = "${(soundButton.volume * 100).toInt()}%",
                                color = if (isPlaying) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                                else Color.White.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Enhanced icon with playing animation
                        Icon(
                            imageVector = getIconByName(soundButton.iconName),
                            contentDescription = "Sound Button",
                            tint = if (isPlaying) 
                                MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                            else Color.White,
                            modifier = Modifier
                                .size(if (isPlaying) 36.dp else 32.dp)
                                .graphicsLayer {
                                    if (isPlaying) {
                                        shadowElevation = 8.dp.toPx()
                                        rotationZ = (System.currentTimeMillis() / 100) % 360f * 0.1f // Subtle rotation
                                    }
                                }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Enhanced text with playing state
                        Text(
                            text = soundButton.name,
                            color = if (isPlaying) 
                                MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                            else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    if (isPlaying) {
                                        shadowElevation = 4.dp.toPx()
                                    }
                                }
                        )
                        
                        // Enhanced volume indicator with playing animation
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isPlaying) 4.dp else 3.dp)
                                .background(
                                    Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(2.dp)
                                )
                        ) {
                            val volumeColor = when {
                                soundButton.volume >= 0.8f -> Color.Red.copy(alpha = 0.9f)
                                soundButton.volume >= 0.6f -> Color.Yellow.copy(alpha = 0.9f)
                                soundButton.volume >= 0.3f -> Color.Green.copy(alpha = 0.9f)
                                else -> Color.White.copy(alpha = 0.7f)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(soundButton.volume)
                                    .fillMaxHeight()
                                    .background(
                                        if (isPlaying) 
                                            volumeColor.copy(alpha = glowAlpha)
                                        else volumeColor,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .graphicsLayer {
                                        if (isPlaying) {
                                            shadowElevation = 2.dp.toPx()
                                        }
                                    }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            val newVolume = (soundButton.volume - 0.1f).coerceAtLeast(0f)
                            onQuickVolumeAdjust(soundButton, newVolume)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color.Black.copy(alpha = 0.3f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = "Volume Down",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { 
                            val newVolume = (soundButton.volume + 0.1f).coerceAtMost(1f)
                            onQuickVolumeAdjust(soundButton, newVolume)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color.Black.copy(alpha = 0.3f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Sound Button",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Sound",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            if (!isEnabled && soundButton != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
    
    if (showContextMenu && soundButton != null) {
        EnhancedSoundButtonContextMenu(
            soundButton = soundButton,
            onDismiss = { showContextMenu = false },
            onEdit = { 
                onEdit(soundButton)
                showContextMenu = false
            },
            onDelete = { 
                onDelete(soundButton)
                showContextMenu = false
            },
            onVolumeChange = { volume ->
                onVolumeChange(soundButton, volume)
            },
            onShowVolumeSlider = { showVolumeSlider = true }
        )
    }
    
    if (showVolumeSlider && soundButton != null) {
        VolumeControlDialog(
            soundButton = soundButton,
            onDismiss = { showVolumeSlider = false },
            onVolumeChange = { volume ->
                onVolumeChange(soundButton, volume)
            }
        )
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedSoundButtonContextMenu(
    soundButton: SoundButton,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onShowVolumeSlider: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Edit Button") },
            onClick = onEdit,
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        )
        
        DropdownMenuItem(
            text = { Text("Volume Control") },
            onClick = { 
                onShowVolumeSlider()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VolumeUp, contentDescription = "Volume")
            },
            trailingIcon = {
                Text(
                    text = "${(soundButton.volume * 100).toInt()}%",
                    color = when {
                        soundButton.volume >= 0.8f -> MaterialTheme.colorScheme.error
                        soundButton.volume >= 0.6f -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        )
        
        DropdownMenuItem(
            text = { Text("Volume: 25%") },
            onClick = { 
                onVolumeChange(0.25f)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VolumeDown, contentDescription = "Low Volume")
            }
        )
        
        DropdownMenuItem(
            text = { Text("Volume: 50%") },
            onClick = { 
                onVolumeChange(0.5f)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VolumeMute, contentDescription = "Medium Volume")
            }
        )
        
        DropdownMenuItem(
            text = { Text("Volume: 75%") },
            onClick = { 
                onVolumeChange(0.75f)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VolumeUp, contentDescription = "High Volume")
            }
        )
        
        DropdownMenuItem(
            text = { Text("Volume: 100%") },
            onClick = { 
                onVolumeChange(1.0f)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VolumeUp, contentDescription = "Max Volume")
            }
        )
        
        Divider()
        
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = { showDeleteConfirmation = true },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
    
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Sound Button") },
            text = { Text("Are you sure you want to delete '${soundButton.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeControlDialog(
    soundButton: SoundButton,
    onDismiss: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    var currentVolume by remember { mutableStateOf(soundButton.volume) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Volume Control - ${soundButton.name}")
        },
        text = {
            Column {
                Text(
                    text = "Adjust volume for Voicemeeter integration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Volume: ${(currentVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        currentVolume >= 0.8f -> MaterialTheme.colorScheme.error
                        currentVolume >= 0.6f -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = currentVolume,
                    onValueChange = { 
                        currentVolume = it
                        onVolumeChange(it)
                    },
                    valueRange = 0f..1f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("25%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("50%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("75%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("100%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { preset ->
                        FilterChip(
                            onClick = { 
                                currentVolume = preset
                                onVolumeChange(preset)
                            },
                            label = { Text("${(preset * 100).toInt()}%") },
                            selected = currentVolume == preset,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
} 