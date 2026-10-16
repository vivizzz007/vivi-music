/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.music.vivi.audio.AudioPulse
import com.music.vivi.audio.beatBump
import com.music.vivi.constants.LiveMeshAdaptiveQualityKey
import com.music.vivi.constants.LiveMeshAutoTempoKey
import com.music.vivi.constants.LiveMeshTapFlashKey
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LowResolutionLayer
import com.music.vivi.ui.utils.autoTempoFactor
import com.music.vivi.ui.utils.liveMeshPeriodMillis
import com.music.vivi.ui.utils.nextAdaptiveScale
import com.music.vivi.ui.utils.reactiveZoom
import com.music.vivi.utils.rememberPreference
import kotlin.math.sin

private const val SHIMMER_MAX_DEG_PER_SECOND = 240f
private const val SHIMMER_ALPHA_AMPLITUDE = 0.35f
private const val SHIMMER_GLOW_AMPLITUDE = 0.08f
private const val BEAT_LAYER_BOOST = 0.18f
private const val BEAT_TINT_RELIEF = 0.6f
private const val CENTER_GLOW_BEAT = 0.35f
private const val CENTER_GLOW_LYRIC = 0.1f
private const val TAP_FLASH_PEAK = 0.5f
private const val ADAPTIVE_WINDOW_SECONDS = 2f
private const val TEMPO_SMOOTHING_PER_SECOND = 2f

// Angles and shimmer phase live in snapshot state, so layers that read them repaint every frame by themselves.
private class LiveMeshClock {
    var anchor by mutableFloatStateOf(0f)
    var fast by mutableFloatStateOf(0f)
    var slow by mutableFloatStateOf(0f)
    var shimmer by mutableFloatStateOf(0f)
    var tapGlow by mutableFloatStateOf(0f)
}

