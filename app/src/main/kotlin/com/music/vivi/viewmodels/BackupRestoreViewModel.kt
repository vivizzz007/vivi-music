package com.music.vivi.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.music.vivi.MainActivity
import com.music.vivi.R
import com.music.vivi.db.InternalDatabase
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.extensions.div
import com.music.vivi.extensions.tryOrNull
import com.music.vivi.extensions.zipInputStream
import com.music.vivi.extensions.zipOutputStream
import com.music.vivi.playback.MusicService
import com.music.vivi.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * ViewModel for Backup and Restore operations.
 * Handles exporting the Database and Settings to a ZIP file, and restoring from it.
 * Also supports importing Playlists from CSV and M3U files.
 */
@HiltViewModel
public class BackupRestoreViewModel @Inject constructor(public val database: MusicDatabase) : ViewModel() {
    public fun backup(context: Context, uri: Uri) {
        var backupSuccessful = false
        var tempBackupCreated = false

        runCatching {
            // Validate output stream can be opened
            val outputStream = context.applicationContext.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                throw IllegalStateException("Unable to open output stream for backup")
            }

            outputStream.use { outStream ->
                outStream.buffered().zipOutputStream().use { zipStream ->
                    var settingsBackedUp = false
                    var databaseBackedUp = false

                    // Backup settings file
                    val settingsFile = context.filesDir / "datastore" / SETTINGS_FILENAME
                    if (settingsFile.exists() && settingsFile.canRead()) {
                        try {
                            val settingsSize = settingsFile.length()
                            if (settingsSize > 0) {
                                settingsFile.inputStream().buffered().use { inputStream ->
                                    zipStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                                    val bytesCopied = inputStream.copyTo(zipStream)
                                    zipStream.closeEntry()

                                    // Verify data was actually written
                                    if (bytesCopied > 0) {
                                        settingsBackedUp = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            reportException(e)
                            // Continue backup even if settings fail, but log it
                        }
                    }

                    // Backup database
                    try {
                        // Checkpoint to ensure database is in consistent state
                        runBlocking(Dispatchers.IO) {
                            database.checkpoint()
                        }

                        val dbPath = database.openHelper.writableDatabase.path
                        if (dbPath == null) {
                            throw IllegalStateException("Database path is null")
                        }

                        val dbFile = java.io.File(dbPath)
                        if (!dbFile.exists()) {
                            throw IllegalStateException("Database file does not exist")
                        }

                        if (!dbFile.canRead()) {
                            throw IllegalStateException("Cannot read database file")
                        }

                        val dbSize = dbFile.length()
                        if (dbSize == 0L) {
                            throw IllegalStateException("Database file is empty")
                        }

                        FileInputStream(dbPath).use { inputStream ->
                            zipStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                            val bytesCopied = inputStream.copyTo(zipStream)
                            zipStream.closeEntry()

                            // Verify complete database was copied
                            if (bytesCopied != dbSize) {
                                throw IllegalStateException(
                                    "Database backup incomplete: copied $bytesCopied of $dbSize bytes"
                                )
                            }

                            databaseBackedUp = true
                        }
                    } catch (e: Exception) {
                        reportException(e)
                        throw IllegalStateException("Failed to backup database: ${e.message}", e)
                    }

                    // Ensure critical data was backed up
                    if (!databaseBackedUp) {
                        throw IllegalStateException("Database backup failed - no data written")
                    }

                    // Flush and finish the ZIP properly
                    zipStream.finish()
                    zipStream.flush()
                    outStream.flush()

                    tempBackupCreated = true
                }
            }

            // Verify the backup file was created and has content
            context.applicationContext.contentResolver.openInputStream(uri)?.use { verifyStream ->
                val backupSize = verifyStream.available()
                if (backupSize <= 0) {
                    throw IllegalStateException("Backup file is empty after creation")
                }
            } ?: throw IllegalStateException("Cannot verify backup file")

            backupSuccessful = true
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            reportException(error)

            // If backup creation started but failed, try to delete the corrupt file
            if (tempBackupCreated && !backupSuccessful) {
                try {
                    context.applicationContext.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    reportException(e)
                }
            }

            val errorMessage = when {
                error.message?.contains("space", ignoreCase = true) == true ->
                    "Backup failed: Not enough storage space"
                error.message?.contains("permission", ignoreCase = true) == true ->
                    "Backup failed: Permission denied"
                error.message?.contains("read", ignoreCase = true) == true ->
                    "Backup failed: Cannot read database"
                else ->
                    context.getString(R.string.backup_create_failed) + ": ${error.message}"
            }

            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    public fun restore(context: Context, uri: Uri) {
        runCatching {
            // Validate input stream can be opened
            val inputStream = context.applicationContext.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                throw IllegalStateException("Unable to open backup file")
            }

            var hasValidEntries = false
            var restoredSettings = false
            var restoredDatabase = false
            var isValidZipFile = false

            inputStream.use { stream ->
                try {
                    stream.zipInputStream().use { zipStream ->
                        isValidZipFile = true
                        var entry = tryOrNull { zipStream.nextEntry }

                        // Check if we can read at least one entry
                        if (entry == null) {
                            throw IllegalStateException("Backup file appears to be empty or corrupted")
                        }

                        while (entry != null) {
                            hasValidEntries = true

                            when (entry.name) {
                                SETTINGS_FILENAME -> {
                                    try {
                                        val settingsFile = context.filesDir / "datastore" / SETTINGS_FILENAME
                                        settingsFile.parentFile?.mkdirs() // Ensure directory exists

                                        settingsFile.outputStream().use { outputStream ->
                                            val bytesCopied = zipStream.copyTo(outputStream)
                                            if (bytesCopied > 0) {
                                                restoredSettings = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        reportException(e)
                                        // Continue restore even if settings fail
                                    }
                                }

                                InternalDatabase.DB_NAME -> {
                                    try {
                                        runBlocking(Dispatchers.IO) {
                                            database.checkpoint()
                                        }
                                        database.close()

                                        val dbPath = database.openHelper.writableDatabase.path
                                        if (dbPath == null) {
                                            throw IllegalStateException("Database path is null")
                                        }

                                        // Create temporary backup of current database
                                        val currentDbFile = java.io.File(dbPath)
                                        val tempBackupFile = java.io.File(dbPath + ".temp_backup")

                                        if (currentDbFile.exists()) {
                                            try {
                                                currentDbFile.copyTo(tempBackupFile, overwrite = true)
                                            } catch (e: Exception) {
                                                reportException(e)
                                            }
                                        }

                                        try {
                                            FileOutputStream(dbPath).use { outputStream ->
                                                val bytesCopied = zipStream.copyTo(outputStream)
                                                if (bytesCopied > 0) {
                                                    restoredDatabase = true
                                                    // Delete temp backup on success
                                                    tempBackupFile.delete()
                                                } else {
                                                    throw IllegalStateException("No data was copied from backup")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Restore original database if restore failed
                                            if (tempBackupFile.exists()) {
                                                try {
                                                    tempBackupFile.copyTo(currentDbFile, overwrite = true)
                                                    tempBackupFile.delete()
                                                } catch (restoreError: Exception) {
                                                    reportException(restoreError)
                                                }
                                            }
                                            throw e
                                        }
                                    } catch (e: Exception) {
                                        reportException(e)
                                        throw IllegalStateException("Failed to restore database: ${e.message}", e)
                                    }
                                }

                                else -> {
                                    // Unknown entry, skip it
                                }
                            }
                            entry = tryOrNull { zipStream.nextEntry }
                        }
                    }
                } catch (e: java.util.zip.ZipException) {
                    throw IllegalStateException("File is not a valid backup file or is corrupted", e)
                }
            }

            // Validate restore was successful
            if (!isValidZipFile) {
                throw IllegalStateException("This app does not support this backup file format")
            }

            if (!hasValidEntries) {
                throw IllegalStateException("Backup file is empty or corrupted. This app cannot restore from this file")
            }

            if (!restoredDatabase) {
                throw IllegalStateException(
                    "Backup file is missing required data. This app cannot restore from this file"
                )
            }

            // Show success message
            Toast.makeText(context, R.string.restore_success, Toast.LENGTH_SHORT).show()

            // Only restart if restore was successful
            context.stopService(Intent(context, MusicService::class.java))
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            context.startActivity(Intent(context, MainActivity::class.java))
            exitProcess(0)
        }.onFailure { error ->
            reportException(error)

            val errorMessage = when {
                error.message?.contains("not a valid backup", ignoreCase = true) == true ->
                    context.getString(R.string.app_does_not_support_backup_file)
                error.message?.contains("corrupted", ignoreCase = true) == true ->
                    context.getString(R.string.backup_file_corrupted)
                error.message?.contains("missing required data", ignoreCase = true) == true ->
                    context.getString(R.string.app_does_not_support_backup_missing_db)
                error.message?.contains("empty", ignoreCase = true) == true ->
                    context.getString(R.string.backup_file_empty)
                error.message?.contains("format", ignoreCase = true) == true ->
                    context.getString(R.string.app_does_not_support_backup_format)
                error.message?.contains("permission", ignoreCase = true) == true ->
                    context.getString(R.string.cannot_restore_permission_denied)
                else ->
                    context.getString(R.string.app_does_not_support_backup_with_error, error.message ?: "")
            }

            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    public fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()

                // Validate file is not empty
                if (lines.isEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.csv_file_empty_or_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                    return songs
                }

                lines.forEachIndexed { index, line ->
                    // Skip empty lines
                    if (line.isBlank()) return@forEachIndexed

                    try {
                        val parts = line.split(",").map { it.trim() }

                        // Validate CSV has at least 2 columns (title and artist)
                        if (parts.size < 2) {
                            // Skip invalid lines silently or log them
                            return@forEachIndexed
                        }

                        val title = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@forEachIndexed
                        val artistStr =
                            parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: context.getString(R.string.unknown_artist)

                        val artists = artistStr.split(";").map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map {
                                ArtistEntity(
                                    id = "",
                                    name = it
                                )
                            }

                        // Ensure we have at least one artist
                        val finalArtists = if (artists.isEmpty()) {
                            listOf(ArtistEntity(id = "", name = context.getString(R.string.unknown_artist)))
                        } else {
                            artists
                        }

                        val mockSong = Song(
                            song = SongEntity(
                                id = "",
                                title = title
                            ),
                            artists = finalArtists
                        )
                        songs.add(mockSong)
                    } catch (e: Exception) {
                        // Log error for this line but continue processing
                        reportException(e)
                    }
                }
            }
        }.onFailure { e ->
            reportException(e)
            Toast.makeText(
                context,
                "Failed to read CSV file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.no_valid_songs_in_csv),
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    public fun loadM3UOnline(context: Context, uri: Uri): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()

                // Validate M3U file format
                if (lines.isEmpty()) {
                    Toast.makeText(
                        context,
                        "M3U file is empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    return songs
                }

                if (!lines.first().startsWith("#EXTM3U")) {
                    Toast.makeText(
                        context,
                        "Invalid M3U file format. File must start with #EXTM3U",
                        Toast.LENGTH_SHORT
                    ).show()
                    return songs
                }

                lines.forEachIndexed { index, rawLine ->
                    if (rawLine.startsWith("#EXTINF:")) {
                        try {
                            // Extract metadata after #EXTINF:
                            val metadata = rawLine.substringAfter("#EXTINF:")

                            // Check if metadata contains comma (required format)
                            if (!metadata.contains(',')) {
                                return@forEachIndexed
                            }

                            val info = metadata.substringAfter(',')

                            // Check if info contains " - " separator
                            if (!info.contains(" - ")) {
                                // Fallback: treat entire info as title
                                val mockSong = Song(
                                    song = SongEntity(
                                        id = "",
                                        title = info.trim()
                                    ),
                                    artists = listOf(ArtistEntity("", "Unknown Artist"))
                                )
                                songs.add(mockSong)
                                return@forEachIndexed
                            }

                            val artistStr = info.substringBefore(" - ").trim()
                            val title = info.substringAfter(" - ").trim()

                            // Validate we have both title and artist
                            if (title.isBlank()) {
                                return@forEachIndexed
                            }

                            val artists = if (artistStr.isNotBlank()) {
                                artistStr.split(';')
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .map { ArtistEntity("", it) }
                            } else {
                                listOf(ArtistEntity("", "Unknown Artist"))
                            }

                            val finalArtists = if (artists.isEmpty()) {
                                listOf(ArtistEntity("", "Unknown Artist"))
                            } else {
                                artists
                            }

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title
                                ),
                                artists = finalArtists
                            )
                            songs.add(mockSong)
                        } catch (e: Exception) {
                            // Log error for this line but continue processing
                            reportException(e)
                        }
                    }
                }
            }
        }.onFailure { e ->
            reportException(e)
            Toast.makeText(
                context,
                "Failed to read M3U file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No valid songs found in M3U file",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    public companion object {
        public const val SETTINGS_FILENAME: String = "settings.preferences_pb"
    }
}
