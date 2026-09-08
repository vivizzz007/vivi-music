/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.CONTENT_TYPE_HEADER
import com.music.vivi.constants.CONTENT_TYPE_PLAYLIST
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.PlaylistSortDescendingKey
import com.music.vivi.constants.PlaylistSortType
import com.music.vivi.constants.PlaylistSortTypeKey
import com.music.vivi.constants.ShowCachedPlaylistKey
import com.music.vivi.constants.ShowDownloadedPlaylistKey
import com.music.vivi.constants.ShowLikedPlaylistKey
import com.music.vivi.constants.ShowTopPlaylistKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.ui.component.CreatePlaylistDialog
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.LibraryPlaylistGridItem
import com.music.vivi.ui.component.LibraryPlaylistListItem
import com.music.vivi.ui.component.LibrarySearchBar
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlaylistGridItem
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.component.StatCard
import com.music.vivi.ui.component.StatCardsSection
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true
    )
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState(initial = "")

    val playlists by viewModel.allPlaylists.collectAsState()

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) {
            playlists
        } else {
            val query = searchQuery.trim()
            playlists.filter { it.playlist.name.contains(query, ignoreCase = true) }
        }
    }

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val recentLikedThumbnails by viewModel.recentLikedThumbnails.collectAsState()
    val recentDownloadedThumbnails by viewModel.recentDownloadedThumbnails.collectAsState()
    val recentAlbumsThumbnails by viewModel.recentAlbumsThumbnails.collectAsState()

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

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing,
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }



    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
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
                        contentType = "search_bar",
                    ) {
                        LibrarySearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.searchQuery.value = it },
                            isSearchActive = isSearchActive,
                            onSearchActiveChange = { isSearchActive = it },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            sortContent = {
                                com.music.vivi.ui.component.SortDropdownMenu(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            PlaylistSortType.NAME -> R.string.sort_by_name
                                            PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                                            PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                                        }
                                    }
                                )
                            }
                        )
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = "stat_card",
                        ) {
                            StatCard(
                                title = stringResource(R.string.liked),
                                icon = painterResource(R.drawable.likes),
                                iconTint = Color.Unspecified,
                                thumbnails = recentLikedThumbnails,
                                onClick = { navController.navigate("auto_playlist/liked") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = "stat_card",
                        ) {
                            StatCard(
                                title = stringResource(R.string.offline),
                                icon = painterResource(R.drawable.downloadedstore),
                                iconTint = Color.Unspecified,
                                thumbnails = recentDownloadedThumbnails,
                                onClick = { navController.navigate("auto_playlist/downloaded") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            )
                        }
                    }

                    item(
                        key = "albumsStatCard",
                        contentType = "stat_card",
                    ) {
                        StatCard(
                            title = stringResource(R.string.albums),
                            icon = painterResource(R.drawable.album_library),
                            iconTint = Color.Unspecified,
                            thumbnails = recentAlbumsThumbnails,
                            onClick = { navController.navigate("album_library") },
                            modifier = Modifier.fillMaxWidth().animateItem()
                        )
                    }

                    item(
                        key = "playlistsStatCard",
                        contentType = "stat_card",
                    ) {
                        StatCard(
                            title = stringResource(R.string.playlists),
                            icon = painterResource(R.drawable.playlist_library),
                            iconTint = Color.Unspecified,
                            thumbnails = emptyList(),
                            onClick = { navController.navigate("playlist_library") },
                            modifier = Modifier.fillMaxWidth().animateItem()
                        )
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = "stat_card",
                        ) {
                            StatCard(
                                title = stringResource(R.string.my_top) + " $topSize",
                                countText = topSize.toString(),
                                icon = painterResource(R.drawable.trending_up),
                                thumbnails = topPlaylist?.thumbnails ?: emptyList(),
                                onClick = { navController.navigate("top_playlist/$topSize") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = "stat_card",
                        ) {
                            StatCard(
                                title = stringResource(R.string.cached_playlist),
                                icon = painterResource(R.drawable.cached),
                                thumbnails = cachePlaylist?.thumbnails ?: emptyList(),
                                onClick = { navController.navigate("cache_playlist/cached") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            )
                        }
                    }



                    filteredPlaylists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                            }
                        }

                        items(
                            items = playlists.distinctBy { it.id },
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { playlist ->
                            LibraryPlaylistGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    },
                )
    }
}
