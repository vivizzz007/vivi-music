package com.music.vivi.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.LyricsLetterByLetterAnimationKey
import com.music.vivi.constants.LyricsWordForWordKey
import com.music.vivi.constants.SwipeGestureEnabledKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.LyricsEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.lyrics.LyricsHelper
import com.music.vivi.lyrics.LyricsResult
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.NetworkConnectivityObserver
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {
    private var job: Job? = null
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val dataStore = context.dataStore

    val swipeGestureEnabled = dataStore.data
        .map { it[SwipeGestureEnabledKey] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lyricsWordForWord = dataStore.data
        .map { it[LyricsWordForWordKey] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lyricsLetterByLetter = dataStore.data
        .map { it[LyricsLetterByLetterAnimationKey] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
    ) {
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

    fun cancelSearch() {
        job?.cancel()
        job = null
    }

    fun toggleRomanizeLyrics(song: com.music.vivi.db.entities.SongEntity?, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            song?.let {
                database.query {
                    upsert(it.copy(romanizeLyrics = enabled))
                }
            }
        }
    }

    fun toggleSwipeGesture(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { settings ->
                settings[SwipeGestureEnabledKey] = enabled
            }
        }
    }

    fun toggleWordForWord(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { settings ->
                settings[LyricsWordForWordKey] = enabled
                if (enabled) {
                    settings[LyricsLetterByLetterAnimationKey] = false
                }
            }
        }
    }

    fun toggleLetterByLetter(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { settings ->
                settings[LyricsLetterByLetterAnimationKey] = enabled
                if (enabled) {
                    settings[LyricsWordForWordKey] = false
                }
            }
        }
    }

    fun updateLyrics(mediaId: String, lyrics: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.query {
                upsert(
                    LyricsEntity(
                        id = mediaId,
                        lyrics = lyrics,
                    ),
                )
            }
        }
    }

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val lyrics = lyricsHelper.getLyrics(mediaMetadata)
            database.query {
                lyricsEntity?.let(::delete)
                upsert(LyricsEntity(mediaMetadata.id, lyrics))
            }
        }
    }
}