package com.music.vivi.Dotlyrics

import androidx.compose.runtime.Composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
// Add these imports if not already present:
// import androidx.compose.animation.core.*
// import androidx.compose.foundation.background
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.ui.graphics.graphicsLayer

// Add this new composable for the animated beat dots
@Composable
fun AnimatedMusicBeatDots(
    isPlaying: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beat_animation")

    // Create staggered animations for each dot
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_scale"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = 200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_scale"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = 400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_scale"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot 1
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = dot1Scale
                    scaleY = dot1Scale
                }
                .background(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        )

        // Dot 2
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = dot2Scale
                    scaleY = dot2Scale
                }
                .background(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        )

        // Dot 3
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = dot3Scale
                    scaleY = dot3Scale
                }
                .background(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        )
    }
}