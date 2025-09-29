package com.music.vivi.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.filterExplicit
import com.music.innertube.pages.ArtistPage
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.extensions.filterExplicitAlbums
import com.music.vivi.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import com.music.vivi.extensions.filterExplicit
import com.music.vivi.utils.get

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map { it[HideExplicitKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    val filteredSections = page.sections
                        .filterNot { section ->
                            section.moreEndpoint?.browseId?.startsWith("MPLAUC") == true
                        }
                        .map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit))
                        }

                    artistPage = page.copy(sections = filteredSections)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
