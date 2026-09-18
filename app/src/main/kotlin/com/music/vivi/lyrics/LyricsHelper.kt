/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import android.util.LruCache
import com.music.vivi.constants.LyricsProviderOrderKey
import com.music.vivi.constants.PreferredLyricsProvider
import com.music.vivi.constants.PreferredLyricsProviderKey
import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.extensions.toEnum
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.NetworkConnectivityObserver
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    /**
     * Resolves the ordered list of lyrics providers from the user's saved priority order.
     * Falls back to migrating the legacy [PreferredLyricsProvider] enum if the new order
     * preference has not been written yet, ensuring a smooth upgrade for existing users.
     */
    private suspend fun resolveLyricsProviders(): List<LyricsProvider> {
        val preferences = context.dataStore.data.first()
        val orderString = preferences[LyricsProviderOrderKey].orEmpty()

        if (orderString.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(orderString)
        }

        // Migration path: place the old preferred provider first in the default order
        val preferredEnum = preferences[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.MUSIXMATCH)
        val preferredName = LyricsProviderRegistry.getProviderNameForEnum(preferredEnum)
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val migratedOrder = listOf(preferredName) + defaultOrder.filter { it != preferredName }
        return migratedOrder.mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
    }



    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeFetches = mutableMapOf<String, Deferred<LyricsWithProvider>>()
    private val fetchesMutex = Mutex()

    private fun isSyncedLyrics(lyrics: String?): Boolean {
        if (lyrics.isNullOrBlank() || lyrics == LYRICS_NOT_FOUND) return false
        val trimmed = lyrics.trimStart()
        return trimmed.startsWith("[") || trimmed.contains(Regex("""\[\d{1,2}:\d{2}"""))
    }

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null && isSyncedLyrics(cached.lyrics)) {
            return LyricsWithProvider(cached.lyrics, cached.providerName)
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            if (cached != null) {
                return LyricsWithProvider(cached.lyrics, cached.providerName)
            }
            // Still proceed but return not found to avoid hanging
            return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val cacheKey = mediaMetadata.id
        val deferred = fetchesMutex.withLock {
            activeFetches.getOrPut(cacheKey) {
                helperScope.async {
                    val providers = resolveLyricsProviders().filter { it.isEnabled(context) }
                    if (providers.isEmpty()) {
                        return@async LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
                    }

                    coroutineScope {
                        val deferreds = providers.map { provider ->
                            provider to async {
                                runCatching {
                                    withTimeoutOrNull(3500L) {
                                        val result = provider.getLyrics(
                                            mediaMetadata.id,
                                            mediaMetadata.title,
                                            mediaMetadata.artists.joinToString { it.name },
                                            mediaMetadata.duration,
                                            mediaMetadata.album?.title,
                                        )
                                        result.getOrNull()
                                    }
                                }.getOrNull()
                            }
                        }

                        // Collect results from all concurrent providers in priority order
                        val results = arrayOfNulls<String>(deferreds.size)
                        deferreds.forEachIndexed { index, (_, def) ->
                            launch {
                                val lyrics = def.await()
                                if (!lyrics.isNullOrBlank() && lyrics != LYRICS_NOT_FOUND) {
                                    results[index] = lyrics
                                }
                            }
                        }

                        // Wait for completion or until a high-priority synced result is determined
                        val startTime = System.currentTimeMillis()
                        val maxWaitMs = 3500L

                        while (System.currentTimeMillis() - startTime < maxWaitMs) {
                            // 1. If provider 0 (highest priority) finished with synced lyrics, return immediately
                            val topLyrics = results[0]
                            if (topLyrics != null && isSyncedLyrics(topLyrics)) {
                                val res = LyricsWithProvider(topLyrics, deferreds[0].first.name)
                                cache.put(cacheKey, listOf(LyricsResult(res.provider, res.lyrics)))
                                return@coroutineScope res
                            }

                            // 2. Find highest priority synced lyrics available so far
                            val firstSyncedIndex = results.indexOfFirst { it != null && isSyncedLyrics(it) }
                            if (firstSyncedIndex != -1) {
                                // If all providers with higher priority than firstSyncedIndex finished,
                                // or if we gave higher-priority providers at least 1000ms, use this synced result!
                                val higherPriorityAllCompleted = (0 until firstSyncedIndex).all { deferreds[it].second.isCompleted }
                                val waitedEnough = System.currentTimeMillis() - startTime >= 1000L
                                if (higherPriorityAllCompleted || waitedEnough) {
                                    val syncedLyrics = results[firstSyncedIndex]!!
                                    val res = LyricsWithProvider(syncedLyrics, deferreds[firstSyncedIndex].first.name)
                                    cache.put(cacheKey, listOf(LyricsResult(res.provider, res.lyrics)))
                                    return@coroutineScope res
                                }
                            }

                            // 3. If all providers have finished, break early
                            if (deferreds.all { it.second.isCompleted }) {
                                break
                            }

                            delay(50)
                        }

                        // Check again for any synced lyrics in priority order
                        val anySyncedIndex = results.indexOfFirst { it != null && isSyncedLyrics(it) }
                        if (anySyncedIndex != -1) {
                            val syncedLyrics = results[anySyncedIndex]!!
                            val res = LyricsWithProvider(syncedLyrics, deferreds[anySyncedIndex].first.name)
                            cache.put(cacheKey, listOf(LyricsResult(res.provider, res.lyrics)))
                            return@coroutineScope res
                        }

                        // Fallback to highest-priority unsynced lyrics
                        val anyUnsyncedIndex = results.indexOfFirst { it != null }
                        if (anyUnsyncedIndex != -1) {
                            val unsyncedLyrics = results[anyUnsyncedIndex]!!
                            val res = LyricsWithProvider(unsyncedLyrics, deferreds[anyUnsyncedIndex].first.name)
                            cache.put(cacheKey, listOf(LyricsResult(res.provider, res.lyrics)))
                            return@coroutineScope res
                        }

                        if (cached != null) {
                            return@coroutineScope LyricsWithProvider(cached.lyrics, cached.providerName)
                        }

                        LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
                    }
                }
            }
        }

        return try {
            deferred.await()
        } finally {
            fetchesMutex.withLock {
                activeFetches.remove(cacheKey)
            }
        }
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }
        
        if (!isNetworkAvailable) {
            // Still try to proceed in case of false negative
            return
        }

        val allResult = java.util.Collections.synchronizedList(mutableListOf<LyricsResult>())
        val providers = resolveLyricsProviders().filter { it.isEnabled(context) }
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            coroutineScope {
                providers.forEach { provider ->
                    launch {
                        try {
                            provider.getAllLyrics(mediaId, songTitle, songArtists, duration, album) { lyrics ->
                                val result = LyricsResult(provider.name, lyrics)
                                allResult += result
                                callback(result)
                            }
                        } catch (e: Exception) {
                            // Catch network-related exceptions like UnresolvedAddressException
                            reportException(e)
                        }
                    }
                }
            }
            cache.put(cacheKey, allResult.toList())
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 150
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)