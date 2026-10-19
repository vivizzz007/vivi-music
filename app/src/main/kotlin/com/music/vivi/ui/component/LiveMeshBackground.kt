/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.music.vivi.audio.AudioPulse
import com.music.vivi.constants.LiveMeshAutoTempoKey
import com.music.vivi.ui.utils.LiveMeshConfig
import com.music.vivi.ui.utils.LiveMeshFrameInput
import com.music.vivi.ui.utils.LiveMeshMotion
import com.music.vivi.ui.utils.LiveMeshSamplePalette
import com.music.vivi.ui.utils.LowResolutionLayer
import com.music.vivi.ui.utils.advanceAngle
import com.music.vivi.ui.utils.autoTempoFactor
import com.music.vivi.ui.utils.beatSpeedFactor
import com.music.vivi.ui.utils.flowBlobs
import com.music.vivi.ui.utils.followTowards
import com.music.vivi.ui.utils.liveMeshPeriodMillis
import com.music.vivi.ui.utils.rememberLiveMeshMotion
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.flow.first
import kotlin.math.sin
import kotlin.random.Random

private const val SHIMMER_MAX_DEG_PER_SECOND = 240f
private const val SHIMMER_GLOW_AMPLITUDE = 0.08f
private const val BEAT_TINT_RELIEF = 0.6f
private const val CENTER_GLOW_BEAT = 0.6f
private const val CENTER_GLOW_LYRIC = 0.25f
private const val CENTER_GLOW_RADIUS_FRACTION = 0.9f
private const val CENTER_GLOW_CORE_ALPHA = 0.7f
private const val FLOW_BLOB_ALPHA = 1f
// The colour holds solid out to this fraction of the radius, so the shape reads as a colour field rather than a faint glow.
private const val FLOW_BLOB_SOLID_STOP = 0.5f
// Visual blur in screen dp; the radius is divided by renderScale because the layer draws at reduced resolution.
private const val FLOW_BLUR_VISUAL_DP = 40f

// Angles, shimmer and flow time live in snapshot state so the background repaints itself every frame.
// The player keeps one clock across artwork changes, so the motion continues instead of restarting per track.
internal class LiveMeshClock {
    var anchor by mutableFloatStateOf(0f)
    var fast by mutableFloatStateOf(0f)
    var slow by mutableFloatStateOf(0f)
    var shimmer by mutableFloatStateOf(0f)
    var flowTime by mutableFloatStateOf(0f)
        private set
    var beatPulse by mutableFloatStateOf(0f)
        private set
    var lyricPulse by mutableFloatStateOf(0f)
        private set
    private var tempo = 1f

    fun followPulses(dtSeconds: Float, beatTarget: Float, lyricTarget: Float, motion: LiveMeshMotion = LiveMeshMotion.Default) {
        beatPulse = followTowards(beatPulse, beatTarget.coerceIn(0f, 1f), motion.pulseRise, motion.pulseFall, dtSeconds)
        lyricPulse = followTowards(lyricPulse, lyricTarget.coerceIn(0f, 1f), motion.pulseRise, motion.pulseFall, dtSeconds)
    }

