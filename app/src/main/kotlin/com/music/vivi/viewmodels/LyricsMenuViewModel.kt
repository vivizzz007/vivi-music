/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.lyrics.LyricsHelper
import com.music.vivi.lyrics.LyricsResult
import com.music.vivi.lyrics.LyricsUtils
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    init {
        viewModelScope.launch {
            networkConnectivity.networkStatus.collect { isConnected ->
                _isNetworkAvailable.value = isConnected
            }
        }

        _isNetworkAvailable.value = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true // Assume connected as fallback
        }
    }

    fun setCurrentSong(song: Song) {
        _currentSong.value = song
    }

    fun search(
        mediaId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                lyricsHelper.getAllLyrics(mediaId, title, artist, duration, album) { result ->
                    results.update {
                        it + result
                    }
                }
                isLoading.value = false
            }
    }

    fun cancelSearch() {
        job?.cancel()
        job = null
    }

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        database.query {
            lyricsEntity?.let(::delete)
            val lyricsWithProvider =
                runBlocking {
                    lyricsHelper.getLyrics(mediaMetadata)
                }
            upsert(LyricsEntity(mediaMetadata.id, lyricsWithProvider.lyrics, lyricsWithProvider.provider))
        }
    }

    val providerCandidates = MutableStateFlow<List<ProviderLyricsItem>>(emptyList())
    val isProbingProviders = MutableStateFlow(false)
    private var probeJob: Job? = null

    fun probeAllProviders(mediaMetadata: MediaMetadata) {
        probeJob?.cancel()
        isProbingProviders.value = true
        providerCandidates.value = emptyList()
        probeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                lyricsHelper.getAllLyrics(
                    mediaId = mediaMetadata.id,
                    songTitle = mediaMetadata.title,
                    songArtists = mediaMetadata.artists.joinToString { it.name },
                    duration = mediaMetadata.duration,
                    album = mediaMetadata.album?.title
                ) { result ->
                    val parsed = LyricsUtils.parseLyrics(result.lyrics)
                    val hasWordSync = parsed.any { !it.words.isNullOrEmpty() }
                    val hasLineSync = parsed.any { it.time > 0 }
                    val previewSnippet = parsed
                        .filter { it.text.isNotBlank() }
                        .take(2)
                        .joinToString(" / ") { it.text.trim() }
                        .ifBlank { result.lyrics.lines().filter { it.isNotBlank() }.take(2).joinToString(" / ") }

                    val item = ProviderLyricsItem(
                        providerName = result.providerName,
                        lyrics = result.lyrics,
                        hasWordSync = hasWordSync,
                        hasLineSync = hasLineSync,
                        previewSnippet = previewSnippet
                    )
                    providerCandidates.update { current ->
                        if (current.any { it.providerName.equals(result.providerName, ignoreCase = true) }) {
                            current
                        } else {
                            current + item
                        }
                    }
                }
            } finally {
                isProbingProviders.value = false
            }
        }
    }

    fun selectProviderLyrics(
        mediaMetadata: MediaMetadata,
        providerName: String,
        lyrics: String,
        onSelected: () -> Unit = {}
    ) {
        database.query {
            upsert(
                LyricsEntity(
                    id = mediaMetadata.id,
                    lyrics = lyrics,
                    provider = providerName
                )
            )
        }
        onSelected()
    }
}

data class ProviderLyricsItem(
    val providerName: String,
    val lyrics: String,
    val hasWordSync: Boolean,
    val hasLineSync: Boolean,
    val previewSnippet: String,
)
