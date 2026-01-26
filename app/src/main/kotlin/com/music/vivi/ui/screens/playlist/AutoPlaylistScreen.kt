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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.SongSortDescendingKey
import com.music.vivi.constants.SongSortType
import com.music.vivi.constants.SongSortTypeKey
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.EmptyPlaceholder
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.RoundedCheckbox
import com.music.vivi.ui.component.SortHeader
import com.music.vivi.ui.menu.AutoPlaylistMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AutoPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlaylistType {
    LIKE,
    DOWNLOAD,
    UPLOADED,
    OTHER,
}

/**
 * Screen for displaying auto-generated playlists like Liked Songs, Offline, Uploaded, etc.
 * These playlists are "virtual" or system-managed rather than user-created local playlists.
 *
 * @param playlistId The ID/type of the auto playlist (e.g., "liked", "downloaded", "uploaded").
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    playlistId: String? = null,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
    onBack: () -> Unit = { navController.navigateUp() },
) {
    if (playlistId != null) {
        LaunchedEffect(playlistId) {
            viewModel.setPlaylist(playlistId)
        }
    }
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // FIX: Collect the playlist ID from the ViewModel's StateFlow
    val currentPlaylistId by viewModel.playlist.collectAsState()

    val playlistTitle = when (currentPlaylistId) {
        "liked" -> stringResource(R.string.liked)
        "uploaded" -> stringResource(R.string.uploaded_playlist)
        else -> stringResource(R.string.offline)
    }

    val songs: List<com.music.vivi.db.entities.Song>? by viewModel.likedSongs.collectAsState(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    var sortType by rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    var sortDescending by rememberPreference(SongSortDescendingKey, true)

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    // FIX: Use the collected playlist ID
    val playlistType = when (currentPlaylistId) {
        "liked" -> PlaylistType.LIKE
        "downloaded" -> PlaylistType.DOWNLOAD
        "uploaded" -> PlaylistType.UPLOADED
        else -> PlaylistType.OTHER
    }

    val wrappedSongs = remember(songs) {
        songs?.map { item -> ItemWrapper(item) }?.toMutableStateList() ?: mutableStateListOf()
    }

    var selection by remember { mutableStateOf(false) }

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
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    // Defer sync to background after UI renders
    LaunchedEffect(Unit) {
        if (ytmSync) {
            launch(Dispatchers.IO) {
                delay(300) // Let UI render first
                if (playlistType == PlaylistType.LIKE) viewModel.syncLikedSongs()
                if (playlistType == PlaylistType.UPLOADED) viewModel.syncUploadedSongs()
            }
        }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
    }

    // Defer download state check to avoid blocking UI
    LaunchedEffect(songs) {
        if (songs?.isEmpty() == true) return@LaunchedEffect

        delay(500) // Wait for UI to fully render first

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
                    text = stringResource(R.string.remove_download_playlist_confirm, playlistTitle),
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

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) {
            wrappedSongs
        } else {
            wrappedSongs.filter { wrapper ->
                val song = wrapper.item
                song.song.title.contains(query.text, true) ||
                    song.artists.any { it.name.contains(query.text, true) }
            }
        }
    }

    val state = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset < 200
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty)
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

                                // Playlist Artwork - Large and centered
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp)
                                ) {
                                    AsyncImage(
                                        model = songs!![0].song.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(Modifier.height(32.dp))

                                // Playlist Title with Logo Icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.new_album_vivi),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = playlistTitle,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        12.dp,
                                        Alignment.CenterHorizontally
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play Button
                                    Surface(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlistTitle,
                                                    items = songs!!.map { it.toMediaItem() }
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
                                                text = stringResource(R.string.play),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }

                                    // Shuffle Button
                                    Surface(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlistTitle,
                                                    items = songs!!.shuffled()
                                                        .map { it.toMediaItem() }
                                                )
                                            )
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
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.shuffle),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                // Playlist Info
                                Text(
                                    text = buildString {
                                        append(
                                            pluralStringResource(
                                                R.plurals.n_song,
                                                songs!!.size,
                                                songs!!.size
                                            )
                                        )
                                        append(" • ${makeTimeString(likeLength * 1000L)}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )

                                Spacer(Modifier.height(16.dp))

                                // Playlist Description
                                Text(
                                    text = when (playlistType) {
                                        PlaylistType.LIKE -> "Your favorite tracks all in one place. Songs you've liked across YouTube Music, automatically synced and ready to play."
                                        PlaylistType.DOWNLOAD -> "All your downloaded songs available offline. Listen anytime, anywhere without an internet connection."
                                        PlaylistType.UPLOADED -> "Your personal music collection. Songs you've uploaded to YouTube Music from your library."
                                        else -> "Your curated collection of songs, perfectly organized for your listening pleasure."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(24.dp))

                                // Additional action buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        ButtonGroupDefaults.ConnectedSpaceBetween
                                    )
                                ) {
                                    // Download Button
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
                                                    songs!!.forEach { song ->
                                                        val downloadRequest =
                                                            DownloadRequest
                                                                .Builder(
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
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                            .semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    ) {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> {
                                                Icon(
                                                    painter = painterResource(R.drawable.offline),
                                                    contentDescription = "saved",
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
                                                    contentDescription = "save",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text(
                                            text = when (downloadState) {
                                                Download.STATE_COMPLETED -> "saved"
                                                Download.STATE_DOWNLOADING -> "saving"
                                                else -> "save"
                                            },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    // Add to Queue Button
                                    ToggleButton(
                                        checked = false,
                                        onCheckedChange = {
                                            playerConnection.addToQueue(
                                                items = songs!!.map { it.toMediaItem() }
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                            .semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.queue_music),
                                            contentDescription = "Add to queue",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text("Queue", style = MaterialTheme.typography.labelMedium)
                                    }

                                    // More Options Button (includes sort and other options)
                                    ToggleButton(
                                        checked = false,
                                        onCheckedChange = {
                                            menuState.show {
                                                AutoPlaylistMenu(
                                                    name = playlistTitle,
                                                    songs = songs ?: emptyList(),
                                                    downloadState = downloadState,
                                                    onDownload = {
                                                        when (downloadState) {
                                                            Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                                                showRemoveDownloadDialog = true
                                                            }

                                                            else -> {
                                                                songs!!.forEach { song ->
                                                                    val downloadRequest =
                                                                        DownloadRequest
                                                                            .Builder(
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
                                                            }
                                                        }
                                                    },
                                                    onDismiss = { menuState.dismiss() }
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                            .semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = "More options",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text("More", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }

                    item(key = "songs_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = { sortType = it },
                                onSortDescendingChange = { sortDescending = it },
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        SongSortType.NAME -> R.string.sort_by_name
                                        SongSortType.ARTIST -> R.string.sort_by_artist
                                        SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                }
                            )
                        }
                    }
                }

                // Songs List with Quick Pick style - Using itemsIndexed for lazy rendering
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.item.song.id }
                ) { index, songWrapper ->
                    val isFirst = index == 0
                    val isLast = index == filteredSongs.size - 1
                    val isActive = songWrapper.item.song.id == mediaMetadata?.id
                    val isSingleSong = filteredSongs.size == 1

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
                                        bottomStart = if (isLast && !isSingleSong) 20.dp else 0.dp,
                                        bottomEnd = if (isLast && !isSingleSong) 20.dp else 0.dp
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
                                isActive = isActive,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                isSwipeable = false,
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
                                                if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlistTitle,
                                                            items = songs!!.map { it.toMediaItem() },
                                                            startIndex = songs!!.indexOfFirst {
                                                                it.id ==
                                                                    songWrapper.item.id
                                                            }
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
                                        }
                                    )
                            )
                        }

                        // Add 3dp spacer between items (except after last)
                        if (!isLast) {
                            Spacer(modifier = Modifier.height(3.dp))
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // Top App Bar
        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    isSearching -> {
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
                    else -> {
                        Text(
                            text = playlistTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        when {
                            isSearching -> {
                                isSearching = false
                                query = TextFieldValue()
                                focusManager.clearFocus()
                            }
                            selection -> {
                                selection = false
                            }
                            else -> {
                                onBack()
                            }
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
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs?.count { it.isSelected } ?: 0
                    RoundedCheckbox(
                        checked = count == wrappedSongs?.size,
                        onCheckedChange = { checked ->
                            if (checked) {
                                wrappedSongs?.forEach { it.isSelected = true }
                            } else {
                                wrappedSongs?.forEach { it.isSelected = false }
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp)
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
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (transparentAppBar &&
                    !selection &&
                    !isSearching
                ) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            scrollBehavior = scrollBehavior
        )
    }
}
