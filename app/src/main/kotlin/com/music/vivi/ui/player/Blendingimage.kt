/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.ui.player

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.music.vivi.ui.utils.resize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sin

private const val MASK = 32

private fun smoothstep(a: Float, b: Float, x: Float): Float {
    val k = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

/**
 * Procedural Apple Music AutoMix liquid cover transition.
 * Song B's cover organically flows into Song A's cover through soft, drifting patches.
 *
 * @param progress 0f = only cover A, 1f = only cover B
 * @param timeSec  playback time in seconds driving the organic patch drift
 */
@Composable
fun AutoMixCover(
    coverA: Bitmap,
    coverB: Bitmap,
    progress: Float,
    timeSec: Float,
    modifier: Modifier = Modifier
) {
    val mask = remember { Bitmap.createBitmap(MASK, MASK, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(MASK * MASK) }
    val imgPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }
    val maskPaint = remember {
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val s = timeSec * 0.7f
        val f = 0.22f
        val th = -0.3f + 1.6f * progress
        for (y in 0 until MASK) {
            val v = y / (MASK - 1f)
            val vTerm = 0.17f * sin(v * 4.4f - s * 1.2f)
            val yOffset = y * MASK
            for (x in 0 until MASK) {
                val u = x / (MASK - 1f)
                val n = 0.5f +
                    0.20f * sin(u * 5.2f + s) +
                    vTerm + 0.13f * sin((u + v) * 7.5f + s * 0.8f)
                val a = (255f * smoothstep(n - f, n + f, th)).toInt()
                pixels[yOffset + x] = (a shl 24) or 0x00FFFFFF
            }
        }
        mask.setPixels(pixels, 0, MASK, 0, 0, MASK, MASK)

        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            val w = size.width
            val h = size.height

            drawCover(nc, coverA, w, h, imgPaint)

            val layer = nc.saveLayer(0f, 0f, w, h, null)
            drawCover(nc, coverB, w, h, imgPaint)
            nc.drawBitmap(mask, null, RectF(0f, 0f, w, h), maskPaint)
            nc.restoreToCount(layer)
        }
    }
}

/** Center-crop the bitmap to fill w x h. */
private fun drawCover(
    nc: android.graphics.Canvas,
    bmp: Bitmap,
    w: Float,
    h: Float,
    paint: Paint
) {
    val k = max(w / bmp.width, h / bmp.height)
    val srcW = w / k
    val srcH = h / k
    val left = (bmp.width - srcW) / 2f
    val top = (bmp.height - srcH) / 2f
    val src = Rect(left.toInt(), top.toInt(), (left + srcW).toInt(), (top + srcH).toInt())
    nc.drawBitmap(bmp, src, RectF(0f, 0f, w, h), paint)
}

private val bitmapCache = LruCache<String, Bitmap>(10)

@Composable
fun Blendingimage(
    fromCoverUrl: String?,
    toCoverUrl: String?,
    nextCoverUrl: String? = null,
    progress: Float,
    crossfadeDuration: Float = 6f,
    crossfadeEnabled: Boolean,
    cropAlbumArt: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var coverA by remember(fromCoverUrl) { mutableStateOf(fromCoverUrl?.let { bitmapCache.get(it) }) }
    var coverB by remember(toCoverUrl) { mutableStateOf(toCoverUrl?.let { bitmapCache.get(it) }) }

    // Preload Song A cover on IO dispatcher
    LaunchedEffect(fromCoverUrl) {
        if (!fromCoverUrl.isNullOrBlank() && coverA == null) {
            val cached = bitmapCache.get(fromCoverUrl)
            if (cached != null) {
                coverA = cached
            } else {
                withContext(Dispatchers.IO) {
                    val req = ImageRequest.Builder(context)
                        .data(fromCoverUrl.resize(800, 800))
                        .allowHardware(false)
                        .build()
                    context.imageLoader.execute(req).image?.toBitmap()?.let {
                        bitmapCache.put(fromCoverUrl, it)
                        coverA = it
                    }
                }
            }
        }
    }

    // Preload next track's cover in advance while Song A is playing
    LaunchedEffect(nextCoverUrl) {
        if (!nextCoverUrl.isNullOrBlank() && bitmapCache.get(nextCoverUrl) == null) {
            withContext(Dispatchers.IO) {
                val req = ImageRequest.Builder(context)
                    .data(nextCoverUrl.resize(800, 800))
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(req).image?.toBitmap()?.let {
                    bitmapCache.put(nextCoverUrl, it)
                }
            }
        }
    }

    // Load Song B cover immediately on IO dispatcher
    LaunchedEffect(toCoverUrl) {
        if (!toCoverUrl.isNullOrBlank() && coverB == null) {
            val cached = bitmapCache.get(toCoverUrl)
            if (cached != null) {
                coverB = cached
            } else {
                withContext(Dispatchers.IO) {
                    val req = ImageRequest.Builder(context)
                        .data(toCoverUrl.resize(800, 800))
                        .allowHardware(false)
                        .build()
                    context.imageLoader.execute(req).image?.toBitmap()?.let {
                        bitmapCache.put(toCoverUrl, it)
                        coverB = it
                    }
                }
            }
        }
    }

    val safeProgress = progress.coerceIn(0f, 1f)

    if (!crossfadeEnabled || toCoverUrl.isNullOrBlank() || safeProgress <= 0f) {
        val targetUrl = if (safeProgress >= 1f && !toCoverUrl.isNullOrBlank()) toCoverUrl else (fromCoverUrl ?: toCoverUrl)
        val isYtThumbnail = remember(targetUrl) {
            targetUrl?.let { it.contains("ytimg") || it.contains("/vi/") || it.contains("youtube.com") } == true
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(targetUrl?.resize(1200, 1200))
                .crossfade(400)
                .build(),
            contentDescription = "Cover Art",
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (cropAlbumArt && isYtThumbnail) {
                        scaleX = 4f / 3f
                        scaleY = 4f / 3f
                    }
                },
            contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit
        )
        return
    }

    val bmpA = coverA ?: fromCoverUrl?.let { bitmapCache.get(it) }
    val bmpB = coverB ?: toCoverUrl?.let { bitmapCache.get(it) }
    val timeSec = safeProgress * crossfadeDuration

    if (bmpA != null && bmpB != null) {
        // Organic liquid AutoMix transition
        AutoMixCover(
            coverA = bmpA,
            coverB = bmpB,
            progress = safeProgress,
            timeSec = timeSec,
            modifier = modifier
        )
    } else {
        // Smooth continuous cross-dissolve fallback (NO sudden 5/5 jump)
        val isYtA = remember(fromCoverUrl) {
            fromCoverUrl?.let { it.contains("ytimg") || it.contains("/vi/") || it.contains("youtube.com") } == true
        }
        val isYtB = remember(toCoverUrl) {
            toCoverUrl.contains("ytimg") || toCoverUrl.contains("/vi/") || toCoverUrl.contains("youtube.com")
        }
        Box(modifier = modifier.fillMaxSize()) {
            if (!fromCoverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fromCoverUrl.resize(1200, 1200),
                    contentDescription = "Cover Art A",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - safeProgress
                            if (cropAlbumArt && isYtA) {
                                scaleX = 4f / 3f
                                scaleY = 4f / 3f
                            }
                        },
                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit
                )
            }
            AsyncImage(
                model = toCoverUrl.resize(1200, 1200),
                contentDescription = "Cover Art B",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = safeProgress
                        if (cropAlbumArt && isYtB) {
                            scaleX = 4f / 3f
                            scaleY = 4f / 3f
                        }
                    },
                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit
            )
        }
    }
}
