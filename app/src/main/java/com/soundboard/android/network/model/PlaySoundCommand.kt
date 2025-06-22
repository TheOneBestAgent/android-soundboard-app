package com.soundboard.android.network.model

import com.google.gson.annotations.SerializedName

data class PlaySoundCommand(
    @SerializedName("file_path")
    val filePath: String,
    
    @SerializedName("volume")
    val volume: Float = 1.0f,
    
    @SerializedName("button_id")
    val buttonId: Int,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class ServerResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class ConnectionInfo(
    @SerializedName("computer_name")
    val computerName: String,
    
    @SerializedName("server_version")
    val serverVersion: String,
    
    @SerializedName("supported_formats")
    val supportedFormats: List<String>,
    
    @SerializedName("max_connections")
    val maxConnections: Int = 1
) 