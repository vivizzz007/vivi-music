package com.music.vivi.desktop

import com.music.vivi.applecanvas.AppleMusicCanvasProvider
import com.music.vivi.canvas.CanvasArtwork
import com.music.vivi.canvas.TidalCanvasProvider
import com.music.vivi.vivimusiccanvas.ViviMusicCanvasProvider

/**
 * Resolves animated canvas artwork for the desktop Player, reusing the same
 * JVM-pure providers as the Android app (Apple Music, Tidal, VIVI Music
 * canvas). Providers return video (MP4/HLS) or animated images (GIF/WebP); the
 * desktop can only animate images (via Coil), so video canvases fall back to
 * their static art or the track thumbnail.
 */
object CanvasResolver {

    suspend fun resolve(title: String, artist: String, album: String? = null): CanvasArtwork? {
        if (title.isBlank() || artist.isBlank()) return null

        AppleMusicCanvasProvider.getBySongArtist(title, artist, album)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            ?.let { return it }

        TidalCanvasProvider.getBySongArtist(title, artist, album)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            ?.let { return it }

        ViviMusicCanvasProvider.getBySongArtist(title, artist, album.orEmpty())
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            ?.let { return it }

        return null
    }

    /** URL to render: an animated GIF/WebP if available, else static art, else [fallback]. */
    fun displayUrl(art: CanvasArtwork?, fallback: String?): String? {
        val animated = art?.preferredAnimationUrl
        return when {
            animated != null && (animated.endsWith(".gif", true) || animated.endsWith(".webp", true)) -> animated
            !art?.static.isNullOrBlank() -> art.static
            else -> fallback
        }
    }
}
