package com.music.vivi.ui.menu

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import coil.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
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

import com.music.vivi.ui.component.DownloadGridMenu
import com.music.vivi.ui.component.GridMenu
import com.music.vivi.ui.component.GridMenuItem
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.ListItem
import com.music.vivi.utils.joinByBullet
import com.music.vivi.utils.makeTimeString
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

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.exoplayer.offline.Download

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val downloadUtil = LocalDownloadUtil.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val artists = remember {
        song.artists.mapNotNull {
            it.id?.let { artistId ->
                MediaMetadata.Artist(id = artistId, name = it.name)
            }
        }
    }

    // Sheet state management
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // States for dialogs and bottom sheets
    var showChoosePlaylistDialog by remember { mutableStateOf(false) }
    var showSelectArtistDialog by remember { mutableStateOf(false) }
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }

    // QR Code generation
    val shareLink = remember(song.id) { song.shareLink }
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

    LaunchedEffect(librarySong?.song?.liked) {
        librarySong?.let {
            downloadUtil.autoDownloadIfLiked(it.song)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // Song header
            ListItem(
                title = song.title,
                subtitle = joinByBullet(
                    song.artists.joinToString { it.name },
                    song.duration?.let { makeTimeString(it * 1000L) }
                ),
                thumbnailContent = {
                    AsyncImage(
                        model = song.thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            database.transaction {
                                librarySong.let { librarySong ->
                                    if (librarySong == null) {
                                        insert(song.toMediaMetadata(), SongEntity::toggleLike)
                                    } else {
                                        update(librarySong.song.toggleLike())
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(if (librarySong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                            tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            contentDescription = null
                        )
                    }
                }
            )

            Divider()

            // Menu items in grid layout
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Playback Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = R.drawable.radio,
                        label = stringResource(R.string.start_radio),
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            onDismiss()
                        }
                    )

                    ActionButton(
                        icon = R.drawable.playlist_play,
                        label = stringResource(R.string.play_next),
                        onClick = {
                            playerConnection.playNext(song.toMediaItem())
                            onDismiss()
                        }
                    )

                    ActionButton(
                        icon = R.drawable.queue_music,
                        label = stringResource(R.string.add_to_queue),
                        onClick = {
                            playerConnection.addToQueue((song.toMediaItem()))
                            onDismiss()
                        }
                    )
                }

                // Library Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (librarySong?.song?.inLibrary != null) {
                        ActionButton(
                            icon = R.drawable.library_add_check,
                            label = stringResource(R.string.remove_from_library),
                            onClick = {
                                database.query {
                                    inLibrary(song.id, null)
                                }
                            }
                        )
                    } else {
                        ActionButton(
                            icon = R.drawable.library_add,
                            label = stringResource(R.string.add_to_library),
                            onClick = {
                                database.transaction {
                                    insert(song.toMediaMetadata())
                                    inLibrary(song.id, LocalDateTime.now())
                                }
                            }
                        )
                    }

                    ActionButton(
                        icon = R.drawable.playlist_add,
                        label = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    )

                    DownloadActionButton(
                        state = download?.state,
                        onDownload = {
                            database.transaction {
                                insert(song.toMediaMetadata())
                            }
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
                        },
                        onRemoveDownload = {
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    )
                }

                // Navigation Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (artists.isNotEmpty()) {
                        ActionButton(
                            icon = R.drawable.artist,
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

                    song.album?.let { album ->
                        ActionButton(
                            icon = R.drawable.album,
                            label = stringResource(R.string.view_album),
                            onClick = {
                                navController.navigate("album/${album.id}")
                                onDismiss()
                            }
                        )
                    }

                    ActionButton(
                        icon = R.drawable.music_note,
                        label = stringResource(R.string.listen_youtube_music),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, song.shareLink.toUri())
                            context.startActivity(intent)
                        }
                    )
                }

                // Share Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = R.drawable.shares,
                        label = stringResource(R.string.share),
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                showShareOptionsSheet = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Share Options Bottom Sheet
    if (showShareOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    showShareOptionsSheet = false
                    sheetState.show()
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
                            scope.launch {
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
                            scope.launch {
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
                scope.launch {
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
                    text = song.title,
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
                        scope.launch {
                            showQrCodeSheet = false
                            showShareOptionsSheet = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    }

    // Add to Playlist Dialog
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

    // Artist Selection Dialog
    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(artists) { artist ->
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
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
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
    }
}

@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
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
                painter = painterResource(icon),
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
private fun DownloadActionButton(
    state: Int?, // Using ExoPlayer's Download.STATE_* constants
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (state) {
        Download.STATE_DOWNLOADING -> Pair(R.drawable.download, stringResource(R.string.downloading))
        Download.STATE_COMPLETED -> Pair(R.drawable.downloaded_icon, stringResource(R.string.remove_download))
        else -> Pair(R.drawable.download, stringResource(R.string.download))
    }

    ActionButton(
        icon = icon,
        label = label,
        onClick = {
            if (state == Download.STATE_COMPLETED) {
                onRemoveDownload()
            } else {
                onDownload()
            }
        },
        modifier = modifier
    )
}