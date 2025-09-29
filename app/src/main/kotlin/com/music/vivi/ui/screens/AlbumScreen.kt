package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Album
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.component.AutoResizeText
import com.music.vivi.ui.component.FontSizeRange
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable

fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }
    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
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
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Album Image with offset (goes under app bar)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset {
                                    IntOffset(x = 0, y = headerOffset)
                                }
                        ) {
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl,
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
                                // Album Title
                                Text(
                                    text = albumWithSongs.album.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                // Artists in a styled box

                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(albumWithSongs.artists) { artist ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    navController.navigate("artist/${artist.id}")
                                                }
                                        ) {
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Action icons on the right (Like, Download, More)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Like Button Box
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                database.query {
                                                    update(albumWithSongs.album.toggleLike())
                                                }
                                            }
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (albumWithSongs.album.bookmarkedAt != null) {
                                                    R.drawable.favorite
                                                } else {
                                                    R.drawable.favorite_border
                                                }
                                            ),
                                            contentDescription = null,
                                            tint = if (albumWithSongs.album.bookmarkedAt != null) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                LocalContentColor.current
                                            },
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }

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
                                                        albumWithSongs.songs.forEach { song ->
                                                            DownloadService.sendRemoveDownload(
                                                                context,
                                                                ExoDownloadService::class.java,
                                                                song.id,
                                                                false,
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        albumWithSongs.songs.forEach { song ->
                                                            val downloadRequest =
                                                                DownloadRequest
                                                                    .Builder(song.id, song.id.toUri())
                                                                    .setCustomCacheKey(song.id)
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
                                                    AlbumMenu(
                                                        originalAlbum = Album(
                                                            albumWithSongs.album,
                                                            albumWithSongs.artists
                                                        ),
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
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
                                    // Play Button
                                    Surface(
                                        onClick = {
                                            if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(albumWithSongs),
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) // Active state color
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
                                                    if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                                        R.drawable.pause
                                                    else
                                                        R.drawable.play
                                                ),
                                                contentDescription = null,
                                                tint = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                                    MaterialTheme.colorScheme.primary // Active icon color
                                                } else {
                                                    LocalContentColor.current
                                                },
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(
                                                    if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                                        R.string.pause
                                                    else
                                                        R.string.play
                                                ),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                                    MaterialTheme.colorScheme.primary // Active text color
                                                } else {
                                                    LocalContentColor.current
                                                }
                                            )
                                        }
                                    }

                                    // Shuffle Button
                                    Surface(
                                        onClick = {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = if (shuffleModeEnabled) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Same as artist box
                                        },
                                        shape = RoundedCornerShape(12.dp) // Same rounded corners as artist box
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), // Same padding as artist box
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

                // Top Tracks Section Header
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

                // Songs List
                if (!wrappedSongs.isNullOrEmpty()) {
                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
//                            albumIndex = index + 1,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
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
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(albumWithSongs, startIndex = index),
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
                                        wrappedSongs.forEach {
                                            it.isSelected = false
                                        }
                                        songWrapper.isSelected = true
                                    },
                                ),
                        )
                    }
                }

                // Other Versions Section
                if (otherVersions.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(
                                items = otherVersions.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    ShimmerHost {
                        Column(Modifier.padding(16.dp)) {
                            // Shimmer for album artwork
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

        // Top App Bar
        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs?.count { it.isSelected } ?: 0
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    if (!transparentAppBar) {
                        Text(
                            text = albumWithSongs?.album?.title.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!selection) {
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
                                    clearAction = { selection = false }
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