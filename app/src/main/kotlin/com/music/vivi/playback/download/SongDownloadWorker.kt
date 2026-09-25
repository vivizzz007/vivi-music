/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback.download

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.music.innertube.models.IpVersion
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.DownloadDirectoryUriKey
import com.music.vivi.constants.IpVersionKey
import com.music.vivi.db.InternalDatabase
import com.music.vivi.extensions.toEnum
import com.music.vivi.utils.YTPlayerUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.time.LocalDateTime

class SongDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: songId
        val artist = inputData.getString(KEY_ARTIST).orEmpty()

        val database = InternalDatabase.newInstance(context)
        var outputFile: DocumentFile? = null

        try {
            val ipVersion = context.dataStore.get(IpVersionKey).toEnum(IpVersion.AUTO)
            val audioQuality = context.dataStore.get(AudioQualityKey).toEnum(AudioQuality.AUTO)
            val connectivityManager = context.getSystemService<ConnectivityManager>()!!

            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                songId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
                context = context,
            ).getOrElse { throwable ->
                Timber.tag(TAG).e(throwable, "Failed to resolve stream for $songId")
                return@withContext Result.failure()
            }

            val destinationDir = resolveDestinationDir(context)
                ?: return@withContext Result.failure()

            val mimeType = playbackData.format.mimeType.substringBefore(";").trim()
            val extension = extensionForMimeType(mimeType)
            val fileName = sanitizeFileName(
                if (artist.isNotBlank()) "$title - $artist" else title
            ) + extension

            destinationDir.findFile(fileName)?.delete()
            val newFile = destinationDir.createFile(mimeType, fileName)
                ?: return@withContext Result.failure()
            outputFile = newFile

            val httpClient = OkHttpClient.Builder()
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        val addresses = Dns.SYSTEM.lookup(hostname)
                        return when (ipVersion) {
                            IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                            IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                            IpVersion.AUTO -> addresses
                        }
                    }
                })
                .build()

            val request = okhttp3.Request.Builder()
                .url(playbackData.streamUrl)
                .apply {
                    playbackData.streamHeaders.forEach { (name, value) -> header(name, value) }
                }
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).e("Download HTTP ${response.code} for $songId")
                    return@withContext Result.failure()
                }
                val body = response.body ?: return@withContext Result.failure()
                val totalBytes = body.contentLength().takeIf { it > 0 }

                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var totalRead = 0L
                        var lastReportedPercent = -1
                        while (!isStopped) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            if (totalBytes != null) {
                                val percent = ((totalRead * 100) / totalBytes).toInt()
                                if (percent != lastReportedPercent) {
                                    lastReportedPercent = percent
                                    setProgress(workDataOf(KEY_PROGRESS to percent))
                                }
                            }
                        }
                    }
                } ?: return@withContext Result.failure()
            }

            if (isStopped) {
                newFile.delete()
                return@withContext Result.failure()
            }

            database.updateDownloadedInfo(songId, true, LocalDateTime.now(), newFile.uri.toString())

            Result.success(workDataOf(KEY_URI to newFile.uri.toString()))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Download failed for $songId")
            runCatching { outputFile?.delete() }
            Result.failure()
        }
    }

    private fun resolveDestinationDir(context: Context): DocumentFile? {
        val customUri = context.dataStore.get(DownloadDirectoryUriKey, "")
        if (customUri.isNotBlank()) {
            val tree = DocumentFile.fromTreeUri(context, customUri.toUri())
            if (tree != null && tree.exists() && tree.canWrite()) return tree
            Timber.tag(TAG).w("Custom download directory is no longer accessible, falling back to default")
        }
        val defaultDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
            "",
        ).apply { mkdirs() }
        return DocumentFile.fromFile(defaultDir)
    }

    private fun extensionForMimeType(mimeType: String): String = when {
        mimeType.contains("webm") -> ".webm"
        mimeType.contains("mp4") || mimeType.contains("aac") -> ".m4a"
        else -> ".audio"
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(150)

    companion object {
        private const val TAG = "SongDownloadWorker"
        const val KEY_SONG_ID = "song_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_PROGRESS = "progress"
        const val KEY_URI = "uri"
    }
}
