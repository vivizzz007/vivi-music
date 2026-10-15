package com.music.vivi.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

class BeatDetectionTest {
    private fun pcm(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocate(samples.size * 2).order(ByteOrder.nativeOrder()).apply {
            samples.forEach { putShort(it) }
            flip()
        }

    @Test
    fun decayedPulseKeepsValueWhenNoTimeHasPassed() {
        assertEquals(0.8f, decayedPulse(0.8f, elapsedMs = 0L), 0f)
    }

    @Test
    fun decayedPulseFallsToOneOverEAfterOneDecayPeriod() {
        assertEquals(exp(-1f), decayedPulse(1f, elapsedMs = BEAT_PULSE_DECAY_MS.toLong()), 1e-3f)
    }

    @Test
    fun decayedPulseIsClampedToUnitRange() {
        assertEquals(1f, decayedPulse(3f, elapsedMs = 0L), 0f)
        assertEquals(0f, decayedPulse(-2f, elapsedMs = 100L), 0f)
    }

    @Test
    fun silentBufferHasZeroRms() {
        assertEquals(0f, pcm16Rms(pcm(0, 0, 0, 0)), 0f)
    }

    @Test
    fun emptyBufferHasZeroRms() {
        assertEquals(0f, pcm16Rms(ByteBuffer.allocate(0)), 0f)
    }

    @Test
    fun fullScaleSquareWaveHasRmsOfOne() {
        val rms = pcm16Rms(pcm(Short.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, Short.MIN_VALUE))
        assertEquals(1f, rms, 1e-3f)
    }

    @Test
    fun rmsDoesNotConsumeTheInputBuffer() {
        val buffer = pcm(1000, -1000)
        pcm16Rms(buffer)
        assertEquals(4, buffer.remaining())
    }

    @Test
    fun loudBlockAfterQuietAudioTriggersPulse() {
        val detector = BeatDetector()
        repeat(20) { detector.onBlock(rms = 0.1f, timeMs = it * 20L) }

        val pulse = detector.onBlock(rms = 0.5f, timeMs = 400L)

        assertEquals(1f, pulse, 0f)
    }

    @Test
    fun steadyLoudnessDoesNotTriggerPulse() {
        val detector = BeatDetector()
        var pulse = 0f
        repeat(50) { pulse = detector.onBlock(rms = 0.3f, timeMs = it * 20L) }

        assertTrue(pulse < 0.5f)
    }

    @Test
    fun beatsWithinMinimumIntervalAreIgnored() {
        val detector = BeatDetector()
        repeat(20) { detector.onBlock(rms = 0.1f, timeMs = it * 20L) }
        detector.onBlock(rms = 0.5f, timeMs = 400L)

        val pulse = detector.onBlock(rms = 0.9f, timeMs = 400L + BEAT_MIN_INTERVAL_MS - 1)

        assertTrue(pulse < 0.95f)
    }

    @Test
    fun quietBlocksBelowMinimumRmsNeverTriggerPulse() {
        val detector = BeatDetector()
        val pulse = detector.onBlock(rms = BEAT_MIN_RMS / 2, timeMs = 0L)

        assertEquals(0f, pulse, 0f)
    }

    @Test
    fun resetForgetsPreviousLoudness() {
        val detector = BeatDetector()
        repeat(20) { detector.onBlock(rms = 0.9f, timeMs = it * 20L) }
        detector.reset()

        val pulse = detector.onBlock(rms = 0.2f, timeMs = 1000L)

        assertEquals(0f, pulse, 0f)
    }
}
