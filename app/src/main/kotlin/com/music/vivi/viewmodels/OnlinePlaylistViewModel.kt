package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterVideoSongs
import com.music.vivi.R
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    database: MusicDatabase
) : ViewModel() {
    private val playlistId = savedStateHandle.get<String>("playlistId")!!

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val relatedItems = MutableStateFlow<List<YTItem>>(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    val dbPlaylist = database.playlistByBrowseId(playlistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    var continuation: String? = null
        private set

    private var proactiveLoadJob: Job? = null

    init {
        fetchInitialPlaylistData()
    }

    private fun fetchInitialPlaylistData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            continuation = null
            proactiveLoadJob?.cancel() // Cancel any ongoing proactive load

            YouTube.playlist(playlistId)
                .onSuccess { playlistPage ->
                    playlist.value = playlistPage.playlist
                    playlistSongs.value = applySongFilters(playlistPage.songs)
                    relatedItems.value = playlistPage.related.orEmpty()
                    continuation = playlistPage.songsContinuation
                    _isLoading.value = false
                    if (continuation != null) {
                        startProactiveBackgroundLoading()
                    }
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: context.getString(R.string.failed_to_load_playlist)
                    _isLoading.value = false
                    reportException(throwable)
                }
        }
    }

    private fun startProactiveBackgroundLoading() {
        proactiveLoadJob?.cancel() // Cancel previous job if any
        proactiveLoadJob = viewModelScope.launch(Dispatchers.IO) {
            var currentProactiveToken = continuation
            while (currentProactiveToken != null && isActive) {
                // If a manual loadMore is happening, pause proactive loading
                if (_isLoadingMore.value) {
                    // Wait until manual load is finished, then re-evaluate
                    // This simple break and restart strategy from loadMoreSongs is preferred
                    break 
                }

                YouTube.playlistContinuation(currentProactiveToken)
                    .onSuccess { playlistContinuationPage ->
                        val currentSongs = playlistSongs.value.toMutableList()
                        currentSongs.addAll(playlistContinuationPage.songs)
                        playlistSongs.value = applySongFilters(currentSongs)
                        currentProactiveToken = playlistContinuationPage.continuation
                        // Update the class-level continuation for manual loadMore if needed
                        this@OnlinePlaylistViewModel.continuation = currentProactiveToken 
                    }.onFailure { throwable ->
                        reportException(throwable)
                        currentProactiveToken = null // Stop proactive loading on error
                    }
            }
            // If loop finishes because currentProactiveToken is null, all songs are loaded proactively.
        }
    }

    fun loadMoreSongs() {
        if (_isLoadingMore.value) return // Already loading more (manually)
        
        val tokenForManualLoad = continuation ?: return // No more songs to load

        proactiveLoadJob?.cancel() // Cancel proactive loading to prioritize manual scroll
        _isLoadingMore.value = true

        viewModelScope.launch(Dispatchers.IO) {
            YouTube.playlistContinuation(tokenForManualLoad)
                .onSuccess { playlistContinuationPage ->
                    val currentSongs = playlistSongs.value.toMutableList()
                    playlistSongs.value = applySongFilters(currentSongs)
                    playlistSongs.value = currentSongs.distinctBy { it.id }
                    continuation = playlistContinuationPage.continuation
                }.onFailure { throwable ->
                    reportException(throwable)
                }.also {
                    _isLoadingMore.value = false
                    // Resume proactive loading if there's still a continuation
                    if (continuation != null && isActive) {
                        startProactiveBackgroundLoading()
                    }
                }
        }
    }

    fun retry() {
        proactiveLoadJob?.cancel()
        fetchInitialPlaylistData() // This will also restart proactive loading if applicable
    }


    private fun applySongFilters(songs: List<SongItem>): List<SongItem> {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        return songs
            .distinctBy { it.id }
            .filterVideoSongs(hideVideoSongs)
    }

    override fun onCleared() {
        super.onCleared()
        proactiveLoadJob?.cancel()
    }
}
