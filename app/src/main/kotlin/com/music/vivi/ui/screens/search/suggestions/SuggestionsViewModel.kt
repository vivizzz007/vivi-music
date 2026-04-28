/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import androidx.navigation.NavController

@HiltViewModel
class SuggestionsViewModel @Inject constructor() : ViewModel() {
    private var currentLoadedRegion: String? = null
    
    private val _suggestionTracks = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionTracks: StateFlow<List<SuggestionTrack>?> = _suggestionTracks

    private val _suggestionArtists = MutableStateFlow<List<SuggestionArtist>?>(null)
    val suggestionArtists: StateFlow<List<SuggestionArtist>?> = _suggestionArtists

    private val _suggestionAlbums = MutableStateFlow<List<SuggestionAlbum>?>(null)
    val suggestionAlbums: StateFlow<List<SuggestionAlbum>?> = _suggestionAlbums

    private val _suggestionVideos = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionVideos: StateFlow<List<SuggestionTrack>?> = _suggestionVideos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isManualLoading = MutableStateFlow(false)
    val isManualLoading: StateFlow<Boolean> = _isManualLoading

    fun refresh(countryCode: String = "system", force: Boolean = false) {
        val resolvedCode = if (countryCode == "system") {
            java.util.Locale.getDefault().country.lowercase()
        } else {
            countryCode.lowercase()
        }

        // Allow refresh if force is true OR if we are switching regions
        if (_isLoading.value && !force && currentLoadedRegion == resolvedCode) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            if (force) _isManualLoading.value = true
            
            // Clear current data if we are switching regions or forcing a fresh load
            if (currentLoadedRegion != resolvedCode || force) {
                _suggestionTracks.value = null
                _suggestionArtists.value = null
                _suggestionAlbums.value = null
                _suggestionVideos.value = null
            }

            try {
                coroutineScope {
                    // Launch each fetch in its own job so they update the UI independently
                    launch {
                        try {
                            val tracks = AppleMusicScraper.fetchTopSongs(resolvedCode)
                            if (tracks.isNotEmpty()) {
                                _suggestionTracks.value = tracks
                                _suggestionArtists.value = AppleMusicScraper.getTrendingArtists(tracks)
                            }
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch songs", e)
                        }
                    }

                    launch {
                        try {
                            val albums = AppleMusicScraper.fetchTopAlbums(resolvedCode)
                            if (albums.isNotEmpty()) {
                                _suggestionAlbums.value = albums
                            }
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch albums", e)
                        }
                    }

                    launch {
                        try {
                            val videos = AppleMusicScraper.fetchTopVideos(resolvedCode)
                            if (videos.isNotEmpty()) {
                                _suggestionVideos.value = videos
                            }
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch videos", e)
                        }
                    }
                }

                currentLoadedRegion = resolvedCode
            } catch (e: Exception) {
                Log.e("SuggestionsViewModel", "Failed to fetch suggestions", e)
            } finally {
                _isLoading.value = false
                _isManualLoading.value = false
            }
        }
    }

    fun playTrack(track: SuggestionTrack, playerConnection: PlayerConnection?) {
        viewModelScope.launch(Dispatchers.IO) {
            val query = "${track.title} ${track.artist}"
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { searchResult ->
                val songs = searchResult.items.filterIsInstance<SongItem>()
                
                // 1. Try to find an exact title match with at least one matching artist
                val bestMatch = songs.firstOrNull { s ->
                    s.title.equals(track.title, ignoreCase = true) &&
                    s.artists.any { a -> track.artist.contains(a.name, ignoreCase = true) }
                } ?: 
                // 2. Try to find a title that contains our target title and matches artist
                songs.firstOrNull { s ->
                    s.title.contains(track.title, ignoreCase = true) &&
                    s.artists.any { a -> track.artist.contains(a.name, ignoreCase = true) }
                } ?:
                // 3. Just find the first one that matches the artist
                songs.firstOrNull { s ->
                    s.artists.any { a -> track.artist.contains(a.name, ignoreCase = true) }
                } ?:
                // 4. Fallback to first song result
                songs.firstOrNull()

                if (bestMatch != null) {
                    withContext(Dispatchers.Main) {
                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = bestMatch.id)))
                    }
                }
            }
        }
    }

    fun navigateToArtist(artist: SuggestionArtist, navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.search(artist.name, YouTube.SearchFilter.FILTER_ARTIST)
                .onSuccess { searchResult ->
                    val firstArtist =
                        searchResult.items.filterIsInstance<ArtistItem>().firstOrNull()
                    if (firstArtist != null) {
                        withContext(Dispatchers.Main) {
                            navController.navigate("artist/${firstArtist.id}")
                        }
                    }
                }
        }
    }
    fun navigateToAlbum(album: SuggestionAlbum, navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            val query = "${album.title} ${album.artist}"
            YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM)
                .onSuccess { searchResult ->
                    val firstAlbum =
                        searchResult.items.filterIsInstance<com.music.innertube.models.AlbumItem>().firstOrNull()
                    if (firstAlbum != null) {
                        withContext(Dispatchers.Main) {
                            navController.navigate("album/${firstAlbum.id}")
                        }
                    }
                }
        }
    }

    fun playVideo(video: SuggestionTrack, playerConnection: PlayerConnection?) {
        viewModelScope.launch(Dispatchers.IO) {
            val query = "${video.title} ${video.artist} official music video"
            YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
                .onSuccess { searchResult ->
                    val firstVideo =
                        searchResult.items.filterIsInstance<SongItem>().firstOrNull()
                    if (firstVideo != null) {
                        withContext(Dispatchers.Main) {
                            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = firstVideo.id)))
                        }
                    }
                }
        }
    }
}
