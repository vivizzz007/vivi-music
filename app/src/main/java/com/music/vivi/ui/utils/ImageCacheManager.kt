package com.music.vivi.ui.utils

import android.graphics.Bitmap

const val MAX_IMAGE_CACHE = 300

/**
 * Cached image
 */
data class CachedBitmap(var path: String?, var image: Bitmap?, var resizedImage: Bitmap?)

var bitmapCache = ArrayDeque<CachedBitmap>()

/**
 * Retrieves an image from the cache
 */
fun retrieveImage(path: String): CachedBitmap? {
    return bitmapCache.firstOrNull { it.path == path }
}

/**
 * Adds an image to the cache
 */
fun cache(path: String, image: Bitmap?, resize: Boolean) {
    if (image == null) {
        return
    }

    if (bitmapCache.size >= MAX_IMAGE_CACHE) {
        bitmapCache.removeFirst()
    }

    val existingCached = retrieveImage(path)
    if (existingCached == null) {
        if (resize) {
            bitmapCache.addLast(CachedBitmap(path, null, image))
        } else {
            bitmapCache.addLast(CachedBitmap(path, image, null))
        }
    } else {
        if (resize) {
            existingCached.resizedImage = image
        } else {
            existingCached.image = image
        }
    }
}