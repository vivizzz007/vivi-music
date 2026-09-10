package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.AlbumSortType
import com.music.vivi.constants.ArtistSortType
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.MixSortDescendingKey
import com.music.vivi.constants.MixSortType
import com.music.vivi.constants.MixSortTypeKey
import com.music.vivi.constants.PlaylistSortType
import com.music.vivi.constants.SongSortType
import com.music.vivi.constants.TopSize
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.extensions.toEnum
import com.music.vivi.constants.PinnedLibraryItemsKey
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import com.music.innertube.YouTube
import kotlinx.coroutines.delay
import com.music.vivi.utils.reportException
import com.music.vivi.extensions.filterExplicitAlbums

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val syncAllLibrary = {
         viewModelScope.launch(Dispatchers.IO) {
             syncUtils.tryAutoSync()
         }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.performFullSyncSuspend()
        }
    }

    val searchQuery = MutableStateFlow("")
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val debouncedSearchQuery = searchQuery.debounce(250)

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
            
    val recentLikedThumbnails = database.likedSongs(SongSortType.CREATE_DATE, true)
        .map { songs -> songs.take(5).mapNotNull { it.thumbnailUrl } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentDownloadedThumbnails = database.downloadedSongs(SongSortType.CREATE_DATE, true)
        .map { songs -> songs.take(3).mapNotNull { it.thumbnailUrl } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var artists =
        database
            .artistsBookmarked(
                ArtistSortType.CREATE_DATE,
                true,
            ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var albums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true).map { it.filterExplicitAlbums(hideExplicit) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentAlbumsThumbnails = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true)
                .map { albums ->
                    albums.filter { !hideExplicit || it.album.explicit != true }.take(3).mapNotNull { it.album.thumbnailUrl }
                }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentArtistsThumbnails = database.artistsBookmarked(ArtistSortType.CREATE_DATE, true)
        .map { artists ->
            artists.take(3).mapNotNull { it.artist.thumbnailUrl }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlistsCount = database.playlists(PlaylistSortType.CREATE_DATE, true)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)



    val sortSettings = context.dataStore.data.map {
        Pair(
            it[MixSortTypeKey].toEnum(MixSortType.CREATE_DATE),
            it[MixSortDescendingKey] ?: true
        )
    }.distinctUntilChanged()

    val pinnedUiItems = context.dataStore.data
        .map { it[PinnedLibraryItemsKey] ?: emptySet() }
        .distinctUntilChanged()
        .flatMapLatest { pinnedSet ->
            val albumIds = pinnedSet.filter { it.startsWith("album:") }.map { it.removePrefix("album:") }
            val artistIds = pinnedSet.filter { it.startsWith("artist:") }.map { it.removePrefix("artist:") }
            val playlistIds = pinnedSet.filter { it.startsWith("playlist:") }.map { it.removePrefix("playlist:") }
            
            val albumFlows = albumIds.map { database.album(it) }
            val artistFlows = artistIds.map { database.artist(it) }
            val playlistFlows = playlistIds.map { database.playlist(it) }
            
            val allFlows = albumFlows + artistFlows + playlistFlows
            if (allFlows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList<Any>())
            } else {
                combine(allFlows) { items ->
                    items.filterNotNull().toList()
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredUiItems = combine(
        artists, debouncedSearchQuery, sortSettings
    ) { arts, query, sortSet ->
        val (sortType, descending) = sortSet
        
        val mergedList: List<Any> = arts
        val collator = Collator.getInstance(Locale.getDefault())
        collator.strength = Collator.PRIMARY
        
        val sortedListResult = when (sortType) {
            MixSortType.CREATE_DATE ->
                mergedList.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }

            MixSortType.NAME ->
                mergedList.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )

            MixSortType.LAST_UPDATED ->
                mergedList.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
        }
        
        val sortedList = if (descending) sortedListResult.reversed() else sortedListResult

        val finalQuery = query.trim()
        val filteredList = if (finalQuery.isBlank()) {
            sortedList
        } else {
            sortedList.filter { item ->
                when (item) {
                    is Playlist -> item.playlist.name.contains(finalQuery, ignoreCase = true)
                    is Artist -> item.artist.name.contains(finalQuery, ignoreCase = true)
                    is Album -> item.album.title.contains(finalQuery, ignoreCase = true)
                    else -> false
                }
            }
        }
        
        val distinctIds = mutableSetOf<String>()
        val result = mutableListOf<Any>()
        for (item in filteredList) {
            val id = when (item) {
                is Playlist -> item.id
                is Artist -> item.id
                is Album -> item.id
                else -> item.hashCode().toString()
            }
            if (distinctIds.add(id)) {
                result.add(item)
            }
        }
        result
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albumsList ->
                albumsList
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                        delay(1000)
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artistsList ->
                artistsList
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                                Duration.between(
                                    it.lastUpdateTime,
                                    LocalDateTime.now(),
                                ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                        delay(1000)
                    }
            }
        }
    }

}
