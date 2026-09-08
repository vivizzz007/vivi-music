/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.spotify.Spotify
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyTrack
import com.music.vivi.R
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SpotifyPlaylistUiState(
    val playlistId: String = "",
    val title: String = "",
    val coverUrl: String? = null,
    val ownerName: String? = null,
    val totalTracks: Int = 0,
    val tracks: List<SpotifyTrack> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val resolvingTrackId: String? = null,
)

@HiltViewModel
class SpotifyPlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val playlistId: String = savedStateHandle.get<String>("playlistId").orEmpty()
    val isLikedSongs: Boolean = playlistId == "liked_songs"

    private val _uiState = MutableStateFlow(
        SpotifyPlaylistUiState(
            playlistId = playlistId,
            title = if (isLikedSongs) context.getString(R.string.spotify_liked_songs) else "",
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (isLikedSongs) {
                    val firstPage = Spotify.likedSongs(limit = 50, offset = 0).getOrThrow()
                    val allTracks = firstPage.items.map { it.track }.toMutableList()
                    val total = firstPage.total

                    // Fetch next batch if available to have up to 100 songs ready
                    if (total > 50) {
                        val secondPage = Spotify.likedSongs(limit = 50, offset = 50).getOrNull()
                        if (secondPage != null) {
                            allTracks.addAll(secondPage.items.map { it.track })
                        }
                    }

                    _uiState.update {
                        it.copy(
                            title = context.getString(R.string.spotify_liked_songs),
                            ownerName = "Spotify",
                            coverUrl = null,
                            totalTracks = total,
                            tracks = allTracks,
                            isLoading = false,
                            error = null,
                        )
                    }
                } else {
                    val playlistResult = Spotify.playlist(playlistId).getOrNull()
                    val tracksResult = Spotify.playlistTracks(playlistId, limit = 100, offset = 0).getOrThrow()
                    val fetchedTracks = tracksResult.items.mapNotNull { it.track }

                    val title = playlistResult?.name?.takeIf { it.isNotBlank() } ?: "Playlist"
                    val cover = playlistResult?.let { SpotifyMapper.getPlaylistThumbnail(it) }
                    val owner = playlistResult?.owner?.displayName
                    val total = playlistResult?.tracks?.total ?: tracksResult.total

                    _uiState.update {
                        it.copy(
                            title = title,
                            coverUrl = cover,
                            ownerName = owner,
                            totalTracks = total,
                            tracks = fetchedTracks,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load Spotify playlist",
                    )
                }
            }
        }
    }

    fun loadMoreTracks() {
        val currentTracks = _uiState.value.tracks
        val total = _uiState.value.totalTracks
        if (_uiState.value.isLoadingMore || currentTracks.size >= total) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val offset = currentTracks.size
                val newTracks = if (isLikedSongs) {
                    val page = Spotify.likedSongs(limit = 50, offset = offset).getOrThrow()
                    page.items.map { it.track }
                } else {
                    val page = Spotify.playlistTracks(playlistId, limit = 100, offset = offset).getOrThrow()
                    page.items.mapNotNull { it.track }
                }

                _uiState.update {
                    it.copy(
                        tracks = currentTracks + newTracks,
                        isLoadingMore = false,
                    )
                }
            } catch (e: Exception) {
                reportException(e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun playTrack(track: SpotifyTrack, playerConnection: PlayerConnection) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(resolvingTrackId = track.id) }
            try {
                val query = SpotifyMapper.buildSearchQuery(track)
                val searchResult = YouTube.search(
                    query = query,
                    filter = YouTube.SearchFilter.FILTER_SONG,
                ).getOrNull()

                val candidates = searchResult?.items
                    ?.filterIsInstance<SongItem>()
                    ?.distinctBy { it.id }
                    .orEmpty()

                val best = candidates.maxByOrNull { candidate ->
                    SpotifyMapper.matchScore(
                        spotifyTitle = track.name,
                        spotifyArtist = track.artists.joinToString(" ") { it.name },
                        spotifyDurationMs = track.durationMs,
                        candidateTitle = candidate.title,
                        candidateArtist = candidate.artists.joinToString(" ") { it.name },
                        candidateDurationSec = candidate.duration,
                    )
                } ?: candidates.firstOrNull()

                if (best != null) {
                    withContext(Dispatchers.Main) {
                        playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = best.id)))
                    }
                } else {
                    // Fallback to searching video items
                    val videoSearch = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                    val videoBest = videoSearch?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                    if (videoBest != null) {
                        withContext(Dispatchers.Main) {
                            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = videoBest.id)))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Track not found on YouTube", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error playing track: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _uiState.update { it.copy(resolvingTrackId = null) }
            }
        }
    }

    fun playAll(playerConnection: PlayerConnection, shuffle: Boolean = false) {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        val targetTrack = if (shuffle) tracks.random() else tracks.first()
        playTrack(targetTrack, playerConnection)
    }
}
