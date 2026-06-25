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
 * - **YouTube (`i.ytimg.com`)**: reconstructs the URL at a safe quality level
 *   (`hqdefault.jpg` or `mqdefault.jpg`). Higher qualities like `sddefault` and
 *   `maxresdefault` are intentionally avoided — they don't exist for many
 *   YouTube Music tracks and would cause 404 → blank image. Any remaining 404s
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

/**
 * Reconstructs an `i.ytimg.com` thumbnail URL using the **WebP** format for
 * faster loading (~30% smaller than JPEG at the same visual quality).
 *
 * YouTube serves WebP thumbnails via the `vi_webp/` path. If WebP isn't
 *
 * Quality tiers:
 * - width ≥ 800 → `maxresdefault.webp` (1280×720) — player
 * - width ≥ 320 → `hqdefault.webp`    (480×360) — list items
 * - smaller     → `mqdefault.webp`    (320×180)
 */
private fun String.resizeYtimg(width: Int?, height: Int?): String {
    val w = width ?: height!!

    val videoId = Regex("/vi(?:_webp)?/([^/]+)/").find(this)?.groupValues?.get(1)
        ?: run {
            Timber.w("[Thumbnail] Could not extract video ID from ytimg URL: %s", this)
            return this
        }

    // Identify the original quality tier from the current URL to prevent upscaling beyond it.
    // The original URL contains the maximum quality available on YouTube's servers.
    val originalTier = when {
        this.contains("maxresdefault") -> "maxresdefault"
        this.contains("sddefault")     -> "sddefault"
        this.contains("hqdefault")     -> "hqdefault"
        this.contains("mqdefault")     -> "mqdefault"
        else                           -> "default"
    }

    val tierRanks = mapOf(
        "maxresdefault" to 4,
        "sddefault"     to 3,
        "hqdefault"     to 2,
        "mqdefault"     to 1,
        "default"       to 0
    )

    // Determine target quality tier based on requested width
    val requestedTier = when {
        w >= 800 -> "maxresdefault"
        w >= 640 -> "sddefault"
        w >= 320 -> "hqdefault"
        w >= 120 -> "mqdefault"
        else     -> "default"
    }

    val originalRank = tierRanks[originalTier] ?: 0
    val requestedRank = tierRanks[requestedTier] ?: 0
    val targetRank = minOf(originalRank, requestedRank)

    val targetTier = when (targetRank) {
        4 -> "maxresdefault"
        3 -> "sddefault"
        2 -> "hqdefault"
        1 -> "mqdefault"
        else -> "default"
    }

    // For default quality, use JPEG. For higher qualities, use WebP for faster loading.
    return if (targetTier == "default") {
        "https://i.ytimg.com/vi/$videoId/default.jpg"
    } else {
        "https://i.ytimg.com/vi_webp/$videoId/$targetTier.webp"
    }
}
