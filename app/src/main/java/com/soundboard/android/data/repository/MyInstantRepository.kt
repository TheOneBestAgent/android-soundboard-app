package com.soundboard.android.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.soundboard.android.network.api.MyInstantApiService
import com.soundboard.android.network.api.MyInstantApiResponse
import com.soundboard.android.network.api.MyInstantResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyInstantRepository @Inject constructor(
    private val myInstantApiService: MyInstantApiService,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    
    private var currentMediaPlayer: MediaPlayer? = null
    
    suspend fun searchSounds(query: String): Result<List<MyInstantResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "🔍 Searching MyInstants API for: $query")
                
                val response = myInstantApiService.searchSounds(query)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val sounds = apiResponse?.data ?: emptyList()
                    Log.d("MyInstantRepository", "✅ Found ${sounds.size} sounds matching '$query'")
                    Result.success(sounds)
                } else {
                    Log.e("MyInstantRepository", "❌ API Error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Search error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun getTrendingSounds(region: String = "us"): Result<List<MyInstantResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "📈 Getting trending sounds for region: $region")
                
                val response = myInstantApiService.getTrendingSounds(region)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val sounds = apiResponse?.data ?: emptyList()
                    Log.d("MyInstantRepository", "✅ Found ${sounds.size} trending sounds")
                    Result.success(sounds)
                } else {
                    Log.e("MyInstantRepository", "❌ API Error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Trending error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun getRecentSounds(): Result<List<MyInstantResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "🆕 Getting recent sounds")
                
                val response = myInstantApiService.getRecentSounds()
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val sounds = apiResponse?.data ?: emptyList()
                    Log.d("MyInstantRepository", "✅ Found ${sounds.size} recent sounds")
                    Result.success(sounds)
                } else {
                    Log.e("MyInstantRepository", "❌ API Error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Recent sounds error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun getBestSounds(): Result<List<MyInstantResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "🏆 Getting best sounds")
                
                val response = myInstantApiService.getBestSounds()
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val sounds = apiResponse?.data ?: emptyList()
                    Log.d("MyInstantRepository", "✅ Found ${sounds.size} best sounds")
                    Result.success(sounds)
                } else {
                    Log.e("MyInstantRepository", "❌ API Error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Best sounds error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun getSoundDetails(soundId: String): Result<MyInstantResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "🔍 Getting sound details for ID: $soundId")
                
                val response = myInstantApiService.getSoundDetails(soundId)
                
                if (response.isSuccessful) {
                    val sound = response.body()
                    if (sound != null) {
                        Log.d("MyInstantRepository", "✅ Found sound details: ${sound.title}")
                        Result.success(sound)
                    } else {
                        Log.e("MyInstantRepository", "❌ Sound not found: $soundId")
                        Result.failure(Exception("Sound not found"))
                    }
                } else {
                    Log.e("MyInstantRepository", "❌ API Error: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
                }
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Sound details error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    suspend fun previewSound(mp3Url: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "🎵 Previewing sound: $mp3Url")
                
                // Stop any currently playing sound
                stopPreview()
                
                currentMediaPlayer = MediaPlayer().apply {
                    setDataSource(mp3Url)
                    setOnPreparedListener { player ->
                        player.start()
                        Log.d("MyInstantRepository", "▶️ Preview started")
                    }
                    setOnCompletionListener {
                        Log.d("MyInstantRepository", "⏹️ Preview completed")
                        stopPreview()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("MyInstantRepository", "❌ Preview error: what=$what, extra=$extra")
                        stopPreview()
                        false
                    }
                    prepareAsync()
                }
                
                Result.success(true)
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Preview error: ${e.message}")
                stopPreview()
                Result.failure(e)
            }
        }
    }
    
    fun stopPreview() {
        currentMediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
                Log.d("MyInstantRepository", "⏹️ Preview stopped")
            } catch (e: Exception) {
                Log.w("MyInstantRepository", "Warning stopping preview: ${e.message}")
            }
        }
        currentMediaPlayer = null
    }
    
    suspend fun downloadSound(mp3Url: String, fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MyInstantRepository", "⬇️ Downloading sound: $fileName from $mp3Url")
                
                val request = Request.Builder()
                    .url(mp3Url)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("Download failed: ${response.code}")
                }
                
                val responseBody = response.body
                if (responseBody == null) {
                    throw IOException("Empty response body")
                }
                
                // Clean filename and ensure it has .mp3 extension
                val cleanFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val finalFileName = if (cleanFileName.endsWith(".mp3")) {
                    cleanFileName
                } else {
                    "$cleanFileName.mp3"
                }
                
                val downloadLocation = settingsRepository.getDownloadLocation()
                
                val filePath = if (downloadLocation.startsWith("custom:")) {
                    // Handle custom folder selected via file picker
                    val customUri = downloadLocation.substring(7) // Remove "custom:" prefix
                    val treeUri = Uri.parse(customUri)
                    val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                    
                    if (documentFile != null && documentFile.exists()) {
                        val newFile = documentFile.createFile("audio/mpeg", finalFileName)
                        if (newFile != null) {
                            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                responseBody.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            Log.d("MyInstantRepository", "✅ Downloaded successfully to custom folder: ${newFile.uri}")
                            newFile.uri.toString()
                        } else {
                            throw IOException("Failed to create file in custom folder")
                        }
                    } else {
                        throw IOException("Custom folder not accessible")
                    }
                } else {
                    // Handle predefined folders
                    val downloadsDir = File(context.getExternalFilesDir(null), downloadLocation)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    
                    val outputFile = File(downloadsDir, finalFileName)
                    
                    // Write file
                    FileOutputStream(outputFile).use { output ->
                        responseBody.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    
                    Log.d("MyInstantRepository", "✅ Downloaded successfully: ${outputFile.absolutePath}")
                    outputFile.absolutePath
                }
                
                Result.success(filePath)
                
            } catch (e: Exception) {
                Log.e("MyInstantRepository", "❌ Download error: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    fun getCategories(): List<String> {
        return listOf(
            "Trending", "Recent", "Best", "Memes", "Sound Effects", 
            "Reactions", "Music", "Games", "Movies", "Television",
            "Anime & Manga", "Sports", "TikTok Trends"
        )
    }
    
    suspend fun loadByCategory(category: String): Result<List<MyInstantResponse>> {
        return when (category.lowercase()) {
            "trending" -> getTrendingSounds()
            "recent" -> getRecentSounds()
            "best" -> getBestSounds()
            else -> searchSounds(category)
        }
    }
} 