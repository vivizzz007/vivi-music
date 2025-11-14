package com.music.vivi.ui.screens


import android.content.Intent
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
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
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.Album
import com.music.vivi.extensions.togglePlayPause
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.component.shimmer.ListItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    var shapeIndex by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

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

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    // Check if any song is explicit
    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs?.songs?.any { it.song.explicit } == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item(key = "album_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(50.dp)) // Space for top app bar

                        // Album Artwork - Large and centered
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp)
                        ) {
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        // Album Title with Logo Icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.new_album_vivi), // Use your app icon
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = albumWithSongs.album.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Save/Like Button
                            Surface(
                                onClick = {
                                    database.query {
                                        update(albumWithSongs.album.toggleLike())
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
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
                                        modifier = Modifier.size(20.dp),
                                        tint = if (albumWithSongs.album.bookmarkedAt != null) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (albumWithSongs.album.bookmarkedAt != null) "Saved" else "Save",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Play Button
                            Surface(
                                onClick = {
                                    if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.pause()
                                    } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.play()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(albumWithSongs),
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
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
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                            "Pause" else "Play",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            // Share Button
                            Surface(
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out ${albumWithSongs.album.title} by ${albumWithSongs.artists.joinToString { it.name }} on YouTube Music: https://music.youtube.com/playlist?list=${albumWithSongs.album.playlistId}")
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
                                        contentDescription = "Share album",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Explicit Label
                        if (hasExplicitContent) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    text = "E  Explicit",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Album Info
                        Text(
                            text = buildString {
                                append("Album")
                                if (albumWithSongs.album.year != null) {
                                    append(" • ${albumWithSongs.album.year}")
                                }
                                append(" • ${albumWithSongs.songs.size} Tracks")
                                val totalDuration = albumWithSongs.songs.sumOf { it.song.duration ?: 0 }
                                val hours = totalDuration / 3600
                                val minutes = (totalDuration % 3600) / 60
                                if (hours > 0) {
                                    append(" • ${hours}h ${minutes}m")
                                } else {
                                    append(" • ${minutes}m")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        // Album Description (placeholder - you can add this to your database)
                        Text(
                            text = "${albumWithSongs.album.title} is an album by ${albumWithSongs.artists.joinToString { it.name }}${
                                if (albumWithSongs.album.year != null) ", released in ${albumWithSongs.album.year}" else ""
                            }. This collection features ${albumWithSongs.songs.size} tracks showcasing their musical artistry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        // Artist Names (clickable)
                        Text(
                            text = buildAnnotatedString {
                                append("By ")
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
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))

                        // Additional action buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {

                            ToggleButton(
                                checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                                onCheckedChange = {
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
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            ) {
                                when (downloadState) {
                                    Download.STATE_COMPLETED -> {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = "saved",
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
                                            contentDescription = "save",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = when (downloadState) {
                                        Download.STATE_COMPLETED -> "saved"
                                        Download.STATE_DOWNLOADING -> "saving"
                                        else -> "save"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            // Shuffle Button
                            ToggleButton(
                                checked = false,
                                onCheckedChange = {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                                    )
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = "Shuffle",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text("Shuffle", style = MaterialTheme.typography.labelMedium)
                            }

                            // More Options Button
                            ToggleButton(
                                checked = false,
                                onCheckedChange = {
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
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = "More options",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text("More", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Replace the songs list section in your AlbumScreen with this modified version:

// Songs List with Quick Pick style
                if (!wrappedSongs.isNullOrEmpty()) {
                    item(key = "songs_container") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            wrappedSongs.forEachIndexed { index, songWrapper ->
                                val isFirst = index == 0
                                val isLast = index == wrappedSongs.size - 1
                                val isActive = songWrapper.item.id == mediaMetadata?.id
                                val isSingleSong = wrappedSongs.size == 1

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = if (isFirst) 20.dp else 0.dp,
                                                topEnd = if (isFirst) 20.dp else 0.dp,
                                                bottomStart = if (isLast && !isSingleSong) 20.dp else 0.dp,
                                                bottomEnd = if (isLast && !isSingleSong) 20.dp else 0.dp
                                            )
                                        )
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainer
                                        )
                                ) {
                                    SongListItem(
                                        song = songWrapper.item,
                                        isActive = isActive,
                                        isPlaying = isPlaying,
                                        showInLibraryIcon = true,
                                        isSwipeable = false,
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
                                            .fillMaxSize()
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

                                // Add 3dp spacer between items (except after last)
                                if (!isLast) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Other Versions Section
                if (otherVersions.isNotEmpty()) {
                    item(key = "other_versions_title") {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "other_versions_list") {
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
                // Loading shimmer (keeping your original)
                item(key = "loading_shimmer") {
                    ShimmerHost {
                        Column(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(60.dp))

                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface),
                            )

                            Spacer(Modifier.height(24.dp))
                            TextPlaceholder(Modifier.width(200.dp))
                            Spacer(Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                repeat(2) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            repeat(6) {
                                ListItemPlaceHolder()
                            }
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
                } else {
//                    // Search button in normal mode
//                    IconButton(
//                        onClick = {
//                            // Add search functionality
//                        },
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.search),
//                            contentDescription = null
//                        )
//                    }
                }
            },
            colors = if (transparentAppBar && !selection) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior
        )
    }
}
