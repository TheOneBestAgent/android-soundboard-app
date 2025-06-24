package com.soundboard.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "soundboard_layouts")
data class SoundboardLayout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,
    
    @ColumnInfo(name = "grid_columns")
    val gridColumns: Int = 4,
    
    @ColumnInfo(name = "grid_rows")
    val gridRows: Int = 6,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Enhanced properties for Phase 3.3
    @ColumnInfo(name = "is_template")
    val isTemplate: Boolean = false,        // Whether this is a template layout
    @ColumnInfo(name = "template_category")
    val templateCategory: String? = null,    // Category: "streaming", "podcasting", "music", "general"
    @ColumnInfo(name = "background_color")
    val backgroundColor: String = "#1A1B3A", // Custom background color
    @ColumnInfo(name = "accent_color")
    val accentColor: String = "#FF4081",     // Custom accent color
    @ColumnInfo(name = "button_spacing")
    val buttonSpacing: Float = 8.0f,         // Spacing between buttons in dp
    @ColumnInfo(name = "corner_radius")
    val cornerRadius: Float = 12.0f,         // Button corner radius in dp
    @ColumnInfo(name = "enable_glow_effect")
    val enableGlowEffect: Boolean = true,    // Whether to show neon glow effects
    @ColumnInfo(name = "max_buttons")
    val maxButtons: Int = gridColumns * gridRows, // Maximum buttons for this layout
    
    // Layout presets for quick selection
    @ColumnInfo(name = "layout_preset")
    val layoutPreset: LayoutPreset = LayoutPreset.CUSTOM,
    
    // Import/Export metadata
    @ColumnInfo(name = "export_version")
    val exportVersion: Int = 1,              // Version for compatibility
    @ColumnInfo(name = "original_author")
    val originalAuthor: String? = null,      // Author for shared layouts
    @ColumnInfo(name = "download_url")
    val downloadUrl: String? = null,         // URL if downloaded from community
    @ColumnInfo(name = "tags")
    val tags: String? = null                 // Comma-separated tags for search
)

enum class LayoutPreset(
    val displayName: String,
    val columns: Int,
    val rows: Int,
    val description: String
) {
    // Phone-optimized layouts
    MINIMAL("Minimal", 2, 3, "6 buttons - Ultra compact"),
    COMPACT("Compact", 3, 4, "12 buttons - Perfect for phones"),
    PHONE_PORTRAIT("Phone Portrait", 3, 6, "18 buttons - Tall phone layout"),
    PHONE_LANDSCAPE("Phone Landscape", 6, 3, "18 buttons - Wide phone layout"),
    
    // Tablet-optimized layouts
    STANDARD("Standard", 4, 6, "24 buttons - Ideal for tablets"),
    WIDE("Wide", 6, 4, "24 buttons - Landscape orientation"),
    TABLET_LARGE("Tablet Large", 5, 6, "30 buttons - Large tablet"),
    TABLET_GRID("Tablet Grid", 6, 6, "36 buttons - Perfect square grid"),
    
    // Professional layouts
    STREAMDECK("StreamDeck", 5, 3, "15 buttons - Elgato StreamDeck style"),
    STREAMDECK_XL("StreamDeck XL", 8, 4, "32 buttons - StreamDeck XL style"),
    PODCAST("Podcast", 4, 4, "16 buttons - Podcast essentials"),
    BROADCASTING("Broadcasting", 6, 5, "30 buttons - Live broadcasting"),
    
    // Specialized layouts
    MUSIC("Music Producer", 8, 6, "48 buttons - Music production"),
    GAMING("Gaming", 5, 4, "20 buttons - Gaming sounds"),
    DJ_MIXER("DJ Mixer", 4, 8, "32 buttons - DJ style vertical"),
    SOUND_EFFECTS("Sound Effects", 7, 5, "35 buttons - Sound effects board"),
    
    // Large layouts
    LARGE("Large", 5, 8, "40 buttons - Maximum control"),
    EXTRA_LARGE("Extra Large", 6, 8, "48 buttons - Professional studio"),
    MEGA("Mega", 8, 8, "64 buttons - Ultimate control"),
    
    CUSTOM("Custom", 4, 6, "Custom configuration")
}

// Data class for layout import/export
data class LayoutExportData(
    val layout: SoundboardLayout,
    val buttons: List<SoundButton>,
    val exportDate: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0"
)

// Template categories for predefined layouts
enum class LayoutCategory(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    STREAMING("Streaming", "Layouts optimized for live streaming", "stream"),
    PODCASTING("Podcasting", "Perfect for podcast recording", "microphone"),
    MUSIC("Music Production", "Layouts for music creators", "music_note"),
    GAMING("Gaming", "Gaming sound effects and alerts", "sports_esports"),
    GENERAL("General Use", "Versatile layouts for any purpose", "apps"),
    CUSTOM("Custom", "User-created custom layouts", "build")
} 