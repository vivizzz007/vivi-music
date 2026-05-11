/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.IBinder
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.music.innertube.YouTube
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.ExportingSongIdsKey
import com.music.vivi.constants.ExportedSongIdsKey
import com.music.vivi.utils.YTPlayerUtils
import com.music.vivi.utils.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class AudioExportService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val songId = intent?.getStringExtra(EXTRA_SONG_ID) ?: return START_NOT_STICKY
        val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE).orEmpty()
        val songArtist = intent.getStringExtra(EXTRA_SONG_ARTIST).orEmpty()
        val songAlbum = intent.getStringExtra(EXTRA_SONG_ALBUM).orEmpty()
        val artworkUrl = intent.getStringExtra(EXTRA_ARTWORK_URL).orEmpty()
        val targetDirectoryUri = intent.getStringExtra(EXTRA_TARGET_DIRECTORY_URI) ?: return START_NOT_STICKY

        serviceScope.launch {
            exportSong(
                songId = songId,
                songTitle = songTitle,
                songArtist = songArtist,
                songAlbum = songAlbum,
                artworkUrl = artworkUrl,
                targetDirectoryUri = targetDirectoryUri,
            )
        }
        return START_NOT_STICKY
    }

    private suspend fun exportSong(
        songId: String,
        songTitle: String,
        songArtist: String,
        songAlbum: String,
        artworkUrl: String,
        targetDirectoryUri: String,
    ) {
        val safeTitle = sanitizeTitle(songTitle.ifBlank { songId })
        addExportingSongId(songId)
        runCatching {
            val connectivityManager = getSystemService<ConnectivityManager>()
                ?: error("No connectivity manager")
            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = songId,
                audioQuality = AudioQuality.HIGH,
                connectivityManager = connectivityManager,
            ).getOrThrow()
            val year = YouTube.getMediaInfo(songId)
                .getOrNull()
                ?.uploadDate
                ?.let { uploadDate ->
                    Regex("(19|20)\\d{2}")
                        .find(uploadDate)
                        ?.value
                        ?.toIntOrNull()
                }
            val rangedStreamUrl = playbackData.streamUrl.let { baseUrl ->
                val totalLength = playbackData.format.contentLength ?: 10_000_000L
                "$baseUrl&range=0-$totalLength"
            }

            val tempSourceFile = File.createTempFile("export_source_", ".m4a", cacheDir)
            val tempArtworkFile = File.createTempFile("export_cover_", ".jpg", cacheDir)
            val tempMp3File = File.createTempFile("export_result_", ".mp3", cacheDir)

            val streamRequest = Request.Builder().url(rangedStreamUrl).build()
            var totalBytes = -1L
            var bytesWritten = 0L
            httpClient.newCall(streamRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Stream request failed with ${response.code}")
                }
                val body = response.body ?: error("No response body")
                totalBytes = body.contentLength()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                body.byteStream().use { input ->
                    tempSourceFile.outputStream().use { output ->
                        var read: Int
                        var lastProgress = -1
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            if (totalBytes > 0) {
                                val progress = ((bytesWritten * 55L) / totalBytes).toInt().coerceIn(0, 55)
                                if (progress > lastProgress) {
                                    lastProgress = progress
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }
            if (totalBytes > 0 && bytesWritten < totalBytes) {
                error("Incomplete export source: wrote $bytesWritten of $totalBytes bytes")
            }

            val artworkDownloaded = artworkUrl.isNotBlank() && runCatching {
                httpClient.newCall(Request.Builder().url(artworkUrl).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    response.body?.byteStream()?.use { input ->
                        tempArtworkFile.outputStream().use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                    }
                }
            }.isSuccess && tempArtworkFile.length() > 0L

            val ffmpegCommand = buildFfmpegCommand(
                inputPath = tempSourceFile.absolutePath,
                outputPath = tempMp3File.absolutePath,
                title = songTitle,
                artist = songArtist,
                album = songAlbum,
                year = year,
                coverPath = if (artworkDownloaded) tempArtworkFile.absolutePath else null,
            )
            val session = FFmpegKit.execute(ffmpegCommand)
            val returnCode = session.returnCode
            if (returnCode == null || !ReturnCode.isSuccess(returnCode)) {
                error("FFmpeg failed: ${session.output}")
            }
            if (!tempMp3File.exists() || tempMp3File.length() <= 0L) {
                error("Exported MP3 file is empty")
            }

            val destinationDir = DocumentFile.fromTreeUri(this, Uri.parse(targetDirectoryUri))
                ?: error("Export directory unavailable")
            val outputFile = destinationDir.createFile("audio/mpeg", "$safeTitle.mp3")
                ?: error("Unable to create output file")

            tempMp3File.inputStream().use { input ->
                contentResolver.openOutputStream(outputFile.uri, "w")?.use { output ->
                    input.copyTo(output)
                    output.flush()
                } ?: error("Unable to open output stream")
            }

            tempSourceFile.delete()
            tempArtworkFile.delete()
            tempMp3File.delete()
            addExportedSongId(songId)
        }.onFailure { }
        removeExportingSongId(songId)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun addExportedSongId(songId: String) {
        dataStore.edit { preferences ->
            val current = preferences[ExportedSongIdsKey].orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val updated = listOf(songId) + current.filterNot { it == songId }
            preferences[ExportedSongIdsKey] = updated.take(1000).joinToString(",")
        }
    }

    private suspend fun addExportingSongId(songId: String) {
        dataStore.edit { preferences ->
            val current = preferences[ExportingSongIdsKey].orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val updated = listOf(songId) + current.filterNot { it == songId }
            preferences[ExportingSongIdsKey] = updated.take(1000).joinToString(",")
        }
    }

    private suspend fun removeExportingSongId(songId: String) {
        dataStore.edit { preferences ->
            val current = preferences[ExportingSongIdsKey].orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            preferences[ExportingSongIdsKey] = current.filterNot { it == songId }.joinToString(",")
        }
    }

    companion object {
        private const val EXTRA_SONG_ID = "extra_song_id"
        private const val EXTRA_SONG_TITLE = "extra_song_title"
        private const val EXTRA_SONG_ARTIST = "extra_song_artist"
        private const val EXTRA_SONG_ALBUM = "extra_song_album"
        private const val EXTRA_ARTWORK_URL = "extra_artwork_url"
        private const val EXTRA_TARGET_DIRECTORY_URI = "extra_target_directory_uri"

        fun start(
            context: Context,
            songId: String,
            songTitle: String,
            songArtist: String,
            songAlbum: String,
            artworkUrl: String,
            targetDirectoryUri: String,
        ) {
            val intent = Intent(context, AudioExportService::class.java).apply {
                putExtra(EXTRA_SONG_ID, songId)
                putExtra(EXTRA_SONG_TITLE, songTitle)
                putExtra(EXTRA_SONG_ARTIST, songArtist)
                putExtra(EXTRA_SONG_ALBUM, songAlbum)
                putExtra(EXTRA_ARTWORK_URL, artworkUrl)
                putExtra(EXTRA_TARGET_DIRECTORY_URI, targetDirectoryUri)
            }
            context.startService(intent)
        }

        private fun sanitizeTitle(title: String): String =
            title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { "song_${System.currentTimeMillis()}" }

        private fun buildFfmpegCommand(
            inputPath: String,
            outputPath: String,
            title: String,
            artist: String,
            album: String,
            year: Int?,
            coverPath: String?,
        ): String {
            val escapedInput = inputPath.ffmpegEscape()
            val escapedOutput = outputPath.ffmpegEscape()
            val titleMeta = title.ffmpegEscape()
            val artistMeta = artist.ffmpegEscape()
            val albumMeta = album.ffmpegEscape()
            val yearMeta = year?.toString()?.ffmpegEscape()
            val dateFlags = if (yearMeta != null) " -metadata date='$yearMeta' -metadata year='$yearMeta'" else ""
            return if (coverPath != null) {
                val escapedCover = coverPath.ffmpegEscape()
                "-y -i '$escapedInput' -i '$escapedCover' -map 0:a -map 1:v -c:v mjpeg -disposition:v attached_pic -c:a libmp3lame -b:a 320k -id3v2_version 3 -metadata title='$titleMeta' -metadata artist='$artistMeta' -metadata album='$albumMeta'$dateFlags -metadata:s:v title='Album cover' -metadata:s:v comment='Cover (front)' '$escapedOutput'"
            } else {
                "-y -i '$escapedInput' -c:a libmp3lame -b:a 320k -id3v2_version 3 -metadata title='$titleMeta' -metadata artist='$artistMeta' -metadata album='$albumMeta'$dateFlags '$escapedOutput'"
            }
        }

        private fun String.ffmpegEscape(): String = replace("'", "'\\''")

    }
}
