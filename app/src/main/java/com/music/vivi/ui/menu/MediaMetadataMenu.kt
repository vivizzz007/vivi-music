package com.music.vivi.ui.menu


import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.ui.component.BottomSheetState
import com.music.vivi.ui.component.DownloadGridMenu
import com.music.vivi.ui.component.GridMenu
import com.music.vivi.ui.component.GridMenuItem
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.MediaMetadataListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime




import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor

import androidx.compose.material3.ModalBottomSheet

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange

import androidx.compose.ui.text.style.TextAlign

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMetadataMenu(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    bottomSheetState: BottomSheetState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)

    val artists = remember(mediaMetadata.artists) {
        mediaMetadata.artists.filter { it.id != null }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // States for share bottom sheets
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }
    val shareLink = remember { "https://music.youtube.com/watch?v=${mediaMetadata.id}" }

    // QR Code generation
    val qrCodeBitmap = remember(shareLink) {
        try {
            val bitMatrix = QRCodeWriter().encode(
                shareLink,
                BarcodeFormat.QR_CODE,
                512,
                512
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

    val coroutineScope = rememberCoroutineScope()

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, mediaMetadata.id)
                }
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
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
        ) {
            MediaMetadataListItem(
                mediaMetadata = mediaMetadata,
                badges = {},
                trailingContent = {
                    val song by database.song(mediaMetadata.id).collectAsState(initial = null)

                    IconButton(
                        onClick = {
                            database.query {
                                val currentSong = song
                                if (currentSong == null) {
                                    insert(mediaMetadata, SongEntity::toggleLike)
                                } else {
                                    update(currentSong.song.toggleLike())
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(if (song?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                            tint = if (song?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            contentDescription = null
                        )
                    }
                }
            )

            HorizontalDivider()

            GridMenu(
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                )
            ) {
                GridMenuItem(
                    icon = R.drawable.radio,
                    title = R.string.start_radio
                ) {
                    playerConnection.service.startRadioSeamlessly()
                    onDismiss()
                }
                GridMenuItem(
                    icon = R.drawable.playlist_play,
                    title = R.string.play_next
                ) {
                    onDismiss()
                    playerConnection.playNext(mediaMetadata.toMediaItem())
                }
                GridMenuItem(
                    icon = R.drawable.queue_music,
                    title = R.string.add_to_queue
                ) {
                    onDismiss()
                    playerConnection.addToQueue(mediaMetadata.toMediaItem())
                }
                GridMenuItem(
                    icon = R.drawable.playlist_add,
                    title = R.string.add_to_playlist
                ) {
                    showChoosePlaylistDialog = true
                }
                DownloadGridMenu(
                    state = download?.state,
                    onDownload = {
                        database.transaction {
                            insert(mediaMetadata)
                        }
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
                if (librarySong?.song?.inLibrary != null) {
                    GridMenuItem(
                        icon = R.drawable.library_add_check,
                        title = R.string.remove_from_library,
                    ) {
                        database.query {
                            inLibrary(mediaMetadata.id, null)
                        }
                    }
                } else {
                    GridMenuItem(
                        icon = R.drawable.library_add,
                        title = R.string.add_to_library,
                    ) {
                        database.transaction {
                            insert(mediaMetadata)
                            inLibrary(mediaMetadata.id, LocalDateTime.now())
                        }
                    }
                }
                if (artists.isNotEmpty()) {
                    GridMenuItem(
                        icon = R.drawable.artist,
                        title = R.string.view_artist
                    ) {
                        if (artists.size == 1) {
                            navController.navigate("artist/${artists[0].id}")
                            bottomSheetState.collapseSoft()
                            onDismiss()
                        } else {
                            showSelectArtistDialog = true
                        }
                    }
                }
                if (mediaMetadata.album != null) {
                    GridMenuItem(
                        icon = R.drawable.album,
                        title = R.string.view_album
                    ) {
                        navController.navigate("album/${mediaMetadata.album.id}")
                        bottomSheetState.collapseSoft()
                        onDismiss()
                    }
                }
                GridMenuItem(
                    icon = R.drawable.shares,
                    title = R.string.share
                ) {
                    coroutineScope.launch {
                        showShareOptionsSheet = true
                    }
                }
            }
        }
    }

    // Share Options Bottom Sheet
    if (showShareOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    showShareOptionsSheet = false
                }
            }
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
                            coroutineScope.launch {
                                showShareOptionsSheet = false
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareLink)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                                onDismiss()
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.link_icon),
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
                            coroutineScope.launch {
                                showShareOptionsSheet = false
                                showQrCodeSheet = true
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.qr_code_icon),
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
                    showQrCodeSheet = false
                    showShareOptionsSheet = true
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.share_via_qr),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap,
                        contentDescription = stringResource(R.string.qr_code),
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = shareLink,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            showQrCodeSheet = false
                            showShareOptionsSheet = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(text = stringResource(R.string.cancels))
                }
            }
        }
    }
}