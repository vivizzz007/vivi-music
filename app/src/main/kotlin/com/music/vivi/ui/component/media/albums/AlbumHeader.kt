package com.music.vivi.ui.component.media.albums

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.AlbumWithSongs
import com.music.vivi.playback.ExoDownloadService
import com.music.vivi.playback.queues.LocalAlbumRadio
import com.music.vivi.ui.menu.AlbumMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumHeader(
    albumWithSongs: AlbumWithSongs,
    downloadState: Int,
    playlistId: String?,
    albumDescription: String?,
    isDescriptionLoading: Boolean,
    navController: NavController,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val menuState = com.music.vivi.ui.component.LocalMenuState.current

    // Check if any song is explicit
    val hasExplicitContent = remember(albumWithSongs) {
        albumWithSongs.songs.any { it.song.explicit }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(50.dp)) // Space for top app bar

        // Album Artwork - Large and centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            AsyncImage(
                model = albumWithSongs.album.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(32.dp))

        // Album Title with Logo Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.new_album_vivi), // Use your app icon
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = albumWithSongs.album.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        // Action Buttons Row (Save, Play, Share)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save/Like Button
            Button(
                onClick = {
                    database.query {
                        update(albumWithSongs.album.toggleLike())
                    }
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            if (albumWithSongs.album.bookmarkedAt != null) {
                                R.drawable.favorite
                            } else {
                                R.drawable.favorite_border
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (albumWithSongs.album.bookmarkedAt != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (albumWithSongs.album.bookmarkedAt !=
                            null
                        ) {
                            stringResource(R.string.saved)
                        } else {
                            stringResource(R.string.save)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Play Button
            Button(
                onClick = {
                    if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                        playerConnection.player.pause()
                    } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                        playerConnection.player.play()
                    } else {
                        playerConnection.service.getAutomix(playlistId ?: "")
                        playerConnection.playQueue(
                            LocalAlbumRadio(albumWithSongs)
                        )
                    }
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                R.drawable.pause
                            } else {
                                R.drawable.play
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                            stringResource(R.string.pause)
                        } else {
                            stringResource(R.string.play)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            // Share Button
            Surface(
                onClick = {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            context.getString(
                                R.string.check_out_album_share,
                                albumWithSongs.album.title,
                                albumWithSongs.artists.joinToString {
                                    it.name
                                },
                                "https://music.youtube.com/playlist?list=${albumWithSongs.album.playlistId}"
                            )
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = stringResource(R.string.share_album),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Explicit Label
        if (hasExplicitContent) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.explicit),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Album Info
        Text(
            text = buildString {
                append(stringResource(R.string.album_text))
                if (albumWithSongs.album.year != null) {
                    append(" • ${albumWithSongs.album.year}")
                }
                append(" • ${albumWithSongs.songs.size} Tracks")
                val totalDuration = albumWithSongs.songs.sumOf { it.song.duration ?: 0 }
                val hours = totalDuration / 3600
                val minutes = (totalDuration % 3600) / 60
                if (hours > 0) {
                    append(" • ${hours}h ${minutes}m")
                } else {
                    append(" • ${minutes}m")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Album Description
        var showDescriptionDialog by rememberSaveable { mutableStateOf(false) }
        var isDescriptionTruncated by remember { mutableStateOf(false) }
        val staticDescription = "${albumWithSongs.album.title} is an album by ${albumWithSongs.artists.joinToString {
            it.name
        }}${
            if (albumWithSongs.album.year != null) ", released in ${albumWithSongs.album.year}" else ""
        }. This collection features ${albumWithSongs.songs.size} tracks showcasing their musical artistry."
        val description = albumDescription ?: staticDescription

        if (albumDescription == null && isDescriptionLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .combinedClickable(
                        onClick = {
                            if (isDescriptionTruncated) {
                                showDescriptionDialog = true
                            }
                        }
                    ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    isDescriptionTruncated = textLayoutResult.hasVisualOverflow
                }
            )
        }

        if (showDescriptionDialog) {
            AlertDialog(
                onDismissRequest = { showDescriptionDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = albumWithSongs.album.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = albumWithSongs.album.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                text = {
                    LazyColumn {
                        item {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDescriptionDialog = false },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Artist Names (clickable)
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.by_text))
                albumWithSongs.artists.forEachIndexed { index, artist ->
                    val link = LinkAnnotation.Clickable(artist.id) {
                        navController.navigate("artist/${artist.id}")
                    }
                    withLink(link) {
                        append(artist.name)
                    }
                    if (index != albumWithSongs.artists.lastIndex) {
                        append(", ")
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // Additional action buttons (Download, Shuffle, More)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            ToggleButton(
                checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                onCheckedChange = {
                    when (downloadState) {
                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                            albumWithSongs.songs.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false
                                )
                            }
                        }
                        else -> {
                            albumWithSongs.songs.forEach { song ->
                                val downloadRequest =
                                    DownloadRequest
                                        .Builder(song.id, song.id.toUri())
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
                },
                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
            ) {
                when (downloadState) {
                    Download.STATE_COMPLETED -> {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = stringResource(R.string.saved),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Download.STATE_DOWNLOADING -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = stringResource(R.string.save),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(
                    text = when (downloadState) {
                        Download.STATE_COMPLETED -> stringResource(R.string.saved)
                        Download.STATE_DOWNLOADING -> stringResource(R.string.saving)
                        else -> stringResource(R.string.save)
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Shuffle Button
            ToggleButton(
                checked = false,
                onCheckedChange = {
                    playerConnection.service.getAutomix(playlistId ?: "")
                    playerConnection.playQueue(
                        LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled()))
                    )
                },
                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.shuffle_content_desc),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(stringResource(R.string.shuffle_label), style = MaterialTheme.typography.labelMedium)
            }

            // More Options Button
            ToggleButton(
                checked = false,
                onCheckedChange = {
                    menuState.show {
                        AlbumMenu(
                            originalAlbum = Album(
                                albumWithSongs.album,
                                albumWithSongs.artists
                            ),
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                    }
                },
                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.more_options),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(stringResource(R.string.more_label), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
