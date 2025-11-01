package com.music.vivi.ui.menu


import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.media3.exoplayer.offline.Download.STATE_STOPPED
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.update.mordernlistmenu.ModernListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun AlbumMenu(
    originalAlbum: Album,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val libraryAlbum by database.album(originalAlbum.id).collectAsState(initial = originalAlbum)
    val album = libraryAlbum ?: originalAlbum
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    val coroutineScope = rememberCoroutineScope()
    val bottomSheetPageState = LocalBottomSheetPageState.current

    LaunchedEffect(Unit) {
        database.albumSongs(album.id).collect {
            songs = it
        }
    }

    var downloadState by remember {
        mutableStateOf(STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                    STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == STATE_QUEUED ||
                                downloads[it.id]?.state == STATE_DOWNLOADING ||
                                downloads[it.id]?.state == STATE_COMPLETED
                    }
                ) {
                    STATE_DOWNLOADING
                } else {
                    STATE_STOPPED
                }
        }
    }

    // Play/Pause state tracking
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val isCurrentAlbumPlaying = remember(mediaMetadata, songs) {
        songs.any { it.id == mediaMetadata?.id }
    }

    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    // Design variables
    val evenCornerRadiusElems = 26.dp
    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )
    val playButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadiusElems, smoothnessAsPercentBR = 60, cornerRadiusBR = evenCornerRadiusElems,
        smoothnessAsPercentTL = 60, cornerRadiusTL = evenCornerRadiusElems, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadiusElems, smoothnessAsPercentTR = 60
    )

    // Favorite state tracking
    val isFavorite = album.album.bookmarkedAt != null

    val favoriteButtonCornerRadius by animateDpAsState(
        targetValue = if (isFavorite) evenCornerRadiusElems else 60.dp,
        animationSpec = tween(durationMillis = 300), label = "FavoriteCornerAnimation"
    )
    val favoriteButtonContainerColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "FavoriteContainerColorAnimation"
    )
    val favoriteButtonContentColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "FavoriteContentColorAnimation"
    )

    val favoriteButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = favoriteButtonCornerRadius,
        smoothnessAsPercentBR = 60,
        cornerRadiusBR = favoriteButtonCornerRadius,
        smoothnessAsPercentTL = 60,
        cornerRadiusTL = favoriteButtonCornerRadius,
        smoothnessAsPercentBL = 60,
        cornerRadiusBL = favoriteButtonCornerRadius,
        smoothnessAsPercentTR = 60
    )

    // Main Content
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Album Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = album.album.thumbnailUrl,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(80.dp)
                    .clip(albumArtShape),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = album.album.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Header favorite button
            FilledTonalIconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    database.query {
                        update(album.album.toggleLike())
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(
                        if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play Button
            MediumExtendedFloatingActionButton(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                onClick = {
                    if (isCurrentAlbumPlaying && isPlaying) {
                        playerConnection.player.pause()
                    } else if (isCurrentAlbumPlaying && !isPlaying) {
                        playerConnection.player.play()
                    } else {
                        onDismiss()
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = album.album.title,
                                    items = songs.map(Song::toMediaItem)
                                )
                            )
                        }
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(
                            if (isCurrentAlbumPlaying && isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (isCurrentAlbumPlaying && isPlaying) "Pause" else "Play"
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = if (isCurrentAlbumPlaying && isPlaying) "Pause" else "Play",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            )

            // Favorite Button
            FilledIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    database.query {
                        update(album.album.toggleLike())
                    }
                },
                shape = favoriteButtonShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = favoriteButtonContainerColor,
                    contentColor = favoriteButtonContentColor
                )
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(
                        if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Share Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    onDismiss()
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${album.album.playlistId}")
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                shape = CircleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.share),
                    contentDescription = "Share album"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Download Button
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            shape = CircleShape,
            onClick = {
                when (downloadState) {
                    STATE_COMPLETED -> {
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    }
                    STATE_QUEUED, STATE_DOWNLOADING -> {
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    }
                    else -> {
                        songs.forEach { song ->
                            val downloadRequest = DownloadRequest
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
            Icon(
                painter = painterResource(
                    when (downloadState) {
                        STATE_COMPLETED -> R.drawable.offline
                        STATE_QUEUED, STATE_DOWNLOADING -> R.drawable.download
                        else -> R.drawable.download
                    }
                ),
                contentDescription = "Download album"
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (downloadState) {
                    STATE_COMPLETED -> "Remove Offline"
                    STATE_QUEUED, STATE_DOWNLOADING -> "Downloading..."
                    else -> "Download Album"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Details Section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Artist
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    if (album.artists.size == 1) {
                        navController.navigate("artist/${album.artists[0].id}")
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.artist),
                    contentDescription = "Artist icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        album.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Songs Count
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = { }
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = "Songs icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Songs",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${songs.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play Next
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    onDismiss()
                    playerConnection.playNext(songs.map { it.toMediaItem() })
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_play),
                    contentDescription = "Play next icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Play Next",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Play after current",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Add to Queue
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Add to queue icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Add to Queue",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Add to queue end",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Add to Playlist
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    showChoosePlaylistDialog = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = "Add to playlist icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Add to Playlist",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Add to existing playlist",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Refresh Metadata
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    refetchIconDegree -= 360
                    scope.launch(Dispatchers.IO) {
                        YouTube.album(album.id).onSuccess {
                            database.transaction {
                                update(album.album, it, album.artists)
                            }
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = "Refresh icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Refresh Metadata",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Update album information",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = rotationAnimation),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Dialogs
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    album.album.playlistId?.let { addPlaylistId ->
                        YouTube.addPlaylistToPlaylist(playlistId, addPlaylistId)
                    }
                }
            }
            songs.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    if (showErrorPlaylistAddDialog) {
        AlertDialog(
            onDismissRequest = { showErrorPlaylistAddDialog = false },
            title = { Text(stringResource(R.string.already_in_playlist)) },
            text = {
                LazyColumn {
                    items(notAddedList) { song ->
                        ModernListItem(
                            icon = R.drawable.music_note,
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            onClick = { }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showErrorPlaylistAddDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(items = album.artists.distinctBy { it.id }, key = { it.id }) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    AsyncImage(
                        model = artist.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}