/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalListenTogetherManager
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ShowArtistDescriptionKey
import com.music.vivi.constants.ShowArtistSubscriberCountKey
import com.music.vivi.constants.ShowMonthlyListenersKey
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.AlbumGridItem
import com.music.vivi.ui.component.ExpandableText
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LinkSegment
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.YouTubeListItem
import com.music.vivi.ui.component.shimmer.ButtonPlaceholder
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.utils.isScrollingUp
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import com.music.vivi.artistvideo.ArtistVideo
import com.music.vivi.constants.ShowArtistVideoKey
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val artistVideoUrl by viewModel.artistVideoUrl.collectAsState()
    val artistVideoSong by viewModel.artistVideoSong.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)
    val showArtistVideo by rememberPreference(key = ShowArtistVideoKey, defaultValue = true)

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost (
                        modifier = Modifier
                            .offset {
                                IntOffset(x = 0, y = headerOffset)
                            }
                    ) {
                        // Artist Image Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .fadingEdge(
                                        top = systemBarsTopPadding + AppBarHeight,
                                        bottom = 200.dp,
                                    ),
                            )
                        }
                        // Artist Name and Controls Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Artist Name Placeholder
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .padding(bottom = 16.dp)
                            )

                            // Buttons Row Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subscribe Button Placeholder
                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(40.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Right side buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Radio Button Placeholder
                                    ButtonPlaceholder(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(40.dp)
                                    )

                                    // Shuffle Button Placeholder
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
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    Box {
                        // Artist Image with offset
                        if (thumbnail != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .offset {
                                        IntOffset(x = 0, y = headerOffset)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .fadingEdge(
                                            bottom = 200.dp,
                                        )
                                ) {
                                    AsyncImage(
                                        model = thumbnail.resize(1200, 1200),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }

                        // Artist Name and Controls Section - positioned at bottom of image

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (thumbnail != null) {
                                        // Position content at the bottom part of the image
                                        // Using screen width to calculate aspect ratio height minus overlap
                                        LocalResources.current.displayMetrics.widthPixels.let { screenWidth ->
                                            with(density) {
                                                ((screenWidth / 1.2f) - 144).toDp()
                                            }
                                        }
                                    } else {
                                        16.dp
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {

                                    //artist video
                                    if (showArtistVideo) {
                                        artistVideoUrl?.let { videoUrl ->
                                            artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                                Spacer(modifier = Modifier.width(5.dp))
                                                ArtistVideo(
                                                    videoUrl = videoUrl,
                                                    modifier = Modifier
                                                        .width(45.dp)
                                                        .height(45.dp),
                                                    onClick = {
                                                        val watchEndpoint = artistVideoSong?.endpoint
                                                            ?: artistPage?.artist?.radioEndpoint
                                                        watchEndpoint?.let {
                                                            playerConnection.playQueue(YouTubeQueue(it))
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(5.dp))

                                    // Artist Name
                                    Text(
                                        text = artistName ?: "Unknown",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 32.sp,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    if (showArtistSubscriberCount) {
                                        artistPage?.subscriberCountText?.let { subscribers ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.artist_screen),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${subscribers.split(' ').firstOrNull() ?: ""} ${stringResource(R.string.subscribers)}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (showMonthlyListeners) {
                                        artistPage?.monthlyListenerCount?.let { monthlyListeners ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.graphic_eq),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${monthlyListeners.split(' ').firstOrNull() ?: ""} ${stringResource(R.string.monthly_listeners)}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!showLocal && showArtistDescription && artistPage != null) {
                                    val description = artistPage?.description
                                    val descriptionRuns = artistPage?.descriptionRuns
                                    
                                    if (!description.isNullOrEmpty() || !descriptionRuns.isNullOrEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.about_artist),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            
                                            ExpandableText(
                                                text = description.orEmpty(),
                                                runs = descriptionRuns?.map {
                                                    LinkSegment(
                                                        text = it.text,
                                                        url = it.navigationEndpoint?.urlEndpoint?.url
                                                    )
                                                },
                                                collapsedMaxLines = 3
                                            )
                                        }
                                    }
                                }

                                // Buttons Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        ButtonGroupDefaults.ConnectedSpaceBetween
                                    )
                                ) {
                                    // Subscribe Button
                                    ToggleButton(
                                        checked = libraryArtist?.artist?.bookmarkedAt != null,
                                        onCheckedChange = {
                                            database.transaction {
                                                val artist = libraryArtist?.artist
                                                if (artist != null) {
                                                    update(artist.toggleLike())
                                                } else {
                                                    artistPage?.artist?.let {
                                                        insert(
                                                            ArtistEntity(
                                                                id = it.id,
                                                                name = it.title,
                                                                channelId = it.channelId,
                                                                thumbnailUrl = it.thumbnail,
                                                            ).toggleLike()
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (libraryArtist?.artist?.bookmarkedAt != null) {
                                                    R.drawable.subscribed
                                                } else {
                                                    R.drawable.subscribe
                                                }
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (libraryArtist?.artist?.bookmarkedAt != null) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                LocalContentColor.current
                                            }
                                        )
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text(
                                            text = stringResource(
                                                if (libraryArtist?.artist?.bookmarkedAt != null) {
                                                    R.string.subscribed
                                                } else {
                                                    R.string.subscribe
                                                }
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    // Radio Button
                                    if (!showLocal && !isGuest) {
                                        artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.radio),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    text = stringResource(R.string.radio),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }

                                    // Shuffle Button
                                    if (!showLocal && !isGuest) {
                                        artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                                },
                                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                                shapes = if (artistPage?.artist?.radioEndpoint != null) {
                                                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                } else {
                                                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shuffle),
                                                    contentDescription = stringResource(R.string.shuffle),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    text = stringResource(R.string.shuffle),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    } else if (librarySongs.isNotEmpty() && !isGuest) {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                val shuffledSongs = librarySongs.shuffled()
                                                if (shuffledSongs.isNotEmpty()) {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                            items = shuffledSongs.map { it.toMediaItem() }
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = stringResource(R.string.shuffle),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(
                                                text = stringResource(R.string.shuffle),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }


                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        val filteredLibrarySongs = if (hideExplicit) {
                            librarySongs.filter { !it.song.explicit }
                        } else {
                            librarySongs
                        }
                        itemsIndexed(
                            items = filteredLibrarySongs,
                            key = { index, item -> "local_song_${item.id}_$index" }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                shape = listItemShape(index, filteredLibrarySongs.size),
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
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                        items = librarySongs.map { it.toMediaItem() },
                                                        startIndex = index
                                                    )
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

                    if (libraryAlbums.isNotEmpty()) {
                        item(key = "local_albums_title") {
                            NavigationTitle(
                                title = stringResource(R.string.albums),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/albums")
                                }
                            )
                        }

                        item(key = "local_albums_list") {
                            val filteredLibraryAlbums = if (hideExplicit) {
                                libraryAlbums.filter { !it.album.explicit }
                            } else {
                                libraryAlbums
                            }
                            LazyRow(
                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                            ) {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" }
                                ) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = album,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${section.title}") {
                                NavigationTitle(
                                    title = section.title,
                                    modifier = Modifier.animateItem(),
                                    onClick = section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            itemsIndexed(
                                items = section.items.distinctBy { it.id },
                                key = { _, it -> "youtube_song_${it.id}" },
                            ) { index, song ->
                                YouTubeListItem(
                                    item = song as SongItem,
                                    isActive = mediaMetadata?.id == song.id,
                                    isPlaying = isPlaying,
                                    shape = listItemShape(index, section.items.distinctBy { it.id }.size),
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song.id),
                                                            song.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        } else {
                            item(key = "section_list_${section.title}") {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { "youtube_album_${it.id}" },
                                    ) { item ->
                                        YouTubeGridItem(
                                            item = item,
                                            isActive = when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            thumbnailRatio = 1f, // Use square thumbnails for all items in horizontal scroll
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    ),
                                                                )

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            when (item) {
                                                                is SongItem ->
                                                                    YouTubeSongMenu(
                                                                        song = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is AlbumItem ->
                                                                    YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is ArtistItem ->
                                                                    YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is PlaylistItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                            }
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
            }
        }

        val isScrollingUp = lazyListState.isScrollingUp()
        val showLocalFab = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true
        
        // Library/Local Toggle FAB
        HideOnScrollFAB(
            visible = showLocalFab,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            }
        )
        
        // Play All FAB (Stacked above Library/Local FAB if visible)
        val canPlayAll = !isGuest && (
            (showLocal && librarySongs.isNotEmpty()) || 
            (!showLocal && artistPage?.sections?.any { 
                (it.items.firstOrNull() as? SongItem)?.album != null 
            } == true)
        )

        if (canPlayAll) {
             androidx.compose.animation.AnimatedVisibility(
                visible = isScrollingUp,
                enter = androidx.compose.animation.slideInVertically { it * 2 },
                exit = androidx.compose.animation.slideOutVertically { it * 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    // Add padding to position it above the other FAB (56dp height + 16dp padding + 8dp spacing)
                    // If the other FAB is visible.
                    .padding(bottom = if (showLocalFab) 64.dp else 0.dp)
            ) {
                val onPlayAllClick: () -> Unit = {
                     if (showLocal) {
                         if (librarySongs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                    items = librarySongs.map { it.toMediaItem() }
                                )
                            )
                        }
                    } else if (artistPage != null) {
                        val songSection = artistPage.sections.find { section ->
                            (section.items.firstOrNull() as? SongItem)?.album != null
                        }
                        
                        val moreEndpoint = songSection?.moreEndpoint
                        if (moreEndpoint != null) {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val result = YouTube.artistItems(moreEndpoint).getOrNull()
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (result != null && result.items.isNotEmpty()) {
                                        val songs = result.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = artistPage.artist.title,
                                                items = songs
                                            )
                                        )
                                    } else {
                                        // Fallback to loaded items
                                        val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = artistPage.artist.title,
                                                    items = songs
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            } else if (songSection != null) {
                            // Use loaded items if no more endpoint
                            val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artistPage.artist.title,
                                    items = songs
                                )
                            )
                        } else {
                            // Fallback to shuffle endpoint (stripped) if no song section found
                            val shuffleEndpoint = artistPage.artist.shuffleEndpoint
                            if (shuffleEndpoint != null) {
                                val endpoint = if (shuffleEndpoint.playlistId != null) {
                                    WatchEndpoint(
                                        playlistId = shuffleEndpoint.playlistId,
                                        params = null, // Remove shuffle params to play in order
                                        videoId = null // Ensure videoId is null to start from beginning of playlist
                                    )
                                } else {
                                    shuffleEndpoint
                                }
                                playerConnection.playQueue(YouTubeQueue(endpoint))
                            }
                        }
                    }
                }

                if (showLocalFab) {
                     androidx.compose.material3.SmallFloatingActionButton(
                        modifier = Modifier.padding(16.dp).offset(x = (-4).dp), // Align center with standard FAB (56dp vs 48dp)
                        onClick = onPlayAllClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                        )
                    }
                } else {
                    androidx.compose.material3.FloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = onPlayAllClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                            modifier = Modifier.size(32.dp)
                        )
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
        title = { if (!transparentAppBar) Text(artistPage?.artist?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}
