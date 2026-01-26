package com.music.vivi.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.CONTENT_TYPE_LIST
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.*
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.media.albums.AlbumListItem
import com.music.vivi.ui.component.media.artists.ArtistListItem
import com.music.vivi.ui.component.media.playlists.PlaylistListItem
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.viewmodels.LocalFilter
import com.music.vivi.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop
/**
 * Screen for searching within the local library (songs, albums, artists, playlists).
 * Filters results by type and allows playback or navigation to detailed views.
 *
 * @param query The search query string.
 * @param onDismiss Callback when search is dismissed or an item is clicked.
 * @param isFromCache Whether the search is being performed within cached/downloaded content.
 * @param pureBlack Whether to use pure black background (OLED optimization).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val searchFilter by viewModel.filter.collectAsState()
    val result by viewModel.result.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .let { base ->
                if (isLandscape) {
                    // Apply safe horizontal insets only in landscape to avoid notch/rail overlap
                    base.windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                    )
                } else {
                    base
                }
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            listOf(
                LocalFilter.ALL to stringResource(R.string.filter_all),
                LocalFilter.SONG to stringResource(R.string.filter_songs),
                LocalFilter.ALBUM to stringResource(R.string.filter_albums),
                LocalFilter.ARTIST to stringResource(R.string.filter_artists),
                LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists)
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = searchFilter == filter,
                    onClick = { viewModel.filter.value = filter },
                    label = { Text(label) },
                    leadingIcon = if (searchFilter == filter) {
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

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            // Keep only bottom safe area inside the list; top handled above (landscape only)
            contentPadding = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues()
        ) {
            result.map.forEach { (filter, items) ->
                if (result.filter == LocalFilter.ALL) {
                    item(key = filter) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .clickable { viewModel.filter.value = filter }
                                .padding(start = 12.dp, end = 18.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    when (filter) {
                                        LocalFilter.SONG -> R.string.filter_songs
                                        LocalFilter.ALBUM -> R.string.filter_albums
                                        LocalFilter.ARTIST -> R.string.filter_artists
                                        LocalFilter.PLAYLIST -> R.string.filter_playlists
                                        LocalFilter.ALL -> error("")
                                    }
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                painter = painterResource(R.drawable.navigate_next),
                                contentDescription = null
                            )
                        }
                    }
                }

                items(
                    items = items.distinctBy { it.id },
                    key = { it.id },
                    contentType = { CONTENT_TYPE_LIST }
                ) { item ->
                    when (item) {
                        is Song -> LibrarySongListItem(
                            song = item,
                            showInLibraryIcon = true,
                            isActive = item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = item,
                                                navController = navController,
                                                onDismiss = {
                                                    onDismiss()
                                                    menuState.dismiss()
                                                },
                                                isFromCache = isFromCache
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
                                .combinedClickable(
                                    onClick = {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            val songs = result.map
                                                .getOrDefault(LocalFilter.SONG, emptyList())
                                                .filterIsInstance<Song>()
                                                .map { it.toMediaItem() }
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.queue_searched_songs),
                                                    items = songs,
                                                    startIndex = songs.indexOfFirst { it.mediaId == item.id }
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = item,
                                                navController = navController,
                                                onDismiss = {
                                                    onDismiss()
                                                    menuState.dismiss()
                                                },
                                                isFromCache = isFromCache
                                            )
                                        }
                                    }
                                )
                                .animateItem()
                        )

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
                                modifier = Modifier
                                    .clickable {
                                        onDismiss()
                                        navController.navigate("album/${item.id}")
                                    }
                                    .animateItem()
                            )
                        }

                        is Artist -> ArtistListItem(
                            artist = item,
                            modifier = Modifier
                                .clickable {
                                    onDismiss()
                                    navController.navigate("artist/${item.id}")
                                }
                                .animateItem()
                        )

                        is Playlist -> PlaylistListItem(
                            playlist = item,
                            modifier = Modifier
                                .clickable {
                                    onDismiss()
                                    navController.navigate("local_playlist/${item.id}")
                                }
                                .animateItem()
                        )
                    }
                }
            }

            if (result.query.isNotEmpty() && result.map.isEmpty()) {
                item(key = "no_result") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found)
                    )
                }
            }
        }
    }
}
