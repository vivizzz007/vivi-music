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
    val defaultColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)

    var extractedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl != null) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val bitmap = result.image?.toBitmap()

                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val vibrantSwatch = palette.vibrantSwatch
                        val darkVibrantSwatch = palette.darkVibrantSwatch
                        val dominantSwatch = palette.dominantSwatch
                        val mutedSwatch = palette.mutedSwatch

                        val colorInt = vibrantSwatch?.rgb
                            ?: darkVibrantSwatch?.rgb
                            ?: dominantSwatch?.rgb
                            ?: mutedSwatch?.rgb

                        if (colorInt != null) {
                            extractedColor = Color(colorInt)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = extractedColor?.copy(alpha = 0.5f) ?: defaultColor,
        animationSpec = tween(durationMillis = 500),
        label = "AlbumGradientColor"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    animatedColor,
                    surfaceColor
                )
            )
        )
    )
}
