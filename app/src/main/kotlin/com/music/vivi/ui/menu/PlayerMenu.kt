package com.music.vivi.ui.menu

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
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
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

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

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

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

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    NewMenuContainer(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        if (isQueueTrigger != true) {
            Android16VolumeSlider(
                volume = playerVolume.value,
                onVolumeChange = { playerConnection.service.playerVolume.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }

        NewActionGrid(
            columns = 3,
            actions = listOf(
                NewAction(
                    icon = { Icon(painterResource(R.drawable.radio), null) },
                    text = stringResource(R.string.start_radio),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.starting_radio), Toast.LENGTH_SHORT).show()
                        playerConnection.startRadioSeamlessly()
                        onDismiss()
                    },
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                NewAction(
                    icon = {
                        Icon(
                            painter = painterResource(
                                when (download?.state) {
                                    Download.STATE_COMPLETED -> R.drawable.offline
                                    else -> R.drawable.download
                                }
                            ),
                            contentDescription = null
                        )
                    },
                    text = when (download?.state) {
                        Download.STATE_COMPLETED -> stringResource(R.string.remove_download)
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                        else -> stringResource(R.string.action_download)
                    },
                    onClick = {
                        when (download?.state) {
                            Download.STATE_COMPLETED, Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, mediaMetadata.id, false)
                            }
                            else -> {
                                database.transaction { insert(mediaMetadata) }
                                val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                    .setCustomCacheKey(mediaMetadata.id)
                                    .setData(mediaMetadata.title.toByteArray())
                                    .build()
                                DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                            }
                        }
                    },
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                NewAction(
                    icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    text = stringResource(R.string.add_to_playlist),
                    onClick = { showChoosePlaylistDialog = true },
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                NewAction(
                    icon = { Icon(painterResource(R.drawable.link), null) },
                    text = stringResource(R.string.copy_link),
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Song Link", "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                NewAction(
                    icon = { Icon(painterResource(R.drawable.share), null) },
                    text = stringResource(R.string.share),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} - https://music.youtube.com/watch?v=${mediaMetadata.id}")
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                        onDismiss()
                    },
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Unified Actions Group (More Content + Settings)
        val hasMoreContent = artists.isNotEmpty() || mediaMetadata.album != null
        if (hasMoreContent) {
            NewMenuSectionHeader(text = stringResource(R.string.more_content))
        }

        // List of all possible items in this group
        val isSettingsOnly = !hasMoreContent
        val hasEqualizer = isQueueTrigger != true
        
        // Artist
        if (artists.isNotEmpty()) {
            val isTop = true
            val isBottom = false // Album or Details follows
            NewMenuItem(
                headlineContent = { Text(stringResource(R.string.view_artist)) },
                supportingContent = { Text("View artist page") },
                leadingContent = { Icon(painterResource(R.drawable.artist), null) },
                position = MenuGroupPosition.Top,
                onClick = {
                    if (mediaMetadata.artists.size == 1) {
                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                        playerBottomSheetState.collapseSoft()
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Album
        if (mediaMetadata.album != null) {
            val isTop = !artists.isNotEmpty()
            NewMenuItem(
                headlineContent = { Text(stringResource(R.string.view_album)) },
                supportingContent = { Text("View full album") },
                leadingContent = { Icon(painterResource(R.drawable.album), null) },
                position = if (isTop) MenuGroupPosition.Top else MenuGroupPosition.Middle,
                onClick = {
                    navController.navigate("album/${mediaMetadata.album.id}")
                    playerBottomSheetState.collapseSoft()
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Details
        val isDetailsTop = !hasMoreContent
        val isDetailsBottom = isQueueTrigger == true
        NewMenuItem(
            headlineContent = { Text(stringResource(R.string.details)) },
            supportingContent = { Text("View information") },
            leadingContent = { Icon(painterResource(R.drawable.info), null) },
            position = when {
                isDetailsTop && isDetailsBottom -> MenuGroupPosition.Single
                isDetailsTop -> MenuGroupPosition.Top
                isDetailsBottom -> MenuGroupPosition.Bottom
                else -> MenuGroupPosition.Middle
            },
            onClick = {
                onShowDetailsDialog()
                onDismiss()
            }
        )

        if (isQueueTrigger != true) {
            Spacer(modifier = Modifier.height(2.dp))
            
            // Equalizer
            NewMenuItem(
                headlineContent = { Text(stringResource(R.string.equalizer)) },
                supportingContent = { Text("Audio settings") },
                leadingContent = { Icon(painterResource(R.drawable.equalizer), null) },
                position = MenuGroupPosition.Middle,
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
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Advanced
            NewMenuItem(
                headlineContent = { Text(stringResource(R.string.advanced)) },
                supportingContent = { Text("Pitch and tempo") },
                leadingContent = { Icon(painterResource(R.drawable.tune), null) },
                position = MenuGroupPosition.Bottom,
                onClick = { showPitchTempoDialog = true }
            )
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