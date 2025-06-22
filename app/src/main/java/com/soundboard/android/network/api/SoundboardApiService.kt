package com.soundboard.android.network.api

import com.soundboard.android.network.model.ConnectionInfo
import com.soundboard.android.network.model.PlaySoundCommand
import com.soundboard.android.network.model.ServerResponse
import retrofit2.Response
import retrofit2.http.*

data class AudioFile(
    val name: String,
    val path: String,
    val size: Long,
    val format: String,
    val modified: String? = null,
    val uri: String? = null // For local files, stores content URI
)

data class ServerHealth(
    val status: String,
    val server_version: String,
    val connected_clients: Int,
    val supported_formats: List<String>,
    val timestamp: String
)

interface SoundboardApiService {
    
    @GET("health")
    suspend fun getHealth(): Response<ServerHealth>
    
    @GET("info")
    suspend fun getServerInfo(): Response<ConnectionInfo>
    
    @GET("audio-files")
    suspend fun getAudioFiles(): Response<List<AudioFile>>
    
    @POST("play")
    suspend fun playSound(@Body command: PlaySoundCommand): Response<ServerResponse>
} 