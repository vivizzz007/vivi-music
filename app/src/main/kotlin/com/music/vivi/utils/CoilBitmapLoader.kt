/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.music.vivi.R
import com.music.vivi.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import timber.log.Timber

class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    private fun createFallbackBitmap(): Bitmap =
        createBitmap(64, 64)

    private fun Bitmap.copyIfNeeded(): Bitmap {
        return if (isRecycled) {
            createFallbackBitmap()
        } else {
            try {
                copy(Bitmap.Config.ARGB_8888, false) ?: createFallbackBitmap()
            } catch (e: Exception) {
                createFallbackBitmap()
            }
        }
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                bitmap?.copyIfNeeded() ?: createFallbackBitmap()
            } catch (e: Exception) {
                Timber.tag("CoilBitmapLoader").w(e, "Failed to decode bitmap data")
                createFallbackBitmap()
            }
        }

    private fun placeholderBitmap(): Bitmap {
        return try {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.music_note)
                ?: return createFallbackBitmap()
            val size = (256 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            val bitmap = createBitmap(size, size)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            bitmap
        } catch (e: Exception) {
            Timber.tag("CoilBitmapLoader").w(e, "Failed to render placeholder artwork")
            createFallbackBitmap()
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            // Normalize to the canonical size variant so this request hits the same
            // disk-cache entry the UI precaches (Thumbnail.kt, DownloadUtil).
            // MediaItem artwork URIs are stored in arbitrary sizes, which caused
            // cache misses (and an empty shade icon) when offline.
            val normalized = try {
                uri.toString().resize(1200, 1200).toUri()
            } catch (e: Exception) {
                uri
            }
            val request = ImageRequest.Builder(context)
                .data(normalized)
                .allowHardware(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()

            val result = context.imageLoader.execute(request)

            when (result) {
                is ErrorResult -> {
                    Timber.tag("CoilBitmapLoader").w(result.throwable, "Failed to load $normalized")
                    placeholderBitmap()
                }
                is SuccessResult -> {
                    try {
                        val bitmap = result.image.toBitmap()
                        bitmap.copyIfNeeded()
                    } catch (e: Exception) {
                        Timber.tag("CoilBitmapLoader").w(e, "Failed to convert image to bitmap")
                        createFallbackBitmap()
                    }
                }
            }
        }
}
