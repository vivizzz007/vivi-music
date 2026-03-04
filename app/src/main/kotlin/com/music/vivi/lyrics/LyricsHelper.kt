/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import android.util.LruCache
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private var lyricsProviders =
        listOf(
            YouLyPlusLyricsProvider,
            BetterLyricsProvider,
            SimpMusicLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )


    val preferred =
        context.dataStore.data
            .map {
                it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.YOULYPLUS)
            }.distinctUntilChanged()
            .map {
                lyricsProviders = when (it) {
                    PreferredLyricsProvider.LRCLIB -> listOf(
                        LrcLibLyricsProvider,
                        YouLyPlusLyricsProvider,
                        BetterLyricsProvider,
                        SimpMusicLyricsProvider,
                        KuGouLyricsProvider,
                        YouTubeSubtitleLyricsProvider,
                        YouTubeLyricsProvider
                    )

                    PreferredLyricsProvider.KUGOU -> listOf(
                        KuGouLyricsProvider,
                        YouLyPlusLyricsProvider,
                        BetterLyricsProvider,
                        SimpMusicLyricsProvider,
                        LrcLibLyricsProvider,
                        YouTubeSubtitleLyricsProvider,
                        YouTubeLyricsProvider
                    )

                    PreferredLyricsProvider.BETTER_LYRICS -> listOf(
                        BetterLyricsProvider,
                        YouLyPlusLyricsProvider,
                        SimpMusicLyricsProvider,
                        LrcLibLyricsProvider,
                        KuGouLyricsProvider,
                        YouTubeSubtitleLyricsProvider,
                        YouTubeLyricsProvider
                    )

                    PreferredLyricsProvider.SIMPMUSIC -> listOf(
                        SimpMusicLyricsProvider,
                        YouLyPlusLyricsProvider,
                        BetterLyricsProvider,
                        LrcLibLyricsProvider,
                        KuGouLyricsProvider,
                        YouTubeSubtitleLyricsProvider,
                        YouTubeLyricsProvider
                    )

                    PreferredLyricsProvider.YOULYPLUS -> listOf(
                        YouLyPlusLyricsProvider,
                        BetterLyricsProvider,
                        SimpMusicLyricsProvider,
                        LrcLibLyricsProvider,
                        KuGouLyricsProvider,
                        YouTubeSubtitleLyricsProvider,
                        YouTubeLyricsProvider
                    )

                }
            }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

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

        val scope = CoroutineScope(SupervisorJob())
        val deferred = scope.async {
            for (provider in lyricsProviders) {
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
                            return@async LyricsWithProvider(lyrics, provider.name)
                        }.onFailure {
                            reportException(it)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        reportException(e)
                    }
                }
            }
            return@async LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val result = deferred.await()
        scope.cancel()
        return result
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
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            lyricsProviders.forEach { provider ->
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
        private const val MAX_CACHE_SIZE = 3
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