package com.music.vivi.audio

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.exp

class AudioPulseTest {
    private val defaultNow = AudioPulse.nowMs
    private var now = START_MS

    @Before
    fun setUp() {
        AudioPulse.nowMs = { now }
        AudioPulse.reset()
    }

    @After
    fun tearDown() {
        AudioPulse.reset()
        AudioPulse.nowMs = defaultNow
    }

    // Zero is the "never happened" sentinel for lyric lines, so tests start the clock away from it.
    private fun at(offsetMs: Long) {
        now = START_MS + offsetMs
    }

    @Test
    fun lyricLineIsFullStrengthAtTheMomentItStarts() {
        AudioPulse.triggerLyricLine(0.6f)

        assertEquals(0.6f, AudioPulse.lyric(), 1e-6f)
    }

    @Test
    fun lyricLineDecaysOverTime() {
        AudioPulse.triggerLyricLine(0.6f)
        at(500L)

        assertEquals(0.6f * exp(-1f), AudioPulse.lyric(), 1e-4f)
    }

    @Test
    fun overlappingLyricLinesKeepTheStrongerPulse() {
        AudioPulse.triggerLyricLine(0.6f)
        AudioPulse.triggerLyricLine(0.3f)

        assertEquals(0.6f, AudioPulse.lyric(), 1e-6f)

        AudioPulse.triggerLyricLine(0.9f)

        assertEquals(0.9f, AudioPulse.lyric(), 1e-6f)
    }

    @Test
    fun lyricStrengthAboveOneIsClampedToOne() {
        AudioPulse.triggerLyricLine(2f)

        assertEquals(1f, AudioPulse.lyric(), 0f)
    }

    @Test
    fun negativeLyricStrengthIsClampedToZero() {
        AudioPulse.triggerLyricLine(-1f)

        assertEquals(0f, AudioPulse.lyric(), 0f)
    }

    @Test
    fun defaultLyricStrengthIsTheConfiguredConstant() {
        AudioPulse.triggerLyricLine()

        assertEquals(LYRIC_LINE_STRENGTH, AudioPulse.lyric(), 1e-6f)
    }

    @Test
    fun lyricTempoIsZeroBeforeAnyLine() {
        assertEquals(0f, AudioPulse.lyricTempo(), 0f)
    }

    @Test
    fun lyricTempoIsZeroAfterOnlyOneLineBecauseNoIntervalIsKnownYet() {
        AudioPulse.triggerLyricLine()

        assertEquals(0f, AudioPulse.lyricTempo(), 0f)
    }

    @Test
    fun lyricTempoIsFullWhenLinesComeOneSecondApart() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()

