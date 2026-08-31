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
import com.music.innertube.models.AlbumItem
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import androidx.navigation.NavController
import android.content.Context
import com.music.innertube.models.filterExplicit
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import com.music.vivi.constants.SuggestionRegionKey

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    private var currentLoadedRegion: String? = null
    
    private val _suggestionTracks = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionTracks: StateFlow<List<SuggestionTrack>?> = _suggestionTracks

    private val _suggestionArtists = MutableStateFlow<List<SuggestionArtist>?>(null)
    val suggestionArtists: StateFlow<List<SuggestionArtist>?> = _suggestionArtists

    private val _suggestionAlbums = MutableStateFlow<List<SuggestionAlbum>?>(null)
    val suggestionAlbums: StateFlow<List<SuggestionAlbum>?> = _suggestionAlbums

    private val _suggestionVideos = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionVideos: StateFlow<List<SuggestionTrack>?> = _suggestionVideos

    private val _youtubeNewReleases = MutableStateFlow<List<AlbumItem>?>(null)
    val youtubeNewReleases: StateFlow<List<AlbumItem>?> = _youtubeNewReleases

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isManualLoading = MutableStateFlow(false)
    val isManualLoading: StateFlow<Boolean> = _isManualLoading

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val regionCode = context.dataStore.get(SuggestionRegionKey, "system")
            refresh(countryCode = regionCode, force = false)
        }
    }

    fun refresh(countryCode: String = "system", force: Boolean = false) {
        val resolvedCode = if (countryCode == "system") {
            java.util.Locale.getDefault().country.lowercase()
        } else {
            countryCode.lowercase()
        }

        // Abort if we already loaded this region (unless forced)
        if (!force && currentLoadedRegion == resolvedCode) return
        
        // Abort if a load is currently happening
        if (_isLoading.value) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            if (force) _isManualLoading.value = true
            
            // Clear current data if we are switching regions or forcing a fresh load
            if (currentLoadedRegion != resolvedCode || force) {
                _suggestionTracks.value = null
                _suggestionArtists.value = null
                _suggestionAlbums.value = null
                _suggestionVideos.value = null
                _youtubeNewReleases.value = null
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

                    launch {
                        try {
                            YouTube.newReleaseAlbums().onSuccess { albums ->
                                val artists: MutableMap<Int, String> = mutableMapOf()
                                val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                                database.allArtistsByPlayTime().first().let { list ->
                                    var favIndex = 0
                                    for ((artistsIndex, artist) in list.withIndex()) {
                                        artists[artistsIndex] = artist.id
                                        if (artist.artist.bookmarkedAt != null) {
                                            favouriteArtists[favIndex] = artist.id
                                            favIndex++
                                        }
                                    }
                                }
                                _youtubeNewReleases.value =
                                    albums
                                        .sortedBy { album ->
                                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                            val firstArtistKey =
                                                artistIds.firstNotNullOfOrNull { artistId ->
                                                    if (artistId in favouriteArtists.values) {
                                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                                    } else {
                                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                                    }
                                                } ?: Int.MAX_VALUE
                                            firstArtistKey
                                        }.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                            }
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch YouTube new releases", e)
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

    /**
     * Returns true if the YT Music artist [ytArtistName] matches the Apple Music
     * [appleMusicArtist] string. Checks both directions so compound/featured-artist
     * strings (e.g. "Taylor Swift feat. Ed Sheeran") are handled correctly —
     * no words are stripped or removed from either side.
     */
    private fun artistMatches(ytArtistName: String, appleMusicArtist: String): Boolean {
        val ytNorm = ytArtistName.trim().lowercase()
        val apNorm = appleMusicArtist.trim().lowercase()
        // Check both directions — no stripping, no word removal
        return apNorm.contains(ytNorm) || ytNorm.contains(apNorm)
    }

    fun playTrack(track: SuggestionTrack, playerConnection: PlayerConnection?) {
        viewModelScope.launch(Dispatchers.IO) {
            val query = "${track.title} ${track.artist}"
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { searchResult ->
                val songs = searchResult.items.filterIsInstance<SongItem>()

                // 1. Exact title + at least one matching artist
                val bestMatch = songs.firstOrNull { s ->
                    s.title.equals(track.title, ignoreCase = true) &&
                    s.artists.any { a -> artistMatches(a.name, track.artist) }
                } ?:
                // 2. Title contains our target title + matching artist
                songs.firstOrNull { s ->
                    s.title.contains(track.title, ignoreCase = true) &&
                    s.artists.any { a -> artistMatches(a.name, track.artist) }
                } ?:
                // 3. Any result where at least one artist matches
                songs.firstOrNull { s ->
                    s.artists.any { a -> artistMatches(a.name, track.artist) }
                }
                // No blind fallback — if nothing matched confidently, we report it

                if (bestMatch != null) {
                    withContext(Dispatchers.Main) {
                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = bestMatch.id)))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "\"${track.title}\" is not available on YouTube Music",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
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
            val query = "${video.title} ${video.artist}"
            YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
                .onSuccess { searchResult ->
                    val songs = searchResult.items.filterIsInstance<SongItem>()

                    // Use the same improved multi-step matching as playTrack
                    val bestMatch = songs.firstOrNull { s ->
                        s.title.equals(video.title, ignoreCase = true) &&
                        s.artists.any { a -> artistMatches(a.name, video.artist) }
                    } ?:
                    songs.firstOrNull { s ->
                        s.title.contains(video.title, ignoreCase = true) &&
                        s.artists.any { a -> artistMatches(a.name, video.artist) }
                    } ?:
                    songs.firstOrNull { s ->
                        s.artists.any { a -> artistMatches(a.name, video.artist) }
                    }
                    // No blind fallback — if nothing matched confidently, we report it

                    if (bestMatch != null) {
                        withContext(Dispatchers.Main) {
                            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = bestMatch.id)))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "\"${video.title}\" is not available on YouTube Music",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
    }
}
