package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.music.vivi.utils.rememberPreference
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

import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.utils.rememberPreference
import com.music.vivi.ui.utils.resize
import com.music.vivi.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable

fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current

    // Calculate the offset value for header
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    val filteredSongs = remember(albumWithSongs, hideExplicit) {
        if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (albumWithSongs == null) {
                item(key = "shimmer") {
                    ShimmerHost(
                        modifier = Modifier
                            .offset { IntOffset(x = 0, y = headerOffset) }
                    ) {
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
                            )
                        }
                        // Album Info Section
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Album Title Placeholder
                            TextPlaceholder(
                                height = 28.dp,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )

                            // Artist Name Placeholder
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
            } else {
                item(key = "header") {
                    Box {
                        // Album Image with offset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .offset { IntOffset(x = 0, y = headerOffset) }
                        ) {
                            AsyncImage(
                                model = albumWithSongs!!.album.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .fadingEdge(bottom = 200.dp)
                            )
                        }

                        // Album Info Section - positioned at bottom of image
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
                            // Album Title
                            Text(
                                text = albumWithSongs!!.album.title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 28.sp
                            )
                            // Artist Name(s) - Same font and color as album title
                            Text(
                                buildAnnotatedString {
                                    withStyle(
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
//                                            color = MaterialTheme.colorScheme.onSurface
                                        ).toSpanStyle()
                                    ) {
                                        albumWithSongs!!.artists.fastForEachIndexed { index, artist ->
                                            val link = LinkAnnotation.Clickable(artist.id) {
                                                navController.navigate("artist/${artist.id}")
                                            }
                                            withLink(link) {
                                                append(artist.name)
                                            }
                                            if (index != albumWithSongs!!.artists.lastIndex) {
                                                append(", ")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            // Album Year if available
                            albumWithSongs!!.album.year?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            // Action Buttons Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Play Button
                                // Play/Pause Button
                                IconButton(
                                    onClick = {
                                        if (mediaMetadata?.album?.id == albumWithSongs?.album?.id && isPlaying) {
                                            // If current album is playing, pause it
                                            playerConnection.player.togglePlayPause()
                                        } else if (mediaMetadata?.album?.id == albumWithSongs?.album?.id && !isPlaying) {
                                            // If current album is paused, resume it
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            // If different album or no album playing, start this album
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs!!)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (mediaMetadata?.album?.id == albumWithSongs?.album?.id && isPlaying) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            }
                                        ),
                                        contentDescription = if (mediaMetadata?.album?.id == albumWithSongs?.album?.id && isPlaying) {
                                            stringResource(R.string.pause)
                                        } else {
                                            stringResource(R.string.play)
                                        },
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                // Shuffle Button
                                IconButton(
                                    onClick = {
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(
                                                albumWithSongs!!.copy(
                                                    songs = albumWithSongs!!.songs.shuffled()
                                                )
                                            )
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
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = stringResource(R.string.shuffle),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                // Like Button
                                IconButton(
                                    onClick = {
                                        database.query {
                                            albumWithSongs?.album?.let { album ->
                                                update(album.toggleLike())
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
                                            if (albumWithSongs?.album?.bookmarkedAt != null) {
                                                R.drawable.favorite
                                            } else {
                                                R.drawable.favorite_border
                                            }
                                        ),
                                        contentDescription = stringResource(R.string.favourite),
                                        tint = if (albumWithSongs?.album?.bookmarkedAt != null) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                // More Options Button
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            AlbumMenu(
                                                originalAlbum = Album(
                                                    albumWithSongs!!.album,
                                                    albumWithSongs!!.artists
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

                // Songs List
                if (filteredSongs.isNotEmpty()) {
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SongListItem(
                            song = song,
                            albumIndex = index + 1,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
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
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs!!, startIndex = index)
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
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

                // Other Versions
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
                                    coroutineScope = coroutineScope,
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
        title = { if (!transparentAppBar) Text(albumWithSongs?.album?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            // Removed the share icon button as requested
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}