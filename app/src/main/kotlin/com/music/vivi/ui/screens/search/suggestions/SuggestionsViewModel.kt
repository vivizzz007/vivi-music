/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _billboardTracks = MutableStateFlow<List<BillboardTrack>?>(null)
    val billboardTracks: StateFlow<List<BillboardTrack>?> = _billboardTracks

    private val _billboardArtists = MutableStateFlow<List<BillboardArtist>?>(null)
    val billboardArtists: StateFlow<List<BillboardArtist>?> = _billboardArtists

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun refresh(regionSlug: String = "system") {
        if (_isLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _billboardTracks.value = BillboardScraper.fetchHot100(regionSlug)
                _billboardArtists.value = BillboardScraper.fetchArtist100()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrack(track: BillboardTrack, playerConnection: PlayerConnection?) {
        viewModelScope.launch(Dispatchers.IO) {
            val query = "${track.title} ${track.artist}"
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { searchResult ->
                // Force it to find the official song track instead of a music video
                val firstSong = searchResult.items.filterIsInstance<SongItem>().firstOrNull()
                if (firstSong != null) {
                    withContext(Dispatchers.Main) {
                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = firstSong.id)))
                    }
                }
            }
        }
    }

    fun navigateToArtist(artist: BillboardArtist, navController: NavController) {
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
}
