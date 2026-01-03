package com.music.vivi.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.reportException
import com.music.vivi.utils.Wikipedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    private val _albumDescription = MutableStateFlow<String?>(null)
    val albumDescription = _albumDescription.asStateFlow()

    private val _isDescriptionLoading = MutableStateFlow(false)
    val isDescriptionLoading = _isDescriptionLoading.asStateFlow()

    init {
        // Reactive Wikipedia Fetching
        // Observes album updates (local or remote) and fetches description if missing
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
                            // Save to database
                            database.query {
                                update(album.album.copy(description = description))
                            }
                        }
                        _isDescriptionLoading.value = false
                    }
                }
            }
        }

        // Network Refresh
        viewModelScope.launch(Dispatchers.IO) {
            val album = database.album(albumId).first()

            YouTube
                .album(albumId)
                .onSuccess { it ->
                    playlistId.value = it.album.playlistId
                    otherVersions.value = it.otherVersions
                    database.transaction {
                        if (album == null) {
                            insert(it)
                        } else {
                            update(album.album, it, album.artists)
                        }
                    }
                    // Note: Wikipedia fetching is handled by the collector above
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
