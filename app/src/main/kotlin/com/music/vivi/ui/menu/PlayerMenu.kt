package com.music.vivi.ui.menu

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
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
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.ui.component.Android16VolumeSlider
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.MenuGroupPosition
import com.music.vivi.ui.component.NewAction
import com.music.vivi.ui.component.NewActionGrid
import com.music.vivi.ui.component.NewMenuContainer
import com.music.vivi.ui.component.NewMenuItem
import com.music.vivi.ui.component.NewMenuSectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val songState = database.song(mediaMetadata.id).collectAsState(initial = null)
    val song = songState.value?.song
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    // State management
    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }
    var showPitchTempoDialog by rememberSaveable { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                    Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
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

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    // Design variables - Android 16 style
    val cornerRadius = 24.dp
    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 60, cornerRadiusTL = cornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius, smoothnessAsPercentTR = 60
    )
    val playButtonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 60, cornerRadiusTL = cornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius, smoothnessAsPercentTR = 60
    )

    // Android 16 grouped shapes
    val topShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius, smoothnessAsPercentBR = 0, cornerRadiusBR = 0.dp,
        smoothnessAsPercentTL = 60, cornerRadiusTL = cornerRadius, smoothnessAsPercentBL = 0,
        cornerRadiusBL = 0.dp, smoothnessAsPercentTR = 60
    )
    val middleShape = RectangleShape
    val bottomShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 0.dp, smoothnessAsPercentBR = 60, cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 0, cornerRadiusTL = 0.dp, smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius, smoothnessAsPercentTR = 0
    )
    val singleShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 60, cornerRadiusTL = cornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius, smoothnessAsPercentTR = 60
    )

    // Favorite state tracking
    val isFavorite = song?.liked == true
    val favoriteButtonCornerRadius by animateDpAsState(
        targetValue = if (isFavorite) cornerRadius else 60.dp,
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

        // Header Row - Song Art and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
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
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
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
                    val currentSong = song ?: mediaMetadata.toSongEntity()
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

        if (isQueueTrigger != true) {
            Android16VolumeSlider(
                volume = playerVolume.value,
                onVolumeChange = { playerConnection.service.playerVolume.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start Radio Button (Primary Action)
            MediumExtendedFloatingActionButton(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                onClick = {
                    Toast.makeText(context, context.getString(R.string.starting_radio), Toast.LENGTH_SHORT).show()
                    playerConnection.startRadioSeamlessly()
                    onDismiss()
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = playButtonShape,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = "Start Radio"
                    )
                },
                text = {
                    Text(
                        modifier = Modifier.padding(end = 10.dp),
                        text = stringResource(R.string.radio),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            )

            // Share Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} - https://music.youtube.com/watch?v=${mediaMetadata.id}")
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                    onDismiss()
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.share),
                    contentDescription = stringResource(R.string.share)
                )
            }

            // Copy Link Button
            FilledTonalIconButton(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Song Link", "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = singleShape
            ) {
                Icon(
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                    painter = painterResource(R.drawable.link),
                    contentDescription = stringResource(R.string.copy_link)
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
                when (download?.state) {
                    Download.STATE_COMPLETED, Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                        DownloadService.sendRemoveDownload(
                            context, ExoDownloadService::class.java, mediaMetadata.id, false
                        )
                    }
                    else -> {
                        database.transaction { insert(mediaMetadata) }
                        val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                            .setCustomCacheKey(mediaMetadata.id)
                            .setData(mediaMetadata.title.toByteArray())
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
                    Download.STATE_COMPLETED -> stringResource(R.string.remove_download)
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                    else -> stringResource(R.string.action_download)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content Group (Artist, Album)
        val hasArtist = artists.isNotEmpty()
        val hasAlbum = mediaMetadata.album != null

        if (hasArtist || hasAlbum) {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Artist
                if (hasArtist) {
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp),
                        shape = if (hasAlbum) topShape else singleShape,
                        onClick = {
                            if (mediaMetadata.artists.size == 1) {
                                navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            } else {
                                showSelectArtistDialog = true
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = stringResource(R.string.view_artist)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.view_artist),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "View artist page",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (hasArtist && hasAlbum) {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // Album
                if (hasAlbum) {
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp),
                        shape = if (hasArtist) bottomShape else singleShape,
                        onClick = {
                            navController.navigate("album/${mediaMetadata.album!!.id}")
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.album),
                            contentDescription = stringResource(R.string.view_album)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.view_album),
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
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Actions Group
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val actions = mutableListOf<@Composable (shape: androidx.compose.ui.graphics.Shape) -> Unit>()

            // Add to Playlist
            actions.add { shape ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = shape,
                    onClick = { showChoosePlaylistDialog = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = stringResource(R.string.add_to_playlist)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.add_to_playlist),
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
            }

            // Equalizer (if available and not queue trigger)
            if (isQueueTrigger != true) {
                actions.add { shape ->
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp),
                        shape = shape,
                        onClick = {
                            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playerConnection.player.audioSessionId)
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                activityResultLauncher.launch(intent)
                            }
                            onDismiss()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.equalizer),
                            contentDescription = stringResource(R.string.equalizer)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.equalizer),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Audio settings",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Pitch and Tempo
                actions.add { shape ->
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp),
                        shape = shape,
                        onClick = { showPitchTempoDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tune),
                            contentDescription = stringResource(R.string.advanced)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.advanced),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Pitch and tempo",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Details
            actions.add { shape ->
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 66.dp),
                    shape = shape,
                    onClick = {
                        onShowDetailsDialog()
                        onDismiss()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = stringResource(R.string.details)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.details),
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

            actions.forEachIndexed { index, action ->
                val shape = when {
                    actions.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == actions.lastIndex -> bottomShape
                    else -> middleShape
                }
                action(shape)
                if (index < actions.lastIndex) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.speed,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        tempo = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}