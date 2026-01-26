@file:OptIn(ExperimentalCoroutinesApi::class)

package com.music.vivi.viewmodels

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.vivi.constants.AlbumFilter
import com.music.vivi.constants.AlbumFilterKey
import com.music.vivi.constants.AlbumSortDescendingKey
import com.music.vivi.constants.AlbumSortType
import com.music.vivi.constants.AlbumSortTypeKey
import com.music.vivi.constants.ArtistFilter
import com.music.vivi.constants.ArtistFilterKey
import com.music.vivi.constants.ArtistSongSortDescendingKey
import com.music.vivi.constants.ArtistSongSortType
import com.music.vivi.constants.ArtistSongSortTypeKey
import com.music.vivi.constants.ArtistSortDescendingKey
import com.music.vivi.constants.ArtistSortType
import com.music.vivi.constants.ArtistSortTypeKey
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.PlaylistSortDescendingKey
import com.music.vivi.constants.PlaylistSortType
import com.music.vivi.constants.PlaylistSortTypeKey
import com.music.vivi.constants.SongFilter
import com.music.vivi.constants.SongFilterKey
import com.music.vivi.constants.SongSortDescendingKey
import com.music.vivi.constants.SongSortType
import com.music.vivi.constants.SongSortTypeKey
import com.music.vivi.constants.TopSize
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.extensions.filterExplicit
import com.music.vivi.extensions.filterExplicitAlbums
import com.music.vivi.extensions.toEnum
import com.music.vivi.playback.DownloadUtil
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the "Songs" tab in Library.
 * Supports filtering by source (Library, Liked, Downloaded, Uploaded) and sorting.
 */
@HiltViewModel
public class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    public val allSongs: StateFlow<List<com.music.vivi.db.entities.Song>> =
        context.dataStore.data
            .map {
                Pair(
                    Triple(
                        it[SongFilterKey].toEnum(SongFilter.LIKED),
                        it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                        (it[SongSortDescendingKey] ?: true)
                    ),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending).map { it.filterExplicit(hideExplicit) }
                    SongFilter.LIKED -> database.likedSongs(sortType, descending).map {
                        it.filterExplicit(hideExplicit)
                    }
                    SongFilter.DOWNLOADED -> database.downloadedSongs(sortType, descending).map {
                        it.filterExplicit(hideExplicit)
                    }
                    SongFilter.UPLOADED -> database.uploadedSongs(sortType, descending).map {
                        it.filterExplicit(hideExplicit)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    public fun syncLibrarySongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLibrarySongs() }
    }

    public fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }
}

/**
 * ViewModel for the "Artists" tab in Library.
 * Automatically updates artist metadata (thumbnails) from YouTube every 10 days.
 */
@HiltViewModel
public class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    public val allArtists: StateFlow<List<com.music.vivi.db.entities.Artist>> =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                    it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                    it[ArtistSortDescendingKey] ?: true
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncArtistsSubscriptions() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { artist: Artist -> artist.artist }
                    .filter { artistEntity: ArtistEntity ->
                        artistEntity.thumbnailUrl == null ||
                            Duration.between(
                                artistEntity.lastUpdateTime,
                                LocalDateTime.now()
                            ) > Duration.ofDays(10)
                    }.forEach { artist: ArtistEntity ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

/**
 * ViewModel for the "Albums" tab in Library.
 * Automatically attempts to fetch metadata for incomplete albums (songCount == 0).
 */
@HiltViewModel
public class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    public val allAlbums: StateFlow<List<com.music.vivi.db.entities.Album>> =
        context.dataStore.data
            .map {
                Pair(
                    Triple(
                        it[AlbumFilterKey].toEnum(AlbumFilter.LIKED),
                        it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                        it[AlbumSortDescendingKey] ?: true
                    ),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    AlbumFilter.LIBRARY -> database.albums(sortType, descending).map {
                        it.filterExplicitAlbums(hideExplicit)
                    }
                    AlbumFilter.LIKED -> database.albumsLiked(sortType, descending).map {
                        it.filterExplicitAlbums(hideExplicit)
                    }
                    AlbumFilter.UPLOADED -> database.albumsUploaded(sortType, descending).map {
                        it.filterExplicitAlbums(hideExplicit)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedAlbums() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
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
                    }
            }
        }
    }
}

/**
 * ViewModel for the "Playlists" tab in Library.
 */
@HiltViewModel
public class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    public val allPlaylists: StateFlow<List<com.music.vivi.db.entities.Playlist>> =
        context.dataStore.data
            .map {
                it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (
                    it[PlaylistSortDescendingKey]
                        ?: true
                    )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.playlists(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    public fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncSavedPlaylists() }
    }

    public val topValue: kotlinx.coroutines.flow.Flow<String> =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

/**
 * ViewModel for displaying songs of a specific artist in the library.
 * This is effectively "Songs by Artist" view.
 */
@HiltViewModel
public class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    public val artist: StateFlow<com.music.vivi.db.entities.Artist?> =
        database
            .artist(artistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    public val songs: StateFlow<List<com.music.vivi.db.entities.Song>> =
        context.dataStore.data
            .map {
                Pair(
                    it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (
                        it[ArtistSongSortDescendingKey]
                            ?: true
                        ),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit) ->
                val (sortType, descending) = sortDesc
                database.artistSongs(artistId, sortType, descending).map { it.filterExplicit(hideExplicit) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

/**
 * ViewModel for the "Overview" / "Mix" tab in Library.
 * Displays horizontal lists of recent artists, albums, playlists.
 */
@HiltViewModel
public class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    public val syncAllLibrary: () -> Unit = {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.syncLikedSongs()
            syncUtils.syncLibrarySongs()
            syncUtils.syncArtistsSubscriptions()
            syncUtils.syncLikedAlbums()
            syncUtils.syncSavedPlaylists()
        }
    }
    public val topValue: kotlinx.coroutines.flow.Flow<String> =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
    public var artists: StateFlow<List<com.music.vivi.db.entities.Artist>> =
        database
            .artistsBookmarked(
                ArtistSortType.CREATE_DATE,
                true
            ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    public var albums: StateFlow<List<com.music.vivi.db.entities.Album>> = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true).map { it.filterExplicitAlbums(hideExplicit) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    public var playlists: StateFlow<List<com.music.vivi.db.entities.Playlist>> = database.playlists(
        PlaylistSortType.CREATE_DATE,
        true
    )
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
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
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                            Duration.between(
                                it.lastUpdateTime,
                                LocalDateTime.now()
                            ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

/**
 * Shared ViewModel for managing the selected tab/filter in the Library screen.
 */
@HiltViewModel
public class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val _filter = mutableStateOf(LibraryFilter.LIBRARY)
    public val filter: State<LibraryFilter> = _filter

    public fun updateFilter(newFilter: LibraryFilter) {
        _filter.value = newFilter
    }
}
