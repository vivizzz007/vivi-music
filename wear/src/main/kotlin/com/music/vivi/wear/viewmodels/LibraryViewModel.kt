/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.PlaylistItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Tabs available in the Wear OS Library screen.
 */
enum class LibraryTab {
    PLAYLISTS,
    ALBUMS,
    DOWNLOADS,
}

/**
 * State representing the user's account library and offline downloads on Wear OS.
 */
data class LibraryState(
    val selectedTab: LibraryTab = LibraryTab.PLAYLISTS,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val playlists: List<PlaylistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val error: String? = null,
)

/**
 * ViewModel managing the account library (playlists, albums, and offline downloads) on Wear OS.
 *
 * Checks YouTube Music login state via [YouTube.cookie], fetches user playlists and albums
 * via :innertube browse endpoints, and coordinates tab navigation.
 */
class LibraryViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fetchPlaylists: suspend () -> Result<List<PlaylistItem>> = {
        runCatching {
            val page = YouTube.library("FEmusic_liked_playlists").getOrThrow()
            page.items
                .filterIsInstance<PlaylistItem>()
                .filter { it.id != "SE" && !it.id.startsWith("SS") && it.id.isNotBlank() }
        }
    },
    private val fetchAlbums: suspend () -> Result<List<AlbumItem>> = {
        runCatching {
            val page = YouTube.library("FEmusic_liked_albums").getOrThrow()
            page.items.filterIsInstance<AlbumItem>()
        }
    },
    private val isCookiePresent: () -> Boolean = { YouTube.cookie != null },
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        refresh()
    }

    /**
     * Updates the currently active library tab.
     */
    fun selectTab(tab: LibraryTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    /**
     * Refreshes the library state.
     *
     * If not logged in ([YouTube.cookie] is null), sets [LibraryState.isLoggedIn] to false.
     * When logged in, concurrently fetches saved playlists and albums from InnerTube.
     */
    fun refresh() {
        val loggedIn = isCookiePresent()
        if (!loggedIn) {
            _state.update {
                it.copy(
                    isLoggedIn = false,
                    isLoading = false,
                    playlists = emptyList(),
                    albums = emptyList(),
                    error = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                isLoggedIn = true,
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch(ioDispatcher) {
            var playlistError: Throwable? = null
            var albumError: Throwable? = null

            // Fetch playlists (FEmusic_liked_playlists), filtering out episodes ("SE") and Shorts ("SS")
            val playlistsResult = fetchPlaylists().onFailure {
                Timber.e(it, "Failed to load library playlists")
                playlistError = it
            }

            // Fetch saved albums (FEmusic_liked_albums)
            val albumsResult = fetchAlbums().onFailure {
                Timber.e(it, "Failed to load library albums")
                albumError = it
            }

            val playlists = playlistsResult.getOrDefault(emptyList())
            val albums = albumsResult.getOrDefault(emptyList())

            val errorMsg = if (playlistsResult.isFailure && albumsResult.isFailure) {
                playlistError?.message ?: albumError?.message ?: "Failed to load library"
            } else {
                null
            }

            _state.update { current ->
                current.copy(
                    isLoggedIn = true,
                    isLoading = false,
                    playlists = if (playlistsResult.isSuccess) playlists else current.playlists,
                    albums = if (albumsResult.isSuccess) albums else current.albums,
                    error = errorMsg,
                )
            }
        }
    }
}
