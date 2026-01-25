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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.EventWithSong
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.PlayerConnection
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.viewmodels.DateAgo

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.localHistoryList(
    filteredEvents: Map<DateAgo, List<EventWithSong>>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    selection: Boolean,
    selectedEventIds: MutableList<Long>,
    onSelectionChange: (Boolean) -> Unit,
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    haptic: HapticFeedback,
    dateAgoToString: (DateAgo) -> String,
) {
    filteredEvents.forEach { (dateAgo, events) ->
        stickyHeader {
            NavigationTitle(
                title = dateAgoToString(dateAgo),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        itemsIndexed(
            items = events,
            key = { index, event -> "${dateAgo}_${event.event.id}_$index" }
        ) { index, event ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val isFirst = index == 0
                val isLast = index == events.size - 1
                val isActive = event.song.id == mediaMetadata?.id
                val isSingleSong = events.size == 1
                val isSelected = selectedEventIds.contains(event.event.id)

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
                            if (isActive) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                ) {
                    LibrarySongListItem(
                        song = event.song,
                        isActive = isActive,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        isSelected = isSelected,
                        inSelectionMode = selection,
                        onSelectionChange = {
                            if (isSelected) {
                                selectedEventIds.remove(event.event.id)
                            } else {
                                selectedEventIds.add(event.event.id)
                            }
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    if (!selection) {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = event.song,
                                                event = event.event,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
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
                                    if (!selection) {
                                        if (event.song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = dateAgoToString(dateAgo),
                                                    items = events.map { it.song.toMediaItem() },
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    } else {
                                        if (isSelected) {
                                            selectedEventIds.remove(event.event.id)
                                        } else {
                                            selectedEventIds.add(event.event.id)
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!selection) {
                                        onSelectionChange(true)
                                        selectedEventIds.clear()
                                        selectedEventIds.add(event.event.id)
                                    }
                                }
                            )
                    )
                }

                // Add 3dp spacer between items (except after last)
                if (!isLast) {
                    Spacer(modifier = Modifier.height(3.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
