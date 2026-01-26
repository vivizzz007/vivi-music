package com.music.vivi.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.music.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.SearchFilterHeight
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.youtube.YouTubeListItem
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Component that displays the results of an online (YouTube Music) search.
 * Shows a list of items (Songs, Videos, Albums, Artists, Playlists) matching the query.
 * Supports filtering by type.
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
public fun OnlineSearchResult(navController: NavController, viewModel: OnlineSearchViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val isLoading = viewModel.isLoading
    val error = viewModel.error

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable (YTItem, Boolean, Boolean) -> Unit = {
            item: YTItem,
            isFirst: Boolean,
            isLast: Boolean,
        ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )

                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )

                    is ArtistItem ->
                        YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss
                        )

                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                }
            }
        }

        val isActive = when (item) {
            is SongItem -> mediaMetadata?.id == item.id
            is AlbumItem -> mediaMetadata?.album?.id == item.id
            else -> false
        }

        val cornerRadius = remember { 24.dp }

        val topShape = remember(cornerRadius) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = cornerRadius,
                smoothnessAsPercentBR = 0,
                cornerRadiusBR = 0.dp,
                smoothnessAsPercentTL = 60,
                cornerRadiusTL = cornerRadius,
                smoothnessAsPercentBL = 0,
                cornerRadiusBL = 0.dp,
                smoothnessAsPercentTR = 60
            )
        }
        val middleShape = remember { RectangleShape }
        val bottomShape = remember(cornerRadius) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = 0.dp,
                smoothnessAsPercentBR = 60,
                cornerRadiusBR = cornerRadius,
                smoothnessAsPercentTL = 0,
                cornerRadiusTL = 0.dp,
                smoothnessAsPercentBL = 60,
                cornerRadiusBL = cornerRadius,
                smoothnessAsPercentTR = 0
            )
        }
        val singleShape = remember(cornerRadius) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = cornerRadius,
                smoothnessAsPercentBR = 60,
                cornerRadiusBR = cornerRadius,
                smoothnessAsPercentTL = 60,
                cornerRadiusTL = cornerRadius,
                smoothnessAsPercentBL = 60,
                cornerRadiusBL = cornerRadius,
                smoothnessAsPercentTR = 60
            )
        }

        val shape = remember(isFirst, isLast, cornerRadius) {
            when {
                isFirst && isLast -> singleShape
                isFirst -> topShape
                isLast -> bottomShape
                else -> middleShape
            }
        }

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
            YouTubeListItem(
                item = item,
                isActive = isActive,
                isPlaying = isPlaying,
                drawHighlight = false,
                trailingContent = {
                    IconButton(
                        onClick = longClick
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
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata()
                                            )
                                        )
                                    }
                                }

                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            }
                        },
                        onLongClick = longClick
                    )
            )
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
        LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = SearchFilterHeight + 25.dp))
            .add(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .asPaddingValues()
    ) {
        if (searchFilter == null) {
            searchSummary?.summaries?.forEach { summary ->
                item {
                    NavigationTitle(
                        when (summary.title) {
                            "Top result" -> stringResource(R.string.top_result)
                            "Other" -> stringResource(R.string.other)
                            else -> summary.title
                        }
                    )
                }

                // Group items in a container
                item(key = "${summary.title}_container") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        summary.items.forEachIndexed { index, item ->
                            val isFirst = index == 0
                            val isLast = index == summary.items.size - 1

                            ytItemContent(item, isFirst, isLast)

                            // Add 3dp spacer between items (except after last)
                            if (!isLast) {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }

            if (searchSummary?.summaries?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found)
                    )
                }
            }
        } else {
            val distinctItems = itemsPage?.items.orEmpty().distinctBy { it.id }

            if (distinctItems.isNotEmpty()) {
                item(key = "filtered_container") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        distinctItems.forEachIndexed { index, item ->
                            val isFirst = index == 0
                            val isLast = index == distinctItems.size - 1 && itemsPage?.continuation == null

                            ytItemContent(item, isFirst, isLast)

                            // Add 3dp spacer between items (except after last)
                            if (!isLast) {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found)
                    )
                }
            }
        }

        if (isLoading &&
            searchFilter == null &&
            searchSummary == null ||
            isLoading &&
            searchFilter != null &&
            itemsPage == null
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }

        if (error != null) {
            item {
                EmptyPlaceholder(
                    icon = R.drawable.search,
                    text = stringResource(R.string.error_unknown),
                    trailingContent = {
                        androidx.compose.material3.Button(
                            onClick = viewModel::retry,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Replace the ChipsRow at the bottom with this:
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(top = AppBarHeight))
            )
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            null to stringResource(R.string.filter_all),
            FILTER_SONG to stringResource(R.string.filter_songs),
            FILTER_VIDEO to stringResource(R.string.filter_videos),
            FILTER_ALBUM to stringResource(R.string.filter_albums),
            FILTER_ARTIST to stringResource(R.string.filter_artists),
            FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
            FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists)
        ).forEach { (filter, label) ->
            FilterChip(
                selected = searchFilter == filter,
                onClick = {
                    if (viewModel.filter.value != filter) {
                        viewModel.filter.value = filter
                    }
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                label = { Text(label) },
                leadingIcon = if (searchFilter == filter) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
