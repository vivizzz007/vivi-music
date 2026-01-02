package com.music.vivi.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.music.vivi.ui.component.shimmer.ShimmerHost
import androidx.compose.material3.TextButton
import com.music.vivi.ui.component.LocalBottomSheetPageState
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

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
    val cornerRadius = 24.dp
    val albumArtShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = cornerRadius, smoothnessAsPercentBR = 60, cornerRadiusBR = cornerRadius,
        smoothnessAsPercentTL = 60, cornerRadiusTL = cornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = cornerRadius, smoothnessAsPercentTR = 60
    )

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(windowInsets.asPaddingValues())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Song Info",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                TextButton(onClick = { sheetState.dismiss() }) {
                    Text("Done")
                }
            }
        }

        // 1. Large Album Art Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(albumArtShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val imageUrl = song?.thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
                
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                     ShimmerHost {
                        Box(modifier = Modifier.fillMaxSize()) // Placeholder
                    }
                }
            }
        }

        // 2. Info Grid
        if (song != null || info != null) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title / Album
                        InfoItem(
                            label = "Title",
                            value = song?.title ?: info?.title ?: "Unknown",
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Registration (Format info)
                        val formatText = buildString {
                            if (currentFormat != null) {
                                append(currentFormat?.mimeType?.substringBefore(";") ?: "Unknown")
                                currentFormat?.bitrate?.let { append(" • ${it / 1000}kbps") }
                            } else {
                                append("Standard")
                            }
                        }
                        InfoItem(
                            label = "Format",
                            value = formatText,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // When (Duration / Year)
                        val duration = song?.song?.duration?.let { totalSeconds ->
                            val minutes = totalSeconds / 60
                            val seconds = totalSeconds % 60
                            "%d:%02d".format(minutes, seconds)
                        } ?: "Unknown"
                        
                        InfoItem(
                            label = "Duration",
                            value = duration,
                            modifier = Modifier.weight(1f)
                        )

                        // Hosted By (Artist)
                        InfoItem(
                            label = "Artist",
                            value = song?.artists?.joinToString { it.name } ?: info?.author ?: "Unknown",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                       // Guests (Views)
                        val viewCount = info?.viewCount?.toInt()?.let { shortNumberFormatter(it) } ?: "N/A"
                        InfoItem(
                            label = "Views",
                            value = "$viewCount Views",
                            modifier = Modifier.weight(1f)
                        )

                         // Likes
                        val likeCount = info?.like?.toInt()?.let { shortNumberFormatter(it) } ?: "N/A"
                        InfoItem(
                            label = "Likes",
                            value = "$likeCount Likes",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                     // Row 4 (Extra details)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                       InfoItem(
                            label = "Itag",
                            value = currentFormat?.itag?.toString() ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        
                         InfoItem(
                            label = "Loudness",
                             value = currentFormat?.loudnessDb?.let { "$it dB" } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Description (Full width at bottom)
             item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                     Text(
                        text = info?.description ?: "No description available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
        } else {
             item {
                 ShimmerHost {
                    Column {
                        TextPlaceholder()
                        Spacer(Modifier.height(8.dp))
                        TextPlaceholder()
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

// Helper formatter (simplified version of what was likely used before or inline)
fun shortNumberFormatter(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> String.format("%.1fk", count / 1000.0)
        else -> String.format("%.1fM", count / 1000000.0)
    }
}
