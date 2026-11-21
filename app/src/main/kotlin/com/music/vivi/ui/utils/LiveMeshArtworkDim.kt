/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val ARTWORK_DIM_SAMPLE_SIZE = 32
internal const val ARTWORK_DIM_STRENGTH = 0.3f
internal const val ARTWORK_DIM_MAX_REDUCTION = 0.8f
private const val HIGHLIGHT_PERCENTILE = 0.9f

internal class ArtworkPixels(
    val argb: IntArray,
    val width: Int,
    val height: Int,
)

// The 90th percentile ignores a few small specular spots, so one white logo does not dim a dark cover.
internal fun highlightLuminance(argbPixels: IntArray): Float {
    if (argbPixels.isEmpty()) return 0f
    val luminances = FloatArray(argbPixels.size) { index ->
        val color = argbPixels[index]
        (0.2126f * ((color shr 16) and 0xFF) + 0.7152f * ((color shr 8) and 0xFF) + 0.0722f * (color and 0xFF)) / 255f
    }
    luminances.sort()
    val index = (luminances.size * HIGHLIGHT_PERCENTILE).toInt().coerceAtMost(luminances.lastIndex)
    return luminances[index]
}

// Multiplier for the whole background: 1 leaves it alone, lower values darken bright artwork so text stays readable.
internal fun artworkDimFactor(highlight: Float, strength: Float = ARTWORK_DIM_STRENGTH): Float {
    val target = 1f - ARTWORK_DIM_MAX_REDUCTION * strength.coerceIn(0f, 1f)
    if (highlight <= target) return 1f
    return target / highlight
}

@Composable
internal fun rememberArtworkPixels(thumbnailUrl: String?): ArtworkPixels? {
    val context = LocalContext.current
    val pixels by produceState<ArtworkPixels?>(initialValue = null, thumbnailUrl) {
        value = null
        if (thumbnailUrl == null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val result = context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .size(ARTWORK_DIM_SAMPLE_SIZE, ARTWORK_DIM_SAMPLE_SIZE)
                        .allowHardware(false)
                        .build()
                )
                val bitmap: Bitmap = result.image?.toBitmap() ?: return@runCatching null
                val argb = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                ArtworkPixels(argb, bitmap.width, bitmap.height)
            }.getOrNull()
        }
    }
    return pixels
}

// Dims by the cover's highlight; the strength comes from the motion settings, so changing it re-dims without reloading.
@Composable
internal fun rememberArtworkDimFactor(thumbnailUrl: String?): Float {
    val pixels = rememberArtworkPixels(thumbnailUrl)
    val strength = rememberLiveMeshMotion().motion.artworkDimStrength
    val highlight = remember(pixels) { pixels?.let { highlightLuminance(it.argb) } }
    return highlight?.let { artworkDimFactor(it, strength) } ?: 1f
}

@Composable
internal fun rememberArtworkPalette(thumbnailUrl: String?): List<Color> {
    val pixels = rememberArtworkPixels(thumbnailUrl)
    return remember(pixels) {
        pixels?.let { paletteFromPixels(it.argb, it.width, it.height) } ?: LiveMeshSamplePalette
    }
}
