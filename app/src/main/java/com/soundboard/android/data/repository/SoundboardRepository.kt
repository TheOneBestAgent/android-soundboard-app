package com.soundboard.android.data.repository

import android.util.Log
import com.soundboard.android.data.dao.ConnectionHistoryDao
import com.soundboard.android.data.dao.SoundButtonDao
import com.soundboard.android.data.dao.SoundboardLayoutDao
import com.soundboard.android.data.model.ConnectionHistory
import com.soundboard.android.data.model.SoundButton
import com.soundboard.android.data.model.SoundboardLayout
import com.soundboard.android.network.SocketManager
import com.soundboard.android.network.api.AudioFile
import com.soundboard.android.network.api.SoundboardApiService
import com.soundboard.android.di.RetrofitFactory
import com.google.gson.Gson
import com.soundboard.android.network.model.PlaySoundCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

@Singleton
class SoundboardRepository @Inject constructor(
    private val soundButtonDao: SoundButtonDao,
    private val soundboardLayoutDao: SoundboardLayoutDao,
    private val connectionHistoryDao: ConnectionHistoryDao,
    private val socketManager: SocketManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val localAudioFileManager: LocalAudioFileManager
) {
    
    companion object {
        private const val TAG = "SoundboardRepository"
    }
    
    // Create a repository scope for audio playback coroutines
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var currentApiService: SoundboardApiService? = null
    private var currentServerUrl: String? = null
    
    // Sound Button operations
    fun getAllSoundButtons(): Flow<List<SoundButton>> = soundButtonDao.getAllSoundButtons()
    
    suspend fun getSoundButtonById(id: Int): SoundButton? = soundButtonDao.getSoundButtonById(id)
    
    suspend fun getSoundButtonByPosition(x: Int, y: Int): SoundButton? = 
        soundButtonDao.getSoundButtonByPosition(x, y)
    
    suspend fun insertSoundButton(soundButton: SoundButton): Long = 
        soundButtonDao.insertSoundButton(soundButton)
    
    suspend fun updateSoundButton(soundButton: SoundButton) = 
        soundButtonDao.updateSoundButton(soundButton)
    
    suspend fun deleteSoundButton(soundButton: SoundButton) = 
        soundButtonDao.deleteSoundButton(soundButton)
    
    suspend fun deleteSoundButtonById(id: Int) = 
        soundButtonDao.deleteSoundButtonById(id)
    
    suspend fun deleteAllSoundButtons() = 
        soundButtonDao.deleteAllSoundButtons()
    

    
    // Layout operations
    fun getAllLayouts(): Flow<List<SoundboardLayout>> = soundboardLayoutDao.getAllLayouts()
    
    suspend fun getActiveLayout(): SoundboardLayout? = soundboardLayoutDao.getActiveLayout()
    
    suspend fun getLayoutById(id: Int): SoundboardLayout? = soundboardLayoutDao.getLayoutById(id)
    
    suspend fun insertLayout(layout: SoundboardLayout): Long = 
        soundboardLayoutDao.insertLayout(layout)
    
    suspend fun updateLayout(layout: SoundboardLayout) = 
        soundboardLayoutDao.updateLayout(layout)
    
    suspend fun deleteLayout(layout: SoundboardLayout) = 
        soundboardLayoutDao.deleteLayout(layout)
    
    suspend fun deleteAllLayouts() = 
        soundboardLayoutDao.deleteAllLayouts()
    
    suspend fun switchActiveLayout(id: Long) = 
        soundboardLayoutDao.switchActiveLayout(id)
    
    // Connection history operations
    fun getAllConnections(): Flow<List<ConnectionHistory>> = 
        connectionHistoryDao.getAllConnections()
    
    fun getAllConnectionHistory(): Flow<List<ConnectionHistory>> = 
        connectionHistoryDao.getAllConnections()
    
    suspend fun getLastUsedConnection(): ConnectionHistory? {
        return try {
            val allConnections = connectionHistoryDao.getAllConnections().first()
            allConnections.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    fun getFavoriteConnections(): Flow<List<ConnectionHistory>> = 
        connectionHistoryDao.getFavoriteConnections()
    
    suspend fun getConnectionByAddress(ipAddress: String, port: Int): ConnectionHistory? = 
        connectionHistoryDao.getConnectionByAddress(ipAddress, port)
    
    suspend fun insertConnection(connection: ConnectionHistory): Long = 
        connectionHistoryDao.insertConnection(connection)
    
    suspend fun updateConnection(connection: ConnectionHistory) = 
        connectionHistoryDao.updateConnection(connection)
    
    suspend fun updateLastConnected(ipAddress: String, port: Int, timestamp: Long) = 
        connectionHistoryDao.updateLastConnected(ipAddress, port, timestamp)
    
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) = 
        connectionHistoryDao.updateFavoriteStatus(id, isFavorite)
    
    suspend fun deleteOldConnections(cutoffTime: Long) = 
        connectionHistoryDao.deleteOldConnections(cutoffTime)
    
    // Business logic methods
    suspend fun initializeDefaultLayout() {
        val activeLayout = getActiveLayout()
        if (activeLayout == null) {
            val defaultLayout = SoundboardLayout(
                name = "Default Layout",
                description = "Default 4x6 soundboard layout",
                isActive = true,
                gridColumns = 4,
                gridRows = 6
            )
            insertLayout(defaultLayout)
        }
    }
    
    suspend fun addOrUpdateConnection(computerName: String, ipAddress: String, port: Int) {
        val existingConnection = getConnectionByAddress(ipAddress, port)
        val currentTime = System.currentTimeMillis()
        
        if (existingConnection != null) {
            updateLastConnected(ipAddress, port, currentTime)
        } else {
            val newConnection = ConnectionHistory(
                computerName = computerName,
                ipAddress = ipAddress,
                port = port,
                lastConnected = currentTime
            )
            insertConnection(newConnection)
        }
    }
    
    // Network operations
    fun connectToServer(ipAddress: String, port: Int) {
        currentServerUrl = "http://$ipAddress:$port/"
        currentApiService = createApiService(currentServerUrl!!)
        socketManager.connect(ipAddress, port)
    }
    
    fun disconnectFromServer() {
        socketManager.disconnect()
        currentApiService = null
        currentServerUrl = null
    }
    
    /**
     * Get the server URL for HTTP requests.
     * If currentServerUrl is null (after app restart), try to determine it from connection state.
     */
    private suspend fun getServerUrl(): String? {
        // If we have a cached URL, use it
        if (currentServerUrl != null) {
            return currentServerUrl
        }
        
        // If not connected via socket, can't determine URL
        if (!socketManager.isConnected()) {
            Log.w(TAG, "⚠️ Not connected to server - cannot determine server URL")
            return null
        }
        
        // Try to get the last used connection to reconstruct the URL
        try {
            val lastConnection = getLastUsedConnection()
            if (lastConnection != null) {
                val reconstructedUrl = "http://${lastConnection.ipAddress}:${lastConnection.port}/"
                Log.d(TAG, "🔄 Reconstructed server URL from last connection: $reconstructedUrl")
                currentServerUrl = reconstructedUrl
                return reconstructedUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reconstructing server URL", e)
        }
        
        // Fallback to localhost for USB connections
        if (socketManager.isConnected()) {
            val fallbackUrl = "http://localhost:8080/"
            Log.d(TAG, "🔄 Using fallback server URL for USB connection: $fallbackUrl")
            currentServerUrl = fallbackUrl
            return fallbackUrl
        }
        
        return null
    }
    
    /**
     * Get or create API service, reconstructing if needed after app restart.
     */
    private suspend fun getOrCreateApiService(): SoundboardApiService? {
        // If we have a cached API service, use it
        if (currentApiService != null) {
            return currentApiService
        }
        
        // Try to get the server URL and recreate the API service
        val serverUrl = getServerUrl()
        if (serverUrl != null) {
            Log.d(TAG, "🔄 Recreating API service for: $serverUrl")
            currentApiService = createApiService(serverUrl)
            return currentApiService
        }
        
        return null
    }
    
    suspend fun playSound(soundButton: SoundButton): Result<String> {
        return try {
            Log.d(TAG, "🎵 PLAY SOUND CALLED: ID=${soundButton.id}, name=${soundButton.name}, isLocalFile=${soundButton.isLocalFile}, filePath=${soundButton.filePath}")
            
            if (soundButton.isLocalFile) {
                Log.d(TAG, "🔄 ROUTE: Using AUDIO FORWARDING for local file: ${soundButton.filePath}")
                // For local files, upload audio data to server for playback on computer speakers
                val result = playLocalFile(soundButton)
                Log.d(TAG, "🔄 AUDIO FORWARDING RESULT: $result")
                result
            } else {
                Log.d(TAG, "🌐 ROUTE: Using SOCKET.IO for server file: ${soundButton.filePath}")
                // For server files, send command to server
                socketManager.playSound(
                    filePath = soundButton.filePath,
                    volume = soundButton.volume,
                    buttonId = soundButton.id
                )
                
                Log.d(TAG, "✅ SOCKET.IO: Sound triggered: ${soundButton.name}")
                Result.success("Playing ${soundButton.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ PLAY SOUND ERROR: ${soundButton.name}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun playLocalFile(soundButton: SoundButton): Result<String> {
        return try {
            Log.d(TAG, "🔄 Playing local file: ${soundButton.filePath}")
            
            // Try to read the file directly first
            val file = java.io.File(soundButton.filePath)
            if (file.exists() && file.canRead()) {
                Log.d(TAG, "✅ Reading file directly: ${file.absolutePath}")
                return uploadAndPlayAudioData(file, soundButton.volume, soundButton.id)
            }
            
            // If direct file access fails, try using content resolver with URI
            Log.d(TAG, "🔄 Direct file access failed, trying content URI approach...")
            
            // Check if the path looks like a content URI
            if (soundButton.filePath.startsWith("content://")) {
                val uri = android.net.Uri.parse(soundButton.filePath)
                val tempFile = createTempFileFromUri(uri, soundButton.name)
                if (tempFile != null) {
                    Log.d(TAG, "✅ Created temp file from URI: ${tempFile.absolutePath}")
                    val result = uploadAndPlayAudioData(tempFile, soundButton.volume, soundButton.id)
                    // Clean up temp file
                    tempFile.delete()
                    return result
                } else {
                    Log.e(TAG, "❌ Failed to create temp file from URI")
                    return Result.failure(Exception("Could not access audio file"))
                }
            }
            
            Log.e(TAG, "❌ Local file not found and not a valid URI: ${soundButton.filePath}")
            Result.failure(Exception("Local file not found: ${soundButton.filePath}"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing local file: ${soundButton.filePath}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun createTempFileFromUri(uri: android.net.Uri, fileName: String): java.io.File? {
        return localAudioFileManager.createTempFileFromUri(uri, fileName)
    }
    
    private suspend fun uploadAndPlayAudioData(file: java.io.File, volume: Float, buttonId: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if we have a connection and get the server URL
            val serverUrl = getServerUrl()
            if (serverUrl == null) {
                return@withContext Result.failure(Exception("No server connection"))
            }
            
            Log.d(TAG, "📤 Uploading audio data to server: ${file.name} (${file.length()} bytes)")
            
            // Read audio file data
            val audioData = file.readBytes()
            
            // Create HTTP request to upload audio data
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val requestBody = audioData.toRequestBody(
                "application/octet-stream".toMediaTypeOrNull()
            )
            
            val request = okhttp3.Request.Builder()
                .url("${serverUrl}play-audio-data")
                .post(requestBody)
                .addHeader("X-Button-Id", buttonId.toString())
                .addHeader("X-File-Name", file.name)
                .addHeader("X-Volume", volume.toString())
                .addHeader("Content-Type", "application/octet-stream")
                .build()
            
            Log.d(TAG, "🌐 Making HTTP request to: ${serverUrl}play-audio-data")
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "✅ Audio uploaded successfully - playing on computer speakers: $responseBody")
                Result.success("Playing ${file.name} on computer speakers")
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "❌ Failed to upload audio: ${response.code} - $errorBody")
                Result.failure(Exception("Failed to upload audio: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading audio data", e)
            Result.failure(e)
        }
    }
    

    
    suspend fun uploadLocalFileToServer(filePath: String): Result<String> {
        return try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("File not found: $filePath"))
            }
            
            // TODO: Implement actual file upload to server
            // This would require adding a file upload endpoint to the server
            Log.d(TAG, "Upload local file to server: ${file.name}")
            
            // For now, return the filename as if it was uploaded
            Result.success(file.name)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading local file", e)
            Result.failure(e)
        }
    }
    
    suspend fun isLocalFileAccessible(filePath: String): Boolean {
        return try {
            val file = java.io.File(filePath)
            file.exists() && file.canRead() && localAudioFileManager.isValidAudioFile(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking local file accessibility", e)
            false
        }
    }
    
    suspend fun getLocalAudioFileMetadata(filePath: String): Result<AudioFile> {
        return try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("File not found: $filePath"))
            }
            
            val audioFile = AudioFile(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                format = file.extension.lowercase(),
                modified = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(file.lastModified())),
                uri = android.net.Uri.fromFile(file).toString()
            )
            
            Result.success(audioFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local file metadata", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableAudioFiles(): Result<List<AudioFile>> {
        return try {
            // Ensure we have an API service, recreate if needed
            val apiService = getOrCreateApiService()
            if (apiService == null) {
                return Result.failure(Exception("Not connected to server"))
            }
            
            val response = apiService.getAudioFiles()
            if (response.isSuccessful) {
                val audioFiles = response.body() ?: emptyList()
                Log.d(TAG, "Retrieved ${audioFiles.size} audio files")
                Result.success(audioFiles)
            } else {
                Log.e(TAG, "Failed to get audio files: ${response.code()}")
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio files", e)
            Result.failure(e)
        }
    }
    
    suspend fun getLocalAudioFiles(): Result<List<AudioFile>> {
        return try {
            val audioFiles = localAudioFileManager.getAudioFilesFromMediaStore()
            Log.d(TAG, "Retrieved ${audioFiles.size} local audio files")
            Result.success(audioFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local audio files", e)
            Result.failure(e)
        }
    }
    
    suspend fun getAudioFilesFromDirectory(directoryPath: String): Result<List<AudioFile>> {
        return try {
            val audioFiles = localAudioFileManager.getAudioFilesFromDirectory(directoryPath)
            Log.d(TAG, "Retrieved ${audioFiles.size} audio files from directory")
            Result.success(audioFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio files from directory", e)
            Result.failure(e)
        }
    }
    
    suspend fun getDirectoryContent(directoryPath: String): Result<LocalAudioFileManager.DirectoryContent> {
        return try {
            val content = localAudioFileManager.getDirectoryContent(directoryPath)
            Log.d(TAG, "Retrieved directory content: ${content.audioFiles.size} files, ${content.subdirectories.size} subdirectories")
            Result.success(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting directory content", e)
            Result.failure(e)
        }
    }
    
    fun getCommonAudioDirectories(): List<String> {
        return localAudioFileManager.getCommonAudioDirectories()
    }
    
    fun getDirectoryDisplayName(directoryPath: String): String {
        return localAudioFileManager.getDirectoryDisplayName(directoryPath)
    }
    
    fun getBreadcrumbs(directoryPath: String): List<Pair<String, String>> {
        return localAudioFileManager.getBreadcrumbs(directoryPath)
    }
    
    suspend fun testServerConnection(): Result<String> {
        return try {
            // Ensure we have an API service, recreate if needed
            val apiService = getOrCreateApiService()
            if (apiService == null) {
                return Result.failure(Exception("Not connected to server"))
            }
            
            val response = apiService.getHealth()
            if (response.isSuccessful) {
                val health = response.body()
                Result.success("Server OK - Version: ${health?.server_version}")
            } else {
                Result.failure(Exception("Server health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing server connection", e)
            Result.failure(e)
        }
    }
    
    // Socket manager access
    fun getSocketManager(): SocketManager = socketManager
    
    private fun createApiService(baseUrl: String): SoundboardApiService {
        val retrofitFactory = RetrofitFactory(okHttpClient, gson)
        return retrofitFactory.createApiService(baseUrl)
    }
} 