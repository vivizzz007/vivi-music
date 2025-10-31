package com.music.vivi.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()
            YouTube
                .album(albumId)
                .onSuccess {
                    playlistId.value = it.album.playlistId
                    otherVersions.value = it.otherVersions
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
