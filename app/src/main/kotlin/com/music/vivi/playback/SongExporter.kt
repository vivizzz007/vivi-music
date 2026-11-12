package com.music.vivi.playback

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Copies a finished download out of the app's private cache into the phone's
 * Music/Ctrl Music folder as an .m4a (AAC) file that any player can open.
 * AAC streams are only remuxed; Opus streams are converted to AAC.
 */
@UnstableApi
object SongExporter {
    private const val FOLDER = "Ctrl Music"

    suspend fun exportToMusic(
        context: Context,
        downloadCache: SimpleCache,
        mediaId: String,
        title: String,
        artist: String,
        album: String?,
    ): Result<Uri> = runCatching {
        val workDir = File(context.cacheDir, "export").apply { mkdirs() }
        val source = File(workDir, "$mediaId.src")
        val output = File(workDir, "$mediaId.m4a")
        try {
            withContext(Dispatchers.IO) { copyFromCache(downloadCache, mediaId, source) }
            withContext(Dispatchers.Main) { convertToM4a(context, source, output) }
            withContext(Dispatchers.IO) {
                publish(context, output, fileName(artist, title), title, artist, album)
            }
        } finally {
            source.delete()
            output.delete()
        }
    }

    private fun copyFromCache(cache: SimpleCache, mediaId: String, target: File) {
        // No upstream: only bytes that are already downloaded can be read.
        val dataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(null)
            .setCacheWriteDataSinkFactory(null)
            .createDataSource()
        val spec = DataSpec.Builder()
            .setUri(Uri.parse("ctrlmusic://download/$mediaId"))
            .setKey(mediaId)
            .build()
        try {
            dataSource.open(spec)
            FileOutputStream(target).use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = dataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    out.write(buffer, 0, read)
                }
            }
        } finally {
            dataSource.close()
        }
        check(target.length() > 0) { "Song is not downloaded yet" }
    }

    private suspend fun convertToM4a(context: Context, source: File, output: File) =
        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (cont.isActive) cont.resumeWithException(exportException)
                    }
                })
                .build()
            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(source)))
                .setRemoveVideo(true)
                .build()
            transformer.start(item, output.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }

    private fun publish(
        context: Context,
        file: File,
        displayName: String,
        title: String,
        artist: String,
        album: String?,
    ): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$FOLDER")
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                album?.let { put(MediaStore.Audio.Media.ALBUM, it) }
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: error("Could not create file in Music")
            try {
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    ?: error("Could not write file")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            return uri
        }

        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        ) { "Allow storage access for Ctrl Music in Android settings, then try again" }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), FOLDER)
            .apply { mkdirs() }
        val target = uniqueFile(dir, displayName)
        file.copyTo(target)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("audio/mp4"), null)
        return Uri.fromFile(target)
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        var n = 2
        while (candidate.exists()) {
            candidate = File(dir, name.removeSuffix(".m4a") + " ($n).m4a")
            n++
        }
        return candidate
    }

    private fun fileName(artist: String, title: String): String {
        val raw = if (artist.isBlank()) title else "$artist - $title"
        val safe = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(120)
        return "${safe.ifBlank { "Song" }}.m4a"
    }
}
