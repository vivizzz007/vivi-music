/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.CONTENT_TYPE_HEADER
import com.music.vivi.constants.CONTENT_TYPE_PLAYLIST
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.MixSortDescendingKey
import com.music.vivi.constants.MixSortType
import com.music.vivi.constants.MixSortTypeKey
import com.music.vivi.constants.PlaylistGridViewKey
import com.music.vivi.constants.LibraryMixIsGridViewKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowCommentButtonKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.extensions.reversed
import com.music.vivi.ui.component.LibraryAlbumGridItem
import com.music.vivi.ui.component.LibraryArtistGridItem
import com.music.vivi.ui.component.LibraryPlaylistGridItem
import com.music.vivi.ui.component.LibraryAlbumListItem
import com.music.vivi.ui.component.LibraryArtistListItem
import com.music.vivi.ui.component.LibraryPlaylistListItem
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.LibrarySearchBar
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlaylistGridItem
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

enum class LibraryFilterType { ALL, ARTIST, ALBUM, PLAYLIST }

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun LibraryMixScreen(
    navController: NavController,
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(navController.graph.id)
    }
    val viewModel: LibraryMixViewModel = hiltViewModel(parentEntry)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val recentLikedThumbnails by viewModel.recentLikedThumbnails.collectAsState()
    val recentDownloadedThumbnails by viewModel.recentDownloadedThumbnails.collectAsState()
    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)


    val filteredItems by viewModel.filteredUiItems.collectAsState()

    var activeFilter by rememberSaveable { mutableStateOf(LibraryFilterType.ALL) }
    
    val finalFilteredItems = remember(filteredItems, activeFilter) {
        when (activeFilter) {
            LibraryFilterType.ALL -> filteredItems
            LibraryFilterType.ARTIST -> filteredItems.filterIsInstance<Artist>()
            LibraryFilterType.ALBUM -> filteredItems.filterIsInstance<Album>()
            LibraryFilterType.PLAYLIST -> filteredItems.filterIsInstance<Playlist>()
        }
    }

    val isGridView = rememberPreference(LibraryMixIsGridViewKey, true)
    
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    val visibleStaticItems = remember(showLiked, showDownloaded, showTop, showCached, activeFilter) {
        buildList {
            if (activeFilter == LibraryFilterType.ALL) {
                if (showLiked) add("liked")
                if (showDownloaded && activeFilter == LibraryFilterType.ALL) add("downloaded")
                if (showTop) add("top")
                if (showCached) add("cached")
            }
        }
    }

    val totalListItems = visibleStaticItems.size + (if (!isGridView.value) finalFilteredItems.size else 0)
    val getShapeForIndex: (Int) -> Shape = { index ->
        val size = totalListItems
        when {
            size == 1 -> RoundedCornerShape(12.dp)
            size == 0 -> RoundedCornerShape(0.dp)
            index == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
            index == size - 1 -> RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
            else -> RoundedCornerShape(2.dp)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
         if (ytmSync) {
             withContext(Dispatchers.IO) {
                 viewModel.syncAllLibrary()
             }
         }
    }

    // Periodic auto-sync every 5 minutes while screen is open
    LaunchedEffect(ytmSync) {
        if (ytmSync) {
            while (true) {
                delay(60 * 1000L) // 1 minute
                viewModel.forceRefresh()
            }
        }
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.forceRefresh() },
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isSyncing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding())
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = if (isGridView.value) {
                         GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                         )
                    } else {
                         GridCells.Fixed(1)
                    },
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    /*item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }*/

                    /*item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }*/
                        item(
                            key = "search_bar",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            LibrarySearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.searchQuery.value = it },
                                isSearchActive = isSearchActive,
                                onSearchActiveChange = { isSearchActive = it },
                                isGridView = isGridView.value,
                                onToggleViewChange = { isGridView.value = !isGridView.value },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                sortContent = {
                                    com.music.vivi.ui.component.SortDropdownMenu(
                                        sortType = sortType,
                                        sortDescending = sortDescending,
                                        onSortTypeChange = onSortTypeChange,
                                        onSortDescendingChange = onSortDescendingChange,
                                        sortTypeText = { t ->
                                            when (t) {
                                                MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                                                MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                                                MixSortType.NAME -> R.string.sort_by_name
                                            }
                                        }
                                    )
                                }
                            )
                        }

                        item(
                            key = "filter_chips",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                items(LibraryFilterType.entries.toTypedArray()) { filterType ->
                                    androidx.compose.material3.FilterChip(
                                        selected = activeFilter == filterType,
                                        onClick = { activeFilter = filterType },
                                        label = {
                                            androidx.compose.material3.Text(
                                                text = when (filterType) {
                                                    LibraryFilterType.ALL -> stringResource(R.string.all) 
                                                    LibraryFilterType.ARTIST -> stringResource(R.string.artists)
                                                    LibraryFilterType.ALBUM -> stringResource(R.string.albums)
                                                    LibraryFilterType.PLAYLIST -> stringResource(R.string.playlists)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    
                    if (showLiked && activeFilter == LibraryFilterType.ALL) {
                        item(
                            key = "likedPlaylist",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "stat_card",
                        ) {
                            Box(
                                modifier = Modifier
                                    // Liked container padding logic: 12dp in Grid, 16dp horizontal w/ 2dp vertical in List
                                    .padding(
                                        horizontal = if (isGridView.value) 12.dp else 16.dp, 
                                        vertical = if (isGridView.value) 12.dp else 2.dp
                                    )
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(if (isGridView.value) RoundedCornerShape(12.dp) else getShapeForIndex(visibleStaticItems.indexOf("liked")))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable(onClick = { navController.navigate("auto_playlist/liked") })
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(MaterialShapes.Cookie4Sided.toShape())
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.favorite_border),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(16.dp))
                                        androidx.compose.foundation.layout.Column {
                                            androidx.compose.material3.Text(
                                                text = stringResource(R.string.liked),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    if (recentLikedThumbnails.isNotEmpty()) {
                                        val displayThumbnails = recentLikedThumbnails.take(5)
                                        var currentIndex by remember { mutableIntStateOf(0) }
                                        
                                        if (displayThumbnails.size > 1) {
                                            LaunchedEffect(displayThumbnails) {
                                                while (true) {
                                                    delay(3000)
                                                    currentIndex = (currentIndex + 1) % displayThumbnails.size
                                                }
                                            }
                                        }

                                        Crossfade(
                                            targetState = currentIndex,
                                            animationSpec = tween(1000),
                                            label = "LikedImageCrossfade"
                                        ) { index ->
                                            AsyncImage(
                                                model = displayThumbnails[index],
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(width = 160.dp, height = 64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showDownloaded && activeFilter == LibraryFilterType.ALL) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            if (isGridView.value) {
                                PlaylistGridItem(
                                    playlist = downloadPlaylist,
                                    fillMaxWidth = true,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("auto_playlist/downloaded") },
                                        ).animateItem(),
                                )
                            } else {
                                PlaylistListItem(
                                    playlist = downloadPlaylist,
                                    shape = getShapeForIndex(visibleStaticItems.indexOf("downloaded")),
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate("auto_playlist/downloaded") }
                                        .animateItem(),
                                )
                            }
                        }
                    }

                    if (showTop && activeFilter == LibraryFilterType.ALL) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            if (isGridView.value) {
                                PlaylistGridItem(
                                    playlist = topPlaylist,
                                    fillMaxWidth = true,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("top_playlist/$topSize") },
                                        ).animateItem(),
                                )
                            } else {
                                PlaylistListItem(
                                    playlist = topPlaylist,
                                    shape = getShapeForIndex(visibleStaticItems.indexOf("top")),
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate("top_playlist/$topSize") }
                                        .animateItem(),
                                )
                            }
                        }
                    }

                    if (showCached && activeFilter == LibraryFilterType.ALL) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            if (isGridView.value) {
                                PlaylistGridItem(
                                    playlist = cachePlaylist,
                                    fillMaxWidth = true,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("cache_playlist/cached") },
                                        ).animateItem(),
                                )
                            } else {
                                PlaylistListItem(
                                    playlist = cachePlaylist,
                                    shape = getShapeForIndex(visibleStaticItems.indexOf("cached")),
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate("cache_playlist/cached") }
                                        .animateItem(),
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = finalFilteredItems,
                        key = { _, item ->
                            when (item) {
                                is Playlist -> item.id
                                is Album -> item.id
                                is Artist -> item.id
                                else -> item.hashCode()
                            }
                        }
                    ) { index, item ->
                        val modifier = Modifier.animateItem().fillMaxWidth()
                        val dynamicShape = getShapeForIndex(visibleStaticItems.size + index)
                        when (item) {
                            is Playlist -> {
                                if (isGridView.value) {
                                    LibraryPlaylistGridItem(
                                        playlist = item,
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        modifier = modifier
                                    )
                                } else {
                                    LibraryPlaylistListItem(
                                        playlist = item,
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        shape = dynamicShape,
                                        modifier = modifier
                                    )
                                }
                            }
                            is Album -> {
                                if (isGridView.value) {
                                    LibraryAlbumGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        album = item,
                                        isActive = item.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying,
                                        modifier = modifier
                                    )
                                } else {
                                    LibraryAlbumListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        album = item,
                                        isActive = item.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying,
                                        shape = dynamicShape,
                                        modifier = modifier
                                    )
                                }
                            }
                            is Artist -> {
                                if (isGridView.value) {
                                    LibraryArtistGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        artist = item,
                                        modifier = modifier
                                    )
                                } else {
                                    LibraryArtistListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        artist = item,
                                        shape = dynamicShape,
                                        modifier = modifier
                                    )
                                }
                            }
                        }
                    }
                }
    }
}
