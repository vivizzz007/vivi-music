package com.music.vivi.crossfade



import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

import androidx.compose.animation.core.*

@Composable
fun <T> CrossfadeTransition(
    currentItem: T?,
    previousItem: T? = null,
    crossfadeEnabled: Boolean = true,
    crossfadeDuration: Int = 1000,
    fadeEasing: Easing = FastOutSlowInEasing,
    additionalAnimations: (Float) -> Modifier = { Modifier }, // Allows scaling/sliding effects
    content: @Composable (T?, Float) -> Unit
) {
    val transition = updateTransition(
        targetState = currentItem,
        label = "CrossfadeTransition"
    )

    val progress by transition.animateFloat(
        transitionSpec = {
            if (crossfadeEnabled && previousItem != null && currentItem != null && previousItem != currentItem) {
                tween(
                    durationMillis = crossfadeDuration,
                    easing = fadeEasing
                )
            } else {
                snap(delayMillis = 0) // Instant transition when disabled
            }
        },
        label = "CrossfadeProgress"
    ) { if (it == currentItem) 1f else 0f }

    Box(modifier = Modifier.fillMaxSize()) {
        // Previous item fading out with optional transformations
        if (previousItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f - progress)
                    .then(additionalAnimations(1f - progress))
            ) {
                content(previousItem, 1f - progress)
            }
        }

        // Current item fading in with optional transformations
        if (currentItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(progress)
                    .then(additionalAnimations(progress))
            ) {
                content(currentItem, progress)
            }
        }
    }
}