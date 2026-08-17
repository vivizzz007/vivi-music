/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.ViewModel
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
import com.music.vivi.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess

data class CsvImportState(
    val previewRows: List<List<String>> = emptyList(),
    val artistColumnIndex: Int = 0,
    val titleColumnIndex: Int = 1,
    val urlColumnIndex: Int = -1,
    val hasHeader: Boolean = true,
)

data class ConvertedSongLog(
    val title: String,
    val artists: String,
)

data class BackupInfo(
    val fileName: String,
    val dateText: String,
    val versionText: String,
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir / "datastore" / SETTINGS_FILENAME).inputStream().buffered()
                        .use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                            inputStream.copyTo(outputStream)
                        }
                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }
                    FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure {
            reportException(it)
            Toast.makeText(context, R.string.backup_create_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun restore(context: Context, uri: Uri) {
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }
        if (inputStream == null) {
            Timber.tag("RESTORE").e("Could not open input stream for uri: $uri")
            Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
            return
        }
        restoreFromInputStream(context, inputStream)
    }

    /**
     * Reads display metadata (name, date, version) for a selected backup file
     * without touching its contents, so the UI can ask for confirmation before
     * applying it.
     */
    fun readBackupInfo(context: Context, uri: Uri): BackupInfo {
        val unknown = context.getString(R.string.restore_unknown)
        var fileName = ""
        var lastModified = -1L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: ""
                    val lmIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
                    if (lmIdx >= 0) lastModified = cursor.getLong(lmIdx) * 1000L
                }
            }
        }
        val parsedDate = parseBackupDate(fileName)
        val fallbackDate = if (lastModified > 0) formatMillis(lastModified) else null
        val dateText = parsedDate ?: fallbackDate ?: unknown
        val versionText = parseBackupVersion(fileName) ?: unknown
        return BackupInfo(fileName, dateText, versionText)
    }

    private fun parseBackupDate(name: String): String? {
        val match = Regex("""(\d{8})[_\s]?(\d{6})""").find(name) ?: return null
        val raw = match.groupValues[1] + match.groupValues[2]
        return runCatching {
            LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a"))
        }.getOrNull()
    }

    private fun parseBackupVersion(name: String): String? =
        Regex("""\d+\.\d+\.\d+""").find(name)?.value

    private fun formatMillis(millis: Long): String? = runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a"))
    }.getOrNull()

    fun restoreFromFile(context: Context, file: java.io.File) {
        val inputStream = try {
            java.io.FileInputStream(file)
        } catch (e: Exception) {
            null
        }
        if (inputStream == null) {
            Timber.tag("RESTORE").e("Could not open input stream for file: ${file.absolutePath}")
            Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
            return
        }
        restoreFromInputStream(context, inputStream)
    }

    private fun restoreFromInputStream(context: Context, raw: java.io.InputStream) {
        val appContext = context.applicationContext
        // Decompress the archive to a persistent staging directory on a
        // background thread, then kill the process. App.onCreate() swaps the
        // staged files in before Room/DataStore are opened on the next launch.
        // This avoids closing the shared Room database while the app is still
        // running, which crashed with in-flight queries ("database is closed").
        Thread {
            val pendingDir = java.io.File(appContext.filesDir, PENDING_RESTORE_DIR)
            val staged = runCatching {
                pendingDir.deleteRecursively()
                pendingDir.mkdirs()
                val tmpDb = java.io.File(pendingDir, InternalDatabase.DB_NAME)
                val tmpSettings = java.io.File(pendingDir, SETTINGS_FILENAME)
                var hasDb = false
                var hasSettings = false

                raw.use {
                    it.zipInputStream().use { inputStream ->
                        var entry = tryOrNull { inputStream.nextEntry } // prevent ZipException
                        while (entry != null) {
                            Timber.tag("RESTORE").i("Found zip entry: ${entry.name}")
                            when (entry.name) {
                                SETTINGS_FILENAME -> {
                                    hasSettings = true
                                    FileOutputStream(tmpSettings).use { output -> inputStream.copyTo(output) }
                                }
                                InternalDatabase.DB_NAME -> {
                                    hasDb = true
                                    FileOutputStream(tmpDb).use { output -> inputStream.copyTo(output) }
                                }
                                else -> Timber.tag("RESTORE").i("Skipping unexpected entry: ${entry.name}")
                            }
                            entry = tryOrNull { inputStream.nextEntry } // prevent ZipException
                        }
                    }
                }
                if (!hasDb && !hasSettings) error("No backup entries found in archive")
                if (hasDb) {
                    validateStagedDatabase(tmpDb)?.let { error("Corrupt backup: $it") }
                }
            }

            Handler(Looper.getMainLooper()).post {
                staged.onSuccess {
                    // The staged files are applied by App.onCreate() before the
                    // database/DataStore are opened on the next launch.
                    exitProcess(0)
                }.onFailure { e ->
                    pendingDir.deleteRecursively()
                    reportException(e)
                    Timber.tag("RESTORE").e(e, "Restore failed")
                    val msg = if (e.message?.startsWith("Corrupt backup:") == true) {
                        appContext.getString(R.string.restore_failed_corrupt)
                    } else {
                        appContext.getString(R.string.restore_failed)
                    }
                    Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    fun previewCsvFile(context: Context, uri: Uri): CsvImportState {
        val previewRows = mutableListOf<List<String>>()
        val csvState: CsvImportState
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                val rowsToPreview = lines.take(6).map { parseCsvLine(it) }
                previewRows.addAll(rowsToPreview)

                val hasHeader = lines.isNotEmpty() && lines[0].contains(",")
                csvState = CsvImportState(
                    previewRows = previewRows,
                    hasHeader = hasHeader,
                )
                return csvState
            }
        }.onFailure {
            reportException(it)
            Toast.makeText(context, "Failed to preview CSV file", Toast.LENGTH_SHORT).show()
        }
        return CsvImportState()
    }

    fun importPlaylistFromCsv(
        context: Context,
        uri: Uri,
        columnMapping: CsvImportState,
        onProgress: (Int) -> Unit = {},
        onLogUpdate: (List<ConvertedSongLog>) -> Unit = {},
    ): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        val recentLogs = mutableListOf<ConvertedSongLog>()

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                val startIndex = if (columnMapping.hasHeader) 1 else 0
                val totalLines = lines.size - startIndex

                lines.drop(startIndex).forEachIndexed { index, line ->
                    val parts = parseCsvLine(line)

                    if (parts.isNotEmpty()) {
                        if (columnMapping.artistColumnIndex < parts.size && columnMapping.titleColumnIndex < parts.size) {
                            val title = parts[columnMapping.titleColumnIndex].trim()
                            val artistStr = parts[columnMapping.artistColumnIndex].trim()
                            val url = if (columnMapping.urlColumnIndex >= 0 && columnMapping.urlColumnIndex < parts.size) {
                                parts[columnMapping.urlColumnIndex].trim()
                            } else {
                                ""
                            }

                            if (title.isNotEmpty() && artistStr.isNotEmpty()) {
                                val artists = artistStr.split(";", ",").map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { ArtistEntity(id = "", name = it) }

                                val mockSong = Song(
                                    song = SongEntity(
                                        id = "",
                                        title = title,
                                    ),
                                    artists = artists,
                                )
                                songs.add(mockSong)

                                // Update log with last 3 songs
                                val logEntry = ConvertedSongLog(
                                    title = title,
                                    artists = artists.joinToString(", ") { it.name },
                                )
                                recentLogs.add(0, logEntry)
                                if (recentLogs.size > 3) {
                                    recentLogs.removeAt(recentLogs.size - 1)
                                }
                                onLogUpdate(recentLogs.toList())
                            }
                        }
                    }

                    // Update progress
                    val progress = ((index + 1) * 100) / totalLines
                    onProgress(progress.coerceIn(0, 99))
                }
            }
        }.onFailure {
            reportException(it)
            Timber.tag("CSV_IMPORT").e(it, "CSV import failed")
            Toast.makeText(
                context,
                "Failed to import CSV file",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        // Legacy method for compatibility
        return importPlaylistFromCsv(context, uri, CsvImportState())
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result.map { it.trim().trim('"') }
    }

    fun loadM3UOnline(
        context: Context,
        uri: Uri,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.first().startsWith("#EXTM3U")) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            // maybe later write this to be more efficient
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)

                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    /**
     * Verifies a staged `song.db` is a valid, non-corrupt SQLite database before
     * it gets swapped in on the next launch (a corrupt file would otherwise crash
     * Room with a `SQLiteDatabaseCorruptException`). Returns an error message,
     * or null when the database is valid.
     */
    private fun validateStagedDatabase(file: java.io.File): String? {
        // Fast check: the SQLite header magic ("SQLite format 3\0").
        val headerOk = runCatching {
            FileInputStream(file).use { input ->
                val magic = ByteArray(16)
                input.read(magic) == 16 &&
                    String(magic, Charsets.US_ASCII) == "SQLite format 3\u0000"
            }
        }.getOrDefault(false)
        if (!headerOk) return "not a valid SQLite database"

        return runCatching {
            val staged = android.database.sqlite.SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
            )
            staged.use { db ->
                val integrity = db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else "no result"
                }
                if (integrity != "ok") return@use "database integrity check failed: $integrity"

                val version = db.rawQuery("PRAGMA user_version", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else -1L
                }
                if (version > InternalDatabase.DB_VERSION) {
                    return@use "database schema version $version is newer than this app supports"
                }
                null
            }
        }.getOrElse { "could not validate the database: ${it.message ?: "unknown error"}" }
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
        const val PENDING_RESTORE_DIR = "pending_restore"

        /**
         * Applies a staged backup (settings + database) before Room and
         * DataStore are opened. Called from App.onCreate() on startup.
         */
        fun applyPendingRestoreIfNeeded(context: Context) {
            val pendingDir = java.io.File(context.filesDir, PENDING_RESTORE_DIR)
            val stagedSettings = java.io.File(pendingDir, SETTINGS_FILENAME)
            val stagedDb = java.io.File(pendingDir, InternalDatabase.DB_NAME)
            if (!stagedSettings.exists() && !stagedDb.exists()) {
                pendingDir.deleteRecursively()
                return
            }
            val result = runCatching {
                if (stagedSettings.exists()) {
                    val target = context.filesDir / "datastore" / SETTINGS_FILENAME
                    target.parentFile?.mkdirs()
                    stagedSettings.copyTo(target, overwrite = true)
                }
                if (stagedDb.exists()) {
                    val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
                    dbFile.parentFile?.mkdirs()
                    java.io.File(dbFile.absolutePath + "-wal").delete()
                    java.io.File(dbFile.absolutePath + "-shm").delete()
                    dbFile.delete()
                    stagedDb.copyTo(dbFile, overwrite = true)
                }
                context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            }
            result.onFailure {
                // Keep the staged backup so the restore can be retried on the
                // next launch instead of silently losing the user's data.
                android.util.Log.e(
                    "BackupRestore",
                    "Failed to apply pending restore; keeping staged files for retry",
                    it
                )
                return
            }
            pendingDir.deleteRecursively()
        }
    }
}
