package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel @Inject constructor(
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    val result = combine(query, filter) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        if (query.isEmpty()) {
            flowOf(LocalSearchResult("", filter, emptyMap()))
        } else {
            when (filter) {
                LocalFilter.ALL -> combine(
                    database.searchSongs(query, PREVIEW_SIZE),
                    database.searchAlbums(query, PREVIEW_SIZE),
                    database.searchArtists(query, PREVIEW_SIZE),
                    database.searchPlaylists(query, PREVIEW_SIZE),
                ) { songs, albums, artists, playlists ->
                    val list = songs + albums + artists  + playlists
                    list.distinctBy { it.id }
                }
                LocalFilter.SONG -> database.searchSongs(query)
                LocalFilter.ALBUM -> database.searchAlbums(query)
                LocalFilter.ARTIST -> database.searchArtists(query)
                LocalFilter.PLAYLIST -> database.searchPlaylists(query)
            }.map { list ->
                LocalSearchResult(
                    query = query,
                    filter = filter,
                    map = list.groupBy {
                        when (it) {
                            is Song -> LocalFilter.SONG
                            is Album -> LocalFilter.ALBUM
                            is Artist -> LocalFilter.ARTIST
                            is Playlist -> LocalFilter.PLAYLIST
                        }
                    })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, LocalSearchResult("", filter.value, emptyMap()))

    companion object {
        const val PREVIEW_SIZE = 3
    }
}

enum class LocalFilter {
    ALL, SONG, ALBUM, ARTIST, PLAYLIST
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)