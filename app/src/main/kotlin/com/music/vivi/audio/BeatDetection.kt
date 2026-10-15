/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.audio

import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sqrt

internal const val BEAT_PULSE_DECAY_MS = 450f
internal const val BEAT_ATTACK_MS = 140f
internal const val BEAT_MIN_INTERVAL_MS = 260L
internal const val BEAT_PEAK = 0.6f
private const val BEAT_RETRIGGER_MARGIN = 0.05f

// A beat swells in over BEAT_ATTACK_MS with a smoothstep curve (no sudden jump), then decays exponentially.
internal fun beatBump(elapsedMs: Long, peak: Float): Float {
    if (peak <= 0f || elapsedMs < 0L) return 0f
    if (elapsedMs < BEAT_ATTACK_MS) {
        val x = elapsedMs / BEAT_ATTACK_MS
        return peak * x * x * (3f - 2f * x)
    }
    return (peak * exp(-(elapsedMs - BEAT_ATTACK_MS) / BEAT_PULSE_DECAY_MS)).coerceIn(0f, peak)
}
internal const val BEAT_THRESHOLD_RATIO = 1.4f
internal const val BEAT_MIN_RMS = 0.05f

internal fun decayedPulse(pulse: Float, elapsedMs: Long, decayMs: Float = BEAT_PULSE_DECAY_MS): Float {
    if (pulse <= 0f || elapsedMs <= 0L) return pulse.coerceIn(0f, 1f)
    return (pulse * exp(-elapsedMs / decayMs)).coerceIn(0f, 1f)
}

internal fun pcm16Rms(buffer: ByteBuffer): Float {
    val shorts = buffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
    val count = shorts.remaining()
    if (count == 0) return 0f
    var sumSquares = 0.0
    while (shorts.hasRemaining()) {
        val sample = shorts.get() / 32768.0
        sumSquares += sample * sample
    }
    return sqrt(sumSquares / count).toFloat()
}

// Tracks a slowly moving average of loudness and reports a pulse whenever a block is clearly louder than
// that average. Times are audio-stream milliseconds so beat spacing stays correct even when playback is paused.
internal class BeatDetector(
    private val thresholdRatio: Float = BEAT_THRESHOLD_RATIO,
    private val minIntervalMs: Long = BEAT_MIN_INTERVAL_MS,
    private val averageSmoothing: Float = 0.05f,
) {
    private var average = 0f
    private var lastBeatMs: Long? = null
    private var lastBlockMs = 0L
    private var pulse = 0f

    fun onBlock(rms: Float, timeMs: Long): Float {
        pulse = decayedPulse(pulse, (timeMs - lastBlockMs).coerceAtLeast(0L))
        lastBlockMs = timeMs

        average = if (average == 0f) rms else average + (rms - average) * averageSmoothing

        val beatAllowed = lastBeatMs?.let { timeMs - it >= minIntervalMs } ?: true
        val isBeat = rms > BEAT_MIN_RMS && rms > average * thresholdRatio && beatAllowed
        if (isBeat) {
            lastBeatMs = timeMs
            pulse = maxOf(pulse, BEAT_PEAK)
        }
        return pulse
    }

    fun reset() {
        average = 0f
        lastBeatMs = null
        lastBlockMs = 0L
        pulse = 0f
    }
}

// Shared between writers (audio thread for beats, UI for lyric lines) and the UI draw loop. Reads decay each pulse
// by wall-clock time so the background settles smoothly after playback stops without another audio callback.
object AudioPulse {
    private class Channel {
        @Volatile private var rawPulse = 0f
        @Volatile private var updatedAtMs = 0L

        fun set(pulse: Float) {
            rawPulse = pulse
            updatedAtMs = SystemClock.elapsedRealtime()
        }

        fun trigger(strength: Float) {
            val now = SystemClock.elapsedRealtime()
            val current = decayedPulse(rawPulse, now - updatedAtMs)
            set(maxOf(current, strength.coerceIn(0f, 1f)))
        }

        fun current(): Float = decayedPulse(rawPulse, SystemClock.elapsedRealtime() - updatedAtMs)

        fun reset() = set(0f)
    }

    private val lyricChannel = Channel()

    @Volatile private var beatStartMs = 0L
    @Volatile private var beatPeak = 0f

    @Volatile var energy = 0f
        private set

    // The detector reports a decaying level every block; a sharp rise over the current bump means a new beat.
    fun publishBeat(pulse: Float, energy: Float) {
        val now = SystemClock.elapsedRealtime()
        if (pulse > beat() + BEAT_RETRIGGER_MARGIN) {
            beatStartMs = now
            beatPeak = pulse
        }
        this.energy = energy
    }

    @Volatile private var lastLyricAtMs = 0L
    @Volatile private var lyricIntervalMs = 0f

    fun triggerLyricLine(strength: Float = LYRIC_LINE_STRENGTH) {
        val now = SystemClock.elapsedRealtime()
        if (lastLyricAtMs != 0L) {
            val interval = (now - lastLyricAtMs).toFloat()
            lyricIntervalMs = if (lyricIntervalMs == 0f) interval else lyricIntervalMs + (interval - lyricIntervalMs) * 0.3f
        }
        lastLyricAtMs = now
        lyricChannel.trigger(strength)
    }

    fun beat(): Float {
        val peak = beatPeak
        if (peak <= 0f) return 0f
        return beatBump(SystemClock.elapsedRealtime() - beatStartMs, peak)
    }

    fun lyric(): Float = lyricChannel.current()

    // 0 for slow or stale lyrics, 1 for lines changing faster than LYRIC_FAST_INTERVAL_MS.
    fun lyricTempo(): Float {
        val last = lastLyricAtMs
        if (last == 0L || SystemClock.elapsedRealtime() - last > LYRIC_TEMPO_TIMEOUT_MS) return 0f
        return lyricTempoFromInterval(lyricIntervalMs)
    }

    fun reset() {
        beatPeak = 0f
        beatStartMs = 0L
        lyricChannel.reset()
        energy = 0f
        lastLyricAtMs = 0L
        lyricIntervalMs = 0f
    }
}

const val LYRIC_LINE_STRENGTH = 0.6f
internal const val LYRIC_FAST_INTERVAL_MS = 1000f
internal const val LYRIC_SLOW_INTERVAL_MS = 5000f
internal const val LYRIC_TEMPO_TIMEOUT_MS = 8000L

internal fun lyricTempoFromInterval(intervalMs: Float): Float =
    ((LYRIC_SLOW_INTERVAL_MS - intervalMs) / (LYRIC_SLOW_INTERVAL_MS - LYRIC_FAST_INTERVAL_MS)).coerceIn(0f, 1f)
