@file:Suppress("NAME_SHADOWING")

package com.music.vivi.ui.screens.playlist

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.ui.component.IconButton
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.PlaylistEditLockKey
import com.music.vivi.constants.PlaylistSongSortDescendingKey
import com.music.vivi.constants.PlaylistSongSortType
import com.music.vivi.constants.PlaylistSongSortTypeKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.extensions.move
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
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.SongSelectionMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.isInternetAvailable
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LocalPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }

    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSongSortDescendingKey, true)
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs
        else songs.filter { song ->
            song.song.title.contains(query.text, ignoreCase = true) ||
                    song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<Int>, Int>(
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

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        selection.fastForEachReversed { mapId ->
            if (songs.find { it.map.id == mapId } == null) {
                selection.remove(mapId)
            }
        }
    }

    val headerItems = 2
    var dragInfo by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (to.index >= headerItems && from.index >= headerItems) {
                val currentDragInfo = dragInfo
                dragInfo = if (currentDragInfo == null) {
                    (from.index - headerItems) to (to.index - headerItems)
                } else {
                    currentDragInfo.first to (to.index - headerItems)
                }
                mutableSongs.move(from.index - headerItems, to.index - headerItems)
            }
        }
    )
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val from = from
                    val to = to
                    val playlistSongMap = database.songMapsToPlaylist(viewModel.playlistId, 0)

                    var fromIndex = from //- headerItems
                    val toIndex = to //- headerItems

                    var successorIndex = if (fromIndex > toIndex) toIndex else toIndex + 1

                    /*
                        * Because of how YouTube Music handles playlist changes, you necessarily need to
                        * have the SetVideoId of the successor when trying to move a song inside of a
                        * playlist.
                        * For this reason, if we are trying to move a song to the last element of a playlist,
                        * we need to first move it as penultimate and then move the last element before it.
                        */
                    if (successorIndex >= playlistSongMap.size) {
                        playlistSongMap[fromIndex].setVideoId?.let { setVideoId ->
                            playlistSongMap[toIndex].setVideoId?.let { successorSetVideoId ->
                                viewModel.playlist.first()?.playlist?.browseId?.let { browseId ->
                                    YouTube.moveSongPlaylist(
                                        browseId,
                                        setVideoId,
                                        successorSetVideoId
                                    )
                                }
                            }
                        }

                        successorIndex = fromIndex
                        fromIndex = toIndex
                    }

                    playlistSongMap[fromIndex].setVideoId?.let { setVideoId ->
                        playlistSongMap[successorIndex].setVideoId?.let { successorSetVideoId ->
                            viewModel.playlist.first()?.playlist?.browseId?.let { browseId ->
                                YouTube.moveSongPlaylist(
                                    browseId,
                                    setVideoId,
                                    successorSetVideoId
                                )
                            }
                        }
                    }
                }
                dragInfo = null
            }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    var dismissJob: Job? by remember { mutableStateOf(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
                title = { Text(text = stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(playlistEntity.name, TextRange(playlistEntity.name.length)),
                onDone = { name ->
                    database.query {
                        update(playlistEntity.copy(name = name))
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                }
            )
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist!!.name),
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
                        songs.forEach { song ->
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = reorderableState.listState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.reorderable(reorderableState)
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                } else {
                    if (!isSearching) {
                        item {
                            LocalPlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                onShowEditDialog = { showEditDialog = true },
                                onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .animateItem()
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                        PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        PlaylistSongSortType.NAME -> R.string.sort_by_name
                                        PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                        PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (!inSelectMode) {
                                IconButton(
                                    onClick = { locked = !locked },
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (locked) R.drawable.lockon_icon else R.drawable.lockoff_icon ),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            itemsIndexed(
                items = if (isSearching) filteredSongs else mutableSongs,
                key = { _, song -> song.map.id }
            ) { index, song ->
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
                ReorderableItem(
                    state = reorderableState,
                    key = song.map.id
                ) {
                    val currentItem by rememberUpdatedState(song)

                    fun deleteFromPlaylist() {
                        database.transaction {
                            coroutineScope.launch {
                                playlist?.playlist?.browseId?.let { it1 ->
                                    val setVideoId = getSetVideoId(currentItem.map.songId)
                                    if (setVideoId?.setVideoId != null) {
                                        YouTube.removeFromPlaylist(
                                            it1, currentItem.map.songId, setVideoId.setVideoId
                                        )
                                    }
                                }
                            }
                            move(currentItem.map.playlistId, currentItem.map.position, Int.MAX_VALUE)
                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                        }
                        dismissJob?.cancel()
                        dismissJob = coroutineScope.launch {
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.removed_song_from_playlist, currentItem.song.song.title),
                                actionLabel = context.getString(R.string.undo),
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                database.transaction {
                                    insert(currentItem.map.copy(position = songs.size))
                                    move(currentItem.map.playlistId, songs.size, currentItem.map.position)
                                }
                            }
                        }
                    }

                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance ->
                            totalDistance
                        },
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                deleteFromPlaylist()
                            }
                            true
                        }
                    )

                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.map.id)
                        } else {
                            selection.remove(song.map.id)
                        }
                    }

                    val content: @Composable () -> Unit = {
                        SongListItem(
                            song = song.song,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = song.map.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song.song,
                                                    playlistSong = song,
                                                    playlistBrowseId = playlist?.playlist?.browseId,
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

                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !isSearching) {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.detectReorder(reorderableState)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.twolines_icon),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.map.id !in selection)
                                        } else if (song.song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist!!.playlist.name,
                                                    items = songs.map { it.song.toMediaItem() },
                                                    startIndex = songs.indexOfFirst { it.map.id == song.map.id }
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
                        )
                    }

                    if (locked || inSelectMode) {
                        content()
                    } else {
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {},
                            content = { content() }
                        )
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (inSelectMode) {
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
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
                            if (query.text.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = TextFieldValue("") }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    )
                } else {
                    if (showTopBarTitle) Text(playlist?.playlist?.name.orEmpty())
                }
            },
            navigationIcon = {
                if (inSelectMode) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
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
                            painterResource(R.drawable.back_icon),
                            contentDescription = null
                        )
                    }
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(
                        checked = selection.size == filteredSongs.size,
                        onCheckedChange = {
                            if (selection.size == filteredSongs.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(filteredSongs.map { it.map.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                SongSelectionMenu(
                                    selection = selection.mapNotNull { mapId ->
                                        songs.find { it.map.id == mapId }?.song
                                    },
                                    onDismiss = menuState::dismiss,
                                    onExitSelectionMode = onExitSelectionMode,
                                    onRemoveFromQueue = {
                                        val sel = selection.toList()
                                        val sortedSongs = songs.sortedBy { it.map.position }
                                        database.transaction {
                                            val remainSongs = mutableListOf<PlaylistSong>()
                                            sortedSongs.forEach { song ->
                                                if (song.map.id in sel) {
                                                    delete(song.map)
                                                } else {
                                                    remainSongs.add(song)
                                                }
                                            }
                                            remainSongs.forEachIndexed { index, song ->
                                                if (song.map.position != index) {
                                                    update(song.map.copy(position = index))
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painterResource(R.drawable.search_icon),
                            contentDescription = null
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun LocalPlaylistHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    onShowEditDialog: () -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()

    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.song.duration }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED })
                    Download.STATE_COMPLETED
                else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED
                                || downloads[it.song.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    })
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(12.dp)
    ) {
            if (playlist.thumbnails.size == 1) {
                AsyncImage(
                    model = playlist.thumbnails[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(AlbumThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .align(alignment = Alignment.CenterHorizontally)
                )
            } else if (playlist.thumbnails.size > 1) {
                Box(
                    modifier = Modifier
                        .size(AlbumThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .align(alignment = Alignment.CenterHorizontally)
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd
                    ).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(alignment)
                                .size(AlbumThumbnailSize / 2)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AutoResizeText(
                    text = playlist.playlist.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSizeRange = FontSizeRange(16.sp, 22.sp)
                )

                Text(
                    text = makeTimeString(playlistLength * 1000L),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
                Spacer(Modifier.height(12.dp))
                Row (
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (downloadState) {
                        Download.STATE_COMPLETED -> {
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                onClick = onShowRemoveDownloadDialog
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.offline),
                                    contentDescription = null
                                )
                            }
                        }

                        Download.STATE_DOWNLOADING -> {
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                onClick = {
                                    songs.forEach { song ->
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
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                onClick = {
                                    songs.forEach { song ->
                                        val downloadRequest = DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                                            .setCustomCacheKey(song.song.id)
                                            .setData(song.song.song.title.toByteArray())
                                            .build()
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            false
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        onClick = onShowEditDialog
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null
                        )
                    }
                    if (editable) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                playerConnection.addToQueue(
                                    items = songs.map { it.song.toMediaItem() },
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                            )
                        }
                    } else {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                database.transaction {
                                    update(playlist.playlist.toggleLike())
                                }
                            }
                        ) {
                            val liked = playlist.playlist.bookmarkedAt != null
                            Icon(
                                painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                                contentDescription = null
                            )
                        }
                    }

                    if (playlist.playlist.browseId != null && isInternetAvailable(context)) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    syncUtils.syncPlaylist(playlist.playlist.browseId, playlist.id)
                                    snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null
                            )
                        }
                    }
                }
            }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = playlist.playlist.name,
                            items = songs.map { it.song.toMediaItem() }
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
                            title = playlist.playlist.name,
                            items = songs.shuffled().map { it.song.toMediaItem() }
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