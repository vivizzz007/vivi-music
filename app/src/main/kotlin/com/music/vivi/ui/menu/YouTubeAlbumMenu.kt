package com.music.vivi.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
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
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.db.entities.AlbumEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.YouTubeAlbumRadio
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.LibrarySongListItem
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Menu for a YouTube Album (online).
 * Allows managing the album (Add to library, Download), playing/shuffling/radio, and sharing.
 * Checks local library status to update UI (e.g. favorite status).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeAlbumMenu(albumItem: AlbumItem, navController: NavController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val album by database.albumWithSongs(albumItem.id).collectAsState(initial = null)
    val artists = albumItem.artists
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.album(albumItem.id).collect { album ->
            if (album == null) {
                YouTube
                    .album(albumItem.id)
                    .onSuccess { albumPage ->
                        database.transaction {
                            insert(albumPage)
                        }
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }

    val downloadState by remember(album) {
        val songs = album?.songs?.map { it.id }
        if (songs == null) {
            flowOf(Download.STATE_STOPPED)
        } else {
            downloadUtil.downloads.map { downloads ->
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
            }.flowOn(Dispatchers.Default)
        }
    }.collectAsState(initial = Download.STATE_STOPPED)

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Design variables
    val cornerRadius = remember { 24.dp }
    val albumArtShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 60
        )
    }
    val playButtonShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 60
        )
    }

    // Android 16 grouped shapes
    val topShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 0,
            cornerRadiusBR = 0.dp,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 0,
            cornerRadiusBL = 0.dp,
            smoothnessAsPercentTR = 60
        )
    }
    val middleShape = remember { RectangleShape }
    val bottomShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = 0.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 0,
            cornerRadiusTL = 0.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 0
        )
    }
    val singleShape = remember(cornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = cornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = cornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = cornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = cornerRadius,
            smoothnessAsPercentTR = 60
        )
    }

    // Favorite state tracking
    val isFavorite = album?.album?.bookmarkedAt != null

    val favoriteButtonCornerRadius by animateDpAsState(
        targetValue = if (isFavorite) cornerRadius else 60.dp,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteCornerAnimation"
    )
    val favoriteButtonContainerColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteContainerColorAnimation"
    )
    val favoriteButtonContentColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteContentColorAnimation"
    )

    val favoriteButtonShape = remember(favoriteButtonCornerRadius) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = favoriteButtonCornerRadius,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = favoriteButtonCornerRadius,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = favoriteButtonCornerRadius,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = favoriteButtonCornerRadius,
            smoothnessAsPercentTR = 60
        )
    }

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
                model = albumItem.thumbnail,
                contentDescription = stringResource(R.string.album_art_content_desc),
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
                        text = albumItem.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artists?.joinToString { it.name }.orEmpty(),
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
                        val album = album
                        if (album != null) {
                            update(album.album.toggleLike())
                        } else {
                            insert(
                                AlbumEntity(
                                    id = albumItem.id,
                                    playlistId = albumItem.playlistId,
                                    title = albumItem.title,
                                    thumbnailUrl = albumItem.thumbnail,
                                    year = albumItem.year,
                                    songCount = 0,
                                    duration = 0
                                ).toggleLike()
                            )
                        }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(
                        if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = if (isFavorite) {
                        stringResource(
                            R.string.remove_from_favorites
                        )
                    } else {
                        stringResource(R.string.add_to_favorites)
                    },
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
                    onDismiss()
                    album?.songs?.let { songs ->
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(YouTubeAlbumRadio(albumItem.playlistId))
                        }
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = stringResource(R.string.play_content_desc)
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(R.string.play_text),
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
                        val album = album
                        if (album != null) {
                            update(album.album.toggleLike())
                        } else {
                            insert(
                                AlbumEntity(
                                    id = albumItem.id,
                                    playlistId = albumItem.playlistId,
                                    title = albumItem.title,
                                    thumbnailUrl = albumItem.thumbnail,
                                    year = albumItem.year,
                                    songCount = 0,
                                    duration = 0
                                ).toggleLike()
                            )
                        }
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
                    contentDescription = if (isFavorite) {
                        stringResource(
                            R.string.remove_from_favorites
                        )
                    } else {
                        stringResource(R.string.add_to_favorites)
                    },
                    tint = if (isFavorite) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Shuffle Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(
                                playlistId = albumItem.playlistId,
                                params = "OBAO8AG8AQ%3D%3D"
                            )
                        )
                    )
                    onDismiss()
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.shuffle)
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
            shape = singleShape,
            onClick = {
                when (downloadState) {
                    Download.STATE_COMPLETED -> {
                        album?.songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                        album?.songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                    else -> {
                        album?.songs?.forEach { song ->
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
                                false
                            )
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    when (downloadState) {
                        Download.STATE_COMPLETED -> R.drawable.offline
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> R.drawable.download
                        else -> R.drawable.download
                    }
                ),
                contentDescription = stringResource(R.string.download_album_content_desc)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (downloadState) {
                    Download.STATE_COMPLETED -> stringResource(R.string.remove_offline)
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> stringResource(R.string.downloading_ellipsis)
                    else -> stringResource(R.string.download_album_text)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Playback Group
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Radio
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = topShape,
                onClick = {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(
                                playlistId = albumItem.playlistId,
                                params = "wAEB"
                            )
                        )
                    )
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.radio),
                    contentDescription = stringResource(R.string.start_radio)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.start_radio_label),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.play_similar_songs_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Play Next
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = middleShape,
                onClick = {
                    coroutineScope.launch {
                        YouTube.album(albumItem.id).onSuccess { albumResponse ->
                            playerConnection.playNext(albumResponse.songs.map { it.toMediaItem() })
                        }
                    }
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_play),
                    contentDescription = stringResource(R.string.play_next_icon_desc)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.play_next_label),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.play_after_current_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Add to Queue
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = bottomShape,
                onClick = {
                    coroutineScope.launch {
                        YouTube.album(albumItem.id).onSuccess { albumResponse ->
                            playerConnection.addToQueue(albumResponse.songs.map { it.toMediaItem() })
                        }
                    }
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = stringResource(R.string.add_to_queue_icon_desc)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.add_to_queue_label),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.add_to_queue_end_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Library and Social Group
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // View Artist
            artists?.let { artists ->
                if (artists.isNotEmpty()) {
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp),
                        shape = topShape,
                        onClick = {
                            if (artists.size == 1) {
                                navController.navigate("artist/${artists[0].id}")
                                onDismiss()
                            } else {
                                showSelectArtistDialog = true
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = stringResource(R.string.artist_icon_desc)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.view_artist_label),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Add to Playlist
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = middleShape,
                onClick = {
                    showChoosePlaylistDialog = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = stringResource(R.string.add_to_playlist_icon_content_desc)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.add_to_playlist_label),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.add_to_existing_playlist_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Share
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = bottomShape,
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, albumItem.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = stringResource(R.string.share)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.share_label),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.share_this_album_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Dialogs
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        songsToCheck = album?.songs?.map { it.id }.orEmpty(),
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    album?.album?.playlistId?.let { addPlaylistId ->
                        YouTube.addPlaylistToPlaylist(playlistId, addPlaylistId)
                    }
                }
            }
            album?.songs?.map { it.id }.orEmpty()
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            }
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false }
                )
            }

            items(notAddedList) { song: Song ->
                LibrarySongListItem(song = song)
            }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(
                items = album?.artists.orEmpty().distinctBy { it.id },
                key = { it.id }
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp)
                ) {
                    AsyncImage(
                        model = artist.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
