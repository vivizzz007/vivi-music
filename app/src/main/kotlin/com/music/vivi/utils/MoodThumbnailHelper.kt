/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.music.innertube.YouTube
import com.music.innertube.pages.MoodAndGenres
import com.music.vivi.ui.utils.resize
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object MoodThumbnailHelper {
    private const val PREFS_NAME = "mood_thumbnails_cache"
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<String?>>()
    private val concurrencySemaphore = Semaphore(8)

    fun loadAllCached(context: Context): Map<String, String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val all = prefs.all
        for ((key, value) in all) {
            if (value is String && value.isNotEmpty()) {
                memoryCache[key] = value
            }
        }
        return HashMap(memoryCache)
    }

    fun getCached(context: Context, cacheKey: String): String? {
        memoryCache[cacheKey]?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(cacheKey, null)
        if (!cached.isNullOrEmpty()) {
            memoryCache[cacheKey] = cached
            return cached
        }
        return null
    }

    suspend fun fetchThumbnail(
        context: Context,
        browseId: String?,
        params: String?
    ): String? {
        if (browseId.isNullOrEmpty()) return null
        val cacheKey = if (!params.isNullOrEmpty()) "${browseId}_$params" else browseId

        getCached(context, cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
                async {
                    concurrencySemaphore.withPermit {
                        runCatching {
                            val browseResult = YouTube.browse(browseId, params).getOrNull()
                            val rawThumbnail = browseResult?.items
                                ?.firstOrNull { it.items.isNotEmpty() }
                                ?.items
                                ?.firstOrNull()
                                ?.thumbnail

                            if (!rawThumbnail.isNullOrEmpty()) {
                                val resized = rawThumbnail.resize(160, 160)
                                memoryCache[cacheKey] = resized

                                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                prefs.edit().putString(cacheKey, resized).apply()

                                preloadImage(context, resized)
                                resized
                            } else {
                                null
                            }
                        }.getOrNull()
                    }
                }
            }

            try {
                deferred.await()
            } finally {
                inFlightRequests.remove(cacheKey)
            }
        }
    }

    suspend fun preloadAll(
        context: Context,
        sections: List<MoodAndGenres>?,
        onThumbnailFetched: (key: String, url: String) -> Unit
    ) {
        if (sections.isNullOrEmpty()) return
        withContext(Dispatchers.IO) {
            val allItems = sections.flatMap { it.items }

            // 1. Instantly deliver already-cached items and pre-warm Coil memory
            for (item in allItems) {
                val key = if (!item.endpoint.params.isNullOrEmpty()) "${item.endpoint.browseId}_${item.endpoint.params}" else item.endpoint.browseId
                val cached = getCached(context, key)
                if (cached != null) {
                    preloadImage(context, cached)
                    onThumbnailFetched(key, cached)
                }
            }

            // 2. Concurrently fetch all uncached items in 8-worker parallel batches
            val uncachedItems = allItems.filter { item ->
                val key = if (!item.endpoint.params.isNullOrEmpty()) "${item.endpoint.browseId}_${item.endpoint.params}" else item.endpoint.browseId
                getCached(context, key) == null
            }

            uncachedItems.chunked(8).forEach { chunk ->
                chunk.map { item ->
                    async {
                        val key = if (!item.endpoint.params.isNullOrEmpty()) "${item.endpoint.browseId}_${item.endpoint.params}" else item.endpoint.browseId
                        val url = fetchThumbnail(context, item.endpoint.browseId, item.endpoint.params)
                        if (url != null) {
                            onThumbnailFetched(key, url)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private fun preloadImage(context: Context, url: String) {
        val request = ImageRequest.Builder(context.applicationContext)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        context.applicationContext.imageLoader.enqueue(request)
    }
}
