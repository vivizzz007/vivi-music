package com.music.vivi.update.apple

import androidx.compose.runtime.Composable
// Compose UI imports
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.LocalPlayerConnection

// Kotlin standard library
import kotlin.math.abs
@Composable
fun AppleMusicStyleLyrics(
    sliderPositionProvider: () -> Long?,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)

    // Parse lyrics into lines with timestamps (you'll need to implement this based on your lyrics format)
    val lyricsLines = remember(currentLyrics) {
        parseLyricsWithTimestamps(currentLyrics?.lyrics)
    }

    // Find current active line based on playback position
    val currentLineIndex = remember(currentPosition, lyricsLines) {
        findCurrentLyricLine(currentPosition, lyricsLines)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 100.dp), // Center the content vertically
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyricsLines) { index, lyricLine ->
            val isActive = index == currentLineIndex
            val isNearActive = kotlin.math.abs(index - currentLineIndex) <= 1

            // Calculate alpha and blur based on distance from active line
            val alpha = when {
                isActive -> 1.0f
                isNearActive -> 0.6f
                else -> 0.3f
            }

            val textSize = when {
                isActive -> 28.sp // Large text for active line
                isNearActive -> 24.sp // Medium for nearby lines
                else -> 20.sp // Smaller for distant lines
            }

            Text(
                text = lyricLine.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = textSize,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                ),
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .then(
                        if (!isActive) {
                            Modifier.blur(if (isNearActive) 1.dp else 2.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clickable {
                        // Optional: seek to this lyric line when clicked
                        if (lyricLine.timestamp >= 0) {
                            playerConnection.player.seekTo(lyricLine.timestamp)
                        }
                    },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 36.sp
            )
        }
    }
}

// Data class for lyric lines with timestamps
data class LyricLine(
    val text: String,
    val timestamp: Long = -1L // -1 if no timestamp available
)

// Helper function to parse lyrics with timestamps
private fun parseLyricsWithTimestamps(lyrics: String?): List<LyricLine> {
    if (lyrics.isNullOrEmpty()) {
        return listOf(LyricLine("No lyrics available"))
    }

    // This is a basic implementation - you'll need to adapt based on your lyrics format
    // For LRC format: [mm:ss.xx]Lyric text
    val lrcPattern = Regex("""^\[(\d{2}):(\d{2})\.(\d{2})\](.*)$""")

    return lyrics.lines().mapNotNull { line ->
        val trimmedLine = line.trim()
        if (trimmedLine.isEmpty()) return@mapNotNull null

        val match = lrcPattern.matchEntire(trimmedLine)
        if (match != null) {
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            val centiseconds = match.groupValues[3].toLongOrNull() ?: 0
            val text = match.groupValues[4].trim()

            val timestamp = (minutes * 60 + seconds) * 1000 + centiseconds * 10
            LyricLine(text, timestamp)
        } else {
            // Plain text without timestamp
            LyricLine(trimmedLine)
        }
    }.ifEmpty {
        listOf(LyricLine("No lyrics available"))
    }
}

// Helper function to find current lyric line based on position
private fun findCurrentLyricLine(position: Long, lines: List<LyricLine>): Int {
    if (lines.isEmpty()) return -1

    // Find the last line whose timestamp is less than or equal to current position
    for (i in lines.indices.reversed()) {
        if (lines[i].timestamp >= 0 && lines[i].timestamp <= position) {
            return i
        }
    }

    // If no timestamp matches, return first line or -1
    return if (lines.isNotEmpty()) 0 else -1
}