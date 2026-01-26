package com.music.vivi.ui.menu

import android.annotation.SuppressLint
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.ui.component.DefaultDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.time.LocalDateTime

/**
 * Menu for a selection of songs (local/library).
 * Provides bulk actions like Play, Shuffle, Add to Playlist, Download, Queue, Library management, Like, and Delete.
 */
@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectionSongMenu(
    songSelection: List<Song>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
    songPosition: List<PlaylistSongMap>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val syncUtils = LocalSyncUtils.current

    val allInLibrary by remember(songSelection) {
        mutableStateOf(
            songSelection.all {
                it.song.inLibrary != null
            }
        )
    }

    val allLiked by remember(songSelection) {
        mutableStateOf(
            songSelection.isNotEmpty() &&
                songSelection.all {
                    it.song.liked
                }
        )
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
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

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        songsToCheck = songSelection.map { it.id },
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                songSelection.forEach { song ->
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, song.id)
                    }
                }
            }
            songSelection.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songSelection.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
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

    // Design variables - Android 16 style
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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Selection Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstSong = songSelection.firstOrNull()
            AsyncImage(
                model = firstSong?.song?.thumbnailUrl,
                contentDescription = stringResource(R.string.song_art),
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
                        text = stringResource(R.string.selection),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${songSelection.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row (Play, Shuffle, Add to Playlist)
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
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.selection),
                            items = songSelection.map { it.toMediaItem() }
                        )
                    )
                    clearAction()
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = stringResource(R.string.play)
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(R.string.play),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            )

            // Shuffle Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    onDismiss()
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.selection),
                            items = songSelection.shuffled().map { it.toMediaItem() }
                        )
                    )
                    clearAction()
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.shuffle)
                )
            }

            // Add to Playlist Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    showChoosePlaylistDialog = true
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = stringResource(R.string.add_to_playlist)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Download Button (standalone)
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
                    Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> showRemoveDownloadDialog = true
                    else -> {
                        songSelection.forEach { song ->
                            val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
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
                contentDescription = stringResource(R.string.download_song)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (downloadState) {
                    Download.STATE_COMPLETED -> stringResource(R.string.remove_download)
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> stringResource(R.string.downloading_ellipsis)
                    else -> stringResource(R.string.action_download)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Queue Management Group
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Play Next (Top)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = topShape,
                onClick = {
                    onDismiss()
                    // Need to reverse to play in correct order next
                    playerConnection.playNext(songSelection.map { it.toMediaItem() })
                    clearAction()
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

            // Add to Queue (Bottom)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = bottomShape,
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
                    clearAction()
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Library & Delete Group
        val libraryItems = mutableListOf<@Composable () -> Unit>()

        // Library Action
        libraryItems.add {
            val shape = if (libraryItems.size == 0 && songPosition?.isNotEmpty() != true) {
                singleShape
            } else if (libraryItems.size == 0) {
                topShape
            } else if (songPosition?.isNotEmpty() != true) {
                bottomShape
            } else {
                middleShape
            }

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = topShape, // Always top in this group structure
                onClick = {
                    if (allInLibrary) {
                        database.query {
                            songSelection.forEach { song ->
                                inLibrary(song.id, null)
                            }
                        }
                        coroutineScope.launch {
                            val tokens = songSelection.mapNotNull { it.song.libraryRemoveToken }
                            tokens.chunked(20).forEach {
                                YouTube.feedback(it)
                            }
                        }
                    } else {
                        database.transaction {
                            songSelection.forEach { song ->
                                insert(song.toMediaMetadata())
                                inLibrary(song.id, LocalDateTime.now())
                            }
                        }
                        coroutineScope.launch {
                            val tokens = songSelection.filter { it.song.inLibrary == null }
                                .mapNotNull { it.song.libraryAddToken }
                            tokens.chunked(20).forEach {
                                YouTube.feedback(it)
                            }
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (allInLibrary) R.drawable.library_add_check else R.drawable.library_add
                    ),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (allInLibrary) {
                            stringResource(R.string.remove_from_library)
                        } else {
                            stringResource(
                                R.string.add_to_library
                            )
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (allInLibrary) {
                            stringResource(R.string.remove_from_your_music)
                        } else {
                            stringResource(
                                R.string.save_to_your_music
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Library (Top)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = topShape,
                onClick = {
                    if (allInLibrary) {
                        database.query {
                            songSelection.forEach { song ->
                                inLibrary(song.id, null)
                            }
                        }
                        coroutineScope.launch {
                            val tokens = songSelection.mapNotNull { it.song.libraryRemoveToken }
                            tokens.chunked(20).forEach {
                                YouTube.feedback(it)
                            }
                        }
                    } else {
                        database.transaction {
                            songSelection.forEach { song ->
                                insert(song.toMediaMetadata())
                                inLibrary(song.id, LocalDateTime.now())
                            }
                        }
                        coroutineScope.launch {
                            val tokens = songSelection.filter { it.song.inLibrary == null }
                                .mapNotNull { it.song.libraryAddToken }
                            tokens.chunked(20).forEach {
                                YouTube.feedback(it)
                            }
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (allInLibrary) R.drawable.library_add_check else R.drawable.library_add
                    ),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (allInLibrary) {
                            stringResource(R.string.remove_from_library)
                        } else {
                            stringResource(
                                R.string.add_to_library
                            )
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (allInLibrary) {
                            stringResource(R.string.remove_from_your_music)
                        } else {
                            stringResource(
                                R.string.save_to_your_music
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(1.dp))

            // Like (Middle or Bottom)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = if (songPosition?.isNotEmpty() == true) middleShape else bottomShape,
                onClick = {
                    database.query {
                        if (allLiked) {
                            songSelection.forEach { song ->
                                update(song.song.toggleLike())
                            }
                        } else {
                            songSelection.filter { !it.song.liked }.forEach { song ->
                                update(song.song.toggleLike())
                            }
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (allLiked) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (allLiked) stringResource(R.string.dislike_all) else stringResource(R.string.like_all),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Delete (Bottom if present)
            if (songPosition?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(1.dp))

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = bottomShape,
                    onClick = {
                        onDismiss()
                        var i = 0
                        database.query {
                            songPosition.forEach { cur ->
                                move(cur.playlistId, cur.position - i, Int.MAX_VALUE)
                                delete(cur.copy(position = Int.MAX_VALUE))
                                i++
                            }
                        }
                        clearAction()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.delete),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Menu for a selection of MediaMetadata (used in some contexts).
 * Provides bulk actions similar to SelectionSongMenu but for MediaMetadata objects.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionMediaMetadataMenu(
    songSelection: List<MediaMetadata>,
    currentItems: List<Timeline.Window>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val allLiked by remember(songSelection) {
        mutableStateOf(songSelection.isNotEmpty() && songSelection.all { it.liked })
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        songsToCheck = songSelection.map { it.id },
        onGetSong = {
            songSelection.map {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        database.insert(it)
                    }
                }
                it.id
            }
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
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

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songSelection.forEach { song ->
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

    // Design variables - Android 16 style
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

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Selection Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstSong = songSelection.firstOrNull()
            AsyncImage(
                model = firstSong?.thumbnailUrl,
                contentDescription = stringResource(R.string.song_art),
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
                        text = stringResource(R.string.selection),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${songSelection.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row (Play, Shuffle, Add to Playlist)
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
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.selection),
                            items = songSelection.map { it.toMediaItem() }
                        )
                    )
                    clearAction()
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = stringResource(R.string.play)
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(R.string.play),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            )

            // Shuffle Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    onDismiss()
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.selection),
                            items = songSelection.shuffled().map { it.toMediaItem() }
                        )
                    )
                    clearAction()
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.shuffle)
                )
            }

            // Add to Playlist Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    showChoosePlaylistDialog = true
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = stringResource(R.string.add_to_playlist)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Download Button (standalone)
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
                    Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> showRemoveDownloadDialog = true
                    else -> {
                        songSelection.forEach { song ->
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
                contentDescription = stringResource(R.string.download_song)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (downloadState) {
                    Download.STATE_COMPLETED -> stringResource(R.string.remove_download)
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> stringResource(R.string.downloading_ellipsis)
                    else -> stringResource(R.string.action_download)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Queue Management Group
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Add to Queue (Top)
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = if (currentItems.isNotEmpty()) topShape else singleShape,
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
                    clearAction()
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

            // Delete from Queue (Bottom if present)
            if (currentItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = bottomShape,
                    onClick = {
                        onDismiss()
                        var i = 0
                        currentItems.forEach { cur ->
                            if (playerConnection.player.availableCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
                                playerConnection.player.removeMediaItem(cur.firstPeriodIndex - i++)
                            }
                        }
                        clearAction()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.delete),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Like Button (standalone)
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp),
            shape = singleShape,
            onClick = {
                database.query {
                    if (allLiked) {
                        songSelection.forEach { song ->
                            update(song.toSongEntity().toggleLike())
                        }
                    } else {
                        songSelection.filter { !it.liked }.forEach { song ->
                            update(song.toSongEntity().toggleLike())
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    if (allLiked) R.drawable.favorite else R.drawable.favorite_border
                ),
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (allLiked) stringResource(R.string.dislike_all) else stringResource(R.string.like_all),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
