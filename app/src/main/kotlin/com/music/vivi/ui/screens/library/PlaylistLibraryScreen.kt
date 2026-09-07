package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.music.vivi.ui.component.LibrarySearchBar
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.constants.PlaylistGridViewKey
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.ui.component.PlaylistGridItem
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.menu.PlaylistMenu
import com.music.vivi.viewmodels.LibraryPlaylistsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistLibraryScreen(
    navController: NavController,
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(navController.graph.id)
    }
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel(parentEntry)
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    
    val playlists by viewModel.allPlaylists.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val (isGridView, setGridView) = rememberPreference(PlaylistGridViewKey, true)
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) {
            playlists
        } else {
            val query = searchQuery.trim()
            playlists.filter { it.playlist.name.contains(query, ignoreCase = true) }
        }
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        com.music.vivi.constants.PlaylistSortTypeKey,
        com.music.vivi.constants.PlaylistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(com.music.vivi.constants.PlaylistSortDescendingKey, true)

    val headerContent = @Composable {
        LibrarySearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.searchQuery.value = it },
            isSearchActive = isSearchActive,
            onSearchActiveChange = { isSearchActive = it },
            keyboardController = keyboardController,
            placeholder = stringResource(R.string.search),
            modifier = Modifier.padding(bottom = 8.dp),
            isGridView = isGridView,
            onToggleViewChange = { setGridView(!isGridView) },
            sortContent = {
                com.music.vivi.ui.component.SortDropdownMenu(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = { t ->
                        when (t) {
                            com.music.vivi.constants.PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                            com.music.vivi.constants.PlaylistSortType.NAME -> R.string.sort_by_name
                            com.music.vivi.constants.PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                            com.music.vivi.constants.PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        }
                    }
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlists)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }, onLongClick = {}) {
                        androidx.compose.material3.Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val playerPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        val cPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = playerPadding.calculateBottomPadding() + 12.dp,
            start = 12.dp,
            end = 12.dp
        )

        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                contentPadding = cPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { headerContent() }
                items(items = filteredPlaylists, key = { it.playlist.id }) { item ->
                    PlaylistGridItem(
                        playlist = item,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("local_playlist/${item.playlist.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        PlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                }
                            )
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = cPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item { headerContent() }
                itemsIndexed(items = filteredPlaylists, key = { _, item -> item.playlist.id }) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == filteredPlaylists.lastIndex
                    val cardShape = if (isFirst && isLast) {
                        RoundedCornerShape(16.dp)
                    } else if (isFirst) {
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    } else if (isLast) {
                        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    } else {
                        RectangleShape
                    }
                    
                    PlaylistListItem(
                        playlist = item,
                        shape = cardShape,
                        horizontalPadding = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("local_playlist/${item.playlist.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        PlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
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
