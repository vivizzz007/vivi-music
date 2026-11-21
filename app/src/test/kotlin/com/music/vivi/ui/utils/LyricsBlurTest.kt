package com.music.vivi.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsBlurTest {
    @Test
    fun activeLineStaysSharp() {
        assertEquals(0f, lyricsBlurRadiusDp(0), 0f)
    }

    @Test
    fun blurFollowsTheLucidProgressiveCurve() {
        assertEquals(2f, lyricsBlurRadiusDp(1), 0f)
        assertEquals(3.5f, lyricsBlurRadiusDp(2), 0f)
        assertEquals(5f, lyricsBlurRadiusDp(3), 0f)
        assertEquals(6.5f, lyricsBlurRadiusDp(4), 0f)
        assertEquals(8f, lyricsBlurRadiusDp(5), 0f)
    }

    @Test
    fun farLinesStayAtTheCap() {
        assertEquals(8f, lyricsBlurRadiusDp(40), 0f)
    }

    @Test
    fun blurNeverDecreasesWithDistance() {
        (1..10).forEach { distance ->
            assertTrue(lyricsBlurRadiusDp(distance) >= lyricsBlurRadiusDp(distance - 1))
        }
    }

    @Test
    fun strengthScalesTheWholeCurveAndHundredPercentIsLucid() {
        assertEquals(3f, lyricsBlurRadiusDp(1, strengthPercent = 150), 1e-6f)
        assertEquals(lyricsBlurRadiusDp(3), lyricsBlurRadiusDp(3, strengthPercent = 100), 0f)
        assertEquals(0f, lyricsBlurRadiusDp(0, strengthPercent = 300), 0f)
    }

    private fun target(
        enabled: Boolean = true,
        autoScroll: Boolean = true,
        isActive: Boolean = false,
        isBackground: Boolean = false,
        selected: Boolean = false,
        distance: Int = 2,
        entryTime: Long = 10_000L,
        nextEntryTime: Long? = 12_000L,
        position: Long = 5_000L,
        lead: Int = 1500,
    ) = lyricsLineBlurTarget(
        enabled = enabled,
        autoScrollActive = autoScroll,
        isActive = isActive,
        isBackground = isBackground,
        isSynced = true,
        selectionActive = selected,
        isSelected = selected,
        distance = distance,
        entryTime = entryTime,
        nextEntryTime = nextEntryTime,
        position = position,
        leadMs = lead,
        strengthPercent = 100,
    )

    @Test
    fun disabledOrScrollingOrActiveLinesAreSharp() {
        assertEquals(0f, target(enabled = false), 0f)
        assertEquals(0f, target(autoScroll = false), 0f)
        assertEquals(0f, target(isActive = true), 0f)
        assertEquals(0f, target(isBackground = true), 0f)
    }

    @Test
    fun selectedLineIsSharpWhileAutoScrollIsOn() {
        assertEquals(0f, target(selected = true), 0f)
    }

    @Test
    fun nextLineSharpensLeadTimeBeforeItStarts() {
        assertEquals(0f, target(distance = 1, entryTime = 10_000L, position = 9_000L, lead = 1500), 0f)
        assertEquals(2f, target(distance = 1, entryTime = 10_000L, position = 5_000L, lead = 1500), 0f)
    }

    @Test
    fun previousLineStaysSharpWhileItStillSings() {
        assertEquals(0f, target(distance = 1, entryTime = 4_000L, nextEntryTime = 8_000L, position = 6_000L), 0f)
        assertEquals(2f, target(distance = 1, entryTime = 4_000L, nextEntryTime = 8_000L, position = 9_000L), 0f)
    }

    @Test
    fun durationDefaultLiesInsideTheSliderRange() {
        assertTrue(LYRICS_BLUR_DURATION_DEFAULT_MS in LYRICS_BLUR_DURATION_MIN_MS..LYRICS_BLUR_DURATION_MAX_MS)
    }
}
