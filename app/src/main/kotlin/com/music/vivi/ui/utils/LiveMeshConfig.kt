/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.music.vivi.constants.LiveMeshBlurStrengthKey
import com.music.vivi.constants.LiveMeshDarkTintKey
import com.music.vivi.constants.LiveMeshSaturationKey
import com.music.vivi.constants.LiveMeshSpeedKey
import com.music.vivi.utils.rememberPreference
import kotlin.math.roundToInt

internal data class LiveMeshConfig(
    val blurStrength: Float,
    val saturation: Float,
    val speed: Float,
    val darkTint: Float,
) {
    companion object {
        const val BLUR_STRENGTH_MIN = 0.5f
        const val BLUR_STRENGTH_MAX = 2f
        const val SATURATION_MIN = 0.5f
        const val SATURATION_MAX = 2.5f
        const val SPEED_MIN = 0.25f
        const val SPEED_MAX = 3f
        const val DARK_TINT_MIN = 0f
        const val DARK_TINT_MAX = 0.6f

        val Default = LiveMeshConfig(
            blurStrength = 1f,
            saturation = 1.8f,
            speed = 1f,
            darkTint = 0.2f,
        )
    }
}

internal fun LiveMeshConfig.clamped(): LiveMeshConfig = LiveMeshConfig(
    blurStrength = blurStrength.coerceIn(LiveMeshConfig.BLUR_STRENGTH_MIN, LiveMeshConfig.BLUR_STRENGTH_MAX),
    saturation = saturation.coerceIn(LiveMeshConfig.SATURATION_MIN, LiveMeshConfig.SATURATION_MAX),
    speed = speed.coerceIn(LiveMeshConfig.SPEED_MIN, LiveMeshConfig.SPEED_MAX),
    darkTint = darkTint.coerceIn(LiveMeshConfig.DARK_TINT_MIN, LiveMeshConfig.DARK_TINT_MAX),
)

// Higher speed shortens the rotation period so the mesh turns faster without changing its shape.
internal fun liveMeshPeriodMillis(baseMillis: Int, speed: Float): Int =
    (baseMillis / speed.coerceAtLeast(LiveMeshConfig.SPEED_MIN)).roundToInt()

@Composable
internal fun rememberLiveMeshConfig(): LiveMeshConfig {
    val blurStrength by rememberPreference(LiveMeshBlurStrengthKey, defaultValue = LiveMeshConfig.Default.blurStrength)
    val saturation by rememberPreference(LiveMeshSaturationKey, defaultValue = LiveMeshConfig.Default.saturation)
    val speed by rememberPreference(LiveMeshSpeedKey, defaultValue = LiveMeshConfig.Default.speed)
    val darkTint by rememberPreference(LiveMeshDarkTintKey, defaultValue = LiveMeshConfig.Default.darkTint)
    return LiveMeshConfig(blurStrength, saturation, speed, darkTint).clamped()
}
