/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.music.vivi.audio.AudioPulse
import com.music.vivi.audio.beatBump
import com.music.vivi.constants.LiveMeshAdaptiveQualityKey
import com.music.vivi.constants.LiveMeshAutoTempoKey
import com.music.vivi.constants.LiveMeshTapFlashKey
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LowResolutionLayer
import com.music.vivi.ui.utils.advanceAngle
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
internal const val TAP_MAX_DURATION_MS = 400L
private const val ADAPTIVE_WINDOW_SECONDS = 2f
private const val TEMPO_SMOOTHING_PER_SECOND = 2f

// Angles, shimmer and the tap flash live in snapshot state so layers repaint themselves every frame.
// The player keeps one clock across artwork changes, so the motion continues instead of restarting per track.
internal class LiveMeshClock {
    var anchor by mutableFloatStateOf(0f)
    var fast by mutableFloatStateOf(0f)
    var slow by mutableFloatStateOf(0f)
    var shimmer by mutableFloatStateOf(0f)
    var tapGlow by mutableFloatStateOf(0f)
    var tapStartMs = 0L
    private var tempo = 1f

    fun advance(dtSeconds: Float, config: LiveMeshConfig, targetTempo: Float, lyricTempo: Float, nowMs: Long) {
        tempo += (targetTempo - tempo) * (dtSeconds * TEMPO_SMOOTHING_PER_SECOND).coerceAtMost(1f)
        val direction = if (config.reverseDirection) -1f else 1f

        anchor = advanceAngle(anchor, -direction, tempo, dtSeconds, liveMeshPeriodMillis(80000, config.speed))
        fast = advanceAngle(fast, direction, tempo, dtSeconds, liveMeshPeriodMillis(40000, config.speed))
        slow = advanceAngle(slow, direction, tempo, dtSeconds, liveMeshPeriodMillis(60000, config.speed))

        if (lyricTempo > 0f) {
            shimmer = wrap(shimmer + dtSeconds * SHIMMER_MAX_DEG_PER_SECOND * lyricTempo * config.lyricReactivity)
        }

        // Updated every frame so the glow repaints for the whole flash, not just at the tap.
        tapGlow = if (tapStartMs > 0L) beatBump(nowMs - tapStartMs, TAP_FLASH_PEAK) else 0f
    }
}

// One clock per screen: the caller owns the frame loop, so a crossfading artwork never advances it twice.
@Composable
internal fun rememberLiveMeshClock(config: LiveMeshConfig): LiveMeshClock {
    val autoTempo by rememberPreference(LiveMeshAutoTempoKey, defaultValue = false)
    val clock = remember { LiveMeshClock() }
    val latestConfig by rememberUpdatedState(config)
    val latestAutoTempo by rememberUpdatedState(autoTempo)

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
                    val targetTempo = if (latestAutoTempo) autoTempoFactor(AudioPulse.bpm()) else 1f
                    clock.advance(dt, latestConfig, targetTempo, AudioPulse.lyricTempo(), AudioPulse.nowMs())
                }
                lastFrameNanos = now
            }
        }
    }
    return clock
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
    lowResolution: Boolean = true,
    clock: LiveMeshClock = rememberLiveMeshClock(config),
    artwork: @Composable (layerModifier: Modifier, alignment: Alignment, colorFilter: ColorFilter) -> Unit,
) {
    val adaptiveSetting by rememberPreference(LiveMeshAdaptiveQualityKey, defaultValue = true)
    val tapFlashSetting by rememberPreference(LiveMeshTapFlashKey, defaultValue = true)
    val adaptiveEnabled = adaptiveQuality && adaptiveSetting

    val spin = if (config.reverseDirection) -1f else 1f
    val baseScale = config.renderScale
    val adaptiveScale = remember(baseScale) { mutableFloatStateOf(baseScale) }
    val renderScale = if (adaptiveEnabled) adaptiveScale.floatValue else baseScale
    val layerScale = if (lowResolution) renderScale else 1f

    val latestAdaptive by rememberUpdatedState(adaptiveEnabled)
    val latestBaseScale by rememberUpdatedState(baseScale)

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        var windowSeconds = 0f
        var windowFrameSeconds = 0f
        var windowFrames = 0
        while (true) {
            withFrameNanos { now ->
                if (lastFrameNanos != 0L && latestAdaptive) {
                    val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
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
                lastFrameNanos = now
            }
        }
    }

    val colorFilter = remember(config.saturation) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(config.saturation) })
    }

    // Only a visible, expanded background may take touches; a tap-consuming layer under the collapsed sheet blocks the screen.
    val touchSlop = LocalViewConfiguration.current.touchSlop
    // Watches presses without consuming them, so a double-tap seek on the artwork cannot swallow the flash.
    val tapModifier = if (interactive && tapFlashSetting) {
        Modifier.pointerInput(touchSlop) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                val isTap = up != null && isTapGesture(
                    distancePx = (up.position - down.position).getDistance(),
                    durationMs = up.uptimeMillis - down.uptimeMillis,
                    touchSlopPx = touchSlop,
                )
                if (isTap) clock.tapStartMs = AudioPulse.nowMs()
            }
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
                    lyric = AudioPulse.lyricPulse(),
                    beatReactivity = config.beatReactivity,
                    lyricReactivity = config.lyricReactivity,
                )
                scaleX = zoom
                scaleY = zoom
            }
    ) {
        val layers: @Composable BoxScope.() -> Unit = {
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(100.dp * config.blurStrength / layerScale)
                    .graphicsLayer { rotationZ = clock.anchor - clock.shimmer * spin },
                Alignment.Center,
                colorFilter,
            )
            artwork(
                Modifier
                    .fillMaxSize()
                    .blur(120.dp * config.blurStrength / layerScale)
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
                    .blur(120.dp * config.blurStrength / layerScale)
                    .graphicsLayer {
                        rotationZ = clock.slow + clock.shimmer * spin
                        this.alpha = layerAlpha(config.layer3Alpha, clock.shimmer, config.lyricReactivity, config.beatReactivity)
                    },
                Alignment.BottomEnd,
                colorFilter,
            )
        }
        if (lowResolution) {
            LowResolutionLayer(factor = renderScale, content = layers)
        } else {
            Box(modifier = Modifier.fillMaxSize(), content = layers)
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
                        AudioPulse.lyricPulse().coerceIn(0f, 1f) * config.lyricReactivity * CENTER_GLOW_LYRIC +
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

// A press that barely moves and lifts quickly is a tap; drags and long holds belong to the sheet or the content.
internal fun isTapGesture(distancePx: Float, durationMs: Long, touchSlopPx: Float): Boolean =
    distancePx < touchSlopPx && durationMs < TAP_MAX_DURATION_MS

// Layer opacity breathes with lyric tempo and flares on beats, so motion is visible across the screen.
private fun layerAlpha(base: Float, shimmerDeg: Float, lyricReactivity: Float, beatReactivity: Float): Float {
    val tempo = AudioPulse.lyricTempo()
    val phase = sin(Math.toRadians(shimmerDeg.toDouble())).toFloat()
    val shimmer = 1f + SHIMMER_ALPHA_AMPLITUDE * tempo * lyricReactivity * phase
    val beatFlare = AudioPulse.beat().coerceIn(0f, 1f) * BEAT_LAYER_BOOST * beatReactivity
    return ((base * shimmer) + beatFlare).coerceIn(0f, 1f)
}
