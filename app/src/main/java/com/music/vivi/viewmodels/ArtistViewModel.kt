package com.music.vivi.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.pages.ArtistPage
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = database.artistSongsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            YouTube.artist(artistId)
                .onSuccess { page ->
                    val filteredSections = page.sections.filterNot { section ->
                        section.title.equals("From your library", ignoreCase = true)
                    }
                    artistPage = page.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                    artistPage = page.copy(sections = filteredSections)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
