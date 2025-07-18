package com.music.vivi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AlbumThumbnailSize
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
import com.music.vivi.viewmodels.AlbumViewModel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.derivedStateOf

import androidx.compose.ui.graphics.Color
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.db.entities.ArtistEntity

import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.utils.resize
import com.music.vivi.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.Surface
import kotlin.collections.shuffle
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

    val wrappedSongs = albumWithSongs?.songs?.map { item -> ItemWrapper(item) }?.toMutableList()
    var selection by remember {
        mutableStateOf(false)
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    // Add LazyListState for scroll behavior
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if current album is playing
    val isCurrentAlbumPlaying = mediaMetadata?.album?.id == albumWithSongs?.album?.id

    // Transparent app bar logic
    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0
        }
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .add(
                    WindowInsets(
                        top = -WindowInsets.systemBars.asPaddingValues()
                            .calculateTopPadding() - AppBarHeight
                    )
                )
                .asPaddingValues(),
        ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item(key = "header") {
                    Column {
                        // Album Image with fading edge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f), // Square aspect ratio for album
                        ) {
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl?.resize(1000, 1000),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .fadingEdge(
                                        bottom = 200.dp,
                                    ),
                            )
                        }

                        // Album Info and Controls Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
                        ) {
                            // Album Title
                            AutoResizeText(
                                text = albumWithSongs.album.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSizeRange = FontSizeRange(24.sp, 32.sp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Artists
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        ).toSpanStyle()
                                    ) {
                                        albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                            val link = LinkAnnotation.Clickable(artist.id) {
                                                navController.navigate("artist/${artist.id}")
                                            }
                                            withLink(link) {
                                                append(artist.name)
                                            }
                                            if (index != albumWithSongs.artists.lastIndex) {
                                                append(", ")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Year
                            if (albumWithSongs.album.year != null) {
                                Text(
                                    text = albumWithSongs.album.year.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            // Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Like Button
                                IconButton(
                                    onClick = {
                                        database.query {
                                            update(albumWithSongs.album.toggleLike())
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(24.dp)
                                        )
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
                                    )
                                }

                                // Download Button
                                when (downloadState) {
                                    Download.STATE_COMPLETED -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        song.id,
                                                        false,
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(24.dp)
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                            )
                                        }
                                    }

                                    Download.STATE_DOWNLOADING -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        song.id,
                                                        false,
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(24.dp)
                                                )
                                        ) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }

                                    else -> {
                                        IconButton(
                                            onClick = {
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
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(24.dp)
                                                )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                }

                                // Menu Button
                                IconButton(
                                    onClick = {
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
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(24.dp)
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Play/Pause Button
                                IconButton(
                                    onClick = {
                                        if (isCurrentAlbumPlaying) {
                                            // If current album is playing, toggle play/pause
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            // If different album or not playing, start playing this album
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs),
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isCurrentAlbumPlaying && isPlaying) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            RoundedCornerShape(24.dp)
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isCurrentAlbumPlaying && isPlaying) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = if (isCurrentAlbumPlaying && isPlaying) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            LocalContentColor.current
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Shuffle Button
                                IconButton(
                                    onClick = {
                                        if (isCurrentAlbumPlaying && shuffleModeEnabled) {
                                            // If current album is playing and shuffle is on, toggle shuffle off
                                            playerConnection.player.shuffleModeEnabled = false
                                        } else if (isCurrentAlbumPlaying) {
                                            // If current album is playing but shuffle is off, enable shuffle
                                            playerConnection.player.shuffleModeEnabled = true
                                        } else {
                                            // Start playing shuffled
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (shuffleModeEnabled) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            RoundedCornerShape(24.dp)
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        tint = if (shuffleModeEnabled) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            LocalContentColor.current
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (wrappedSongs != null) {
                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            albumIndex = index + 1,
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
                                )
                                .animateItem(),
                        )
                    }
                }

                if (otherVersions.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                        )
                    }
                    item {
                        LazyRow {
                            items(
                                items = otherVersions,
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
                item(key = "shimmer") {
                    ShimmerHost {
                        // Album Image Placeholder
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
                                    .fadingEdge(
                                        top = WindowInsets.systemBars
                                            .asPaddingValues()
                                            .calculateTopPadding() + AppBarHeight,
                                        bottom = 200.dp,
                                    ),
                            )
                        }

                        // Album Info Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Album Title Placeholder
                            TextPlaceholder(
                                height = 32.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(bottom = 8.dp)
                            )

                            // Artist Placeholder
                            TextPlaceholder(
                                height = 20.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .padding(bottom = 4.dp)
                            )

                            // Year Placeholder
                            TextPlaceholder(
                                height = 16.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .padding(bottom = 16.dp)
                            )

                            // Buttons Row Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                repeat(5) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(24.dp)
                                            )
                                    )
                                }
                            }
                        }

                        // Songs List Placeholder
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp),
            ) {
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
            }
        },
        actions = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp),
                ) {
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
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp),
                ) {
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
            }
        },
        colors = if (transparentAppBar && !selection) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}