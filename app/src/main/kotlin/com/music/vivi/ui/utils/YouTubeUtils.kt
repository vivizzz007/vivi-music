/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

// Precompiled once — avoids recompilation on every resize() call for ytimg URLs
private val YTIMG_QUALITY_REGEX = Regex("/(hqdefault|mqdefault|sddefault|hq720|default)\\.jpg")

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    // Handle Google Content CDN (lh3.googleusercontent.com, ggpht.com)
    if (this.contains("googleusercontent.com") || this.contains("ggpht.com")) {
        val baseUrl = this.substringBefore("=")
        val w = width ?: height  // Fix: was `width` alone — would produce =wnull if only height passed
        val h = height ?: width
        // -l100: max quality, -rw: WebP format
        return "$baseUrl=w$w-h$h-l100-rw"
    }

    // Handle YouTube video thumbnail CDN (i.ytimg.com, img.youtube.com)
    if (this.contains("i.ytimg.com/vi/") || this.contains("img.youtube.com/vi/")) {
        if (this.contains("/maxresdefault")) return this
        return this.replace(YTIMG_QUALITY_REGEX, "/maxresdefault.jpg")
    }

    return this
}