        assertEquals(1f, AudioPulse.lyricTempo(), 1e-6f)
    }

    @Test
    fun lyricTempoIsZeroWhenLinesComeSlowerThanSlowInterval() {
        AudioPulse.triggerLyricLine()
        at(6000L)
        AudioPulse.triggerLyricLine()

        assertEquals(0f, AudioPulse.lyricTempo(), 0f)
    }

    @Test
    fun lyricTempoSmoothsIntervalsWithAMovingAverage() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()
        at(4000L)
        AudioPulse.triggerLyricLine()

        // 1000 ms, then a 3000 ms gap: 1000 + (3000 - 1000) * 0.3 = 1600 ms.
        val expected = (LYRIC_SLOW_INTERVAL_MS - 1600f) / (LYRIC_SLOW_INTERVAL_MS - LYRIC_FAST_INTERVAL_MS)
        assertEquals(expected, AudioPulse.lyricTempo(), 1e-5f)
    }

    @Test
    fun lyricTempoStaysActiveUntilTimeoutThenFallsSilent() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()

        at(1000L + LYRIC_TEMPO_TIMEOUT_MS)
        assertEquals(1f, AudioPulse.lyricTempo(), 1e-6f)

        at(1000L + LYRIC_TEMPO_TIMEOUT_MS + 1L)
        assertEquals(0f, AudioPulse.lyricTempo(), 0f)
    }

    @Test
    fun lyricRhythmIsSilentBeforeAnyLine() {
        assertEquals(0f, AudioPulse.lyricRhythm(), 0f)
    }

    @Test
    fun lyricRhythmIsSilentWhileOnlyOneLineIsKnown() {
        AudioPulse.triggerLyricLine()
        at(300L)

        assertEquals(0f, AudioPulse.lyricRhythm(), 0f)
    }

    @Test
    fun lyricRhythmPulsesBetweenLinesAtMeasuredInterval() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()
        at(1220L)

        assertEquals(LYRIC_RHYTHM_PEAK, AudioPulse.lyricRhythm(), 1e-6f)
    }

    @Test
    fun lyricRhythmRepeatsOnEveryMeasuredPeriod() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()
        at(2220L)

        assertEquals(LYRIC_RHYTHM_PEAK, AudioPulse.lyricRhythm(), 1e-6f)
    }

    @Test
    fun lyricRhythmStopsWhenLyricsGoStale() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()
        at(1000L + LYRIC_TEMPO_TIMEOUT_MS + 1L)

        assertEquals(0f, AudioPulse.lyricRhythm(), 0f)
    }

    @Test
    fun lyricPulseIsTheStrongerOfLineAndRhythm() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()
        at(1220L)

        val line = AudioPulse.lyric()
        val rhythm = AudioPulse.lyricRhythm()

        assertTrue(line > rhythm)
        assertEquals(line, AudioPulse.lyricPulse(), 1e-6f)
    }

    @Test
    fun lyricPulseFallsBackToLineWhenRhythmIsSilent() {
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()
        at(3000L)

        assertEquals(0f, AudioPulse.lyricRhythm(), 0f)
        assertEquals(AudioPulse.lyric(), AudioPulse.lyricPulse(), 1e-6f)
    }

    @Test
    fun beatIsSilentBeforeAnyBeatWasPublished() {
        assertEquals(0f, AudioPulse.beat(), 0f)
    }

    @Test
    fun publishedBeatSwellsInAndDecays() {
        AudioPulse.publishBeat(0.8f, energy = 0.3f)

        assertEquals(0f, AudioPulse.beat(), 0f)

        at(220L)
        assertEquals(0.8f, AudioPulse.beat(), 1e-6f)

        at(720L)
        assertEquals(0.8f * exp(-1f), AudioPulse.beat(), 1e-4f)
    }

    @Test
    fun publishStoresLatestEnergy() {
        AudioPulse.publishBeat(0f, energy = 0.5f)
        AudioPulse.publishBeat(0f, energy = 0.1f)

        assertEquals(0.1f, AudioPulse.energy, 0f)
    }

    @Test
    fun weakerPulseInsideCurrentBumpDoesNotRetrigger() {
        AudioPulse.publishBeat(0.8f, energy = 0f)
        at(100L)
        AudioPulse.publishBeat(0.3f, energy = 0f)
        at(220L)

        assertEquals(0.8f, AudioPulse.beat(), 1e-6f)
    }

    @Test
    fun sharpRiseAfterDecayRetriggersBeat() {
        AudioPulse.publishBeat(0.8f, energy = 0f)
        at(400L)
        AudioPulse.publishBeat(0.8f, energy = 0f)

        assertEquals(0f, AudioPulse.beat(), 0f)

        at(620L)
        assertEquals(0.8f, AudioPulse.beat(), 1e-6f)
    }

    @Test
    fun bpmIsNullWithTooFewBeats() {
        AudioPulse.publishBeat(0.8f, energy = 0f)
        at(500L)
        AudioPulse.publishBeat(0.8f, energy = 0f)
        at(1000L)
        AudioPulse.publishBeat(0.8f, energy = 0f)

        assertNull(AudioPulse.bpm())
    }

    @Test
    fun steadyHalfSecondBeatsReadAsOneTwentyBpm() {
        publishBeatsEvery(500L, count = 4)

        assertEquals(120f, AudioPulse.bpm()!!, 1e-3f)
    }

    @Test
    fun slowSecondBeatsAreFoldedIntoComfortableTempo() {
        publishBeatsEvery(1000L, count = 4)

        assertEquals(120f, AudioPulse.bpm()!!, 1e-3f)
    }

    @Test
    fun fastBeatsAreFoldedDownIntoComfortableTempo() {
        publishBeatsEvery(300L, count = 6)

        assertEquals(100f, AudioPulse.bpm()!!, 1e-3f)
    }

    @Test
    fun resetClearsLyricsBeatsAndTempo() {
        publishBeatsEvery(500L, count = 4)
        AudioPulse.triggerLyricLine()
        at(1000L)
        AudioPulse.triggerLyricLine()

        AudioPulse.reset()

        assertEquals(0f, AudioPulse.lyric(), 0f)
        assertEquals(0f, AudioPulse.lyricTempo(), 0f)
        assertEquals(0f, AudioPulse.lyricRhythm(), 0f)
        assertEquals(0f, AudioPulse.beat(), 0f)
        assertEquals(0f, AudioPulse.energy, 0f)
        assertNull(AudioPulse.bpm())
    }

    private fun publishBeatsEvery(intervalMs: Long, count: Int) {
        repeat(count) {
            AudioPulse.publishBeat(0.8f, energy = 0f)
            now += intervalMs
        }
    }

    private companion object {
        const val START_MS = 10_000L
    }
}
