/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.vivi.audio.BEAT_PEAK
import com.music.vivi.audio.beatBump
import com.music.vivi.audio.lyricRhythmPulse
import kotlin.math.roundToLong

// What the clock needs for one frame; the player fills it from the audio analysis, the preview from its simulation.
internal data class LiveMeshFrameInput(
    val beat: Float,
    val lyric: Float,
    val lyricTempo: Float,
    val targetTempo: Float,
    val nowMs: Long,
)

internal const val SIMULATED_BPM_MIN = 60f
internal const val SIMULATED_BPM_MAX = 180f
internal const val SIMULATED_TEMPO_MIN = 0.5f
internal const val SIMULATED_TEMPO_MAX = 2f

// Observable so the preview's controls and the frame loop read the same values.
internal class LiveMeshSimulation {
    var bpm by mutableFloatStateOf(120f)
    var tempo by mutableFloatStateOf(1f)
    var lyricTempo by mutableFloatStateOf(0.5f)
    var artworkHighlight by mutableFloatStateOf(0.4f)
    var playing by mutableStateOf(true)
}

// Beats land on multiples of the beat period and lyric lines every two beats, like a song with a steady tempo.
internal fun simulatedFrame(bpm: Float, tempo: Float, lyricTempo: Float, nowMs: Long): LiveMeshFrameInput {
    val beatPeriodMs = (60_000f / bpm.coerceIn(SIMULATED_BPM_MIN, SIMULATED_BPM_MAX)).roundToLong().coerceAtLeast(1L)
    val linePeriodMs = beatPeriodMs * 2
    return LiveMeshFrameInput(
        beat = beatBump(nowMs % beatPeriodMs, BEAT_PEAK),
        lyric = lyricRhythmPulse(nowMs % linePeriodMs, linePeriodMs.toFloat(), lyricTempo),
        lyricTempo = lyricTempo.coerceIn(0f, 1f),
        targetTempo = tempo.coerceIn(SIMULATED_TEMPO_MIN, SIMULATED_TEMPO_MAX),
        nowMs = nowMs,
    )
}
