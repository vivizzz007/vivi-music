package com.music.vivi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Snapshot of an in-progress update download. */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long,
) {
    val percent: Int
        get() = if (totalBytes <= 0) 0 else ((downloadedBytes * 100) / totalBytes).toInt()
}

/**
 * Downloads update installers into `~/.vivimusic/updates/` and reports progress
 * (percent + speed). Also exposes the list of downloaded installers and a way
 * to delete them.
 */
object UpdateDownloader {
    val updatesDir: File =
        File(System.getProperty("user.home"), ".vivimusic/updates").apply { mkdirs() }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Files previously downloaded by this updater (installers only). */
    fun downloadedInstallers(): List<File> =
        updatesDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun deleteAll() {
        updatesDir.listFiles()?.forEach { it.delete() }
    }

    fun delete(file: File) {
        file.delete()
    }

    /**
     * Downloads [url] to `updatesDir/[fileName]`, invoking [onProgress] as bytes
     * arrive. Returns the downloaded file. Throws on network errors.
     */
    suspend fun download(
        url: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .header("Accept", "application/octet-stream")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("HTTP ${response.code}")
            }
            val body = response.body
            val total = body.contentLength()
            val dest = File(updatesDir, fileName)
            dest.parentFile?.mkdirs()

            var downloaded = 0L
            var lastSampleAt = System.currentTimeMillis()
            var lastSampleBytes = 0L
            var speed = 0L

            dest.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastSampleAt
                        if (elapsed >= 500) {
                            speed = ((downloaded - lastSampleBytes) * 1000L) / elapsed
                            lastSampleAt = now
                            lastSampleBytes = downloaded
                        }
                        onProgress(DownloadProgress(downloaded, total, speed))
                    }
                }
            }
            dest
        }
    }
}
