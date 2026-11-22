/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.player.backdrop

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val BLUR_TARGET_SIZE_PX = 128
private const val BLUR_RADIUS = 10
private const val BLUR_PASSES = 3

private val blurCache = ConcurrentHashMap<String, ImageBitmap>()

/**
 * Loads, downsamples, and box-blurs the artwork off the main thread.
 * Caches by URL to avoid re-blurring on recompositions.
 * Holds the previous blurred artwork until the new one is prepared to eliminate flickers.
 */
@Composable
fun rememberArtworkBlurBitmap(artworkUri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    val url = artworkUri?.toString()

    var blurredBitmap by remember {
        mutableStateOf(url?.let(blurCache::get))
    }

    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect

        // Return immediately if cached
        blurCache[url]?.let {
            blurredBitmap = it
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .size(BLUR_TARGET_SIZE_PX, BLUR_TARGET_SIZE_PX)
                .scale(Scale.FILL)
                .allowHardware(false)
                .build()

            val sourceBitmap = runCatching {
                context.imageLoader.execute(request).image?.toBitmap()
            }.getOrNull() ?: return@withContext

            val blurred = withContext(Dispatchers.Default) {
                applyBoxBlur(sourceBitmap, BLUR_PASSES, BLUR_RADIUS)
            }

            if (blurred != null) {
                blurCache[url] = blurred
                blurredBitmap = blurred
            }
        }
    }

    return blurredBitmap
}

private fun applyBoxBlur(src: Bitmap, passes: Int, radius: Int): ImageBitmap? {
    val width = src.width
    val height = src.height
    if (width < 2 || height < 2) return src.asImageBitmap()

    val softwareBitmap = if (src.config == Bitmap.Config.HARDWARE) {
        src.copy(Bitmap.Config.ARGB_8888, false)
    } else src

    val scaledWidth = minOf(width, BLUR_TARGET_SIZE_PX)
    val scaledHeight = (height * scaledWidth / width.toFloat()).toInt().coerceAtLeast(4)
    val small = Bitmap.createScaledBitmap(softwareBitmap, scaledWidth, scaledHeight, true)

    val pixels = IntArray(scaledWidth * scaledHeight)
    small.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

    repeat(passes) {
        boxBlurPass(pixels, scaledWidth, scaledHeight, radius, horizontal = true)
        boxBlurPass(pixels, scaledWidth, scaledHeight, radius, horizontal = false)
    }

    val output = Bitmap.createBitmap(pixels, scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
    return output.asImageBitmap()
}

/**
 * Fast running-sum box blur pass along horizontal rows or vertical columns.
 * Clamps edge pixels for smooth borders without black vignettes.
 */
private fun boxBlurPass(
    pixels: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    horizontal: Boolean
) {
    val lines = if (horizontal) height else width
    val length = if (horizontal) width else height
    val line = IntArray(length)
    val window = radius * 2 + 1

    for (l in 0 until lines) {
        fun at(i: Int) = if (horizontal) l * width + i else i * width + l

        for (i in 0 until length) {
            line[i] = pixels[at(i)]
        }

        var r = 0
        var g = 0
        var b = 0

        for (k in -radius..radius) {
            val p = line[k.coerceIn(0, length - 1)]
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }

        for (i in 0 until length) {
            pixels[at(i)] = (0xFF shl 24) or
                ((r / window) shl 16) or
                ((g / window) shl 8) or
                (b / window)

            val out = line[(i - radius).coerceIn(0, length - 1)]
            val inn = line[(i + radius + 1).coerceIn(0, length - 1)]

            r += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            g += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            b += (inn and 0xFF) - (out and 0xFF)
        }
    }
}