    fun advance(
        dtSeconds: Float,
        config: LiveMeshConfig,
        targetTempo: Float,
        lyricTempo: Float,
        nowMs: Long,
        motion: LiveMeshMotion = LiveMeshMotion.Default,
    ) {
        tempo = followTowards(tempo, targetTempo, motion.tempoRise, motion.tempoFall, dtSeconds)
        val direction = if (config.reverseDirection) -1f else 1f
        val speed = tempo * beatSpeedFactor(beatPulse, config.beatReactivity, motion.beatSpeedBoost)

        flowTime += dtSeconds * tempo * config.speed

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
    val autoTempo by rememberPreference(LiveMeshAutoTempoKey, defaultValue = true)
    val latestAutoTempo by rememberUpdatedState(autoTempo)
    return rememberLiveMeshClockDriver(
        config = config,
        playing = playing,
        motion = rememberLiveMeshMotion().motion,
    ) {
        LiveMeshFrameInput(
            beat = AudioPulse.beat(),
            lyric = AudioPulse.lyricPulse(),
            lyricTempo = AudioPulse.lyricTempo(),
            targetTempo = if (latestAutoTempo) autoTempoFactor(AudioPulse.bpm()) else 1f,
            nowMs = AudioPulse.nowMs(),
        )
    }
}

// The frame loop lives here once, so every surface moves with the same physics and the same motion settings.
@Composable
internal fun rememberLiveMeshClockDriver(
    config: LiveMeshConfig,
    playing: Boolean,
    motion: LiveMeshMotion,
    input: () -> LiveMeshFrameInput,
): LiveMeshClock {
    val clock = remember { LiveMeshClock() }
    val latestConfig by rememberUpdatedState(config)
    val latestPlaying by rememberUpdatedState(playing)
    val latestMotion by rememberUpdatedState(motion)
    val latestInput by rememberUpdatedState(input)

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
                    val frame = latestInput()
                    clock.followPulses(dt, frame.beat, frame.lyric, latestMotion)
                    clock.advance(dt, latestConfig, frame.targetTempo, frame.lyricTempo, frame.nowMs, latestMotion)
                }
                lastFrameNanos = now
            }
        }
    }
    return clock
}

// Colour blobs drift along unrelated sine paths, so the cover's colours flow across the whole surface, centre included.
// The per-instance seed gives every track and every screen its own rhythm; time and beats are read in the draw phase.
@Composable
internal fun LiveMeshBackground(
    config: LiveMeshConfig,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    artworkDim: Float = 1f,
    palette: List<Color> = LiveMeshSamplePalette,
    clock: LiveMeshClock = rememberLiveMeshClock(config),
) {
    val seed = remember { Random.nextInt() }
    val colors = palette.ifEmpty { LiveMeshSamplePalette }

    val flowBlur = (FLOW_BLUR_VISUAL_DP / config.renderScale).dp

    Box(modifier = modifier.fillMaxSize().alpha(alpha)) {
        LowResolutionLayer(factor = config.renderScale) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(flowBlur)
                    .drawBehind {
                    drawRect(colors.first())
                    val extent = maxOf(size.width, size.height)
                    flowBlobs(clock.flowTime, seed, clock.beatPulse, config.beatReactivity).forEach { blob ->
                        val color = lerp(
                            colors[blob.paletteIndex % colors.size],
                            colors[blob.blendIndex % colors.size],
                            blob.blend,
                        )
                        val reach = blob.radius * extent
                        withTransform({
                            translate(blob.x * size.width, blob.y * size.height)
                            rotate(Math.toDegrees(blob.angle.toDouble()).toFloat(), pivot = Offset.Zero)
                            scale(blob.stretchX, blob.stretchY, pivot = Offset.Zero)
                        }) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0f to color.copy(alpha = FLOW_BLOB_ALPHA),
                                        FLOW_BLOB_SOLID_STOP to color.copy(alpha = FLOW_BLOB_ALPHA),
                                        1f to color.copy(alpha = 0f),
                                    ),
                                    center = Offset.Zero,
                                    radius = reach,
                                ),
                                radius = reach,
                                center = Offset.Zero,
                            )
                        }
                    }
                }
            )
        }

        // Beats briefly lift the tint across the whole background, so the reaction is not limited to the colour blobs.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val beat = clock.beatPulse
                    val tint = (config.darkTint * (1f - beat * BEAT_TINT_RELIEF * config.beatReactivity)).coerceIn(0f, 1f)
                    this.alpha = 1f - (1f - tint) * artworkDim
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
                    val radius = size.minDimension * CENTER_GLOW_RADIUS_FRACTION
                    val glow = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = CENTER_GLOW_CORE_ALPHA), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radius,
                    )
                    onDrawBehind { drawRect(brush = glow) }
                }
        )
    }
}

private fun wrap(degrees: Float): Float = degrees % 360f
