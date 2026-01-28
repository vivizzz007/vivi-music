package com.music.vivi.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Manages video caching for faster playback and reduced network usage.
 * Uses ExoPlayer's SimpleCache with LRU eviction policy.
 */
object VideoCacheManager {
    private const val CACHE_DIR_NAME = "video_cache"
    private const val MAX_CACHE_SIZE = 100L * 1024 * 1024 // 100MB

    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    /**
     * Initialize the video cache. Should be called once during app startup.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (cache != null) return

        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
        val databaseProvider = StandaloneDatabaseProvider(context)

        cache = SimpleCache(cacheDir, evictor, databaseProvider)

        // Create cache data source factory
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("vivi-music")
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)

        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache!!)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // Use default
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Get the cache data source factory for creating cached data sources.
     */
    fun getCacheDataSourceFactory(): DataSource.Factory {
        return cacheDataSourceFactory
            ?: throw IllegalStateException("VideoCacheManager not initialized")
    }

    /**
     * Get the cache instance.
     */
    fun getCache(): SimpleCache {
        return cache ?: throw IllegalStateException("VideoCacheManager not initialized")
    }

    /**
     * Clear all cached video data.
     */
    fun clearCache() {
        cache?.let { cache ->
            try {
                val keys = cache.keys
                for (key in keys) {
                    cache.removeResource(key)
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to clear video cache")
            }
        }
    }

    /**
     * Get current cache size in bytes.
     */
    fun getCacheSize(): Long {
        return cache?.cacheSpace ?: 0L
    }

    /**
     * Release cache resources. Should be called when app is destroyed.
     */
    @Synchronized
    fun release() {
        try {
            cache?.release()
            cache = null
            cacheDataSourceFactory = null
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to release video cache")
        }
    }
}
