package com.music.vivi.desktop

import com.music.vivi.applecanvas.AppleMusicCanvasProvider
import com.music.vivi.canvas.CanvasArtwork
import com.music.vivi.canvas.TidalCanvasProvider
import com.music.vivi.vivimusiccanvas.ViviMusicCanvasProvider

/** Canvas artwork source (mirrors the Android app's `CanvasSource` enum). */
enum class CanvasSource(val key: String) {
    AUTO("AUTO"),
    APPLE_MUSIC("APPLE_MUSIC"),
    VIVIMUSIC("VIVIMUSIC"),
    TIDAL("TIDAL");

    companion object {
        fun from(key: String?): CanvasSource =
            entries.firstOrNull { it.key == key || it.name == key } ?: AUTO
    }
}

/**
 * Resolves animated canvas artwork for the desktop Player, reusing the same
 * JVM-pure providers as the Android app (Apple Music, Tidal, VIVI Music
 * canvas). Providers return video (MP4/HLS) or animated images (GIF/WebP); the
 * desktop can only animate images (via Coil), so video canvases fall back to
 * their static art or the track thumbnail.
 */
object CanvasResolver {

    suspend fun resolve(
        title: String,
        artist: String,
        album: String? = null,
        source: CanvasSource = CanvasSource.AUTO,
    ): CanvasArtwork? {
        if (title.isBlank() || artist.isBlank()) return null

        suspend fun tryApple() = AppleMusicCanvasProvider.getBySongArtist(title, artist, album)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }

        suspend fun tryTidal() = TidalCanvasProvider.getBySongArtist(title, artist, album)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }

        suspend fun tryVivi() = ViviMusicCanvasProvider.getBySongArtist(title, artist, album.orEmpty())
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }

        // A specific source only queries that provider; AUTO tries them in the
        // same priority order as the Android app.
        return when (source) {
            CanvasSource.APPLE_MUSIC -> tryApple()
            CanvasSource.TIDAL -> tryTidal()
            CanvasSource.VIVIMUSIC -> tryVivi()
            CanvasSource.AUTO -> tryApple() ?: tryTidal() ?: tryVivi()
        }
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
