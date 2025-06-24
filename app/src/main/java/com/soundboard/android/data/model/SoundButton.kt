package com.soundboard.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_buttons")
data class SoundButton(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "is_local_file")
    val isLocalFile: Boolean = false, // true for local files, false for server files
    
    @ColumnInfo(name = "position_x")
    val positionX: Int,
    
    @ColumnInfo(name = "position_y")
    val positionY: Int,
    
    @ColumnInfo(name = "color")
    val color: String = "#2196F3",
    
    @ColumnInfo(name = "icon_name")
    val iconName: String? = null,
    
    @ColumnInfo(name = "volume")
    val volume: Float = 1.0f,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) 