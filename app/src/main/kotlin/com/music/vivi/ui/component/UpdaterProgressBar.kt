/*
 * Derivative work based on ImageToolbox's FancySlider (Apache License 2.0)
 * Original Copyright (c) T8RIN (Malik Mukhametzyanov)
 * Modifications for ViviMusic Updater include custom sizing, track logic, and UI integration.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun UpdaterProgressBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    progress: Float = 0f,
    isIndeterminate: Boolean = false,
    trackHeight: Dp = 38.dp, // Thick FancySlider track
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    activeTickColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    inactiveTickColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    thumbColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    numTicks: Int = 20, // Reduced from 100 to 20 for a more visually appealing spacing
    shape: Shape = CircleShape
) {
    // For indeterminate, animate window moving across track
    val infiniteTransition = rememberInfiniteTransition(label = "IndeterminateSliderTransition")
    
    val animatedPos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IndeterminatePosition"
    )

    val currentThumbPos = if (isIndeterminate) animatedPos else progress.coerceIn(0f, 1f)
    
    val activeRangeStart = 0f
    val activeRangeEnd = currentThumbPos.coerceIn(0f, 1f)
    
    val tickFractions = FloatArray(numTicks + 1) { i -> i.toFloat() / numTicks }

    Box(
        modifier = modifier
    ) {
        // The Thick Track Canvas with tiny dots
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
        ) {
            drawFancyTrack(
                        tickFractions = tickFractions,
                        activeRangeStart = activeRangeStart,
                        activeRangeEnd = activeRangeEnd,
                        inactiveTrackColor = inactiveColor,
                        activeTrackColor = activeColor,
                        inactiveTickColor = inactiveTickColor,
                        activeTickColor = activeTickColor,
                        shape = shape
                    )
                }
                
        // The Thumb on top of the track
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .zIndex(100f)
        ) {
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val visualThumbPos = if (isRtl) 1f - currentThumbPos else currentThumbPos
            
            val thumbRadius = 13.dp.toPx()
            // Allow the thumb to glide exactly exactly within the pill-shaped track bounds
            val trackCapRadius = size.height / 2f
            // We only pad by the track cap radius so it hits the very edges of the pill curve perfectly
            val thumbAvailableWidth = size.width - 2 * trackCapRadius
            val thumbX = trackCapRadius + (thumbAvailableWidth * visualThumbPos)
            
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX, size.height / 2f)
            )
        }
    }
}

private fun DrawScope.drawFancyTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    shape: Shape
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val activeStart = if (isRtl) 1f - activeRangeEnd else activeRangeStart
    val activeEnd = if (isRtl) 1f - activeRangeStart else activeRangeEnd
    val tickSize = 4.dp.toPx() 

    // Draw the full inactive background track
    drawFancyTrackSegment(
        startFraction = 0f,
        endFraction = 1f,
        color = inactiveTrackColor,
        shape = shape
    )
    
    // Draw the active track portion representing progress
    drawFancyTrackSegment(
        startFraction = 1f,
        endFraction = activeEnd,
        color = activeTrackColor,
        shape = shape
    )

    // Draw the evenly spaced tracking dots
    val availableDotWidth = size.width - size.height // Accounting for the semi-circle caps (size.height/2 on left and right)
    for (tick in tickFractions) {
        val outsideFraction = tick !in activeRangeStart..activeRangeEnd
        val visualTick = if (isRtl) 1f - tick else tick
        val tickX = (size.height / 2f) + (availableDotWidth * visualTick)
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(tickX, center.y),
            radius = tickSize / 2f
        )
    }
}

private fun DrawScope.drawFancyTrackSegment(
    startFraction: Float,
    endFraction: Float,
    color: Color,
    shape: Shape
) {
    if (endFraction <= 0f) return
    val width = size.width * endFraction.coerceIn(0f, 1f)

    val outline = shape.createOutline(
        size = Size(
            width = width,
            height = size.height
        ),
        layoutDirection = layoutDirection,
        density = this
    )

    when (outline) {
        is Outline.Rectangle -> drawRect(color = color, topLeft = outline.rect.topLeft, size = outline.rect.size)
        is Outline.Rounded -> drawPath(path = Path().apply { addRoundRect(outline.roundRect) }, color = color)
        is Outline.Generic -> drawPath(path = outline.path, color = color)
    }
}


