/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

import com.music.vivi.constants.DataSaverKey
import com.music.vivi.utils.ViviPrefCache
import timber.log.Timber

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

    val isDataSaverEnabled = ViviPrefCache.get(DataSaverKey) == true

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
        (width ?: height ?: 1200).coerceAtLeast(544)
    }
    val h = if (isDataSaverEnabled) {
        (height ?: width ?: 150).coerceAtMost(150)
    } else {
        (height ?: width ?: 1200).coerceAtLeast(544)
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
 * Rewrites a YouTube thumbnail URL to request the highest quality tier.
 * Supports fallback to hqdefault/mqdefault for smaller requested widths.
 */
private fun String.resizeYtimg(width: Int?, height: Int?, isDataSaverEnabled: Boolean): String {
    // Extract video ID using regex that matches any standard YouTube thumbnail path
    val videoId = Regex("/vi(?:_webp)?/([^/]+)/").find(this)?.groupValues?.get(1) ?: return this

    if (isDataSaverEnabled) {
        val w = width ?: height ?: 150
        return when {
            w >= 800 -> {
                // If data saver is enabled, don't use maxresdefault, use mqdefault (medium quality)
                "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
            }
            else -> {
                // For small list thumbnails, use default.jpg (120x90)
                "https://i.ytimg.com/vi/$videoId/default.jpg"
            }
        }
    }

    val w = width ?: height ?: 1200

    return when {
        w >= 800 -> {
            // For player artwork (high resolution), we always request maxresdefault.jpg
            // If the video does not support Full HD maxresdefault.jpg, Coil's client-side
            // fallback (onError) automatically falls back to hqdefault.jpg, ensuring no breakage.
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        }
        w >= 320 -> {
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }
        else -> {
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
        }
    }
}


