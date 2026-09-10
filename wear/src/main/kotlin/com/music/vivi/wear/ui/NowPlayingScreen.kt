/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.playback.BluetoothOutputState

/**
 * Now Playing screen for Wear OS.
 *
 * Displays current track info with album art, playback controls (play/pause, skip),
 * and a circular progress ring. All on a true-black AMOLED background.
 */
@Composable
fun NowPlayingScreen(
    mediaState: MediaControllerState,
    onNavigateToLibrary: () -> Unit,
) {
    val context = LocalContext.current
    val btState by WearApp.instance.audioRouter.bluetoothState.collectAsState()
    val controller = mediaState.controller
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                val direction = if (event.verticalScrollPixels > 0) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction,
                    AudioManager.FLAG_SHOW_UI,
                )
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        // Circular progress ring around the edge
        if (mediaState.duration > 0) {
            CircularProgressIndicator(
                progress = {
                    (mediaState.position.toFloat() / mediaState.duration.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxSize(),
                startAngle = 270f,
                endAngle = 270f,
                colors = androidx.wear.compose.material3.ProgressIndicatorDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                strokeWidth = 4.dp,
            )
        }

        val currentItem = mediaState.currentMediaItem
        if (currentItem != null) {
            PlaybackContent(
                title = currentItem.mediaMetadata.title?.toString() ?: stringResource(R.string.unknown_title),
                artist = currentItem.mediaMetadata.artist?.toString(),
                artworkUri = currentItem.mediaMetadata.artworkUri?.toString(),
                isPlaying = mediaState.isPlaying,
                btState = btState,
                onPlayPause = {
                    if (mediaState.isPlaying) controller?.pause() else controller?.play()
                },
                onSkipPrevious = { controller?.seekToPrevious() },
                onSkipNext = { controller?.seekToNext() },
                onNavigateToLibrary = onNavigateToLibrary,
                onAdjustVolume = {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_SAME,
                        AudioManager.FLAG_SHOW_UI,
                    )
                },
                onOpenAudioSettings = {
                    WearApp.instance.audioRouter.openBluetoothSettings(context)
                },
            )
        } else {
            IdleContent(onNavigateToLibrary = onNavigateToLibrary)
        }
    }
}

@Composable
private fun PlaybackContent(
    title: String,
    artist: String?,
    artworkUri: String?,
    isPlaying: Boolean,
    btState: BluetoothOutputState,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onAdjustVolume: () -> Unit,
    onOpenAudioSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Album art
        if (artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Track title
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Artist
        if (artist != null) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Skip previous
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.skip_previous),
                    modifier = Modifier.size(24.dp),
                )
            }

            // Play/Pause
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    modifier = Modifier.size(26.dp),
                )
            }

            // Skip next
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.skip_next),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary controls row: Volume button + Audio route chip
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Volume adjustment button
            IconButton(
                onClick = onAdjustVolume,
                modifier = Modifier.size(30.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.volume),
                    modifier = Modifier.size(18.dp),
                )
            }

            // Audio route chip (Speaker icon or Bluetooth icon + device name)
            Button(
                onClick = onOpenAudioSettings,
                modifier = Modifier.height(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val connectedState = btState as? BluetoothOutputState.Connected
                    Icon(
                        imageVector = if (connectedState != null) Icons.Filled.Bluetooth else Icons.Filled.Speaker,
                        contentDescription = stringResource(R.string.audio_route),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = connectedState?.deviceName ?: stringResource(R.string.watch_speaker),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Library button
        Button(
            onClick = onNavigateToLibrary,
            modifier = Modifier.height(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(
                text = stringResource(R.string.library),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun IdleContent(onNavigateToLibrary: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.select_song_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onNavigateToLibrary,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.library))
        }
    }
}
