package com.music.vivi.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Animated breathing dots indicator for instrumental tracks.
 * Mimics Apple Music's instrumental visualizer.
 */
@Composable
public fun InstrumentalDots(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "instrumental_dots")

    Row(
        modifier = modifier
            .padding(vertical = 24.dp), // More vertical space
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Apple Music style: 3 large, smooth breathing dots
        // They wave in sequence (scale and alpha)
        val dotSize = 10.dp
        val spacing = 8.dp

        repeat(3) { index ->
            val delay = index * 200 // Staggered wave

            val animationProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot_progress_$index"
            )

            // Sine wave calculation for smooth breathing
            val fraction = animationProgress * 2 * Math.PI
            val alpha = (kotlin.math.sin(fraction).toFloat() + 1f) / 2f * 0.7f + 0.3f // Range 0.3..1.0
            val scale = (kotlin.math.sin(fraction).toFloat() + 1f) / 2f * 0.4f + 0.8f // Range 0.8..1.2

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(dotColor, CircleShape)
            )

            if (index < 2) {
                Spacer(modifier = Modifier.width(spacing))
            }
        }
    }
}
