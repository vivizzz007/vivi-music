package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.music.vivi.constants.AlbumFilter
import com.music.vivi.constants.AlbumFilterKey
import com.music.vivi.ui.component.ChipsRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.constants.AlbumGridViewKey
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.ui.component.AlbumGridItem
import com.music.vivi.ui.component.AlbumListItem
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.viewmodels.LibraryAlbumsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumLibraryScreen(
    navController: NavController,
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(navController.graph.id)
    }
    val viewModel: LibraryAlbumsViewModel = hiltViewModel(parentEntry)
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val isPlaying by playerConnection?.isEffectivelyPlaying?.collectAsState(false) ?: mutableStateOf(false)
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState(null) ?: mutableStateOf(null)

    val albums by viewModel.allAlbums.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.ALL)
    
    LaunchedEffect(filter) {
        // Nothing here now that All is natively supported
    }
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val (isGridView, setGridView) = rememberPreference(AlbumGridViewKey, true)

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) {
            albums
        } else {
            val query = searchQuery.trim()
            albums.filter { it.album.title.contains(query, ignoreCase = true) || it.artists.any { artist -> artist.name.contains(query, ignoreCase = true) } }
        }
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        com.music.vivi.constants.AlbumSortTypeKey,
        com.music.vivi.constants.AlbumSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(com.music.vivi.constants.AlbumSortDescendingKey, true)

    val keyboardController = LocalSoftwareKeyboardController.current
    
    val headerContent = @Composable {
        Column {
            LibrarySearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.searchQuery.value = it },
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                keyboardController = keyboardController,
                placeholder = stringResource(R.string.search_albums),
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
                                com.music.vivi.constants.AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date
                                com.music.vivi.constants.AlbumSortType.NAME -> R.string.sort_by_name
                                com.music.vivi.constants.AlbumSortType.ARTIST -> R.string.sort_by_artist
                                com.music.vivi.constants.AlbumSortType.YEAR -> R.string.sort_by_year
                                com.music.vivi.constants.AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count
                                com.music.vivi.constants.AlbumSortType.LENGTH -> R.string.sort_by_length
                                com.music.vivi.constants.AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time
                            }
                        }
                    )
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                ChipsRow(
                    chips = listOf(
                        AlbumFilter.ALL to stringResource(R.string.all),
                        AlbumFilter.LIKED to stringResource(R.string.filter_liked),
                        AlbumFilter.LIBRARY to stringResource(R.string.filter_library)
                    ),
                    currentValue = filter,
                    onValueUpdate = { filter = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.albums)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }, onLongClick = {}) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
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
                items(items = filteredAlbums, key = { it.id }) { item ->
                    AlbumGridItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        coroutineScope = coroutineScope,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = item,
                                            navController = navController,
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
                itemsIndexed(items = filteredAlbums, key = { _, item -> item.id }) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == filteredAlbums.lastIndex
                    val cardShape = if (isFirst && isLast) {
                        RoundedCornerShape(16.dp)
                    } else if (isFirst) {
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    } else if (isLast) {
                        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    } else {
                        RectangleShape
                    }
                    
                    AlbumListItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        shape = cardShape,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        horizontalPadding = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("album/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = item,
                                            navController = navController,
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
