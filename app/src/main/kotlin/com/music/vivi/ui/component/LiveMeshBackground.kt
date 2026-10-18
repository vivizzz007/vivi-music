/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.runtime.snapshotFlow
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
import com.music.vivi.constants.LiveMeshAdaptiveQualityKey
import com.music.vivi.constants.LiveMeshAutoTempoKey
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LowResolutionLayer
import com.music.vivi.ui.utils.PULSE_FALL_PER_SECOND
import com.music.vivi.ui.utils.PULSE_RISE_PER_SECOND
import com.music.vivi.ui.utils.TEMPO_FALL_PER_SECOND
import com.music.vivi.ui.utils.TEMPO_RISE_PER_SECOND
import com.music.vivi.ui.utils.advanceAngle
import com.music.vivi.ui.utils.autoTempoFactor
import com.music.vivi.ui.utils.beatSpeedFactor
import com.music.vivi.ui.utils.followTowards
import com.music.vivi.ui.utils.liveMeshPeriodMillis
import com.music.vivi.ui.utils.nextAdaptiveScale
import com.music.vivi.ui.utils.reactiveZoom
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.flow.first
import kotlin.math.sin

private const val SHIMMER_MAX_DEG_PER_SECOND = 240f
private const val SHIMMER_ALPHA_AMPLITUDE = 0.35f
private const val SHIMMER_GLOW_AMPLITUDE = 0.08f
private const val BEAT_LAYER_BOOST = 0.18f
private const val BEAT_TINT_RELIEF = 0.6f
private const val CENTER_GLOW_BEAT = 0.35f
private const val CENTER_GLOW_LYRIC = 0.1f
private const val ADAPTIVE_WINDOW_SECONDS = 2f

// Angles and shimmer live in snapshot state so layers repaint themselves every frame.
// The player keeps one clock across artwork changes, so the motion continues instead of restarting per track.
internal class LiveMeshClock {
    var anchor by mutableFloatStateOf(0f)
    var fast by mutableFloatStateOf(0f)
    var slow by mutableFloatStateOf(0f)
    var shimmer by mutableFloatStateOf(0f)
    var beatPulse by mutableFloatStateOf(0f)
        private set
    var lyricPulse by mutableFloatStateOf(0f)
        private set
    private var tempo = 1f

    fun followPulses(dtSeconds: Float, beatTarget: Float, lyricTarget: Float) {
        beatPulse = followTowards(beatPulse, beatTarget.coerceIn(0f, 1f), PULSE_RISE_PER_SECOND, PULSE_FALL_PER_SECOND, dtSeconds)
        lyricPulse = followTowards(lyricPulse, lyricTarget.coerceIn(0f, 1f), PULSE_RISE_PER_SECOND, PULSE_FALL_PER_SECOND, dtSeconds)
    }

    fun advance(dtSeconds: Float, config: LiveMeshConfig, targetTempo: Float, lyricTempo: Float, nowMs: Long) {
        tempo = followTowards(tempo, targetTempo, TEMPO_RISE_PER_SECOND, TEMPO_FALL_PER_SECOND, dtSeconds)
        val direction = if (config.reverseDirection) -1f else 1f
        val speed = tempo * beatSpeedFactor(beatPulse, config.beatReactivity)

        anchor = advanceAngle(anchor, -direction, speed, dtSeconds, liveMeshPeriodMillis(80000, config.speed))
        fast = advanceAngle(fast, direction, speed, dtSeconds, liveMeshPeriodMillis(40000, config.speed))
        slow = advanceAngle(slow, direction, speed, dtSeconds, liveMeshPeriodMillis(60000, config.speed))

        if (lyricTempo > 0f) {
            shimmer = wrap(shimmer + dtSeconds * SHIMMER_MAX_DEG_PER_SECOND * lyricTempo * config.lyricReactivity)
        }
    }
}

// One clock per screen: the caller owns the frame loop, so a crossfading artwork never advances it twice.
@Composable
internal fun rememberLiveMeshClock(config: LiveMeshConfig, playing: Boolean = true): LiveMeshClock {
    val autoTempo by rememberPreference(LiveMeshAutoTempoKey, defaultValue = false)
    val clock = remember { LiveMeshClock() }
    val latestConfig by rememberUpdatedState(config)
    val latestAutoTempo by rememberUpdatedState(autoTempo)
    val latestPlaying by rememberUpdatedState(playing)

    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        while (true) {
            // Suspending instead of skipping frames keeps the render loop fully idle while paused.
            if (!latestPlaying) {
                lastFrameNanos = 0L
                snapshotFlow { latestPlaying }.first { it }
            }
            withFrameNanos { now ->
                if (lastFrameNanos != 0L) {
                    val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
                    val targetTempo = if (latestAutoTempo) autoTempoFactor(AudioPulse.bpm()) else 1f
                    clock.followPulses(dt, AudioPulse.beat(), AudioPulse.lyricPulse())
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
    lowResolution: Boolean = true,
    clock: LiveMeshClock = rememberLiveMeshClock(config),
    artwork: @Composable (layerModifier: Modifier, alignment: Alignment, colorFilter: ColorFilter) -> Unit,
) {
    val adaptiveSetting by rememberPreference(LiveMeshAdaptiveQualityKey, defaultValue = true)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .graphicsLayer {
                // Scale up so the rotating layers never reveal their edges; beats and lyric lines push it further.
                val zoom = reactiveZoom(
                    baseZoom = config.zoom,
                    beat = clock.beatPulse,
                    lyric = clock.lyricPulse,
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
                        this.alpha = layerAlpha(config.layer2Alpha, clock, config.lyricReactivity, config.beatReactivity)
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
                        this.alpha = layerAlpha(config.layer3Alpha, clock, config.lyricReactivity, config.beatReactivity)
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
                    val beat = clock.beatPulse
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
                    val tempoBreath = AudioPulse.lyricTempo() * config.lyricReactivity *
                        SHIMMER_GLOW_AMPLITUDE * sin(Math.toRadians(clock.shimmer.toDouble())).toFloat()
                    val glow = clock.beatPulse * config.beatReactivity * CENTER_GLOW_BEAT +
                        clock.lyricPulse * config.lyricReactivity * CENTER_GLOW_LYRIC +
                        tempoBreath
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
private fun layerAlpha(base: Float, clock: LiveMeshClock, lyricReactivity: Float, beatReactivity: Float): Float {
    val tempo = AudioPulse.lyricTempo()
    val phase = sin(Math.toRadians(clock.shimmer.toDouble())).toFloat()
    val shimmer = 1f + SHIMMER_ALPHA_AMPLITUDE * tempo * lyricReactivity * phase
    val beatFlare = clock.beatPulse * BEAT_LAYER_BOOST * beatReactivity
    return ((base * shimmer) + beatFlare).coerceIn(0f, 1f)
}
