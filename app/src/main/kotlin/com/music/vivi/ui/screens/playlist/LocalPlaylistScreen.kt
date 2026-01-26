package com.music.vivi.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.utils.completed
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.PlaylistEditLockKey
import com.music.vivi.constants.PlaylistSongSortDescendingKey
import com.music.vivi.constants.PlaylistSongSortType
import com.music.vivi.constants.PlaylistSongSortTypeKey
import com.music.vivi.constants.SwipeToRemoveSongKey
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.move
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.ActionPromptDialog
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.DraggableScrollbar
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.OverlayEditButton
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.menu.CustomThumbnailMenu
import com.music.vivi.ui.menu.PlaylistMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.screens.settings.DarkMode
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.utils.reportException
import com.music.vivi.viewmodels.LocalPlaylistViewModel
import com.yalantis.ucrop.UCrop
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

/**
 * Screen for displaying a user-created local playlist.
 * Allows editing (renaming, reordering songs, removing songs) and playback.
 * The playlist data is stored in the local Room database.
 */
@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    playlistId: String? = null,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
    onBack: () -> Unit = { navController.navigateUp() },
) {
    if (playlistId != null) {
        LaunchedEffect(playlistId) {
            viewModel.setPlaylistId(playlistId)
        }
    }
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
        if (query.text.isEmpty()) {
            songs
        } else {
            songs.filter { song ->
                song.song.song.title.contains(query.text, ignoreCase = true) ||
                    song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var selection by remember { mutableStateOf(false) }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null
                    )
                },
                title = { Text(text = stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(
                    playlistEntity.name,
                    TextRange(playlistEntity.name.length)
                ),
                onDone = { name ->
                    database.query {
                        update(
                            playlistEntity.copy(
                                name = name,
                                lastUpdateTime = LocalDateTime.now()
                            )
                        )
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
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) {
                            database.transaction {
                                playlist?.id?.let { clearPlaylist(it) }
                            }
                        }
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

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.delete_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    val headerItems = 2
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
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

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId.value!!, from, to)
                }

                if (viewModel.playlist.value?.playlist?.browseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId.value!!, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(
                                viewModel.playlist.value?.playlist?.browseId!!,
                                setVideoId,
                                successorSetVideoId
                            )
                        }
                    }
                }
                dragInfo = null
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                            modifier = Modifier
                        )
                    }
                } else {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(Modifier.height(50.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp)
                                ) {
                                    PlaylistThumbnail(
                                        playlist = playlist,
                                        editable = editable,
                                        menuState = menuState,
                                        snackbarHostState = snackbarHostState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }

                                Spacer(Modifier.height(32.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.queue_music),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = playlist.playlist.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            if (editable) {
                                                showDeletePlaylistDialog = true
                                            } else {
                                                database.transaction {
                                                    update(playlist.playlist.toggleLike())
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (editable) {
                                                        R.drawable.delete
                                                    } else if (playlist.playlist.bookmarkedAt != null) {
                                                        R.drawable.favorite
                                                    } else {
                                                        R.drawable.favorite_border
                                                    }
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (!editable && playlist.playlist.bookmarkedAt != null) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (editable) {
                                                    stringResource(R.string.delete)
                                                } else if (playlist.playlist.bookmarkedAt !=
                                                    null
                                                ) {
                                                    stringResource(R.string.saved)
                                                } else {
                                                    stringResource(R.string.save)
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist.playlist.name,
                                                    items = songs.map { it.song.toMediaItem() }
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.play_text),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            playerConnection.addToQueue(
                                                items = songs.map { it.song.toMediaItem() }
                                            )
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = stringResource(R.string.add_to_queue_content_desc),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                Text(
                                    text = buildString {
                                        val trackCount = if (playlist.songCount == 0 &&
                                            playlist.playlist.remoteSongCount != null
                                        ) {
                                            playlist.playlist.remoteSongCount ?: 0
                                        } else {
                                            playlist.songCount
                                        }

                                        val hours = playlistLength / 3600
                                        val minutes = (playlistLength % 3600) / 60

                                        // Build longer descriptive sentence
                                        append(stringResource(R.string.playlist_description_start))
                                        append(" ")
                                        append(pluralStringResource(R.plurals.n_song, trackCount, trackCount))
                                        append(". ")

                                        if (hours > 0 && minutes > 0) {
                                            append(stringResource(R.string.playlist_duration_description))
                                            append(" $hours ")
                                            append(
                                                if (hours >
                                                    1
                                                ) {
                                                    stringResource(R.string.hours)
                                                } else {
                                                    stringResource(R.string.hour)
                                                }
                                            )
                                            append(" ")
                                            append(stringResource(R.string.and))
                                            append(" $minutes ")
                                            append(
                                                if (minutes >
                                                    1
                                                ) {
                                                    stringResource(R.string.minutes)
                                                } else {
                                                    stringResource(R.string.minute)
                                                }
                                            )
                                            append(" ")
                                            append(stringResource(R.string.of_music))
                                        } else if (hours > 0) {
                                            append(stringResource(R.string.playlist_duration_description))
                                            append(" $hours ")
                                            append(
                                                if (hours >
                                                    1
                                                ) {
                                                    stringResource(R.string.hours)
                                                } else {
                                                    stringResource(R.string.hour)
                                                }
                                            )
                                            append(" ")
                                            append(stringResource(R.string.of_music))
                                        } else if (minutes > 0) {
                                            append(stringResource(R.string.playlist_duration_description))
                                            append(" $minutes ")
                                            append(
                                                if (minutes >
                                                    1
                                                ) {
                                                    stringResource(R.string.minutes)
                                                } else {
                                                    stringResource(R.string.minute)
                                                }
                                            )
                                            append(" ")
                                            append(stringResource(R.string.of_music))
                                        }

                                        append(". ")
                                        append(stringResource(R.string.playlist_enjoy_message))
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )

                                Spacer(Modifier.height(24.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // First row: Download, Shuffle, and Edit (if editable)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            ButtonGroupDefaults.ConnectedSpaceBetween
                                        )
                                    ) {
                                        ToggleButton(
                                            checked =
                                            downloadState == Download.STATE_COMPLETED ||
                                                downloadState == Download.STATE_DOWNLOADING,
                                            onCheckedChange = {
                                                when (downloadState) {
                                                    Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                                        showRemoveDownloadDialog = true
                                                    }
                                                    else -> {
                                                        songs.forEach { song ->
                                                            val downloadRequest = DownloadRequest
                                                                .Builder(song.song.id, song.song.id.toUri())
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
                                                }
                                            },
                                            modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        ) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    Icon(
                                                        painter = painterResource(R.drawable.offline),
                                                        contentDescription = stringResource(R.string.saved),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Download.STATE_DOWNLOADING -> {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(16.dp),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                else -> {
                                                    Icon(
                                                        painter = painterResource(R.drawable.download),
                                                        contentDescription = stringResource(R.string.save),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(
                                                text = when (downloadState) {
                                                    Download.STATE_COMPLETED -> stringResource(R.string.saved)
                                                    Download.STATE_DOWNLOADING -> stringResource(R.string.saving)
                                                    else -> stringResource(R.string.save)
                                                },
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist.playlist.name,
                                                        items = songs.shuffled().map { it.song.toMediaItem() }
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                            shapes = when {
                                                editable -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                playlist.playlist.browseId != null -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                                else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = stringResource(R.string.shuffle_content_desc),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(
                                                stringResource(R.string.shuffle_label),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        // Show Sync button in first row only if NOT editable
                                        if (!editable && playlist.playlist.browseId != null) {
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val playlistPage = YouTube.playlist(playlist.playlist.browseId)
                                                            .completed()
                                                            .getOrNull() ?: return@launch
                                                        database.transaction {
                                                            clearPlaylist(playlist.id)
                                                            playlistPage.songs
                                                                .map(SongItem::toMediaMetadata)
                                                                .onEach(::insert)
                                                                .mapIndexed { position, song ->
                                                                    PlaylistSongMap(
                                                                        songId = song.id,
                                                                        playlistId = playlist.id,
                                                                        position = position,
                                                                        setVideoId = song.setVideoId
                                                                    )
                                                                }
                                                                .forEach(::insert)
                                                        }
                                                    }
                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        snackbarHostState.showSnackbar(
                                                            context.getString(R.string.playlist_synced)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.sync),
                                                    contentDescription = stringResource(R.string.sync_content_desc),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    stringResource(R.string.sync_label),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }

                                        // Show Edit button in first row only if editable
                                        if (editable) {
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    showEditDialog = true
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.edit),
                                                    contentDescription = stringResource(R.string.edit_content_desc),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    stringResource(R.string.edit_label),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                    // Second row: Sync button (only if editable and has browseId)
                                    if (editable && playlist.playlist.browseId != null) {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val playlistPage = YouTube.playlist(playlist.playlist.browseId)
                                                        .completed()
                                                        .getOrNull() ?: return@launch
                                                    database.transaction {
                                                        clearPlaylist(playlist.id)
                                                        playlistPage.songs
                                                            .map(SongItem::toMediaMetadata)
                                                            .onEach(::insert)
                                                            .mapIndexed { position, song ->
                                                                PlaylistSongMap(
                                                                    songId = song.id,
                                                                    playlistId = playlist.id,
                                                                    position = position,
                                                                    setVideoId = song.setVideoId
                                                                )
                                                            }
                                                            .forEach(::insert)
                                                    }
                                                }
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    snackbarHostState.showSnackbar(
                                                        context.getString(R.string.playlist_synced)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
                                            shapes = ToggleButtonDefaults.shapes()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.sync),
                                                contentDescription = stringResource(R.string.sync_content_desc),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(
                                                stringResource(R.string.sync_label),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    item(key = "controls_row") {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                            PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            PlaylistSongSortType.NAME -> R.string.sort_by_name
                                            PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                            PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    }
                                )
                                // Animated Lock toggle button on the right

                                if (editable) {
                                    FilledIconToggleButton(
                                        checked = locked,
                                        onCheckedChange = { locked = it },
                                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        if (locked) {
                                            Icon(
                                                painter = painterResource(R.drawable.lock),
                                                contentDescription = "Locked"
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.lock_open),
                                                contentDescription = "Unlocked"
                                            )
                                        }
                                    }
                                }
                            }

                            // Space between filter and song list
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (!selection) {
                        itemsIndexed(
                            items = if (isSearching) filteredSongs else mutableSongs,
                            key = { _, song -> song.map.id }
                        ) { index, song ->
                            ReorderableItem(
                                state = reorderableState,
                                key = song.map.id
                            ) {
                                val currentItem by rememberUpdatedState(song)
                                val isFirst = index == 0
                                val isLast = index == (if (isSearching) filteredSongs.size else mutableSongs.size) - 1
                                val isActive = song.song.id == mediaMetadata?.id

                                var isVisible by remember { mutableStateOf(true) }

                                fun deleteFromPlaylist() {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        database.transaction {
                                            launch {
                                                playlist.playlist.browseId?.let { browseId ->
                                                    val setVideoId = getSetVideoId(currentItem.map.songId)
                                                    if (setVideoId?.setVideoId != null) {
                                                        YouTube.removeFromPlaylist(
                                                            browseId,
                                                            currentItem.map.songId,
                                                            setVideoId.setVideoId!!
                                                        )
                                                    }
                                                }
                                            }
                                            move(
                                                currentItem.map.playlistId,
                                                currentItem.map.position,
                                                Int.MAX_VALUE
                                            )
                                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                        }
                                    }
                                }

                                val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = false)
                                val dismissBoxState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                                )

                                LaunchedEffect(dismissBoxState.currentValue) {
                                    if (swipeRemoveEnabled &&
                                        (
                                            dismissBoxState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                                                dismissBoxState.currentValue == SwipeToDismissBoxValue.EndToStart
                                            )
                                    ) {
                                        deleteFromPlaylist()
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isVisible,
                                    exit =
                                    shrinkVertically(animationSpec = tween(300)) +
                                        fadeOut(animationSpec = tween(300))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        val songContent: @Composable () -> Unit = {
                                            val cornerRadius = remember { 24.dp }

                                            val topShape = remember(cornerRadius) {
                                                AbsoluteSmoothCornerShape(
                                                    cornerRadiusTR = cornerRadius,
                                                    smoothnessAsPercentBR = 0,
                                                    cornerRadiusBR = 0.dp,
                                                    smoothnessAsPercentTL = 60,
                                                    cornerRadiusTL = cornerRadius,
                                                    smoothnessAsPercentBL = 0,
                                                    cornerRadiusBL = 0.dp,
                                                    smoothnessAsPercentTR = 60
                                                )
                                            }
                                            val middleShape = remember { RectangleShape }
                                            val bottomShape = remember(cornerRadius) {
                                                AbsoluteSmoothCornerShape(
                                                    cornerRadiusTR = 0.dp,
                                                    smoothnessAsPercentBR = 60,
                                                    cornerRadiusBR = cornerRadius,
                                                    smoothnessAsPercentTL = 0,
                                                    cornerRadiusTL = 0.dp,
                                                    smoothnessAsPercentBL = 60,
                                                    cornerRadiusBL = cornerRadius,
                                                    smoothnessAsPercentTR = 0
                                                )
                                            }
                                            val singleShape = remember(cornerRadius) {
                                                AbsoluteSmoothCornerShape(
                                                    cornerRadiusTR = cornerRadius,
                                                    smoothnessAsPercentBR = 60,
                                                    cornerRadiusBR = cornerRadius,
                                                    smoothnessAsPercentTL = 60,
                                                    cornerRadiusTL = cornerRadius,
                                                    smoothnessAsPercentBL = 60,
                                                    cornerRadiusBL = cornerRadius,
                                                    smoothnessAsPercentTR = 60
                                                )
                                            }

                                            val shape = remember(isFirst, isLast, cornerRadius) {
                                                when {
                                                    isFirst && isLast -> singleShape
                                                    isFirst -> topShape
                                                    isLast -> bottomShape
                                                    else -> middleShape
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(ListItemHeight)
                                                    .clip(shape)
                                                    .background(
                                                        if (isActive) {
                                                            MaterialTheme.colorScheme.secondaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainer
                                                        }
                                                    )
                                            ) {
                                                LibrarySongListItem(
                                                    song = song.song,
                                                    isActive = isActive,
                                                    isPlaying = isPlaying,
                                                    showInLibraryIcon = true,
                                                    isSwipeable = false,
                                                    trailingContent = {
                                                        IconButton(
                                                            onClick = {
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = song.song,
                                                                        playlistSong = song,
                                                                        playlistBrowseId = playlist.playlist.browseId,
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

                                                        if (sortType == PlaylistSongSortType.CUSTOM &&
                                                            !locked &&
                                                            !isSearching &&
                                                            editable
                                                        ) {
                                                            IconButton(
                                                                onClick = { },
                                                                modifier = Modifier.draggableHandle()
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.drag_handle),
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        }
                                                    },
                                                    isSelected = false, // Not using selection here directly as per original? Wait, original had no selection params here?
                                                    // Original code snippet for FIRST SongListItem:
                                                    // song = song.song, isActive, isPlaying, showInLibraryIcon=true, isSwipeable=false, drawHighlight=false
                                                    // trailingContent includes menu and drag handle.
                                                    // modifier combinedClickable with onClick and onLongClick for selection.

                                                    // LibrarySongListItem has isSelected and inSelectionMode params.
                                                    // But here logic is handled via modifier.
                                                    // I should pass isSelected = false (or check wrappedSongs? this is mutableSongs loop).
                                                    // Wait, selection logic in LocalPlaylistScreen uses `wrappedSongs` for selection mode, but `itemsIndexed` iterates `if (isSearching) filteredSongs else mutableSongs`.
                                                    // `filteredSongs` are wrappers?
                                                    // filteredSongs logic:
                                                    // if query empty -> songs (mutableSongs?) No.
                                                    // val filteredSongs = remember(songs, query) { ... songs.filter ... }
                                                    // See line 230: filteredSongs handles `songs` (List<PlaylistSong>).
                                                    // `wrappedSongs` (line 250) wraps filteredSongs.

                                                    // Wait, `itemsIndexed` at line 956 uses `if (isSearching) filteredSongs else mutableSongs`.
                                                    // `mutableSongs` is list of contents.
                                                    // `filteredSongs` is list of contents.
                                                    // `wrappedSongs` is list of `ItemWrapper` used when `selection` is true?

                                                    // Line 955: `if (!selection)`.
                                                    // So here selection is FALSE.
                                                    // So I should pass `inSelectionMode = false`.

                                                    inSelectionMode = false,
                                                    onSelectionChange = {},
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (song.song.id == mediaMetadata?.id) {
                                                                    playerConnection.player.togglePlayPause()
                                                                } else {
                                                                    playerConnection.playQueue(
                                                                        ListQueue(
                                                                            title = playlist.playlist.name,
                                                                            items = songs.map { it.song.toMediaItem() },
                                                                            startIndex = songs.indexOfFirst {
                                                                                it.map.id ==
                                                                                    song.map.id
                                                                            }
                                                                        )
                                                                    )
                                                                }
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )
                                                                if (!selection) {
                                                                    selection = true
                                                                }
                                                                wrappedSongs.forEach { it.isSelected = false }
                                                                wrappedSongs.find {
                                                                    it.item.map.id == song.map.id
                                                                }?.isSelected =
                                                                    true
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        if (locked || !swipeRemoveEnabled || !editable) {
                                            songContent()
                                        } else {
                                            SwipeToDismissBox(
                                                state = dismissBoxState,
                                                backgroundContent = {
                                                    val color by animateColorAsState(
                                                        targetValue = when (dismissBoxState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd,
                                                            SwipeToDismissBoxValue.EndToStart,
                                                            -> MaterialTheme.colorScheme.errorContainer
                                                            else -> Color.Transparent
                                                        },
                                                        label = "swipe_background_color"
                                                    )
                                                    val cornerRadius = remember { 24.dp }

                                                    val topShape = remember(cornerRadius) {
                                                        AbsoluteSmoothCornerShape(
                                                            cornerRadiusTR = cornerRadius,
                                                            smoothnessAsPercentBR = 0,
                                                            cornerRadiusBR = 0.dp,
                                                            smoothnessAsPercentTL = 60,
                                                            cornerRadiusTL = cornerRadius,
                                                            smoothnessAsPercentBL = 0,
                                                            cornerRadiusBL = 0.dp,
                                                            smoothnessAsPercentTR = 60
                                                        )
                                                    }
                                                    val middleShape = remember { RectangleShape }
                                                    val bottomShape = remember(cornerRadius) {
                                                        AbsoluteSmoothCornerShape(
                                                            cornerRadiusTR = 0.dp,
                                                            smoothnessAsPercentBR = 60,
                                                            cornerRadiusBR = cornerRadius,
                                                            smoothnessAsPercentTL = 0,
                                                            cornerRadiusTL = 0.dp,
                                                            smoothnessAsPercentBL = 60,
                                                            cornerRadiusBL = cornerRadius,
                                                            smoothnessAsPercentTR = 0
                                                        )
                                                    }
                                                    val singleShape = remember(cornerRadius) {
                                                        AbsoluteSmoothCornerShape(
                                                            cornerRadiusTR = cornerRadius,
                                                            smoothnessAsPercentBR = 60,
                                                            cornerRadiusBR = cornerRadius,
                                                            smoothnessAsPercentTL = 60,
                                                            cornerRadiusTL = cornerRadius,
                                                            smoothnessAsPercentBL = 60,
                                                            cornerRadiusBL = cornerRadius,
                                                            smoothnessAsPercentTR = 60
                                                        )
                                                    }

                                                    val shape = remember(isFirst, isLast, cornerRadius) {
                                                        when {
                                                            isFirst && isLast -> singleShape
                                                            isFirst -> topShape
                                                            isLast -> bottomShape
                                                            else -> middleShape
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(shape)
                                                            .background(color)
                                                            .padding(horizontal = 20.dp),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.delete),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            ) {
                                                songContent()
                                            }
                                        }

                                        if (!isLast) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = wrappedSongs,
                            key = { _, song -> song.item.map.id }
                        ) { index, songWrapper ->
                            ReorderableItem(
                                state = reorderableState,
                                key = songWrapper.item.map.id
                            ) {
                                val currentItem by rememberUpdatedState(songWrapper.item)
                                val isFirst = index == 0
                                val isLast = index == wrappedSongs.size - 1
                                val isActive = songWrapper.item.song.id == mediaMetadata?.id

                                var isVisible by remember { mutableStateOf(true) }

                                fun deleteFromPlaylist() {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        database.transaction {
                                            move(
                                                currentItem.map.playlistId,
                                                currentItem.map.position,
                                                Int.MAX_VALUE
                                            )
                                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                        }
                                        wrappedSongs.remove(songWrapper)
                                    }
                                }

                                val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = false)
                                val dismissBoxState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                                )

                                LaunchedEffect(dismissBoxState.currentValue) {
                                    if (swipeRemoveEnabled &&
                                        (
                                            dismissBoxState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                                                dismissBoxState.currentValue == SwipeToDismissBoxValue.EndToStart
                                            )
                                    ) {
                                        deleteFromPlaylist()
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isVisible,
                                    exit =
                                    shrinkVertically(animationSpec = tween(300)) +
                                        fadeOut(animationSpec = tween(300))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        val songContent: @Composable () -> Unit = {
                                            val cornerRadius = remember { 24.dp }

                                            val topShape = remember(cornerRadius) {
                                                AbsoluteSmoothCornerShape(
                                                    cornerRadiusTR = cornerRadius,
                                                    smoothnessAsPercentBR = 0,
                                                    cornerRadiusBR = 0.dp,
                                                    smoothnessAsPercentTL = 60,
                                                    cornerRadiusTL = cornerRadius,
                                                    smoothnessAsPercentBL = 0,
                                                    cornerRadiusBL = 0.dp,
                                                    smoothnessAsPercentTR = 60
                                                )
                                            }
                                            val middleShape = remember { RectangleShape }
                                            val bottomShape = remember(cornerRadius) {
                                                AbsoluteSmoothCornerShape(
                                                    cornerRadiusTR = 0.dp,
                                                    smoothnessAsPercentBR = 60,
                                                    cornerRadiusBR = cornerRadius,
                                                    smoothnessAsPercentTL = 0,
                                                    cornerRadiusTL = 0.dp,
                                                    smoothnessAsPercentBL = 60,
                                                    cornerRadiusBL = cornerRadius,
                                                    smoothnessAsPercentTR = 0
                                                )
                                            }
                                            val singleShape = remember(cornerRadius) {
                                                AbsoluteSmoothCornerShape(
                                                    cornerRadiusTR = cornerRadius,
                                                    smoothnessAsPercentBR = 60,
                                                    cornerRadiusBR = cornerRadius,
                                                    smoothnessAsPercentTL = 60,
                                                    cornerRadiusTL = cornerRadius,
                                                    smoothnessAsPercentBL = 60,
                                                    cornerRadiusBL = cornerRadius,
                                                    smoothnessAsPercentTR = 60
                                                )
                                            }

                                            val shape = remember(isFirst, isLast, cornerRadius) {
                                                when {
                                                    isFirst && isLast -> singleShape
                                                    isFirst -> topShape
                                                    isLast -> bottomShape
                                                    else -> middleShape
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(ListItemHeight)
                                                    .clip(shape)
                                                    .background(
                                                        if (isActive) {
                                                            MaterialTheme.colorScheme.secondaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainer
                                                        }
                                                    )
                                            ) {
                                                LibrarySongListItem(
                                                    song = songWrapper.item.song,
                                                    isActive = isActive,
                                                    isPlaying = isPlaying,
                                                    showInLibraryIcon = true,
                                                    isSwipeable = false,
                                                    trailingContent = {
                                                        IconButton(
                                                            onClick = {
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = songWrapper.item.song,
                                                                        playlistSong = songWrapper.item,
                                                                        playlistBrowseId = playlist.playlist.browseId,
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

                                                        if (sortType == PlaylistSongSortType.CUSTOM &&
                                                            !locked &&
                                                            !isSearching &&
                                                            editable
                                                        ) {
                                                            IconButton(
                                                                onClick = { },
                                                                modifier = Modifier.draggableHandle()
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.drag_handle),
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        }
                                                    },
                                                    isSelected = songWrapper.isSelected,
                                                    inSelectionMode = selection,
                                                    onSelectionChange = { songWrapper.isSelected = it },
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .combinedClickable(
                                                            onClick = {
                                                                songWrapper.isSelected = !songWrapper.isSelected
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.LongPress
                                                                )
                                                                if (!selection) {
                                                                    selection = true
                                                                }
                                                                wrappedSongs.forEach { it.isSelected = false }
                                                                songWrapper.isSelected = true
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        if (locked || !swipeRemoveEnabled || !editable) {
                                            songContent()
                                        } else {
                                            SwipeToDismissBox(
                                                state = dismissBoxState,
                                                backgroundContent = {
                                                    val color by animateColorAsState(
                                                        targetValue = when (dismissBoxState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd,
                                                            SwipeToDismissBoxValue.EndToStart,
                                                            -> MaterialTheme.colorScheme.errorContainer
                                                            else -> Color.Transparent
                                                        },
                                                        label = "swipe_background_color"
                                                    )
                                                    val cornerRadius = remember { 24.dp }

                                                    val topShape = remember(cornerRadius) {
                                                        AbsoluteSmoothCornerShape(
                                                            cornerRadiusTR = cornerRadius,
                                                            smoothnessAsPercentBR = 0,
                                                            cornerRadiusBR = 0.dp,
                                                            smoothnessAsPercentTL = 60,
                                                            cornerRadiusTL = cornerRadius,
                                                            smoothnessAsPercentBL = 0,
                                                            cornerRadiusBL = 0.dp,
                                                            smoothnessAsPercentTR = 60
                                                        )
                                                    }
                                                    val middleShape = remember { RectangleShape }
                                                    val bottomShape = remember(cornerRadius) {
                                                        AbsoluteSmoothCornerShape(
                                                            cornerRadiusTR = 0.dp,
                                                            smoothnessAsPercentBR = 60,
                                                            cornerRadiusBR = cornerRadius,
                                                            smoothnessAsPercentTL = 0,
                                                            cornerRadiusTL = 0.dp,
                                                            smoothnessAsPercentBL = 60,
                                                            cornerRadiusBL = cornerRadius,
                                                            smoothnessAsPercentTR = 0
                                                        )
                                                    }
                                                    val singleShape = remember(cornerRadius) {
                                                        AbsoluteSmoothCornerShape(
                                                            cornerRadiusTR = cornerRadius,
                                                            smoothnessAsPercentBR = 60,
                                                            cornerRadiusBR = cornerRadius,
                                                            smoothnessAsPercentTL = 60,
                                                            cornerRadiusTL = cornerRadius,
                                                            smoothnessAsPercentBL = 60,
                                                            cornerRadiusBL = cornerRadius,
                                                            smoothnessAsPercentTR = 60
                                                        )
                                                    }

                                                    val shape = remember(isFirst, isLast, cornerRadius) {
                                                        when {
                                                            isFirst && isLast -> singleShape
                                                            isFirst -> topShape
                                                            isLast -> bottomShape
                                                            else -> middleShape
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(shape)
                                                            .background(color)
                                                            .padding(horizontal = 20.dp),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.delete),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            ) {
                                                songContent()
                                            }
                                        }

                                        if (!isLast) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "songs_list_end") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 2
        )

        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
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
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (selection) {
                            selection = false
                        } else {
                            onBack()
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
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    RoundedCheckbox(
                        checked = count == wrappedSongs.size,
                        onCheckedChange = { checked ->
                            if (checked) {
                                wrappedSongs.forEach { it.isSelected = true }
                            } else {
                                wrappedSongs.forEach { it.isSelected = false }
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.song },
                                    songPosition = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.map },
                                    onDismiss = menuState::dismiss,
                                    clearAction = {
                                        selection = false
                                        wrappedSongs.clear()
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }

                    playlist?.let { playlistData ->
                        IconButton(
                            onClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = playlistData,
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
                    }
                }
            },
            colors = if (transparentAppBar && !selection) {
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

@Composable
fun PlaylistThumbnail(
    playlist: Playlist,
    editable: Boolean,
    menuState: com.music.vivi.ui.component.MenuState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    val overrideThumbnail = remember { mutableStateOf<String?>(null) }
    var isCustomThumbnail: Boolean = playlist.thumbnails.firstOrNull()?.let {
        it.contains("studio_square_thumbnail") || it.contains("content://com.music.vivi")
    } ?: false

    val result = remember { mutableStateOf<Uri?>(null) }
    var pendingCropDestUri by remember { mutableStateOf<Uri?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            val output = res.data?.let { UCrop.getOutput(it) } ?: pendingCropDestUri
            if (output != null) result.value = output
        }
    }

    val (darkMode, _) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val cropColor = MaterialTheme.colorScheme
    val darkTheme = darkMode == DarkMode.ON || (darkMode == DarkMode.AUTO && isSystemInDarkTheme())

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            val destFile = java.io.File(context.cacheDir, "playlist_cover_crop_${System.currentTimeMillis()}.jpg")
            val destUri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", destFile)
            pendingCropDestUri = destUri

            val options = UCrop.Options().apply {
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                setCompressionQuality(90)
                setHideBottomControls(true)
                setToolbarTitle(context.getString(R.string.edit_playlist_cover))
                setStatusBarLight(!darkTheme)
                setToolbarColor(cropColor.surface.toArgb())
                setToolbarWidgetColor(cropColor.inverseSurface.toArgb())
                setRootViewBackgroundColor(cropColor.surface.toArgb())
                setLogoColor(cropColor.surface.toArgb())
            }

            val intent = UCrop.of(sourceUri, destUri)
                .withAspectRatio(1f, 1f)
                .withOptions(options)
                .getIntent(context)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(intent)
        }
    }

    LaunchedEffect(result.value) {
        val uri = result.value ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            when {
                playlist.playlist.browseId == null -> {
                    overrideThumbnail.value = uri.toString()
                    isCustomThumbnail = true
                    database.query {
                        update(playlist.playlist.copy(thumbnailUrl = uri.toString()))
                    }
                }
                else -> {
                    val bytes = uriToByteArray(context, uri)
                    YouTube.uploadCustomThumbnailLink(
                        playlist.playlist.browseId,
                        bytes!!
                    ).onSuccess { newThumbnailUrl ->
                        overrideThumbnail.value = newThumbnailUrl
                        isCustomThumbnail = true
                        database.query {
                            update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl))
                        }
                    }.onFailure {
                        if (it is ClientRequestException) {
                            snackbarHostState.showSnackbar(
                                "${it.response.status.value} ${it.response.status.description}"
                            )
                        }
                        reportException(it)
                    }
                }
            }
        }
    }

    if (showEditNoteDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.edit_playlist_cover),
            onDismiss = { showEditNoteDialog = false },
            onConfirm = {
                showEditNoteDialog = false
                pickLauncher.launch(
                    PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCancel = { showEditNoteDialog = false }
        ) {
            if (playlist.playlist.browseId != null) {
                Text(
                    text = stringResource(R.string.edit_playlist_cover_note),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = stringResource(R.string.edit_playlist_cover_note_wait),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }

    when (playlist.thumbnails.size) {
        0 -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.8f),
                modifier = Modifier.size(96.dp)
            )
        }
        1 -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(overrideThumbnail.value ?: playlist.thumbnails[0])
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (editable) {
                    OverlayEditButton(
                        visible = true,
                        onClick = {
                            if (isCustomThumbnail) {
                                menuState.show {
                                    CustomThumbnailMenu(
                                        onEdit = {
                                            pickLauncher.launch(
                                                PickVisualMediaRequest(
                                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        onRemove = {
                                            when {
                                                playlist.playlist.browseId == null -> {
                                                    overrideThumbnail.value = null
                                                    database.query {
                                                        update(playlist.playlist.copy(thumbnailUrl = null))
                                                    }
                                                }
                                                else -> {
                                                    scope.launch(Dispatchers.IO) {
                                                        YouTube.removeThumbnailPlaylist(
                                                            playlist.playlist.browseId
                                                        ).onSuccess { newThumbnailUrl ->
                                                            overrideThumbnail.value = newThumbnailUrl
                                                            database.query {
                                                                update(
                                                                    playlist.playlist.copy(
                                                                        thumbnailUrl = newThumbnailUrl
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            isCustomThumbnail = false
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            } else {
                                showEditNoteDialog = true
                            }
                        },
                        alignment = Alignment.BottomEnd
                    )
                }
            }
        }
        else -> {
            Box(modifier = modifier) {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).fastForEachIndexed { index, alignment ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(overrideThumbnail.value ?: playlist.thumbnails.getOrNull(index))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)
                            .fillMaxSize(0.5f)
                    )
                }
                if (editable) {
                    OverlayEditButton(
                        visible = true,
                        onClick = {
                            if (isCustomThumbnail) {
                                menuState.show {
                                    CustomThumbnailMenu(
                                        onEdit = {
                                            pickLauncher.launch(
                                                PickVisualMediaRequest(
                                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        onRemove = {
                                            when {
                                                playlist.playlist.browseId == null -> {
                                                    overrideThumbnail.value = null
                                                    database.query {
                                                        update(playlist.playlist.copy(thumbnailUrl = null))
                                                    }
                                                }
                                                else -> {
                                                    scope.launch(Dispatchers.IO) {
                                                        YouTube.removeThumbnailPlaylist(
                                                            playlist.playlist.browseId
                                                        ).onSuccess { newThumbnailUrl ->
                                                            overrideThumbnail.value = newThumbnailUrl
                                                            database.query {
                                                                update(
                                                                    playlist.playlist.copy(
                                                                        thumbnailUrl = newThumbnailUrl
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            isCustomThumbnail = false
                                        },
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            } else {
                                showEditNoteDialog = true
                            }
                        },
                        alignment = Alignment.BottomEnd
                    )
                }
            }
        }
    }
}

fun uriToByteArray(context: Context, uri: Uri): ByteArray? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
} catch (_: SecurityException) {
    null
}
