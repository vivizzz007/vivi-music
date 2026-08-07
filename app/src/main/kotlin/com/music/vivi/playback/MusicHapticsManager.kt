/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Beat-synchronized haptic feedback ("Music Haptics"), similar in spirit to the
 * feature in SuvMusic (github.com/suvojeet-sengupta/SuvMusic) - but implemented
 * differently to fit this app's architecture. SuvMusic samples raw PCM directly
 * from its own custom native (C/JNI) audio engine; this app plays audio through
 * standard ExoPlayer/Media3, which has no such native pipeline. Instead, this
 * uses Android's built-in [Visualizer] API to tap into the actual audio output
 * for the player's session in real time - no native code required, and it works
 * with any audio source ExoPlayer can play.
 *
 * Beat detection: RMS (root mean square) amplitude is computed from each
 * captured waveform. A "beat" fires when the RMS clears an adaptive threshold -
 * the greater of a small absolute floor (so near-silence never triggers) or a
 * fraction of a slowly-decaying running peak (so quiet songs still produce
 * haptics, and loud/dense songs don't spam continuously). A minimum interval
 * between pulses keeps the vibration feeling like discrete beats rather than a
 * continuous buzz.
 */
class MusicHapticsManager(context: Context) {
    private val appContext = context.applicationContext

    private var visualizer: Visualizer? = null
    private var runningPeak = 0f
    private var lastPulseTimeMs = 0L

    /** 0f..1f, how strong each haptic pulse is. */
    var intensity: Float = 1f

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Attaches to the given audio session and starts listening for beats.
     * Safe to call repeatedly (e.g. on every track change) - always stops any
     * previous session first.
     */
    fun start(audioSessionId: Int) {
        stop()

        if (audioSessionId == 0 || vibrator?.hasVibrator() != true) return

        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { onWaveform(it) }
                        }

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Not used - waveform RMS is enough for beat detection.
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    false
                )
                enabled = true
            }
            runningPeak = 0f
        } catch (e: Exception) {
            // Some devices/OEMs restrict Visualizer access, or the session id
            // may already be gone by the time this runs - fail silently rather
            // than crash playback over a cosmetic feature.
            visualizer = null
        }
    }

    private fun onWaveform(waveform: ByteArray) {
        if (waveform.isEmpty()) return

        var sumSquares = 0.0
        for (b in waveform) {
            // Unsigned 8-bit PCM: 128 is the silent midpoint.
            val sample = (b.toInt() and 0xFF) - 128
            sumSquares += (sample * sample).toDouble()
        }
        val rms = sqrt(sumSquares / waveform.size).toFloat()

        // Slowly decay the running peak so it tracks the recent loudness trend
        // rather than a single all-time max.
        runningPeak = max(rms, runningPeak * 0.98f)
        val threshold = max(ABSOLUTE_FLOOR, runningPeak * PEAK_FRACTION)

        val now = System.currentTimeMillis()
        if (rms >= threshold && now - lastPulseTimeMs >= MIN_PULSE_INTERVAL_MS) {
            lastPulseTimeMs = now
            pulse()
        }
    }

    private fun pulse() {
        val v = vibrator ?: return
        val amplitude = (255 * intensity).toInt().coerceIn(1, 255)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(PULSE_DURATION_MS, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(PULSE_DURATION_MS)
            }
        } catch (e: Exception) {
            // Ignore - a missed haptic pulse should never affect playback.
        }
    }

    fun stop() {
        visualizer?.let {
            try {
                it.enabled = false
                it.release()
            } catch (e: Exception) {
                // Ignore - already released or session gone.
            }
        }
        visualizer = null
        runningPeak = 0f
    }

    companion object {
        private const val ABSOLUTE_FLOOR = 8f
        private const val PEAK_FRACTION = 0.6f
        private const val MIN_PULSE_INTERVAL_MS = 120L
        private const val PULSE_DURATION_MS = 20L
    }
}
