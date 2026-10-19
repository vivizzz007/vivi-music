/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import com.music.vivi.constants.DataSaverKey
import timber.log.Timber

/**
 * Global flag indicating whether Data Saver is active.
 * Updated reactively from DataStore in [App.kt].
 */
@Volatile
var isDataSaverEnabled: Boolean = false

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
                      this.contains(Regex("=[wshd]\\d+"))

    // Detect YouTube video thumbnails (containing i.ytimg.com, img.youtube.com, or path segment /vi/)
    val isYtimg = this.contains("ytimg") || this.contains("youtube.com") || this.contains("/vi/")

    return when {
        isGoogleCdn -> resizeGoogleCdn(width, height, isDataSaverEnabled)
        isYtimg -> resizeYtimg(width, height, isDataSaverEnabled)
        else -> this
    }
}

/**
 * Rewrites a Google CDN URL to request high resolution dimensions.
 * Uses generic regex to locate parameters, ensuring robustness.
 */
private fun String.resizeGoogleCdn(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    val w = if (isDataSaverEnabled) {
        (width ?: height ?: 150).coerceAtMost(150)
    } else {
        // Honour the caller's requested size; fall back to 544 only when no size given
        width ?: height ?: 544
    }
    val h = if (isDataSaverEnabled) {
        (height ?: width ?: 150).coerceAtMost(150)
    } else {
        height ?: width ?: 544
    }

    // Handle wNNN-hNNN path segment style parameters
    if (this.contains(Regex("w\\d+-h\\d+"))) {
        return this.replace(Regex("w\\d+-h\\d+"), "w$w-h$h")
    }

    // Safely extract base URL by stripping any parameters like =w120, =s120, =h120, etc.
    val baseUrl = this.split(Regex("=[wshd]"), limit = 2)[0]
    return "$baseUrl=w$w-h$h-p-l90-rj"
}

/**
 * Rewrites a YouTube thumbnail URL to request the highest **reliably available** quality tier.
 *
 * Quality ladder (highest → lowest, all guaranteed to exist for every video):
 *   w >= 800  → hqdefault.jpg  (480×360,  ALWAYS available — primary player quality)
 *   w >= 320  → mqdefault.jpg  (320×180,  list items)
 *   w <  320  → default.jpg    (120×90,   small icons / data-saver fallback)
 *
 * NOTE: WebP variants (vi_webp/hqdefault.webp etc.) are NOT available for all YouTube videos —
 * older uploads and some music videos return 404 for all vi_webp paths.
 * JPEG variants (vi/hqdefault.jpg etc.) are always available and are used here to prevent
 * blank thumbnails for affected videos (e.g. https://music.youtube.com/watch?v=JNJJ-QkZ8cM).
 */
private fun String.resizeYtimg(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    // Extract video ID using regex that matches any standard YouTube thumbnail path
    val videoId = Regex("/vi(?:_webp)?/([^/]+)/").find(this)?.groupValues?.get(1)
    if (videoId == null) {
        Timber.w("[Thumbnail] resizeYtimg: could not extract videoId from: $this")
        return this
    }

    if (isDataSaverEnabled) {
        val w = width ?: height ?: 150
        val result = when {
            w >= 800 -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
            else     -> "https://i.ytimg.com/vi/$videoId/default.jpg"
        }
        Timber.d("[Thumbnail] resizeYtimg (dataSaver): id=$videoId w=$w → $result")
        return result
    }

    val w = width ?: height ?: 1200
    val result = when {
        w >= 800  -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        w >= 320  -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
        else      -> "https://i.ytimg.com/vi/$videoId/default.jpg"
    }
    Timber.d("[Thumbnail] resizeYtimg: id=$videoId w=$w → $result")
    return result
}


