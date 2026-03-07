/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.music.vivi.constants.AppBarHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.HideExplicitKey
import com.music.vivi.constants.HideVideoSongsKey
import com.music.vivi.db.entities.Album
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.component.AlbumGradient
import com.music.vivi.ui.component.ExpandableText
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.LinkSegment
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.YouTubeGridItem
import com.music.vivi.ui.menu.AlbumMenu
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.menu.SongMenu
import com.music.vivi.ui.menu.YouTubeAlbumMenu
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.listItemShape
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val releasesForYou by viewModel.releasesForYou.collectAsState()
    val description by viewModel.description.collectAsState()
    val descriptionRuns by viewModel.descriptionRuns.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(key = HideVideoSongsKey, defaultValue = false)

    val filteredSongs = remember(albumWithSongs, hideExplicit, hideVideoSongs) {
        var songs = albumWithSongs?.songs ?: emptyList()
        if (hideExplicit) {
            songs = songs.filter { !it.song.explicit }
        }
        if (hideVideoSongs) {
            songs = songs.filter { !it.song.isVideo }
        }
        songs
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
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
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

    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs?.album?.explicit == true
    }

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item(key = "album_header") {
                val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                val headerOffset = with(LocalDensity.current) {
                    -(systemBarsTopPadding + AppBarHeight).roundToPx()
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Gradient Background
                  AlbumGradient(
                        thumbnailUrl = albumWithSongs.album.thumbnailUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .offset { IntOffset(x = 0, y = headerOffset) }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(20.dp)) // Space for top app bar

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

                    // Metadata & Actions Section - Left Aligned
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (albumWithSongs.artists.size == 1) {
                            val artist = albumWithSongs.artists.first()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = albumWithSongs.album.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("artist/${artist.id}")
                                            }
                                        )
                                ) {
                                    AsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            // Album Title with Logo Icon for multiple artists
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.album),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = albumWithSongs.album.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }

                    Spacer(Modifier.height(24.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Save/Like Button
                        androidx.compose.material3.Button(
                            onClick = {
                                database.query {
                                    update(albumWithSongs.album.toggleLike())
                                }
                            },
                            shape = androidx.compose.material3.ButtonDefaults.shape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                vertical = 12.dp
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
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
                                    text = if (albumWithSongs.album.bookmarkedAt != null) stringResource(R.string.saved) else stringResource(R.string.save),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Play Button
                        androidx.compose.material3.Button(
                            onClick = {
                                if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                    playerConnection.player.pause()
                                } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                    playerConnection.player.play()
                                } else {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs)
                                    )
                                }
                            },
                            shape = androidx.compose.material3.ButtonDefaults.shape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                vertical = 12.dp
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
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
                                        stringResource(R.string.pause) else stringResource(R.string.play),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Share Button
                        Surface(
                            onClick = {
                                val intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/playlist?list=${albumWithSongs.album.playlistId}"
                                    )
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(
                                        intent,
                                        null
                                    )
                                )
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

                        Spacer(Modifier.height(16.dp))

                        // Explicit Label
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

                        // Album Info
                        Text(
                            text = buildString {
                                append(stringResource(R.string.album_text))
                                if (albumWithSongs.album.year != null) {
                                    append(" • ${albumWithSongs.album.year}")
                                }
                                append(" • ${albumWithSongs.songs.size} Tracks")
                                val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                                val hours = totalDuration / 3600
                                val minutes = (totalDuration % 3600) / 60
                                if (hours > 0) {
                                    append(" • ${hours}h ${minutes}m")
                                } else {
                                    append(" • ${minutes}m")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    val staticDescription = remember(albumWithSongs) {
                        "${albumWithSongs.album.title} is an album by ${albumWithSongs.artists.joinToString { it.name }}${
                            if (albumWithSongs.album.year != null) ", released in ${albumWithSongs.album.year}" else ""
                        }. This collection features ${albumWithSongs.songs.size} tracks showcasing their musical artistry."
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_album),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ExpandableText(
                            text = description ?: staticDescription,
                            runs = descriptionRuns?.map {
                                LinkSegment(
                                    text = it.text,
                                    url = it.navigationEndpoint?.urlEndpoint?.url
                                )
                            },
                            collapsedMaxLines = 3
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (albumWithSongs.artists.size > 1) {
                        // Artist Names (clickable) - only for multiple artists
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.by_text))
                                append(" ")
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            textAlign = TextAlign.Start
                        )

                        Spacer(Modifier.height(24.dp))
                    }

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
                                        albumWithSongs!!.songs.forEach { song ->
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                song.id,
                                                false,
                                            )
                                        }
                                    }

                                    else -> {
                                        albumWithSongs!!.songs.forEach { song ->
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
                                        contentDescription = stringResource(R.string.save_album),
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
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs!!.copy(songs = albumWithSongs.songs.shuffled())),
                                )
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
                                    AlbumMenu(
                                        originalAlbum = Album(
                                            albumWithSongs!!.album,
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

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            if (filteredSongs.isNotEmpty()) {
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.id },
                ) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    SongListItem(
                        song = song,
//                        albumIndex = index + 1,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        shape = listItemShape(index, filteredSongs.size),
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = song.id in selection,
                                    onCheckedChange = onCheckedChange
                                )
                            } else {
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
                            }
                        },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .combinedClickable(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(song.id !in selection)
                                    } else if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(albumWithSongs, startIndex = index),
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!inSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inSelectMode = true
                                        onCheckedChange(true)
                                    }
                                },
                            ),
                    )
                }
            }

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
                                modifier =
                                Modifier
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

            if (releasesForYou.isNotEmpty()) {
                item(key = "releases_for_you_title") {
                    NavigationTitle(
                        title = stringResource(R.string.releases_for_you),
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "releases_for_you_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = releasesForYou.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
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

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(50.dp))
            }
        } else {
            item(key = "loading") {
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
    }

    TopAppBar(
        title = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (inSelectMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
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
                            selection.addAll(filteredSongs.map { it.id })
                        }
                    }
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = selection.mapNotNull { songId ->
                                    filteredSongs.find { it.id == songId }
                                },
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
            }
        },
        colors = if (transparentAppBar) {
            androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
        }
    )
}
