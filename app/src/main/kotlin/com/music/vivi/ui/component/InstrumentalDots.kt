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

@Composable
fun InstrumentalDots(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    val infiniteTransition = rememberInfiniteTransition(label = "instrumental_dots")

    Row(
        modifier = modifier
            .padding(vertical = 12.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 300
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha_$index"
            )

            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )

            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer glow layer
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(alpha * 0.3f)
                        .graphicsLayer {
                            scaleX = scale * 1.5f
                            scaleY = scale * 1.5f
                        }
                        .background(dotColor, CircleShape)
                )
                
                // Main dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .background(dotColor, CircleShape)
                )
            }
            
            if (index < 2) {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
