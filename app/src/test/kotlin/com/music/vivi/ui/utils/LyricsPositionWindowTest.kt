package com.music.vivi.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsPositionWindowTest {
    @Test
    fun windowOpensBeforeTheLineByMoreThanTheLongestBlurLead() {
        val window = lyricsPositionWindow(entryTime = 10_000L, nextEntryTime = 14_000L, lastWordEndMs = null)

        assertEquals(10_000L - LYRICS_WINDOW_LEAD_MS, window.first)
        assertTrue(10_000L - window.first > LYRICS_BLUR_LEAD_MAX_MS)
    }

    @Test
    fun windowClosesShortlyAfterTheNextLineStarts() {
        val window = lyricsPositionWindow(entryTime = 10_000L, nextEntryTime = 14_000L, lastWordEndMs = null)

        assertEquals(14_000L + LYRICS_WINDOW_TAIL_MS, window.last)
    }

    @Test
    fun wordsRunningPastTheNextLineKeepTheWindowOpen() {
        val window = lyricsPositionWindow(entryTime = 10_000L, nextEntryTime = 14_000L, lastWordEndMs = 15_200L)

        assertEquals(15_200L + LYRICS_WINDOW_TAIL_MS, window.last)
    }

    @Test
    fun wordsEndingBeforeTheNextLineDoNotShrinkTheWindow() {
        val window = lyricsPositionWindow(entryTime = 10_000L, nextEntryTime = 14_000L, lastWordEndMs = 12_000L)

        assertEquals(14_000L + LYRICS_WINDOW_TAIL_MS, window.last)
    }

    @Test
    fun lastLineStaysOpenForever() {
        val window = lyricsPositionWindow(entryTime = 10_000L, nextEntryTime = null, lastWordEndMs = 11_000L)

        assertEquals(Long.MAX_VALUE, window.last)
    }

    @Test
    fun outOfOrderNextLineStillGivesAValidWindow() {
        val window = lyricsPositionWindow(entryTime = 10_000L, nextEntryTime = 9_000L, lastWordEndMs = null)

        assertTrue(window.first <= window.last)
        assertTrue(10_000L in window)
    }

    @Test
    fun clampedPositionKeepsBlurDecisionsForFarLines() {
        // A line far in the future must still read as upcoming and outside the sharpening lead after clamping.
        val entry = 60_000L
        val next = 64_000L
        val window = lyricsPositionWindow(entry, next, lastWordEndMs = null)
        val clamped = 1_000L.coerceIn(window)

        val blur = lyricsLineBlurTarget(
            enabled = true,
            autoScrollActive = true,
            isActive = false,
            isBackground = false,
            isSynced = true,
            selectionActive = false,
            isSelected = false,
            distance = 1,
            entryTime = entry,
            nextEntryTime = next,
            position = clamped,
            leadMs = LYRICS_BLUR_LEAD_MAX_MS,
            strengthPercent = 100,
        )

        assertEquals(lyricsBlurRadiusDp(1), blur, 0f)
    }

    @Test
    fun clampedPositionKeepsFinishedLinesFinished() {
        val entry = 10_000L
        val next = 14_000L
        val window = lyricsPositionWindow(entry, next, lastWordEndMs = null)
        val clamped = 90_000L.coerceIn(window)

        val blur = lyricsLineBlurTarget(
            enabled = true,
            autoScrollActive = true,
            isActive = false,
            isBackground = false,
            isSynced = true,
            selectionActive = false,
            isSelected = false,
            distance = 1,
            entryTime = entry,
            nextEntryTime = next,
            position = clamped,
            leadMs = LYRICS_BLUR_LEAD_DEFAULT_MS,
            strengthPercent = 100,
        )

        assertTrue(clamped >= next)
        assertEquals(lyricsBlurRadiusDp(1), blur, 0f)
    }
}
