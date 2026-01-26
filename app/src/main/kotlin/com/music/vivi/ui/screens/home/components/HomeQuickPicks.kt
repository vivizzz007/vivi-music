package com.music.vivi.ui.component.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.utils.ImmutableList

/**
 * "Quick Picks" section using the [HomeQuickPicks] song row layout.
 * Usually the first section on the Home screen, showing frequently played or highly recommended songs.
 * Groups songs into columns of 4 rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeQuickPicks(
    quickPicks: ImmutableList<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    maxWidth: Dp,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
    val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

    Column(modifier = modifier) {
        NavigationTitle(
            title = stringResource(R.string.quick_picks),
            modifier = Modifier
        )

        // Group songs into chunks of 4
        val songGroups = quickPicks.distinctBy { it.id }.chunked(4)

        LazyRow(
            state = rememberLazyListState(),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                .asPaddingValues()
                .let {
                    PaddingValues(
                        start = it.calculateStartPadding(LocalLayoutDirection.current) + 12.dp,
                        end = it.calculateEndPadding(LocalLayoutDirection.current),
                        top = it.calculateTopPadding(),
                        bottom = it.calculateBottomPadding()
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = songGroups,
                key = { group -> group.firstOrNull()?.id ?: "" }
            ) { songGroup ->
                Column(
                    modifier = Modifier
                        .width(horizontalLazyGridItemWidth),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    songGroup.forEachIndexed { index, song ->
                        val isFirst = index == 0
                        val isLast = index == songGroup.size - 1
                        val isActive = song.id == mediaMetadata?.id

                        // Static shape without animation
                        val shape = RoundedCornerShape(
                            topStart = if (isFirst) 20.dp else 0.dp,
                            topEnd = if (isFirst) 20.dp else 0.dp,
                            bottomStart = if (isLast) 20.dp else 0.dp,
                            bottomEnd = if (isLast) 20.dp else 0.dp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .clip(shape)
                                .background(
                                    if (isActive) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                        ) {
                            LibrarySongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = isActive,
                                isPlaying = isPlaying,
                                isSwipeable = false,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue.radio(
                                                        song.toMediaMetadata()
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
