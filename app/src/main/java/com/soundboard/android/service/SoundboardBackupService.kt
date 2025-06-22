package com.soundboard.android.service

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.soundboard.android.data.repository.SettingsRepository
import com.soundboard.android.data.repository.SoundboardRepository
import com.soundboard.android.data.model.SoundButton
import com.soundboard.android.data.model.SoundboardLayout
import kotlinx.coroutines.flow.first

@Singleton
class SoundboardBackupService @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val soundboardRepository: SoundboardRepository
) {
    
    companion object {
        private const val BACKUP_VERSION = "2.0"
        private const val APP_VERSION = "6.2"
    }
    
    data class BackupData(
        val metadata: BackupMetadata,
        val settings: Map<String, Any>?,
        val layouts: List<SoundboardLayout>?,
        val soundButtons: List<SoundButton>?,
        val pathMappings: Map<String, String>?
    )
    
    data class BackupMetadata(
        val version: String,
        val appVersion: String,
        val timestamp: Long,
        val profileName: String,
        val deviceInfo: String,
        val includeSettings: Boolean,
        val includeLayouts: Boolean,
        val includeSoundButtons: Boolean,
        val includeLocalFiles: Boolean,
        val totalLayouts: Int,
        val totalSoundButtons: Int,
        val totalLocalFiles: Int
    )
    
    /**
     * Create a comprehensive backup of the soundboard configuration
     */
    suspend fun createCompleteBackup(
        context: Context,
        uri: Uri,
        includeSettings: Boolean = true,
        includeLayouts: Boolean = true,
        includeSoundButtons: Boolean = true,
        includeLocalFiles: Boolean = false,
        profileName: String = settingsRepository.getCurrentProfileName()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val layouts = if (includeLayouts) soundboardRepository.getAllLayouts().first() else emptyList()
            val soundButtons = if (includeSoundButtons) soundboardRepository.getAllSoundButtons().first() else emptyList()
            val localFiles = soundButtons.filter { it.isLocalFile }
            
            val metadata = BackupMetadata(
                version = BACKUP_VERSION,
                appVersion = APP_VERSION,
                timestamp = System.currentTimeMillis(),
                profileName = profileName,
                deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                includeSettings = includeSettings,
                includeLayouts = includeLayouts,
                includeSoundButtons = includeSoundButtons,
                includeLocalFiles = includeLocalFiles,
                totalLayouts = layouts.size,
                totalSoundButtons = soundButtons.size,
                totalLocalFiles = localFiles.size
            )
            
            val backupData = BackupData(
                metadata = metadata,
                settings = if (includeSettings) settingsRepository.getAllSettings() else null,
                layouts = if (includeLayouts) layouts else null,
                soundButtons = if (includeSoundButtons) soundButtons else null,
                pathMappings = if (includeLocalFiles) createPathMappings(localFiles) else null
            )
            
            val jsonBackup = serializeBackupData(backupData)
            
            // Write to file
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonBackup.toString(2).toByteArray())
            } ?: throw IOException("Could not open output stream")
            
            // Update last backup timestamp
            settingsRepository.setLastBackupTimestamp(System.currentTimeMillis())
            
            Result.success("Backup created successfully with ${metadata.totalLayouts} layouts and ${metadata.totalSoundButtons} sound buttons")
            
        } catch (e: Exception) {
            Result.failure(IOException("Failed to create backup: ${e.message}"))
        }
    }
    
    /**
     * Restore a complete backup of the soundboard configuration
     */
    suspend fun restoreCompleteBackup(
        context: Context,
        uri: Uri,
        restoreSettings: Boolean = true,
        restoreLayouts: Boolean = true,
        restoreSoundButtons: Boolean = true,
        mergeWithExisting: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Could not read backup file")
            
            val backupData = deserializeBackupData(backupContent)
            
            // Validate backup version compatibility
            if (!isBackupVersionCompatible(backupData.metadata.version)) {
                throw IOException("Backup version ${backupData.metadata.version} is not compatible with current app version")
            }
            
            var restoredItems = 0
            
            // Restore settings
            if (restoreSettings && backupData.settings != null) {
                settingsRepository.applySettings(backupData.settings)
                restoredItems++
            }
            
            // Restore layouts
            if (restoreLayouts && backupData.layouts != null) {
                if (!mergeWithExisting) {
                    // Clear existing layouts first
                    soundboardRepository.deleteAllLayouts()
                }
                
                backupData.layouts.forEach { layout ->
                    val layoutToInsert = layout.copy(id = 0) // Reset ID for new insertion
                    soundboardRepository.insertLayout(layoutToInsert)
                }
                restoredItems += backupData.layouts.size
            }
            
            // Restore sound buttons
            if (restoreSoundButtons && backupData.soundButtons != null) {
                if (!mergeWithExisting) {
                    // Clear existing sound buttons first
                    soundboardRepository.deleteAllSoundButtons()
                }
                
                backupData.soundButtons.forEach { soundButton ->
                    val buttonToInsert = soundButton.copy(id = 0) // Reset ID for new insertion
                    val resolvedButton = resolveFilePaths(buttonToInsert, backupData.pathMappings)
                    soundboardRepository.insertSoundButton(resolvedButton)
                }
                restoredItems += backupData.soundButtons.size
            }
            
            Result.success("Successfully restored $restoredItems items from backup created on ${formatTimestamp(backupData.metadata.timestamp)}")
            
        } catch (e: Exception) {
            Result.failure(IOException("Failed to restore backup: ${e.message}"))
        }
    }
    
    /**
     * Export individual layout with its sound buttons
     */
    suspend fun exportLayout(
        context: Context,
        uri: Uri,
        layoutId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val layout = soundboardRepository.getLayoutById(layoutId.toInt())
                ?: throw IOException("Layout not found")
            
            val soundButtons = soundboardRepository.getAllSoundButtons().first()
            
            val exportData = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("timestamp", System.currentTimeMillis())
                put("type", "layout_export")
                put("layout", serializeLayout(layout))
                put("sound_buttons", JSONArray().apply {
                    soundButtons.forEach { button ->
                        put(serializeSoundButton(button))
                    }
                })
                put("path_mappings", createPathMappings(soundButtons.filter { it.isLocalFile }))
            }
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportData.toString(2).toByteArray())
            } ?: throw IOException("Could not open output stream")
            
            Result.success("Layout '${layout.name}' exported successfully with ${soundButtons.size} sound buttons")
            
        } catch (e: Exception) {
            Result.failure(IOException("Failed to export layout: ${e.message}"))
        }
    }
    
    /**
     * Import individual layout
     */
    suspend fun importLayout(
        context: Context,
        uri: Uri,
        newLayoutName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val exportContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Could not read export file")
            
            val exportData = JSONObject(exportContent)
            
            if (exportData.getString("type") != "layout_export") {
                throw IOException("Invalid export file type")
            }
            
            val layoutJson = exportData.getJSONObject("layout")
            val soundButtonsJson = exportData.getJSONArray("sound_buttons")
            val pathMappings = if (exportData.has("path_mappings")) {
                deserializePathMappings(exportData.getJSONObject("path_mappings"))
            } else emptyMap()
            
            // Deserialize layout
            val layout = deserializeLayout(layoutJson).copy(
                id = 0, // Reset ID for new insertion
                name = newLayoutName ?: "${deserializeLayout(layoutJson).name} (Imported)",
                isActive = false // Don't make imported layout active by default
            )
            
            // Insert layout and get new ID
            val newLayoutId = soundboardRepository.insertLayout(layout)
            
            // Deserialize and insert sound buttons
            var importedButtons = 0
            for (i in 0 until soundButtonsJson.length()) {
                val buttonJson = soundButtonsJson.getJSONObject(i)
                val soundButton = deserializeSoundButton(buttonJson).copy(
                    id = 0 // Reset ID for new insertion
                )
                val resolvedButton = resolveFilePaths(soundButton, pathMappings)
                soundboardRepository.insertSoundButton(resolvedButton)
                importedButtons++
            }
            
            Result.success("Layout '${layout.name}' imported successfully with $importedButtons sound buttons")
            
        } catch (e: Exception) {
            Result.failure(IOException("Failed to import layout: ${e.message}"))
        }
    }
    
    /**
     * Create automatic backup if enabled
     */
    suspend fun performAutoBackup(context: Context): Result<String> {
        if (!settingsRepository.getAutoBackupEnabled()) {
            return Result.success("Auto-backup is disabled")
        }
        
        val frequency = settingsRepository.getAutoBackupFrequency()
        val lastBackup = settingsRepository.getLastBackupTimestamp()
        val now = System.currentTimeMillis()
        
        val shouldBackup = when (frequency) {
            "Hourly" -> now - lastBackup > 60 * 60 * 1000 // 1 hour
            "Daily" -> now - lastBackup > 24 * 60 * 60 * 1000 // 24 hours
            "Weekly" -> now - lastBackup > 7 * 24 * 60 * 60 * 1000 // 7 days
            "Monthly" -> now - lastBackup > 30 * 24 * 60 * 60 * 1000 // 30 days
            else -> false
        }
        
        if (!shouldBackup) {
            return Result.success("Auto-backup not due yet")
        }
        
        // Create auto-backup file name
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val profileName = settingsRepository.getCurrentProfileName()
        val fileName = "soundboard_auto_backup_${profileName}_$timestamp.json"
        
        // For auto-backup, we'll use internal storage or external files directory
        val backupDir = context.getExternalFilesDir("backups") ?: context.filesDir
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val backupFile = java.io.File(backupDir, fileName)
        val uri = Uri.fromFile(backupFile)
        
        return createCompleteBackup(
            context = context,
            uri = uri,
            includeSettings = settingsRepository.getBackupIncludeSettings(),
            includeLayouts = settingsRepository.getBackupIncludeLayouts(),
            includeSoundButtons = settingsRepository.getBackupIncludeSoundButtons(),
            includeLocalFiles = settingsRepository.getBackupIncludeLocalFiles()
        )
    }
    
    // Private helper methods
    
    private fun createPathMappings(localFiles: List<SoundButton>): Map<String, String> {
        val mappings = mutableMapOf<String, String>()
        localFiles.forEach { button ->
            if (button.isLocalFile) {
                // Extract just the filename from the full path for mapping
                val fileName = button.filePath.substringAfterLast("/")
                mappings[button.filePath] = fileName
            }
        }
        return mappings
    }
    
    private fun resolveFilePaths(
        soundButton: SoundButton,
        pathMappings: Map<String, String>?
    ): SoundButton {
        if (!soundButton.isLocalFile || pathMappings == null) {
            return soundButton
        }
        
        val strategy = settingsRepository.getPathResolutionStrategy()
        val preservePaths = settingsRepository.getPreserveFilePaths()
        
        return when (strategy) {
            "Smart" -> {
                // Try to resolve path intelligently
                if (preservePaths && pathMappings.containsKey(soundButton.filePath)) {
                    // Try to find file in common locations
                    val fileName = pathMappings[soundButton.filePath] ?: soundButton.filePath.substringAfterLast("/")
                    val defaultPath = settingsRepository.getDefaultLocalAudioPath()
                    soundButton.copy(filePath = "$defaultPath/$fileName")
                } else {
                    soundButton
                }
            }
            "Preserve" -> soundButton // Keep original paths
            "Reset" -> {
                // Reset to default location
                val fileName = soundButton.filePath.substringAfterLast("/")
                val defaultPath = settingsRepository.getDefaultLocalAudioPath()
                soundButton.copy(filePath = "$defaultPath/$fileName")
            }
            else -> soundButton
        }
    }
    
    private fun serializeBackupData(backupData: BackupData): JSONObject {
        return JSONObject().apply {
            put("metadata", serializeMetadata(backupData.metadata))
            
            if (backupData.settings != null) {
                put("settings", JSONObject(backupData.settings))
            }
            
            if (backupData.layouts != null) {
                put("layouts", JSONArray().apply {
                    backupData.layouts.forEach { layout ->
                        put(serializeLayout(layout))
                    }
                })
            }
            
            if (backupData.soundButtons != null) {
                put("sound_buttons", JSONArray().apply {
                    backupData.soundButtons.forEach { button ->
                        put(serializeSoundButton(button))
                    }
                })
            }
            
            if (backupData.pathMappings != null) {
                put("path_mappings", JSONObject(backupData.pathMappings))
            }
        }
    }
    
    private fun deserializeBackupData(jsonContent: String): BackupData {
        val json = JSONObject(jsonContent)
        
        val metadata = deserializeMetadata(json.getJSONObject("metadata"))
        
        val settings = if (json.has("settings")) {
            val settingsJson = json.getJSONObject("settings")
            settingsJson.keys().asSequence().associateWith { key ->
                settingsJson.get(key)
            }
        } else null
        
        val layouts = if (json.has("layouts")) {
            val layoutsJson = json.getJSONArray("layouts")
            (0 until layoutsJson.length()).map { i ->
                deserializeLayout(layoutsJson.getJSONObject(i))
            }
        } else null
        
        val soundButtons = if (json.has("sound_buttons")) {
            val buttonsJson = json.getJSONArray("sound_buttons")
            (0 until buttonsJson.length()).map { i ->
                deserializeSoundButton(buttonsJson.getJSONObject(i))
            }
        } else null
        
        val pathMappings = if (json.has("path_mappings")) {
            deserializePathMappings(json.getJSONObject("path_mappings"))
        } else null
        
        return BackupData(metadata, settings, layouts, soundButtons, pathMappings)
    }
    
    private fun serializeMetadata(metadata: BackupMetadata): JSONObject {
        return JSONObject().apply {
            put("version", metadata.version)
            put("app_version", metadata.appVersion)
            put("timestamp", metadata.timestamp)
            put("profile_name", metadata.profileName)
            put("device_info", metadata.deviceInfo)
            put("include_settings", metadata.includeSettings)
            put("include_layouts", metadata.includeLayouts)
            put("include_sound_buttons", metadata.includeSoundButtons)
            put("include_local_files", metadata.includeLocalFiles)
            put("total_layouts", metadata.totalLayouts)
            put("total_sound_buttons", metadata.totalSoundButtons)
            put("total_local_files", metadata.totalLocalFiles)
        }
    }
    
    private fun deserializeMetadata(json: JSONObject): BackupMetadata {
        return BackupMetadata(
            version = json.getString("version"),
            appVersion = json.getString("app_version"),
            timestamp = json.getLong("timestamp"),
            profileName = json.getString("profile_name"),
            deviceInfo = json.getString("device_info"),
            includeSettings = json.getBoolean("include_settings"),
            includeLayouts = json.getBoolean("include_layouts"),
            includeSoundButtons = json.getBoolean("include_sound_buttons"),
            includeLocalFiles = json.getBoolean("include_local_files"),
            totalLayouts = json.getInt("total_layouts"),
            totalSoundButtons = json.getInt("total_sound_buttons"),
            totalLocalFiles = json.getInt("total_local_files")
        )
    }
    
    private fun serializeLayout(layout: SoundboardLayout): JSONObject {
        return JSONObject().apply {
            put("id", layout.id)
            put("name", layout.name)
            put("description", layout.description)
            put("is_active", layout.isActive)
            put("grid_columns", layout.gridColumns)
            put("grid_rows", layout.gridRows)
            put("created_at", layout.createdAt)
            put("updated_at", layout.updatedAt)
            put("is_template", layout.isTemplate)
            put("template_category", layout.templateCategory)
            put("background_color", layout.backgroundColor)
            put("accent_color", layout.accentColor)
            put("button_spacing", layout.buttonSpacing)
            put("corner_radius", layout.cornerRadius)
            put("enable_glow_effect", layout.enableGlowEffect)
            put("max_buttons", layout.maxButtons)
            put("layout_preset", layout.layoutPreset.name)
            put("export_version", layout.exportVersion)
            put("original_author", layout.originalAuthor)
            put("download_url", layout.downloadUrl)
            put("tags", layout.tags)
        }
    }
    
    private fun deserializeLayout(json: JSONObject): SoundboardLayout {
        return SoundboardLayout(
            id = json.getLong("id"),
            name = json.getString("name"),
            description = json.optString("description"),
            isActive = json.getBoolean("is_active"),
            gridColumns = json.getInt("grid_columns"),
            gridRows = json.getInt("grid_rows"),
            createdAt = json.getLong("created_at"),
            updatedAt = json.getLong("updated_at"),
            isTemplate = json.getBoolean("is_template"),
            templateCategory = json.optString("template_category"),
            backgroundColor = json.getString("background_color"),
            accentColor = json.getString("accent_color"),
            buttonSpacing = json.getDouble("button_spacing").toFloat(),
            cornerRadius = json.getDouble("corner_radius").toFloat(),
            enableGlowEffect = json.getBoolean("enable_glow_effect"),
            maxButtons = json.getInt("max_buttons"),
            layoutPreset = com.soundboard.android.data.model.LayoutPreset.valueOf(json.getString("layout_preset")),
            exportVersion = json.getInt("export_version"),
            originalAuthor = json.optString("original_author"),
            downloadUrl = json.optString("download_url"),
            tags = json.optString("tags")
        )
    }
    
    private fun serializeSoundButton(button: SoundButton): JSONObject {
        return JSONObject().apply {
            put("id", button.id)
            put("name", button.name)
            put("file_path", button.filePath)
            put("is_local_file", button.isLocalFile)
            put("position_x", button.positionX)
            put("position_y", button.positionY)
            put("color", button.color)
            put("icon_name", button.iconName)
            put("volume", button.volume)
            put("created_at", button.createdAt)
            put("updated_at", button.updatedAt)
        }
    }
    
    private fun deserializeSoundButton(json: JSONObject): SoundButton {
        return SoundButton(
            id = json.getInt("id"),
            name = json.getString("name"),
            filePath = json.getString("file_path"),
            isLocalFile = json.getBoolean("is_local_file"),
            positionX = json.getInt("position_x"),
            positionY = json.getInt("position_y"),
            color = json.getString("color"),
            iconName = json.optString("icon_name"),
            volume = json.getDouble("volume").toFloat(),
            createdAt = json.getLong("created_at"),
            updatedAt = json.getLong("updated_at")
        )
    }
    
    private fun deserializePathMappings(json: JSONObject): Map<String, String> {
        return json.keys().asSequence().associateWith { key ->
            json.getString(key)
        }
    }
    
    private fun isBackupVersionCompatible(version: String): Boolean {
        return when (version) {
            "1.0", "2.0" -> true
            else -> false
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
} 