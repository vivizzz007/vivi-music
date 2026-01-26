package com.music.vivi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.ChartsPage
import com.music.innertube.pages.ExplorePage
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.MoodAndGenresButtonHeight
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.MoodAndGenresButton
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.youtube.YouTubeGridItem
import com.music.vivi.ui.component.media.youtube.YouTubeListItem
import com.music.vivi.ui.component.shimmer.ContainedLoadingIndicator
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.GridSnapLayoutInfoProvider
import com.music.vivi.viewmodels.ChartsViewModel
import com.music.vivi.viewmodels.ExploreViewModel

/**
 * The "Explore" tab screen.
 * Aggregates Charts, New Releases, and Moods & Genres.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
public fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val explorePage: ExplorePage? by exploreViewModel.explorePage.collectAsState()
    val chartsPage: ChartsPage? by chartsViewModel.chartsPage.collectAsState()
    val isChartsLoading: Boolean by chartsViewModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop by backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState() ?: return

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            chartsViewModel.loadCharts()
        }
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()
                )
            )

            if (isChartsLoading || chartsPage == null || explorePage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            } else {
                chartsPage?.sections?.filter { it.title != "Top music videos" }?.forEach { section ->
                    NavigationTitle(
                        title = when (section.title) {
                            "Trending" -> stringResource(R.string.trending)
                            else -> section.title ?: stringResource(R.string.charts)
                        }
                    )
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                        val lazyGridState = rememberLazyGridState()
                        val snapLayoutInfoProvider = remember(lazyGridState) {
                            GridSnapLayoutInfoProvider(
                                lazyGridState = lazyGridState,
                                positionInLayout = { layoutSize, itemSize ->
                                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                                }
                            )
                        }

                        LazyHorizontalGrid(
                            state = lazyGridState,
                            rows = GridCells.Fixed(4),
                            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                        ) {
                            items(
                                items = section.items.filterIsInstance<SongItem>().distinctBy { it.id },
                                key = { it.id }
                            ) { song ->
                                YouTubeListItem(
                                    item = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            endpoint = WatchEndpoint(videoId = song.id),
                                                            preloadItem = song.toMediaMetadata()
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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

                explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
                        onClick = {
                            navController.navigate("new_release")
                        }
                    )
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                    ) {
                        items(
                            items = newReleaseAlbums.distinctBy { it.id },
                            key = { it.id }
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }

                chartsPage?.sections?.find { it.title == "Top music videos" }?.let { topVideosSection ->
                    NavigationTitle(
                        title = stringResource(R.string.top_music_videos)
                    )
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues()
                    ) {
                        items(
                            items = topVideosSection.items.filterIsInstance<SongItem>().distinctBy { it.id },
                            key = { it.id }
                        ) { video ->
                            YouTubeGridItem(
                                item = video,
                                isActive = video.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (video.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        endpoint = WatchEndpoint(videoId = video.id),
                                                        preloadItem = video.toMediaMetadata()
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = video,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }

                explorePage?.moodAndGenres?.let { moodAndGenres ->
                    NavigationTitle(
                        title = stringResource(R.string.mood_and_genres),
                        onClick = {
                            navController.navigate("mood_and_genres")
                        }
                    )
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier.height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp)
                    ) {
                        items(moodAndGenres) {
                            MoodAndGenresButton(
                                title = it.title,
                                onClick = {
                                    navController.navigate(
                                        "youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}"
                                    )
                                },
                                modifier = Modifier
                                    .padding(6.dp)
                                    .width(180.dp)
                            )
                        }
                    }
                }
            }

            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}
