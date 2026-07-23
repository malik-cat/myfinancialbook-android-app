package com.myfinancialbook.app.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class GoogleDriveManager(private val context: Context) {

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("My Financial Book").build()
    }

    private var _backupFolderId: String? = null
    private val backupFolderName = "MyFinancialBook_Backups"

    private suspend fun getOrCreateBackupFolder(service: Drive): String {
        _backupFolderId?.let { return it }

        val query = "name = '$backupFolderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val existing = service.files().list()
            .setQ(query)
            .setFields("files(id)")
            .execute().files

        if (!existing.isNullOrEmpty()) {
            _backupFolderId = existing[0].id
            return _backupFolderId!!
        }

        val folder = com.google.api.services.drive.model.File().apply {
            name = backupFolderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val created = service.files().create(folder).execute()
        _backupFolderId = created.id
        return _backupFolderId!!
    }

    suspend fun uploadBackup(dbFile: File): Unit = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: throw Exception("Google Drive Service not initialized")
        val folderId = getOrCreateBackupFolder(service)

        val existingFiles = service.files().list()
            .setQ("name = 'my_financial_book_backup.db' and '$folderId' in parents and trashed = false")
            .setOrderBy("modifiedTime desc")
            .execute().files

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = "my_financial_book_backup.db"
            parents = listOf(folderId)
        }
        val mediaContent = FileContent("application/x-sqlite3", dbFile)

        if (existingFiles.isNullOrEmpty()) {
            val created = service.files().create(fileMetadata, mediaContent).execute()
            Log.d("GoogleDriveManager", "Created new backup: ${created.id}, size: ${dbFile.length()}")
        } else {
            val updated = service.files().update(existingFiles[0].id, null, mediaContent).execute()
            Log.d("GoogleDriveManager", "Updated existing backup: ${updated.id}, size: ${dbFile.length()}")
        }
    }

    suspend fun downloadBackup(dbFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext false
            val folderId = getOrCreateBackupFolder(service)

            val existingFiles = service.files().list()
                .setQ("name = 'my_financial_book_backup.db' and '$folderId' in parents and trashed = false")
                .setOrderBy("modifiedTime desc")
                .execute().files

            if (existingFiles.isNullOrEmpty()) return@withContext false

            val driveFileId = existingFiles[0].id
            val parentDir = dbFile.parentFile ?: return@withContext false
            if (!parentDir.exists()) parentDir.mkdirs()

            // Download to temp file first
            val tempFile = File(parentDir, "my_financial_book_backup_temp.db")
            java.io.FileOutputStream(tempFile).use { fos ->
                service.files().get(driveFileId).executeMediaAndDownloadTo(fos)
                fos.fd.sync()
            }

            if (tempFile.length() == 0L) {
                Log.e("GoogleDriveManager", "Downloaded backup is empty"); tempFile.delete()
                return@withContext false
            }

            // Wipe old database files (including -wal, -shm, -journal) to prevent SQLite WAL replay corruption
            val extensions = listOf("", "-wal", "-shm", "-journal")
            extensions.forEach { ext ->
                val f = File(parentDir, "my_financial_book.db$ext")
                if (f.exists()) f.delete()
            }

            // Copy temp file to actual db path
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            java.io.RandomAccessFile(dbFile, "rw").use { it.fd.sync() }

            Log.d("GoogleDriveManager", "Successfully downloaded latest backup: $driveFileId, size: ${dbFile.length()}")
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Failed to download backup", e)
            false
        }
    }

    suspend fun downloadBackupTo(destFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService() ?: return@withContext false
            val folderId = getOrCreateBackupFolder(service)

            val existingFiles = service.files().list()
                .setQ("name = 'my_financial_book_backup.db' and '$folderId' in parents and trashed = false")
                .setOrderBy("modifiedTime desc")
                .execute().files

            if (existingFiles.isNullOrEmpty()) return@withContext false

            val driveFileId = existingFiles[0].id
            val parentDir = destFile.parentFile ?: return@withContext false
            if (!parentDir.exists()) parentDir.mkdirs()

            java.io.FileOutputStream(destFile).use { fos ->
                service.files().get(driveFileId).executeMediaAndDownloadTo(fos)
                fos.fd.sync()
            }

            if (destFile.length() == 0L) {
                Log.e("GoogleDriveManager", "Downloaded backup is empty"); destFile.delete()
                return@withContext false
            }

            Log.d("GoogleDriveManager", "Successfully downloaded latest backup: $driveFileId, size: ${destFile.length()}")
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Failed to download backup", e)
            false
        }
    }
}
