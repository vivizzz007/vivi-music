package com.music.vivi.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.SongSortDescendingKey
import com.music.vivi.constants.SongSortType
import com.music.vivi.constants.SongSortTypeKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.constants.YtmSyncKey
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
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
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AutoPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable

fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val playlist = when (viewModel.playlist) {
        "liked" -> stringResource(R.string.liked)
        "uploaded" -> stringResource(R.string.uploaded_playlist)
        else -> stringResource(R.string.offline)
    }

    val songs by viewModel.likedSongs.collectAsState(null)
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

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    val playlistId = viewModel.playlist
    val playlistType = when (playlistId) {
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

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
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
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist),
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

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs?.filter { wrapper ->
            val song = wrapper.item
            song.song.title.contains(query.text, true) ||
                    song.artists.any { it.name.contains(query.text, true) }
        }
    }

    val density = LocalDensity.current
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize()
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
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Playlist Image with offset (goes under app bar)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .offset {
                                        IntOffset(x = 0, y = headerOffset)
                                    }
                            ) {
                                AsyncImage(
                                    model = songs!![0].song.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .fadingEdge(
                                            bottom = 200.dp,
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Content positioned at bottom of image
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = LocalResources.current.displayMetrics.widthPixels.let { screenWidth ->
                                            with(density) {
                                                ((screenWidth / 1.5f) - 100).toDp()
                                            }
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                                    MaterialTheme.colorScheme.background
                                                )
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Playlist Title
                                    Text(
                                        text = playlist,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // Playlist Info in styled boxes
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Song Count
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Text(
                                                text = pluralStringResource(
                                                    R.plurals.n_song,
                                                    songs!!.size,
                                                    songs!!.size,
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }

                                        // Duration
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        ) {
                                            Text(
                                                text = makeTimeString(likeLength * 1000L),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // Action icons on the right (Download, Queue, More)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Download Button Box
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    when (downloadState) {
                                                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                                            showRemoveDownloadDialog = true
                                                        }
                                                        else -> {
                                                            songs!!.forEach { song ->
                                                                val downloadRequest =
                                                                    DownloadRequest
                                                                        .Builder(song.song.id, song.song.id.toUri())
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
                                                        }
                                                    }
                                                }
                                        ) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    Icon(
                                                        painter = painterResource(R.drawable.offline),
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                                Download.STATE_DOWNLOADING -> {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .padding(8.dp),
                                                    )
                                                }
                                                else -> {
                                                    Icon(
                                                        painter = painterResource(R.drawable.download),
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Queue Button Box
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    playerConnection.addToQueue(
                                                        items = songs!!.map { it.toMediaItem() },
                                                    )
                                                }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = null,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Play and Shuffle Buttons in separate boxes
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Play/Pause Button
                                        Surface(
                                            onClick = {
                                                // Check if current playlist is playing
                                                val isCurrentPlaylistPlaying = isPlaying &&
                                                        songs?.any { it.song.id == mediaMetadata?.id } == true

                                                if (isCurrentPlaylistPlaying) {
                                                    // Pause if currently playing this playlist
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    // Play the playlist
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist,
                                                            items = songs!!.map { it.toMediaItem() },
                                                        ),
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isCurrentPlaylistPlaying = isPlaying &&
                                                        songs?.any { it.song.id == mediaMetadata?.id } == true

                                                Icon(
                                                    painter = painterResource(
                                                        if (isCurrentPlaylistPlaying) R.drawable.pause
                                                        else R.drawable.play
                                                    ),
                                                    contentDescription = null,
                                                    tint = if (isCurrentPlaylistPlaying) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        LocalContentColor.current
                                                    },
                                                    modifier = Modifier.size(20.dp),
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(
                                                        if (isCurrentPlaylistPlaying) R.string.pause
                                                        else R.string.play
                                                    ),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isCurrentPlaylistPlaying) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        LocalContentColor.current
                                                    }
                                                )
                                            }
                                        }

                                        // Shuffle Button (unchanged)
                                        Surface(
                                            onClick = {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist,
                                                        items = songs!!.shuffled().map { it.toMediaItem() },
                                                    ),
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            color = if (shuffleModeEnabled) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shuffle),
                                                    contentDescription = null,
                                                    tint = if (shuffleModeEnabled) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        LocalContentColor.current
                                                    },
                                                    modifier = Modifier.size(20.dp),
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.shuffle),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (shuffleModeEnabled) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        LocalContentColor.current
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // Sort Header
                    if (!isSearching) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.songs),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

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
                                    },
                                )
                            }
                        }
                    }

                    // Songs List
                    if (filteredSongs != null) {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _, song -> song.item.id },
                        ) { index, songWrapper ->
                            SongListItem(
                                song = songWrapper.item,
                                isActive = songWrapper.item.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                showInLibraryIcon = true,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = songWrapper.item,
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
                                                            title = playlist,
                                                            items = songs!!.map { it.toMediaItem() },
                                                            startIndex = songs!!.indexOfFirst { it.id == songWrapper.item.id }
                                                        ),
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
                                                wrappedSongs?.forEach { it.isSelected = false }
                                                songWrapper.isSelected = true
                                            }
                                        },
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }
            } else {
                // Loading state
                item {
                    ShimmerHost {
                        Column(Modifier.padding(16.dp)) {
                            // Shimmer for playlist artwork
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(1f)
                                    .align(Alignment.CenterHorizontally)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface),
                            )

                            Spacer(Modifier.height(24.dp))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextPlaceholder()
                                TextPlaceholder()
                            }

                            Spacer(Modifier.height(24.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ButtonPlaceholder(Modifier.weight(1f))
                                ButtonPlaceholder(Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        repeat(6) {
                            ListItemPlaceHolder()
                        }
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

        // Top App Bar
        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs?.count { it.isSelected } ?: 0
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
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    else -> {
                        if (!transparentAppBar) {
                            Text(
                                text = playlist,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                            }
                            selection -> {
                                selection = false
                            }
                            else -> {
                                navController.navigateUp()
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
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs?.size) {
                                wrappedSongs.forEach { it.isSelected = false }
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
            colors = if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior
        )
    }
}

enum class PlaylistType {
    LIKE, DOWNLOAD, UPLOADED, OTHER
}