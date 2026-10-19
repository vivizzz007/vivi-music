package com.music.vivi.ui.utils

import androidx.compose.ui.graphics.Color
import com.music.vivi.ui.component.LiveMeshClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiveMeshFlowTest {
    private val red = 0xFFFF0000.toInt()
    private val green = 0xFF00FF00.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    private fun List<Color>.containsCloseTo(target: Color): Boolean = any { color ->
        abs(color.red - target.red) < 1e-3f &&
            abs(color.green - target.green) < 1e-3f &&
            abs(color.blue - target.blue) < 1e-3f
    }

    @Test
    fun paletteKeepsEveryDistinctColourOfAFourRegionCover() {
        val pixels = IntArray(16) { index ->
            val x = index % 4
            val y = index / 4
            when {
                y < 2 && x < 2 -> red
                y < 2 -> green
                x < 2 -> blue
                else -> white
            }
        }

        val palette = paletteFromPixels(pixels, 4, 4)!!

        assertEquals(4, palette.size)
        assertTrue(palette.containsCloseTo(Color(0xFFFF0000)))
        assertTrue(palette.containsCloseTo(Color(0xFF00FF00)))
        assertTrue(palette.containsCloseTo(Color(0xFF0000FF)))
        assertTrue(palette.containsCloseTo(Color(0xFFFFFFFF)))
    }

    @Test
    fun darkCoverKeepsItsBlackAndItsVividAccent() {
        val pixels = IntArray(16) { black }
        pixels[0] = red

        val palette = paletteFromPixels(pixels, 4, 4)!!

        assertTrue(palette.containsCloseTo(Color(0xFF000000)))
        assertTrue(palette.containsCloseTo(Color(0xFFFF0000)))
    }

    @Test
    fun dominantClusterComesFirst() {
        val pixels = IntArray(16) { index -> if (index < 3) red else black }

        val palette = paletteFromPixels(pixels, 4, 4)!!

        assertEquals(Color(0xFF000000), palette.first())
    }

    @Test
    fun dominantBackgroundShadesStayFirstWhileAccentsSurvive() {
        val shadedBlues = listOf(0xFF1E90D8.toInt(), 0xFF1A84C6.toInt(), 0xFF2196E0.toInt())
        val orange = 0xFFFF8000.toInt()
        val darkGrey = 0xFF303030.toInt()
        val pixels = IntArray(100) { index ->
            when {
                index < 60 -> shadedBlues[index % shadedBlues.size]
                index < 85 -> darkGrey
                else -> orange
            }
        }

        val palette = paletteFromPixels(pixels, 10, 10)!!

        assertTrue(palette.first().blue > palette.first().red)
        assertTrue(palette.any { it.red > 0.95f && it.green in 0.4f..0.6f && it.blue < 0.05f })
    }

    @Test
    fun uniformCoverGivesTheSameColourInEverySlot() {
        val palette = paletteFromPixels(IntArray(16) { black }, 4, 4)!!

        assertTrue(palette.all { it.red == 0f && it.green == 0f && it.blue == 0f })
    }

    @Test
    fun paletteIsDeterministicForTheSameCover() {
        val pixels = IntArray(64) { index -> if (index % 3 == 0) blue else if (index % 5 == 0) green else black }

        assertEquals(paletteFromPixels(pixels, 8, 8), paletteFromPixels(pixels, 8, 8))
    }

    @Test
    fun paletteIsUnavailableForTooSmallOrShortArtwork() {
        assertNull(paletteFromPixels(IntArray(1), width = 1, height = 1))
        assertNull(paletteFromPixels(IntArray(4), width = 4, height = 4))
    }

    @Test
    fun unitHashStaysBelowOneForAnyInputs() {
        for (seed in listOf(0, 1, -7, 123456, Int.MAX_VALUE, Int.MIN_VALUE)) {
            for (index in 0 until FLOW_BLOB_COUNT) {
                for (salt in 0 until 10) {
                    val value = unitHash(seed, index, salt)
                    assertTrue(value >= 0f && value < 1f)
                }
            }
        }
    }

    @Test
    fun blobsStayInsideTheSurfaceForAnyTimeSeedAndBeat() {
        for (time in listOf(0f, 1f, 37.5f, 999f)) {
            for (seed in listOf(0, 42, -9, 77777)) {
                for (beat in listOf(0f, 0.5f, 1f)) {
                    flowBlobs(time, seed, beat, beatReactivity = 1f).forEach { blob ->
                        assertTrue(blob.x in 0f..1f)
                        assertTrue(blob.y in 0f..1f)
                        assertTrue(blob.radius > 0f)
                    }
                }
            }
        }
    }

    @Test
    fun blobsWanderOverTime() {
        val early = flowBlobs(0f, seed = 42, beat = 0f, beatReactivity = 0f)
        val later = flowBlobs(10f, seed = 42, beat = 0f, beatReactivity = 0f)

        assertTrue(early.zip(later).any { (a, b) -> abs(a.x - b.x) > 1e-3f || abs(a.y - b.y) > 1e-3f })
    }

    @Test
    fun differentSeedsGiveDifferentRhythms() {
        val first = flowBlobs(5f, seed = 1, beat = 0f, beatReactivity = 0f)
        val second = flowBlobs(5f, seed = 2, beat = 0f, beatReactivity = 0f)

        assertNotEquals(first, second)
    }

    @Test
    fun sameSeedAndTimeAreDeterministic() {
        assertEquals(
            flowBlobs(5f, seed = 9, beat = 0.3f, beatReactivity = 0.6f),
            flowBlobs(5f, seed = 9, beat = 0.3f, beatReactivity = 0.6f),
        )
    }

    @Test
    fun beatGrowsEveryBlobByTheBeatRadius() {
        val calm = flowBlobs(3f, seed = 5, beat = 0f, beatReactivity = 1f)
        val hit = flowBlobs(3f, seed = 5, beat = 1f, beatReactivity = 1f)

        calm.zip(hit).forEach { (before, after) ->
            assertEquals(0.15f, after.radius - before.radius, 1e-5f)
        }
    }

    @Test
    fun zeroBeatReactivityIgnoresBeats() {
        val calm = flowBlobs(3f, seed = 5, beat = 0f, beatReactivity = 0f)
        val hit = flowBlobs(3f, seed = 5, beat = 1f, beatReactivity = 0f)

        assertEquals(calm, hit)
    }

    @Test
    fun blobsKeepTheirPaletteOrder() {
        assertEquals(
            (0 until FLOW_BLOB_COUNT).toList(),
            flowBlobs(0f, seed = 3, beat = 0f, beatReactivity = 0f).map { it.paletteIndex },
        )
    }

    @Test
    fun shapesAndColourBlendsStayWithinTheirRanges() {
        for (time in listOf(0f, 3.5f, 120f)) {
            flowBlobs(time, seed = 11, beat = 0f, beatReactivity = 0f).forEach { blob ->
                assertTrue(blob.stretchX in 0.4f..1.0f)
                assertTrue(blob.stretchY in 0.4f..1.0f)
                assertTrue(blob.blend in 0f..1f)
                assertTrue(blob.blendIndex != blob.paletteIndex)
            }
        }
    }

    @Test
    fun shapesChangeOverTimeSoTheyDoNotStayRound() {
        val early = flowBlobs(0f, seed = 4, beat = 0f, beatReactivity = 0f)
        val later = flowBlobs(20f, seed = 4, beat = 0f, beatReactivity = 0f)

        assertTrue(early.zip(later).any { (a, b) -> abs(a.stretchX - b.stretchX) > 1e-3f || abs(a.angle - b.angle) > 1e-3f })
    }

    @Test
    fun flowTimeAdvancesWithTempoAndSpeed() {
        val clock = LiveMeshClock()

        clock.advance(1f, LiveMeshConfig.Default.copy(speed = 2f), targetTempo = 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(2f, clock.flowTime, 1e-3f)
    }
}
