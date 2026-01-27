package com.music.vivi.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

import com.music.vivi.R
// import com.music.vivi.models.MediaMetadata // Explicit usage in signature to avoid ambiguity

/**
 * A CD-Style Player Cover.
 * Shows the album art as a "Jewel Case" (square) on top.
 * A "Disc" spins behind it, sliding out slightly when playing.
 * The disc art is generated from the album cover by applying a circular mask and a "central hole".
 */
@Composable
fun CDPlayerCover(
    mediaMetadata: com.music.vivi.models.MediaMetadata?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Smooth rotation logic
        val currentRotation = remember { androidx.compose.animation.core.Animatable(0f) }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                // Infinite loop to rotate
                while (true) {
                    currentRotation.animateTo(
                        targetValue = currentRotation.value + 360f,
                        animationSpec = tween(8000, easing = LinearEasing)
                    )
                }
            } else {
                // When paused, we stop animating.
                // However, we want to maintain the current rotation.
                // The coroutine cancellation stops the animation cleanly at the current value.
            }
        }

        // Slide out animation for the disc
        val density = LocalDensity.current
        val translationXPx = remember(density) { with(density) { 60.dp.toPx() } }

        // Use state object directly to avoid recomposition on every frame
        val translationXState = animateFloatAsState(
            targetValue = if (isPlaying) translationXPx else 0f,
            animationSpec = tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "slide"
        )

        val context = LocalContext.current

        // Remember the ImageRequest to avoid rebuilding it on every recomposition
        val imageRequest = remember(mediaMetadata?.thumbnailUrl, context) {
            if (mediaMetadata != null) {
                ImageRequest.Builder(context)
                    .data(mediaMetadata.thumbnailUrl)
                    .crossfade(true)
                    .build()
            } else null
        }

        // Remember expensive Brush creation
        val cdReflectionBrush = remember {
             Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                )
            )
        }

        // Disc (Behind)
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f)
                .graphicsLayer {
                    translationX = translationXState.value // Read state inside graphicsLayer
                    rotationZ = currentRotation.value // Read Animatable value inside graphicsLayer
                }
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.DarkGray)
        ) {
            // Disc texture (Crop of Album Art)
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = mediaMetadata?.title ?: "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // Vinyl/CD Effects Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // 1. Central Hole (Transparent/Black)
                drawCircle(
                    color = Color.Black,
                    radius = radius * 0.15f,
                    center = center
                )

                // 2. Subtle radial gradients to simulate CD reflection
                drawCircle(
                    brush = cdReflectionBrush,
                    radius = radius,
                    center = center
                )

                // 3. Border ring
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Jewel Case Cover (Front)
        // Stays static, square.
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f) // Match size of disc visually
                .shadow(12.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = mediaMetadata?.title ?: "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                // Glossy overlay for "Plastic Case" feel
                // Creating this gradient is cheap enough, but could be remembered if desired.
                // Keeping inline for now as it depends on size (though LinearGradient doesn't strictly depend on size unless using shader, Brush does).
                // Actually Brush.linearGradient depends on Start/End coordinates which depend on size.
                // Canvas 'drawRect' with Brush handles this.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )

                    // Spine detail
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(4.dp.toPx(), 0f),
                        end = Offset(4.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else {
                 // Placeholder
                 Image(
                     painter = painterResource(R.drawable.music_note),
                     contentDescription = "No Album Art",
                     modifier = Modifier.fillMaxSize().padding(32.dp),
                     colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                 )
            }
        }
    }
}
