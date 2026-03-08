/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.playlist

import android.content.Intent
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.YouTubePlaylistQueue
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSelectionSongMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.makeTimeString
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.core.net.toUri
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.OnlineBlur
import com.music.vivi.constants.AppBarHeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val relatedItems by viewModel.relatedItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val downloadUtil = LocalDownloadUtil.current
    val context = LocalContext.current

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs.mapIndexed { i, s -> i to s }
        else songs.mapIndexed { i, s -> i to s }.filter {
            it.second.title.contains(query.text, true) ||
                    it.second.artists.fastAny { a -> a.name.contains(query.text, true) }
        }
    }

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

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.second.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 150
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            if (playlist == null || songs.isEmpty()) {
                if (isLoading) {
                    item(key = "loading_placeholder") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                }
            } else {
                playlist?.let { playlist ->
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            OnlinePlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                dbPlaylist = dbPlaylist,
                                downloadState = downloadState,
                                navController = navController,
                                coroutineScope = coroutineScope,
                                continuation = viewModel.continuation
                            )
                        }
                    }

                    itemsIndexed(filteredSongs) { index, (_, songItem) ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(songItem.id)
                            } else {
                                selection.remove(songItem.id)
                            }
                        }

                        YouTubeListItem(
                            item = songItem,
                            isActive = mediaMetadata?.id == songItem.id,
                            isPlaying = isPlaying,
                            isSelected = inSelectMode && songItem.id in selection,
                            shape = listItemShape(index, filteredSongs.size),
                            modifier = Modifier
                                .combinedClickable(
                                    enabled = !hideExplicit || !songItem.explicit,
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(songItem.id !in selection)
                                        } else if (songItem.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubePlaylistQueue(
                                                    playlistId = playlist.id,
                                                    playlistTitle = playlist.title,
                                                    initialSongs = filteredSongs.map { it.second },
                                                    initialContinuation = viewModel.continuation,
                                                    startIndex = index
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
                                .animateItem(),
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = songItem.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(songItem, navController, menuState::dismiss)
                                        }
                                    }) {
                                        Icon(painterResource(R.drawable.more_vert), null)
                                    }
                                }
                            }
                        )
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }

                    if (relatedItems.isNotEmpty() && !isSearching) {
                        item(key = "related_title") {
                            NavigationTitle(
                                title = stringResource(R.string.you_might_also_like),
                                modifier = Modifier.animateItem()
                            )
                        }

                        item(key = "related_items") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                items(relatedItems) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        modifier = Modifier
                                            .width(160.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        is AlbumItem -> navController.navigate("album/${item.browseId}")
                                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                                        is SongItem -> playerConnection.playQueue(
                                                            YouTubeQueue(WatchEndpoint(videoId = item.id))
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        when (item) {
                                                            is PlaylistItem -> YouTubePlaylistMenu(
                                                                playlist = item,
                                                                coroutineScope = coroutineScope,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                            is SongItem -> YouTubeSongMenu(
                                                                song = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                            is AlbumItem -> YouTubeAlbumMenu(
                                                                albumItem = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                            is ArtistItem -> YouTubeArtistMenu(
                                                                artist = item,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(Modifier.height(50.dp))
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (inSelectMode) {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
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
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else {
                    AnimatedContent(
                        targetState = !transparentAppBar,
                        transitionSpec = {
                            fadeIn().togetherWith(fadeOut())
                        },
                        label = "TopAppBarTitle"
                    ) { show ->
                        if (show) {
                            Text(playlist?.title ?: "")
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (inSelectMode) {
                            onExitSelectionMode()
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !inSelectMode) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (inSelectMode) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(
                        checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                        onCheckedChange = {
                            if (selection.size == filteredSongs.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                selection.addAll(filteredSongs.map { it.second.id })
                            }
                        }
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
                                YouTubeSelectionSongMenu(
                                    songSelection = filteredSongs.filter { it.second.id in selection }
                                        .map { it.second },
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode
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
            colors = if (transparentAppBar && !isSearching && !inSelectMode) {
                androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlinePlaylistHeader(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    dbPlaylist: Playlist?,
    downloadState: Int,
    navController: NavController,
    coroutineScope: CoroutineScope,
    continuation: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val hasExplicitContent = remember(songs) {
        songs.any { it.explicit }
    }

    val density = LocalDensity.current
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        OnlineBlur(
            thumbnailUrl = playlist.thumbnail,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .offset { IntOffset(0, headerOffset) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp)) // Space for top app bar

            // Artwork - Large and centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(playlist.thumbnail)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(32.dp))

            // Title with Logo Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.album),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons Row (Save, Play, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Button
                Button(
                    onClick = {
                        if (dbPlaylist != null) {
                            database.transaction {
                                val currentPlaylist = dbPlaylist.playlist
                                update(currentPlaylist, playlist)
                                update(currentPlaylist.toggleLike())
                            }
                        } else {
                            database.transaction {
                                val playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = playlist.isEditable,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                ).toggleLike()
                                insert(playlistEntity)
                                coroutineScope.launch(Dispatchers.IO) {
                                    songs.map { it.toMediaMetadata() }
                                        .onEach(::insert)
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index,
                                                setVideoId = song.setVideoId
                                            )
                                        }
                                        .forEach(::insert)
                                }
                            }
                        }
                    },
                    shape = ButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (dbPlaylist?.playlist?.bookmarkedAt != null) stringResource(R.string.saved) else stringResource(
                                R.string.save
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Play Button
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                YouTubePlaylistQueue(
                                    playlistId = playlist.id,
                                    playlistTitle = playlist.title,
                                    initialSongs = songs,
                                    initialContinuation = continuation
                                )
                            )
                        }
                    },
                    shape = ButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying && mediaMetadata?.album?.id == playlist.id) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying && mediaMetadata?.album?.id == playlist.id) stringResource(
                                R.string.pause
                            ) else stringResource(R.string.play),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Share Button
                Surface(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
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
                            painter = painterResource(R.drawable.share),
                            contentDescription = stringResource(R.string.share),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Explicit Label — only shown when playlist contains explicit songs
            if (hasExplicitContent) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.explicit),
                            contentDescription = "Explicit",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.explicit),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }


            // Metadata Info
            val totalDuration = songs.sumOf { it.duration ?: 0 }
            Text(
                text = buildString {
                    append(pluralStringResource(R.plurals.n_song, songs.size, songs.size))
                    if (totalDuration > 0) {
                        append(" • ")
                        append(makeTimeString(totalDuration * 1000L))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            // Author Name
            playlist.author?.name?.let { authorName ->
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Toggle Buttons Row (Download, Shuffle, More)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                // Download Toggle
                ToggleButton(
                    checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                    onCheckedChange = {
                        when (downloadState) {
                            Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            }

                            else -> {
                                songs.forEach { song ->
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.title.toByteArray())
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
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
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
                                contentDescription = stringResource(R.string.action_download),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = when (downloadState) {
                            Download.STATE_COMPLETED -> stringResource(R.string.saved)
                            Download.STATE_DOWNLOADING -> stringResource(R.string.saving)
                            else -> stringResource(R.string.save_album)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Shuffle Button
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                YouTubePlaylistQueue(
                                    playlistId = playlist.id,
                                    playlistTitle = playlist.title,
                                    initialSongs = songs.shuffled(),
                                    initialContinuation = continuation
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
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

                // More Options Button
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        menuState.show {
                            YouTubePlaylistMenu(
                                playlist = playlist,
                                songs = songs,
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = stringResource(R.string.more_options),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(
                        stringResource(R.string.more_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}