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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.utils.completed
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.utils.resize
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Menu for a YouTube Playlist (online).
 * Allows importing the playlist, playing/shuffling/radio, adding to queue, and sharing.
 * If songs are provided, allows downloading the playlist.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDownloadDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    val downloadState by remember(songs) {
        if (songs.isEmpty()) {
            flowOf(Download.STATE_STOPPED)
        } else {
            downloadUtil.downloads.map { downloads ->
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
            }.flowOn(Dispatchers.Default)
        }
    }.collectAsState(initial = Download.STATE_STOPPED)

    // Design variables
    val cornerRadius = remember { 24.dp }
    val playlistArtShape = remember(cornerRadius) {
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
    val isFavorite = dbPlaylist?.playlist?.bookmarkedAt != null
    val showFavoriteButton = playlist.id != "LM" && !playlist.isEditable

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

        // Header Row - Playlist Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = playlist.thumbnail,
                contentDescription = stringResource(R.string.playlist_art),
                modifier = Modifier
                    .size(80.dp)
                    .clip(playlistArtShape),
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
                        text = playlist.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playlist.songCountText ?: stringResource(R.string.playlist_fallback),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Header favorite button (only show if applicable)
            if (showFavoriteButton) {
                FilledTonalIconButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 6.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = {
                        if (dbPlaylist?.playlist == null) {
                            database.transaction {
                                val playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = false,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                ).toggleLike()
                                insert(playlistEntity)
                                coroutineScope.launch(Dispatchers.IO) {
                                    songs.ifEmpty {
                                        YouTube.playlist(playlist.id).completed()
                                            .getOrNull()?.songs.orEmpty()
                                    }.map { it.toMediaMetadata() }
                                        .onEach(::insert)
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index
                                            )
                                        }
                                        .forEach(::insert)
                                }
                            }
                        } else {
                            database.transaction {
                                val currentPlaylist = dbPlaylist!!.playlist
                                update(currentPlaylist, playlist)
                                update(currentPlaylist.toggleLike())
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row - Dynamic based on available endpoints
        val hasPlay = playlist.playEndpoint != null
        val hasRadio = playlist.radioEndpoint != null

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Play Button (if available)
            if (hasPlay) {
                playlist.playEndpoint?.let { playEndpoint ->
                    FilledTonalButton(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight(),
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(playEndpoint))
                            onDismiss()
                        },
                        shape = singleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = stringResource(R.string.play_button)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.play_button),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            // Favorite Button (if applicable)
            if (showFavoriteButton) {
                FilledIconButton(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    onClick = {
                        if (dbPlaylist?.playlist == null) {
                            database.transaction {
                                val playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    thumbnailUrl = playlist.thumbnail,
                                    isEditable = false,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params
                                ).toggleLike()
                                insert(playlistEntity)
                                coroutineScope.launch(Dispatchers.IO) {
                                    songs.ifEmpty {
                                        YouTube.playlist(playlist.id).completed()
                                            .getOrNull()?.songs.orEmpty()
                                    }.map { it.toMediaMetadata() }
                                        .onEach(::insert)
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index
                                            )
                                        }
                                        .forEach(::insert)
                                }
                            }
                        } else {
                            database.transaction {
                                val currentPlaylist = dbPlaylist!!.playlist
                                update(currentPlaylist, playlist)
                                update(currentPlaylist.toggleLike())
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
                        }
                    )
                }
            }

            // Share Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.share),
                    contentDescription = stringResource(R.string.share_playlist)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Download Button (only show if songs available)
        if (songs.isNotEmpty()) {
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
                            showRemoveDownloadDialog = true
                        }
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            showRemoveDownloadDialog = true
                        }
                        else -> {
                            songs.forEach { song ->
                                val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.title.toByteArray())
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
                    contentDescription = stringResource(R.string.download_playlist)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (downloadState) {
                        Download.STATE_COMPLETED -> stringResource(R.string.remove_offline)
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> stringResource(
                            R.string.downloading_ellipsis
                        )
                        else -> stringResource(R.string.download_playlist_text)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Actions Section
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Shuffle Button (if available)
            playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = topShape,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle_icon)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.shuffle_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.play_in_random_order),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Radio Button (if available)
            playlist.radioEndpoint?.let { radioEndpoint ->
                val radioShape = if (playlist.shuffleEndpoint != null) middleShape else topShape
                Spacer(modifier = Modifier.height(1.dp))
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = radioShape,
                    onClick = {
                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = stringResource(R.string.radio_icon)
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
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Play Next
            val playNextShape = if (playlist.radioEndpoint != null ||
                playlist.shuffleEndpoint != null
            ) {
                middleShape
            } else {
                topShape
            }
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = playNextShape,
                onClick = {
                    coroutineScope.launch {
                        songs
                            .ifEmpty {
                                withContext(Dispatchers.IO) {
                                    YouTube
                                        .playlist(playlist.id)
                                        .completed()
                                        .getOrNull()
                                        ?.songs
                                        .orEmpty()
                                }
                            }.let { songs ->
                                playerConnection.playNext(
                                    songs.map {
                                        it.copy(thumbnail = it.thumbnail.resize(544, 544)).toMediaItem()
                                    }
                                )
                            }
                    }
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_play),
                    contentDescription = stringResource(R.string.play_next_icon)
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
                        stringResource(R.string.play_after_current),
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
                shape = middleShape,
                onClick = {
                    coroutineScope.launch {
                        songs
                            .ifEmpty {
                                withContext(Dispatchers.IO) {
                                    YouTube
                                        .playlist(playlist.id)
                                        .completed()
                                        .getOrNull()
                                        ?.songs
                                        .orEmpty()
                                }
                            }.let { songs ->
                                playerConnection.addToQueue(
                                    songs.map {
                                        it.copy(thumbnail = it.thumbnail.resize(544, 544)).toMediaItem()
                                    }
                                )
                            }
                    }
                    onDismiss()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = stringResource(R.string.add_to_queue_icon)
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
                        stringResource(R.string.add_to_queue_end),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Add to Playlist
            val addToPlaylistShape = if (canSelect) middleShape else bottomShape
            Spacer(modifier = Modifier.height(1.dp))
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = addToPlaylistShape,
                onClick = {
                    showChoosePlaylistDialog = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = stringResource(R.string.add_to_playlist_label)
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

            // Select (if applicable)
            if (canSelect) {
                Spacer(modifier = Modifier.height(1.dp))
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = bottomShape,
                    onClick = {
                        onDismiss()
                        selectAction()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.select_all),
                        contentDescription = stringResource(R.string.select_label)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.select_label),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.this_collection_text),
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
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        songsToCheck = songs.map { it.id },
        onGetSong = { targetPlaylist ->
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(targetPlaylist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            targetPlaylist.playlist.browseId?.let { playlistId ->
                YouTube.addPlaylistToPlaylist(playlistId, targetPlaylist.id)
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs = songs
                .ifEmpty {
                    YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach(::insert)
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        onDismiss = { showImportPlaylistDialog = false }
    )

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
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
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

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

            items(notAddedList) { song ->
                ListItem(
                    headlineContent = { Text(text = song.title) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize)
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text = joinByBullet(
                                song.artists.joinToString { it.name },
                                makeTimeString(song.duration * 1000L)
                            )
                        )
                    }
                )
            }
        }
    }
}
