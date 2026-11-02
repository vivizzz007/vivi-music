/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

internal const val LYRICS_BLUR_DURATION_DEFAULT_MS = 1000
internal const val LYRICS_BLUR_DURATION_MIN_MS = 200
internal const val LYRICS_BLUR_DURATION_MAX_MS = 2000
internal const val LYRICS_BLUR_LEAD_DEFAULT_MS = 1500
internal const val LYRICS_BLUR_LEAD_MIN_MS = 0
internal const val LYRICS_BLUR_LEAD_MAX_MS = 3000
internal const val LYRICS_BLUR_STRENGTH_DEFAULT_PERCENT = 150
internal const val LYRICS_BLUR_STRENGTH_MIN_PERCENT = 50
internal const val LYRICS_BLUR_STRENGTH_MAX_PERCENT = 300

// Progressive curve from the Lucid theme: lines fade with distance from the active line, the active line stays sharp.
// The strength percent scales the whole curve; 100 reproduces the Lucid values exactly.
internal fun lyricsBlurRadiusDp(distance: Int, strengthPercent: Int = 100): Float {
    val base = when {
        distance <= 0 -> 0f
        distance == 1 -> 2f
        distance == 2 -> 3.5f
        distance == 3 -> 5f
        distance == 4 -> 6.5f
        else -> 8f
    }
    return base * strengthPercent / 100f
}

// Shared rules for every lyrics style, so the same line is blurred the same way wherever it is drawn.
// The next line sharpens ahead of its start; the line just before the active one stays sharp while it still sings.
internal fun lyricsLineBlurTarget(
    enabled: Boolean,
    autoScrollActive: Boolean,
    isActive: Boolean,
    isBackground: Boolean,
    isSynced: Boolean,
    selectionActive: Boolean,
    isSelected: Boolean,
    distance: Int,
    entryTime: Long,
    nextEntryTime: Long?,
    position: Long,
    leadMs: Int,
    strengthPercent: Int,
): Float {
    val linePassed = entryTime <= position
    val linePlaying = nextEntryTime != null && position < nextEntryTime
    return when {
        !enabled || !autoScrollActive || isActive || isBackground || !isSynced -> 0f
        selectionActive && isSelected -> 0f
        linePassed && distance == 1 && linePlaying -> 0f
        !linePassed && distance == 1 && entryTime - position <= leadMs -> 0f
        else -> lyricsBlurRadiusDp(distance, strengthPercent)
    }
}

@Composable
internal fun Modifier.animatedLyricsBlur(targetDp: Float, durationMs: Int): Modifier {
    val radius by animateFloatAsState(
        targetValue = targetDp,
        animationSpec = tween(durationMillis = durationMs),
        label = "lyricsBlur",
    )
    return if (radius > 0f) this.blur(radius.dp) else this
}