// Single renderer for the Live Mesh background. The caller supplies how one artwork layer is drawn.
// Angles are accumulated per frame instead of using infinite animations, so auto tempo and speed changes
// never make the layers jump. Reactions are read in the draw phase, so nothing recomposes per frame.
@Composable
internal fun LiveMeshBackground(
    config: LiveMeshConfig,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    adaptiveQuality: Boolean = false,
    interactive: Boolean = false,
    artwork: @Composable (layerModifier: Modifier, alignment: Alignment, colorFilter: ColorFilter) -> Unit,
) {
    val autoTempo by rememberPreference(LiveMeshAutoTempoKey, defaultValue = false)
    val adaptiveSetting by rememberPreference(LiveMeshAdaptiveQualityKey, defaultValue = true)
    val tapFlashSetting by rememberPreference(LiveMeshTapFlashKey, defaultValue = true)
    val adaptiveEnabled = adaptiveQuality && adaptiveSetting

    val spin = if (config.reverseDirection) -1f else 1f
    val baseScale = config.renderScale
    val adaptiveScale = remember(baseScale) { mutableFloatStateOf(baseScale) }
    val renderScale = if (adaptiveEnabled) adaptiveScale.floatValue else baseScale

    val clock = remember { LiveMeshClock() }
    val tapStartMs = remember { mutableLongStateOf(0L) }
    val tempoFactor = remember { floatArrayOf(1f) }

    val latestConfig by rememberUpdatedState(config)
    val latestAutoTempo by rememberUpdatedState(autoTempo)
    val latestAdaptive by rememberUpdatedState(adaptiveEnabled)
    val latestBaseScale by rememberUpdatedState(baseScale)

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        var windowSeconds = 0f
        var windowFrameSeconds = 0f
        var windowFrames = 0
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
                    val cfg = latestConfig
                    val spinDir = if (cfg.reverseDirection) -1f else 1f

                    val targetTempo = if (latestAutoTempo) autoTempoFactor(AudioPulse.bpm()) else 1f
                    tempoFactor[0] += (targetTempo - tempoFactor[0]) * (dt * TEMPO_SMOOTHING_PER_SECOND).coerceAtMost(1f)
                    val tempo = tempoFactor[0]

                    clock.anchor = wrap(
                        clock.anchor - spinDir * 360f * tempo * dt * 1000f / liveMeshPeriodMillis(80000, cfg.speed)
                    )
                    clock.fast = wrap(
                        clock.fast + spinDir * 360f * tempo * dt * 1000f / liveMeshPeriodMillis(40000, cfg.speed)
                    )
                    clock.slow = wrap(
                        clock.slow + spinDir * 360f * tempo * dt * 1000f / liveMeshPeriodMillis(60000, cfg.speed)
                    )

                    val tempoReaction = AudioPulse.lyricTempo()
                    if (tempoReaction > 0f) {
                        clock.shimmer = wrap(
                            clock.shimmer + dt * SHIMMER_MAX_DEG_PER_SECOND * tempoReaction * cfg.lyricReactivity
                        )
                    }

                    // Updated every frame so the glow repaints for the whole flash, not just at the tap.
                    val tapStart = tapStartMs.longValue
                    clock.tapGlow = if (tapStart > 0L) {
                        beatBump(SystemClock.elapsedRealtime() - tapStart, TAP_FLASH_PEAK)
                    } else {
                        0f
                    }

                    if (latestAdaptive) {
                        windowSeconds += dt
                        windowFrameSeconds += dt
                        windowFrames++
                        if (windowSeconds >= ADAPTIVE_WINDOW_SECONDS) {
                            val avgFrameMs = windowFrameSeconds / windowFrames * 1000f
                            val next = nextAdaptiveScale(adaptiveScale.floatValue, avgFrameMs, latestBaseScale)
                            if (next != adaptiveScale.floatValue) adaptiveScale.floatValue = next
                            windowSeconds = 0f
                            windowFrameSeconds = 0f
                            windowFrames = 0
                        }
                    }
                }
                lastFrameNanos = now
            }
        }
    }

    val colorFilter = remember(config.saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(config.saturation) })
    }

    // Only a visible, expanded background may take touches; a tap-consuming layer under the collapsed sheet blocks the screen.
    val tapModifier = if (interactive && tapFlashSetting) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { tapStartMs.longValue = SystemClock.elapsedRealtime() })
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(tapModifier)
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
        LowResolutionLayer(factor = renderScale) {
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(100.dp * config.blurStrength / renderScale)
                    .graphicsLayer { rotationZ = clock.anchor - clock.shimmer * spin },
                Alignment.Center,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / renderScale)
                    .graphicsLayer {
                        rotationZ = clock.fast + clock.shimmer * spin
                        this.alpha = layerAlpha(config.layer2Alpha, clock.shimmer, config.lyricReactivity, config.beatReactivity)
                    },
                Alignment.TopStart,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / renderScale)
                    .graphicsLayer {
                        rotationZ = clock.slow + clock.shimmer * spin
                        this.alpha = layerAlpha(config.layer3Alpha, clock.shimmer, config.lyricReactivity, config.beatReactivity)
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
                    val tap = clock.tapGlow
                    val tempoBreath = AudioPulse.lyricTempo() * config.lyricReactivity *
                        SHIMMER_GLOW_AMPLITUDE * sin(Math.toRadians(clock.shimmer.toDouble())).toFloat()
                    val glow = AudioPulse.beat().coerceIn(0f, 1f) * config.beatReactivity * CENTER_GLOW_BEAT +
                        AudioPulse.lyric().coerceIn(0f, 1f) * config.lyricReactivity * CENTER_GLOW_LYRIC +
                        tap + tempoBreath
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

private fun wrap(degrees: Float): Float = degrees % 360f

// Layer opacity breathes with lyric tempo and flares on beats, so motion is visible across the screen.
private fun layerAlpha(base: Float, shimmerDeg: Float, lyricReactivity: Float, beatReactivity: Float): Float {
    val tempo = AudioPulse.lyricTempo()
    val phase = sin(Math.toRadians(shimmerDeg.toDouble())).toFloat()
    val shimmer = 1f + SHIMMER_ALPHA_AMPLITUDE * tempo * lyricReactivity * phase
    val beatFlare = AudioPulse.beat().coerceIn(0f, 1f) * BEAT_LAYER_BOOST * beatReactivity
    return ((base * shimmer) + beatFlare).coerceIn(0f, 1f)
}
