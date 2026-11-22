package com.music.vivi.ui.player.lyricsanimation

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val COOKIE_COUNT = 3
private val COOKIE_SIZE = 20.dp
private val COOKIE_GAP = 10.dp
private val ROW_HEIGHT = 44.dp
private val ROW_SPACING = 16.dp

private const val MIN_TAIL_MS = 600f
private const val MAX_TAIL_MS = 1800f
private const val TAIL_FRACTION = 0.18f

private const val MIN_ROTATION_PERIOD_MS = 1200f
private const val MAX_ROTATION_PERIOD_MS = 2800f

private val COOKIE_EASING = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)

/**
 * Creates and caches the normalized 9-sided "cookie" polygon path using
 * official Material 3 Expressive [MaterialShapes.Cookie9Sided], centered
 * exactly at (0, 0) with a radius of 1.0f so it rotates perfectly on its own axis.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object CookiePolygonCache {
    val path: AndroidPath by lazy {
        val rawPath = runCatching {
            MaterialShapes.Cookie9Sided.toPath()
        }.getOrElse {
            buildScallopedCookiePath(lobes = 9, outerRadius = 1f, innerRadius = 0.82f)
        }

        // Center the path at (0, 0) and normalize radius to 1.0f
        val bounds = RectF()
        rawPath.computeBounds(bounds, true)
        val normalized = AndroidPath()
        val matrix = Matrix()
        matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
        val maxExtent = maxOf(bounds.width(), bounds.height()).takeIf { it > 0.001f } ?: 1f
        val scale = 2f / maxExtent
        matrix.postScale(scale, scale)
        rawPath.transform(matrix, normalized)
        normalized
    }

    private fun buildScallopedCookiePath(lobes: Int, outerRadius: Float, innerRadius: Float): AndroidPath {
        val path = AndroidPath()
        val totalPoints = lobes * 2
        val angleStep = (2f * PI.toFloat()) / totalPoints

        var first = true
        for (i in 0 until totalPoints) {
            val isPeak = i % 2 == 0
            val r = if (isPeak) outerRadius else innerRadius
            val angle = i * angleStep - (PI.toFloat() / 2f)
            val x = r * cos(angle)
            val y = r * sin(angle)

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return path
    }
}

/**
 * Animated 9-sided rotating "cookie" pause indicator for musical interludes and instrumental breaks.
 *
 * Built with Material 3 Expressive [MaterialShapes.Cookie9Sided].
 *
 * The rotation is a continuous, uninterrupted rhythm throughout the break.
 * As each cookie sequentially pops up, it dynamically joins the ongoing spinning rhythm
 * without stopping or pausing previous cookies. In the final countdown tail before vocals resume,
 * all visible cookies smoothly scale down and close together.
 */
@Composable
fun InstrumentalCookieIndicator(
    timeMs: Long,
    untilMs: Long,
    isActive: Boolean,
    clock: State<Long>,
    color: Color,
    lineAlpha: Float,
    blurRadius: Dp,
    glowRoom: Dp,
    onSeek: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    val swell by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isActive) 400 else 350,
            easing = COOKIE_EASING
        ),
        label = "cookieSwell"
    )

    val maybeBlur = if (blurRadius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    val totalWidth = COOKIE_SIZE * COOKIE_COUNT + COOKIE_GAP * (COOKIE_COUNT - 1)
    val boxAlignment = when (alignment) {
        Alignment.CenterHorizontally -> Alignment.Center
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        contentAlignment = boxAlignment,
        modifier = modifier
            .fillMaxWidth()
            .height((ROW_HEIGHT + ROW_SPACING) * swell)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .then(maybeBlur)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSeek)
                .padding(glowRoom)
                .size(width = totalWidth, height = COOKIE_SIZE)
                .graphicsLayer {
                    alpha = lineAlpha * swell
                }
                .drawBehind {
                    val now = clock.value
                    val totalSpan = (untilMs - timeMs).coerceAtLeast(1L).toFloat()
                    val elapsedMs = (now - timeMs).coerceIn(0L, totalSpan.toLong()).toFloat()

                    val tailMs = (totalSpan * TAIL_FRACTION).coerceIn(MIN_TAIL_MS, MAX_TAIL_MS)
                    val activeSpanMs = (totalSpan - tailMs).coerceAtLeast(1f)
                    val phaseDurationMs = activeSpanMs / COOKIE_COUNT.toFloat()

                    // Exit cue during the final tail window right before vocals resume
                    val inTail = elapsedMs >= activeSpanMs
                    val tailProgress = if (inTail) {
                        ((elapsedMs - activeSpanMs) / tailMs).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val exitScale = if (inTail) {
                        if (tailProgress < 0.25f) {
                            1f + 0.08f * (tailProgress / 0.25f)
                        } else {
                            val down = ((tailProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
                            1.08f * (1f - down * down * (3f - 2f * down))
                        }
                    } else {
                        1f
                    }
                    val exitAlpha = if (inTail) {
                        val down = ((tailProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                        1f - down
                    } else {
                        1f
                    }

                    // Continuous, uninterrupted rotation rhythm throughout the break (never stops or stutters)
                    val rotationPeriodMs = phaseDurationMs.coerceIn(MIN_ROTATION_PERIOD_MS, MAX_ROTATION_PERIOD_MS)
                    val continuousRhythmRotation = (elapsedMs / rotationPeriodMs) * 360f

                    val radius = COOKIE_SIZE.toPx() / 2f
                    val stride = (COOKIE_SIZE + COOKIE_GAP).toPx()

                    val baseCookiePath = CookiePolygonCache.path
                    val matrix = Matrix()
                    val transformedAndroidPath = AndroidPath()

                    for (index in 0 until COOKIE_COUNT) {
                        val phaseStart = index * phaseDurationMs

                        if (elapsedMs < phaseStart) {
                            // This cookie has not appeared yet
                            continue
                        }

                        // Dynamic entry: pops in with spring bounce and immediately joins the ongoing rhythm
                        val timeSincePop = elapsedMs - phaseStart
                        val popDurationMs = minOf(phaseDurationMs * 0.35f, 450f).coerceAtLeast(1f)
                        val popProgress = (timeSincePop / popDurationMs).coerceIn(0f, 1f)
                        val springOvershoot = sin(popProgress * PI.toFloat()) * 0.16f

                        val entryScale = if (popProgress < 1f) (popProgress + springOvershoot) else 1f
                        val cookieScale = entryScale * exitScale
                        val cookieAlpha = popProgress * exitAlpha

                        if (cookieScale <= 0.01f || cookieAlpha <= 0.01f) {
                            continue
                        }

                        val centerX = radius + index * stride
                        val centerY = size.height / 2f

                        matrix.reset()
                        matrix.setScale(radius, radius)
                        matrix.postTranslate(centerX, centerY)
                        transformedAndroidPath.reset()
                        baseCookiePath.transform(matrix, transformedAndroidPath)

                        val composePath = transformedAndroidPath.asComposePath()

                        // Spins continuously in the shared rhythm without stopping previous cookies
                        rotate(degrees = continuousRhythmRotation, pivot = Offset(centerX, centerY)) {
                            scale(scale = cookieScale, pivot = Offset(centerX, centerY)) {
                                drawPath(
                                    path = composePath,
                                    color = color.copy(alpha = cookieAlpha)
                                )
                            }
                        }
                    }
                }
        )
    }
}

