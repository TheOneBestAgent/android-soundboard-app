package com.soundboard.android.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.soundboard.android.network.api.AudioFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAudioFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "LocalAudioFileManager"
        private val SUPPORTED_FORMATS = setOf("mp3", "wav", "m4a", "ogg", "flac", "aac")
    }
    
    data class DirectoryContent(
        val audioFiles: List<AudioFile>,
        val subdirectories: List<String>,
        val parentDirectory: String? = null,
        val currentPath: String
    )
    
    suspend fun getAudioFilesFromMediaStore(): List<AudioFile> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<AudioFile>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val data = cursor.getString(dataColumn)
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val mimeType = cursor.getString(mimeColumn)
                    
                    // Extract file extension
                    val extension = name.substringAfterLast('.', "").lowercase()
                    
                    // Only include supported audio formats
                    if (extension in SUPPORTED_FORMATS) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        
                        audioFiles.add(
                            AudioFile(
                                name = name,
                                path = data, // Full file path for local files
                                format = extension,
                                size = size,
                                uri = contentUri.toString() // Store URI for access
                            )
                        )
                    }
                }
            }
            
            Log.d(TAG, "Found ${audioFiles.size} local audio files")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning local audio files", e)
        }
        
        audioFiles
    }
    
    suspend fun getAudioFilesFromDirectory(directoryPath: String): List<AudioFile> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<AudioFile>()
        
        try {
            val directory = File(directoryPath)
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val extension = file.extension.lowercase()
                        if (extension in SUPPORTED_FORMATS) {
                            audioFiles.add(
                                AudioFile(
                                    name = file.name,
                                    path = file.absolutePath,
                                    format = extension,
                                    size = file.length(),
                                    uri = Uri.fromFile(file).toString()
                                )
                            )
                        }
                    }
                }
                
                Log.d(TAG, "Found ${audioFiles.size} audio files in $directoryPath")
            } else {
                Log.w(TAG, "Directory does not exist or is not accessible: $directoryPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory $directoryPath", e)
        }
        
        audioFiles.sortedBy { it.name }
    }
    
    suspend fun getDirectoryContent(directoryPath: String): DirectoryContent = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<AudioFile>()
        val subdirectories = mutableListOf<String>()
        
        try {
            val directory = File(directoryPath)
            if (directory.exists() && directory.isDirectory && directory.canRead()) {
                directory.listFiles()?.forEach { file ->
                    when {
                        file.isDirectory && file.canRead() -> {
                            // Add subdirectory
                            subdirectories.add(file.absolutePath)
                        }
                        file.isFile -> {
                            val extension = file.extension.lowercase()
                            if (extension in SUPPORTED_FORMATS) {
                                audioFiles.add(
                                    AudioFile(
                                        name = file.name,
                                        path = file.absolutePath,
                                        format = extension,
                                        size = file.length(),
                                        uri = Uri.fromFile(file).toString()
                                    )
                                )
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Found ${audioFiles.size} audio files and ${subdirectories.size} subdirectories in $directoryPath")
            } else {
                Log.w(TAG, "Directory does not exist or is not accessible: $directoryPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory $directoryPath", e)
        }
        
        val parentDirectory = getParentDirectory(directoryPath)
        
        DirectoryContent(
            audioFiles = audioFiles.sortedBy { it.name },
            subdirectories = subdirectories.sortedBy { File(it).name },
            parentDirectory = parentDirectory,
            currentPath = directoryPath
        )
    }
    
    private fun getParentDirectory(directoryPath: String): String? {
        return try {
            val directory = File(directoryPath)
            val parent = directory.parentFile
            
            // Don't go above external storage or root
            val externalStorage = android.os.Environment.getExternalStorageDirectory()
            if (parent != null && parent.exists() && parent.canRead() && 
                parent.absolutePath.startsWith(externalStorage.absolutePath)) {
                parent.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting parent directory for $directoryPath", e)
            null
        }
    }
    
    fun getCommonAudioDirectories(): List<String> {
        val directories = mutableListOf<String>()
        
        // Add common audio directories
        val externalStorage = android.os.Environment.getExternalStorageDirectory()
        
        listOf(
            "Music",
            "Download",
            "Downloads", 
            "Sounds",
            "Audio",
            "Ringtones",
            "Notifications",
            "Podcasts",
            "Audiobooks"
        ).forEach { dirName ->
            val dir = File(externalStorage, dirName)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                directories.add(dir.absolutePath)
            }
        }
        
        // Add app-specific directories
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)?.let {
            if (it.exists()) directories.add(it.absolutePath)
        }
        
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.let {
            if (it.exists()) directories.add(it.absolutePath)
        }
        
        // Add DCIM for screen recordings with audio
        val dcimDir = File(externalStorage, "DCIM")
        if (dcimDir.exists() && dcimDir.isDirectory && dcimDir.canRead()) {
            directories.add(dcimDir.absolutePath)
        }
        
        return directories
    }
    
    fun isValidAudioFile(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return false
        
        val extension = file.extension.lowercase()
        return extension in SUPPORTED_FORMATS
    }
    
    fun getDirectoryDisplayName(directoryPath: String): String {
        return try {
            val directory = File(directoryPath)
            directory.name
        } catch (e: Exception) {
            directoryPath.substringAfterLast('/')
        }
    }
    
    fun getBreadcrumbs(directoryPath: String): List<Pair<String, String>> {
        val breadcrumbs = mutableListOf<Pair<String, String>>()
        val externalStorage = android.os.Environment.getExternalStorageDirectory()
        
        try {
            var currentDir = File(directoryPath)
            val pathParts = mutableListOf<Pair<String, String>>()
            
            // Build path from current directory up to external storage
            while (currentDir.absolutePath.startsWith(externalStorage.absolutePath) && 
                   currentDir.absolutePath != externalStorage.absolutePath) {
                pathParts.add(0, Pair(currentDir.name, currentDir.absolutePath))
                currentDir = currentDir.parentFile ?: break
            }
            
            // Add storage root
            pathParts.add(0, Pair("Storage", externalStorage.absolutePath))
            
            breadcrumbs.addAll(pathParts)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating breadcrumbs for $directoryPath", e)
            // Fallback to simple display
            breadcrumbs.add(Pair(getDirectoryDisplayName(directoryPath), directoryPath))
        }
        
        return breadcrumbs
    }
    
    suspend fun createTempFileFromUri(uri: Uri, fileName: String): java.io.File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = java.io.File.createTempFile("soundboard_", "_$fileName", context.cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
                Log.d(TAG, "Created temp file from URI: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                tempFile
            } else {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file from URI: $uri", e)
            null
        }
    }
} 