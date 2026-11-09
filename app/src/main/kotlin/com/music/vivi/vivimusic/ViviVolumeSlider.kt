/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.vivimusic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.vivi.R
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LeftCapShape = RoundedCornerShape(
    topStartPercent = 50,
    bottomStartPercent = 50,
    topEndPercent = 0,
    bottomEndPercent = 0
)

private val RightCapShape = RoundedCornerShape(
    topStartPercent = 0,
    bottomStartPercent = 0,
    topEndPercent = 50,
    bottomEndPercent = 50
)

private val FirstSegmentShape = RoundedCornerShape(
    topStart = 6.dp,
    bottomStart = 6.dp,
    topEnd = 0.dp,
    bottomEnd = 0.dp
)

private val LastSegmentShape = RoundedCornerShape(
    topStart = 0.dp,
    bottomStart = 0.dp,
    topEnd = 6.dp,
    bottomEnd = 6.dp
)

/**
 * Custom pointer modifier supporting both single tap and hold-to-repeat (long-press).
 */
@Composable
private fun Modifier.repeatableClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    initialDelayMillis: Long = 400L,
    repeatIntervalMillis: Long = 120L,
    onValueChangeFinished: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

    return this
        .indication(interactionSource, ripple())
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    currentOnClick()

                    var isHeld = true
                    val repeatJob = launch {
                        delay(initialDelayMillis)
                        while (isHeld) {
                            currentOnClick()
                            delay(repeatIntervalMillis)
                        }
                    }

                    try {
                        val up = waitForUpOrCancellation()
                        isHeld = false
                        repeatJob.cancel()
                        if (up != null) {
                            up.consume()
                            interactionSource.tryEmit(PressInteraction.Release(press))
                        } else {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                        currentOnFinished?.invoke()
                    } finally {
                        isHeld = false
                        repeatJob.cancel()
                    }
                }
            }
        }
}

/**
 * InlineSlider styled strictly according to Material 3 Expressive / Wear OS guidelines:
 * - Seamless uniform stadium pill container
 * - D-shaped decrease button with outline speaker (volume down) + hold-to-repeat
 * - 6-segment central track with curved end caps and fluid continuous scrubbing
 * - D-shaped increase button with outline speaker (volume up) + hold-to-repeat
 * - Snappy segment-boundary animation on button click
 */
@Composable
fun InlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..15f,
    steps: Int = 5,
    decreaseIcon: @Composable () -> Unit = {
        Icon(
            painter = painterResource(R.drawable.volume_down),
            contentDescription = "Decrease",
            modifier = Modifier.size(20.dp)
        )
    },
    increaseIcon: @Composable () -> Unit = {
        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = "Increase",
            modifier = Modifier.size(20.dp)
        )
    },
    onValueChangeFinished: (() -> Unit)? = null
) {
    val totalRange = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val targetFraction = ((value - valueRange.start) / totalRange).coerceIn(0f, 1f)
    val totalSegments = if (steps > 0) (steps + 1) else 6

    var isDragging by remember { mutableStateOf(false) }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isDragging) {
            snap()
        } else {
            tween(durationMillis = 150, easing = FastOutSlowInEasing)
        },
        label = "inlineSliderProgress"
    )

    val canDecrease = enabled && value > valueRange.start
    val canIncrease = enabled && value < valueRange.endInclusive

    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val iconColor = MaterialTheme.colorScheme.onSurface
    val disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    val decreaseInteractionSource = remember { MutableInteractionSource() }
    val increaseInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(containerColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Button: D-shaped decrease cap with hold-to-repeat
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .clip(LeftCapShape)
                .repeatableClickable(
                    interactionSource = decreaseInteractionSource,
                    enabled = canDecrease,
                    onValueChangeFinished = onValueChangeFinished
                ) {
                    val prevSegment = (ceil(targetFraction * totalSegments - 0.001f).toInt() - 1).coerceAtLeast(0)
                    val prevFraction = prevSegment.toFloat() / totalSegments
                    val nextValue = valueRange.start + prevFraction * totalRange
                    onValueChange(nextValue)
                },
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides (if (canDecrease) iconColor else disabledIconColor)
            ) {
                decreaseIcon()
            }
        }

        // Left Seam Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(dividerColor)
        )

        // Center: 6 Curved Segmented Blocks with Fluid Scrubbing & Snappy Animation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RectangleShape)
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        val trackPaddingPx = 10.dp.toPx()
                        val availableWidth = (size.width - 2 * trackPaddingPx).coerceAtLeast(1f)
                        val touchFraction = ((down.position.x - trackPaddingPx) / availableWidth).coerceIn(0f, 1f)
                        val initialValue = valueRange.start + touchFraction * totalRange
                        onValueChange(initialValue)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                isDragging = false
                                onValueChangeFinished?.invoke()
                                break
                            }
                            change.consume()
                            val dragFraction = ((change.position.x - trackPaddingPx) / availableWidth).coerceIn(0f, 1f)
                            val draggedValue = valueRange.start + dragFraction * totalRange
                            onValueChange(draggedValue)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalSegments) {
                    val segStart = i.toFloat() / totalSegments
                    val segEnd = (i + 1).toFloat() / totalSegments
                    val segFillFraction = ((animatedFraction - segStart) / (segEnd - segStart)).coerceIn(0f, 1f)

                    val shape = when (i) {
                        0 -> FirstSegmentShape
                        totalSegments - 1 -> LastSegmentShape
                        else -> RectangleShape
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(inactiveTrackColor)
                    ) {
                        if (segFillFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segFillFraction)
                                    .background(activeTrackColor)
                            )
                        }
                    }
                }
            }
        }

        // Right Seam Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(dividerColor)
        )

        // Right Button: D-shaped increase cap with hold-to-repeat
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .clip(RightCapShape)
                .repeatableClickable(
                    interactionSource = increaseInteractionSource,
                    enabled = canIncrease,
                    onValueChangeFinished = onValueChangeFinished
                ) {
                    val nextSegment = (floor(targetFraction * totalSegments + 0.001f).toInt() + 1).coerceAtMost(totalSegments)
                    val nextFraction = nextSegment.toFloat() / totalSegments
                    val nextValue = valueRange.start + nextFraction * totalRange
                    onValueChange(nextValue)
                },
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides (if (canIncrease) iconColor else disabledIconColor)
            ) {
                increaseIcon()
            }
        }
    }
}

/**
 * ViviVolumeSlider component delegating to clean InlineSlider.
 */
@Composable
fun ViviVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 5,
    onValueChangeFinished: (() -> Unit)? = null
) {
    InlineSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        onValueChangeFinished = onValueChangeFinished
    )
}
