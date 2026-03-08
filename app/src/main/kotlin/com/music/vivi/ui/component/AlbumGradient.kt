/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import com.music.vivi.ui.theme.PlayerColorExtractor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun AlbumGradient(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColorInt = MaterialTheme.colorScheme.primaryContainer.toArgb()

    var extractedColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl != null) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val bitmap = result.image?.toBitmap()

                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                        val colors = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColorInt
                        )
                        extractedColors = colors
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val color1 by animateColorAsState(
        targetValue = extractedColors.getOrNull(0)?.copy(alpha = 0.5f) ?: surfaceColor,
        animationSpec = tween(durationMillis = 800),
        label = "AlbumGradientColor1"
    )
    val color2 by animateColorAsState(
        targetValue = extractedColors.getOrNull(1)?.copy(alpha = 0.3f) ?: surfaceColor,
        animationSpec = tween(durationMillis = 800),
        label = "AlbumGradientColor2"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to color1,
                    0.5f to color2,
                    1.0f to surfaceColor
                )
            )
        )
    )
}
