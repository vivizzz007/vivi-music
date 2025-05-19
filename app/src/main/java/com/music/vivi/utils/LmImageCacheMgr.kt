package com.music.vivi.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.music.vivi.models.ImageCacheManager
import androidx.core.graphics.scale

class LmImageCacheMgr {

    private var localImageCache = ImageCacheManager(300)
    fun getLocalThumbnail(path: String?): Bitmap? = getLocalThumbnail(path, false)
    fun getLocalThumbnail(path: String?, resize: Boolean): Bitmap? {
        if (path == null) {
            return null
        }
        val mData = MediaMetadataRetriever()

        var image: Bitmap = try {
            mData.setDataSource(path)
            val art = mData.embeddedPicture
            BitmapFactory.decodeByteArray(art, 0, art!!.size)
        } catch (e: Exception) {
            localImageCache.cache(path, null, resize)
            null
        } ?: return null

        if (resize) {
            image = image.scale(100, 100, false)
        }

        localImageCache.cache(path, image, resize)
        return image
    }
}