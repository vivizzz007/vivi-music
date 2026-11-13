/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.player.backdrop

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

/**
 * High-performance blurred artwork backdrop modeled after BitChord.
 * Crossfades smoothly on track changes and applies a dark contrast scrim
 * so all player controls, typography, and lyrics maintain sharp legibility.
 */
@Composable
fun PlayerBlurBackdrop(
    artworkUri: Uri?,
    modifier: Modifier = Modifier
) {
    val blurBitmap = rememberArtworkBlurBitmap(artworkUri)

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Crossfade(
            targetState = blurBitmap,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "playerBlurBackdropCrossfade"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Contrast scrim: dark gradient keeping player controls and lyrics readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )
    }
}

