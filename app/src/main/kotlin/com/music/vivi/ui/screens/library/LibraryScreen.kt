package com.music.vivi.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
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
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.*
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.reversed
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.*
import com.music.vivi.ui.component.library.*
import com.music.vivi.ui.component.media.albums.AlbumListItem
import com.music.vivi.ui.component.media.artists.ArtistGridItem
import com.music.vivi.ui.component.media.artists.ArtistListItem
import com.music.vivi.ui.component.media.playlists.PlaylistGridItem
import com.music.vivi.ui.component.media.playlists.PlaylistListItem
import com.music.vivi.ui.menu.*
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

/**
 * The main Library Screen hosting the different library filters.
 * Switches between [LibraryMixScreen], [LibraryPlaylistsScreen], [LibrarySongsScreen], etc.
 * based on the selected [LibraryFilter] chip.
 */
@Composable
fun LibraryScreen(navController: NavController, onNavigate: ((String) -> Unit)? = null) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val filterContent = @Composable {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            listOf(
                LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                LibraryFilter.ARTISTS to stringResource(R.string.filter_artists)
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = filterType == filter,
                    onClick = {
                        filterType = if (filterType == filter) {
                            LibraryFilter.LIBRARY
                        } else {
                            filter
                        }
                    },
                    label = { Text(label) },
                    leadingIcon = if (filterType == filter) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = stringResource(R.string.selected_content_desc),
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent, onNavigate)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent, onNavigate)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY }
            )

            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
                onNavigate = onNavigate
            )

            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * The "Default" or "All" view of the library.
 * Displays a mixed list of Playlists, Artists, and Albums sorted by user preference.
 * Supports switching between List and Grid views.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val uploadedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.uploaded_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, true)

    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    var allItems = albums.value + artist.value + playlist.value
    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }

            MixSortType.NAME ->
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    }
                )

            MixSortType.LAST_UPDATED ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
        }.reversed(sortDescending)

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
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

    val headerContent = @Composable {
        var dropdownExpanded by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { type ->
                    when (type) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                }
            )

            // Connected toggle buttons for view type on the right
            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                // List view button
                ToggleButton(
                    checked = viewType == LibraryViewType.LIST,
                    onCheckedChange = { viewType = LibraryViewType.LIST },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.semantics { role = Role.RadioButton }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.list),
                        contentDescription = "List View",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                }
                // Grid view button
                ToggleButton(
                    checked = viewType == LibraryViewType.GRID,
                    onCheckedChange = { viewType = LibraryViewType.GRID },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.semantics { role = Role.RadioButton }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.grid_view),
                        contentDescription = "Grid View",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = likedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/liked"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/downloaded"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "top_playlist/$topSize"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = cachePlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "cache_playlist/cached"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/uploaded"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    items(
                        items = allItems.distinctBy { it.id },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
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
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val route = "local_playlist/${item.id}"
                                                if (onNavigate !=
                                                    null
                                                ) {
                                                    onNavigate(route)
                                                } else {
                                                    navController.navigate(route)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem()
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
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
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val route = "artist/${item.id}"
                                                if (onNavigate !=
                                                    null
                                                ) {
                                                    onNavigate(route)
                                                } else {
                                                    navController.navigate(route)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem()
                                )
                            }

                            is Album -> {
                                val downloadUtil = LocalDownloadUtil.current
                                val database = LocalDatabase.current
                                val downloadState by downloadUtil.getDownload(item.id).collectAsState(initial = null)
                                val albumState by database.album(item.id).collectAsState(initial = item)
                                val isFavorite = albumState?.album?.bookmarkedAt != null

                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    isFavorite = isFavorite,
                                    downloadState = downloadState?.state,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
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
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val route = "album/${item.id}"
                                                if (onNavigate !=
                                                    null
                                                ) {
                                                    onNavigate(route)
                                                } else {
                                                    navController.navigate(route)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem()
                                )
                            }

                            else -> {}
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "auto_playlist/liked"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "auto_playlist/downloaded"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "top_playlist/$topSize"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = cachePlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "cache_playlist/cached"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = uploadedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/uploaded")
                                    }
                                    .animateItem()
                            )
                        }
                    }

                    items(
                        items = allItems.distinctBy { it.id },
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistGridItem(
                                    playlist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val route = "local_playlist/${item.id}"
                                                if (onNavigate !=
                                                    null
                                                ) {
                                                    onNavigate(route)
                                                } else {
                                                    navController.navigate(route)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem()
                                )
                            }

                            is Artist -> {
                                ArtistGridItem(
                                    artist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val route = "artist/${item.id}"
                                                if (onNavigate !=
                                                    null
                                                ) {
                                                    onNavigate(route)
                                                } else {
                                                    navController.navigate(route)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem()
                                )
                            }

                            is Album -> {
                                LibraryAlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    navController = navController,
                                    menuState = menuState,
                                    onItemClick = {
                                        val route = "album/${item.id}"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    },
                                    modifier = Modifier
                                )
                            }

                            else -> {}
                        }
                    }
                }
        }
    }
}

