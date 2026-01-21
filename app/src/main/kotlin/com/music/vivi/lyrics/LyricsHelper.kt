package com.music.vivi.lyrics

import android.content.Context
import android.util.LruCache
import com.music.vivi.constants.PowerSaverKey
import com.music.vivi.constants.PreferredLyricsProvider
import com.music.vivi.constants.PreferredLyricsProviderKey
import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.extensions.toEnum
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.reportException
import com.music.vivi.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val lyricsProviders =
        listOf(
            BetterLyricsLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    private suspend fun getOrderedProviders(): List<LyricsProvider> {
        val preferredProvider = context.dataStore.data
            .map { it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.BETTERLYRICS) }
            .firstOrNull() ?: PreferredLyricsProvider.BETTERLYRICS

        val otherProviders = when (preferredProvider) {
            PreferredLyricsProvider.BETTERLYRICS -> listOf(
                LrcLibLyricsProvider,
                KuGouLyricsProvider,
                YouTubeSubtitleLyricsProvider,
                YouTubeLyricsProvider
            )
            PreferredLyricsProvider.LRCLIB -> listOf(
                LrcLibLyricsProvider,
                KuGouLyricsProvider,
                YouTubeSubtitleLyricsProvider,
                YouTubeLyricsProvider
            )
            PreferredLyricsProvider.KUGOU -> listOf(
                KuGouLyricsProvider,
                LrcLibLyricsProvider,
                YouTubeSubtitleLyricsProvider,
                YouTubeLyricsProvider
            )
        }

        return listOf(BetterLyricsLyricsProvider) + otherProviders.filter { it != BetterLyricsLyricsProvider }
    }

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return cached.lyrics
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return LYRICS_NOT_FOUND
        }

        val providers = getOrderedProviders()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        val deferreds = providers.filter { it.isEnabled(context) }.map { provider ->
            scope.async {
                try {
                    val result = provider.getLyrics(
                        mediaMetadata.id,
                        mediaMetadata.title,
                        mediaMetadata.artists.joinToString { it.name },
                        mediaMetadata.duration,
                    )
                    
                    result.getOrNull()
                } catch (e: Exception) {
                    reportException(e)
                    null
                }
            }
        }

        // Wait for results and pick the best one
        // Priority: 
        // 1. Synced lyrics (starts with [)
        // 2. Plain lyrics
        
        var bestLyrics: String? = null
        
        // Wait for all but we can optimize by returning early if we find high-quality synced lyrics
        // However, with small number of providers, waiting for all is okay and simple.
        val results = deferreds.mapNotNull { it.await() }
        
        bestLyrics = results.find { it.startsWith("[") } ?: results.firstOrNull() ?: LYRICS_NOT_FOUND
        
        scope.cancel()
        return bestLyrics
    }


    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
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

        // Check power saver before making network requests
        val powerSaver = context.dataStore.data
            .map { it[PowerSaverKey] ?: false }
            .firstOrNull() ?: false

        if (powerSaver) {
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

        val providers = getOrderedProviders()
        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { rawLyrics ->
                            val result = LyricsResult(provider.name, rawLyrics)
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
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
