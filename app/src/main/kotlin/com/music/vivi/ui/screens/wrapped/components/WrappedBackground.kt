/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.screens.wrapped.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.music.vivi.ui.utils.rememberReduceBackgroundEffects
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DOT_SPACING_PX = 30f

internal fun dotGridPoints(width: Float, height: Float, spacing: Float): List<Offset> {
    if (width <= 0f || height <= 0f) return emptyList()
    val points = ArrayList<Offset>()
    for (x in 0..(width / spacing).toInt()) {
        for (y in 0..(height / spacing).toInt()) {
            points.add(Offset(x * spacing, y * spacing))
        }
    }
    return points
}

// The dot grid never moves, so it is rasterized once per size instead of re-issuing ~3,000 points every frame.
private fun renderDotGrid(widthPx: Float, heightPx: Float, density: Density): ImageBitmap? {
    val width = widthPx.roundToInt()
    val height = heightPx.roundToInt()
    if (width <= 0 || height <= 0) return null
    val points = dotGridPoints(widthPx, heightPx, DOT_SPACING_PX)
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(bitmap),
        size = Size(width.toFloat(), height.toFloat())
    ) {
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = Color.White.copy(alpha = 0.05f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
    return bitmap
}

@Composable
fun WrappedBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val reduceAnimations = rememberReduceBackgroundEffects()
    val infiniteTransition = rememberInfiniteTransition(label = "WrappedBackground")
    val staticPhase = remember { mutableFloatStateOf(0f) }
    val staticScale = remember { mutableFloatStateOf(1f) }

    // Animations for the glow blobs
    val blob1Offset by if (reduceAnimations) staticPhase else infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1"
    )

    val blob2Offset by if (reduceAnimations) staticPhase else infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2"
    )

    val scale by if (reduceAnimations) staticScale else infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Optimization: Cache the color list objects so they are not re-allocated every single frame
    val blob1Colors = remember { listOf(Color(0xFF7C3AED).copy(alpha = 0.4f), Color.Transparent) }
    val blob2Colors = remember { listOf(Color(0xFF06B6D4).copy(alpha = 0.3f), Color.Transparent) }
    val blob3Colors = remember { listOf(Color(0xFFDB2777).copy(alpha = 0.2f), Color.Transparent) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0620)) // Deep Indigo Base
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val dotLayer = remember(widthPx, heightPx, density) {
            renderDotGrid(widthPx, heightPx, density)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Blob 1: Vibrant Purple
            val b1X = width * 0.3f + sin(blob1Offset) * width * 0.2f
            val b1Y = height * 0.2f + cos(blob1Offset) * height * 0.1f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = blob1Colors,
                    center = Offset(b1X, b1Y),
                    radius = width * 0.8f * scale
                ),
                radius = width * 0.8f * scale,
                center = Offset(b1X, b1Y)
            )

            // Blob 2: Cyan/Blue
            val b2X = width * 0.7f + cos(blob2Offset) * width * 0.2f
            val b2Y = height * 0.8f + sin(blob2Offset) * height * 0.1f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = blob2Colors,
                    center = Offset(b2X, b2Y),
                    radius = width * 0.9f * scale
                ),
                radius = width * 0.9f * scale,
                center = Offset(b2X, b2Y)
            )

            // Blob 3: Deep Pink (Fixed)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = blob3Colors,
                    center = Offset(width * 0.1f, height * 0.9f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width * 0.1f, height * 0.9f)
            )

            dotLayer?.let { drawImage(it) }
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
