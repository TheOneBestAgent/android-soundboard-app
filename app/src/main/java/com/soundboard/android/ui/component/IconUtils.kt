package com.soundboard.android.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Helper function to get icon by name
fun getIconByName(iconName: String?): ImageVector {
    return when (iconName) {
        "music_note" -> Icons.Default.MusicNote
        "volume_up" -> Icons.Default.VolumeUp
        "volume_down" -> Icons.Default.VolumeDown
        "volume_off" -> Icons.Default.VolumeOff
        "mic" -> Icons.Default.Mic
        "mic_off" -> Icons.Default.MicOff
        "headphones" -> Icons.Default.Headphones
        "speaker" -> Icons.Default.Speaker
        "queue_music" -> Icons.Default.QueueMusic
        "library_music" -> Icons.Default.LibraryMusic
        "sports_esports" -> Icons.Default.SportsEsports
        "gamepad" -> Icons.Default.Gamepad
        "casino" -> Icons.Default.Casino
        "rocket_launch" -> Icons.Default.RocketLaunch
        "military_tech" -> Icons.Default.MilitaryTech
        "emoji_events" -> Icons.Default.EmojiEvents
        "chat" -> Icons.Default.Chat
        "chat_bubble" -> Icons.Default.ChatBubble
        "forum" -> Icons.Default.Forum
        "people" -> Icons.Default.People
        "person" -> Icons.Default.Person
        "group" -> Icons.Default.Group
        "thumb_up" -> Icons.Default.ThumbUp
        "favorite" -> Icons.Default.Favorite
        "flash_on" -> Icons.Default.FlashOn
        "auto_awesome" -> Icons.Default.AutoAwesome
        "celebration" -> Icons.Default.Celebration
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "bolt" -> Icons.Default.Bolt
        "star" -> Icons.Default.Star
        "diamond" -> Icons.Default.Diamond
        "flare" -> Icons.Default.Flare
        "play_arrow" -> Icons.Default.PlayArrow
        "pause" -> Icons.Default.Pause
        "stop" -> Icons.Default.Stop
        "skip_next" -> Icons.Default.SkipNext
        "skip_previous" -> Icons.Default.SkipPrevious
        "replay" -> Icons.Default.Replay
        "shuffle" -> Icons.Default.Shuffle
        "repeat" -> Icons.Default.Repeat
        "videocam" -> Icons.Default.Videocam
        "videocam_off" -> Icons.Default.VideocamOff
        "record_voice_over" -> Icons.Default.RecordVoiceOver
        "live_tv" -> Icons.Default.LiveTv
        "broadcast_on_home" -> Icons.Default.BroadcastOnHome
        "radio" -> Icons.Default.Radio
        "home" -> Icons.Default.Home
        "settings" -> Icons.Default.Settings
        "info" -> Icons.Default.Info
        "help" -> Icons.Default.Help
        "notifications" -> Icons.Default.Notifications
        "alarm" -> Icons.Default.Alarm
        "timer" -> Icons.Default.Timer
        "schedule" -> Icons.Default.Schedule
        "event" -> Icons.Default.Event
        "today" -> Icons.Default.Today
        "folder" -> Icons.Default.Folder
        "file_copy" -> Icons.Default.FileCopy
        "download" -> Icons.Default.Download
        "upload" -> Icons.Default.Upload
        "cloud" -> Icons.Default.Cloud
        "wifi" -> Icons.Default.Wifi
        "bluetooth" -> Icons.Default.Bluetooth
        "phone" -> Icons.Default.Phone
        "email" -> Icons.Default.Email
        "message" -> Icons.Default.Message
        else -> Icons.Default.MusicNote // Default fallback
    }
}

// Helper function to check if an icon is a custom image URI
fun isCustomIcon(iconName: String?): Boolean {
    return iconName?.startsWith("custom:") == true
}

// Helper function to extract URI from custom icon string
fun getCustomIconUri(iconName: String?): String? {
    return if (isCustomIcon(iconName)) {
        iconName?.removePrefix("custom:")
    } else {
        null
    }
} 