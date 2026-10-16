package com.music.vivi.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

class BeatDetectorScenarioTest {
    private fun pcm(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocate(samples.size * 2).order(ByteOrder.nativeOrder()).apply {
            samples.forEach { putShort(it) }
            flip()
        }

    @Test
    fun constantHalfScaleSignalHasHalfRms() {
        assertEquals(0.5f, pcm16Rms(pcm(16384, 16384, 16384, 16384)), 1e-4f)
    }

    @Test
    fun trailingOddByteIsIgnoredByRms() {
        val buffer = ByteBuffer.allocate(5).order(ByteOrder.nativeOrder())
            .putShort(Short.MAX_VALUE)
            .putShort(Short.MAX_VALUE)
            .put(1)
        buffer.flip()

        assertEquals(Short.MAX_VALUE / 32768f, pcm16Rms(buffer), 1e-4f)
    }

    @Test
    fun decayedPulseWithNegativeElapsedKeepsItsValue() {
        assertEquals(0.5f, decayedPulse(0.5f, elapsedMs = -100L), 0f)
    }

    @Test
    fun decayedPulseOfZeroStaysZero() {
        assertEquals(0f, decayedPulse(0f, elapsedMs = 300L), 0f)
    }

    @Test
    fun beatBumpIsHalfPeakHalfWayThroughAttack() {
        assertEquals(0.4f, beatBump(elapsedMs = (BEAT_ATTACK_MS / 2).toLong(), peak = 0.8f), 1e-3f)
    }

    @Test
    fun beatBumpDecaysExponentiallyAfterAttack() {
        val afterOne = beatBump(elapsedMs = (BEAT_ATTACK_MS + BEAT_PULSE_DECAY_MS).toLong(), peak = 1f)
        val afterTwo = beatBump(elapsedMs = (BEAT_ATTACK_MS + 2 * BEAT_PULSE_DECAY_MS).toLong(), peak = 1f)

        assertEquals(exp(-1f), afterOne, 1e-3f)
        assertEquals(exp(-2f), afterTwo, 1e-3f)
    }

    @Test
    fun beatBumpWithNegativePeakIsSilent() {
        assertEquals(0f, beatBump(elapsedMs = 300L, peak = -1f), 0f)
    }

    @Test
    fun quietBlockBelowAbsoluteMinimumNeverTriggersEvenIfRelativelyLouder() {
        val detector = BeatDetector()
        detector.onBlock(rms = 0.01f, timeMs = 0L)

        val pulse = detector.onBlock(rms = 0.019f, timeMs = 20L)

        assertEquals(0f, pulse, 0f)
    }

    @Test
    fun blockBelowThresholdRatioDoesNotTrigger() {
        val detector = BeatDetector()
        repeat(20) { detector.onBlock(rms = 0.5f, timeMs = it * 20L) }

        val pulse = detector.onBlock(rms = 0.55f, timeMs = 400L)

        assertEquals(0f, pulse, 0f)
    }

    @Test
    fun customThresholdRatioMakesDetectorMoreSensitive() {
        val sensitive = BeatDetector(thresholdRatio = 1.1f)
        val standard = BeatDetector()
        repeat(20) {
            sensitive.onBlock(rms = 0.5f, timeMs = it * 20L)
            standard.onBlock(rms = 0.5f, timeMs = it * 20L)
        }

        assertEquals(BEAT_PEAK, sensitive.onBlock(rms = 0.56f, timeMs = 400L), 1e-6f)
        assertEquals(0f, standard.onBlock(rms = 0.56f, timeMs = 400L), 0f)
    }

    @Test
    fun pulseDecaysAcrossQuietBlocksAfterABeat() {
        val detector = BeatDetector()
        repeat(20) { detector.onBlock(rms = 0.1f, timeMs = it * 20L) }
        detector.onBlock(rms = 0.9f, timeMs = 400L)

        val decayed = detector.onBlock(rms = 0.1f, timeMs = 900L)

        assertEquals(BEAT_PEAK * exp(-1f), decayed, 1e-3f)
    }

    @Test
    fun customMinimumIntervalAllowsCloserBeatsThanDefault() {
        val tight = BeatDetector(minIntervalMs = 100L)
        val standard = BeatDetector()
        val sequence = listOf(
            0L to 0.1f, 20L to 0.1f, 40L to 0.1f, 60L to 0.1f, 80L to 0.1f, 100L to 0.1f,
            120L to 0.1f, 140L to 0.1f, 160L to 0.1f, 180L to 0.1f, 200L to 0.1f, 220L to 0.1f,
            240L to 0.1f, 260L to 0.1f, 280L to 0.1f, 300L to 0.1f, 320L to 0.1f, 340L to 0.1f,
            360L to 0.1f, 380L to 0.1f,
            400L to 0.9f, 420L to 0.1f, 440L to 0.1f, 460L to 0.1f, 480L to 0.1f, 500L to 0.1f,
        )
        sequence.forEach { (time, rms) ->
            tight.onBlock(rms, time)
            standard.onBlock(rms, time)
        }

        val tightPulse = tight.onBlock(rms = 0.9f, timeMs = 520L)
        val standardPulse = standard.onBlock(rms = 0.9f, timeMs = 520L)

        assertEquals(BEAT_PEAK, tightPulse, 1e-6f)
        assertTrue(standardPulse < BEAT_PEAK)
    }

    @Test
    fun medianIsNotPulledByOneSlowInterval() {
        val bpm = estimateBpm(listOf(0L, 500L, 1000L, 1700L))

        assertEquals(120f, bpm!!, 1e-3f)
    }

    @Test
    fun intervalsLongerThanPlausibleMaximumAreDropped() {
        assertNull(estimateBpm(listOf(0L, 2000L, 2500L, 3000L)))
    }

    @Test
    fun negativeLyricIntervalIsTreatedAsFastestTempo() {
        assertEquals(1f, lyricTempoFromInterval(-500f), 0f)
    }

    @Test
    fun zeroLyricIntervalIsTreatedAsFastestTempo() {
        assertEquals(1f, lyricTempoFromInterval(0f), 0f)
    }
}
