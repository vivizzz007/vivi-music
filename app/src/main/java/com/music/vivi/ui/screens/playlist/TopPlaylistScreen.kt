package com.music.vivi.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.CONTENT_TYPE_SONG
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
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.menu.SongMenu
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import com.music.vivi.ui.menu.SongSelectionMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.makeTimeString
import com.music.vivi.viewmodels.TopPlaylistViewModel

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
    val focusRequester = remember { FocusRequester() }

    val songs by viewModel.topSongs.collectAsState(null)
    val mutableSongs =
        remember {
            mutableStateListOf<Song>()
        }

    val likeLength =
        remember(songs) {
            songs?.fastSumBy { it.song.duration } ?: 0
        }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }


    LaunchedEffect(inSelectMode) {
        backStackEntry?.savedStateHandle?.set("inSelectMode", inSelectMode)
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val sortType by viewModel.topPeriod.collectAsState()

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true)
                    Download.STATE_COMPLETED
                else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED
                                || downloads[it.song.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true)
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, stringResource(R.string.my_top)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
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
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }
    val searchQueryStr = searchQuery.text.trim()
    val filteredSongs = if (searchQueryStr.isEmpty()) {
        songs
    } else {
        songs?.filter { song ->
            song.song.title.contains(searchQueryStr, ignoreCase = true) ||
                    song.artists.joinToString("")
                        .contains(searchQueryStr, ignoreCase = true)
        }
    }
    val state = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = state,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (filteredSongs.isNullOrEmpty() && isSearching) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search_icon,
                        text = stringResource(R.string.no_results_found)
                    )
                }
            }
            if (songs != null) {
                if (songs!!.isEmpty() && !isSearching) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty)
                        )
                    }
                } else {
                    if (!isSearching) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                val libcarditem = 25.dp
                                Box(
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize)
                                        .clip(RoundedCornerShape(libcarditem))
                                        .align(alignment = Alignment.CenterHorizontally)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainer,
                                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                                        )
                                ) {
                                    Icon(
                                        painterResource(R.drawable.trending_up),
                                        contentDescription = null,
                                        tint = LocalContentColor.current.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                            .align(Alignment.Center)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AutoResizeText(
                                        text = stringResource(R.string.my_top),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSizeRange = FontSizeRange(16.sp, 22.sp)
                                    )
                                    Text(
                                        text = makeTimeString(likeLength * 1000L),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> {
                                                Button(
                                                    onClick = {
                                                        showRemoveDownloadDialog = true
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(4.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.offline),
                                                        contentDescription = null
                                                    )
                                                }
                                            }

                                            Download.STATE_DOWNLOADING -> {
                                                Button(
                                                    onClick = {
                                                        songs!!.forEach { song ->
                                                            DownloadService.sendRemoveDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                song.song.id,
                                                                false
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(24.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                                }
                                            }

                                            else -> {
                                                Button(
                                                    onClick = {
                                                        songs!!.forEach { song ->
                                                            val downloadRequest =
                                                                DownloadRequest.Builder(
                                                                    song.song.id,
                                                                    song.song.id.toUri()
                                                                )
                                                                    .setCustomCacheKey(song.song.id)
                                                                    .setData(song.song.title.toByteArray())
                                                                    .build()
                                                            DownloadService.sendAddDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                downloadRequest,
                                                                false
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(4.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.download),
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                playerConnection.addToQueue(
                                                    items = songs!!.map { it.toMediaItem() },
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.my_top),
                                                    items = songs!!.map { it.toMediaItem() }
                                                )
                                            )
                                        },
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.play),
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(stringResource(R.string.play))
                                    }
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.my_top),
                                                    items = songs!!.shuffled()
                                                        .map { it.toMediaItem() }
                                                )
                                            )
                                        },
                                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.shuffle),
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(stringResource(R.string.shuffle))
                                    }
                                }
                            }
                        }
                    }
                    if (filteredSongs!!.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp)
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
                }
                if (filteredSongs != null) {
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> CONTENT_TYPE_SONG }
                    ) { index, songWrapper ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(songWrapper.id)
                            } else {
                                selection.remove(songWrapper.id)
                            }
                        }
                        if (index == 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.n_song,
                                        filteredSongs.size,
                                        filteredSongs.size
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                        SongListItem(
                            song = songWrapper,
                            isActive = songWrapper.song.id == mediaMetadata?.id,
                            showInLibraryIcon = true,
                            isPlaying = isPlaying,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = songWrapper.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = songWrapper,
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
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(songWrapper.id !in selection)
                                        } else if (songWrapper.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.my_top),
                                                    items = songs!!.map { it.toMediaItem() },
                                                    startIndex = songs!!.indexOfFirst { it.song.id == songWrapper.id }
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            }
        }
    }

    if (inSelectMode) {
        TopAppBar(
            title = {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            },
            navigationIcon = {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                    )
                }
            },
            actions = {
                Checkbox(
                    checked = selection.size == filteredSongs?.size && selection.isNotEmpty(),
                    onCheckedChange = {
                        if (selection.size == filteredSongs?.size) {
                            selection.clear()
                        } else {
                            selection.clear()
                            selection.addAll(filteredSongs?.map { it.id }.orEmpty())
                        }
                    }
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SongSelectionMenu(
                                selection = selection.mapNotNull { songId ->
                                    songs?.find { it.id == songId }
                                },
                                onDismiss = menuState::dismiss,
                                onExitSelectionMode = onExitSelectionMode
                            )
                        }
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            }
        )
    } else {
        TopAppBar(
            title = {
                if (isSearching) {
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
                            .focusRequester(focusRequester),
                        trailingIcon = {
                            if (searchQuery.text.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = TextFieldValue("") }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    )
                }
            },
            navigationIcon = {
                com.music.vivi.ui.component.IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = TextFieldValue()
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (!isSearching && !inSelectMode) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = null
                        )
                    }
                }
            },
            scrollBehavior = if (!isSearching) scrollBehavior else null
        )
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}