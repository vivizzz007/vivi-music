package com.music.vivi.ui.screens.library

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.innertube.utils.parseCookieString
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AutoPlaylistCachedPlaylistShowKey
import com.music.vivi.constants.AutoPlaylistDownloadShowKey
import com.music.vivi.constants.AutoPlaylistLikedShowKey
import com.music.vivi.constants.AutoPlaylistLocalPlaylistShowKey
import com.music.vivi.constants.AutoPlaylistTopPlaylistShowKey
import com.music.vivi.constants.AutoSyncLocalSongsKey
import com.music.vivi.constants.CONTENT_TYPE_HEADER
import com.music.vivi.constants.CONTENT_TYPE_PLAYLIST
import com.music.vivi.constants.ChipSortTypeKey
import com.music.vivi.constants.GridThumbnailHeight
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.LibraryFilter
import com.music.vivi.constants.MixSortDescendingKey
import com.music.vivi.constants.MixSortType
import com.music.vivi.constants.MixSortTypeKey
import com.music.vivi.constants.MixViewType
import com.music.vivi.constants.MixViewTypeKey
import com.music.vivi.constants.ScannerSensitivity
import com.music.vivi.constants.ScannerSensitivityKey
import com.music.vivi.constants.ScannerStrictExtKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.extensions.reversed
import com.music.vivi.ui.component.AlbumGridItem
import com.music.vivi.ui.component.AlbumListItem
import com.music.vivi.ui.component.ArtistGridItem
import com.music.vivi.ui.component.ArtistListItem
import com.music.vivi.ui.component.ChipsRow
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.PlaylistGridItem
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.ArtistMenu
import com.music.vivi.ui.menu.PlaylistMenu
import com.music.vivi.ui.utils.scanLocal
import com.music.vivi.ui.utils.syncDB
import com.music.vivi.utils.isInternetAvailable
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips =
                    listOf(
                        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                        LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    var viewType by rememberEnumPreference(MixViewTypeKey, MixViewType.GRID)

    val (autoPlaylistLiked) = rememberPreference(AutoPlaylistLikedShowKey, defaultValue = true)
    val (autoPlaylistDownload) = rememberPreference(
        AutoPlaylistDownloadShowKey, defaultValue = true
    )
    val (autoPlaylistTopPlaylist) = rememberPreference(
        AutoPlaylistTopPlaylistShowKey, defaultValue = true
    )
    val (autoPlaylistCached) = rememberPreference(
        AutoPlaylistCachedPlaylistShowKey, defaultValue = true
    )
    val (autoPlaylistLocal) = rememberPreference(
        AutoPlaylistLocalPlaylistShowKey, defaultValue = true
    )

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }

    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadSongs by viewModel.downloadSongs.collectAsState(initial = null)
    val topSongs by viewModel.topSongs.collectAsState(initial = null)
    val localSongsCount by viewModel.localSongsCount.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()
    val topSize = 50
    val likedPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = stringResource(R.string.liked)
        ),
        songCount = if (likedSongs != null) likedSongs!!.size else 0,
        thumbnails = emptyList()
    )
    val downloadPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = stringResource(R.string.offline)
        ),
        songCount = if (downloadSongs != null) downloadSongs!!.size else 0,
        thumbnails = emptyList()
    )
    val topSizeInt = topSize.toString().toInt()
    if (topSongs != null)
        println(topSongs?.size)

    val topPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = stringResource(R.string.my_top)
        ),
        songCount = topSongs?.let { minOf(it.size, topSizeInt) } ?: 0,
        thumbnails = emptyList()
    )
    val localPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = stringResource(R.string.local)
        ),
        songCount = localSongsCount,
        thumbnails = emptyList()
    )

    val cachedPlaylist = Playlist(
        playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = stringResource(R.string.cached)
        ),
        songCount = cachedSongs.size,
        thumbnails = emptyList()
    )

    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var isScannerActive by remember { mutableStateOf(false) }
    var isScanFinished by remember { mutableStateOf(false) }
    var mediaPermission by remember { mutableStateOf(true) }
    val mediaPermissionLevel =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    val (autoSyncLocalSongs) = rememberPreference(
        key = AutoSyncLocalSongsKey,
        defaultValue = true
    )
    val (scannerSensitivity) = rememberEnumPreference(
        key = ScannerSensitivityKey,
        defaultValue = ScannerSensitivity.LEVEL_2
    )
    val (strictExtensions) = rememberPreference(ScannerStrictExtKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                MixViewType.LIST -> lazyListState.animateScrollToItem(0)
                MixViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        if (ytmSync && isLoggedIn && isInternetAvailable(context)) {
            withContext(Dispatchers.IO) {
                viewModel.syncAllLibrary()
            }
        }
    }

    if (autoSyncLocalSongs) {
        LaunchedEffect(Unit) {
            if (isScannerActive) {
                return@LaunchedEffect
            }
            if (context.checkSelfPermission(mediaPermissionLevel)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    context as Activity,
                    arrayOf(mediaPermissionLevel), PackageManager.PERMISSION_GRANTED
                )
                mediaPermission = false
                return@LaunchedEffect
            } else if (context.checkSelfPermission(mediaPermissionLevel)
                == PackageManager.PERMISSION_GRANTED
            ) {
                mediaPermission = true
            }
            isScanFinished = false
            isScannerActive = true
            coroutineScope.launch(Dispatchers.IO) {
                val directoryStructure = scanLocal(context).value
                syncDB(database, directoryStructure.toList(), scannerSensitivity, strictExtensions)
                isScannerActive = false
                isScanFinished = true
            }
        }
    }

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
                    },
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

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when (viewType) {
                            MixViewType.LIST -> R.drawable.list
                            MixViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }
    if (viewType == MixViewType.LIST) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!autoPlaylistLiked && !autoPlaylistDownload && !autoPlaylistTopPlaylist && !autoPlaylistCached && !autoPlaylistLocal && allItems.isEmpty()) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(1),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.library_music_outlined,
                            text = stringResource(R.string.library_empty),
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(1),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (autoPlaylistLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = likedPlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }

                    if (autoPlaylistDownload) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/downloaded")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistTopPlaylist) {
                        item(
                            key = "topPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistCached) {
                        item(
                            key = "cachedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = cachedPlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("CachedPlaylist")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistLocal) {
                        item(
                            key = "localPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistListItem(
                                playlist = localPlaylist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("AutoPlaylistLocal")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    items(
                        allItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST }
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("local_playlist/${item.id}")
                                                },
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
                                            .animateItem()
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
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
                                            .animateItem()
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!autoPlaylistLiked && !autoPlaylistDownload && !autoPlaylistTopPlaylist && !autoPlaylistCached && !autoPlaylistLocal && allItems.isEmpty()) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(1),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        filterContent()
                    }
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.library_music_outlined,
                            text = stringResource(R.string.library_empty)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
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
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }
                    if (autoPlaylistLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistDownload) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/downloaded")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistTopPlaylist) {
                        item(
                            key = "topPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistCached) {
                        item(
                            key = "cachedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = cachedPlaylist,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("CachedPlaylist")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    if (autoPlaylistLocal) {
                        item(
                            key = "localPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) {
                            PlaylistGridItem(
                                playlist = localPlaylist,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("AutoPlaylistLocal")
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                    items(
                        allItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
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
                                                    navController.navigate("local_playlist/${item.id}")
                                                },
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
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                )
                            }

                            is Album -> {
                                AlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
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
                                            .animateItem()
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}