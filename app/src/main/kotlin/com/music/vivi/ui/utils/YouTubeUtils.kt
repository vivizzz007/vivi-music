/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import timber.log.Timber

/**
 * Resizes a Google CDN or YouTube thumbnail URL to the requested dimensions.
 *
 * - **Google CDN** (`googleusercontent.com`, `ggpht.com`): rewrites the
 *   dimension query parameters to request the exact size from the CDN.
 * - **YouTube (`i.ytimg.com`)**: scales the thumbnail quality suffix based on requested size.
 * - **Everything else**: returned unchanged.
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    val isGoogleCdn = this.contains("googleusercontent.com") || this.contains("ggpht.com")
    val isYtimg = this.contains("i.ytimg.com")

    return when {
        isGoogleCdn -> resizeGoogleCdn(width, height)
        isYtimg -> resizeYtimg(width, height)
        else -> this
    }
}

/**
 * Rewrites a Google CDN URL to request the given dimensions.
 * Handles both `wNNN-hNNN` path-segment style and `=w`/`=s`/`=h` query-param style.
 */
private fun String.resizeGoogleCdn(width: Int?, height: Int?): String {
    val w = width ?: height!!
    val h = height ?: width!!

    if (this.contains(Regex("w\\d+-h\\d+"))) {
        return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
    }

    val baseUrl = this.split("=w", "=s", "=h", limit = 2)[0]
    return if ((this.contains("=w") && this.contains("-h")) || (width != null && height != null)) {
        "$baseUrl=w$w-h$h-p-l90-rj"
    } else {
        "$baseUrl=s$w-p-l90-rj"
    }
}

private fun String.resizeYtimg(width: Int?, height: Int?): String {
    val w = width ?: height!!
    val videoId = Regex("/vi(?:_webp)?/([^/]+)/").find(this)?.groupValues?.get(1) ?: return this

    return when {
        w >= 800 -> {
            // Player artwork: Request HD maxresdefault if it exists, otherwise fall back to hqdefault
            if (this.contains("maxresdefault") || this.contains("sddefault")) {
                "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
            } else {
                "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            }
        }
        w >= 320 -> {
            // List items (like 544px): hqdefault is guaranteed to exist and is sharp
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }
        else -> {
            // Small thumbnails: mqdefault is guaranteed to exist and loads fast
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
        }
    }
}
