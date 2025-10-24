package com.music.vivi.ui.menu


import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.ListQueue
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.utils.ShowMediaInfo
import com.music.vivi.ui.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    // State management
    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

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

    val isFavorite = librarySong?.song?.liked == true
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

    // Shared favorite toggle function
    val toggleFavorite: () -> Unit = {
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
    }

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

        // Header Row - Song Art and Title (removed duplicate favorite button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.thumbnail,
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
                        text = song.title,
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                title = song.title,
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
                        text = if (isCurrentSongPlaying && isPlaying) "Pause" else "Play"
                    )
                }
            )

            // Favorite Button (fixed with shared toggle function)
            FilledIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = toggleFavorite,
                shape = favoriteButtonShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = favoriteButtonContainerColor,
                    contentColor = favoriteButtonContentColor
                )
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
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
                        putExtra(Intent.EXTRA_TEXT, song.shareLink)
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

        Spacer(modifier = Modifier.height(20.dp))

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
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Details Section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Artist
            if (artists.isNotEmpty()) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
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
                ) {
                    Icon(
                        painter = painterResource(R.drawable.artist),
                        contentDescription = "Artist icon"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Artist",
                            style = MaterialTheme.typography.bodyLarge
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

            // Album
            song.album?.let { album ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        onDismiss()
                        navController.navigate("album/${album.id}")
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "View full album",
                            style = MaterialTheme.typography.bodySmall
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
                    playerConnection.playNext(song.copy(thumbnail = song.thumbnail.resize(544, 544)).toMediaItem())
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Play after current",
                        style = MaterialTheme.typography.bodySmall
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Add to queue end",
                        style = MaterialTheme.typography.bodySmall
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Play similar songs",
                        style = MaterialTheme.typography.bodySmall
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Add to existing playlist",
                        style = MaterialTheme.typography.bodySmall
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
            ) {
                Icon(
                    painter = painterResource(
                        if (librarySong?.song?.inLibrary == null) R.drawable.library_add
                        else R.drawable.library_add_check
                    ),
                    contentDescription = "Library icon"
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (librarySong?.song?.inLibrary == null) "Add to Library" else "Remove from Library",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        if (librarySong?.song?.inLibrary == null) "Save to your music" else "Remove from your music",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Remove from History
            if (song.historyRemoveToken != null) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = CircleShape,
                    onClick = {
                        coroutineScope.launch {
                            YouTube.feedback(listOf(song.historyRemoveToken!!))
                            delay(500)
                            onHistoryRemoved()
                            onDismiss()
                        }
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Delete from listening history",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "View information",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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