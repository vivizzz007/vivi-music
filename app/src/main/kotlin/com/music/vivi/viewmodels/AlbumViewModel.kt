package com.music.vivi.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.Wikipedia
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for displaying Album details.
 *
 * Responsibilities:
 * - Fetching Album details from YouTube Music.
 * - Observing local library state for the album (Bookmarking/DB sync).
 * - Reactively fetching and updating Album Description (Wikipedia).
 * - Handling sync between local and remote album data.
 */
@HiltViewModel
public class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _albumId = MutableStateFlow(savedStateHandle.get<String>("albumId"))
    public val albumId: StateFlow<String?> = _albumId.asStateFlow()

    public val playlistId: MutableStateFlow<String> = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    public val albumWithSongs: StateFlow<com.music.vivi.db.entities.AlbumWithSongs?> = _albumId.filterNotNull().flatMapLatest { id ->
        database.albumWithSongs(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    public var otherVersions: MutableStateFlow<List<AlbumItem>> = MutableStateFlow(emptyList())
    public var releasesForYou: MutableStateFlow<List<AlbumItem>> = MutableStateFlow(emptyList())

    private val _albumDescription = MutableStateFlow<String?>(null)
    public val albumDescription: StateFlow<String?> = _albumDescription.asStateFlow()

    private val _isDescriptionLoading = MutableStateFlow(false)
    public val isDescriptionLoading: StateFlow<Boolean> = _isDescriptionLoading.asStateFlow()

    init {
        // Reactive Wikipedia Fetching
        viewModelScope.launch(Dispatchers.IO) {
            albumWithSongs.collect { album ->
                if (album != null) {
                    if (album.album.description != null) {
                        _albumDescription.value = album.album.description
                    } else if (_albumDescription.value == null) {
                        _isDescriptionLoading.value = true
                        val artistName = album.artists.firstOrNull()?.name
                        val description = Wikipedia.fetchAlbumInfo(album.album.title, artistName)
                        if (description != null) {
                            _albumDescription.value = description
                            database.query {
                                update(album.album.copy(description = description))
                            }
                        }
                        _isDescriptionLoading.value = false
                    }
                }
            }
        }

        // Network Refresh with direct YouTube calls
        viewModelScope.launch(Dispatchers.IO) {
            _albumId.collect { id ->
                if (id == null) return@collect
                val album = database.album(id).first()
                YouTube.album(id).onSuccess { it ->
                    playlistId.value = it.album.playlistId
                    otherVersions.value = it.otherVersions
                    releasesForYou.value = it.releasesForYou
                    database.transaction {
                        if (album == null) {
                            insert(it)
                        } else {
                            update(album.album, it, album.artists)
                        }
                    }
                }.onFailure {
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }
            }
        }
    }

    public fun setAlbumId(id: String) {
        if (_albumId.value != id) {
            _albumId.value = id
        }
    }
}
