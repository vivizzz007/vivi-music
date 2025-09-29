package com.music.vivi.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
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
import com.music.vivi.ui.component.AlbumListItem
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.ListItem
import com.music.vivi.ui.component.NewAction
import com.music.vivi.ui.component.NewActionGrid
import com.music.vivi.ui.component.SongListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


import androidx.compose.material3.TextButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults


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
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            item {
                Text(
                    text = "Select Artist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(album.artists.distinctBy { it.id }) { artist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(16.dp)
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Album Header Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    AsyncImage(
                        model = album.album.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.album.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = album.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${songs.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = {
                            database.query {
                                update(album.album.toggleLike())
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(if (album.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                            tint = if (album.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Section
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // Play Action
                item {
                    ModernActionCard(
                        icon = R.drawable.play,
                        title = "Play",
                        subtitle = "Play album",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
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
                    )
                }

                // Shuffle Action
                item {
                    ModernActionCard(
                        icon = R.drawable.shuffle,
                        title = "Shuffle",
                        subtitle = "Shuffle play",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            onDismiss()
                            if (songs.isNotEmpty()) {
                                album.album.playlistId?.let { playlistId ->
                                    playerConnection.service.getAutomix(playlistId)
                                }
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = album.album.title,
                                        items = songs.shuffled().map(Song::toMediaItem)
                                    )
                                )
                            }
                        }
                    )
                }

                // Download Action
                item {
                    when (downloadState) {
                        STATE_COMPLETED -> {
                            ModernActionCard(
                                icon = R.drawable.offline,
                                title = "Downloaded",
                                subtitle = "Remove offline",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                onClick = {
                                    songs.forEach { song ->
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    }
                                }
                            )
                        }
                        STATE_QUEUED, STATE_DOWNLOADING -> {
                            ModernActionCard(
                                icon = R.drawable.download,
                                title = "Downloading",
                                subtitle = "In progress...",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                showProgress = true,
                                onClick = {
                                    songs.forEach { song ->
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            song.id,
                                            false,
                                        )
                                    }
                                }
                            )
                        }
                        else -> {
                            ModernActionCard(
                                icon = R.drawable.download,
                                title = "Download",
                                subtitle = "Save offline",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = {
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
                            )
                        }
                    }
                }

                // Share Action
                item {
                    ModernActionCard(
                        icon = R.drawable.share,
                        title = "Share",
                        subtitle = "Share album",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${album.album.playlistId}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                }
            }
        }

        // Playback Section
        item {
            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    ModernListItem(
                        icon = R.drawable.playlist_play,
                        title = stringResource(R.string.play_next),
                        subtitle = "Play after current",
                        onClick = {
                            onDismiss()
                            playerConnection.playNext(songs.map { it.toMediaItem() })
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    ModernListItem(
                        icon = R.drawable.queue_music,
                        title = stringResource(R.string.add_to_queue),
                        subtitle = "Add to queue end",
                        onClick = {
                            onDismiss()
                            playerConnection.addToQueue(songs.map { it.toMediaItem() })
                        }
                    )
                }
            }
        }

        // Management Section
        item {
            Text(
                text = "Management",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    ModernListItem(
                        icon = R.drawable.playlist_add,
                        title = stringResource(R.string.add_to_playlist),
                        subtitle = "Add to existing playlist",
                        onClick = {
                            showChoosePlaylistDialog = true
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    ModernListItem(
                        icon = R.drawable.artist,
                        title = stringResource(R.string.view_artist),
                        subtitle = if (album.artists.size == 1) album.artists.first().name else "${album.artists.size} artists",
                        onClick = {
                            if (album.artists.size == 1) {
                                navController.navigate("artist/${album.artists[0].id}")
                                onDismiss()
                            } else {
                                showSelectArtistDialog = true
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    ModernListItem(
                        icon = R.drawable.sync,
                        title = stringResource(R.string.refetch),
                        subtitle = "Update album information",
                        onClick = {
                            refetchIconDegree -= 360
                            scope.launch(Dispatchers.IO) {
                                YouTube.album(album.id).onSuccess {
                                    database.transaction {
                                        update(album.album, it, album.artists)
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(rotationZ = rotationAnimation),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}