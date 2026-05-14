/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache

/**
 * Application-wide singleton cache for Canvas / motion-artwork video streams.
 *
 * Kept as a plain object (not Hilt-injected) because [CanvasArtworkPlayer] is a
 * standalone `@Composable` that lives outside MusicService.
 *
 * Default capacity: 256 MB, evicted with LRU.
 * Storage: `<filesDir>/canvas_cache`
 *
 * Call [reinitialize] after saving a new size preference so the evictor is
 * updated; the next recomposition of [CanvasArtworkPlayer] will pick up the
 * fresh instance via [get].
 */
object CanvasVideoCache {

    const val DEFAULT_MAX_MB = 256

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context, maxMb: Int = DEFAULT_MAX_MB): SimpleCache {
        return instance ?: synchronized(this) {
            instance ?: build(context, maxMb).also { instance = it }
        }
    }

    /**
     * Release the current instance and create a new one with [maxMb] capacity.
     * Pass [maxMb] = 0 to disable caching (uses [NoOpCacheEvictor]).
     * The next call to [get] will return the new instance.
     */
    fun reinitialize(context: Context, maxMb: Int) {
        synchronized(this) {
            instance?.release()
            instance = build(context, maxMb)
        }
    }

    /** Fully release the cache without rebuilding. Useful only in tests. */
    fun release() {
        synchronized(this) {
            instance?.release()
            instance = null
        }
    }

    private fun build(context: Context, maxMb: Int): SimpleCache = SimpleCache(
        context.applicationContext.filesDir.resolve("canvas_cache"),
        if (maxMb <= 0) NoOpCacheEvictor()
        else LeastRecentlyUsedCacheEvictor(maxMb * 1024 * 1024L),
        StandaloneDatabaseProvider(context.applicationContext),
    )
}
