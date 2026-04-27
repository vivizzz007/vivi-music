/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    
    // googleusercontent.com handling (includes lh3-lh6, yt3, etc.)
    if (this.contains("googleusercontent.com") && this.contains("=w")) {
        val baseUrl = this.split("=w")[0]
        val w = width ?: 0
        val h = height ?: width ?: 0
        // Reverting to l90-rj (JPEG) for better compatibility while keeping high resolution
        return "$baseUrl=w$w-h$h-p-l90-rj"
    }

    // yt3.ggpht.com handling (avatars)
    if (this.contains("yt3.ggpht.com")) {
        // Correctly strip any existing size parameter (=s... or -s...) before appending the new one
        val baseUrl = this.split("=")[0].split("-s")[0]
        return "$baseUrl=s${width ?: height}"
    }

    // Fallback for other lh3-style URLs that might not have =w yet
    "https://lh\\d\\.googleusercontent\\.com/.*".toRegex().matchEntire(this)?.let {
        val w = width ?: 0
        val h = height ?: width ?: 0
        return "${this.split("=")[0]}=w$w-h$h-p-l90-rj"
    }

    return this
}
