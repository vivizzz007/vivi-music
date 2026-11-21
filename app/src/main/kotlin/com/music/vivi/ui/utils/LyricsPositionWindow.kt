/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Before the start the window must reach past the longest blur lead, so the next line can still sharpen ahead of time.
internal const val LYRICS_WINDOW_LEAD_MS = LYRICS_BLUR_LEAD_MAX_MS + 500L
internal const val LYRICS_WINDOW_TAIL_MS = 500L

/**
 * The span of playback time in which a lyrics line can look different from frame to frame. Outside of it the line is
 * either fully upcoming or fully sung, so any position beyond the edges draws the same.
 */
internal fun lyricsPositionWindow(entryTime: Long, nextEntryTime: Long?, lastWordEndMs: Long?): LongRange {
    val start = entryTime - LYRICS_WINDOW_LEAD_MS
    if (nextEntryTime == null) return start..Long.MAX_VALUE
    val sungUntil = maxOf(nextEntryTime, lastWordEndMs ?: nextEntryTime, entryTime)
    return start..(sungUntil + LYRICS_WINDOW_TAIL_MS)
}

/**
 * Playback position clamped to the line's [lyricsPositionWindow]. The position ticks every frame, but only the two or
 * three lines around the current one see a changing value, so every other line skips recomposition entirely.
 */
@Composable
internal fun rememberWindowedLyricsPosition(
    position: LongState,
    offsetMs: Long,
    entryTime: Long,
    nextEntryTime: Long?,
    lastWordEndMs: Long?,
): Long {
    val window = remember(entryTime, nextEntryTime, lastWordEndMs) {
        lyricsPositionWindow(entryTime, nextEntryTime, lastWordEndMs)
    }
    val windowed by remember(position, offsetMs, window) {
        derivedStateOf { (position.longValue + offsetMs).coerceIn(window) }
    }
    return windowed
}
