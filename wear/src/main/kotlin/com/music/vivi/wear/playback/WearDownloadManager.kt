/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.music.vivi.db.MusicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.Executor

/**
 * Coordinates offline song and playlist caching on Wear OS.
 *
 * Interacts with Media3 [DownloadManager] and synchronizes download completion/removal
 * with the Room database [MusicDatabase] from :core-database.
 */
class WearDownloadManager(
    private val context: Context,
    private val database: MusicDatabase,
    private val databaseProvider: DatabaseProvider,
    private val downloadCache: SimpleCache,
    upstreamDataSourceFactory: DataSource.Factory,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads: StateFlow<Map<String, Download>> = _downloads.asStateFlow()

    private val resolvingDataSourceFactory: DataSource.Factory = ResolvingDataSource.Factory(upstreamDataSourceFactory) { dataSpec ->
        val uri = dataSpec.uri
        if (uri.scheme == "http" || uri.scheme == "https") {
            return@Factory dataSpec
        }

        val songId = dataSpec.key ?: uri.lastPathSegment ?: uri.toString()
        val resolvedUrl = runBlocking(Dispatchers.IO) {
            WearStreamResolver.resolveStreamUrl(songId)
        } ?: throw IllegalStateException("Failed to resolve stream URL for $songId during download")

        dataSpec.withUri(resolvedUrl.toUri())
    }

    val media3DownloadManager: DownloadManager = DownloadManager(
        context,
        databaseProvider,
        downloadCache,
        resolvingDataSourceFactory,
        Executor(Runnable::run),
    ).apply {
        maxParallelDownloads = 2 // Optimized for watch thermal/battery constraints
        addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?,
            ) {
                _downloads.update { map ->
                    map + (download.request.id to download)
                }

                coroutineScope.launch {
                    when (download.state) {
                        Download.STATE_COMPLETED -> {
                            Timber.i("Download completed for track: %s", download.request.id)
                            database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                        }
                        Download.STATE_FAILED,
                        Download.STATE_REMOVING,
                        Download.STATE_STOPPED -> {
                            Timber.w("Download state changed to %d for %s (error=%s)", download.state, download.request.id, finalException?.message)
                            database.updateDownloadedInfo(download.request.id, false, null)
                        }
                        else -> {}
                    }
                }
            }

            override fun onDownloadRemoved(
                downloadManager: DownloadManager,
                download: Download,
            ) {
                val songId = download.request.id
                Timber.i("Download removed for track: %s", songId)
                _downloads.update { map -> map - songId }
                coroutineScope.launch {
                    database.updateDownloadedInfo(songId, false, null)
                }
            }
        })
    }

    init {
        // Hydrate initial download state from Media3 persistent download index
        val initialMap = mutableMapOf<String, Download>()
        try {
            val cursor = media3DownloadManager.downloadIndex.getDownloads()
            while (cursor.moveToNext()) {
                initialMap[cursor.download.request.id] = cursor.download
            }
            cursor.close()
        } catch (e: Exception) {
            Timber.e(e, "Error loading initial downloads from index")
        }
        _downloads.value = initialMap
    }

    /**
     * Queues a single song for background offline download.
     */
    fun downloadSong(songId: String, title: String, uri: Uri? = null) {
        val downloadUri = uri ?: "vivi://song/$songId".toUri()
        val request = DownloadRequest.Builder(songId, downloadUri)
            .setCustomCacheKey(songId)
            .setData(title.toByteArray(Charsets.UTF_8))
            .build()

        DownloadService.sendAddDownload(
            context,
            WearDownloadService::class.java,
            request,
            false,
        )
        Timber.i("Dispatched download request for song: %s (%s)", title, songId)
    }

    /**
     * Queues all tracks within a playlist for offline download.
     */
    suspend fun downloadPlaylist(playlistId: String) {
        val playlistSongs = database.playlistSongs(playlistId).firstOrNull().orEmpty()
        Timber.i("Queuing playlist %s (%d tracks) for offline caching", playlistId, playlistSongs.size)
        for (item in playlistSongs) {
            downloadSong(item.song.id, item.song.title)
        }
    }

    /**
     * Removes an offline downloaded song from watch storage.
     */
    fun removeDownload(songId: String) {
        DownloadService.sendRemoveDownload(
            context,
            WearDownloadService::class.java,
            songId,
            false,
        )
        Timber.i("Dispatched remove download request for song: %s", songId)
    }

    /**
     * Cancels all current and pending downloads.
     */
    fun cancelAllDownloads() {
        media3DownloadManager.currentDownloads.forEach { download ->
            removeDownload(download.request.id)
        }
    }

    /**
     * Returns a Flow observing the download status of a specific song.
     */
    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    /**
     * Returns whether the given song is fully downloaded and available for offline playback.
     */
    fun isDownloaded(songId: String): Boolean {
        val download = _downloads.value[songId]
        return (download != null && download.state == Download.STATE_COMPLETED) ||
            downloadCache.isCached(songId, 0, 1) ||
            downloadCache.keys.contains(songId)
    }

    /**
     * Returns whether the given song is currently queued or downloading.
     */
    fun isDownloading(songId: String): Boolean {
        val download = _downloads.value[songId] ?: return false
        return download.state == Download.STATE_DOWNLOADING ||
            download.state == Download.STATE_QUEUED ||
            download.state == Download.STATE_RESTARTING
    }

    /**
     * Releases background coroutine scopes.
     */
    fun release() {
        coroutineScope.cancel()
    }
}