/**
 * Displays the user's playlists (Local, Downloaded, and innerTube synced).
 * Supports sorting by name, creation date, song count, etc.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true
    )
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val playlists: List<com.music.vivi.db.entities.Playlist> by viewModel.allPlaylists.collectAsState()

    val topSize by viewModel.topValue.collectAsState(initial = 50)

    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val uploadedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.uploaded_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList()
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, true)

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
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
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

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(
                    R.plurals.n_playlist,
                    playlists.size,
                    playlists.size
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = likedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/liked"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/downloaded"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "top_playlist/$topSize"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = cachePlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "cache_playlist/cached"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/uploaded"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                            )
                        }
                    }

                    playlists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item(key = "empty_placeholder") {
                            }
                        }

                        items(
                            items = playlists.distinctBy { it.id },
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            LibraryPlaylistListItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier,
                                onItemClick = onNavigate?.let { navigate ->
                                    {
                                        if (!playlist.playlist.isEditable &&
                                            playlist.songCount == 0 &&
                                            playlist.playlist.remoteSongCount != 0
                                        ) {
                                            navigate("online_playlist/${playlist.playlist.browseId}")
                                        } else {
                                            navigate("local_playlist/${playlist.id}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "auto_playlist/liked"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "auto_playlist/downloaded"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "top_playlist/$topSize"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = cachePlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val route = "cache_playlist/cached"
                                            if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                        }
                                    )
                            )
                        }
                    }

                    if (showUploaded) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = uploadedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "auto_playlist/uploaded"
                                        if (onNavigate != null) onNavigate(route) else navController.navigate(route)
                                    }
                            )
                        }
                    }

                    playlists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                            }
                        }

                        items(
                            items = playlists.distinctBy { it.id },
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            LibraryPlaylistGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier,
                                onItemClick = onNavigate?.let { navigate ->
                                    {
                                        if (!playlist.playlist.isEditable &&
                                            playlist.songCount == 0 &&
                                            playlist.playlist.remoteSongCount != 0
                                        ) {
                                            navigate("online_playlist/${playlist.playlist.browseId}")
                                        } else {
                                            navigate("local_playlist/${playlist.id}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    }
                )
            }
        }
    }
}

/**
 * Displays all songs in the library.
 * Supports filtering by source (Liked, Library, Uploaded, Downloaded) and sorting.
 * Allows multi-selection of songs for playlist management.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val songs: List<com.music.vivi.db.entities.Song> by viewModel.allSongs.collectAsState()

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                SongFilter.UPLOADED -> viewModel.syncUploadedSongs()
                else -> return@LaunchedEffect
            }
        }
    }

    val wrappedSongs = remember { mutableStateListOf<ItemWrapper<Song>>() }
    LaunchedEffect(songs) {
        val wrappers = withContext(Dispatchers.Default) {
            songs.map { item -> ItemWrapper(item) }
        }
        wrappedSongs.clear()
        wrappedSongs.addAll(wrappers)
    }
    var selection by remember {
        mutableStateOf(false)
    }

    val lazyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val filteredSongs = remember { mutableStateListOf<ItemWrapper<Song>>() }
        LaunchedEffect(wrappedSongs.size, hideExplicit) {
            val result = withContext(Dispatchers.Default) {
                if (hideExplicit) {
                    wrappedSongs.filter { !it.item.song.explicit }
                } else {
                    wrappedSongs.toList()
                }
            }
            filteredSongs.clear()
            filteredSongs.addAll(result)
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item(
                key = "filter",
                contentType = CONTENT_TYPE_HEADER
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    FilterChip(
                        label = { Text(stringResource(R.string.songs)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        onClick = onDeselect,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = ""
                            )
                        }
                    )

                    listOf(
                        SongFilter.LIKED to stringResource(R.string.filter_liked),
                        SongFilter.LIBRARY to stringResource(R.string.filter_library),
                        SongFilter.UPLOADED to stringResource(R.string.filter_uploaded),
                        SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded)
                    ).forEach { (songFilter, label) ->
                        FilterChip(
                            selected = filter == songFilter,
                            onClick = { filter = songFilter },
                            label = { Text(label) },
                            leadingIcon = if (filter == songFilter) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = stringResource(R.string.selected_content_desc),
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

            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        IconButton(
                            onClick = { selection = false }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null
                            )
                        }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            modifier = Modifier.weight(1f)
                        )
                        RoundedCheckbox(
                            checked = count == wrappedSongs.size,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    wrappedSongs.forEach { it.isSelected = true }
                                } else {
                                    wrappedSongs.forEach { it.isSelected = false }
                                }
                            }
                        )

                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }
                                            .map { it.item },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        SongSortType.NAME -> R.string.sort_by_name
                                        SongSortType.ARTIST -> R.string.sort_by_artist
                                        SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                }
                            )

                            Spacer(Modifier.weight(1f))

                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    songs.size,
                                    songs.size
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (filteredSongs.isNotEmpty()) {
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, item -> item.item.id },
                    contentType = { _, _ -> CONTENT_TYPE_SONG }
                ) { index, songWrapper ->
                    val isFirst = index == 0
                    val isLast = index == filteredSongs.size - 1
                    val isActive = songWrapper.item.id == mediaMetadata?.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (isFirst) 20.dp else 0.dp,
                                        topEnd = if (isFirst) 20.dp else 0.dp,
                                        bottomStart = if (isLast) 20.dp else 0.dp,
                                        bottomEnd = if (isLast) 20.dp else 0.dp
                                    )
                                )
                                .background(
                                    if (isActive) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                        ) {
                            LibrarySongListItem(
                                song = songWrapper.item,
                                showInLibraryIcon = true,
                                isActive = isActive,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = songWrapper.item,
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
                                isSelected = songWrapper.isSelected,
                                inSelectionMode = selection,
                                onSelectionChange = { songWrapper.isSelected = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (songWrapper.item.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = context.getString(R.string.queue_all_songs),
                                                            items = songs.map { it.toMediaItem() },
                                                            startIndex = index
                                                        )
                                                    )
                                                }
                                            } else {
                                                songWrapper.isSelected = !songWrapper.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) {
                                                selection = true
                                            }
                                            wrappedSongs.forEach {
                                                it.isSelected = false
                                            }
                                            songWrapper.isSelected = true
                                        }
                                    )
                            )
                        }

                        // Add 3dp spacer between items (except after last)
                        if (!isLast) {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = songs.isNotEmpty() == true,
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.queue_all_songs),
                        items = songs.shuffled().map { it.toMediaItem() }
                    )
                )
            }
        )
    }
}

/**
 * Displays all albums in the library.
 * Supports filtering (Liked, Library, Uploaded) and sorting options (Artist, Year, Song Count, etc.).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        AlbumSortTypeKey,
        AlbumSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val filterContent = @Composable {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            FilterChip(
                label = { Text(stringResource(R.string.albums)) },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = onDeselect,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.close), contentDescription = "")
                }
            )

            listOf(
                AlbumFilter.LIKED to stringResource(R.string.filter_liked),
                AlbumFilter.LIBRARY to stringResource(R.string.filter_library),
                AlbumFilter.UPLOADED to stringResource(R.string.filter_uploaded)
            ).forEach { (albumFilter, label) ->
                FilterChip(
                    selected = filter == albumFilter,
                    onClick = { filter = albumFilter },
                    label = { Text(label) },
                    leadingIcon = if (filter == albumFilter) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = stringResource(R.string.selected_content_desc),
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

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    val albums: List<com.music.vivi.db.entities.Album> by viewModel.allAlbums.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date
                        AlbumSortType.NAME -> R.string.sort_by_name
                        AlbumSortType.ARTIST -> R.string.sort_by_artist
                        AlbumSortType.YEAR -> R.string.sort_by_year
                        AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count
                        AlbumSortType.LENGTH -> R.string.sort_by_length
                        AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(R.plurals.n_album, albums.size, albums.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    albums.let { albums ->
                        if (albums.isEmpty()) {
                            item(key = "empty_placeholder") {
                                EmptyPlaceholder(
                                    icon = R.drawable.album,
                                    text = stringResource(R.string.library_album_empty),
                                    modifier = Modifier
                                )
                            }
                        }

                        val filteredAlbumsForList = if (hideExplicit) {
                            albums.filter { !it.album.explicit }
                        } else {
                            albums
                        }

                        if (filteredAlbumsForList.isNotEmpty()) {
                            itemsIndexed(
                                items = filteredAlbumsForList.distinctBy { it.id },
                                key = { _, album -> album.id }
                            ) { index, album ->
                                val isFirst = index == 0
                                val isLast = index == filteredAlbumsForList.distinctBy { it.id }.size - 1
                                val isActive = album.id == mediaMetadata?.album?.id

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = if (isFirst) 20.dp else 0.dp,
                                                    topEnd = if (isFirst) 20.dp else 0.dp,
                                                    bottomStart = if (isLast) 20.dp else 0.dp,
                                                    bottomEnd = if (isLast) 20.dp else 0.dp
                                                )
                                            )
                                            .background(
                                                if (isActive) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainer
                                                }
                                            )
                                    ) {
                                        LibraryAlbumListItem(
                                            navController = navController,
                                            menuState = menuState,
                                            album = album,
                                            isActive = isActive,
                                            isPlaying = isPlaying,
                                            modifier = Modifier.fillMaxSize(),
                                            onItemClick = onNavigate?.let { navigate ->
                                                { navigate("album/${album.id}") }
                                            }
                                        )
                                    }

                                    // Add 3dp spacer between items (except after last)
                                    if (!isLast) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                    }
                                }
                            }
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    albums.let { albums ->
                        if (albums.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.album,
                                    text = stringResource(R.string.library_album_empty),
                                    modifier = Modifier
                                )
                            }
                        }

                        val filteredAlbumsForGrid = if (hideExplicit) {
                            albums.filter { !it.album.explicit }
                        } else {
                            albums
                        }
                        items(
                            items = filteredAlbumsForGrid.distinctBy { it.id },
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ALBUM }
                        ) { album ->
                            LibraryAlbumGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                album = album,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier,
                                onItemClick = onNavigate?.let { navigate -> { navigate("album/${album.id}") } }
                            )
                        }
                    }
                }
        }
    }
}

/**
 * Displays all artists in the library.
 * Supports filtering (Liked, Library) and sorting options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID)

    var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        ArtistSortTypeKey,
        ArtistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val filterContent = @Composable {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            FilterChip(
                label = { Text(stringResource(R.string.artists)) },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = onDeselect,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.close), contentDescription = "")
                }
            )

            listOf(
                ArtistFilter.LIKED to stringResource(R.string.filter_liked),
                ArtistFilter.LIBRARY to stringResource(R.string.filter_library)
            ).forEach { (artistFilter, label) ->
                FilterChip(
                    selected = filter == artistFilter,
                    onClick = { filter = artistFilter },
                    label = { Text(label) },
                    leadingIcon = if (filter == artistFilter) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = stringResource(R.string.selected_content_desc),
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
    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    val artists: List<com.music.vivi.db.entities.Artist> by viewModel.allArtists.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        ArtistSortType.NAME -> R.string.sort_by_name
                        ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count
                        ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            artists?.let { artists ->
                Text(
                    text = pluralStringResource(
                        R.plurals.n_artist,
                        artists.size,
                        artists.size
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    artists.let { artists ->
                        if (artists.isEmpty()) {
                            item(key = "empty_placeholder") {
                                EmptyPlaceholder(
                                    icon = R.drawable.artist,
                                    text = stringResource(R.string.library_artist_empty),
                                    modifier = Modifier
                                )
                            }
                        }

                        val distinctArtists = artists.distinctBy { it.id }
                        if (distinctArtists.isNotEmpty()) {
                            itemsIndexed(
                                items = distinctArtists,
                                key = { _, artist -> artist.id },
                                contentType = { _, _ -> CONTENT_TYPE_ARTIST }
                            ) { index, artist ->
                                val isFirst = index == 0
                                val isLast = index == distinctArtists.size - 1

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = if (isFirst) 20.dp else 0.dp,
                                                    topEnd = if (isFirst) 20.dp else 0.dp,
                                                    bottomStart = if (isLast) 20.dp else 0.dp,
                                                    bottomEnd = if (isLast) 20.dp else 0.dp
                                                )
                                            )
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        LibraryArtistListItem(
                                            artist = artist,
                                            onArtistClick = {
                                                if (onNavigate != null) {
                                                    onNavigate("artist/${artist.id}")
                                                } else {
                                                    navController.navigate("artist/${artist.id}")
                                                }
                                            },
                                            onMenuClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = artist,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Add 3dp spacer between items (except after last)
                                    if (!isLast) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                    }
                                }
                            }
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    artists.let { artists ->
                        if (artists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.artist,
                                    text = stringResource(R.string.library_artist_empty),
                                    modifier = Modifier
                                )
                            }
                        }

                        items(
                            items = artists.distinctBy { it.id },
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ARTIST }
                        ) { artist ->
                            LibraryArtistGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier,
                                artist = artist,
                                onItemClick = onNavigate?.let { navigate -> { navigate("artist/${artist.id}") } }
                            )
                        }
                    }
                }
        }
    }
}
