package com.soundboard.android.service

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.soundboard.android.data.repository.SettingsRepository
import com.soundboard.android.data.model.SoundButton

@Singleton
class PathManagerService @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    companion object {
        private val COMMON_AUDIO_DIRECTORIES = listOf(
            "Music",
            "Downloads",
            "Sounds",
            "Audio",
            "Soundboard",
            "Documents/Sounds",
            "Documents/Audio"
        )
        
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            ".mp3", ".wav", ".m4a", ".ogg", ".flac", ".aac", ".wma"
        )
    }
    
    data class PathResolution(
        val originalPath: String,
        val resolvedPath: String?,
        val isResolved: Boolean,
        val strategy: String,
        val confidence: Float
    )
    
    data class FileMapping(
        val originalPath: String,
        val fileName: String,
        val fileSize: Long?,
        val lastModified: Long?,
        val possiblePaths: List<String>
    )
    
    /**
     * Resolve file paths intelligently based on the configured strategy
     */
    suspend fun resolveFilePath(
        context: Context,
        originalPath: String,
        isLocalFile: Boolean
    ): PathResolution = withContext(Dispatchers.IO) {
        
        if (!isLocalFile) {
            // Server files don't need path resolution
            return@withContext PathResolution(
                originalPath = originalPath,
                resolvedPath = originalPath,
                isResolved = true,
                strategy = "Server",
                confidence = 1.0f
            )
        }
        
        val strategy = settingsRepository.getPathResolutionStrategy()
        val preservePaths = settingsRepository.getPreserveFilePaths()
        
        when (strategy) {
            "Smart" -> resolvePathSmart(context, originalPath, preservePaths)
            "Preserve" -> resolvePathPreserve(originalPath)
            "Reset" -> resolvePathReset(context, originalPath)
            else -> resolvePathSmart(context, originalPath, preservePaths)
        }
    }
    
    /**
     * Smart path resolution - tries to find files in likely locations
     */
    private suspend fun resolvePathSmart(
        context: Context,
        originalPath: String,
        preservePaths: Boolean
    ): PathResolution = withContext(Dispatchers.IO) {
        
        val fileName = originalPath.substringAfterLast("/")
        
        // First, try the original path if preserve is enabled
        if (preservePaths && File(originalPath).exists()) {
            return@withContext PathResolution(
                originalPath = originalPath,
                resolvedPath = originalPath,
                isResolved = true,
                strategy = "Smart-Preserve",
                confidence = 1.0f
            )
        }
        
        // Search in common directories
        val possiblePaths = findFileInCommonLocations(context, fileName)
        
        if (possiblePaths.isNotEmpty()) {
            // Use the first match with highest confidence
            val bestMatch = possiblePaths.first()
            return@withContext PathResolution(
                originalPath = originalPath,
                resolvedPath = bestMatch,
                isResolved = true,
                strategy = "Smart-Search",
                confidence = 0.8f
            )
        }
        
        // Fallback to default location
        val defaultPath = settingsRepository.getDefaultLocalAudioPath()
        val fallbackPath = "$defaultPath/$fileName"
        
        return@withContext PathResolution(
            originalPath = originalPath,
            resolvedPath = fallbackPath,
            isResolved = false,
            strategy = "Smart-Fallback",
            confidence = 0.3f
        )
    }
    
    /**
     * Preserve original paths - minimal changes
     */
    private fun resolvePathPreserve(originalPath: String): PathResolution {
        return PathResolution(
            originalPath = originalPath,
            resolvedPath = originalPath,
            isResolved = File(originalPath).exists(),
            strategy = "Preserve",
            confidence = if (File(originalPath).exists()) 1.0f else 0.0f
        )
    }
    
    /**
     * Reset paths to default location
     */
    private fun resolvePathReset(context: Context, originalPath: String): PathResolution {
        val fileName = originalPath.substringAfterLast("/")
        val defaultPath = settingsRepository.getDefaultLocalAudioPath()
        val newPath = "$defaultPath/$fileName"
        
        return PathResolution(
            originalPath = originalPath,
            resolvedPath = newPath,
            isResolved = File(newPath).exists(),
            strategy = "Reset",
            confidence = if (File(newPath).exists()) 0.9f else 0.4f
        )
    }
    
    /**
     * Find files in common audio directories
     */
    private suspend fun findFileInCommonLocations(
        context: Context,
        fileName: String
    ): List<String> = withContext(Dispatchers.IO) {
        
        val foundPaths = mutableListOf<String>()
        
        // Check external storage directories
        val externalStorage = Environment.getExternalStorageDirectory()
        COMMON_AUDIO_DIRECTORIES.forEach { dir ->
            val dirPath = File(externalStorage, dir)
            val filePath = File(dirPath, fileName)
            if (filePath.exists() && filePath.isFile) {
                foundPaths.add(filePath.absolutePath)
            }
        }
        
        // Check MediaStore for audio files
        try {
            val mediaStoreResults = findFileInMediaStore(context, fileName)
            foundPaths.addAll(mediaStoreResults)
        } catch (e: Exception) {
            // MediaStore query failed, continue with file system search
        }
        
        // Remove duplicates and sort by modification time (newest first)
        return@withContext foundPaths.distinct().sortedByDescending { path ->
            try {
                File(path).lastModified()
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    /**
     * Search for files using MediaStore
     */
    private suspend fun findFileInMediaStore(
        context: Context,
        fileName: String
    ): List<String> = withContext(Dispatchers.IO) {
        
        val foundPaths = mutableListOf<String>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )
        
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(dataColumn)
                    if (filePath != null && File(filePath).exists()) {
                        foundPaths.add(filePath)
                    }
                }
            }
        } catch (e: Exception) {
            // MediaStore query failed
        }
        
        return@withContext foundPaths
    }
    
    /**
     * Create file mappings for backup purposes
     */
    suspend fun createFileMappings(
        context: Context,
        soundButtons: List<SoundButton>
    ): Map<String, FileMapping> = withContext(Dispatchers.IO) {
        
        val mappings = mutableMapOf<String, FileMapping>()
        
        soundButtons.filter { it.isLocalFile }.forEach { button ->
            val file = File(button.filePath)
            val fileName = button.filePath.substringAfterLast("/")
            
            // Find all possible locations for this file
            val possiblePaths = findFileInCommonLocations(context, fileName)
            
            mappings[button.filePath] = FileMapping(
                originalPath = button.filePath,
                fileName = fileName,
                fileSize = if (file.exists()) file.length() else null,
                lastModified = if (file.exists()) file.lastModified() else null,
                possiblePaths = possiblePaths
            )
        }
        
        return@withContext mappings
    }
    
    /**
     * Validate file paths and report issues
     */
    suspend fun validateFilePaths(
        context: Context,
        soundButtons: List<SoundButton>
    ): PathValidationReport = withContext(Dispatchers.IO) {
        
        val validPaths = mutableListOf<String>()
        val invalidPaths = mutableListOf<String>()
        val resolvablePaths = mutableListOf<Pair<String, String>>()
        
        soundButtons.forEach { button ->
            if (button.isLocalFile) {
                val file = File(button.filePath)
                if (file.exists() && file.isFile) {
                    validPaths.add(button.filePath)
                } else {
                    invalidPaths.add(button.filePath)
                    
                    // Try to find alternative paths
                    val fileName = button.filePath.substringAfterLast("/")
                    val alternatives = findFileInCommonLocations(context, fileName)
                    if (alternatives.isNotEmpty()) {
                        resolvablePaths.add(button.filePath to alternatives.first())
                    }
                }
            }
        }
        
        return@withContext PathValidationReport(
            totalPaths = soundButtons.count { it.isLocalFile },
            validPaths = validPaths,
            invalidPaths = invalidPaths,
            resolvablePaths = resolvablePaths
        )
    }
    
    /**
     * Auto-fix invalid paths where possible
     */
    suspend fun autoFixPaths(
        context: Context,
        soundButtons: List<SoundButton>
    ): List<SoundButton> = withContext(Dispatchers.IO) {
        
        val fixedButtons = mutableListOf<SoundButton>()
        
        soundButtons.forEach { button ->
            if (button.isLocalFile) {
                val resolution = resolveFilePath(context, button.filePath, true)
                if (resolution.isResolved && resolution.resolvedPath != null) {
                    fixedButtons.add(button.copy(filePath = resolution.resolvedPath))
                } else {
                    fixedButtons.add(button) // Keep original if can't resolve
                }
            } else {
                fixedButtons.add(button) // Server files don't need fixing
            }
        }
        
        return@withContext fixedButtons
    }
    
    /**
     * Get recommended default audio path based on device
     */
    fun getRecommendedDefaultPath(): String {
        val externalStorage = Environment.getExternalStorageDirectory()
        
        // Check if common directories exist and return the first available
        COMMON_AUDIO_DIRECTORIES.forEach { dir ->
            val dirPath = File(externalStorage, dir)
            if (dirPath.exists() && dirPath.isDirectory) {
                return dir
            }
        }
        
        // Fallback to Music directory
        return "Music"
    }
    
    /**
     * Check if a file path is likely to be an audio file
     */
    fun isAudioFile(filePath: String): Boolean {
        val extension = filePath.substringAfterLast(".", "").lowercase()
        return SUPPORTED_AUDIO_EXTENSIONS.contains(".$extension")
    }
    
    /**
     * Generate a relative path from an absolute path for portability
     */
    fun makePathPortable(absolutePath: String): String {
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        return if (absolutePath.startsWith(externalStorage)) {
            absolutePath.removePrefix("$externalStorage/")
        } else {
            absolutePath.substringAfterLast("/")
        }
    }
    
    /**
     * Convert a portable path back to absolute path
     */
    fun makePathAbsolute(portablePath: String): String {
        return if (portablePath.startsWith("/")) {
            portablePath // Already absolute
        } else {
            val externalStorage = Environment.getExternalStorageDirectory().absolutePath
            "$externalStorage/$portablePath"
        }
    }
}

data class PathValidationReport(
    val totalPaths: Int,
    val validPaths: List<String>,
    val invalidPaths: List<String>,
    val resolvablePaths: List<Pair<String, String>>
) {
    val validCount: Int get() = validPaths.size
    val invalidCount: Int get() = invalidPaths.size
    val resolvableCount: Int get() = resolvablePaths.size
    val healthPercentage: Float get() = if (totalPaths > 0) (validCount.toFloat() / totalPaths) * 100f else 100f
} 