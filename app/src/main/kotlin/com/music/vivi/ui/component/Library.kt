package com.music.vivi.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.R
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.ui.component.media.albums.AlbumGridItem
import com.music.vivi.ui.component.media.albums.AlbumListItem
import com.music.vivi.ui.component.media.artists.ArtistGridItem
import com.music.vivi.ui.component.media.playlists.PlaylistGridItem
import com.music.vivi.ui.component.media.playlists.PlaylistListItem
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.ArtistMenu
import com.music.vivi.ui.menu.PlaylistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Grid item for an Artist in the Library.
 * Handles navigation and context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun LibraryArtistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier,
    onItemClick: (() -> Unit)? = null,
): Unit = ArtistGridItem(
    artist = artist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (onItemClick != null) {
                    onItemClick()
                } else {
                    navController.navigate("artist/${artist.id}")
                }
            },
            onLongClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
)

/**
 * List item for an Album in the Library.
 * Displays download state, favorite status, and context menu.
 */
@Composable
public fun LibraryAlbumListItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onItemClick: (() -> Unit)? = null,
) {
    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val downloadState by downloadUtil.getDownload(album.id).collectAsState(initial = null)
    val albumState by database.album(album.id).collectAsState(initial = album)
    val isFavorite = albumState?.album?.bookmarkedAt != null

    AlbumListItem(
        album = album,
        isActive = isActive,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        downloadState = downloadState?.state,
        trailingContent = {
            androidx.compose.material3.IconButton(
                onClick = {
                    menuState.show {
                        AlbumMenu(
                            originalAlbum = album,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (onItemClick != null) {
                    onItemClick()
                } else {
                    navController.navigate("album/${album.id}")
                }
            }
    )
}

/**
 * Grid item for an Album in the Library.
 * Displays download state (aggregated from songs), favorite status, and context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun LibraryAlbumGridItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onItemClick: (() -> Unit)? = null,
) {
    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val playerConnection = com.music.vivi.LocalPlayerConnection.current ?: return

    val songs by androidx.compose.runtime.produceState<List<com.music.vivi.db.entities.Song>>(
        initialValue = emptyList(),
        album.id
    ) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            value = database.albumSongs(album.id).first()
        }
    }
    val allDownloads by downloadUtil.downloads.collectAsState()
    val downloadState by androidx.compose.runtime.remember(songs, allDownloads) {
        androidx.compose.runtime.mutableStateOf(
            if (songs.isEmpty()) {
                androidx.media3.exoplayer.offline.Download.STATE_STOPPED
            } else {
                when {
                    songs.all {
                        allDownloads[it.id]?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
                    } -> androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
                    songs.any {
                        allDownloads[it.id]?.state in
                            listOf(
                                androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
                                androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
                            )
                    } -> androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
                    else -> androidx.media3.exoplayer.offline.Download.STATE_STOPPED
                }
            }
        )
    }
    val isFavorite = album.album.bookmarkedAt != null

    AlbumGridItem(
        album = album,
        isActive = isActive,
        isPlaying = isPlaying,
        isFavorite = isFavorite,
        downloadState = downloadState,
        onPlayClick = {
            coroutineScope.launch {
                val albumWithSongs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.albumWithSongs(album.id).firstOrNull()
                }
                albumWithSongs?.let {
                    playerConnection.playQueue(com.music.vivi.playback.queues.LocalAlbumRadio(it))
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (onItemClick != null) {
                        onItemClick()
                    } else {
                        navController.navigate("album/${album.id}")
                    }
                },
                onLongClick = {
                    menuState.show {
                        AlbumMenu(
                            originalAlbum = album,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                    }
                }
            )
    )
}

/**
 * List item for a Playlist in the Library.
 * Handles both local and online (synced) playlists navigation and menus.
 */
@Composable
public fun LibraryPlaylistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onItemClick: (() -> Unit)? = null,
): Unit = PlaylistListItem(
    playlist = playlist,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            YouTubePlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            if (onItemClick != null) {
                onItemClick()
            } else {
                if (!playlist.playlist.isEditable &&
                    playlist.songCount == 0 &&
                    playlist.playlist.remoteSongCount != 0
                ) {
                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                } else {
                    navController.navigate("local_playlist/${playlist.id}")
                }
            }
        }
)

/**
 * Grid item for a Playlist in the Library.
 * Handles both local and online (synced) playlists navigation and menus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun LibraryPlaylistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onItemClick: (() -> Unit)? = null,
): Unit = PlaylistGridItem(
    playlist = playlist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (onItemClick != null) {
                    onItemClick()
                } else {
                    if (!playlist.playlist.isEditable &&
                        playlist.songCount == 0 &&
                        playlist.playlist.remoteSongCount != 0
                    ) {
                        navController.navigate("online_playlist/${playlist.playlist.browseId}")
                    } else {
                        navController.navigate("local_playlist/${playlist.id}")
                    }
                }
            },
            onLongClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            YouTubePlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        )
)

/**
 * List item for a Song in the Library.
 * Wrapper around `SongListItem` that injects local dependencies like download state and preferences.
 */
@Composable
public fun LibrarySongListItem(
    song: com.music.vivi.db.entities.Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    inSelectionMode: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    trailingContent: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    drawHighlight: Boolean = true,
) {
    val downloadUtil = LocalDownloadUtil.current
    val downloadState by downloadUtil.getDownload(song.id).collectAsState(initial = null)
    val swipeEnabled by com.music.vivi.utils.rememberPreference(
        com.music.vivi.constants.SwipeToSongKey,
        defaultValue = false
    )

    com.music.vivi.ui.component.media.songs.SongListItem(
        song = song,
        modifier = modifier,
        albumIndex = albumIndex,
        showLikedIcon = showLikedIcon,
        showInLibraryIcon = showInLibraryIcon,
        showDownloadIcon = showDownloadIcon,
        downloadState = downloadState?.state,
        swipeEnabled = swipeEnabled,
        isSelected = isSelected,
        isActive = isActive,
        isPlaying = isPlaying,
        isSwipeable = isSwipeable,
        inSelectionMode = inSelectionMode,
        onSelectionChange = onSelectionChange,
        trailingContent = trailingContent,
        drawHighlight = drawHighlight
    )
}
