package com.music.vivi.ui.screens.wrapped.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WrappedBackgroundTest {
    @Test
    fun gridCoversEveryCellInclusiveOfEdges() {
        val points = dotGridPoints(width = 60f, height = 30f, spacing = 30f)

        assertEquals(
            listOf(
                Offset(0f, 0f), Offset(0f, 30f),
                Offset(30f, 0f), Offset(30f, 30f),
                Offset(60f, 0f), Offset(60f, 30f),
            ),
            points,
        )
    }

    @Test
    fun gridIsEmptyForZeroWidth() {
        assertTrue(dotGridPoints(width = 0f, height = 300f, spacing = 30f).isEmpty())
    }

    @Test
    fun gridIsEmptyForZeroHeight() {
        assertTrue(dotGridPoints(width = 300f, height = 0f, spacing = 30f).isEmpty())
    }

    @Test
    fun gridIsEmptyForNegativeDimensions() {
        assertTrue(dotGridPoints(width = -10f, height = 300f, spacing = 30f).isEmpty())
        assertTrue(dotGridPoints(width = 300f, height = -10f, spacing = 30f).isEmpty())
    }

    @Test
    fun gridKeepsOnlyCellsStartingInsideWhenSizeIsNotAMultipleOfSpacing() {
        val points = dotGridPoints(width = 100f, height = 10f, spacing = 30f)

        assertEquals(
            listOf(0f, 30f, 60f, 90f),
            points.map { it.x }.distinct(),
        )
        assertTrue(points.all { it.y == 0f })
    }

    @Test
    fun gridReducesToSingleOriginDotWhenSpacingExceedsSize() {
        assertEquals(
            listOf(Offset(0f, 0f)),
            dotGridPoints(width = 10f, height = 10f, spacing = 30f),
        )
    }

    @Test
    fun gridPointCountMatchesColumnsTimesRows() {
        val points = dotGridPoints(width = 1080f, height = 2400f, spacing = 30f)

        assertEquals((1080 / 30 + 1) * (2400 / 30 + 1), points.size)
    }
}
