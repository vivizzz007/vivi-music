package com.music.vivi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import com.music.vivi.applelyrics.TimeUtils
import kotlin.collections.isNotEmpty
// Add these imports to your existing imports at the top of the file

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.music.vivi.LocalSyncUtils


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    /* ------------------  existing declarations  ------------------ */
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

    val headerOffset = with(density) {
        -(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + AppBarHeight).roundToPx()
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

    /* ------------------  screen content  ------------------ */
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            if (albumWithSongs == null) {
                /* ---- shimmer skeleton ---- */
                item(key = "shimmer") {
                    ShimmerHost(
                        modifier = Modifier.offset { IntOffset(0, headerOffset) }
                    ) {
                        /* cover shimmer */
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 0.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        /* text placeholders */
                        Column(Modifier.padding(16.dp)) {
                            TextPlaceholder(height = 28.dp, modifier = Modifier.fillMaxWidth(0.8f))
                            Spacer(Modifier.height(8.dp))
                            TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.6f))
                        }

                        /* button placeholders */
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(4) {
                                Box(
                                    Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .shimmer()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }

                        /* list placeholders */
                        repeat(6) { ListItemPlaceHolder() }
                    }
                }
            } else {
                /* -------------------------------------------------
                   1.  album cover – large card
                   ------------------------------------------------- */
                item {
                    Spacer(Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        AsyncImage(
                            model = albumWithSongs!!.album.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                /* -------------------------------------------------
                   2.  album / artist text + big buttons (row)
                   ------------------------------------------------- */
                item {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        /* ---- text column ---- */
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = albumWithSongs!!.album.title,
                                style = MaterialTheme.typography.headlineSmall, // Changed from headlineLarge
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,                 // force single line
                                overflow = TextOverflow.Ellipsis
                            )
                            if (albumWithSongs!!.artists.size > 2) {
                                // Scrolling text for multiple artists with clickable functionality
                                Text(
                                    text = buildAnnotatedString {
                                        albumWithSongs!!.artists.fastForEachIndexed { idx, artist ->
                                            val link = LinkAnnotation.Clickable(artist.id) { navController.navigate("artist/${artist.id}") }
                                            withLink(link) {
                                                withStyle(SpanStyle(textDecoration = TextDecoration.None)) {
                                                    append(artist.name)
                                                }
                                            }
                                            if (idx != albumWithSongs!!.artists.lastIndex) append(", ")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.basicMarquee(),
                                    maxLines = 1
                                )
                            } else {
                                // Normal text for 1-2 artists with clickable links
                                Text(
                                    text = buildAnnotatedString {
                                        albumWithSongs!!.artists.fastForEachIndexed { idx, artist ->
                                            val link = LinkAnnotation.Clickable(artist.id) { navController.navigate("artist/${artist.id}") }
                                            withLink(link) {
                                                withStyle(SpanStyle(textDecoration = TextDecoration.None)) {
                                                    append(artist.name)
                                                }
                                            }
                                            if (idx != albumWithSongs!!.artists.lastIndex) append(", ")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            albumWithSongs!!.album.year?.let { year ->
                                Text(
                                    text = "$year • Album",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        /* ---- big buttons ---- */
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            /* play / pause */
                            IconButton(
                                onClick = {
                                    if (mediaMetadata?.album?.id == albumWithSongs?.album?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs!!))
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(16.dp)
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            /* shuffle */
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
                                    .size(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
//card
                if (filteredSongs.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface  // Same color for both light and dark mode
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Column {
                                /* header row - using the utility function */
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = TimeUtils.calculateTotalDuration(
                                                songs = filteredSongs,
                                                durationExtractor = { it.song.duration * 1000L }   // if duration is in seconds// This can be Int or Long
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        /* favourite toggle */
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable {
                                                    database.query {
                                                        albumWithSongs?.album?.let { album ->
                                                            update(album.toggleLike())
                                                        }
                                                    }
                                                },
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (albumWithSongs?.album?.bookmarkedAt != null) R.drawable.favorite
                                                        else R.drawable.favorite_border
                                                    ),
                                                    contentDescription = null,
                                                    tint = if (albumWithSongs?.album?.bookmarkedAt != null) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        /* Album-order chip – now opens the same album menu */
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable {
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = Album(
                                                                albumWithSongs!!.album,
                                                                albumWithSongs!!.artists
                                                            ),
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                },
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.drop_down),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }

                                /* Add divider above the first song */
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    thickness = 1.dp
                                )

                                val syncUtils = LocalSyncUtils.current

                                filteredSongs.forEachIndexed { idx, song ->
                                    SongListItem(
                                        song = song,
//                                        albumIndex = idx + 1,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        showInLibraryIcon = true,
                                        badges = {
                                            val downloadUtil = LocalDownloadUtil.current
                                            val download by downloadUtil.getDownload(song.id).collectAsState(initial = null)

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Check if song is liked (favorite), not inLibrary
                                                if (song.song.liked) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.library_add_check),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                // Download icon
                                                if (download?.state == Download.STATE_COMPLETED) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.offline),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Explicit indicator (stays in original position)
                                                if (song.song.explicit) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(end = 8.dp)
                                                            .size(18.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFFFD700)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "E",
                                                            color = Color.Black,
                                                            style = MaterialTheme.typography.labelSmall
                                                                .copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }

                                                // Favorite button (moved to +1 position - after explicit, before menu)
                                                IconButton(
                                                    onClick = {
                                                        val updatedSong = song.song.toggleLike()
                                                        database.query {
                                                            update(updatedSong)
                                                        }
                                                        syncUtils?.likeSong(updatedSong)
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(
                                                            if (song.song.liked) R.drawable.favorite
                                                            else R.drawable.favorite_border
                                                        ),
                                                        tint = if (song.song.liked) MaterialTheme.colorScheme.error
                                                        else MaterialTheme.colorScheme.onSurface,
                                                        contentDescription = stringResource(R.string.favourite),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Menu button (remains in last position)
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
                                                        contentDescription = stringResource(R.string.more_options)
                                                    )
                                                }
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
                                                            LocalAlbumRadio(albumWithSongs!!, startIndex = idx)
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                                Spacer(Modifier.height(16.dp).fillMaxWidth())
                            }
                        }
                    }
                }

                  /* -------------------------------------------------
                       4.  other versions – improved UI
                    ------------------------------------------------- */
                // Replace your existing "other versions" section with this improved version

                if (otherVersions.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.other_versions),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .animateContentSize()
                        )
                        Spacer(Modifier.height(15.dp))
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            flingBehavior = rememberSnapFlingBehavior(lazyListState = rememberLazyListState())
                        ) {
                            itemsIndexed(
                                items = otherVersions,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        // Stunning border effect
                                        .border(
                                            width = 1.5.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                ),
                                                start = Offset(0f, 0f),
                                                end = Offset(100f, 100f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        // Subtle shadow for depth
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                        .graphicsLayer {
                                            scaleX = 1f
                                            scaleY = 1f
                                        }
                                        .combinedClickable(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                navController.navigate("album/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                        .animateItem(
                                            fadeInSpec = tween(
                                                durationMillis = 300,
                                                delayMillis = index * 50
                                            ),
                                            fadeOutSpec = tween(durationMillis = 200),
                                            placementSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Album cover - Fixed size with border radius only on top
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                        ) {
                                            AsyncImage(
                                                model = item.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )

                                            // Gradient overlay at bottom of image
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(32.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.3f)
                                                            )
                                                        )
                                                    )
                                            )

                                            // Play indicator for active album
                                            if (mediaMetadata?.album?.id == item.id && isPlaying) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(48.dp)
                                                        .background(
                                                            Color.Black.copy(alpha = 0.7f),
                                                            CircleShape
                                                        )
                                                        .border(
                                                            width = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.pause),
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Album info - Takes remaining space with consistent layout
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp)
                                                .animateContentSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Main content with fade-in animation
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.animateContentSize(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            ) {
                                                Text(
                                                    text = item.title ?: "Unknown Album",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight
                                                )

                                                item.artists?.firstOrNull()?.name?.let { artistName ->
                                                    Text(
                                                        text = artistName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }

                                            // Bottom info - Always at bottom with stylish text
                                            item.year?.let { year ->
                                                Text(
                                                    text = "$year • Album",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            } ?: run {
                                                Text(
                                                    text = "Album",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(40.dp)) }
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

    /* ------------------  transparent app-bar  ------------------ */
    TopAppBar(
        title = { if (!transparentAppBar) Text(albumWithSongs?.album?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null
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