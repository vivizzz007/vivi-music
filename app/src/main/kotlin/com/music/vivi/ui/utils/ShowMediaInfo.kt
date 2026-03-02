/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.MediaInfo
import com.music.vivi.LocalDatabase
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.db.entities.FormatEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars
    var info by remember { mutableStateOf<MediaInfo?>(null) }
    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }
    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val sheetState = LocalBottomSheetPageState.current
    val clipboardManager = LocalClipboard.current

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }
    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect { song = it }
    }
    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect { currentFormat = it }
    }

    // Shapes
    val albumArtShape = RoundedCornerShape(24.dp)

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = windowInsets.asPaddingValues().calculateBottomPadding())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header: "Song Info" title + Done button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.song_info),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { sheetState.dismiss() }) {
                    Text(stringResource(R.string.done))
                }
            }
        }

        // Large Album Art Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(albumArtShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val imageUrl = song?.thumbnailUrl
                    ?: "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Info Grid
        if (song != null || info != null) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Row 1: Title / Artist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.song_info_title_label),
                            value = song?.title ?: info?.title ?: stringResource(R.string.unknown),
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.artist),
                            value = song?.artists?.joinToString { it.name }
                                ?: info?.author
                                ?: stringResource(R.string.unknown),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Duration / Media ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val duration = song?.song?.duration?.let { totalSeconds ->
                            val minutes = totalSeconds / 60
                            val seconds = totalSeconds % 60
                            "%d:%02d".format(minutes, seconds)
                        } ?: stringResource(R.string.unknown)
                        InfoItem(
                            label = stringResource(R.string.song_info_duration_label),
                            value = duration,
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.media_id),
                            value = song?.id ?: videoId,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Views / Likes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val viewCount = info?.viewCount?.toInt()?.let { shortNumberFormatter(it) } ?: "N/A"
                        InfoItem(
                            label = stringResource(R.string.views),
                            value = stringResource(R.string.song_info_views_count, viewCount),
                            modifier = Modifier.weight(1f)
                        )
                        val likeCount = info?.like?.toInt()?.let { shortNumberFormatter(it) } ?: "N/A"
                        InfoItem(
                            label = stringResource(R.string.likes),
                            value = stringResource(R.string.song_info_likes_count, likeCount),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 4: Dislikes / Subscribers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.dislikes),
                            value = info?.dislike?.let { shortNumberFormatter(it.toInt()) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.subscribers),
                            value = info?.subscribers ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 5: Itag / Loudness
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.song_info_itag_label),
                            value = currentFormat?.itag?.toString() ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.loudness),
                            value = currentFormat?.loudnessDb?.let { "$it dB" } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 6: Format / Bitrate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.mime_type),
                            value = currentFormat?.mimeType?.substringBefore(";") ?: stringResource(R.string.song_info_standard),
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.bitrate),
                            value = currentFormat?.bitrate?.let { "${it / 1000} Kbps" } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 7: Codecs / Sample Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.codecs),
                            value = currentFormat?.codecs ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.sample_rate),
                            value = currentFormat?.sampleRate?.let { "$it Hz" } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 8: File Size / Volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            label = stringResource(R.string.file_size),
                            value = currentFormat?.contentLength?.let {
                                Formatter.formatShortFileSize(context, it)
                            } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        InfoItem(
                            label = stringResource(R.string.volume),
                            value = if (playerConnection != null)
                                "${(playerConnection.player.volume * 100).toInt()}%"
                            else "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Description (full width at bottom)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.description),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (info == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    } else {
                        Text(
                            text = info?.description ?: stringResource(R.string.song_info_no_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

        } else {
            // Loading state before song/info data arrives
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerHost {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextPlaceholder()
                            TextPlaceholder()
                            TextPlaceholder()
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .clickable {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun shortNumberFormatter(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> String.format("%.1fk", count / 1000.0)
        else -> String.format("%.1fM", count / 1_000_000.0)
    }
}
