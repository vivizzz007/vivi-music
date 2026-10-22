/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.request.ImageRequest
import com.music.vivi.constants.BackgroundEffectsMode
import com.music.vivi.constants.BackgroundEffectsModeKey
import com.music.vivi.constants.PlayerBackgroundStyle
import com.music.vivi.utils.rememberEnumPreference

internal const val LOW_POWER_IMAGE_SIZE = 96

internal fun shouldReduceBackgroundEffects(
    mode: BackgroundEffectsMode,
    isLowRamDevice: Boolean,
    animatorDurationScale: Float,
): Boolean = when (mode) {
    BackgroundEffectsMode.FULL -> false
    BackgroundEffectsMode.REDUCED -> true
    BackgroundEffectsMode.AUTO -> isLowRamDevice || animatorDurationScale == 0f
}

internal fun PlayerBackgroundStyle.withReducedEffects(): PlayerBackgroundStyle = when (this) {
    PlayerBackgroundStyle.LIVE_MESH -> PlayerBackgroundStyle.BLUR
    PlayerBackgroundStyle.GLOW_ANIMATED -> PlayerBackgroundStyle.GRADIENT
    else -> this
}

internal fun Modifier.backgroundBlur(radius: Dp, reduced: Boolean): Modifier =
    if (reduced) this else blur(radius)

// Decoding a background image at a tiny size and letting the GPU upscale it gives the same soft look as a
// blur at a fraction of the cost, and it works on every API level (Compose blur needs Android 12+).
internal fun backgroundImageSize(reduced: Boolean, fullSize: Int): Int =
    if (reduced) minOf(fullSize, LOW_POWER_IMAGE_SIZE) else fullSize

internal fun ImageRequest.Builder.backgroundSize(reduced: Boolean, fullSize: Int): ImageRequest.Builder {
    val side = backgroundImageSize(reduced, fullSize)
    return size(side, side)
}

@Composable
internal fun rememberReduceBackgroundEffects(): Boolean {
    val mode by rememberEnumPreference(
        key = BackgroundEffectsModeKey,
        defaultValue = BackgroundEffectsMode.AUTO,
    )
    val context = LocalContext.current
    val isLowRamDevice = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    }
    val animatorScale = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    return shouldReduceBackgroundEffects(mode, isLowRamDevice, animatorScale)
}

// Blur cost scales with the layer's pixel count, so the blurred content is rasterized at 1/factor size and
// upscaled by the compositor. Callers must divide blur radii by factor to keep the same on-screen softness.
@Composable
internal fun LowResolutionLayer(
    factor: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .requiredSize(maxWidth / factor, maxHeight / factor)
                .graphicsLayer {
                    scaleX = factor
                    scaleY = factor
                    transformOrigin = TransformOrigin(0f, 0f)
                    compositingStrategy = CompositingStrategy.Offscreen
                },
            content = content
        )
    }
}

@Composable
internal fun rememberReducedBackgroundStyle(style: PlayerBackgroundStyle): PlayerBackgroundStyle {
    val reduce = rememberReduceBackgroundEffects()
    return remember(style, reduce) { if (reduce) style.withReducedEffects() else style }
}
