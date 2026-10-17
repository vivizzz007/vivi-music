package com.music.vivi.ui.component

import com.music.vivi.ui.utils.LiveMeshConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMeshClockTest {
    private val neutral = LiveMeshConfig.Default.copy(speed = 1f, reverseDirection = false, lyricReactivity = 0.5f)

    // Angles per one second at tempo 1 with the default periods: anchor 80 s, fast 40 s, slow 60 s.
    private val anchorPerSecond = -4.5f
    private val fastPerSecond = 9f
    private val slowPerSecond = 6f

    @Test
    fun oneSecondAtNeutralTempoTurnsEachLayerByItsPeriodShare() {
        val clock = LiveMeshClock()

        clock.advance(dtSeconds = 1f, config = neutral, targetTempo = 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(anchorPerSecond, clock.anchor, 1e-3f)
        assertEquals(fastPerSecond, clock.fast, 1e-3f)
        assertEquals(slowPerSecond, clock.slow, 1e-3f)
    }

    @Test
    fun reverseDirectionFlipsEveryLayer() {
        val clock = LiveMeshClock()
        val reversed = neutral.copy(reverseDirection = true)

        clock.advance(1f, reversed, 1f, 0f, 0L)

        assertEquals(-anchorPerSecond, clock.anchor, 1e-3f)
        assertEquals(-fastPerSecond, clock.fast, 1e-3f)
        assertEquals(-slowPerSecond, clock.slow, 1e-3f)
    }

    @Test
    fun higherSpeedTurnsFasterThroughShorterPeriods() {
        val clock = LiveMeshClock()

        clock.advance(1f, neutral.copy(speed = 2f), 1f, 0f, 0L)

        assertEquals(anchorPerSecond * 2f, clock.anchor, 1e-3f)
        assertEquals(fastPerSecond * 2f, clock.fast, 1e-3f)
        assertEquals(slowPerSecond * 2f, clock.slow, 1e-3f)
    }

    @Test
    fun zeroDeltaLeavesEveryAngleUnchanged() {
        val clock = LiveMeshClock()

        clock.advance(0f, neutral, 2f, 1f, 0L)

        assertEquals(0f, clock.anchor, 0f)
        assertEquals(0f, clock.fast, 0f)
        assertEquals(0f, clock.slow, 0f)
    }

    @Test
    fun tempoSmoothsTowardTargetInsteadOfJumping() {
        val clock = LiveMeshClock()

        // Tempo starts at 1 and moves halfway to 2 in a quarter second (factor 0.5): 1.5.
        clock.advance(0.25f, neutral, targetTempo = 2f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(-360f * 1.5f * 0.25f * 1000f / 80000f, clock.anchor, 1e-3f)
    }

    @Test
    fun tempoConvergesToTargetAfterManyFrames() {
        val clock = LiveMeshClock()

        repeat(50) { clock.advance(0.1f, neutral, targetTempo = 2f, lyricTempo = 0f, nowMs = 0L) }
        val before = clock.fast
        clock.advance(0.1f, neutral, targetTempo = 2f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(1.8f, clock.fast - before, 1e-2f)
    }

    @Test
    fun tempoFallsBackWhenTargetDrops() {
        val clock = LiveMeshClock()
        clock.advance(0.25f, neutral, targetTempo = 2f, lyricTempo = 0f, nowMs = 0L)
        val tempoAfterRise = 1.5f

        clock.advance(0.25f, neutral, targetTempo = 0.5f, lyricTempo = 0f, nowMs = 0L)

        val tempoAfterFall = tempoAfterRise + (0.5f - tempoAfterRise) * 0.5f
        val expectedStep = 360f * tempoAfterFall * 0.25f * 1000f / 80000f
        assertEquals(-1.6875f - expectedStep, clock.anchor, 1e-3f)
    }

    @Test
    fun shimmerStaysStillWithoutLyricTempo() {
        val clock = LiveMeshClock()

        clock.advance(1f, neutral, 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(0f, clock.shimmer, 0f)
    }

    @Test
    fun shimmerAdvancesWithLyricTempoAndReactivity() {
        val clock = LiveMeshClock()

        clock.advance(1f, neutral, 1f, lyricTempo = 1f, nowMs = 0L)

        assertEquals(240f * 1f * 0.5f, clock.shimmer, 1e-3f)
    }

    @Test
    fun zeroLyricReactivityKeepsShimmerStill() {
        val clock = LiveMeshClock()

        clock.advance(1f, neutral.copy(lyricReactivity = 0f), 1f, lyricTempo = 1f, nowMs = 0L)

        assertEquals(0f, clock.shimmer, 0f)
    }

    @Test
    fun shimmerWrapsAroundFullTurn() {
        val clock = LiveMeshClock()
        val fullReactivity = neutral.copy(lyricReactivity = 1f)

        clock.advance(1f, fullReactivity, 1f, lyricTempo = 1f, nowMs = 0L)
        clock.advance(1f, fullReactivity, 1f, lyricTempo = 1f, nowMs = 0L)

        assertEquals(480f % 360f, clock.shimmer, 1e-3f)
    }

    @Test
    fun layersKeepTurningAcrossSeveralFrames() {
        val clock = LiveMeshClock()

        repeat(4) { clock.advance(0.25f, neutral, 1f, 0f, nowMs = 0L) }

        assertEquals(anchorPerSecond, clock.anchor, 1e-3f)
        assertEquals(fastPerSecond, clock.fast, 1e-3f)
        assertEquals(slowPerSecond, clock.slow, 1e-3f)
    }
}
