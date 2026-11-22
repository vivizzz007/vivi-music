/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Music style scrubber: a hairline capsule with no thumb knob, which
 * thickens under your finger and settles back when you let go.
 */
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    idleHeight: Dp = 7.dp,
    activeHeight: Dp = 12.dp,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(value) }

    LaunchedEffect(value) {
        if (!isDragging) {
            dragPosition = value
        }
    }

    val isInteractive = enabled && valueRange.endInclusive > valueRange.start
    val currentValue = if (isDragging) dragPosition else value
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((currentValue - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    val height by animateDpAsState(
        targetValue = if (isDragging) activeHeight else idleHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "thinSliderHeight",
    )

    val activeColor = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
    val inactiveColor = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Generous invisible touch target — visible bar is 6-12dp
            .height(activeHeight + 22.dp)
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(valueRange) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isDragging = true
                            var success = false
                            try {
                                val rawFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                                val newPos = valueRange.start + rawFraction * rangeSpan
                                dragPosition = newPos
                                currentOnValueChange(newPos)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!pointer.pressed) {
                                        pointer.consume()
                                        success = true
                                        break
                                    }
                                    if (pointer.positionChanged()) {
                                        val dragFraction = (pointer.position.x / size.width).coerceIn(0f, 1f)
                                        val dragVal = valueRange.start + dragFraction * rangeSpan
                                        dragPosition = dragVal
                                        currentOnValueChange(dragVal)
                                        pointer.consume()
                                    }
                                }
                            } finally {
                                isDragging = false
                                if (success) {
                                    currentOnValueChangeFinished?.invoke()
                                }
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val radius = CornerRadius(size.height / 2f)

            // 1. Inactive background track capsule
            drawRoundRect(
                color = inactiveColor,
                size = size,
                cornerRadius = radius,
            )

            // 2. Active progress fill capsule
            val filled = size.width * fraction
            if (filled > 0f) {
                drawRoundRect(
                    color = activeColor,
                    size = Size(
                        width = filled.coerceAtLeast(size.height).coerceAtMost(size.width),
                        height = size.height,
                    ),
                    cornerRadius = radius,
                )
            }
        }
    }
}

