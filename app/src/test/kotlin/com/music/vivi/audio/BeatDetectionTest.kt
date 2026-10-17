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

        assertEquals(BEAT_PEAK, pulse, 0f)
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
    fun steadyHalfSecondBeatsEstimateOneTwentyBpm() {
        val beats = (0..10).map { it * 500L }

        assertEquals(120f, estimateBpm(beats)!!, 1e-3f)
    }

    @Test
    fun doubleTimeReadingIsFoldedIntoComfortableRange() {
        val beats = (0..10).map { it * 250L }

        assertEquals(120f, estimateBpm(beats)!!, 1e-3f)
    }

    @Test
    fun slowBeatsAreFoldedUpIntoComfortableRange() {
        val beats = (0..10).map { it * 1000L }

        assertEquals(120f, estimateBpm(beats)!!, 1e-3f)
    }

    @Test
    fun tooFewPlausibleIntervalsGiveNoEstimate() {
        assertEquals(null, estimateBpm(listOf(0L, 500L)))
        assertEquals(null, estimateBpm(emptyList()))
    }

    @Test
    fun implausiblyShortIntervalsAreIgnoredForEstimate() {
        val beats = (0..10).map { it * 100L }

        assertEquals(null, estimateBpm(beats))
    }

    @Test
    fun beatBumpStartsAtZero() {
        assertEquals(0f, beatBump(0L, peak = BEAT_PEAK), 0f)
    }

    @Test
    fun beatBumpReachesPeakAtEndOfAttack() {
        assertEquals(BEAT_PEAK, beatBump(BEAT_ATTACK_MS.toLong(), peak = BEAT_PEAK), 1e-4f)
    }

    @Test
    fun beatBumpRisesMonotonicallyDuringAttack() {
        val samples = (0..BEAT_ATTACK_MS.toInt() step 10).map { beatBump(it.toLong(), peak = BEAT_PEAK) }

        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun beatBumpDecaysAfterAttack() {
        val atPeak = beatBump(BEAT_ATTACK_MS.toLong(), peak = BEAT_PEAK)
        val later = beatBump((BEAT_ATTACK_MS + BEAT_PULSE_DECAY_MS).toLong(), peak = BEAT_PEAK)

        assertEquals(atPeak * exp(-1f), later, 1e-3f)
    }

    @Test
    fun beatBumpIsZeroWithoutPeakOrNegativeTime() {
        assertEquals(0f, beatBump(100L, peak = 0f), 0f)
        assertEquals(0f, beatBump(-5L, peak = BEAT_PEAK), 0f)
    }

    @Test
    fun lyricTempoIsFullWhenLinesChangeFasterThanFastInterval() {
        assertEquals(1f, lyricTempoFromInterval(500f), 0f)
        assertEquals(1f, lyricTempoFromInterval(LYRIC_FAST_INTERVAL_MS), 0f)
    }

    @Test
    fun lyricTempoIsZeroWhenLinesAreSlowerThanSlowInterval() {
        assertEquals(0f, lyricTempoFromInterval(LYRIC_SLOW_INTERVAL_MS), 0f)
        assertEquals(0f, lyricTempoFromInterval(9000f), 0f)
    }

    @Test
    fun lyricTempoScalesLinearlyBetweenFastAndSlowIntervals() {
        val midpoint = (LYRIC_FAST_INTERVAL_MS + LYRIC_SLOW_INTERVAL_MS) / 2
        assertEquals(0.5f, lyricTempoFromInterval(midpoint), 1e-6f)
    }

    @Test
    fun resetForgetsPreviousLoudness() {
        val detector = BeatDetector()
        repeat(20) { detector.onBlock(rms = 0.9f, timeMs = it * 20L) }
        detector.reset()

        val pulse = detector.onBlock(rms = 0.2f, timeMs = 1000L)

        assertEquals(0f, pulse, 0f)
    }

    @Test
    fun lyricRhythmIsSilentWithoutTempo() {
        assertEquals(0f, lyricRhythmPulse(sinceLineMs = 300L, intervalMs = 1000f, tempo = 0f), 0f)
    }

    @Test
    fun lyricRhythmIsSilentWithoutMeasuredInterval() {
        assertEquals(0f, lyricRhythmPulse(sinceLineMs = 300L, intervalMs = 0f, tempo = 1f), 0f)
    }

    @Test
    fun lyricRhythmIsSilentBeforeAnyLineWasSeen() {
        assertEquals(0f, lyricRhythmPulse(sinceLineMs = -1L, intervalMs = 1000f, tempo = 1f), 0f)
    }

    @Test
    fun lyricRhythmPeaksOnceEveryLineInterval() {
        val peakAfterFirstLine = lyricRhythmPulse(sinceLineMs = 220L, intervalMs = 1000f, tempo = 1f)
        val peakAfterSecondLine = lyricRhythmPulse(sinceLineMs = 1220L, intervalMs = 1000f, tempo = 1f)

        assertEquals(LYRIC_RHYTHM_PEAK, peakAfterFirstLine, 1e-6f)
        assertEquals(peakAfterFirstLine, peakAfterSecondLine, 1e-6f)
    }

    @Test
    fun lyricRhythmIsLowestRightAfterALineChange() {
        val atLineChange = lyricRhythmPulse(sinceLineMs = 0L, intervalMs = 1000f, tempo = 1f)
        val midInterval = lyricRhythmPulse(sinceLineMs = 500L, intervalMs = 1000f, tempo = 1f)

        assertEquals(0f, atLineChange, 0f)
        assertEquals(true, midInterval > atLineChange)
    }

    @Test
    fun lyricRhythmScalesWithTempo() {
        val full = lyricRhythmPulse(sinceLineMs = 220L, intervalMs = 1000f, tempo = 1f)
        val half = lyricRhythmPulse(sinceLineMs = 220L, intervalMs = 1000f, tempo = 0.5f)

        assertEquals(full * 0.5f, half, 1e-6f)
    }
}
