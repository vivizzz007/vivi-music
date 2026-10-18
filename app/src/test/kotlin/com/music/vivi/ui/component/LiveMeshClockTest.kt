package com.music.vivi.ui.component

import com.music.vivi.ui.utils.BEAT_SPEED_BOOST
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LiveMeshMotion
import com.music.vivi.ui.utils.PULSE_FALL_PER_SECOND
import com.music.vivi.ui.utils.PULSE_RISE_PER_SECOND
import com.music.vivi.ui.utils.TEMPO_FALL_PER_SECOND
import com.music.vivi.ui.utils.TEMPO_RISE_PER_SECOND
import com.music.vivi.ui.utils.beatSpeedFactor
import com.music.vivi.ui.utils.followTowards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class LiveMeshClockTest {
    private val neutral = LiveMeshConfig.Default.copy(speed = 1f, reverseDirection = false, lyricReactivity = 0.5f, beatReactivity = 0f)

    // Angles per one second at tempo 1 with the default periods: anchor 80 s, fast 40 s, slow 60 s.
    private val anchorPerSecond = -4.5f
    private val fastPerSecond = 9f
    private val slowPerSecond = 6f

    // Tempo after 0.25 s of rising from 1 toward 2 at the rise rate.
    private val risenTempo = 1f + (2f - 1f) * (1f - exp(-TEMPO_RISE_PER_SECOND * 0.25f))

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

        clock.advance(0.25f, neutral, targetTempo = 2f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(anchorPerSecond * risenTempo * 0.25f, clock.anchor, 1e-3f)
    }

    @Test
    fun tempoRisesFasterThanItFalls() {
        val rise = followTowards(current = 1f, target = 2f, risePerSecond = TEMPO_RISE_PER_SECOND, fallPerSecond = TEMPO_FALL_PER_SECOND, dtSeconds = 0.25f) - 1f
        val fall = 2f - followTowards(current = 2f, target = 1f, risePerSecond = TEMPO_RISE_PER_SECOND, fallPerSecond = TEMPO_FALL_PER_SECOND, dtSeconds = 0.25f)

        assertTrue(rise > fall)
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
        val riseStep = anchorPerSecond * risenTempo * 0.25f

        clock.advance(0.25f, neutral, targetTempo = 0.5f, lyricTempo = 0f, nowMs = 0L)

        val tempoAfterFall = risenTempo + (0.5f - risenTempo) * (1f - exp(-TEMPO_FALL_PER_SECOND * 0.25f))
        assertEquals(riseStep + anchorPerSecond * tempoAfterFall * 0.25f, clock.anchor, 1e-3f)
    }

    @Test
    fun motionTempoRiseSetsHowFastTheRotationPicksUp() {
        val clock = LiveMeshClock()
        val fastRise = LiveMeshMotion.Default.copy(tempoRise = 10f)

        clock.advance(0.25f, neutral, targetTempo = 2f, lyricTempo = 0f, nowMs = 0L, motion = fastRise)

        val tempo = 1f + (2f - 1f) * (1f - exp(-10f * 0.25f))
        assertEquals(anchorPerSecond * tempo * 0.25f, clock.anchor, 1e-3f)
    }

    @Test
    fun zeroBeatBoostKeepsSpeedFixedOnBeats() {
        val clock = LiveMeshClock()
        clock.followPulses(dtSeconds = 1f, beatTarget = 1f, lyricTarget = 0f)

        clock.advance(
            1f,
            neutral.copy(beatReactivity = 1f),
            targetTempo = 1f,
            lyricTempo = 0f,
            nowMs = 0L,
            motion = LiveMeshMotion.Default.copy(beatSpeedBoost = 0f),
        )

        assertEquals(anchorPerSecond, clock.anchor, 1e-3f)
    }

    @Test
    fun motionPulseRiseSetsHowFastBeatsJump() {
        val clock = LiveMeshClock()
        val slowRise = LiveMeshMotion.Default.copy(pulseRise = 1f)

        clock.followPulses(dtSeconds = 0.5f, beatTarget = 1f, lyricTarget = 0f, motion = slowRise)

        assertEquals(1f - exp(-1f * 0.5f), clock.beatPulse, 1e-5f)
    }

    @Test
    fun beatSpeedsUpRotationWhenReactive() {
        val clock = LiveMeshClock()
        clock.followPulses(dtSeconds = 1f, beatTarget = 1f, lyricTarget = 0f)

        clock.advance(1f, neutral.copy(beatReactivity = 1f), targetTempo = 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(anchorPerSecond * (1f + BEAT_SPEED_BOOST), clock.anchor, 1e-3f)
    }

    @Test
    fun beatDoesNotChangeSpeedWithZeroReactivity() {
        val clock = LiveMeshClock()
        clock.followPulses(dtSeconds = 1f, beatTarget = 1f, lyricTarget = 1f)

        clock.advance(1f, neutral.copy(beatReactivity = 0f), targetTempo = 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(anchorPerSecond, clock.anchor, 1e-3f)
    }

    @Test
    fun beatPulseRisesQuicklyAndSettlesSlowly() {
        val clock = LiveMeshClock()

        clock.followPulses(dtSeconds = 0.05f, beatTarget = 1f, lyricTarget = 0f)
        val risen = clock.beatPulse
        clock.followPulses(dtSeconds = 0.05f, beatTarget = 0f, lyricTarget = 0f)

        assertEquals(1f - exp(-PULSE_RISE_PER_SECOND * 0.05f), risen, 1e-5f)
        assertEquals(risen * exp(-PULSE_FALL_PER_SECOND * 0.05f), clock.beatPulse, 1e-5f)
    }

    @Test
    fun pulsesStayWithinUnitRangeForOutOfRangeTargets() {
        val clock = LiveMeshClock()

        clock.followPulses(dtSeconds = 1f, beatTarget = 5f, lyricTarget = -2f)

        assertEquals(1f, clock.beatPulse, 1e-5f)
        assertEquals(0f, clock.lyricPulse, 0f)
    }

    @Test
    fun followTowardsReachesTargetAndIgnoresZeroStep() {
        assertEquals(0.4f, followTowards(current = 0.4f, target = 1f, risePerSecond = 40f, fallPerSecond = 2.5f, dtSeconds = 0f), 0f)
        assertEquals(1f, followTowards(current = 0f, target = 1f, risePerSecond = 40f, fallPerSecond = 2.5f, dtSeconds = 10f), 1e-6f)
    }

    @Test
    fun followTowardsGivesTheSameResultRegardlessOfFrameSplit() {
        val halfSteps = followTowards(followTowards(0f, 1f, 3f, 1.8f, 0.1f), 1f, 3f, 1.8f, 0.1f)
        val wholeStep = followTowards(0f, 1f, 3f, 1.8f, 0.2f)

        assertEquals(wholeStep, halfSteps, 1e-5f)
    }

    @Test
    fun beatSpeedFactorScalesWithPulseAndReactivityAndClamps() {
        assertEquals(1f, beatSpeedFactor(beat = 1f, beatReactivity = 0f), 0f)
        assertEquals(1f + 0.5f * BEAT_SPEED_BOOST * 0.5f, beatSpeedFactor(beat = 0.5f, beatReactivity = 0.5f), 1e-6f)
        assertEquals(1f + BEAT_SPEED_BOOST, beatSpeedFactor(beat = 3f, beatReactivity = 1f), 1e-6f)
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
