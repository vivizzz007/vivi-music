/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val OverlayInactiveColor = Color(0x26FFFFFF)
private val OverlayInactiveShadeColor = Color(0x33FFFFFF)
private val OverlayActiveColor = Color(0xBFFFFFFF)
private val OverlayActiveShadeColor = Color(0x73FFFFFF)

/**
 * Slim capsule scrubber with two-pass overlay blend, drag-to-expand, and no thumb knob.
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
    idleHeight: Dp = 7.25.dp,
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

    val dragHighlightAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "thinSliderHighlight",
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
            val pillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset.Zero, size),
                        cornerRadius = radius,
                    )
                )
            }
            val filled = (size.width * fraction).coerceIn(0f, size.width)
            val useOverlay = activeColor.luminance() > 0.5f

            clipPath(pillPath) {
                if (useOverlay) {
                    if (filled < size.width) {
                        val inactiveSize = Size(size.width - filled, size.height)
                        val inactiveOffset = Offset(filled, 0f)
                        drawRect(
                            color = OverlayInactiveColor,
                            topLeft = inactiveOffset,
                            size = inactiveSize,
                            blendMode = BlendMode.Overlay,
                        )
                        drawRect(
                            color = OverlayInactiveShadeColor,
                            topLeft = inactiveOffset,
                            size = inactiveSize,
                        )
                    }
                    if (filled > 0f) {
                        val activeSize = Size(filled, size.height)
                        drawRect(
                            color = OverlayActiveColor,
                            topLeft = Offset.Zero,
                            size = activeSize,
                            blendMode = BlendMode.Overlay,
                        )
                        drawRect(
                            color = OverlayActiveShadeColor,
                            topLeft = Offset.Zero,
                            size = activeSize,
                        )
                        if (dragHighlightAlpha > 0f) {
                            drawRect(
                                color = Color.White.copy(alpha = dragHighlightAlpha),
                                topLeft = Offset.Zero,
                                size = activeSize,
                            )
                        }
                    }
                } else {
                    drawRect(color = inactiveColor, size = size)
                    if (filled > 0f) {
                        drawRect(color = activeColor, size = Size(filled, size.height))
                    }
                }
            }
        }
    }
}

