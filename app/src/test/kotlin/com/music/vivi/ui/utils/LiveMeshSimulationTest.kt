package com.music.vivi.ui.utils

import com.music.vivi.audio.BEAT_PEAK
import com.music.vivi.audio.beatImpulse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMeshSimulationTest {
    @Test
    fun beatPulseFollowsTheBeatBumpAtTheBeatPeriod() {
        // 120 BPM is a 500 ms beat, so 1000 ms lands exactly on the next beat.
        val onBeat = simulatedFrame(bpm = 120f, tempo = 1f, lyricTempo = 0f, nowMs = 1000L)
        val startOfBeat = simulatedFrame(bpm = 120f, tempo = 1f, lyricTempo = 0f, nowMs = 0L)

        assertEquals(BEAT_PEAK, startOfBeat.beat, 1e-6f)
        assertEquals(BEAT_PEAK, onBeat.beat, 1e-6f)
    }

    @Test
    fun beatPulseDecaysBetweenBeats() {
        val soon = simulatedFrame(bpm = 120f, tempo = 1f, lyricTempo = 0f, nowMs = 300L)
        val later = simulatedFrame(bpm = 120f, tempo = 1f, lyricTempo = 0f, nowMs = 450L)

        assertTrue(soon.beat > later.beat)
    }

    @Test
    fun speedBecomesTheTargetTempoWithinSupportedRange() {
        assertEquals(1.5f, simulatedFrame(120f, tempo = 1.5f, lyricTempo = 0f, nowMs = 0L).targetTempo, 0f)
        assertEquals(SIMULATED_TEMPO_MIN, simulatedFrame(120f, tempo = 0f, lyricTempo = 0f, nowMs = 0L).targetTempo, 0f)
        assertEquals(SIMULATED_TEMPO_MAX, simulatedFrame(120f, tempo = 9f, lyricTempo = 0f, nowMs = 0L).targetTempo, 0f)
    }

    @Test
    fun lyricPulseStaysSilentWithoutLyricTempo() {
        for (time in 0L..5000L step 250L) {
            assertEquals(0f, simulatedFrame(120f, tempo = 1f, lyricTempo = 0f, nowMs = time).lyric, 0f)
        }
    }

    @Test
    fun lyricPulseRisesAfterEachLineStartWhenLyricTempoIsSet() {
        // Lines repeat every two beats (1000 ms at 120 BPM); the bump starts at zero, so sample a little after the line starts.
        val afterLineStart = simulatedFrame(120f, tempo = 1f, lyricTempo = 1f, nowMs = 2400L)

        assertTrue(afterLineStart.lyric > 0f)
    }

    @Test
    fun bpmOutsideTheSupportedRangeIsClamped() {
        val tooSlow = simulatedFrame(bpm = 1f, tempo = 1f, lyricTempo = 0f, nowMs = 1000L)
        val atMinimum = simulatedFrame(bpm = SIMULATED_BPM_MIN, tempo = 1f, lyricTempo = 0f, nowMs = 1000L)

        assertEquals(atMinimum.beat, tooSlow.beat, 1e-6f)
    }

    @Test
    fun beatImpulseLandsAtFullPeakOnsetAndDecays() {
        assertEquals(BEAT_PEAK, beatImpulse(0L, BEAT_PEAK), 1e-6f)
        assertTrue(beatImpulse(100L, BEAT_PEAK) < beatImpulse(0L, BEAT_PEAK))
        assertEquals(0f, beatImpulse(-5L, BEAT_PEAK), 0f)
    }

    @Test
    fun lyricTempoIsClampedToUnitRange() {
        assertEquals(1f, simulatedFrame(120f, tempo = 1f, lyricTempo = 4f, nowMs = 0L).lyricTempo, 0f)
        assertEquals(0f, simulatedFrame(120f, tempo = 1f, lyricTempo = -1f, nowMs = 0L).lyricTempo, 0f)
    }
}
