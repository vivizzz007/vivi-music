@file:OptIn(ExperimentalFoundationApi::class)

package com.music.vivi.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.utils.completed
import kotlinx.coroutines.launch
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.PlaylistEditLockKey
import com.music.vivi.constants.PlaylistSongSortDescendingKey
import com.music.vivi.constants.PlaylistSongSortType
import com.music.vivi.constants.PlaylistSongSortTypeKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.move
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.DraggableScrollbar
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.LocalPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalConfiguration
import com.music.vivi.ui.utils.fadingEdge
import kotlinx.coroutines.withContext


@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable


fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.song.duration }
    }
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSongSortTypeKey,
        PlaylistSongSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSongSortDescendingKey,
        true
    )
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs else {
            songs.filter { song ->
                song.song.song.title.contains(query.text, ignoreCase = true) ||
                        song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
            }
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }
    var selection by remember { mutableStateOf(false) }
    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { ItemWrapper(it) }
    }.toMutableStateList()
    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    } else if (selection) {
        BackHandler { selection = false }
    }


    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = when {
                songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } -> Download.STATE_COMPLETED
                songs.all {
                    val state = downloads[it.song.id]?.state
                    state == Download.STATE_QUEUED || state == Download.STATE_DOWNLOADING || state == Download.STATE_COMPLETED
                } -> Download.STATE_DOWNLOADING
                else -> Download.STATE_STOPPED
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }

    // Dialogs
    if (showEditDialog) {
        playlist?.playlist?.let { entity ->
            TextFieldDialog(
                icon = { Icon(painterResource(R.drawable.edit), null) },
                title = { Text(stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(entity.name, TextRange(entity.name.length)),
                onDone = { name ->
                    database.query {
                        update(entity.copy(name = name, lastUpdateTime = LocalDateTime.now()))
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        entity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                }
            )
        }
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist!!.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    showRemoveDownloadDialog = false
                    if (!editable) {
                        database.transaction { playlist?.id?.let { clearPlaylist(it) } }
                    }
                    songs.forEach { song ->
                        DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.song.id, false)
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist?.playlist!!.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    showDeletePlaylistDialog = false
                    database.query { playlist?.let { delete(it.playlist) } }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                    }
                    navController.popBackStack()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    val density = LocalDensity.current
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) { -(systemBarsTopPadding + AppBarHeight).roundToPx() }

    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= 1 && from.index >= 1) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                (from.index - 1) to (to.index - 1)
            } else {
                currentDragInfo.first to (to.index - 1)
            }
            mutableSongs.move(from.index - 1, to.index - 1)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }
                dragInfo = null
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist?.let { playlistEntity ->

                if (playlistEntity.songCount == 0 && playlistEntity.playlist.remoteSongCount == 0) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {

                    if (!isSearching) {
                        item(key = "header") {
                            Box {
                                // Full-width Header Image with Parallax Offset
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .offset { IntOffset(0, headerOffset) }
                                ) {
                                    if (playlistEntity.thumbnails.size == 1) {
                                        AsyncImage(
                                            model = playlistEntity.thumbnails[0],
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .fadingEdge(bottom = 200.dp),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .fadingEdge(bottom = 200.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        ) {
                                            playlistEntity.thumbnails.take(4).forEachIndexed { index, thumb ->
                                                val alignments = listOf(
                                                    Alignment.TopStart,
                                                    Alignment.TopEnd,
                                                    Alignment.BottomStart,
                                                    Alignment.BottomEnd
                                                ).getOrNull(index)
                                                if (alignments != null) {
                                                    AsyncImage(
                                                        model = thumb,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .align(alignments)
                                                            .fillMaxSize(0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Playlist Info Section - positioned at bottom of image
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = with(density) {
                                                ((LocalConfiguration.current.screenWidthDp.dp / 1f) - 144.dp)
                                            }.value.dp,
                                            start = 16.dp,
                                            end = 16.dp
                                        )
                                ) {
                                    Text(
                                        text = playlistEntity.playlist.name,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 28.sp
                                    )

                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.n_song,
                                            playlistEntity.songCount,
                                            playlistEntity.songCount
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Text(
                                        text = makeTimeString(playlistLength * 1000L),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    // Action Buttons Row (same style as OnlinePlaylistScreen)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Like/Delete Button
                                        if (editable) {
                                            IconButton(
                                                onClick = { showDeletePlaylistDialog = true },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.delete),
                                                    contentDescription = stringResource(R.string.delete),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    database.query {
                                                        playlist?.playlist?.let { playlistEntity ->
                                                            update(playlistEntity.toggleLike())
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (playlist?.playlist?.bookmarkedAt != null) R.drawable.favorite
                                                        else R.drawable.favorite_border
                                                    ),
                                                    contentDescription = stringResource(R.string.favourite),
                                                    tint = if (playlist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Edit Button (only for editable playlists)
                                        if (editable) {
                                            IconButton(
                                                onClick = { showEditDialog = true },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.edit),
                                                    contentDescription = stringResource(R.string.edit),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Add this state variable at the top of your composable
                                        var isSyncing by remember { mutableStateOf(false) }

                                        playlist?.playlist?.browseId?.let { browseId ->
                                            if (isSyncing) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(12.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(24.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        isSyncing = true
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            try {
                                                                val playlistPage = YouTube.playlist(browseId)
                                                                    .completed()
                                                                    .getOrNull() ?: return@launch

                                                                database.transaction {
                                                                    playlist?.id?.let { playlistId ->
                                                                        clearPlaylist(playlistId)
                                                                        playlistPage.songs.forEachIndexed { index, songItem ->
                                                                            val song = songItem.toMediaMetadata()
                                                                            insert(song)
                                                                            insert(
                                                                                PlaylistSongMap(
                                                                                    songId = song.id,
                                                                                    playlistId = playlistId,
                                                                                    position = index
                                                                                )
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                withContext(Dispatchers.Main) {
                                                                    snackbarHostState.showSnackbar(
                                                                        message = context.getString(R.string.playlist_synced),
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                }
                                                            } catch (e: Exception) {
                                                                withContext(Dispatchers.Main) {
                                                                    snackbarHostState.showSnackbar(
                                                                        message = context.getString(R.string.sync_failed),
                                                                        duration = SnackbarDuration.Long
                                                                    )
                                                                }
                                                            } finally {
                                                                isSyncing = false
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.sync),
                                                        contentDescription = stringResource(R.string.sync),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Download Button states
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> {
                                                IconButton(
                                                    onClick = { showRemoveDownloadDialog = true },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.offline),
                                                        contentDescription = stringResource(R.string.downloaded),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Download.STATE_DOWNLOADING -> {
                                                IconButton(
                                                    onClick = {
                                                        songs.forEach { song ->
                                                            DownloadService.sendRemoveDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                song.song.id,
                                                                false
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                ) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(24.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            else -> {
                                                IconButton(
                                                    onClick = {
                                                        songs.forEach { song ->
                                                            val request = DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                                                                .setCustomCacheKey(song.song.id)
                                                                .setData(song.song.song.title.toByteArray())
                                                                .build()
                                                            DownloadService.sendAddDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                request,
                                                                false
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.download),
                                                        contentDescription = stringResource(R.string.download),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Add to Queue Button
                                        IconButton(
                                            onClick = {
                                                playerConnection?.addToQueue(
                                                    items = songs.map { it.song.toMediaItem() }
                                                )
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = stringResource(R.string.add_to_queue),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Share Button (for YouTube playlists)
                                        playlist?.playlist?.browseId?.let { browseId ->
                                            IconButton(
                                                onClick = {
                                                    val shareText = "https://music.youtube.com/playlist?list=$browseId"
                                                    val intent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, null))
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.share),
                                                    contentDescription = stringResource(R.string.share),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                            if (editable) {
                                IconButton(onClick = { locked = !locked }) {
                                    Icon(
                                        painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                        null
                                    )
                                }
                            }
                        }
                    }

                    if (!selection) {
                        itemsIndexed(
                            items = if (isSearching) filteredSongs else mutableSongs,
                            key = { _, song -> song.map.id },
                        ) { index, song ->
                            ReorderableItem(
                                state = reorderableState,
                                key = song.map.id
                            ) {
                                val currentItem by rememberUpdatedState(song)
                                fun deleteFromPlaylist() {
                                    database.transaction {
                                        coroutineScope.launch {
                                            playlistEntity.playlist.browseId?.let { browseId ->
                                                val setVideoId = getSetVideoId(currentItem.map.songId)
                                                if (setVideoId?.setVideoId != null) {
                                                    YouTube.removeFromPlaylist(browseId, currentItem.map.songId, setVideoId.setVideoId!!)
                                                }
                                            }
                                        }
                                        move(currentItem.map.playlistId, currentItem.map.position, Int.MAX_VALUE)
                                        delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                    }
                                }
                                val dismissBoxState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance }
                                )
                                var processedDismiss by remember { mutableStateOf(false) }
                                LaunchedEffect(dismissBoxState.currentValue) {
                                    val dv = dismissBoxState.currentValue
                                    if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                        processedDismiss = true
                                        deleteFromPlaylist()
                                    }
                                    if (dv == SwipeToDismissBoxValue.Settled) {
                                        processedDismiss = false
                                    }
                                }
                                val content: @Composable () -> Unit = {
                                    SongListItem(
                                        song = song.song,
                                        isActive = song.song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        showInLibraryIcon = true,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song.song,
                                                            playlistSong = song,
                                                            playlistBrowseId = playlistEntity.playlist.browseId,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                            if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching) {
                                                IconButton(
                                                    onClick = {},
                                                    modifier = Modifier.draggableHandle(),
                                                ) {
                                                    Icon(
                                                        painterResource(R.drawable.drag_handle),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (song.song.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = playlistEntity.playlist.name,
                                                                items = songs.map { it.song.toMediaItem() },
                                                                startIndex = songs.indexOfFirst { it.map.id == song.map.id },
                                                            )
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (!selection) {
                                                        selection = true
                                                    }
                                                    wrappedSongs.forEach { it.isSelected = false }
                                                    wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true
                                                },
                                            )
                                    )
                                }
                                if (locked || selection) {
                                    content()
                                } else {
                                    SwipeToDismissBox(
                                        state = dismissBoxState,
                                        backgroundContent = {},
                                    ) {
                                        content()
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = wrappedSongs,
                            key = { _, songWrapper -> songWrapper.item.map.id },
                        ) { _, songWrapper ->
                            ReorderableItem(
                                state = reorderableState,
                                key = songWrapper.item.map.id,
                            ) {
                                val currentItem by rememberUpdatedState(songWrapper.item)
                                fun deleteFromPlaylist() {
                                    database.transaction {
                                        move(currentItem.map.playlistId, currentItem.map.position, Int.MAX_VALUE)
                                        delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                    }
                                }
                                val dismissBoxState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance }
                                )
                                var processedDismiss2 by remember { mutableStateOf(false) }
                                LaunchedEffect(dismissBoxState.currentValue) {
                                    val dv = dismissBoxState.currentValue
                                    if (!processedDismiss2 && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                        processedDismiss2 = true
                                        deleteFromPlaylist()
                                    }
                                    if (dv == SwipeToDismissBoxValue.Settled) {
                                        processedDismiss2 = false
                                    }
                                }
                                val content: @Composable () -> Unit = {
                                    SongListItem(
                                        song = songWrapper.item.song,
                                        isActive = songWrapper.item.song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        showInLibraryIcon = true,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = songWrapper.item.song,
                                                            playlistBrowseId = playlistEntity.playlist.browseId,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                            if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching) {
                                                IconButton(
                                                    onClick = {},
                                                    modifier = Modifier.draggableHandle(),
                                                ) {
                                                    Icon(
                                                        painterResource(R.drawable.drag_handle),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        isSelected = songWrapper.isSelected && selection,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (!selection) {
                                                        if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                ListQueue(
                                                                    title = playlistEntity.playlist.name,
                                                                    items = songs.map { it.song.toMediaItem() },
                                                                    startIndex = wrappedSongs.indexOf(songWrapper),
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
                                                    wrappedSongs.forEach { it.isSelected = false }
                                                    songWrapper.isSelected = true
                                                },
                                            )
                                    )
                                }
                                if (locked || !editable) {
                                    content()
                                } else {
                                    SwipeToDismissBox(
                                        state = dismissBoxState,
                                        backgroundContent = {},
                                    ) {
                                        content()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues())
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 2
        )

        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge)
                    }
                    isSearching -> {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                    !transparentAppBar -> {
                        Text(playlist?.playlist?.name.orEmpty())
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearching) {
                        isSearching = false
                        query = TextFieldValue()
                    } else if (selection) {
                        selection = false
                    } else {
                        navController.navigateUp()
                    }
                }) {
                    Icon(
                        painterResource(if (selection) R.drawable.close else R.drawable.arrow_back),
                        null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = {
                        wrappedSongs.forEach { it.isSelected = !(count == wrappedSongs.size) }
                    }) {
                        Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null)
                    }
                    IconButton(onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song },
                                songPosition = wrappedSongs.filter { it.isSelected }.map { it.item.map },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false }
                            )
                        }
                    }) {
                        Icon(painterResource(R.drawable.more_vert), null)
                    }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(painterResource(R.drawable.search), null)
                    }
                }
            },
            colors = if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
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
