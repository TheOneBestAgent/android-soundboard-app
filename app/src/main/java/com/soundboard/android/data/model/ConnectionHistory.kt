package com.soundboard.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_history")
data class ConnectionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "computer_name")
    val computerName: String,
    
    @ColumnInfo(name = "ip_address")
    val ipAddress: String,
    
    @ColumnInfo(name = "port")
    val port: Int,
    
    @ColumnInfo(name = "last_connected")
    val lastConnected: Long,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
) 