/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import kotlin.math.exp
import kotlin.math.sqrt

internal const val SHAKE_THRESHOLD_G = 2.7f
internal const val SHAKE_MIN_INTERVAL_MS = 600L
internal const val SHAKE_STANDARD_GRAVITY = 9.80665f
internal const val SHAKE_DECAY_MS = 900f
internal const val SHAKE_SPIN_PEAK = 5f
internal const val SHAKE_GLOW_PEAK = 0.35f

// Reports a shake when the total acceleration, including gravity, reaches the threshold; resting in hand reads about 1 g.
internal class ShakeDetector(
    private val thresholdG: Float = SHAKE_THRESHOLD_G,
    private val minIntervalMs: Long = SHAKE_MIN_INTERVAL_MS,
) {
    private var lastShakeMs: Long? = null

    fun onSample(x: Float, y: Float, z: Float, timeMs: Long): Boolean {
        val gForce = sqrt(x * x + y * y + z * z) / SHAKE_STANDARD_GRAVITY
        if (gForce < thresholdG) return false
        val last = lastShakeMs
        if (last != null && timeMs - last < minIntervalMs) return false
        lastShakeMs = timeMs
        return true
    }

    fun reset() {
        lastShakeMs = null
    }
}

// 1 right at the shake, falling exponentially afterwards; 0 before the shake or long after it.
internal fun shakeBurst(elapsedMs: Long): Float {
    if (elapsedMs < 0L) return 0f
    return exp(-elapsedMs / SHAKE_DECAY_MS).coerceIn(0f, 1f)
}

// Spin multiplier for the layers: 1 when calm, up to 1 + SHAKE_SPIN_PEAK right after a shake.
internal fun shakeSpinMultiplier(elapsedMs: Long): Float = 1f + SHAKE_SPIN_PEAK * shakeBurst(elapsedMs)

internal fun shakeGlowLevel(elapsedMs: Long): Float = SHAKE_GLOW_PEAK * shakeBurst(elapsedMs)

// The accelerometer is only read while the player is open and the user has the effect switched on.
internal fun shouldListenForShake(expanded: Boolean, shakeEnabled: Boolean): Boolean = expanded && shakeEnabled
