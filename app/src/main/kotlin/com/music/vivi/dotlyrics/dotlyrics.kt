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
import androidx.compose.ui.draw.scale
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
    val infiniteTransition = rememberInfiniteTransition(label = "DotLoading")

    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing), // Slightly faster
            repeatMode = RepeatMode.Restart
        ),
        label = "DotProgress"
    )

    // Enhanced glow animation
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(if (isCurrentLine) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(3) { index ->
            val dotAlpha by animateFloatAsState(
                targetValue = when {
                    // Use actual progress if available, otherwise use animated progress
                    progress > 0f -> {
                        val progressThreshold = (index + 1) / 3f
                        if (progress >= progressThreshold) 1f else 0.4f
                    }
                    else -> {
                        val delay = index * 0.2f
                        val normalizedProgress = ((animatedProgress + delay) % 1f)
                        when {
                            normalizedProgress < 0.3f -> (normalizedProgress / 0.3f).coerceIn(0.4f, 1f)
                            normalizedProgress < 0.7f -> 1f
                            else -> ((1f - normalizedProgress) / 0.3f).coerceIn(0.4f, 1f)
                        }
                    }
                },
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "DotAlpha$index"
            )

            val dotScale by animateFloatAsState(
                targetValue = when {
                    progress > 0f -> {
                        val progressThreshold = (index + 1) / 3f
                        if (progress >= progressThreshold) 1.1f else 0.9f
                    }
                    else -> {
                        val delay = index * 0.2f
                        val normalizedProgress = ((animatedProgress + delay) % 1f)
                        if (normalizedProgress < 0.5f) 1f + (normalizedProgress * 0.3f) else 1f + ((1f - normalizedProgress) * 0.3f)
                    }
                },
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "DotScale$index"
            )

            Box(
                modifier = Modifier
                    .size(if (isCurrentLine) 14.dp else 10.dp) // Slightly larger
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                    }
            ) {
                // Glow effect background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(glowPulse)
                        .background(
                            color.copy(alpha = (dotAlpha * 0.3f).coerceAtMost(0.5f)),
                            shape = CircleShape
                        )
                        .blur(4.dp)
                )

                // Main dot with enhanced brightness
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = when {
                                isCurrentLine -> color.copy(alpha = dotAlpha * 1.2f) // Extra bright for current line
                                else -> color.copy(alpha = dotAlpha)
                            },
                            shape = CircleShape
                        )
                        .then(
                            if (isCurrentLine) {
                                Modifier.shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape,
                                    ambientColor = color.copy(alpha = 0.6f),
                                    spotColor = color.copy(alpha = 0.8f)
                                )
                            } else Modifier
                        )
                )

                // Inner bright core for extra glow
                if (dotAlpha > 0.7f) {
                    Box(
                        modifier = Modifier
                            .size(if (isCurrentLine) 6.dp else 4.dp)
                            .align(Alignment.Center)
                            .background(
                                Color.White.copy(alpha = (dotAlpha - 0.7f) * 1.5f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}