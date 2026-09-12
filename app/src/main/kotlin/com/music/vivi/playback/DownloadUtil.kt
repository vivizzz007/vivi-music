/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.database.DatabaseProvider
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.music.innertube.YouTube
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.AutoDownloadPlaylistsKey
import com.music.vivi.constants.IpVersionKey
import com.music.vivi.constants.SaveDownloadsToPublicFolderKey
import androidx.datastore.preferences.core.edit
import com.music.innertube.models.IpVersion
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import okhttp3.Dns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.FormatEntity
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.di.DownloadCache
import com.music.vivi.di.PlayerCache
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.YTPlayerUtils
import com.music.vivi.utils.enumPreference
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val ipVersion by enumPreference(context, IpVersionKey, IpVersion.AUTO)
    private val songUrlCache = StreamUrlCache()
    // Keep a reference to context so we can read DataStore prefs for JioSaavn support
    private val appContext: Context = context

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val streamHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(okhttp3.ConnectionPool(32, 5, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    val addresses = Dns.SYSTEM.lookup(hostname)
                    return when (this@DownloadUtil.ipVersion) {
                        IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                        IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                        IpVersion.AUTO -> addresses
                    }
                }
            })
            .proxy(YouTube.proxy)
            .proxyAuthenticator { _, response ->
                YouTube.proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .build()
    }

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setCacheWriteDataSinkFactory(null) // PREVENTS DEADLOCKS! Don't write to playerCache here; just read from it!
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(streamHttpClient)
                        .setUserAgent(com.music.innertube.models.YouTubeClient.USER_AGENT_WEB),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1
            Timber.tag("DownloadDiagnostics").d("dataSourceFactory triggered for mediaId=$mediaId, position=${dataSpec.position}, length=${dataSpec.length}")

            if (dataSpec.uri.scheme == "http" || dataSpec.uri.scheme == "https") {
                val cached = songUrlCache[mediaId]
                return@Factory if (cached != null) {
                    dataSpec.withUri(cached.url.toUri())
                        .withRequestHeaders(dataSpec.httpRequestHeaders + cached.requestHeaders)
                } else {
                    dataSpec
                }
            }

            val knownContentLength = database.getSongByIdBlocking(mediaId)?.format?.contentLength
                ?: ContentMetadata.getContentLength(playerCache.getContentMetadata(mediaId)).takeIf { it > 0L }
            if (knownContentLength != null && knownContentLength > 0L && playerCache.isCached(mediaId, 0, knownContentLength)) {
                Timber.tag("DownloadDiagnostics").d("Stream already 100% cached in playerCache for $mediaId ($knownContentLength bytes) - copying directly")
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.let { cachedStream ->
                Timber.tag("DownloadDiagnostics").d("Using cached stream URL from valid songUrlCache for $mediaId")
                return@Factory dataSpec.withUri(cachedStream.url.toUri())
                    .withRequestHeaders(dataSpec.httpRequestHeaders + cachedStream.requestHeaders)
            }
            Timber.tag("DownloadDiagnostics").w("No valid cached URL found for $mediaId. Triggering heavy network playback resolution!")
            val cacheGeneration = songUrlCache.generation(mediaId)

            val playbackData = runBlocking(Dispatchers.IO) {
                val song = database.song(mediaId).firstOrNull()?.song
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    // Pass context so the JioSaavn intercept fires when the toggle is ON
                    context = appContext,
                    contentHints = com.music.innertube.strategy.ContentHints(
                        isExplicit = song?.explicit,
                        isUploaded = song?.isUploaded,
                    ),
                    allowBoundedRange = true,
                    preferM4a = true,
                )
            }.getOrThrow()
            val format = playbackData.format

            val actualContentLength =
                format.contentLength?.takeIf { it > 0L } ?: run {
                    val request = okhttp3.Request.Builder()
                        .get()
                        .url(playbackData.streamUrl)
                        .header("Range", "bytes=0-0")
                        .apply {
                            playbackData.streamHeaders.forEach { (name, value) ->
                                header(name, value)
                            }
                        }
                        .build()
                    try {
                        streamHttpClient.newCall(request).execute().use { response ->
                            downloadContentLength(
                                statusCode = response.code,
                                contentRange = response.header("Content-Range"),
                                contentLength = response.header("Content-Length"),
                            )
                        }
                    } catch (_: java.io.IOException) {
                        null
                    }
                } ?: 0L

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"") ?: "mp4a.40.2",
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = actualContentLength,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                // Metadata registration only — dateDownload is intentionally NOT set here.
                // It belongs solely to onDownloadChanged()'s STATE_COMPLETED branch below,
                // which only fires once the download has actually finished.
                val existing = getSongByIdBlocking(mediaId)?.song
                val fallbackTitle = runCatching {
                    downloadManager.downloadIndex.getDownload(mediaId)?.request?.data?.let { String(it) }
                }.getOrNull()?.takeIf { it.isNotBlank() }

                val resolvedDuration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0
                val updatedSong = if (existing != null) {
                    existing.copy(
                        duration = if (existing.duration > 0) existing.duration else resolvedDuration,
                        thumbnailUrl = existing.thumbnailUrl ?: playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url?.resize(1200, 1200),
                    )
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: fallbackTitle ?: "Unknown",
                        duration = resolvedDuration,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url?.resize(1200, 1200),
                        dateDownload = null,
                        isDownloaded = false
                    )
                }

                upsert(updatedSong)

                // Pre-cache standard thumbnail resolutions immediately when download starts
                updatedSong.thumbnailUrl?.let { url ->
                    val imageLoader = SingletonImageLoader.get(context)
                    listOfNotNull(
                        url,
                        url.resize(120, 120),
                        url.resize(544, 544),
                        url.resize(1200, 1200)
                    ).distinct().forEach { targetUrl ->
                        val request = ImageRequest.Builder(context)
                            .data(targetUrl)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }
                }
            }

            val streamUrl = playbackData.streamUrl

            songUrlCache.put(
                mediaId = mediaId,
                url = streamUrl,
                requestHeaders = playbackData.streamHeaders,
                clientName = playbackData.streamClient,
                expiresInSeconds = playbackData.streamExpiresInSeconds,
                requireBoundedRange = false,
                rangeChunkSizeBytes = 0L,
                useRangeChunks = false,
                expectedGeneration = cacheGeneration,
            )
            dataSpec.withUri(streamUrl.toUri())
                .withRequestHeaders(dataSpec.httpRequestHeaders + playbackData.streamHeaders)
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    private var progressPollingJob: kotlinx.coroutines.Job? = null

    private fun startProgressPollingIfNeeded() {
        if (progressPollingJob?.isActive == true) return
        progressPollingJob = scope.launch {
            while (isActive) {
                val current = downloadManager.currentDownloads
                if (current.isEmpty()) {
                    break
                }
                downloads.update { map ->
                    val updated = map.toMutableMap()
                    current.forEach { updated[it.request.id] = it }
                    updated
                }
                kotlinx.coroutines.delay(500)
            }
            progressPollingJob = null
        }
    }

    fun shouldDownloadSong(songId: String): Boolean {
        val download = downloads.value[songId]
        if (download != null) {
            when (download.state) {
                Download.STATE_DOWNLOADING,
                Download.STATE_QUEUED,
                Download.STATE_RESTARTING -> return false
                Download.STATE_FAILED,
                Download.STATE_STOPPED -> return true
                Download.STATE_COMPLETED -> {
                    val cacheKey = if (downloadCache.keys.contains(songId)) songId else songId.toUri().toString()
                    val cachedBytes = downloadCache.getCachedBytes(cacheKey, 0, 100_000_000L)
                    if (cachedBytes in 1L..1_150_000L) {
                        return true
                    }
                    val song = database.getSongByIdBlocking(songId)?.song
                    if (song != null && song.duration <= 0) {
                        return true
                    }
                    return false
                }
                else -> return true
            }
        }
        val song = database.getSongByIdBlocking(songId)?.song
        if (song != null && (song.isDownloaded || song.dateDownload != null)) {
            if (song.duration <= 0) return true
            val cacheKey = if (downloadCache.keys.contains(songId)) songId else songId.toUri().toString()
            val cachedBytes = downloadCache.getCachedBytes(cacheKey, 0, 100_000_000L)
            if (cachedBytes in 1L..1_150_000L || cachedBytes == 0L) return true
            return false
        }
        return true
    }

    fun reDownloadSong(songId: String, title: String) {
        scope.launch(Dispatchers.IO) {
            database.updateDownloadedInfo(songId, false, null)
            songUrlCache.invalidate(songId)
            DownloadService.sendRemoveDownload(
                appContext,
                ExoDownloadService::class.java,
                songId,
                false
            )
            kotlinx.coroutines.delay(200)
            val downloadRequest = DownloadRequest.Builder(songId, songId.toUri())
                .setCustomCacheKey(songId)
                .setData(title.toByteArray())
                .build()
            DownloadService.sendAddDownload(
                appContext,
                ExoDownloadService::class.java,
                downloadRequest,
                false
            )
        }
    }

    fun download(songId: String, title: String) {
        scope.launch {
            if (!shouldDownloadSong(songId)) return@launch
            val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                .Builder(songId, songId.toUri())
                .setCustomCacheKey(songId)
                .setData(title.toByteArray())
                .build()
            androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                appContext,
                ExoDownloadService::class.java,
                downloadRequest,
                false
            )
        }
    }

    fun downloadSongs(songs: List<Pair<String, String>>) {
        scope.launch {
            songs.forEach { (songId, title) ->
                if (shouldDownloadSong(songId)) {
                    val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                        .Builder(songId, songId.toUri())
                        .setCustomCacheKey(songId)
                        .setData(title.toByteArray())
                        .build()
                    androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                        appContext,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false
                    )
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            java.util.concurrent.Executors.newFixedThreadPool(6)
        ).apply {
            maxParallelDownloads = 4
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        Timber.tag("DownloadDiagnostics").d(
                            "onDownloadChanged [%s]: state = %s, progress = %.2f%%, exception = %s",
                            download.request.id,
                            downloadStateToString(download.state),
                            download.percentDownloaded,
                            finalException?.message ?: "None"
                        )
                        
                        if (download.state == Download.STATE_FAILED) {
                            songUrlCache.invalidate(download.request.id)
                        }

                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED) {
                            startProgressPollingIfNeeded()
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    val songId = download.request.id
                                    database.updateDownloadedInfo(songId, true, LocalDateTime.now())

                                    val saveToPublic = appContext.dataStore[SaveDownloadsToPublicFolderKey] ?: false
                                    if (saveToPublic || pendingExternalExportSongIds.remove(songId)) {
                                        exportSongToPublicStorage(songId)
                                    }
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        val downloadId = download.request.id
                        Timber.tag("DownloadDiagnostics").d("onDownloadRemoved [%s]: App requested download removal", downloadId)
                        songUrlCache.invalidate(downloadId)

                        scope.launch {
                            runCatching {
                                database.updateDownloadedInfo(downloadId, false, null)
                            }.onSuccess {
                                downloads.update { map ->
                                    map.toMutableMap().apply {
                                        remove(downloadId)
                                    }
                                }
                                Timber.tag("DownloadUtil").d("Successfully removed download $downloadId from in-memory map")
                            }.onFailure { error ->
                                Timber.tag("DownloadUtil").e(error, "Failed to update database for removed download $downloadId, keeping in-memory entry")
                            }
                        }
                    }
                }
            )
        }

    val pendingExternalExportSongIds: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
        if (result.values.any { it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED }) {
            startProgressPollingIfNeeded()
        }
        repairIncompleteDownloads()
    }

    fun repairIncompleteDownloads() {
        scope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1000)
                val downloadedSongs = database.downloadedSongsByNameAsc().firstOrNull() ?: emptyList()
                if (downloadedSongs.isEmpty()) return@launch

                Timber.tag("DownloadUtil").i("Scanning ${downloadedSongs.size} downloaded songs for repair...")
                for (songWithData in downloadedSongs) {
                    val song = songWithData.song
                    val songId = song.id
                    val download = downloads.value[songId]
                    if (download != null && (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED || download.state == Download.STATE_RESTARTING)) {
                        continue
                    }

                    val format = songWithData.format ?: database.format(songId).firstOrNull()
                    val cacheKey = when {
                        downloadCache.keys.contains(songId) -> songId
                        downloadCache.keys.contains(songId.toUri().toString()) -> songId.toUri().toString()
                        else -> downloadCache.keys.firstOrNull { it.contains(songId) } ?: songId
                    }
                    val cachedBytes = downloadCache.getCachedBytes(cacheKey, 0, 100_000_000L)
                    val expectedLength = format?.contentLength ?: 0L

                    val isTruncated = (cachedBytes in 1L..1_150_000L && (song.duration > 30 || (format?.bitrate ?: 0) > 0)) ||
                            (expectedLength > 1_500_000L && cachedBytes < (expectedLength * 0.9).toLong()) ||
                            cachedBytes == 0L ||
                            song.duration <= 0

                    if (isTruncated) {
                        Timber.tag("DownloadUtil").w("Repairing corrupt/truncated song: \"${song.title}\" ($songId) - cached=$cachedBytes bytes, expected=$expectedLength, duration=${song.duration}")
                        reDownloadSong(songId, song.title)
                    }
                }
            } catch (e: Exception) {
                Timber.tag("DownloadUtil").e(e, "Error repairing incomplete downloads")
            }
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun exportSongToPublicStorage(songId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val songWithData = database.song(songId).firstOrNull() ?: return@launch
                val song = songWithData.song
                val songTitle = song.title
                val artistName = songWithData.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
                val albumName = songWithData.album?.title
                val format = songWithData.format ?: database.format(songId).firstOrNull()
                val rawMimeType = format?.mimeType ?: "audio/mp4"
                val isOpusOrWebm = rawMimeType.contains("webm", ignoreCase = true) || rawMimeType.contains("opus", ignoreCase = true)

                val safeTitle = songTitle
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    .trim()
                    .ifBlank { songId }

                val cacheKey = when {
                    downloadCache.keys.contains(songId) -> songId
                    downloadCache.keys.contains(songId.toUri().toString()) -> songId.toUri().toString()
                    else -> downloadCache.keys.firstOrNull { it.contains(songId) } ?: songId
                }

                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(downloadCache)
                    .setUpstreamDataSourceFactory(null)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()

                val dataSpec = DataSpec.Builder()
                    .setUri(cacheKey.toUri())
                    .setKey(cacheKey)
                    .build()

                var rawAudio = ByteArray(0)
                for (retry in 0..2) {
                    val audioOut = ByteArrayOutputStream()
                    try {
                        cacheDataSource.open(dataSpec)
                        val buffer = ByteArray(32768)
                        while (true) {
                            val bytes = cacheDataSource.read(buffer, 0, buffer.size)
                            if (bytes <= 0 || bytes == C.RESULT_END_OF_INPUT) break
                            audioOut.write(buffer, 0, bytes)
                        }
                        rawAudio = audioOut.toByteArray()
                        if (rawAudio.isNotEmpty()) break
                    } catch (e: Exception) {
                        Timber.tag("DownloadUtil").w(e, "Retry $retry reading cache for $songId")
                    } finally {
                        try { cacheDataSource.close() } catch (_: Exception) {}
                    }
                    if (rawAudio.isEmpty()) {
                        kotlinx.coroutines.delay(200)
                    }
                }

                if (rawAudio.isEmpty()) return@launch

                val expectedLength = format?.contentLength ?: 0L
                val isTruncated = (expectedLength > 1_500_000L && rawAudio.size < (expectedLength * 0.9).toLong()) ||
                        (rawAudio.size in 1..1_150_000 && (song.duration > 30 || (format?.bitrate ?: 0) > 0))
                if (isTruncated) {
                    Timber.tag("DownloadUtil").w("Cache for $songId is truncated (${rawAudio.size} bytes). Triggering fresh download before export.")
                    pendingExternalExportSongIds.add(songId)
                    reDownloadSong(songId, songTitle)
                    return@launch
                }

                val isMp4 = AudioTagEmbedder.isMp4Header(rawAudio) || !isOpusOrWebm
                val fileName = if (isMp4) "${safeTitle}.m4a" else "${safeTitle}.opus"
                val mimeType = if (isMp4) "audio/mp4" else "audio/opus"

                val artworkBytes: ByteArray? = try {
                    song.thumbnailUrl?.let { thumbUrl ->
                        val fullUrl = thumbUrl.resize(1200, 1200)
                        java.net.URL(fullUrl).openStream().use { it.readBytes() }
                    }
                } catch (e: Exception) {
                    Timber.tag("DownloadUtil").w(e, "Failed to download thumbnail bytes for $songId")
                    null
                }

                val lyricsText: String? = try {
                    val dbLyrics = database.lyrics(songId).firstOrNull()
                    if (dbLyrics != null && dbLyrics.lyrics.isNotBlank() && dbLyrics.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                        dbLyrics.lyrics
                    } else null
                } catch (e: Exception) {
                    null
                }

                val finalAudio = AudioTagEmbedder.embedMetadata(
                    audioBytes = rawAudio,
                    isM4a = isMp4,
                    artworkBytes = artworkBytes,
                    lyrics = lyricsText,
                    title = songTitle,
                    artist = artistName,
                    album = albumName
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = appContext.contentResolver
                    val relativePath = "${Environment.DIRECTORY_MUSIC}/ViviMusic"

                    val projection = arrayOf(MediaStore.Audio.Media._ID)
                    val baseSelection = "(${MediaStore.Audio.Media.DISPLAY_NAME} = ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} = ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} = ?) AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                    val baseArgs = arrayOf(fileName, safeTitle, "${safeTitle}.oga", "%ViviMusic%")
                    var existingUri: Uri? = null

                    resolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        baseSelection,
                        baseArgs,
                        null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            if (existingUri == null) {
                                existingUri = uri
                            } else {
                                runCatching { resolver.delete(uri, null, null) }
                            }
                        }
                    }

                    val uri = existingUri ?: run {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Audio.Media.TITLE, songTitle)
                            put(MediaStore.Audio.Media.ARTIST, artistName)
                            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                        resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    }

                    if (uri != null) {
                        val updateValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                        runCatching { resolver.update(uri, updateValues, null, null) }

                        resolver.openOutputStream(uri, "wt")?.use { out ->
                            out.write(finalAudio)
                            out.flush()
                        }
                        val finishValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, finishValues, null, null)
                        Timber.tag("DownloadUtil").d("Exported $fileName to MediaStore ($uri)")
                    }
                } else {
                    val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "ViviMusic")
                    musicDir.mkdirs()
                    File(musicDir, "${safeTitle}.oga").takeIf { it.exists() }?.delete()
                    File(musicDir, safeTitle).takeIf { it.exists() && it.name != fileName }?.delete()

                    val targetFile = File(musicDir, fileName)
                    FileOutputStream(targetFile).use { out ->
                        out.write(finalAudio)
                        out.flush()
                    }
                    MediaScannerConnection.scanFile(appContext, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
                    Timber.tag("DownloadUtil").d("Exported $fileName to public storage: ${targetFile.absolutePath}")
                }
            } catch (e: Exception) {
                Timber.tag("DownloadUtil").e(e, "Failed to export song $songId to public storage")
            }
        }
    }

    fun exportAllDownloadedSongs() {
        scope.launch(Dispatchers.IO) {
            val downloadedSongs = database.downloadedSongsByNameAsc().firstOrNull() ?: emptyList()
            for (song in downloadedSongs) {
                exportSongToPublicStorage(song.id)
            }
        }
    }

    fun exportPlaylistToPublicStorage(songIds: List<String>) {
        exportAlbumToPublicStorage(songIds)
    }

    fun exportAlbumToPublicStorage(songIds: List<String>) {
        scope.launch(Dispatchers.IO) {
            songIds.forEach { songId ->
                val isDownloaded = downloads.value[songId]?.state == Download.STATE_COMPLETED ||
                        database.song(songId).firstOrNull()?.song?.let { it.dateDownload != null || it.isDownloaded } == true
                if (isDownloaded) {
                    exportSongToPublicStorage(songId)
                } else {
                    pendingExternalExportSongIds.add(songId)
                    val song = database.song(songId).firstOrNull()?.song
                    val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                        .Builder(songId, songId.toUri())
                        .setCustomCacheKey(songId)
                        .setData(song?.title?.toByteArray() ?: songId.toByteArray())
                        .build()
                    androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                        appContext,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false
                    )
                }
            }
        }
    }

    fun autoDownloadIfPlaylistDownloaded(playlistId: String, songIds: List<String>) {
        scope.launch(Dispatchers.IO) {
            val autoDownloadPlaylists = appContext.dataStore.data.firstOrNull()?.get(AutoDownloadPlaylistsKey)?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            val isExplicitAutoDownload = playlistId in autoDownloadPlaylists
            
            val isPlaylistDownloaded = if (!isExplicitAutoDownload) {
                val existingSongs = database.playlistSongs(playlistId).firstOrNull() ?: emptyList()
                if (existingSongs.isNotEmpty()) {
                    val downloadedCount = existingSongs.count { 
                        downloads.value[it.song.id]?.state == Download.STATE_COMPLETED || it.song.song.dateDownload != null || it.song.song.isDownloaded 
                    }
                    downloadedCount >= existingSongs.size - 1
                } else false
            } else true

            if (isExplicitAutoDownload || isPlaylistDownloaded) {
                appContext.dataStore.edit { prefs ->
                    val current = prefs[AutoDownloadPlaylistsKey]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
                    prefs[AutoDownloadPlaylistsKey] = (current + playlistId).joinToString(",")
                }
                songIds.forEach { songId ->
                    if (!shouldDownloadSong(songId)) return@forEach
                    val song = database.song(songId).firstOrNull()?.song
                    val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                        .Builder(songId, songId.toUri())
                        .setCustomCacheKey(songId)
                        .setData(song?.title?.toByteArray() ?: songId.toByteArray())
                        .build()
                    androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                        appContext,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false
                    )
                }
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun Throwable?.isExpiredStreamError(): Boolean {
        var current = this
        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException &&
                (current.responseCode == 403 || current.responseCode == 410)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

private fun downloadStateToString(state: Int): String {
    return when (state) {
        Download.STATE_QUEUED -> "QUEUED"
        Download.STATE_STOPPED -> "STOPPED"
        Download.STATE_DOWNLOADING -> "DOWNLOADING"
        Download.STATE_COMPLETED -> "COMPLETED"
        Download.STATE_FAILED -> "FAILED"
        Download.STATE_REMOVING -> "REMOVING"
        Download.STATE_RESTARTING -> "RESTARTING"
        else -> "UNKNOWN ($state)"
    }
}

internal fun downloadContentLength(
    statusCode: Int,
    contentRange: String?,
    contentLength: String?,
): Long? {
    val rangePattern =
        when (statusCode) {
            206 -> PARTIAL_CONTENT_RANGE
            416 -> UNSATISFIED_CONTENT_RANGE
            else -> null
        }
    if (rangePattern != null) {
        return contentRange
            ?.trim()
            ?.let(rangePattern::matchEntire)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
    }
    return if (statusCode == 200) contentLength?.toLongOrNull()?.takeIf { it > 0L } else null
}

private val PARTIAL_CONTENT_RANGE = Regex("""bytes\s+0-0/(\d+)""", RegexOption.IGNORE_CASE)
private val UNSATISFIED_CONTENT_RANGE = Regex("""bytes\s+\*/(\d+)""", RegexOption.IGNORE_CASE)

