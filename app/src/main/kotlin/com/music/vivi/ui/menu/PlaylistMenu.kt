package com.music.vivi.ui.menu

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.PlaylistListItem
import com.music.vivi.ui.component.TextFieldDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlist(playlist.id).collectAsState(initial = playlist)

    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable == true

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    // State management
    var showEditDialog by remember { mutableStateOf(false) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }

    // Design variables
    val evenCornerRadiusElems = 26.dp
    val playlistArtShape = AbsoluteSmoothCornerShape(
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
    val isFavorite = dbPlaylist?.playlist?.bookmarkedAt != null

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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // PlaylistListItem Header
        PlaylistListItem(
            playlist = playlist,
            trailingContent = {
                if (playlist.playlist.isEditable != true) {
                    IconButton(
                        onClick = {
                            database.query {
                                dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                            tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            contentDescription = null
                        )
                    }
                }
            },
        )

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
                    onDismiss()
                    if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = playlist.playlist.name,
                                items = songs.map(Song::toMediaItem)
                            )
                        )
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = "Play"
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = "Play",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            )

            // Favorite Button (only for non-editable playlists)
            if (!editable) {
                FilledIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        database.query {
                            dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
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
            } else {
                // Shuffle Button (for editable playlists)
                FilledTonalIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        onDismiss()
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = playlist.playlist.name,
                                    items = songs.shuffled().map(Song::toMediaItem)
                                )
                            )
                        }
                    },
                    shape = CircleShape
                ) {
                    Icon(
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle"
                    )
                }
            }

            // Share Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    onDismiss()
                    val shareLink = playlist.playlist.shareLink
                        ?: "https://music.youtube.com/playlist?list=${dbPlaylist?.playlist?.browseId}"
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                shape = CircleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.share),
                    contentDescription = "Share playlist"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Download Button
        if (downloadPlaylist != true) {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = when (downloadState) {
                        Download.STATE_COMPLETED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    contentColor = when (downloadState) {
                        Download.STATE_COMPLETED -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                ),
                shape = CircleShape,
                onClick = {
                    when (downloadState) {
                        Download.STATE_COMPLETED -> {
                            showRemoveDownloadDialog = true
                        }
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            showRemoveDownloadDialog = true
                        }
                        else -> {
                            songs.forEach { song ->
                                val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.song.title.toByteArray())
                                    .build()
                                DownloadService.sendAddDownload(
                                    context, ExoDownloadService::class.java, downloadRequest, false
                                )
                            }
                        }
                    }
                }
            ) {
                if (downloadState == Download.STATE_QUEUED || downloadState == Download.STATE_DOWNLOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            when (downloadState) {
                                Download.STATE_COMPLETED -> R.drawable.offline
                                else -> R.drawable.download
                            }
                        ),
                        contentDescription = "Download"
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (downloadState) {
                        Download.STATE_COMPLETED -> "Remove Offline"
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> "Downloading..."
                        else -> "Download Playlist"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Details Section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Start Radio
            playlist.playlist.browseId?.let { browseId ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube.playlist(browseId).getOrNull()?.playlist?.let { playlistItem ->
                                playlistItem.radioEndpoint?.let { radioEndpoint ->
                                    withContext(Dispatchers.Main) {
                                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                    }
                                }
                            }
                        }
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = "Radio icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Start Radio",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Play similar songs",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Play Next
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    coroutineScope.launch {
                        playerConnection.playNext(songs.map { it.toMediaItem() })
                    }
                    onDismiss()
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

            // Edit Playlist
            if (editable && autoPlaylist != true) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        showEditDialog = true
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = "Edit icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Edit Playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Rename playlist",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Favorite/Bookmark (only for non-editable playlists)
            if (!editable) {
                FilledIconButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    onClick = {
                        database.query {
                            dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
                        }
                    },
                    shape = favoriteButtonShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = favoriteButtonContainerColor,
                        contentColor = favoriteButtonContentColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = "Favorite icon",
                            tint = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isFavorite) "Remove bookmark" else "Bookmark playlist",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Delete Playlist
            if (autoPlaylist != true) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    onClick = {
                        showDeletePlaylistDialog = true
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Delete Playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Remove permanently",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_playlist)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue = TextFieldValue(
                playlist.playlist.name,
                TextRange(playlist.playlist.name.length),
            ),
            onDone = { name ->
                onDismiss()
                database.query {
                    update(
                        playlist.playlist.copy(
                            name = name,
                            lastUpdateTime = LocalDateTime.now()
                        )
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                }
            },
        )
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.playlist.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = { showDeletePlaylistDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            if (playlist.playlist.bookmarkedAt != null) {
                                update(playlist.playlist.toggleLike())
                            }
                            delete(playlist.playlist)
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }
}