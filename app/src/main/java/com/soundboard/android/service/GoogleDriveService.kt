package com.soundboard.android.service

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    private val context: Context
) {
    private var driveService: Drive? = null
    private var googleSignInClient: GoogleSignInClient? = null
    
    companion object {
        private const val SOUNDBOARD_FOLDER_NAME = "Soundboard Backups"
        private const val BACKUP_FILE_PREFIX = "soundboard_backup_"
    }
    
    init {
        initializeGoogleSignIn()
    }
    
    private fun initializeGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    suspend fun signIn(signInLauncher: ActivityResultLauncher<Intent>) {
        withContext(Dispatchers.Main) {
            val signInIntent = googleSignInClient?.signInIntent
            signInIntent?.let { signInLauncher.launch(it) }
        }
    }
    
    suspend fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                account?.let {
                    val credential = GoogleAccountCredential.usingOAuth2(
                        context,
                        listOf(DriveScopes.DRIVE_FILE)
                    )
                    credential.selectedAccount = account.account
                    
                    driveService = Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory(),
                        credential
                    )
                        .setApplicationName("Soundboard App")
                        .build()
                    
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun isSignedIn(): Boolean {
        return withContext(Dispatchers.IO) {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            account != null && driveService != null
        }
    }
    
    suspend fun signOut() {
        withContext(Dispatchers.Main) {
            googleSignInClient?.signOut()
            driveService = null
        }
    }
    
    suspend fun uploadBackup(backupData: String, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: throw IllegalStateException("Not signed in to Google Drive")
                
                // Create or get the Soundboard folder
                val folderId = getOrCreateSoundboardFolder(drive)
                
                // Create file metadata
                val fileMetadata = File().apply {
                    name = "${BACKUP_FILE_PREFIX}${fileName}.json"
                    parents = listOf(folderId)
                }
                
                // Create file content
                val content = com.google.api.client.http.ByteArrayContent(
                    "application/json",
                    backupData.toByteArray()
                )
                
                // Upload file
                val file = drive.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute()
                
                file.id
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    suspend fun downloadBackup(fileId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: throw IllegalStateException("Not signed in to Google Drive")
                
                val outputStream = ByteArrayOutputStream()
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                
                outputStream.toString("UTF-8")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    suspend fun listBackups(): List<BackupFile> {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: throw IllegalStateException("Not signed in to Google Drive")
                
                val folderId = getOrCreateSoundboardFolder(drive)
                
                val result = drive.files().list()
                    .setQ("parents in '$folderId' and name contains '$BACKUP_FILE_PREFIX'")
                    .setFields("files(id, name, createdTime, size)")
                    .execute()
                
                result.files?.map { file ->
                    BackupFile(
                        id = file.id,
                        name = file.name.removePrefix(BACKUP_FILE_PREFIX).removeSuffix(".json"),
                        createdTime = file.createdTime?.value ?: 0L,
                        size = file.getSize() ?: 0L
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun deleteBackup(fileId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: throw IllegalStateException("Not signed in to Google Drive")
                drive.files().delete(fileId).execute()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    private fun getOrCreateSoundboardFolder(drive: Drive): String {
        // Check if folder exists
        val result = drive.files().list()
            .setQ("name='$SOUNDBOARD_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder'")
            .setFields("files(id)")
            .execute()
        
        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            // Create folder
            val folderMetadata = File().apply {
                name = SOUNDBOARD_FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }
            
            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            folder.id
        }
    }
    
    fun getSignInClient(): GoogleSignInClient? = googleSignInClient
}

data class BackupFile(
    val id: String,
    val name: String,
    val createdTime: Long,
    val size: Long
) 