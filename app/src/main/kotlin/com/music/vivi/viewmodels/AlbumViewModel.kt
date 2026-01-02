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
        viewModelScope.launch(Dispatchers.IO) {
            val album = database.album(albumId).first()
            
            // Try fetching with database info first if available
            album?.let {
                viewModelScope.launch(Dispatchers.IO) {
                    if (_albumDescription.value == null) {
                        _isDescriptionLoading.value = true
                        val artistName = it.artists.firstOrNull()?.name
                        val description = Wikipedia.fetchAlbumInfo(it.album.title, artistName)
                        if (description != null) {
                            _albumDescription.value = description
                        }
                        _isDescriptionLoading.value = false
                    }
                }
            }

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

                    // Fetch Wikipedia description if not already fetched or if we have better info now
                    if (_albumDescription.value == null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            _isDescriptionLoading.value = true
                            val artistName = it.album.artists?.firstOrNull()?.name
                            val description = Wikipedia.fetchAlbumInfo(it.album.title, artistName)
                            _albumDescription.value = description
                            _isDescriptionLoading.value = false
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
