package com.music.vivi.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.pages.ArtistPage
import com.music.vivi.constants.ArtistSongSortType
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.extensions.filterExplicit
import com.music.vivi.extensions.filterExplicitAlbums
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main Artist Detail Screen.
 *
 * Responsibilities:
 * - Fetches Artist page data from YouTube Music.
 * - Observes local library songs and albums for this artist.
 * - Merges library data with network data.
 * - Handles filtering preferences (Hide Explicit / Hide Music Videos).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _artistId = MutableStateFlow(savedStateHandle.get<String>("artistId"))
    public val artistId: StateFlow<String?> = _artistId.asStateFlow()

    public var artistPage: ArtistPage? by mutableStateOf(null)

    public val libraryArtist: StateFlow<com.music.vivi.db.entities.Artist?> = _artistId.filterNotNull().flatMapLatest { id ->
        database.artist(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    public val librarySongs: StateFlow<List<com.music.vivi.db.entities.Song>> = combine(
        context.dataStore.data.map { it[HideExplicitKey] ?: false }.distinctUntilChanged(),
        _artistId.filterNotNull()
    ) { hideExplicit, id ->
        Pair(hideExplicit, id)
    }.flatMapLatest { (hideExplicit, id) ->
        database.artistSongs(id, ArtistSongSortType.CREATE_DATE, true).map { it.filterExplicit(hideExplicit) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public val libraryAlbums: StateFlow<List<com.music.vivi.db.entities.Album>> = combine(
        context.dataStore.data.map { it[HideExplicitKey] ?: false }.distinctUntilChanged(),
        _artistId.filterNotNull()
    ) { hideExplicit, id ->
        Pair(hideExplicit, id)
    }.flatMapLatest { (hideExplicit, id) ->
        database.artistAlbumsPreview(id).map { it.filterExplicitAlbums(hideExplicit) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    public fun setArtistId(id: String) {
        if (_artistId.value != id) {
            _artistId.value = id
            fetchArtistsFromYTM()
        }
    }

    public fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val id = _artistId.value ?: return@launch
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            YouTube.artist(id)
                .onSuccess { page ->
                    val filteredSections = page.sections
                        .filterNot { section ->
                            section.moreEndpoint?.browseId?.startsWith("MPLAUC") == true
                        }
                        .map { section ->
                            section.copy(
                                items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs)
                            )
                        }

                    artistPage = page.copy(sections = filteredSections)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
