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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.AppBarHeight
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ShowMonthlyListenersKey
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.HideOnScrollFAB
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.media.albums.AlbumGridItem
import com.music.vivi.ui.component.media.songs.roundedSongItems
import com.music.vivi.ui.component.media.youtube.YouTubeGridItem
import com.music.vivi.ui.component.media.youtube.YouTubeListItem
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.menu.YouTubeArtistMenu
import com.music.vivi.ui.menu.YouTubePlaylistMenu
import com.music.vivi.ui.menu.YouTubeSongMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.ui.utils.fadingEdge
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.ArtistViewModel
/**
 * The Main Artist Page Screen.
 *
 * Features:
 * - Parallax Header with Artist Image.
 * - Subscribe/Exclude buttons.
 * - "Radio" and "Shuffle" controls.
 * - Sections for Songs, Albums, Videos, and "Fans also like".
 * - Supports both Local (Library) and Remote (Online) artist views.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
public fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    artistId: String? = null,
    viewModel: ArtistViewModel = hiltViewModel(),
    onBack: () -> Unit = { navController.navigateUp() },
) {
    if (artistId != null) {
        LaunchedEffect(artistId) {
            viewModel.setArtistId(artistId)
        }
    }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)

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
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
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
                                AsyncImage(
                                    model = thumbnail.resize(1200, 1200),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .fadingEdge(
                                            bottom = 200.dp
                                        )
                                )
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
                                // Artist Name
                                Text(
                                    text = artistName ?: "Unknown",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

//                                Subscriber count badge
//                                changes from youtube.kt
//                                subscriptionbutton

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    artistPage?.artist?.subscriberCountText?.let { subscribers ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.person),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = subscribers,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Medium
                                            )
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
                                                    text = monthlyListeners,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.about_artist),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

// Replace the description section with this fixed version :

                                var showDescriptionDialog by rememberSaveable { mutableStateOf(false) }
                                var isDescriptionTruncated by remember { mutableStateOf(false) }
                                val description = artistPage?.description ?: run {
                                    buildString {
                                        append(artistName ?: "Unknown")
                                        append(" is a music artist.")
                                    }
                                }

// Build the display text - only show full text, let maxLines handle truncation
                                val displayText = description

                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .padding(bottom = 16.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (isDescriptionTruncated) {
                                                    showDescriptionDialog = true
                                                }
                                            }
                                        ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { textLayoutResult ->
                                        // Check if text is actually truncated by the maxLines constraint
                                        isDescriptionTruncated = textLayoutResult.hasVisualOverflow
                                    }
                                )

                                if (showDescriptionDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDescriptionDialog = false },
                                        title = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (thumbnail != null) {
                                                    AsyncImage(
                                                        model = thumbnail,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(R.drawable.person),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                                            .padding(8.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                                Spacer(Modifier.width(16.dp))
                                                Text(
                                                    text =
                                                    artistPage?.artist?.title
                                                        ?: stringResource(R.string.artist_info_fallback),
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                            }
                                        },
                                        text = {
                                            LazyColumn {
                                                item {
                                                    Text(
                                                        text = description,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = { showDescriptionDialog = false },
                                                shapes = ButtonDefaults.shapes()
                                            ) {
                                                Text(stringResource(android.R.string.ok))
                                            }
                                        }
                                    )
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
                                                                thumbnailUrl = it.thumbnail
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
                                                    R.string.subed
                                                } else {
                                                    R.string.sub
                                                }
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    // Radio Button
                                    if (!showLocal) {
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
                                    if (!showLocal) {
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
                                                    contentDescription = stringResource(R.string.shuffle_content_desc),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    text = stringResource(R.string.shuffle),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    } else if (librarySongs.isNotEmpty()) {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                val shuffledSongs = librarySongs.shuffled()
                                                if (shuffledSongs.isNotEmpty()) {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title =
                                                            libraryArtist?.artist?.name
                                                                ?: context.getString(R.string.unknown_artist),
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
                                                contentDescription = stringResource(R.string.shuffle_content_desc),
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
                    // Replace the local songs itemsIndexed section with this:

                    if (librarySongs.isNotEmpty()) {
                        val filteredLibrarySongs = if (hideExplicit) {
                            librarySongs.filter { !it.song.explicit }
                        } else {
                            librarySongs
                        }
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        if (filteredLibrarySongs.isNotEmpty()) {
                            roundedSongItems(
                                items = filteredLibrarySongs,
                                key = { "local_song_${it.id}" },
                                isActive = { it.id == mediaMetadata?.id },
                                onItemClick = { song ->
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title =
                                                libraryArtist?.artist?.name
                                                    ?: context.getString(R.string.unknown_artist),
                                                items = librarySongs.map { it.toMediaItem() },
                                                startIndex = librarySongs.indexOf(song)
                                            )
                                        )
                                    }
                                },
                                onItemLongClick = { song ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            ) { song, shape, modifier ->
                                LibrarySongListItem(
                                    song = song,
                                    showInLibraryIcon = true,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
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
                                    modifier = modifier
                                )
                            }
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
                                contentPadding = WindowInsets.systemBars.only(
                                    WindowInsetsSides.Horizontal
                                ).asPaddingValues()
                            ) {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" }
                                ) { album ->
                                    AlbumGridItem(
                                        album = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        onPlayClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = album.album.title,
                                                    items = album.songs.map { it.toMediaItem() },
                                                    startIndex = 0
                                                )
                                            )
                                        },
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
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        // /online song playing have a new design
                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            item(key = "youtube_songs_container_${section.title}") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    section.items.distinctBy { it.id }.forEachIndexed { index, item ->
                                        val song = item as SongItem
                                        val isFirst = index == 0
                                        val isLast = index == section.items.distinctBy { it.id }.size - 1
                                        val isActive = mediaMetadata?.id == song.id

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(ListItemHeight)
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = if (isFirst) 20.dp else 0.dp,
                                                        topEnd = if (isFirst) 20.dp else 0.dp,
                                                        bottomStart = if (isLast) 20.dp else 0.dp,
                                                        bottomEnd = if (isLast) 20.dp else 0.dp
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
                                            YouTubeListItem(
                                                item = song,
                                                isActive = isActive,
                                                isPlaying = isPlaying,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                YouTubeSongMenu(
                                                                    song = song,
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
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (song.id == mediaMetadata?.id) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = song.id),
                                                                        song.toMediaMetadata()
                                                                    )
                                                                )
                                                            }
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                YouTubeSongMenu(
                                                                    song = song,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss
                                                                )
                                                            }
                                                        }
                                                    )
                                            )
                                        }

                                        // Add 3dp spacer between items (except after last)
                                        if (!isLast) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            item(key = "section_list_${section.title}") {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars.only(
                                        WindowInsetsSides.Horizontal
                                    ).asPaddingValues()
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { "youtube_album_${it.id}" }
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
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    )
                                                                )

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                            is PlaylistItem -> navController.navigate(
                                                                "online_playlist/${item.id}"
                                                            )
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
                                                                        onDismiss = menuState::dismiss
                                                                    )

                                                                is AlbumItem ->
                                                                    YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss
                                                                    )

                                                                is ArtistItem ->
                                                                    YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss
                                                                    )

                                                                is PlaylistItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss
                                                                    )
                                                            }
                                                        }
                                                    }
                                                )
                                                .animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )

        TopAppBar(
            title = { if (!transparentAppBar) Text(artistPage?.artist?.title.orEmpty()) },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.artistPage?.artist?.shareLink?.let { link ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(context.getString(R.string.artist_link), link)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.link),
                        contentDescription = null
                    )
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
