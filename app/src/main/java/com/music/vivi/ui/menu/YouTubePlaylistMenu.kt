
package com.music.vivi.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.utils.completed
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.extensions.toMediaItem
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.playback.queues.YouTubeQueue
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.GridMenu
import com.music.vivi.ui.component.GridMenuItem
import com.music.vivi.ui.component.YouTubeListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri


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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    // States for dialogs and bottom sheets
    var showChoosePlaylistDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showShareOptionsSheet by remember { mutableStateOf(false) }
    var showQrCodeSheet by remember { mutableStateOf(false) }

    // Sheet state management
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // QR Code generation
    val shareLink = remember(playlist.id) { playlist.shareLink }
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
            // Playlist header
            YouTubeListItem(
                item = playlist,
                trailingContent = {
                    if (playlist.id != "LM" && !playlist.isEditable) {
                        IconButton(
                            onClick = {
                                if (dbPlaylist?.playlist == null) {
                                    database.transaction {
                                        val playlistEntity = PlaylistEntity(
                                            name = playlist.title,
                                            browseId = playlist.id,
                                            isEditable = false,
                                            remoteSongCount = playlist.songCountText?.let {
                                                Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                            },
                                            playEndpointParams = playlist.playEndpoint?.params,
                                            shuffleEndpointParams = playlist.shuffleEndpoint.params,
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
                                        update(dbPlaylist!!.playlist.toggleLike())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                                tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error
                                else LocalContentColor.current,
                                contentDescription = null
                            )
                        }
                    }
                }
            )

            Divider()

            // Menu items in grid layout
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // First Row - Playback Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Play
                    playlist.playEndpoint?.let {
                        ActionButton(
                            icon = R.drawable.play,
                            label = stringResource(R.string.play),
                            onClick = {
                                playerConnection.playQueue(YouTubeQueue(it))
                                onDismiss()
                            }
                        )
                    }

                    // Shuffle
                    ActionButton(
                        icon = R.drawable.shuffle,
                        label = stringResource(R.string.shuffle),
                        onClick = {
                            playerConnection.playQueue(YouTubeQueue(playlist.shuffleEndpoint))
                            onDismiss()
                        }
                    )

                    // Radio (if available)
                    playlist.radioEndpoint?.let {
                        ActionButton(
                            icon = R.drawable.radio,
                            label = stringResource(R.string.start_radio),
                            onClick = {
                                playerConnection.playQueue(YouTubeQueue(it))
                                onDismiss()
                            }
                        )
                    }
                }

                // Second Row - Queue Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Play Next
                    ActionButton(
                        icon = R.drawable.playlist_play,
                        label = stringResource(R.string.play_next),
                        onClick = {
                            coroutineScope.launch {
                                songs.ifEmpty {
                                    withContext(Dispatchers.IO) {
                                        YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                                    }
                                }.let { songs ->
                                    playerConnection.playNext(songs.map { it.toMediaItem() })
                                }
                            }
                            onDismiss()
                        }
                    )

                    // Add to Queue
                    ActionButton(
                        icon = R.drawable.queue_music,
                        label = stringResource(R.string.add_to_queue),
                        onClick = {
                            coroutineScope.launch {
                                songs.ifEmpty {
                                    withContext(Dispatchers.IO) {
                                        YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                                    }
                                }.let { songs ->
                                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                }
                            }
                            onDismiss()
                        }
                    )

                    // Add to Playlist
                    ActionButton(
                        icon = R.drawable.playlist_add,
                        label = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    )
                }

                // Third Row - Other Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Listen on YouTube Music
                    ActionButton(
                        icon = R.drawable.music_note,
                        label = stringResource(R.string.listen_youtube_music),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, playlist.shareLink.toUri())
                            context.startActivity(intent)
                        }
                    )

                    // Share (if not "LM" playlist)
                    if (playlist.id != "LM") {
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
                    text = stringResource(R.string.share_playlist),
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
                    text = playlist.title,
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

                // Add Cancel button
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
            coroutineScope.launch(Dispatchers.IO) {
                targetPlaylist.playlist.browseId?.let { playlistId ->
                    YouTube.addPlaylistToPlaylist(playlistId, targetPlaylist.id)
                }
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    // Delete Playlist Dialog
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            deletePlaylistById(playlist.id)
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
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