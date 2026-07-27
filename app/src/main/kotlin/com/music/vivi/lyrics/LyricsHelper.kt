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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import com.music.vivi.constants.UseWordSyncPriorityFetchKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * Per-provider failure tracking so a provider that's currently rate-limited
     * or down doesn't get retried on every single song lookup. After
     * [FAILURE_THRESHOLD] consecutive failures, a provider "sits out" for
     * [COOLDOWN_MS] before it's tried again. A single success resets its count.
     */
    private data class ProviderHealth(var consecutiveFailures: Int = 0, var cooldownUntil: Long = 0L)
    private val providerHealth = java.util.concurrent.ConcurrentHashMap<String, ProviderHealth>()

    private fun isProviderCoolingDown(providerName: String): Boolean {
        val health = providerHealth[providerName] ?: return false
        return System.currentTimeMillis() < health.cooldownUntil
    }

    private fun recordProviderSuccess(providerName: String) {
        providerHealth.getOrPut(providerName) { ProviderHealth() }.consecutiveFailures = 0
    }

    private fun recordProviderFailure(providerName: String) {
        val health = providerHealth.getOrPut(providerName) { ProviderHealth() }
        synchronized(health) {
            health.consecutiveFailures++
            if (health.consecutiveFailures >= FAILURE_THRESHOLD) {
                health.cooldownUntil = System.currentTimeMillis() + COOLDOWN_MS
                health.consecutiveFailures = 0
            }
        }
    }


    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
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
            // Still proceed but return not found to avoid hanging
            return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val cacheKey = mediaMetadata.id
        val deferred = fetchesMutex.withLock {
            activeFetches.getOrPut(cacheKey) {
                helperScope.async {
                    try {
                        val useWordSyncPriority = context.dataStore.data.first()[UseWordSyncPriorityFetchKey] ?: true
                        if (useWordSyncPriority) {
                            raceProviders(mediaMetadata)
                        } else {
                            sequentialProviders(mediaMetadata)
                        }
                    } catch (e: Exception) {
                        // Should not happen - both strategies are defensive internally -
                        // but never let an unexpected exception here leave the caller
                        // (and whatever UI state is awaiting it) stuck indefinitely.
                        reportException(e)
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

    
    private suspend fun sequentialProviders(mediaMetadata: MediaMetadata): LyricsWithProvider {
        val providers = resolveLyricsProviders()
        for (provider in providers) {
            if (provider.isEnabled(context)) {
                try {
                    val result = provider.getLyrics(
                        mediaMetadata.id,
                        mediaMetadata.title,
                        mediaMetadata.artists.joinToString { it.name },
                        mediaMetadata.duration,
                        mediaMetadata.album?.title,
                    )
                    result.onSuccess { lyrics ->
                        return LyricsWithProvider(lyrics, provider.name)
                    }.onFailure {
                        reportException(it)
                    }
                } catch (e: Exception) {
                    // Catch network-related exceptions like UnresolvedAddressException
                    reportException(e)
                }
            }
        }
        return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
    }

    private data class Candidate(
        val lyrics: String,
        val providerName: String,
        val tier: Int,
    )

    
    private suspend fun raceProviders(mediaMetadata: MediaMetadata): LyricsWithProvider = coroutineScope {
        val providers = resolveLyricsProviders().filter { it.isEnabled(context) && !isProviderCoolingDown(it.name) }
        if (providers.isEmpty()) {
            return@coroutineScope LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val priorityIndex = providers.withIndex().associate { (index, provider) -> provider.name to index }
        val results = Channel<Candidate>(Channel.UNLIMITED)

        // Signals once every provider that could possibly return word-by-word
        // data has finished (succeeded, failed, or timed out).
        val wordCapableRemaining = AtomicInteger(providers.count { it.supportsWordSync })
        val wordCapableDone = CompletableDeferred<Unit>()
        if (wordCapableRemaining.get() == 0) wordCapableDone.complete(Unit)

        val jobs = providers.map { provider ->
            async {
                try {

                    val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                            mediaMetadata.album?.title,
                        )
                    }
                    if (result == null) {
                        recordProviderFailure(provider.name)
                    } else {
                        result.onSuccess { lyrics ->
                            recordProviderSuccess(provider.name)
                            if (lyrics.isNotBlank()) {
                                val tier = LyricsUtils.getSyncTier(lyrics)
                                if (tier > LyricsUtils.TIER_PLAIN) {
                                    results.trySend(Candidate(lyrics, provider.name, tier))
                                }
                            }
                        }.onFailure {
                            recordProviderFailure(provider.name)
                            reportException(it)
                        }
                    }
                } catch (e: Exception) {
                    // Catch network-related exceptions like UnresolvedAddressException
                    recordProviderFailure(provider.name)
                    reportException(e)
                } finally {
                    if (provider.supportsWordSync && wordCapableRemaining.decrementAndGet() == 0) {
                        wordCapableDone.complete(Unit)
                    }
                }
            }
        }


        // Close the channel once every provider has finished, so the receive
        // loop below ends on its own even if fewer results arrive than expected.
        val closerJob = launch {
            jobs.joinAll()
            results.close()
        }

        var best: Candidate? = null
        val overallDeadlineAt = System.currentTimeMillis() + OVERALL_TIMEOUT_MS
        // Set when the first line-sync result arrives; word-sync providers then
        // get at most WORD_SYNC_PATIENCE_MS to beat it before we give up waiting.
        var wordSyncPatientUntil = Long.MAX_VALUE

        try {
            pollLoop@ while (true) {
                val timeLeft = overallDeadlineAt - System.currentTimeMillis()
                if (timeLeft <= 0) break@pollLoop

                // We already have a usable result and either:
                //  (a) every word-capable provider has finished, OR
                //  (b) our patience window for word-sync has expired.
                // Either way, stop waiting.
                if (best != null && (wordCapableDone.isCompleted ||
                            System.currentTimeMillis() >= wordSyncPatientUntil)) break@pollLoop

                // Poll in short slices so we notice wordCapableDone completing
                // without needing a full select expression.
                val slice = minOf(POLL_SLICE_MS, timeLeft)
                val received = withTimeoutOrNull(slice) { results.receiveCatching() }
                if (received == null) continue@pollLoop // this slice timed out - loop back and re-check exit conditions
                if (received.isClosed) break@pollLoop // channel drained - nothing more will ever arrive
                val candidate = received.getOrNull() ?: continue@pollLoop

                if (candidate.tier == LyricsUtils.TIER_WORD_SYNC) {
                    best = candidate
                    break@pollLoop // can't do better than word-by-word - stop immediately
                }

                // Line-sync result: record it and start the patience countdown so
                // we don't wait the full 15 s for word-sync providers that clearly
                // don't have this track.
                val candidatePriority = priorityIndex[candidate.providerName] ?: Int.MAX_VALUE
                val bestPriority = best?.let { priorityIndex[it.providerName] ?: Int.MAX_VALUE }
                val candidateIsBetter = best == null ||
                        candidate.tier > best.tier ||
                        (candidate.tier == best.tier && candidatePriority < bestPriority!!)

                if (candidateIsBetter) {
                    best = candidate
                    // Start patience window on the first line-sync result we accept.
                    if (wordSyncPatientUntil == Long.MAX_VALUE) {
                        wordSyncPatientUntil = System.currentTimeMillis() + WORD_SYNC_PATIENCE_MS
                    }
                }
            }
        } finally {
            jobs.forEach { it.cancel() }
            closerJob.cancel()
            results.close()
        }

        best?.let { LyricsWithProvider(it.lyrics, it.providerName) }
            ?: LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
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

        val allResult = mutableListOf<LyricsResult>()
        val providers = resolveLyricsProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
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
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 20
        private const val FAILURE_THRESHOLD = 3
        private const val COOLDOWN_MS = 60_000L
        private const val POLL_SLICE_MS = 150L
        private const val WORD_SYNC_PATIENCE_MS = 4_000L
        private const val OVERALL_TIMEOUT_MS = 16_000L
        private const val PROVIDER_TIMEOUT_MS = 8_000L
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