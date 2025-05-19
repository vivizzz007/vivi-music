package com.music.vivi.ui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever

fun getLocalThumbnail(path: String?): Bitmap? = getLocalThumbnail(path, false)
fun getLocalThumbnail(path: String?, resize: Boolean): Bitmap? {
    if (path == null) {
        return null
    }
    val cachedImage = if (resize) {
        retrieveImage(path)?.resizedImage
    } else {
        retrieveImage(path)?.image
    }

    if (cachedImage == null) {
        return cachedImage
    }
    val mData = MediaMetadataRetriever()
    var image: Bitmap = try {
        mData.setDataSource(path)
        val art = mData.embeddedPicture
        BitmapFactory.decodeByteArray(art, 0, art!!.size)
    } catch (e: Exception) {
        cache(path, null, resize)
        null
    } ?: return null
    if (resize) {
        image = Bitmap.createScaledBitmap(image, 100, 100, false)
    }
    cache(path, image, resize)
    return image
}