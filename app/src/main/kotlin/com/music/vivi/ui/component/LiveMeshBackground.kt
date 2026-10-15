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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.sin

private const val SHIMMER_MAX_DEG_PER_SECOND = 240f
private const val SHIMMER_ALPHA_AMPLITUDE = 0.35f
private const val BEAT_LAYER_BOOST = 0.18f
private const val BEAT_TINT_RELIEF = 0.6f
private const val CENTER_GLOW_BEAT = 0.35f
private const val CENTER_GLOW_LYRIC = 0.1f

// Single renderer for the animated Live Mesh background. The caller supplies how one artwork layer is drawn
// (network image in the player, bundled drawable in previews); rotation, blur, tint and reactions live here.
// Reactions are read in the draw phase, so beats and lyric lines animate the whole background without recomposing.
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

    // Extra spin that accumulates faster while lyric lines change quickly ("shimmer").
    val shimmerDeg = remember { mutableFloatStateOf(0f) }
    val lyricReactivity by rememberUpdatedState(config.lyricReactivity)
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    val tempo = AudioPulse.lyricTempo()
                    if (tempo > 0f) {
                        val seconds = (now - lastFrameNanos) / 1_000_000_000f
                        shimmerDeg.floatValue =
                            (shimmerDeg.floatValue + seconds * SHIMMER_MAX_DEG_PER_SECOND * tempo * lyricReactivity) % 360f
                    }
                }
                lastFrameNanos = now
            }
        }
    }

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
                    .graphicsLayer { rotationZ = anchorRotation - shimmerDeg.floatValue * spin },
                Alignment.Center,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / config.renderScale)
                    .graphicsLayer {
                        rotationZ = fastRotation + shimmerDeg.floatValue * spin
                        this.alpha = layerAlpha(config.layer2Alpha, shimmerDeg.floatValue, config.lyricReactivity, config.beatReactivity)
                    },
                Alignment.TopStart,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / config.renderScale)
                    .graphicsLayer {
                        rotationZ = slowRotation + shimmerDeg.floatValue * spin
                        this.alpha = layerAlpha(config.layer3Alpha, shimmerDeg.floatValue, config.lyricReactivity, config.beatReactivity)
                    },
                Alignment.BottomEnd,
                colorFilter,
            )
        }

        // Beats briefly lift the tint across the whole background, so the reaction is not limited to the zoom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val beat = AudioPulse.beat().coerceIn(0f, 1f)
                    this.alpha = (config.darkTint * (1f - beat * BEAT_TINT_RELIEF * config.beatReactivity)).coerceIn(0f, 1f)
                }
                .background(Color.Black)
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

        // Soft light in the middle of the screen so beats and lyric lines are felt at the center, not only at the edges.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val glow = AudioPulse.beat().coerceIn(0f, 1f) * config.beatReactivity * CENTER_GLOW_BEAT +
                        AudioPulse.lyric().coerceIn(0f, 1f) * config.lyricReactivity * CENTER_GLOW_LYRIC
                    this.alpha = glow.coerceIn(0f, 1f)
                }
                .drawWithCache {
                    val radius = size.minDimension * 0.6f
                    val glow = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radius,
                    )
                    onDrawBehind { drawRect(brush = glow) }
                }
        )
    }
}

// Layer opacity breathes with lyric tempo and flares on beats, so motion is visible across the screen.
private fun layerAlpha(base: Float, shimmerDeg: Float, lyricReactivity: Float, beatReactivity: Float): Float {
    val tempo = AudioPulse.lyricTempo()
    val phase = sin(Math.toRadians(shimmerDeg.toDouble())).toFloat()
    val shimmer = 1f + SHIMMER_ALPHA_AMPLITUDE * tempo * lyricReactivity * phase
    val beatFlare = AudioPulse.beat().coerceIn(0f, 1f) * BEAT_LAYER_BOOST * beatReactivity
    return ((base * shimmer) + beatFlare).coerceIn(0f, 1f)
}
