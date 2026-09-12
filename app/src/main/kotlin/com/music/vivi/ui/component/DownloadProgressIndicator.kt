/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import coil3.compose.AsyncImage
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalDownloadUtil
import com.music.vivi.R
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.ui.utils.formatFileSize
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DownloadProgressPill(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val downloadUtil = LocalDownloadUtil.current
    val downloads by downloadUtil.downloads.collectAsState()

    val activeDownloads = remember(downloads) {
        downloads.values.filter {
            it.state == Download.STATE_DOWNLOADING ||
            it.state == Download.STATE_QUEUED ||
            it.state == Download.STATE_RESTARTING
        }
    }

    AnimatedVisibility(
        visible = activeDownloads.isNotEmpty(),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (activeDownloads.isEmpty()) return@AnimatedVisibility

        val downloadingItem = activeDownloads.firstOrNull { it.state == Download.STATE_DOWNLOADING }
            ?: activeDownloads.first()
        val title = remember(downloadingItem.request.data, downloadingItem.request.id) {
            runCatching { String(downloadingItem.request.data) }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: downloadingItem.request.id
        }

        val progress = downloadingItem.percentDownloaded
        val progressText = if (progress >= 0f) "${progress.toInt()}%" else "Queued"
        val countText = "${activeDownloads.size}"

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(44.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                if (progress >= 0f) {
                    CircularProgressIndicator(
                        progress = { (progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "$title • $progressText ($countText remaining)",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Expand queue",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueBottomSheet(
    onDismissRequest: () -> Unit,
) {
    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val downloads by downloadUtil.downloads.collectAsState()

    val activeDownloads = remember(downloads) {
        downloads.values.filter {
            it.state == Download.STATE_DOWNLOADING ||
            it.state == Download.STATE_QUEUED ||
            it.state == Download.STATE_RESTARTING
        }.sortedWith(
            compareBy<Download> {
                if (it.state == Download.STATE_DOWNLOADING) 0 else 1
            }.thenBy { it.request.id }
        )
    }

    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)

    if (activeDownloads.isEmpty()) {
        LaunchedEffect(Unit) {
            onDismissRequest()
        }
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${stringResource(R.string.downloading)} (${activeDownloads.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeDownloads, key = { it.request.id }) { download ->
                    DownloadQueueItem(download = download, database = database)
                }
            }
        }
    }
}

@Composable
private fun DownloadQueueItem(
    download: Download,
    database: MusicDatabase,
) {
    val songId = download.request.id
    val title = remember(download.request.data, songId) {
        runCatching { String(download.request.data) }.getOrNull()?.takeIf { it.isNotBlank() } ?: songId
    }

    val songEntity by produceState<SongEntity?>(initialValue = null, songId) {
        value = withContext(Dispatchers.IO) {
            database.getSongByIdBlocking(songId)?.song
        }
    }

    val isDownloading = download.state == Download.STATE_DOWNLOADING
    val percent = download.percentDownloaded
    val bytesDownloaded = download.bytesDownloaded
    val contentLength = download.contentLength

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (songEntity?.thumbnailUrl != null) {
                        AsyncImage(
                            model = songEntity?.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songEntity?.title ?: title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDownloading) {
                            if (percent >= 0f && contentLength > 0L) {
                                "${percent.toInt()}% • ${formatFileSize(bytesDownloaded)} / ${formatFileSize(contentLength)}"
                            } else if (percent >= 0f) {
                                "${percent.toInt()}% • ${formatFileSize(bytesDownloaded)}"
                            } else {
                                "${stringResource(R.string.downloading)} • ${formatFileSize(bytesDownloaded)}"
                            }
                        } else {
                            "Queued"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                if (percent >= 0f) {
                    LinearProgressIndicator(
                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
