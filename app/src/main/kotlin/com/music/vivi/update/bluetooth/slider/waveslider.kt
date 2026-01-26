package com.music.vivi.bluetooth.slider

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun WaveSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
        thumbColor = Color.White
    ),
    radius: Dp = 72.dp,
    strokeWidth: Dp = 4.dp,
    isPlaying: Boolean = false,
) {
    var isDragging by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Box(
        modifier = modifier.size(radius * 2),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            onValueChangeFinished?.invoke()
                        }
                    ) { change, _ ->
                        val center = size.width / 2f
                        val angle = atan2(
                            change.position.y - center,
                            change.position.x - center
                        ).let { if (it < 0) it + 2 * PI.toFloat() else it }

                        val newValue = valueRange.start +
                            (angle / (2 * PI.toFloat())) * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue.coerceIn(valueRange))
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radiusPx = radius.toPx() - strokeWidth.toPx() / 2
            val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)

            // Background track
            drawCircle(
                color = colors.inactiveTrackColor,
                radius = radiusPx,
                center = center,
                style = Stroke(width = strokeWidth.toPx())
            )

            // Progress arc
            if (progress > 0) {
                drawArc(
                    color = colors.activeTrackColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = center - Offset(radiusPx, radiusPx),
                    size = Size(radiusPx * 2, radiusPx * 2),
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Thumb with pulsing effect when playing
                val thumbRadius = if (isPlaying && !isDragging) {
                    strokeWidth.toPx() * (1.2f + 0.3f * sin(phase * 2f))
                } else {
                    strokeWidth.toPx() * 1.2f
                }

                val thumbAngle = Math.toRadians(progress * 360 - 90.0)
                val thumbX = center.x + radiusPx * cos(thumbAngle).toFloat()
                val thumbY = center.y + radiusPx * sin(thumbAngle).toFloat()

                drawCircle(
                    color = colors.thumbColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, thumbY)
                )
            }
        }
    }
}
