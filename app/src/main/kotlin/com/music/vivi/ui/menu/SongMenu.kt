package com.music.vivi.ui.menu

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.LocalSyncUtils
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.Event
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current.getDownload(originalSong.id)
        .collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted = artistMaps.mapNotNull { map ->
                song.artists.firstOrNull { it.id == map.artistId }
            }
            value = sorted
        }
    }

    // State management
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    val TextFieldValueSaver: Saver<TextFieldValue, *> = Saver(
        save = { it.text },
        restore = { text -> TextFieldValue(text, TextRange(text.length)) }
    )

    var titleField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.song.title))
    }

    var artistField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.artists.firstOrNull()?.name.orEmpty()))
    }

    val bottomSheetPageState = LocalBottomSheetPageState.current

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
    val isFavorite = song.song.liked

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

    // Play/Pause state tracking
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isCurrentSongPlaying = remember(mediaMetadata, song) {
        song.id == mediaMetadata?.id
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // Header Row - Song Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = "Song Art",
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
                        text = song.song.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artists.joinToString { it.name },
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
                    val currentSong = song.song
                    val updatedSong = currentSong.copy(liked = !currentSong.liked)
                    database.query {
                        update(updatedSong)
                    }
                    syncUtils.likeSong(updatedSong)
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
                    if (isCurrentSongPlaying && isPlaying) {
                        playerConnection.player.pause()
                    } else if (isCurrentSongPlaying && !isPlaying) {
                        playerConnection.player.play()
                    } else {
                        onDismiss()
                        playerConnection.playQueue(
                            ListQueue(
                                title = song.song.title,
                                items = listOf(song.toMediaItem())
                            )
                        )
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(
                            if (isCurrentSongPlaying && isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (isCurrentSongPlaying && isPlaying) "Pause" else "Play"
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = if (isCurrentSongPlaying && isPlaying) "Pause" else "Play",
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
                    val currentSong = song.song
                    val updatedSong = currentSong.copy(liked = !currentSong.liked)
                    database.query {
                        update(updatedSong)
                    }
                    syncUtils.likeSong(updatedSong)
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
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                shape = CircleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.share),
                    contentDescription = "Share song"
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
                when (download?.state) {
                    Download.STATE_COMPLETED -> {
                        DownloadService.sendRemoveDownload(
                            context, ExoDownloadService::class.java, song.id, false
                        )
                    }
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                        DownloadService.sendRemoveDownload(
                            context, ExoDownloadService::class.java, song.id, false
                        )
                    }
                    else -> {
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
        ) {
            Icon(
                painter = painterResource(
                    when (download?.state) {
                        Download.STATE_COMPLETED -> R.drawable.offline
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> R.drawable.download
                        else -> R.drawable.download
                    }
                ),
                contentDescription = "Download song"
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (download?.state) {
                    Download.STATE_COMPLETED -> "Remove Offline"
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> "Downloading..."
                    else -> "Download Song"
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
                    if (song.artists.size == 1) {
                        navController.navigate("artist/${song.artists[0].id}")
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
                        song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Album
            if (song.song.albumId != null) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        onDismiss()
                        navController.navigate("album/${song.song.albumId}")
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.album),
                        contentDescription = "Album icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Album",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "View full album",
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
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
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
                    playerConnection.addToQueue(song.toMediaItem())
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

            // Start Radio
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    onDismiss()
                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.radio),
                    contentDescription = "Start radio icon"
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

            // Edit Song
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
                        "Edit",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Edit song details",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Library Management
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    val currentSong = song.song
                    val isInLibrary = currentSong.inLibrary != null
                    val token = if (isInLibrary) currentSong.libraryRemoveToken else currentSong.libraryAddToken

                    token?.let {
                        coroutineScope.launch {
                            YouTube.feedback(listOf(it))
                        }
                    }

                    database.query {
                        update(song.song.toggleLibrary())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (song.song.inLibrary == null) R.drawable.library_add
                        else R.drawable.library_add_check
                    ),
                    contentDescription = "Library icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (song.song.inLibrary == null) "Add to Library" else "Remove from Library",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (song.song.inLibrary == null) "Save to your music" else "Remove from your music",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Conditional items
            if (event != null) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        onDismiss()
                        database.query { delete(event) }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Remove from History",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Delete from listening history",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (playlistSong != null) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        database.transaction {
                            coroutineScope.launch {
                                playlistBrowseId?.let { playlistId ->
                                    if (playlistSong.map.setVideoId != null) {
                                        YouTube.removeFromPlaylist(
                                            playlistId, playlistSong.map.songId, playlistSong.map.setVideoId
                                        )
                                    }
                                }
                            }
                            move(playlistSong.map.playlistId, playlistSong.map.position, Int.MAX_VALUE)
                            delete(playlistSong.map.copy(position = Int.MAX_VALUE))
                        }
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Remove from Playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Delete from this playlist",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isFromCache) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        onDismiss()
                        cacheViewModel.removeSongFromCache(song.id)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Remove from Cache",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Clear cached data",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Refetch
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    refetchIconDegree -= 360
                    scope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(song.id)).onSuccess {
                            val newSong = it.firstOrNull()
                            if (newSong != null) {
                                database.transaction {
                                    update(song, newSong.toMediaMetadata())
                                }
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
                        "Update song information",
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

            // Song Details
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp),
                shape = CircleShape,
                onClick = {
                    onDismiss()
                    bottomSheetPageState.show {
                        ShowMediaInfo(song.id)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = "Info icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Song Details",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "View information",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Dialogs
    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_song)) },
            textFields = listOf(
                stringResource(R.string.song_title) to titleField,
                stringResource(R.string.artist_name) to artistField
            ),
            onTextFieldsChange = { index, newValue ->
                if (index == 0) titleField = newValue
                else artistField = newValue
            },
            onDoneMultiple = { values ->
                coroutineScope.launch {
                    database.query {
                        update(song.song.copy(title = values[0]))
                        val artist = song.artists.firstOrNull()
                        if (artist != null) {
                            update(artist.copy(name = values[1]))
                        }
                    }
                    showEditDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
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
            items(items = song.artists, key = { it.id }) { artist ->
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