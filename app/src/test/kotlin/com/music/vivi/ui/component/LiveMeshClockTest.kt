package com.music.vivi.ui.component

import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.SHAKE_DECAY_MS
import com.music.vivi.ui.utils.SHAKE_GLOW_PEAK
import com.music.vivi.ui.utils.SHAKE_SPIN_PEAK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun shakeGlowIsZeroWithoutAShake() {
        val clock = LiveMeshClock()

        clock.advance(1f, neutral, 1f, 0f, nowMs = 5_000L)

        assertEquals(0f, clock.shakeGlow, 0f)
    }

    @Test
    fun shakeGlowPeaksRightAfterTheShake() {
        val clock = LiveMeshClock()
        clock.triggerShake(1_000L)

        clock.advance(0f, neutral, 1f, 0f, nowMs = 1_000L)

        assertEquals(SHAKE_GLOW_PEAK, clock.shakeGlow, 1e-6f)
    }

    @Test
    fun shakeSpinsLayersFasterThanCalmFrames() {
        val calm = LiveMeshClock()
        val shaken = LiveMeshClock()
        shaken.triggerShake(0L)

        calm.advance(1f, neutral, 1f, 0f, nowMs = 0L)
        shaken.advance(1f, neutral, 1f, 0f, nowMs = 0L)

        assertEquals(anchorPerSecond * (1f + SHAKE_SPIN_PEAK), shaken.anchor, 1e-3f)
        assertEquals(fastPerSecond * (1f + SHAKE_SPIN_PEAK), shaken.fast, 1e-3f)
        assertEquals(anchorPerSecond, calm.anchor, 1e-3f)
    }

    @Test
    fun shakeSpinFadesBackToNormalSpeed() {
        val clock = LiveMeshClock()
        clock.triggerShake(0L)
        clock.advance(0f, neutral, 1f, 0f, nowMs = 0L)

        // Far enough after the shake that the burst has effectively decayed.
        clock.advance(1f, neutral, 1f, 0f, nowMs = 60_000L)

        assertEquals(anchorPerSecond, clock.anchor, 1e-2f)
        assertEquals(0f, clock.shakeGlow, 1e-6f)
    }

    @Test
    fun shakeSpinFollowsReverseDirection() {
        val clock = LiveMeshClock()
        clock.triggerShake(0L)

        clock.advance(1f, neutral.copy(reverseDirection = true), 1f, 0f, nowMs = 0L)

        assertEquals(-anchorPerSecond * (1f + SHAKE_SPIN_PEAK), clock.anchor, 1e-3f)
        assertEquals(-fastPerSecond * (1f + SHAKE_SPIN_PEAK), clock.fast, 1e-3f)
    }

    @Test
    fun shakeSpinScalesWithAutoTempo() {
        val clock = LiveMeshClock()
        clock.triggerShake(0L)

        // Tempo starts at 1 and is pulled to 0.5 for this frame only, so the burst multiplies that tempo.
        clock.advance(0.25f, neutral, targetTempo = 0.5f, lyricTempo = 0f, nowMs = 0L)
        val tempo = 1f + (0.5f - 1f) * 0.5f
        val expected = -360f * tempo * (1f + SHAKE_SPIN_PEAK) * 0.25f * 1000f / 80000f

        assertEquals(expected, clock.anchor, 1e-3f)
    }

    @Test
    fun shakeDoesNotChangeShimmerWithoutLyricTempo() {
        val clock = LiveMeshClock()
        clock.triggerShake(0L)

        clock.advance(1f, neutral, 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(0f, clock.shimmer, 0f)
    }

    @Test
    fun shakeGlowDecaysAcrossFramesMonotonically() {
        val clock = LiveMeshClock()
        clock.triggerShake(0L)
        var previous = Float.MAX_VALUE

        for (now in 0L..2_000L step 100L) {
            clock.advance(0.1f, neutral, 1f, 0f, nowMs = now)
            assertTrue("at $now", clock.shakeGlow <= previous)
            previous = clock.shakeGlow
        }
    }

    @Test
    fun shakeGlowFadesAfterThePeak() {
        val clock = LiveMeshClock()
        clock.triggerShake(1_000L)

        clock.advance(0f, neutral, 1f, 0f, nowMs = 1_000L + SHAKE_DECAY_MS.toLong())

        assertEquals(SHAKE_GLOW_PEAK * Math.exp(-1.0).toFloat(), clock.shakeGlow, 1e-4f)
    }

    @Test
    fun shakeIgnoresTimesBeforeTheShake() {
        val clock = LiveMeshClock()
        clock.triggerShake(1_000L)

        clock.advance(0f, neutral, 1f, 0f, nowMs = 900L)

        assertEquals(0f, clock.shakeGlow, 0f)
    }

    @Test
    fun repeatedShakeRestartsTheBurst() {
        val clock = LiveMeshClock()
        clock.triggerShake(1_000L)
        clock.advance(0f, neutral, 1f, 0f, nowMs = 2_000L)
        assertTrue(clock.shakeGlow < SHAKE_GLOW_PEAK)

        clock.triggerShake(2_000L)
        clock.advance(0f, neutral, 1f, 0f, nowMs = 2_000L)

        assertEquals(SHAKE_GLOW_PEAK, clock.shakeGlow, 1e-6f)
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
