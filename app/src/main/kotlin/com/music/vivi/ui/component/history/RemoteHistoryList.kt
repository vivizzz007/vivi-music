package com.music.vivi.ui.component.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.innertube.pages.HistoryPage
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.youtube.YouTubeListItem
import com.music.vivi.ui.menu.YouTubeSongMenu

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.remoteHistoryList(
    filteredRemoteContent: List<HistoryPage.HistorySection>?,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    onHistoryRemoved: () -> Unit,
) {
    filteredRemoteContent?.forEach { section ->
        stickyHeader {
            NavigationTitle(
                title = section.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        items(
            items = section.songs,
            key = { "${section.title}_${it.id}_${section.songs.indexOf(it)}" }
        ) { song ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val index = section.songs.indexOf(song)
                val isFirst = index == 0
                val isLast = index == section.songs.size - 1
                val isSingleSong = section.songs.size == 1

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ListItemHeight)
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isFirst) 20.dp else 0.dp,
                                topEnd = if (isFirst) 20.dp else 0.dp,
                                bottomStart = if (isLast && !isSingleSong) 20.dp else 0.dp,
                                bottomEnd = if (isLast && !isSingleSong) 20.dp else 0.dp
                            )
                        )
                        .background(
                            if (song.id == mediaMetadata?.id) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                ) {
                    YouTubeListItem(
                        item = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                            onHistoryRemoved = onHistoryRemoved
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
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(song.toMediaMetadata())
                                        )
                                    }
                                },
                                onLongClick = {
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                            onHistoryRemoved = onHistoryRemoved
                                        )
                                    }
                                }
                            )
                    )
                }
                if (!isLast) {
                    Spacer(modifier = Modifier.height(3.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
