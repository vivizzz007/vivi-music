package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.LocalItem
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for searching the Local Library / Database.
 * Supports filtering by Songs, Albums, Artists, or Playlists.
 * Returns a unified [LocalSearchResult] object.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class LocalSearchViewModel
@Inject
constructor(database: MusicDatabase) : ViewModel() {
    public val query: MutableStateFlow<String> = MutableStateFlow("")
    public val filter: MutableStateFlow<LocalFilter> = MutableStateFlow(LocalFilter.ALL)

    public val result: StateFlow<LocalSearchResult> =
        combine(query, filter) { query, filter ->
            query to filter
        }.flatMapLatest { (query, filter) ->
            if (query.isEmpty()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                when (filter) {
                    LocalFilter.ALL ->
                        combine(
                            database.searchSongs(query, PREVIEW_SIZE),
                            database.searchAlbums(query, PREVIEW_SIZE),
                            database.searchArtists(query, PREVIEW_SIZE),
                            database.searchPlaylists(query, PREVIEW_SIZE)
                        ) { songs, albums, artists, playlists ->
                            songs + albums + artists + playlists
                        }

                    LocalFilter.SONG -> database.searchSongs(query)
                    LocalFilter.ALBUM -> database.searchAlbums(query)
                    LocalFilter.ARTIST -> database.searchArtists(query)
                    LocalFilter.PLAYLIST -> database.searchPlaylists(query)
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                        list.groupBy {
                            when (it) {
                                is Song -> LocalFilter.SONG
                                is Album -> LocalFilter.ALBUM
                                is Artist -> LocalFilter.ARTIST
                                is Playlist -> LocalFilter.PLAYLIST
                            }
                        }
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    public companion object {
        public const val PREVIEW_SIZE: Int = 3
    }
}

public enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

public data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
