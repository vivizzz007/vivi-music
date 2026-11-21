package com.music.vivi.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMeshArtworkDimTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun emptyArtworkHasNoHighlight() {
        assertEquals(0f, highlightLuminance(IntArray(0)), 0f)
    }

    @Test
    fun uniformBlackAndWhiteMeasureAtTheExtremes() {
        assertEquals(0f, highlightLuminance(IntArray(64) { black }), 1e-6f)
        assertEquals(1f, highlightLuminance(IntArray(64) { white }), 1e-6f)
    }

    @Test
    fun brightestTenPercentOfPixelsDecidesTheHighlight() {
        val pixels = IntArray(100) { index -> if (index < 90) black else white }

        assertEquals(1f, highlightLuminance(pixels), 1e-6f)
    }

    @Test
    fun aSmallBrightSpotBelowTheTopPercentileDoesNotCountAsHighlight() {
        val pixels = IntArray(100) { index -> if (index < 95) black else white }

        assertEquals(0f, highlightLuminance(pixels), 1e-6f)
    }

    @Test
    fun greenWeighsMoreThanBlueInLuminance() {
        val green = highlightLuminance(intArrayOf(0xFF00FF00.toInt()))
        val blue = highlightLuminance(intArrayOf(0xFF0000FF.toInt()))

        assertEquals(0.7152f, green, 1e-4f)
        assertEquals(0.0722f, blue, 1e-4f)
    }

    @Test
    fun darkArtworkIsNotDimmed() {
        assertEquals(1f, artworkDimFactor(highlight = 0.5f), 0f)
    }

    @Test
    fun brightArtworkIsDimmedToTheTargetHighlight() {
        // Target highlight at strength 0.3 is 1 - 0.8 * 0.3 = 0.76.
        assertEquals(0.76f, artworkDimFactor(highlight = 1f), 1e-6f)
        assertEquals(0.76f / 0.9f, artworkDimFactor(highlight = 0.9f), 1e-6f)
    }

    @Test
    fun zeroStrengthNeverDims() {
        assertEquals(1f, artworkDimFactor(highlight = 1f, strength = 0f), 1e-6f)
    }

    @Test
    fun fullStrengthDimsWhiteArtworkToTwentyPercent() {
        assertEquals(0.2f, artworkDimFactor(highlight = 1f, strength = 1f), 1e-6f)
    }

    @Test
    fun outOfRangeStrengthIsClamped() {
        assertEquals(artworkDimFactor(highlight = 1f, strength = 1f), artworkDimFactor(highlight = 1f, strength = 4f), 0f)
        assertEquals(1f, artworkDimFactor(highlight = 1f, strength = -2f), 0f)
    }
}
