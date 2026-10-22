/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

internal const val LYRIC_FOLLOW_DISTANCE = 3

// The list also holds gap indicators and skips blank lines, so a line number is not its row number.
internal fun listRowFor(rowLineIndices: List<Int?>, lineIndex: Int): Int =
    rowLineIndices.indexOf(lineIndex).takeIf { it != -1 } ?: lineIndex

// Only the active line and its close neighbours need the per-frame playback position for karaoke.
internal fun followsPlaybackPosition(isActive: Boolean, distanceFromActive: Int): Boolean =
    isActive || distanceFromActive <= LYRIC_FOLLOW_DISTANCE

// Scrolls the sync button to two rows above the current line, so the lines being sung stay in context.
internal fun syncScrollIndex(rowIndex: Int, providerOffset: Int, contextRows: Int = 2): Int =
    maxOf(0, rowIndex - contextRows) + providerOffset
