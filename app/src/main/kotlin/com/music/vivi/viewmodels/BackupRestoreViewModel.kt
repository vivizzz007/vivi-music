/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import android.content.Intent
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
import com.music.vivi.playback.MusicService
import com.music.vivi.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
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
        // Phase 1 (background): decompress the archive and stage the restored
        // files to temporary paths, so the heavy I/O never blocks the UI thread
        // (which previously froze the app on large backups).
        Thread {
            val tmpDb = java.io.File(context.cacheDir, "restore_song.db.tmp")
            val tmpSettings = java.io.File(context.cacheDir, "restore_settings.pb.tmp")
            tmpDb.delete()
            tmpSettings.delete()
            var hasDb = false
            var hasSettings = false

            val staged = runCatching {
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
            }

            // Phase 2 (main thread): swap the staged files in and kill the
            // process in one synchronous block, so no other UI work can touch
            // the database between close() and exitProcess().
            Handler(Looper.getMainLooper()).post {
                staged
                    .onSuccess { applyStagedRestore(context, tmpDb.takeIf { hasDb }, tmpSettings.takeIf { hasSettings }) }
                    .onFailure { e ->
                        reportException(e)
                        Timber.tag("RESTORE").e(e, "Restore failed")
                        Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                        tmpDb.delete()
                        tmpSettings.delete()
                    }
            }
        }.start()
    }

    private fun applyStagedRestore(context: Context, tmpDb: java.io.File?, tmpSettings: java.io.File?) {
        runCatching {
            // Stop playback first so the service can't hit the database while we
            // close and replace it below.
            context.stopService(Intent(context, MusicService::class.java))

            if (tmpSettings != null) {
                val target = context.filesDir / "datastore" / SETTINGS_FILENAME
                target.parentFile?.mkdirs()
                tmpSettings.copyTo(target, overwrite = true)
            }

            if (tmpDb != null) {
                // capture path before closing DB to avoid reopening race
                val dbPath = database.openHelper.writableDatabase.path
                database.close()
                // The DB uses write-ahead logging: delete the WAL/SHM sidecars
                // and the old DB before swapping in the restored file, so the
                // restored DB is never mixed with stale journal frames on the
                // next launch (which forced uninstall/reinstall).
                java.io.File(dbPath + "-wal").delete()
                java.io.File(dbPath + "-shm").delete()
                java.io.File(dbPath).delete()
                tmpDb.copyTo(java.io.File(dbPath), overwrite = true)
                Timber.tag("RESTORE").i("DB overwrite complete")
            }

            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            // Kill the process so the next launch reads the restored files fresh.
            exitProcess(0)
        }.onFailure {
            reportException(it)
            Timber.tag("RESTORE").e(it, "Restore failed")
            Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
        }
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

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
    }
}
