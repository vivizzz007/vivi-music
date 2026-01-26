package com.music.vivi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.ChartsPage
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.youtube.YouTubeGridItem
import com.music.vivi.ui.component.media.youtube.YouTubeListItem
import com.music.vivi.ui.component.shimmer.ContainedLoadingIndicator
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.GridSnapLayoutInfoProvider
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.viewmodels.ChartsViewModel

/**
 * Screen displaying Music Charts (Top Songs, Top Videos, Trending, etc.).
 * Fetches data via [ChartsViewModel].
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
public fun ChartsScreen(navController: NavController, viewModel: ChartsViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val chartsPage: ChartsPage? by viewModel.chartsPage.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val topMusicVideosText = stringResource(R.string.top_music_videos)

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            viewModel.loadCharts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.charts)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = { navController.backToMain() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading || chartsPage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        .asPaddingValues()
                ) {
                    chartsPage?.sections?.filter { it.title != topMusicVideosText }?.forEach { section ->
                        item(key = "section_title_${section.title}") {
                            NavigationTitle(
                                title = when (section.title) {
                                    "Trending" -> stringResource(R.string.trending)
                                    else -> section.title ?: stringResource(R.string.charts)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                        item(key = "section_content_${section.title}") {
                            val lazyItemScope = this
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >=
                                    320.dp
                                ) {
                                    0.475f
                                } else {
                                    0.9f
                                }
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
                                    modifier = with(lazyItemScope) { Modifier.animateItem() }
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
                    }

                    chartsPage?.sections?.find { it.title == topMusicVideosText }?.let { topVideosSection ->
                        item(key = "top_videos_title") {
                            NavigationTitle(
                                title = stringResource(R.string.top_music_videos),
                                modifier = Modifier.animateItem()
                            )
                        }
                        item(key = "top_videos_content") {
                            LazyRow(
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier.animateItem()
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
                    }
                }
            }
        }
    }
}
