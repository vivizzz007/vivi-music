package com.music.vivi.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class LiveMeshShakeTest {
    private val gravity = SHAKE_STANDARD_GRAVITY

    @Test
    fun restingPhoneDoesNotCountAsShake() {
        val detector = ShakeDetector()

        assertFalse(detector.onSample(0f, 0f, gravity, timeMs = 0L))
    }

    @Test
    fun walkingLevelMotionDoesNotCountAsShake() {
        val detector = ShakeDetector()

        assertFalse(detector.onSample(3f, 4f, gravity + 2f, timeMs = 0L))
    }

    @Test
    fun strongJerkAboveThresholdCountsAsShake() {
        val detector = ShakeDetector()
        val strength = SHAKE_THRESHOLD_G * gravity + 1f

        assertTrue(detector.onSample(strength, 0f, 0f, timeMs = 0L))
    }

    @Test
    fun sampleExactlyAtThresholdCountsAsShake() {
        val detector = ShakeDetector()

        assertTrue(detector.onSample(SHAKE_THRESHOLD_G * gravity, 0f, 0f, timeMs = 0L))
    }

    @Test
    fun sampleJustBelowThresholdDoesNotCount() {
        val detector = ShakeDetector()

        assertFalse(detector.onSample(SHAKE_THRESHOLD_G * gravity - 0.1f, 0f, 0f, timeMs = 0L))
    }

    @Test
    fun forceIsTakenFromAllThreeAxes() {
        val detector = ShakeDetector()
        val component = SHAKE_THRESHOLD_G * gravity / Math.sqrt(3.0).toFloat() + 0.5f

        assertTrue(detector.onSample(component, component, component, timeMs = 0L))
    }

    @Test
    fun negativeAxesCountTowardForce() {
        val detector = ShakeDetector()

        assertTrue(detector.onSample(-SHAKE_THRESHOLD_G * gravity, 0f, 0f, timeMs = 0L))
    }

    @Test
    fun shakesInsideMinimumIntervalAreIgnored() {
        val detector = ShakeDetector()
        val strong = SHAKE_THRESHOLD_G * gravity + 5f

        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = 1_000L))
        assertFalse(detector.onSample(strong, 0f, 0f, timeMs = 1_000L + SHAKE_MIN_INTERVAL_MS - 1L))
    }

    @Test
    fun shakeAfterMinimumIntervalCountsAgain() {
        val detector = ShakeDetector()
        val strong = SHAKE_THRESHOLD_G * gravity + 5f

        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = 1_000L))
        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = 1_000L + SHAKE_MIN_INTERVAL_MS))
    }

    @Test
    fun ignoredSamplesDoNotPushBackTheInterval() {
        val detector = ShakeDetector()
        val strong = SHAKE_THRESHOLD_G * gravity + 5f

        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = 0L))
        assertFalse(detector.onSample(strong, 0f, 0f, timeMs = 300L))
        assertFalse(detector.onSample(0f, 0f, gravity, timeMs = 500L))
        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = SHAKE_MIN_INTERVAL_MS))
    }

    @Test
    fun customThresholdChangesWhatCountsAsShake() {
        val gentle = ShakeDetector(thresholdG = 1.5f)

        assertTrue(gentle.onSample(1.5f * gravity, 0f, 0f, timeMs = 0L))
    }

    @Test
    fun customIntervalChangesHowOftenShakesCount() {
        val quick = ShakeDetector(minIntervalMs = 100L)
        val strong = SHAKE_THRESHOLD_G * gravity + 5f

        assertTrue(quick.onSample(strong, 0f, 0f, timeMs = 0L))
        assertTrue(quick.onSample(strong, 0f, 0f, timeMs = 100L))
    }

    @Test
    fun resetAllowsAShakeRightAfterAnotherOne() {
        val detector = ShakeDetector()
        val strong = SHAKE_THRESHOLD_G * gravity + 5f
        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = 1_000L))

        detector.reset()

        assertTrue(detector.onSample(strong, 0f, 0f, timeMs = 1_001L))
    }

    @Test
    fun burstIsFullRightAtTheShake() {
        assertEquals(1f, shakeBurst(0L), 0f)
    }

    @Test
    fun burstIsZeroBeforeTheShake() {
        assertEquals(0f, shakeBurst(-1L), 0f)
    }

    @Test
    fun burstFallsByOneOverEAfterOneDecayPeriod() {
        assertEquals(exp(-1f), shakeBurst(SHAKE_DECAY_MS.toLong()), 1e-4f)
    }

    @Test
    fun burstDecaysMonotonically() {
        var previous = shakeBurst(0L)
        for (elapsed in 50L..3_000L step 50L) {
            val current = shakeBurst(elapsed)
            assertTrue("at $elapsed", current <= previous)
            previous = current
        }
    }

    @Test
    fun spinMultiplierIsOneWhenCalm() {
        assertEquals(1f, shakeSpinMultiplier(-1L), 0f)
    }

    @Test
    fun spinMultiplierPeaksAtOnePlusSpinPeak() {
        assertEquals(1f + SHAKE_SPIN_PEAK, shakeSpinMultiplier(0L), 1e-6f)
    }

    @Test
    fun spinMultiplierReturnsTowardOneAfterLongDecay() {
        assertEquals(1f, shakeSpinMultiplier(60_000L), 1e-3f)
    }

    @Test
    fun glowIsZeroBeforeTheShakeAndPeaksAtTheShake() {
        assertEquals(0f, shakeGlowLevel(-1L), 0f)
        assertEquals(SHAKE_GLOW_PEAK, shakeGlowLevel(0L), 1e-6f)
    }
}
