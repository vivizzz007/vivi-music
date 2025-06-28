package com.music.vivi.ui.menu

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.music.innertube.YouTube
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListItemHeight
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.db.entities.Event
import com.music.vivi.db.entities.PlaylistSong
import com.music.vivi.db.entities.Song
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.DownloadGridMenu
import com.music.vivi.ui.component.GridMenu
import com.music.vivi.ui.component.GridMenuItem
import com.music.vivi.ui.component.ListDialog
import com.music.vivi.ui.component.SongListItem
import com.music.vivi.ui.component.TextFieldDialog
import com.music.vivi.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.EnumMap
import kotlinx.coroutines.delay



@OptIn(ExperimentalMaterial3Api::class)
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
    val downloadUtil = LocalDownloadUtil.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by downloadUtil.getDownload(originalSong.id)
        .collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val cacheViewModel: CachePlaylistViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    // Sheet state management
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // States for dialogs and bottom sheets
    var showEditDialog by remember { mutableStateOf(false) }
    var showChoosePlaylistDialog by remember { mutableStateOf(false) }
    var showSelectArtistDialog by remember { mutableStateOf(false) }
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }

    // QR Code generation
    val shareLink = remember(song.id) {
        "https://music.youtube.com/watch?v=${song.id}"
    }
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

    LaunchedEffect(song.song.liked) {
        downloadUtil.autoDownloadIfLiked(song.song)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
        ) {
            // Song header
            SongListItem(
                song = song,
                showLikedIcon = false,
                badges = {},
                trailingContent = {
                    IconButton(
                        onClick = {
                            database.query {
                                update(song.song.toggleLike())
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(if (song.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                            tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            contentDescription = null
                        )
                    }
                }
            )

            Divider()

            // Menu items in grid layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Play Next
                item {
                    ActionButton(
                        icon = R.drawable.playlist_play,
                        label = stringResource(R.string.play_next),
                        onClick = {
                            onDismiss()
                            playerConnection.playNext(song.toMediaItem())
                        }
                    )
                }

                // Add to Queue
                item {
                    ActionButton(
                        icon = R.drawable.queue_music,
                        label = stringResource(R.string.add_to_queue),
                        onClick = {
                            onDismiss()
                            playerConnection.addToQueue((song.toMediaItem()))
                        }
                    )
                }

                // Start Radio (if not local)
                if (!song.song.isLocal) {
                    item {
                        ActionButton(
                            icon = R.drawable.radio,
                            label = stringResource(R.string.start_radio),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        )
                    }
                }

                // Library Toggle
                item {
                    ActionButton(
                        icon = if (song.song.inLibrary != null) R.drawable.library_add_check else R.drawable.library_add,
                        label = stringResource(if (song.song.inLibrary != null) R.string.remove_from_library else R.string.add_to_library),
                        onClick = {
                            database.query {
                                update(song.song.toggleLibrary())
                            }
                        }
                    )
                }

                // View Artist (if available)
                if (song.artists.isNotEmpty()) {
                    item {
                        ActionButton(
                            icon = R.drawable.artist,
                            label = stringResource(R.string.view_artist),
                            onClick = {
                                if (song.artists.size == 1) {
                                    navController.navigate("artist/${song.artists[0].id}")
                                    onDismiss()
                                } else {
                                    showSelectArtistDialog = true
                                }
                            }
                        )
                    }
                }

                // View Album (if available)
                if (song.song.albumId != null) {
                    item {
                        ActionButton(
                            icon = R.drawable.album,
                            label = stringResource(R.string.view_album),
                            onClick = {
                                navController.navigate("album/${song.song.albumId}")
                                onDismiss()
                            }
                        )
                    }
                }

                // Download
                item {
                    ActionButton(
                        icon = when (download?.state) {
                            Download.STATE_DOWNLOADING -> R.drawable.downloading_icon
                            Download.STATE_COMPLETED -> R.drawable.downloaded_icon
                            else -> R.drawable.download
                        },
                        label = when (download?.state) {
                            Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                            Download.STATE_COMPLETED -> stringResource(R.string.remove_download)
                            else -> stringResource(R.string.download)
                        },
                        onClick = {
                            when (download?.state) {
                                Download.STATE_COMPLETED -> {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false
                                    )
                                }
                                else -> {
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
                    )
                }

                // Share
                item {
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

                // Listen on YouTube Music (if not local)
                if (!song.song.isLocal) {
                    item {
                        ActionButton(
                            icon = R.drawable.music_note,
                            label = stringResource(R.string.listen_youtube_music),
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://music.youtube.com/watch?v=${song.id}".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Edit (if not local)
                if (!song.song.isLocal) {
                    item {
                        ActionButton(
                            icon = R.drawable.edit,
                            label = stringResource(R.string.edit),
                            onClick = { showEditDialog = true }
                        )
                    }
                }

                // Add to Playlist (if not local)
                if (!song.song.isLocal) {
                    item {
                        ActionButton(
                            icon = R.drawable.playlist_add,
                            label = stringResource(R.string.add_to_playlist),
                            onClick = { showChoosePlaylistDialog = true }
                        )
                    }
                }

                // Remove from Playlist/Cache/History
                item {
                    when {
                        playlistSong != null -> {
                            ActionButton(
                                icon = R.drawable.playlist_remove,
                                label = stringResource(R.string.remove_from_playlist),
                                onClick = {
                                    database.transaction {
                                        coroutineScope.launch {
                                            playlistBrowseId?.let { playlistId ->
                                                if (playlistSong.map.setVideoId != null) {
                                                    YouTube.removeFromPlaylist(
                                                        playlistId,
                                                        playlistSong.map.songId,
                                                        playlistSong.map.setVideoId
                                                    )
                                                }
                                            }
                                        }
                                        move(playlistSong.map.playlistId, playlistSong.map.position, Int.MAX_VALUE)
                                        delete(playlistSong.map.copy(position = Int.MAX_VALUE))
                                    }
                                    onDismiss()
                                }
                            )
                        }
                        isFromCache -> {
                            ActionButton(
                                icon = R.drawable.cached,
                                label = stringResource(R.string.remove_from_cache),
                                onClick = {
                                    onDismiss()
                                    cacheViewModel.removeSongFromCache(song.id)
                                }
                            )
                        }
                        event != null -> {
                            ActionButton(
                                icon = R.drawable.delete,
                                label = stringResource(R.string.remove_from_history),
                                onClick = {
                                    onDismiss()
                                    database.query {
                                        delete(event)
                                    }
                                }
                            )
                        }
                        else -> {
                            // Empty item to maintain grid alignment
                            Spacer(modifier = Modifier.size(0.dp))
                        }
                    }
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
    // QR Code Bottom Sheet
    // QR Code Bottom Sheet
    if (showQrCodeSheet) {
        var showSaveSuccess by remember { mutableStateOf(false) }
        var saveError by remember { mutableStateOf<String?>(null) }

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
                    text = song.song.title,
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

                // Button Row with Save on left and Cancel on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Save QR Code Button (left side)
                    Button(
                        onClick = {
                            if (qrCodeBitmap != null) {
                                try {
                                    val filename = "QR_${song.song.title.replace(" ", "_")}_${System.currentTimeMillis()}.png"
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/YouTubeMusicQR")
                                    }

                                    val resolver = context.contentResolver
                                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                                    uri?.let {
                                        resolver.openOutputStream(it)?.use { outputStream ->
                                            qrCodeBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                            showSaveSuccess = true
                                            // Auto-close after 1 second if successful
                                            scope.launch {
                                                delay(1000)
                                                showQrCodeSheet = false
                                                showShareOptionsSheet = false
                                                onDismiss()
                                            }
                                        }
                                    } ?: run {
                                        saveError = context.getString(R.string.failed_to_create_file)
                                    }
                                } catch (e: Exception) {
                                    saveError = e.localizedMessage ?: context.getString(R.string.failed_to_save_qr)
                                }
                            } else {
                                saveError = context.getString(R.string.qr_code_not_generated)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.save_qr_code))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Cancel Button (right side)
                    Button(
                        onClick = {
                            scope.launch {
                                showQrCodeSheet = false
                                showShareOptionsSheet = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.cancel))
                    }
                }

                // Show success/error messages
                if (showSaveSuccess) {
                    Text(
                        text = stringResource(R.string.qr_saved_successfully),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                saveError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_song)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue = TextFieldValue(
                song.song.title,
                TextRange(song.song.title.length)
            ),
            onDone = { title ->
                onDismiss()
                database.query {
                    update(song.song.copy(title = title))
                }
            }
        )
    }

    // Add to Playlist Dialog
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

    // Artist Selection Dialog
    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(
                items = song.artists,
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
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape)
                        )
                    }
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
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
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
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