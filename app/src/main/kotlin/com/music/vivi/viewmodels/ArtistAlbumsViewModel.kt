package com.music.vivi.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for displaying the list of Albums for a specific Artist from the local database.
 */
@HiltViewModel
public class ArtistAlbumsViewModel @Inject constructor(database: MusicDatabase, savedStateHandle: SavedStateHandle) :
    ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    public val artist: StateFlow<com.music.vivi.db.entities.Artist?> = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    public val albums: StateFlow<List<com.music.vivi.db.entities.Album>> = database.artistAlbumsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
