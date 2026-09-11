/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.playback.WearDownloadCache

/**
 * Reusable Wear Compose icon button displaying reactive offline download status for a track.
 *
 * - When downloaded: displays a checkmark icon allowing removal.
 * - When downloading or queued: displays a circular progress indicator.
 * - Otherwise: displays a download icon allowing one-tap download.
 */
@Composable
fun DownloadIconButton(
    songId: String,
    onDownloadClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val downloads by WearApp.instance.downloadManager.downloads.collectAsState()
    val download = downloads[songId]
    val isDownloaded = download?.state == Download.STATE_COMPLETED || WearDownloadCache.isDownloaded(songId)
    val isDownloading = download?.state == Download.STATE_DOWNLOADING ||
        download?.state == Download.STATE_QUEUED ||
        download?.state == Download.STATE_RESTARTING

    when {
        isDownloaded -> {
            IconButton(
                onClick = { onRemoveClick?.invoke() },
                modifier = modifier.size(32.dp),
                enabled = onRemoveClick != null,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        isDownloading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.size(32.dp),
            ) {
                val progress = download.percentDownloaded / 100f
                CircularProgressIndicator(
                    progress = { if (progress in 0f..1f) progress else 0f },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        else -> {
            IconButton(
                onClick = onDownloadClick,
                modifier = modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
