package com.music.vivi.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.MyTopFilter
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.makeTimeString
import com.music.vivi.viewmodels.TopPlaylistViewModel


//new code improvement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.derivedStateOf
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.utils.resize
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.Surface

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.items

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastAny

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable


fun TopPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val maxSize = viewModel.top

    val songs by viewModel.topSongs.collectAsState(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    val wrappedSongs = songs?.map { item -> ItemWrapper(item) }?.toMutableList()

    // LazyListState and other UI state declarations
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var topAppBarHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // Search functionality
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        } else {
            lazyListState.animateScrollToItem(0)
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            searchQuery = TextFieldValue()
        }
    }

    var selection by remember { mutableStateOf(false) }

    val sortType by viewModel.topPeriod.collectAsState()
    val name = stringResource(R.string.my_top) + " $maxSize"

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    // Check if current playlist is playing
    val isCurrentPlaylistPlaying = true

    // Transparent app bar logic
    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && !isSearching
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = if (isSearching) {
                PaddingValues(
                    top = (topAppBarHeightPx / density.density).dp +
                            WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                    start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(LayoutDirection.Ltr),
                    end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(LayoutDirection.Ltr)
                )
            } else {
                LocalPlayerAwareWindowInsets.current
                    .add(
                        WindowInsets(
                            top = -WindowInsets.systemBars.asPaddingValues()
                                .calculateTopPadding() - AppBarHeight
                        )
                    )
                    .asPaddingValues()
            },
        ) {
            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    if (!isSearching) {
                        item(key = "header") {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                ) {
                                    AsyncImage(
                                        model = songs!![0].song.thumbnailUrl?.resize(1000, 1000),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopCenter)
                                            .fadingEdge(bottom = 200.dp),
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
                                ) {
                                    AutoResizeText(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSizeRange = FontSizeRange(24.sp, 32.sp),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.n_song,
                                            songs!!.size,
                                            songs!!.size,
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    Text(
                                        text = makeTimeString(likeLength * 1000L),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
//                                        Spacer(modifier = Modifier.weight(1f))
                                        //DOWNLOAD SECTION STARTING
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> {
                                                IconButton(
                                                    onClick = { showRemoveDownloadDialog = true },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(24.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.offline),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }

                                            Download.STATE_DOWNLOADING -> {
                                                IconButton(
                                                    onClick = {
                                                        songs!!.forEach { song ->
                                                            DownloadService.sendRemoveDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                song.song.id,
                                                                false,
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(24.dp)
                                                        )
                                                ) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(24.dp),
                                                    )
                                                }
                                            }

                                            else -> {
                                                IconButton(
                                                    onClick = {
                                                        songs!!.forEach { song ->
                                                            val downloadRequest =
                                                                DownloadRequest
                                                                    .Builder(
                                                                        song.song.id,
                                                                        song.song.id.toUri(),
                                                                    )
                                                                    .setCustomCacheKey(song.song.id)
                                                                    .setData(song.song.title.toByteArray())
                                                                    .build()
                                                            DownloadService.sendAddDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                downloadRequest,
                                                                false,
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(24.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.download),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        }

//                                        IconButton(
//                                            onClick = {
//                                                playerConnection.addToQueue(
//                                                    items = songs!!.map { it.toMediaItem() },
//                                                )
//                                            },
//                                            modifier = Modifier
//                                                .size(48.dp)
//                                                .background(
//                                                    MaterialTheme.colorScheme.surfaceVariant,
//                                                    RoundedCornerShape(24.dp)
//                                                )
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(R.drawable.queue_music),
//                                                contentDescription = null,
//                                            )
//                                        }

//                                        IconButton(
//                                            onClick = {
//                                                // Add menu functionality here
//                                            },
//                                            modifier = Modifier
//                                                .size(48.dp)
//                                                .background(
//                                                    MaterialTheme.colorScheme.surfaceVariant,
//                                                    RoundedCornerShape(24.dp)
//                                                )
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(R.drawable.more_vert),
//                                                contentDescription = null,
//                                            )
//                                        }



                                        IconButton(
                                            onClick = {
                                                if (isCurrentPlaylistPlaying) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = name,
                                                            items = songs!!.map { it.toMediaItem() },
                                                        ),
                                                    )
                                                }
                                            },
//                                            modifier = Modifier
//                                                .size(48.dp)
//                                                .background(
//                                                    if (isCurrentPlaylistPlaying && isPlaying) {
//                                                        MaterialTheme.colorScheme.primary
//                                                    } else {
//                                                        MaterialTheme.colorScheme.surfaceVariant
//                                                    },
//                                                    RoundedCornerShape(24.dp)
//                                                )
                                        ) {
//                                            Icon(
//                                                painter = painterResource(
//                                                    if (isCurrentPlaylistPlaying && isPlaying) {
//                                                        R.drawable.pause
//                                                    } else {
//                                                        R.drawable.play
//                                                    }
//                                                ),
//                                                contentDescription = null,
//                                                tint = if (isCurrentPlaylistPlaying && isPlaying) {
//                                                    MaterialTheme.colorScheme.onPrimary
//                                                } else {
//                                                    LocalContentColor.current
//                                                },
//                                                modifier = Modifier.size(20.dp)
//                                            )
                                        }

//                                        IconButton(
//                                            onClick = {
//                                                if (isCurrentPlaylistPlaying && shuffleModeEnabled) {
//                                                    playerConnection.player.shuffleModeEnabled = false
//                                                } else if (isCurrentPlaylistPlaying) {
//                                                    playerConnection.player.shuffleModeEnabled = true
//                                                } else {
//                                                    playerConnection.playQueue(
//                                                        ListQueue(
//                                                            title = name,
//                                                            items = songs!!.shuffled().map { it.toMediaItem() },
//                                                        ),
//                                                    )
//                                                }
//                                            },
//                                            modifier = Modifier
//                                                .size(48.dp)
//                                                .background(
//                                                    if (shuffleModeEnabled) {
//                                                        MaterialTheme.colorScheme.primary
//                                                    } else {
//                                                        MaterialTheme.colorScheme.surfaceVariant
//                                                    },
//                                                    RoundedCornerShape(24.dp)
//                                                )
//                                        ) {
//                                            Icon(
//                                                painter = painterResource(R.drawable.shuffle),
//                                                contentDescription = null,
//                                                tint = if (shuffleModeEnabled) {
//                                                    MaterialTheme.colorScheme.onPrimary
//                                                } else {
//                                                    LocalContentColor.current
//                                                },
//                                                modifier = Modifier.size(20.dp)
//                                            )
//                                        }
                                    }
                                }

//                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = false,
                                onSortTypeChange = {
                                    viewModel.topPeriod.value = it
                                },
                                onSortDescendingChange = {},
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        MyTopFilter.ALL_TIME -> R.string.all_time
                                        MyTopFilter.DAY -> R.string.past_24_hours
                                        MyTopFilter.WEEK -> R.string.past_week
                                        MyTopFilter.MONTH -> R.string.past_month
                                        MyTopFilter.YEAR -> R.string.past_year
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                showDescending = false,
                            )
                        }
                    }
                }

                val searchQueryStr = searchQuery.text.trim()
                val filteredSongs = if (searchQueryStr.isEmpty()) {
                    songs?.mapIndexed { index, song -> index to song } ?: emptyList()
                } else {
                    songs
                        ?.mapIndexed { index, song -> index to song }
                        ?.filter { (_, song) ->
                            song.song.title.contains(searchQueryStr, ignoreCase = true) ||
                                    song.artists.fastAny {
                                        it.name.contains(searchQueryStr, ignoreCase = true)
                                    }
                        } ?: emptyList()
                }

                if (filteredSongs.isNotEmpty()) {
                    items(
                        items = filteredSongs,
                        key = { it.second.id },
                    ) { (index, song) ->
                        SongListItem(
                            song = song,
                            albumIndex = index + 1,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            isSelected = wrappedSongs?.find { it.item.id == song.id }?.isSelected == true && selection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (song.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = name,
                                                        items = filteredSongs.map { it.second.toMediaItem() },
                                                        startIndex = index,
                                                    ),
                                                )
                                            }
                                        } else {
                                            wrappedSongs?.find { it.item.id == song.id }?.let {
                                                it.isSelected = !it.isSelected
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSongs?.forEach { it.isSelected = false }
                                        wrappedSongs?.find { it.item.id == song.id }?.isSelected = true
                                    },
                                )
                                .animateItem()
                        )
                    }
                } else if (searchQueryStr.isNotEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.search_off,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

    TopAppBar(
        title = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else if (isSearching) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            } else {
                if (!transparentAppBar) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp),
            ) {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = TextFieldValue()
                        } else if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp),
                ) {
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs?.size) {
                                wrappedSongs?.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs?.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs?.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp),
                ) {
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs?.filter { it.isSelected }!!
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                }
            } else if (!isSearching) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp),
                ) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        },
        colors = if (transparentAppBar && !selection) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
        modifier = Modifier.onSizeChanged { size ->
            topAppBarHeightPx = size.height
        }
    )
}