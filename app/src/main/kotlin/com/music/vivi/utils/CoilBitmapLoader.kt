package com.music.vivi.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

/**
 * A [BitmapLoader] implementation that uses Coil to load images.
 *
 * This adapter allows Media3/ExoPlayer to leverage Coil's caching and image loading capabilities
 * for fetching artwork to display in the system notification and lockscreen.
 *
 * @param context Application context.
 * @param scope Coroutine scope for async loading.
 */
class CoilBitmapLoader(private val context: Context, private val scope: CoroutineScope) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = scope.future(Dispatchers.IO) {
        BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: error("Could not decode image data")
    }

    /**
     * Loads a bitmap from the given [Uri].
     *
     * This method:
     * 1.  Creates a Coil [ImageRequest].
     * 2.  Disables hardware bitmaps (required for remote views/notifications).
     * 3.  Executes the request synchronously (blocking the IO dispatcher).
     * 4.  Returns a [ListenableFuture] compatible with Media3's async API.
     *
     * @param uri The URI of the image to load.
     * @return A Future containing the loaded Bitmap or a placeholder on error.
     */
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)

        // In case of error, returns an empty bitmap
        when (result) {
            is ErrorResult -> {
                createBitmap(64, 64)
            }
            is SuccessResult -> {
                try {
                    result.image.toBitmap()
                } catch (e: Exception) {
                    createBitmap(64, 64)
                }
            }
        }
    }
}
