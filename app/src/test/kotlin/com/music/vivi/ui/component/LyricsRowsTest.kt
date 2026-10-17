package com.music.vivi.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRowsTest {
    @Test
    fun linesWithoutIndicatorsKeepTheirNumber() {
        val rows = listOf<Int?>(0, 1, 2, 3)

        assertEquals(2, listRowFor(rows, 2))
    }

    @Test
    fun gapIndicatorShiftsLaterLinesDown() {
        // Row 1 is a gap indicator, so line 1 sits on row 2.
        val rows = listOf<Int?>(0, null, 1, 2)

        assertEquals(2, listRowFor(rows, 1))
        assertEquals(3, listRowFor(rows, 2))
    }

    @Test
    fun skippedBlankLinesShiftLaterLinesUp() {
        // Line 1 is blank and not in the list, so line 2 sits on row 1.
        val rows = listOf<Int?>(0, 2, 3)

        assertEquals(1, listRowFor(rows, 2))
    }

    @Test
    fun missingLineFallsBackToItsNumber() {
        assertEquals(7, listRowFor(listOf<Int?>(0, 1), 7))
    }

    @Test
    fun emptyListFallsBackToTheLineNumber() {
        assertEquals(3, listRowFor(emptyList(), 3))
    }

    @Test
    fun firstLineMapsToRowZeroEvenAfterIndicators() {
        assertEquals(0, listRowFor(listOf<Int?>(0, null, 1), 0))
    }

    @Test
    fun activeLineAlwaysFollowsPlaybackPosition() {
        assertTrue(followsPlaybackPosition(isActive = true, distanceFromActive = 0))
        assertTrue(followsPlaybackPosition(isActive = true, distanceFromActive = 40))
    }

    @Test
    fun linesWithinFollowDistanceFollowPlaybackPosition() {
        for (distance in 1..LYRIC_FOLLOW_DISTANCE) {
            assertTrue("distance $distance", followsPlaybackPosition(isActive = false, distanceFromActive = distance))
        }
    }

    @Test
    fun linesJustOutsideFollowDistanceStayStill() {
        assertFalse(followsPlaybackPosition(isActive = false, distanceFromActive = LYRIC_FOLLOW_DISTANCE + 1))
    }

    @Test
    fun farLinesStayStill() {
        assertFalse(followsPlaybackPosition(isActive = false, distanceFromActive = 50))
    }

    @Test
    fun syncScrollsTwoRowsAboveTheCurrentLine() {
        assertEquals(3, syncScrollIndex(rowIndex = 5, providerOffset = 0))
    }

    @Test
    fun syncIncludesTheProviderHeaderRow() {
        assertEquals(4, syncScrollIndex(rowIndex = 5, providerOffset = 1))
    }

    @Test
    fun syncNearTheTopClampsToFirstRow() {
        assertEquals(0, syncScrollIndex(rowIndex = 1, providerOffset = 0))
        assertEquals(1, syncScrollIndex(rowIndex = 1, providerOffset = 1))
    }

    @Test
    fun syncOnTheFirstLineStaysAtTheTop() {
        assertEquals(0, syncScrollIndex(rowIndex = 0, providerOffset = 0))
    }

    @Test
    fun syncUsesConfiguredContextRows() {
        assertEquals(7, syncScrollIndex(rowIndex = 10, providerOffset = 0, contextRows = 3))
    }

    @Test
    fun syncLandsOnTheRowOfTheCurrentLineAfterGapIndicators() {
        // Line 4 sits on row 5 because one gap indicator comes before it.
        val rows = listOf<Int?>(0, 1, null, 2, 3, 4, null, 5)
        val row = listRowFor(rows, 4)

        assertEquals(5, row)
        assertEquals(3, syncScrollIndex(row, providerOffset = 0))
    }
}
