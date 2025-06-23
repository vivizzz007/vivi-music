@file:Suppress("NAME_SHADOWING")

package com.music.vivi.ui.menu

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.PlayerSliderTrack
//import com.music.vivi.ui.component.SleepTimerGridMenu
import com.music.vivi.utils.rememberEnumPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.alpha

// Compose Foundation
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap

// Animation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap


@SuppressLint("ConfigurationScreenWidthHeight", "StringFormatInvalid")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    bottomSheetState: BottomSheetState,
    onShowDetailsDialog: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val activityResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val download by downloadUtil.getDownload(mediaMetadata.id).collectAsState(initial = null)
    val artists = remember(mediaMetadata.artists) { mediaMetadata.artists.filter { it.id != null } }
    val snackbarHostState = remember { SnackbarHostState() }

    // States for dialogs and bottom sheets
    var showTempoPitchDialog by remember { mutableStateOf(false) }
    var showChoosePlaylistDialog by remember { mutableStateOf(false) }
    var showSelectArtistDialog by remember { mutableStateOf(false) }
    val modalBottomSheetState = rememberModalBottomSheetState()
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }

    // Generate share link and QR content
    val shareLink = remember(mediaMetadata.id) {
        "https://music.youtube.com/watch?v=${mediaMetadata.id}"
    }
    val qrCodeContent = remember(mediaMetadata) {
        // Format: "Song Title|ShareLink" for easy parsing
        "${mediaMetadata.title}|$shareLink"
    }

    // QR Code Bitmap with song name included
    val qrCodeBitmap = remember(qrCodeContent) {
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = QRCodeWriter().encode(
                qrCodeContent,
                BarcodeFormat.QR_CODE,
                512,
                512,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
            }.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    // For bottom sheet height
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetHeight = remember { screenHeight * 0.55f }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
        ) {
            // Volume Control Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (playerVolume.value == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Slider(
                        value = playerVolume.value,
                        onValueChange = { playerConnection.service.playerVolume.value = it },
                        modifier = Modifier.weight(1f),
                        valueRange = 0f..1f
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${(playerVolume.value * 100).toInt()}%",
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Divider()

            // Main Actions Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // First Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Download Button
                    DownloadButton(
                        state = download?.state,
                        onDownload = {
                            database.transaction { insert(mediaMetadata) }
                            val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                .setCustomCacheKey(mediaMetadata.id)
                                .setData(mediaMetadata.title.toByteArray())
                                .build()
                            DownloadService.sendAddDownload(
                                context,
                                ExoDownloadService::class.java,
                                downloadRequest,
                                false
                            )
                        },
                        onRemoveDownload = {
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                mediaMetadata.id,
                                false
                            )
                        }
                    )

                    // Radio Button
                    ActionButton(
                        icon = Icons.Default.Radio,
                        label = stringResource(R.string.start_radio),
                        onClick = {
                            playerConnection.service.startRadioSeamlessly()
                            onDismiss()
                        }
                    )

                    // Add to Playlist Button
                    ActionButton(
                        icon = Icons.Default.PlaylistAdd,
                        label = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    )
                }

                // Second Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Library Toggle
                    ActionButton(
                        icon = if (librarySong?.song?.inLibrary != null) Icons.Default.LibraryAddCheck else Icons.Default.LibraryAdd,
                        label = stringResource(if (librarySong?.song?.inLibrary != null) R.string.remove_from_library else R.string.add_to_library),
                        onClick = {
                            if (librarySong?.song?.inLibrary != null) {
                                database.query { inLibrary(mediaMetadata.id, null) }
                            } else {
                                database.transaction {
                                    insert(mediaMetadata)
                                    inLibrary(mediaMetadata.id, LocalDateTime.now())
                                }
                            }
                        }
                    )

                    // View Artist
                    if (artists.isNotEmpty()) {
                        ActionButton(
                            icon = Icons.Default.Person,
                            label = stringResource(R.string.view_artist),
                            onClick = {
                                if (artists.size == 1) {
                                    navController.navigate("artist/${artists[0].id}")
                                    onDismiss()
                                } else {
                                    showSelectArtistDialog = true
                                }
                            }
                        )
                    }

                    // View Album
                    if (mediaMetadata.album != null) {
                        ActionButton(
                            icon = Icons.Default.Album,
                            label = stringResource(R.string.view_album),
                            onClick = {
                                navController.navigate("album/${mediaMetadata.album.id}")
                                onDismiss()
                            }
                        )
                    }
                }

                // Third Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Share Button
                    ActionButton(
                        icon = Icons.Default.Share,
                        label = stringResource(R.string.share),
                        onClick = { showShareOptionsSheet = true }
                    )

                    // Details Button
                    ActionButton(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.details),
                        onClick = {
                            onShowDetailsDialog()
                            onDismiss()
                        }
                    )

                    // Equalizer Button
                    ActionButton(
                        icon = Icons.Default.Equalizer,
                        label = stringResource(R.string.equalizer),
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
                }

                // Fourth Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Tempo/Pitch Button
                    ActionButton(
                        icon = Icons.Default.Speed,
                        label = stringResource(R.string.tempo_and_pitch),
                        onClick = { showTempoPitchDialog = true }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Share Options Bottom Sheet
    if (showShareOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareOptionsSheet = false },
            sheetState = modalBottomSheetState
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.share_song),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                // Share via link
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, qrCodeContent) // Now includes name
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                            showShareOptionsSheet = false
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.share_link))
                }

                Divider()

                // Share via QR code
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showShareOptionsSheet = false
                            showQrCodeSheet = true
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.share_qr_code))
                }
            }
        }
    }

    // QR Code Bottom Sheet
    if (showQrCodeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    modalBottomSheetState.hide()
                    showQrCodeSheet = false
                }
            },
            sheetState = modalBottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.scan_qr_to_play),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // QR Code Container
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrCodeBitmap != null) {
                        Image(
                            bitmap = qrCodeBitmap,
                            contentDescription = stringResource(R.string.qr_code),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = shareLink,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                            showQrCodeSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(text = stringResource(R.string.close))
                }
            }
        }
    }

    // Add to Playlist Dialog
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch {
                try {
                    playlist.playlist.browseId?.let { browseId ->
                        val result = withContext(Dispatchers.IO) {
                            YouTube.addToPlaylist(browseId, mediaMetadata.id)
                        }

                        result.fold(
                            onSuccess = { response ->
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.added_to_playlist, playlist.playlist.name),
                                    duration = SnackbarDuration.Short
                                )
                            },
                            onFailure = { e ->
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.failed_to_add_to_playlist),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        )
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.failed_to_add_to_playlist),
                        duration = SnackbarDuration.Short
                    )
                } finally {
                    showChoosePlaylistDialog = false
                }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    // Artist Selection Dialog
    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(56.dp)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            bottomSheetState.collapseSoft()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
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

    // Tempo/Pitch Dialog
    if (showTempoPitchDialog) {
        TempoPitchDialog(onDismiss = { showTempoPitchDialog = false })
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DownloadButton(
    state: Int?, // Using ExoPlayer's Download.STATE_* constants
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (state) {
        Download.STATE_DOWNLOADING -> Pair(Icons.Default.Pause, stringResource(R.string.downloading))
        Download.STATE_COMPLETED -> Pair(Icons.Default.Delete, stringResource(R.string.remove_download))
        else -> Pair(Icons.Default.Download, stringResource(R.string.download))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        IconButton(
            onClick = {
                if (state == Download.STATE_COMPLETED) {
                    onRemoveDownload()
                } else {
                    onDownload()
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TempoPitchDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters = PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
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
                }
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
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
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" }
                )
            }
        }
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
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null
            )
        }
    }
}