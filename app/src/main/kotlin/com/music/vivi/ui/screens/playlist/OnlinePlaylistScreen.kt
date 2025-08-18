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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.metadata
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
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
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.OnlinePlaylistViewModel

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.derivedStateOf
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.ui.utils.fadingEdge
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val density = LocalDensity.current

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) {
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

    // Calculate the offset value for header (same as AlbumScreen)
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                .asPaddingValues(),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    item(key = "shimmer") {
                        ShimmerHost(
                            modifier = Modifier
                                .offset { IntOffset(x = 0, y = headerOffset) }
                        ) {
                            // Playlist Image Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shimmer()
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                            // Playlist Info Section
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Playlist Title Placeholder
                                TextPlaceholder(
                                    height = 28.dp,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )

                                // Author Name Placeholder
                                TextPlaceholder(
                                    height = 20.dp,
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                        .padding(vertical = 8.dp)
                                )

                                // Buttons Row Placeholder
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shimmer()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(8.dp)
                                        )
                                        if (it < 3) Spacer(modifier = Modifier.width(16.dp))
                                    }
                                }
                            }
                            // Songs List Placeholder
                            repeat(6) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                } else if (playlist != null) {
                    if (!isSearching) {
                        item(key = "header") {
                            Box {
                                // Playlist Image with offset (same as AlbumScreen)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .offset { IntOffset(x = 0, y = headerOffset) }
                                ) {
                                    AsyncImage(
                                        model = playlist.thumbnail,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .fadingEdge(bottom = 200.dp)
                                    )
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
                                    // Playlist Title
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 28.sp
                                    )

                                    // Author Name
                                    playlist.author?.let { artist ->
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Normal
                                                    ).toSpanStyle()
                                                ) {
                                                    if (artist.id != null) {
                                                        val link = LinkAnnotation.Clickable(artist.id!!) {
                                                            navController.navigate("artist/${artist.id!!}")
                                                        }
                                                        withLink(link) {
                                                            append(artist.name)
                                                        }
                                                    } else {
                                                        append(artist.name)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }

                                    // Song count
                                    playlist.songCountText?.let { songCountText ->
                                        Text(
                                            text = songCountText,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Action Buttons Row (same style as AlbumScreen)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Shuffle Button (primary since it's main action)
                                        playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                            IconButton(
                                                onClick = {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(shuffleEndpoint)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shuffle),
                                                    contentDescription = stringResource(R.string.shuffle),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }

                                        // Radio Button
                                        playlist.radioEndpoint?.let { radioEndpoint ->
                                            IconButton(
                                                onClick = {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(radioEndpoint)
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.radio),
                                                    contentDescription = stringResource(R.string.radio),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }

                                        // Like Button (if not "LM" playlist)
                                        if (playlist.id != "LM") {
                                            IconButton(
                                                onClick = {
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
                                                            val currentPlaylist = dbPlaylist!!.playlist
                                                            update(currentPlaylist, playlist)
                                                            update(currentPlaylist.toggleLike())
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
                                                        if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                            R.drawable.favorite
                                                        } else {
                                                            R.drawable.favorite_border
                                                        }
                                                    ),
                                                    contentDescription = stringResource(R.string.favourite),
                                                    tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }

                                        // More Options Button
                                        IconButton(
                                            onClick = {
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
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = stringResource(R.string.more_options),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
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

                    items(
                        items = wrappedSongs,
                        key = { song -> song.item.second.id }
                    ) { song ->
                        YouTubeListItem(
                            item = song.item.second,
                            isActive = mediaMetadata?.id == song.item.second.id,
                            isPlaying = isPlaying,
                            isSelected = song.isSelected && selection,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song.item.second,
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
                                .combinedClickable(
                                    enabled = !hideExplicit || !song.item.second.explicit,
                                    onClick = {
                                        if (!selection) {
                                            if (song.item.second.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId = playlist.id)
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        song.item.second.endpoint
                                                            ?: WatchEndpoint(
                                                                videoId = song.item.second.id,
                                                            ),
                                                        song.item.second.toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        } else {
                                            song.isSelected = !song.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSongs.forEach { it.isSelected = false }
                                        song.isSelected = true
                                    },
                                )
                                .animateItem(),
                        )
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
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 1
        )

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
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    else -> {
                        if (!transparentAppBar) Text(playlist?.title.orEmpty())
                    }
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
                        contentDescription = null
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
                            contentDescription = null
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
                            contentDescription = null
                        )
                    }
                } else {
                    if (!isSearching) {
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
            colors = if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}