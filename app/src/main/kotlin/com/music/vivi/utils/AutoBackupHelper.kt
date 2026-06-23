/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.music.vivi.BuildConfig
import com.music.vivi.db.InternalDatabase
import com.music.vivi.viewmodels.BackupRestoreViewModel
import com.music.vivi.extensions.div
import com.music.vivi.extensions.zipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry

object AutoBackupHelper {
    private const val WEEKLY_WORK_NAME = "weekly_auto_backup"

    fun getBackupDir(context: Context): File {
        val dir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(null), "backups")
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            File(downloadsDir, "vivimusic")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun performBackup(context: Context, backupType: String): Boolean {
        val database = InternalDatabase.newInstance(context)
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val timestamp = LocalDateTime.now().format(formatter)
            
            val fileName = if (backupType == "before_update") {
                "auto_backup_before_update_${BuildConfig.VERSION_NAME}_$timestamp.backup"
            } else {
                "auto_backup_${backupType}_$timestamp.backup"
            }
            
            // Create a temporary file in cacheDir
            val tempFile = File(context.cacheDir, fileName)
            
            FileOutputStream(tempFile).use { fos ->
                fos.buffered().zipOutputStream().use { outputStream ->
                    // 1. Settings Backup
                    val settingsFile = context.filesDir / "datastore" / BackupRestoreViewModel.SETTINGS_FILENAME
                    if (settingsFile.exists()) {
                        settingsFile.inputStream().buffered().use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(BackupRestoreViewModel.SETTINGS_FILENAME))
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // 2. Room Database Backup
                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }

                    val dbPath = context.getDatabasePath(InternalDatabase.DB_NAME)
                    if (dbPath.exists()) {
                        FileInputStream(dbPath).use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }

            // Save the temp file to the final destination (public Download/vivimusic)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/vivimusic")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            tempFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        throw e
                    }
                } else {
                    throw java.io.IOException("Failed to create MediaStore entry in Downloads")
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val publicDir = File(downloadsDir, "vivimusic")
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                val targetFile = File(publicDir, fileName)
                tempFile.copyTo(targetFile, overwrite = true)
            }
            
            if (tempFile.exists()) {
                tempFile.delete()
            }

            // Clean up old backups of this type
            cleanUpOldBackups(context, backupType)
            Timber.tag("AutoBackup").d("Automatic backup completed successfully.")
            return true
        } catch (e: Exception) {
            reportException(e)
            Timber.tag("AutoBackup").e(e, "Automatic backup failed")
            return false
        } finally {
            database.close()
        }
    }

    private fun cleanUpOldBackups(context: Context, backupType: String) {
        val backups = getAutoBackups(context).filter { file ->
            file.name.startsWith("auto_backup_${backupType}_")
        }

        if (backups.size > 5) {
            // getAutoBackups returns sorted descending (newest first).
            // So we delete backups from index 5 to end (oldest).
            for (i in 5 until backups.size) {
                val file = backups[i]
                Timber.tag("AutoBackup").d("Deleting old backup: %s", file.name)
                deleteBackup(context, file)
            }
        }
    }

    fun getAutoBackups(context: Context): List<File> {
        val backupsList = mutableListOf<File>()
        
        // 1. App-specific backups folder (always writable, modern API Q+)
        try {
            val appDir = File(context.getExternalFilesDir(null), "backups")
            if (appDir.exists()) {
                val files = appDir.listFiles { file ->
                    file.isFile && file.name.startsWith("auto_backup_") && file.name.endsWith(".backup")
                }
                if (files != null) {
                    backupsList.addAll(files)
                }
            }
        } catch (e: Exception) {
            Timber.tag("AutoBackup").e(e, "Error reading backups from app dir")
        }

        // 2. Public Downloads/vivimusic folder (using MediaStore on Q+ or File API on legacy)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val projection = arrayOf(
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.DATA
                )
                val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? OR ${MediaStore.Downloads.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf("Download/vivimusic/%", "Download/vivimusic")
                
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATA)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameColumn)
                        val path = cursor.getString(dataColumn)
                        if (name.startsWith("auto_backup_") && name.endsWith(".backup") && path != null) {
                            val file = File(path)
                            if (backupsList.none { it.name == file.name }) {
                                backupsList.add(file)
                            }
                        }
                    }
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val publicDir = File(downloadsDir, "vivimusic")
                if (publicDir.exists()) {
                    val files = publicDir.listFiles { file ->
                        file.isFile && file.name.startsWith("auto_backup_") && file.name.endsWith(".backup")
                    }
                    if (files != null) {
                        for (file in files) {
                            if (backupsList.none { it.name == file.name }) {
                                backupsList.add(file)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("AutoBackup").e(e, "Error reading backups from public dir")
        }

        return backupsList.sortedByDescending { it.lastModified() } // Newest first
    }

    fun deleteBackup(context: Context, file: File): Boolean {
        return try {
            var deleted = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val selection = "${MediaStore.Downloads.DATA} = ?"
                val selectionArgs = arrayOf(file.absolutePath)
                val deletedRows = resolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    selection,
                    selectionArgs
                )
                deleted = deletedRows > 0
            }
            if (file.exists()) {
                val fileDeleted = file.delete()
                deleted = deleted || fileDeleted
            }
            deleted
        } catch (e: Exception) {
            reportException(e)
            Timber.tag("AutoBackup").e(e, "Failed to delete backup file")
            false
        }
    }

    fun updateWeeklyBackupWork(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            Timber.tag("AutoBackup").d("Enqueuing periodic weekly backup worker")
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            
            val autoBackupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag(WEEKLY_WORK_NAME)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WEEKLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                autoBackupRequest
            )
        } else {
            Timber.tag("AutoBackup").d("Cancelling periodic weekly backup worker")
            workManager.cancelUniqueWork(WEEKLY_WORK_NAME)
        }
    }
}
