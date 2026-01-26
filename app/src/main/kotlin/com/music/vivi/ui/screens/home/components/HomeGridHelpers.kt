package com.music.vivi.ui.component.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavController
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.LocalItem
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.component.media.albums.AlbumGridItem
import com.music.vivi.ui.component.media.artists.ArtistGridItem
import com.music.vivi.ui.component.media.songs.SongGridItem
import com.music.vivi.ui.component.media.youtube.YouTubeGridItem
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.ArtistMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import kotlinx.coroutines.CoroutineScope

/**
 * Helper composite for rendering a grid item for a LOCAL database item (Song, Album, Artist).
 * Routes to the appropriate specific item component.
 */
@Composable
internal fun HomeLocalGridItem(
    item: LocalItem,
    isPlaying: Boolean,
    activeId: String?,
    activeAlbumId: String?,
    navController: NavController,
    menuState: MenuState,
    scope: CoroutineScope,
    playerConnection: PlayerConnection,
) {
    val haptic = LocalHapticFeedback.current

    when (item) {
        is Song -> SongGridItem(
            song = item,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (item.id == activeId) {
                            playerConnection.player.togglePlayPause()
                        } else {
                            playerConnection.playQueue(
                                YouTubeQueue.radio(item.toMediaMetadata())
                            )
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        menuState.show {
                            SongMenu(
                                originalSong = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                ),
            isActive = item.id == activeId,
            isPlaying = isPlaying
        )

        is Album -> AlbumGridItem(
            album = item,
            isActive = item.id == activeAlbumId,
            isPlaying = isPlaying,
            onPlayClick = {
                playerConnection.playQueue(
                    com.music.vivi.playback.queues.ListQueue(
                        title = item.album.title,
                        items = item.songs.map { it.toMediaItem() },
                        startIndex = 0
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        navController.navigate("album/${item.id}")
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
        )

        is Artist -> ArtistGridItem(
            artist = item,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        navController.navigate("artist/${item.id}")
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        menuState.show {
                            ArtistMenu(
                                originalArtist = item,
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
        )

        is Playlist -> {}
    }
}

/**
 * Helper composite for rendering a grid item for a YOUTUBE specific item (Song, Video, Album, Playlist, Artist).
 * Routes to the appropriate specific item component.
 */
@Composable
internal fun HomeYTGridItem(
    item: YTItem,
    isPlaying: Boolean,
    activeId: String?,
    activeAlbumId: String?,
    navController: NavController,
    menuState: MenuState,
    scope: CoroutineScope,
    playerConnection: PlayerConnection,
) {
    val haptic = LocalHapticFeedback.current

    YouTubeGridItem(
        item = item,
        isActive = item.id == activeId || item.id == activeAlbumId,
        isPlaying = isPlaying,
        coroutineScope = scope,
        thumbnailRatio = 1f,
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    when (item) {
                        is SongItem -> playerConnection.playQueue(
                            YouTubeQueue(
                                item.endpoint ?: WatchEndpoint(
                                    videoId = item.id
                                ),
                                item.toMediaMetadata()
                            )
                        )

                        is AlbumItem -> navController.navigate("album/${item.id}")
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem -> YouTubeSongMenu(
                                song = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )

                            is AlbumItem -> YouTubeAlbumMenu(
                                albumItem = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )

                            is ArtistItem -> YouTubeArtistMenu(
                                artist = item,
                                onDismiss = menuState::dismiss
                            )

                            is PlaylistItem -> YouTubePlaylistMenu(
                                playlist = item,
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            )
    )
}
