/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.PlaylistContinuationPage
import com.music.innertube.pages.PlaylistPage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * State representing a playlist or album detail view on Wear OS.
 */
data class PlaylistDetailState(
    val isLoading: Boolean = true,
    val playlist: PlaylistItem? = null,
    val songs: List<SongItem> = emptyList(),
    val continuation: String? = null,
    val error: String? = null,
)

/**
 * ViewModel managing track listings and pagination for a YouTube Music playlist or album.
 *
 * Handles Liked Songs ("LM"), user playlists ("PL..."), and album releases ("OLAK5uy_...").
 */
class PlaylistDetailViewModel(
    val playlistId: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fetchPlaylist: suspend (String) -> Result<PlaylistPage> = { id ->
        val cleanId = if (id.startsWith("VL") && id.length > 2) id.removePrefix("VL") else id
        runCatching { YouTube.playlist(cleanId).getOrThrow() }
    },
    private val fetchContinuation: suspend (String) -> Result<PlaylistContinuationPage> = { token ->
        runCatching { YouTube.playlistContinuation(token).getOrThrow() }
    },
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    @Volatile
    private var isLoadingMore = false

    init {
        loadPlaylist()
    }

    /**
     * Loads the initial batch of tracks for the playlist or album.
     */
    fun loadPlaylist() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(ioDispatcher) {
            // Strip any accidental "VL" prefix since YouTube.playlist prefixes "VL" internally
            val cleanId = if (playlistId.startsWith("VL") && playlistId.length > 2) {
                playlistId.removePrefix("VL")
            } else {
                playlistId
            }

            val result = fetchPlaylist(cleanId)

            result.fold(
                onSuccess = { page ->
                    val resolvedPlaylist = if (cleanId == "LM" && page.playlist.title.isBlank()) {
                        page.playlist.copy(title = "Liked Songs")
                    } else {
                        page.playlist
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            playlist = resolvedPlaylist,
                            songs = page.songs,
                            continuation = page.songsContinuation ?: page.continuation,
                            error = null,
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load playlist tracks for $cleanId")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load playlist",
                        )
                    }
                },
            )
        }
    }

    /**
     * Loads additional tracks using the continuation token if available.
     */
    fun loadMore() {
        val continuationToken = _state.value.continuation ?: return
        if (isLoadingMore) return
        isLoadingMore = true

        viewModelScope.launch(ioDispatcher) {
            fetchContinuation(continuationToken).fold(
                onSuccess = { contPage ->
                    _state.update { current ->
                        current.copy(
                            songs = current.songs + contPage.songs,
                            continuation = contPage.continuation,
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load playlist continuation")
                },
            )
            isLoadingMore = false
        }
    }

    companion object {
        fun provideFactory(
            playlistId: String,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
            fetchPlaylist: (suspend (String) -> Result<PlaylistPage>)? = null,
            fetchContinuation: (suspend (String) -> Result<PlaylistContinuationPage>)? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return if (fetchPlaylist != null && fetchContinuation != null) {
                    PlaylistDetailViewModel(playlistId, ioDispatcher, fetchPlaylist, fetchContinuation) as T
                } else if (fetchPlaylist != null) {
                    PlaylistDetailViewModel(playlistId, ioDispatcher, fetchPlaylist = fetchPlaylist) as T
                } else {
                    PlaylistDetailViewModel(playlistId, ioDispatcher) as T
                }
            }
        }
    }
}
