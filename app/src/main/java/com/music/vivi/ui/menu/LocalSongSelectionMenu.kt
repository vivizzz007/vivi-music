package com.music.vivi.ui.menu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.GridMenu
import com.music.vivi.ui.component.GridMenuItem
import java.time.LocalDateTime

@Composable
fun LocalSongSelectionMenu(
    selection: List<Song>,
    onDismiss: () -> Unit,
    onExitSelectionMode: () -> Unit,
) {
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val allInLibrary by remember(selection) { // exclude local songs
        mutableStateOf(selection.isNotEmpty() && selection.all { !it.song.isLocal && it.song.inLibrary != null })
    }

    val allLiked by remember(selection) {
        mutableStateOf(selection.isNotEmpty() && selection.all { it.song.liked })
    }


    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    items = selection.map { it.toMediaItem() },
                ),
            )
            onExitSelectionMode()
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    items = selection.shuffled().map { it.toMediaItem() },
                ),
            )
            onExitSelectionMode()
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(selection.map { it.toMediaItem() })
            onExitSelectionMode()
        }
        if (allInLibrary) {
            GridMenuItem(
                icon = R.drawable.library_add_check,
                title = R.string.remove_from_library,
            ) {
                database.transaction {
                    selection.forEach { song ->
                        inLibrary(song.id, null)
                    }
                }
            }
        } else {
            GridMenuItem(
                icon = R.drawable.library_add,
                title = R.string.add_to_library,
            ) {
                database.transaction {
                    selection.forEach { song ->
                        inLibrary(song.id, LocalDateTime.now())
                    }
                }
            }
        }

        GridMenuItem(
            icon = if (allLiked) R.drawable.favorite else R.drawable.favorite_border,
            tint = { if (allLiked) MaterialTheme.colorScheme.error else LocalContentColor.current },
            title = if (allLiked) R.string.action_remove_like_all else R.string.action_like_all,
        ) {
            database.transaction {
                if (allLiked) {
                    selection.forEach { song ->
                        update(song.song.copy(liked = false))
                    }
                } else {
                    selection.forEach { song ->
                        val likedSong = song.song.copy(liked = true)
                        update(likedSong)
                        downloadUtil.autoDownloadIfLiked(likedSong)
                    }
                }
            }
        }
    }
}