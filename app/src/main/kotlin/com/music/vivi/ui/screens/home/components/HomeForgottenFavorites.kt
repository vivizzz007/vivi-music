package com.music.vivi.ui.component.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.GridSnapLayoutInfoProvider
import com.music.vivi.utils.ImmutableList
import kotlin.math.min

/**
 * "Forgotten Favorites" section.
 * Displays a grid of songs that the user used to listen to but hasn't recently.
 * Calculated locally based on play history and skip counts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeForgottenFavorites(
    forgottenFavorites: ImmutableList<Song>,
    mediaMetadataId: String?,
    isPlaying: Boolean,
    maxWidth: Dp,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    if (forgottenFavorites.isEmpty()) return

    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val lazyGridState = rememberLazyGridState()

    LaunchedEffect(forgottenFavorites) {
        lazyGridState.scrollToItem(0)
    }

    val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
    val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

    val snapLayoutInfoProvider = remember(lazyGridState) {
        GridSnapLayoutInfoProvider(
            lazyGridState = lazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
            }
        )
    }

    NavigationTitle(
        title = stringResource(R.string.forgotten_favorites),
        modifier = Modifier
    )

    // take min in case list size is less than 4
    val rows = min(4, forgottenFavorites.size)
    LazyHorizontalGrid(
        state = lazyGridState,
        rows = GridCells.Fixed(rows),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        flingBehavior = rememberSnapFlingBehavior(
            snapLayoutInfoProvider
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight * rows)
    ) {
        items(
            items = forgottenFavorites.distinctBy { it.id },
            key = { it.id }
        ) { song ->
            val isActive = song.id == mediaMetadataId

            // Copied generic Box wrapping from Home (lines 550-559 in Home, but for LazyRow)
            // ForgottenFavorites seems to put SongListItem directly in grid cell.
            // Check snippet 1283 Lines 735+. It puts SongListItem inside modifiers.
            // But wait, GridItem has width set to horizontalLazyGridItemWidth.

            Box(
                modifier = Modifier
                    .width(horizontalLazyGridItemWidth)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        .fillMaxWidth() // Fill box width
                        .combinedClickable(
                            onClick = {
                                if (song.id == mediaMetadataId) {
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
