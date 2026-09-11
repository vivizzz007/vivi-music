/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.SongItem
import com.music.vivi.models.MediaMetadata
import com.music.vivi.utils.resize
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.playback.WearPlaybackService
import com.music.vivi.wear.ui.components.DownloadIconButton
import com.music.vivi.wear.viewmodels.PlaylistDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Playlist and Album detail screen for Wear OS.
 *
 * Displays playlist metadata, Play All and Download All actions, and a scrollable list
 * of tracks. Supports rotary crown input and AMOLED true-black background.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    title: String = "",
    playlistTitle: String = title,
    mediaState: MediaControllerState,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit = onNavigateBack,
    viewModel: PlaylistDetailViewModel = viewModel(
        key = playlistId,
        factory = PlaylistDetailViewModel.provideFactory(playlistId),
    ),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    val effectiveTitle = state.playlist?.title?.takeIf { it.isNotBlank() }
        ?: playlistTitle.takeIf { it.isNotBlank() }
        ?: title.takeIf { it.isNotBlank() }
        ?: "Playlist"

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun playAtIndex(index: Int) {
        scope.launch(Dispatchers.Main) {
            val controller = mediaState.controller ?: return@launch
            val mediaItems = state.songs.map { song ->
                WearPlaybackService.buildMediaItem(
                    songId = song.id,
                    title = song.title,
                    artist = song.artists.firstOrNull()?.name,
                    artworkUri = song.thumbnail.takeIf { it.isNotBlank() }?.toUri(),
                )
            }
            controller.setMediaItems(mediaItems, index, 0L)
            controller.prepare()
            controller.play()
            onNavigateToNowPlaying()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading && state.songs.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            state.error != null && state.songs.isEmpty() -> {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.error ?: "Failed to load playlist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.loadPlaylist() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            else -> {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onRotaryScrollEvent { event ->
                            scope.launch {
                                listState.scrollBy(event.verticalScrollPixels)
                            }
                            true
                        },
                ) {
                    // Header: Playlist / Album Title
                    item {
                        ListHeader {
                            Text(
                                text = effectiveTitle,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Author / Track count subtitle
                    val subtitle = state.playlist?.author?.name
                        ?: state.playlist?.songCountText
                        ?: if (state.songs.isNotEmpty()) "${state.songs.size} tracks" else null

                    if (subtitle != null) {
                        item {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // "Play All" Button
                    item {
                        Button(
                            onClick = { playAtIndex(0) },
                            enabled = state.songs.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play All",
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "  Play All",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    // "Download All" Button
                    item {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val currentSongs = state.songs
                                    if (currentSongs.isEmpty()) return@launch
                                    val metaList = currentSongs.map { song ->
                                        MediaMetadata(
                                            id = song.id,
                                            title = song.title,
                                            artists = song.artists.map { a ->
                                                MediaMetadata.Artist(id = a.id, name = a.name)
                                            },
                                            duration = song.duration ?: 0,
                                            thumbnailUrl = song.thumbnail,
                                        )
                                    }
                                    WearApp.instance.downloadManager.downloadPlaylist(
                                        playlistId = playlistId,
                                        playlistTitle = effectiveTitle,
                                        songs = metaList,
                                    )
                                }
                            },
                            enabled = state.songs.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Download All",
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "  Download All",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    // Track Items
                    itemsIndexed(
                        items = state.songs,
                        key = { index, song -> "${song.id}_$index" },
                    ) { index, song ->
                        PlaylistSongItem(
                            song = song,
                            onClick = { playAtIndex(index) },
                        )
                    }

                    // Load More Continuation Button
                    if (state.continuation != null) {
                        item {
                            Button(
                                onClick = { viewModel.loadMore() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Text("Load More", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an individual track row with thumbnail, title, artist, duration, and download action.
 */
@Composable
private fun PlaylistSongItem(
    song: SongItem,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (song.thumbnail.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.thumbnail.resize(120, 120))
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val artist = song.artists.firstOrNull()?.name
                val durationText = song.duration?.takeIf { it > 0 }?.let { formatDuration(it) }
                val subtitle = listOfNotNull(artist, durationText).joinToString(" • ")

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            DownloadIconButton(
                songId = song.id,
                onDownloadClick = {
                    WearApp.instance.downloadManager.downloadSong(
                        songId = song.id,
                        title = song.title,
                        artist = song.artists.firstOrNull()?.name,
                        thumbnailUrl = song.thumbnail,
                        duration = song.duration ?: 0,
                        uri = null,
                    )
                },
                onRemoveClick = {
                    WearApp.instance.downloadManager.removeDownload(song.id)
                },
            )
        }
    }
}

private fun formatDuration(durationSeconds: Int): String {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
