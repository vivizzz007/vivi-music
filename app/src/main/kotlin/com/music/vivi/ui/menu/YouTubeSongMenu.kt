package com.music.vivi.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.NewAction
import com.music.vivi.ui.component.NewActionGrid
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
    onHistoryRemoved: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val artists = remember {
        song.artists.mapNotNull {
            it.id?.let { artistId ->
                MediaMetadata.Artist(id = artistId, name = it.name)
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val bottomSheetPageState = LocalBottomSheetPageState.current

    // Modern Card-based Layout
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
        // Modern Song Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Song thumbnail with modern styling
                    Card(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = song.thumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artists.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Status indicators
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (download?.state == Download.STATE_COMPLETED) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Downloaded",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (librarySong?.song?.liked == true) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.favorite),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Liked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Like button with modern styling
                    FilledTonalIconButton(
                        onClick = {
                            database.transaction {
                                librarySong.let { librarySong ->
                                    val s: SongEntity
                                    if (librarySong == null) {
                                        insert(song.toMediaMetadata(), SongEntity::toggleLike)
                                        s = song.toMediaMetadata().toSongEntity().let(SongEntity::toggleLike)
                                    } else {
                                        s = librarySong.song.toggleLike()
                                        update(s)
                                    }
                                    syncUtils.likeSong(s)
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (librarySong?.song?.liked == true)
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            painter = painterResource(
                                if (librarySong?.song?.liked == true) R.drawable.favorite
                                else R.drawable.favorite_border
                            ),
                            tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        // Modern Action Grid
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
                // Download Action
                item {
                    when (download?.state) {
                        Download.STATE_COMPLETED -> {
                            ModernActionCard(
                                icon = R.drawable.offline,
                                title = "Downloaded",
                                subtitle = "Tap to remove",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context, ExoDownloadService::class.java, song.id, false
                                    )
                                }
                            )
                        }
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            ModernActionCard(
                                icon = R.drawable.download,
                                title = "Downloading",
                                subtitle = "In progress...",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                showProgress = true,
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context, ExoDownloadService::class.java, song.id, false
                                    )
                                }
                            )
                        }
                        else -> {
                            ModernActionCard(
                                icon = R.drawable.download,
                                title = "Download",
                                subtitle = "Save offline",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    database.transaction {
                                        insert(song.toMediaMetadata())
                                    }
                                    val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                        .setCustomCacheKey(song.id)
                                        .setData(song.title.toByteArray())
                                        .build()
                                    DownloadService.sendAddDownload(
                                        context, ExoDownloadService::class.java, downloadRequest, false
                                    )
                                }
                            )
                        }
                    }
                }

                // Add to Playlist
                item {
                    ModernActionCard(
                        icon = R.drawable.playlist_add,
                        title = "Playlist",
                        subtitle = "Add to playlist",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { showChoosePlaylistDialog = true }
                    )
                }

                // Share
                item {
                    ModernActionCard(
                        icon = R.drawable.share,
                        title = "Share",
                        subtitle = "Send to others",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, song.shareLink)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                }
            }
        }

        // Playback Actions Section
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
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    ModernListItem(
                        icon = R.drawable.radio,
                        title = "Start radio",
                        subtitle = "Play similar songs",
                        onClick = {
                            onDismiss()
                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    ModernListItem(
                        icon = R.drawable.playlist_play,
                        title = "Play next",
                        subtitle = "Add to front of queue",
                        onClick = {
                            onDismiss()
                            playerConnection.playNext(song.copy(thumbnail = song.thumbnail.resize(544,544)).toMediaItem())
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    ModernListItem(
                        icon = R.drawable.queue_music,
                        title = "Add to queue",
                        subtitle = "Play after current queue",
                        onClick = {
                            onDismiss()
                            playerConnection.addToQueue(song.toMediaItem())
                        }
                    )
                }
            }
        }

        // Management Actions Section
        item {
            Text(
                text = "Manage",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    ModernListItem(
                        icon = if (librarySong?.song?.inLibrary != null) R.drawable.library_add_check else R.drawable.library_add,
                        title = if (librarySong?.song?.inLibrary != null) "Remove from library" else "Add to library",
                        subtitle = if (librarySong?.song?.inLibrary != null) "Remove from your music" else "Save to your music",
                        onClick = {
                            val isInLibrary = librarySong?.song?.inLibrary != null
                            val token = if (isInLibrary) song.libraryRemoveToken else song.libraryAddToken

                            token?.let {
                                coroutineScope.launch {
                                    YouTube.feedback(listOf(it))
                                }
                            }

                            if (isInLibrary) {
                                database.query {
                                    inLibrary(song.id, null)
                                }
                            } else {
                                database.transaction {
                                    insert(song.toMediaMetadata())
                                    inLibrary(song.id, LocalDateTime.now())
                                    addLibraryTokens(song.id, song.libraryAddToken, song.libraryRemoveToken)
                                }
                            }
                        }
                    )

                    if (song.historyRemoveToken != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )
                        ModernListItem(
                            icon = R.drawable.delete,
                            title = "Remove from history",
                            subtitle = "Delete from listening history",
                            onClick = {
                                coroutineScope.launch {
                                    YouTube.feedback(listOf(song.historyRemoveToken!!))

                                    delay(500)

                                    onHistoryRemoved()

                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Browse Actions Section
        item {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column {
                    if (artists.isNotEmpty()) {
                        ModernListItem(
                            icon = R.drawable.artist,
                            title = "View artist",
                            subtitle = artists.joinToString(", ") { it.name },
                            onClick = {
                                if (artists.size == 1) {
                                    artists.firstOrNull()?.id?.let { artistId ->
                                        navController.navigate("artist/$artistId")
                                        onDismiss()
                                    }
                                } else {
                                    showSelectArtistDialog = true
                                }
                            }
                        )

                        if (song.album != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                        }
                    }

                    song.album?.let { album ->
                        ModernListItem(
                            icon = R.drawable.album,
                            title = "View album",
                            subtitle = "Go to full album",
                            onClick = {
                                onDismiss()
                                navController.navigate("album/${album.id}")
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )

                    ModernListItem(
                        icon = R.drawable.info,
                        title = "Song details",
                        subtitle = "View information",
                        onClick = {
                            onDismiss()
                            bottomSheetPageState.show {
                                ShowMediaInfo(song.id)
                            }
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

    // Dialogs
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(song.toMediaMetadata())
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(items = artists, key = { it.id ?: "" }) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            artist.id?.let { artistId ->
                                navController.navigate("artist/$artistId")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    // Note: You might want to add artist thumbnails here if available
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .padding(horizontal = 24.dp),
                    ) {
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
}

