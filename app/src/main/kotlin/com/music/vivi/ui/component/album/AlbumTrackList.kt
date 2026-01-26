package com.music.vivi.ui.component.album

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.db.entities.AlbumWithSongs
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.component.media.songs.roundedSongItems
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.ItemWrapper

fun LazyListScope.albumTrackList(
    wrappedSongs: List<ItemWrapper<Song>>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    selection: Boolean,
    onSelectionStart: () -> Unit,
    playlistId: String?,
    albumWithSongs: AlbumWithSongs?,
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    haptic: HapticFeedback,
) {
    if (wrappedSongs.isNotEmpty()) {
        roundedSongItems(
            items = wrappedSongs,
            key = { it.item.id },
            isActive = { it.item.id == mediaMetadata?.id },
            onItemClick = { songWrapper ->
                if (!selection) {
                    if (songWrapper.item.id == mediaMetadata?.id) {
                        playerConnection.player.togglePlayPause()
                    } else {
                        if (playlistId != null) {
                            playerConnection.service.getAutomix(playlistId)
                        }
                        playerConnection.playQueue(
                            LocalAlbumRadio(albumWithSongs!!, startIndex = wrappedSongs.indexOf(songWrapper))
                        )
                    }
                } else {
                    songWrapper.isSelected = !songWrapper.isSelected
                }
            },
            onItemLongClick = { songWrapper ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (!selection) {
                    onSelectionStart()
                }
                wrappedSongs.forEach {
                    it.isSelected = false
                }
                songWrapper.isSelected = true
            }
        ) { songWrapper, shape, modifier ->
            LibrarySongListItem(
                song = songWrapper.item,
                isActive = songWrapper.item.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                showInLibraryIcon = true,
                isSwipeable = false,
                drawHighlight = false,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = songWrapper.item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                },
                isSelected = songWrapper.isSelected,
                inSelectionMode = selection,
                onSelectionChange = { songWrapper.isSelected = it },
                modifier = modifier
            )
        }
    }
}
