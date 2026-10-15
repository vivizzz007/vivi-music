/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.music.vivi.audio.AudioPulse
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LowResolutionLayer
import com.music.vivi.ui.utils.liveMeshPeriodMillis
import com.music.vivi.ui.utils.reactiveZoom

// Single renderer for the animated Live Mesh background. The caller supplies how one artwork layer is drawn
// (network image in the player, bundled drawable in previews); everything else — rotation, blur, tint, beat zoom — lives here.
@Composable
internal fun LiveMeshBackground(
    config: LiveMeshConfig,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    artwork: @Composable (layerModifier: Modifier, alignment: Alignment, colorFilter: ColorFilter) -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "liveMeshBackground")
    val spin = if (config.reverseDirection) -1f else 1f

    val anchorRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f * spin,
        animationSpec = infiniteRepeatable(
            animation = tween(liveMeshPeriodMillis(80000, config.speed), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anchorRotation"
    )
    val fastRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * spin,
        animationSpec = infiniteRepeatable(
            animation = tween(liveMeshPeriodMillis(40000, config.speed), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fastRotation"
    )
    val slowRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * spin,
        animationSpec = infiniteRepeatable(
            animation = tween(liveMeshPeriodMillis(60000, config.speed), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowRotation"
    )

    val colorFilter = remember(config.saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(config.saturation) })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .graphicsLayer {
                // Scale up so the rotating layers never reveal their edges; beats and lyric lines push it further.
                val zoom = reactiveZoom(
                    baseZoom = config.zoom,
                    beat = AudioPulse.beat(),
                    lyric = AudioPulse.lyric(),
                    beatReactivity = config.beatReactivity,
                    lyricReactivity = config.lyricReactivity,
                )
                scaleX = zoom
                scaleY = zoom
            }
    ) {
        LowResolutionLayer(factor = config.renderScale) {
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(100.dp * config.blurStrength / config.renderScale)
                    .graphicsLayer { rotationZ = anchorRotation },
                Alignment.Center,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / config.renderScale)
                    .graphicsLayer {
                        rotationZ = fastRotation
                        this.alpha = config.layer2Alpha
                    },
                Alignment.TopStart,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / config.renderScale)
                    .graphicsLayer {
                        rotationZ = slowRotation
                        this.alpha = config.layer3Alpha
                    },
                Alignment.BottomEnd,
                colorFilter,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = config.darkTint))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = config.bottomShade))
                    )
                )
        )
    }
}
