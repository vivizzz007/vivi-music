package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.ArtistFilter
import com.music.vivi.constants.ArtistFilterKey
import com.music.vivi.constants.ArtistSourceFilter
import com.music.vivi.constants.ArtistSourceFilterKey
import com.music.vivi.constants.ArtistGridViewKey
import com.music.vivi.constants.ArtistSortDescendingKey
import com.music.vivi.constants.ArtistSortType
import com.music.vivi.constants.ArtistSortTypeKey
import com.music.vivi.constants.GridItemSize
import com.music.vivi.constants.GridItemsSizeKey
import com.music.vivi.constants.GridThumbnailHeight
import androidx.compose.foundation.layout.asPaddingValues
import com.music.vivi.ui.component.ChipsRow
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LibraryArtistGridItem
import com.music.vivi.ui.component.LibraryArtistListItem
import com.music.vivi.ui.component.LibrarySearchBar
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SortDropdownMenu
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LibraryArtistsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistLibraryScreen(
    navController: NavController,
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(navController.graph.id)
    }
    val viewModel: LibraryArtistsViewModel = hiltViewModel(parentEntry)
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val artists by viewModel.allArtists.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    var sourceFilter by rememberEnumPreference(
        ArtistSourceFilterKey,
        ArtistSourceFilter.ALL
    )
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val (isGridView, setGridView) = rememberPreference(ArtistGridViewKey, true)

    val filteredArtists = remember(artists, searchQuery) {
        if (searchQuery.isBlank()) {
            artists
        } else {
            val query = searchQuery.trim()
            artists.filter { it.artist.name.contains(query, ignoreCase = true) }
        }
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        ArtistSortTypeKey,
        ArtistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)

    val keyboardController = LocalSoftwareKeyboardController.current
    
    val headerContent = @Composable {
        Column {
            LibrarySearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                keyboardController = keyboardController,
                placeholder = stringResource(R.string.search),
                modifier = Modifier.padding(bottom = 8.dp),
                isGridView = isGridView,
                onToggleViewChange = { setGridView(!isGridView) },
                sortContent = {
                    SortDropdownMenu(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { t ->
                            when (t) {
                                ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date
                                ArtistSortType.NAME -> R.string.sort_by_name
                                ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count
                                ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time
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
                        ArtistSourceFilter.ALL to "All Sources",
                        ArtistSourceFilter.LOCAL to "Local",
                        ArtistSourceFilter.YOUTUBE to "YouTube"
                    ),
                    currentValue = sourceFilter,
                    onValueUpdate = { sourceFilter = it },
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
                title = { Text(stringResource(R.string.artists)) },
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
                items(items = filteredArtists.distinctBy { it.id }, key = { it.id }) { item ->
                    LibraryArtistGridItem(
                        artist = item,
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = cPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item { headerContent() }
                itemsIndexed(items = filteredArtists.distinctBy { it.id }, key = { _, item -> item.id }) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == filteredArtists.lastIndex
                    val cardShape = if (isFirst && isLast) {
                        RoundedCornerShape(16.dp)
                    } else if (isFirst) {
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    } else if (isLast) {
                        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    } else {
                        RectangleShape
                    }
                    
                    LibraryArtistListItem(
                        artist = item,
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            }
        }
    }
}
