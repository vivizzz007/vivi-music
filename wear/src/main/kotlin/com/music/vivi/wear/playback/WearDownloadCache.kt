/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.music.innertube.YouTube
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages offline and streaming media caches for Wear OS using Media3 [SimpleCache].
 *
 * Implements a 2-tier layered caching hierarchy:
 * - Layer 1 (Offline Downloads): Pinned in `filesDir/downloads` with [NoOpCacheEvictor],
 *   ensuring user-downloaded songs and playlists are never evicted automatically.
 * - Layer 2 (Playback Stream Cache): Fast LRU cache in `filesDir/player_cache` (max 256MB)
 *   to cache recently streamed online tracks without exhausting watch flash storage.
 */
object WearDownloadCache {

    /**
     * 256 MB maximum size for the transient online playback cache on watch storage.
     */
    const val PLAYER_CACHE_MAX_BYTES = 256 * 1024 * 1024L

    const val DOWNLOAD_DIR_NAME = "downloads"
    const val PLAYER_CACHE_DIR_NAME = "player_cache"

    @Volatile
    private var _downloadCache: SimpleCache? = null

    @Volatile
    private var _playerCache: SimpleCache? = null

    @Volatile
    private var _databaseProvider: DatabaseProvider? = null

    val downloadCache: SimpleCache
        get() = _downloadCache
            ?: throw IllegalStateException("WearDownloadCache.downloadCache is not initialized. Call initialize() first.")

    val playerCache: SimpleCache
        get() = _playerCache
            ?: throw IllegalStateException("WearDownloadCache.playerCache is not initialized. Call initialize() first.")

    val databaseProvider: DatabaseProvider
        get() = _databaseProvider
            ?: throw IllegalStateException("WearDownloadCache.databaseProvider is not initialized. Call initialize() first.")

    val isInitialized: Boolean
        get() = _downloadCache != null && _playerCache != null

    /**
     * Initializes the singleton SimpleCache instances for offline downloads and playback caching.
     */
    @Synchronized
    fun initialize(
        context: Context,
        databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context),
    ) {
        if (isInitialized) {
            Timber.d("WearDownloadCache is already initialized")
            return
        }

        _databaseProvider = databaseProvider

        val downloadDir = getDownloadCacheDir(context)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        _downloadCache = SimpleCache(
            downloadDir,
            NoOpCacheEvictor(),
            databaseProvider,
        )

        val playerCacheDir = getPlayerCacheDir(context)
        if (!playerCacheDir.exists()) {
            playerCacheDir.mkdirs()
        }
        _playerCache = SimpleCache(
            playerCacheDir,
            LeastRecentlyUsedCacheEvictor(PLAYER_CACHE_MAX_BYTES),
            databaseProvider,
        )

        Timber.i("WearDownloadCache initialized successfully: downloads=%s, playerCache=%s", downloadDir.path, playerCacheDir.path)
    }

    /**
     * Returns the directory on internal watch storage where offline downloaded songs are stored.
     */
    fun getDownloadCacheDir(context: Context): File = File(context.filesDir, DOWNLOAD_DIR_NAME)

    /**
     * Returns the directory on internal watch storage where transient streamed audio segments are cached.
     */
    fun getPlayerCacheDir(context: Context): File = File(context.filesDir, PLAYER_CACHE_DIR_NAME)

    /**
     * Creates an [OkHttpClient] configured for media streaming on Wear OS.
     */
    fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
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

    /**
     * Builds a layered [DataSource.Factory] that prioritizes the offline download cache,
     * falls back to the playback LRU cache, and finally connects to the network upstream.
     */
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val okHttpClient = createOkHttpClient()
        val upstreamFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient))

        // Layer 2: Player Cache (transient LRU)
        val playerCacheFactory = CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Layer 1: Download Cache (pinned offline storage)
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(playerCacheFactory)
            .setCacheWriteDataSinkFactory(null) // Reads from downloads, does not write unmanaged playback writes to it
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Returns a [DataSource.Factory] that only reads from offline downloads and never touches the network.
     */
    fun getDownloadOnlyDataSourceFactory(): DataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Checks whether a song with [songId] has been fully or partially cached in the offline download storage.
     */
    fun isDownloaded(songId: String): Boolean {
        if (_downloadCache == null) return false
        return downloadCache.keys.contains(songId) || downloadCache.isCached(songId, 0, 1)
    }

    /**
     * Checks whether a song with [songId] is cached in the transient player cache.
     */
    fun isPlayerCached(songId: String): Boolean {
        if (_playerCache == null) return false
        return playerCache.keys.contains(songId) || playerCache.isCached(songId, 0, 1)
    }

    /**
     * Returns the total storage space used by offline downloads in bytes.
     */
    fun getUsedDownloadStorageBytes(): Long = _downloadCache?.cacheSpace ?: 0L

    /**
     * Returns the total storage space used by the transient playback cache in bytes.
     */
    fun getUsedPlayerStorageBytes(): Long = _playerCache?.cacheSpace ?: 0L

    /**
     * Releases active cache resources. Primarily used for testing or app teardown.
     */
    @Synchronized
    fun release() {
        try {
            _downloadCache?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing downloadCache")
        } finally {
            _downloadCache = null
        }

        try {
            _playerCache?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing playerCache")
        } finally {
            _playerCache = null
        }

        _databaseProvider = null
    }
}
