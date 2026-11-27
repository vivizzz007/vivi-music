/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import com.music.vivi.constants.DataSaverKey

/**
 * Global flag indicating whether Data Saver is active.
 * Updated reactively from DataStore in [App.kt].
 */
@Volatile
var isDataSaverEnabled: Boolean = false

private val GoogleParamRegex = Regex("=[wshd]\\d+")
private val GooglePathDimRegex = Regex("w\\d+-h\\d+")
private val GoogleBaseSplitRegex = Regex("=[wshd]")
private val YtVideoIdRegex = Regex("/vi(?:_webp)?/([^/]+)/")

/**
 * Resizes a Google CDN or YouTube thumbnail URL to the requested dimensions.
 * Uses domain-independent and parameter-based matching to maximize quality
 * and ensure fetching logic does not break if YouTube changes hostnames or paths.
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    // Detect Google CDN URLs (including googleusercontent.com, ggpht.com, or any domain using Google parameter format like '=w120')
    val isGoogleCdn = this.contains("googleusercontent.com") || 
                      this.contains("ggpht.com") || 
                      GoogleParamRegex.containsMatchIn(this)

    // Detect YouTube video thumbnails (containing i.ytimg.com, img.youtube.com, or path segment /vi/)
    val isYtimg = this.contains("ytimg") || this.contains("youtube.com") || this.contains("/vi/")

    return when {
        isGoogleCdn -> resizeGoogleCdn(width, height, isDataSaverEnabled)
        isYtimg -> resizeYtimg(width, height, isDataSaverEnabled)
        else -> this
    }
}

/**
 * Returns true if the thumbnail URL points to a 4:3 letterboxed YouTube image
 * (hqdefault, sddefault, or default.jpg), which requires 4/3 zoom compensation
 * to eliminate black letterbox bars when cropped to a square container.
 * True 16:9 images (maxresdefault, mqdefault) return false and do not need extra zoom.
 */
fun isLetterboxedYtThumbnail(url: String?): Boolean {
    if (url == null) return false
    val isYt = url.contains("ytimg") || url.contains("/vi/") || url.contains("youtube.com")
    return isYt && (url.contains("hqdefault") || url.contains("sddefault") || url.contains("/default.jpg"))
}

/**
 * Rewrites a Google CDN URL to request high resolution dimensions.
 * Uses generic regex to locate parameters, ensuring robustness.
 */
private fun String.resizeGoogleCdn(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    val w = if (isDataSaverEnabled) {
        (width ?: height ?: 150).coerceAtMost(150)
    } else {
        // Enforce 240px minimum on modern high-DPI screens so 48dp list icons are never blurry
        (width ?: height ?: 544).coerceAtLeast(240)
    }
    val h = if (isDataSaverEnabled) {
        (height ?: width ?: 150).coerceAtMost(150)
    } else {
        (height ?: width ?: 544).coerceAtLeast(240)
    }

    // Handle wNNN-hNNN path segment style parameters
    if (GooglePathDimRegex.containsMatchIn(this)) {
        return GooglePathDimRegex.replace(this, "w$w-h$h")
    }

    // Safely extract base URL by stripping any parameters like =w120, =s120, =h120, etc.
    val baseUrl = this.split(GoogleBaseSplitRegex, limit = 2)[0]
    return "$baseUrl=w$w-h$h-p-l90-rj"
}

/**
 * Rewrites a YouTube thumbnail URL to request the highest quality tier.
 *
 * Quality ladder:
 *   w >= 800  → maxresdefault.jpg (1280×720 HD, native 16:9, crystal clear for player/artwork)
 *   w >= 250  → hqdefault.jpg    (480×360, medium for cards/grids/miniplayer)
 *   w <  250  → mqdefault.jpg    (320×180, clean 16:9, sharp for list items/queue)
 *
 * Data Saver:
 *   w >= 800  → mqdefault.jpg    (320×180, ~11 KB — 85% data saved)
 *   w <  800  → default.jpg      (120×90,  ~3 KB)
 */
private fun String.resizeYtimg(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    // Extract video ID using regex that matches any standard YouTube thumbnail path
    val videoId = YtVideoIdRegex.find(this)?.groupValues?.get(1) ?: return this

    if (isDataSaverEnabled) {
        val w = width ?: height ?: 150
        return when {
            w >= 800 -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
            else     -> "https://i.ytimg.com/vi/$videoId/default.jpg"
        }
    }

    val w = width ?: height ?: 1200
    return when {
        w >= 800  -> "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        w >= 250  -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        else      -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
    }
}


