package com.music.vivi.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.SongItem
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.DraggableScrollbar
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.SelectionMediaMetadataMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.OnlinePlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember {
        mutableStateOf(false)
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs =
        remember(songs, query, hideExplicit) {
            var result = if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.fastAny {
                                    it.name.contains(
                                        query.text,
                                        ignoreCase = true
                                    )
                                }
                    }
            }

            // Filter explicit content if needed
            if (hideExplicit) {
                result = result.filter { !it.second.explicit }
            }
            result
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
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    val density = LocalDensity.current
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 100
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            playlist.let { playlist ->
                if (isLoading) {
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
                } else if (playlist != null) {
                    if (!isSearching) {
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
                                        model = playlist.thumbnail,
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
                                            text = playlist.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        // Author in a styled box
                                        playlist.author?.let { author ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable(
                                                        indication = null,
                                                        interactionSource = remember { MutableInteractionSource() }
                                                    ) {
                                                        if (author.id != null) {
                                                            navController.navigate("artist/${author.id}")
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = author.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Song count text
                                        playlist.songCountText?.let { songCountText ->
                                            Text(
                                                text = songCountText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Action icons on the right (Like, More)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Like Button Box
                                            if (playlist.id != "LM") {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            if (dbPlaylist?.playlist == null) {
                                                                database.transaction {
                                                                    val playlistEntity = PlaylistEntity(
                                                                        name = playlist.title,
                                                                        browseId = playlist.id,
                                                                        thumbnailUrl = playlist.thumbnail,
                                                                        isEditable = playlist.isEditable,
                                                                        playEndpointParams = playlist.playEndpoint?.params,
                                                                        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                                        radioEndpointParams = playlist.radioEndpoint?.params
                                                                    ).toggleLike()
                                                                    insert(playlistEntity)
                                                                    songs.map(SongItem::toMediaMetadata)
                                                                        .onEach(::insert)
                                                                        .mapIndexed { index, song ->
                                                                            PlaylistSongMap(
                                                                                songId = song.id,
                                                                                playlistId = playlistEntity.id,
                                                                                position = index
                                                                            )
                                                                        }
                                                                        .forEach(::insert)
                                                                }
                                                            } else {
                                                                database.transaction {
                                                                    // Update playlist information including thumbnail before toggling like
                                                                    val currentPlaylist = dbPlaylist!!.playlist
                                                                    update(currentPlaylist, playlist)
                                                                    update(currentPlaylist.toggleLike())
                                                                }
                                                            }
                                                        }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(
                                                            if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                                        ),
                                                        contentDescription = null,
                                                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                            MaterialTheme.colorScheme.error
                                                        } else {
                                                            LocalContentColor.current
                                                        },
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }

                                            // More Options Button Box
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        menuState.show {
                                                            YouTubePlaylistMenu(
                                                                playlist = playlist,
                                                                songs = songs,
                                                                coroutineScope = coroutineScope,
                                                                onDismiss = menuState::dismiss,
                                                                selectAction = { selection = true },
                                                                canSelect = true,
                                                            )
                                                        }
                                                    }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        // Play and Shuffle Buttons in separate boxes
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Play Button
                                            Surface(
                                                onClick = {
                                                    val isCurrentPlaylist = songs.any { it.id == mediaMetadata?.id }

                                                    if (isCurrentPlaylist) {
                                                        // Toggle play/pause for current playlist
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        // Play the playlist from start
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = playlist?.title ?: "",
                                                                items = songs.map { it.toMediaItem() },
                                                            )
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                color = if (isPlaying && songs.any { it.id == mediaMetadata?.id }) {
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
                                                        painter = painterResource(
                                                            if (isPlaying && songs.any { it.id == mediaMetadata?.id })
                                                                R.drawable.pause
                                                            else
                                                                R.drawable.play
                                                        ),
                                                        contentDescription = null,
                                                        tint = if (isPlaying && songs.any { it.id == mediaMetadata?.id }) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            LocalContentColor.current
                                                        },
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(
                                                            if (isPlaying && songs.any { it.id == mediaMetadata?.id })
                                                                R.string.pause
                                                            else
                                                                R.string.play
                                                        ),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isPlaying && songs.any { it.id == mediaMetadata?.id }) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            LocalContentColor.current
                                                        }
                                                    )
                                                }
                                            }

// Shuffle Button
                                            Surface(
                                                onClick = {
                                                    val shuffledSongs = songs.map { it.toMediaItem() }.shuffled()
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist?.title ?: "",
                                                            items = shuffledSongs,
                                                        )
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

                        // Songs Section Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.songs),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        // Show empty playlist message when playlist is loaded but has no songs
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_playlist),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_playlist_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Songs List
                    if (!filteredSongs.isNullOrEmpty()) {
                        itemsIndexed(
                            items = wrappedSongs,
                            key = { _, song -> song.item.second.id },
                        ) { index, songWrapper ->
                            YouTubeListItem(
                                item = songWrapper.item.second,
                                isActive = mediaMetadata?.id == songWrapper.item.second.id,
                                isPlaying = isPlaying,
                                isSelected = songWrapper.isSelected && selection,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = songWrapper.item.second,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (songWrapper.item.second.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist.title,
                                                            items = filteredSongs.map { it.second.toMediaItem() },
                                                            startIndex = index
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
                                    ),
                            )
                        }
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item {
                            ShimmerHost {
                                repeat(2) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }

                } else {
                    // Show error state when playlist is null and there's an error
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (error != null) {
                                    stringResource(R.string.error_unknown)
                                } else {
                                    stringResource(R.string.playlist_not_found)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (error != null) {
                                    error!!
                                } else {
                                    stringResource(R.string.playlist_not_found_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.retry()
                                    }
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 1
        )

        // Top App Bar
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
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) {
                    Text(
                        text = playlist?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        contentDescription = null,
                        tint = if (transparentAppBar) Color.White else LocalContentColor.current
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null,
                            tint = if (transparentAppBar) Color.White else LocalContentColor.current
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.second.toMediaItem().metadata!! },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = if (transparentAppBar) Color.White else LocalContentColor.current
                        )
                    }
                } else {
                    if (!isSearching) {
                        IconButton(
                            onClick = { isSearching = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                tint = if (transparentAppBar) Color.White else LocalContentColor.current
                            )
                        }
                    }
                }
            },
            colors = if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter),
        )
    }
}