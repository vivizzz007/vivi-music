/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

/**
 * Resizes a Google CDN or YouTube thumbnail URL to the requested dimensions.
 * Uses domain-independent and parameter-based matching to maximize quality
 * and ensure fetching logic does not break if YouTube changes hostnames or paths.
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
    isDataSaverEnabled: Boolean = false,
): String {
    val isGoogleCdn = this.contains("googleusercontent.com") || 
                      this.contains("ggpht.com") || 
                      this.contains(Regex("=[wshd]\\d+"))

    val isYtimg = this.contains("ytimg") || this.contains("youtube.com") || this.contains("/vi/")

    return when {
        isGoogleCdn -> resizeGoogleCdn(width, height, isDataSaverEnabled)
        isYtimg -> resizeYtimg(width, height, isDataSaverEnabled)
        else -> this
    }
}

private fun String.resizeGoogleCdn(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    val w = if (isDataSaverEnabled) {
        (width ?: height ?: 150).coerceAtMost(150)
    } else {
        (width ?: height ?: 1200).coerceAtLeast(544)
    }
    val h = if (isDataSaverEnabled) {
        (height ?: width ?: 150).coerceAtMost(150)
    } else {
        (height ?: width ?: 1200).coerceAtLeast(544)
    }

    if (this.contains(Regex("w\\d+-h\\d+"))) {
        return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
    }

    val baseUrl = this.split(Regex("=[wshd]"), limit = 2)[0]
    return "$baseUrl=w$w-h$h-p-l90-rj"
}

private fun String.resizeYtimg(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    val videoId = Regex("/vi(?:_webp)?/([^/]+)/").find(this)?.groupValues?.get(1) ?: return this

    if (isDataSaverEnabled) {
        val w = width ?: height ?: 150
        return when {
            w >= 800 -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
            else -> "https://i.ytimg.com/vi/$videoId/default.jpg"
        }
    }

    val w = width ?: height ?: 1200

    return when {
        w >= 800 -> "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        w >= 320 -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        else -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
    }
}
