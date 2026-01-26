package com.music.vivi.viewmodels

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.SwipeGestureEnabledKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.lyrics.LyricsHelper
import com.music.vivi.lyrics.LyricsResult
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.NetworkConnectivityObserver
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Lyrics screen/bottom sheet.
 * Handles searching for lyrics, Romanization toggles, and Swipe-to-Seek settings.
 */
@HiltViewModel
public class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    public val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private var job: Job? = null
    public val results: MutableStateFlow<List<LyricsResult>> = MutableStateFlow(emptyList<LyricsResult>())
    public val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _isNetworkAvailable = MutableStateFlow(false)
    public val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _currentSong = mutableStateOf<Song?>(null)
    public val currentSong: State<Song?> = _currentSong

    public val swipeGestureEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[SwipeGestureEnabledKey] ?: true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    public fun toggleSwipeGesture() {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                val current = settings[SwipeGestureEnabledKey] ?: true
                settings[SwipeGestureEnabledKey] = !current
            }
        }
    }

    public fun setSwipeGesture(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[SwipeGestureEnabledKey] = enabled
            }
        }
    }

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

    public fun setCurrentSong(song: Song) {
        _currentSong.value = song
    }

    public fun search(mediaId: String, title: String, artist: String, duration: Int) {
        isLoading.value = true
        results.value = emptyList()
        job?.cancel()
        job =
            viewModelScope.launch(Dispatchers.IO) {
                lyricsHelper.getAllLyrics(mediaId, title, artist, duration) { result ->
                    results.update {
                        it + result
                    }
                }
                isLoading.value = false
            }
    }

    public fun cancelSearch() {
        job?.cancel()
        job = null
    }

    public fun toggleRomanization(song: SongEntity) {
        viewModelScope.launch {
            database.query {
                upsert(song.copy(romanizeLyrics = !song.romanizeLyrics))
            }
        }
    }

    public fun setRomanization(song: SongEntity, enabled: Boolean) {
        viewModelScope.launch {
            database.query {
                upsert(song.copy(romanizeLyrics = enabled))
            }
        }
    }

    public fun updateLyrics(mediaId: String, lyrics: String) {
        viewModelScope.launch {
            database.query {
                upsert(LyricsEntity(id = mediaId, lyrics = lyrics))
            }
        }
    }

    public fun refetchLyrics(mediaMetadata: MediaMetadata, lyricsEntity: LyricsEntity?) {
        viewModelScope.launch {
            val lyrics = withContext(Dispatchers.IO) {
                lyricsHelper.getLyrics(mediaMetadata)
            }
            database.query {
                lyricsEntity?.let(::delete)
                upsert(LyricsEntity(mediaMetadata.id, lyrics))
            }
        }
    }
}
