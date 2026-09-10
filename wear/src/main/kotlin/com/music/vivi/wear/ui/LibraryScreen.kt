/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.music.vivi.db.entities.Song
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.playback.WearPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Library/Downloads screen for Wear OS.
 *
 * Displays songs available offline on the watch using [ScalingLazyColumn].
 * True-black AMOLED background.
 */
@Composable
fun LibraryScreen(
    mediaState: MediaControllerState,
    onNavigateBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val database = remember { WearApp.instance.database }

    // Observe downloaded songs from Room database sorted by download date
    val downloadedSongs by remember {
        database.downloadedSongsByCreateDateAsc()
    }.collectAsState(initial = emptyList())

    val listState = rememberScalingLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (downloadedSongs.isEmpty()) {
            EmptyLibraryContent()
        } else {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.library),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                items(
                    items = downloadedSongs,
                    key = { it.song.id },
                ) { song ->
                    SongItem(
                        song = song,
                        onClick = {
                            scope.launch(Dispatchers.Main) {
                                val controller = mediaState.controller ?: return@launch
                                val mediaItem = WearPlaybackService.buildMediaItem(
                                    songId = song.song.id,
                                    title = song.song.title,
                                    artist = song.artists.firstOrNull()?.name,
                                    artworkUri = song.song.thumbnailUrl?.toUri(),
                                )
                                controller.setMediaItem(mediaItem)
                                controller.prepare()
                                controller.play()
                                onNavigateBack()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                text = song.song.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artistName = song.artists.firstOrNull()?.name
            if (artistName != null) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.no_downloads),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.no_downloads_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
