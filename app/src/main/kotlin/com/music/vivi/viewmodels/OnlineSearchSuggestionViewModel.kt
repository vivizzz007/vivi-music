/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.utils.YouTubeUrlParser
import kotlinx.coroutines.flow.firstOrNull
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.SearchHistory
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.music.vivi.db.entities.EventWithSong
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchFilterOption {
    SONGS,
    ARTISTS,
    ALBUMS,
    BY_LYRICS
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(SearchFilterOption.SONGS)
    val isSearchSubmitted = MutableStateFlow(false)

    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    fun submitSearch(q: String) {
        isSearchSubmitted.value = true
        query.value = q
    }

    fun setFilter(filter: SearchFilterOption) {
        selectedFilter.value = filter
    }

    init {
        viewModelScope.launch {
            combine(
                combine(query, selectedFilter, isSearchSubmitted) { q, filter, submitted ->
                    Triple(q, filter, submitted)
                }.flatMapLatest { (query, filter, submitted) ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history,
                                isSearchSubmitted = false,
                                selectedFilter = filter,
                            )
                        }
                    } else {
                        val parsedUrl = YouTubeUrlParser.parse(query)
                        val parsedItem = if (parsedUrl != null) fetchParsedUrlItem(parsedUrl) else null
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                        val trimmedQuery = query.trim()

                        when (filter) {
                            SearchFilterOption.SONGS -> {
                                val result = if (parsedUrl != null) null else YouTube.searchSuggestions(query).getOrNull()
                                val localLyricsSongs = if (trimmedQuery.length >= 3) {
                                    database.searchSongsByLyrics(trimmedQuery).firstOrNull().orEmpty().map { song ->
                                        SongItem(
                                            id = song.song.id,
                                            title = song.song.title,
                                            artists = song.artists.map { com.music.innertube.models.Artist(id = it.id, name = it.name) },
                                            album = song.album?.let { com.music.innertube.models.Album(id = it.id, name = it.title) },
                                            duration = song.song.duration,
                                            thumbnail = song.song.thumbnailUrl ?: "",
                                            explicit = song.song.explicit,
                                            endpoint = WatchEndpoint(videoId = song.song.id)
                                        )
                                    }
                                } else emptyList()

                                val directSongSearch = if (parsedUrl == null && trimmedQuery.isNotEmpty()) {
                                    YouTube.search(trimmedQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                        ?.items
                                        ?.filterIsInstance<SongItem>()
                                        .orEmpty()
                                } else emptyList()

                                val lyricsMatchedIds = localLyricsSongs.map { it.id }.toSet()

                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        val topItems = (listOfNotNull(parsedItem) +
                                            result?.recommendedItems.orEmpty())
                                            .distinctBy { it.id }
                                            .filter { it.id != parsedItem?.id }
                                            .filterExplicit(hideExplicit)
                                            .filterVideoSongs(hideVideoSongs)

                                        val finalTopItems = if (topItems.isEmpty() && directSongSearch.isNotEmpty()) {
                                            directSongSearch.take(1)
                                        } else {
                                            topItems
                                        }

                                        val finalSongs = (localLyricsSongs + directSongSearch)
                                            .distinctBy { it.id }
                                            .filter { song -> finalTopItems.none { it.id == song.id } }
                                            .filterExplicit(hideExplicit)
                                            .filterVideoSongs(hideVideoSongs)

                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions =
                                            result
                                                ?.queries
                                                ?.filter { suggestionQuery ->
                                                    history.none { it.query == suggestionQuery }
                                                }.orEmpty(),
                                            items = finalTopItems,
                                            songs = finalSongs,
                                            isFromLink = parsedUrl != null,
                                            isSearchSubmitted = submitted,
                                            selectedFilter = filter,
                                            lyricsMatchedSongIds = lyricsMatchedIds,
                                        )
                                    }
                            }

                            SearchFilterOption.ARTISTS -> {
                                val artists = if (trimmedQuery.isNotEmpty()) {
                                    YouTube.search(trimmedQuery, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                                        ?.items
                                        ?.filterIsInstance<ArtistItem>()
                                        .orEmpty()
                                } else emptyList()

                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = emptyList(),
                                            items = artists,
                                            songs = emptyList(),
                                            isFromLink = false,
                                            isSearchSubmitted = submitted,
                                            selectedFilter = filter,
                                            lyricsMatchedSongIds = emptySet(),
                                        )
                                    }
                            }

                            SearchFilterOption.ALBUMS -> {
                                val albums = if (trimmedQuery.isNotEmpty()) {
                                    YouTube.search(trimmedQuery, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                                        ?.items
                                        ?.filterIsInstance<AlbumItem>()
                                        .orEmpty()
                                } else emptyList()

                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = emptyList(),
                                            items = albums,
                                            songs = emptyList(),
                                            isFromLink = false,
                                            isSearchSubmitted = submitted,
                                            selectedFilter = filter,
                                            lyricsMatchedSongIds = emptySet(),
                                        )
                                    }
                            }

                            SearchFilterOption.BY_LYRICS -> {
                                val localLyricsSongs = if (trimmedQuery.length >= 2) {
                                    database.searchSongsByLyrics(trimmedQuery).firstOrNull().orEmpty().map { song ->
                                        SongItem(
                                            id = song.song.id,
                                            title = song.song.title,
                                            artists = song.artists.map { com.music.innertube.models.Artist(id = it.id, name = it.name) },
                                            album = song.album?.let { com.music.innertube.models.Album(id = it.id, name = it.title) },
                                            duration = song.song.duration,
                                            thumbnail = song.song.thumbnailUrl ?: "",
                                            explicit = song.song.explicit,
                                            endpoint = WatchEndpoint(videoId = song.song.id)
                                        )
                                    }
                                } else emptyList()

                                val onlineLyricsSongs = if (trimmedQuery.isNotEmpty()) {
                                    val lyricsQuery = "$trimmedQuery lyrics"
                                    val r1 = YouTube.search(lyricsQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                        ?.items?.filterIsInstance<SongItem>().orEmpty()
                                    val r2 = YouTube.search(trimmedQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                        ?.items?.filterIsInstance<SongItem>().orEmpty()
                                    (r1 + r2).distinctBy { it.id }
                                } else emptyList()

                                val allLyricsSongs = (localLyricsSongs + onlineLyricsSongs)
                                    .distinctBy { it.id }
                                    .filterExplicit(hideExplicit)
                                    .filterVideoSongs(hideVideoSongs)

                                val allLyricsIds = allLyricsSongs.map { it.id }.toSet()

                                database
                                    .searchHistory(query)
                                    .map { it.take(3) }
                                    .map { history ->
                                        SearchSuggestionViewState(
                                            history = history,
                                            suggestions = emptyList(),
                                            items = emptyList(),
                                            songs = allLyricsSongs,
                                            isFromLink = false,
                                            isSearchSubmitted = submitted,
                                            selectedFilter = filter,
                                            lyricsMatchedSongIds = allLyricsIds,
                                        )
                                    }
                            }
                        }
                    }
                },
                context.dataStore.data
                    .map { prefs ->
                        val ids = prefs[com.music.vivi.constants.SearchListenHistoryKey]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
                        ids.take(9).mapNotNull { id ->
                            database.getSongByIdBlocking(id)?.let { song ->
                                EventWithSong(
                                    com.music.vivi.db.entities.Event(songId = id, timestamp = java.time.LocalDateTime.now(), playTime = 0),
                                    song
                                )
                            }
                        }
                    }
                    .flowOn(kotlinx.coroutines.Dispatchers.IO)
            ) { state, recentEvents ->
                state.copy(recentEvents = recentEvents)
            }.collect {
                _viewState.value = it
            }
        }
    }

    private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? {
        println("[LINK_PARSE_DEBUG] Fetching metadata for: $parsedUrl")
        return try {
            val item = when (parsedUrl) {
                is YouTubeUrlParser.ParsedUrl.Video -> {
                    YouTube.queue(listOf(parsedUrl.id)).getOrNull()?.firstOrNull()
                }

                is YouTubeUrlParser.ParsedUrl.Artist -> {
                    YouTube.artist(parsedUrl.id).getOrNull()?.artist
                }
            }
            println("[LINK_PARSE_DEBUG] Fetch successful: ${item?.id} (${item?.javaClass?.simpleName})")
            item
        } catch (e: Exception) {
            println("[LINK_PARSE_DEBUG] Fetch failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val songs: List<SongItem> = emptyList(),
    val recentEvents: List<EventWithSong> = emptyList(),
    val isFromLink: Boolean = false,
    val isSearchSubmitted: Boolean = false,
    val selectedFilter: SearchFilterOption = SearchFilterOption.SONGS,
    val lyricsMatchedSongIds: Set<String> = emptySet(),
)
