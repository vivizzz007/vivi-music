package com.music.vivi.dotlyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin


// Add your DotLoadingProgress composable here



@Composable
fun DotLoadingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    isCurrentLine: Boolean = false
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val dotCount = 3
    val dotSpacing = 20.dp

    // All dots animate together based on the overall progress
    val dotProgress = sin(clampedProgress * PI).toFloat()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing)
    ) {
        repeat(dotCount) { index ->
            // Apple-style bounce animation for all dots
            val animatedScale by animateFloatAsState(
                targetValue = when {
                    dotProgress > 0.8f -> if (isCurrentLine) 1.4f else 1.2f
                    dotProgress > 0.3f -> if (isCurrentLine) 1.2f else 1.1f
                    else -> 1f
                },
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 400f
                ),
                label = "dotScale_$index"
            )

            // Smooth alpha transition with Apple-style easing
            val animatedAlpha by animateFloatAsState(
                targetValue = when {
                    dotProgress > 0.5f -> color.alpha
                    dotProgress > 0.1f -> color.alpha * 0.7f
                    else -> color.alpha * 0.3f
                },
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                ),
                label = "dotAlpha_$index"
            )

            // Subtle glow effect for current line
            val glowAlpha by animateFloatAsState(
                targetValue = if (isCurrentLine && dotProgress > 0.5f) 0.4f else 0f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                ),
                label = "glowAlpha_$index"
            )

            // Color transition for dots
            val animatedColor by animateColorAsState(
                targetValue = when {
                    dotProgress > 0.7f && isCurrentLine -> color
                    dotProgress > 0.5f -> color.copy(alpha = 0.9f)
                    dotProgress > 0.2f -> color.copy(alpha = 0.6f)
                    else -> color.copy(alpha = 0.3f)
                },
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                ),
                label = "dotColor_$index"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            ) {
                // Glow effect background for current line
                if (isCurrentLine && glowAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color.copy(alpha = glowAlpha),
                                shape = CircleShape
                            )
                            .blur(4.dp)
                    )
                }

                // Main dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.Center)
                        .background(
                            animatedColor.copy(alpha = animatedAlpha),
                            shape = CircleShape
                        )
                        .then(
                            if (isCurrentLine && dotProgress > 0.5f) {
                                Modifier.shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    spotColor = color.copy(alpha = 0.3f)
                                )
                            } else Modifier
                        )
                )
            }
        }
    }
}
