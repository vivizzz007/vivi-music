package com.music.vivi.lyrics

import android.content.Context
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.vivi.constants.EnableYouTubeLyricsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object YouTubeLyricsProvider : LyricsProvider {
    override val name = "YouTube Music"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableYouTubeLyricsKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        runCatching {
            val nextResult = YouTube.next(WatchEndpoint(videoId = id)).getOrThrow()
            YouTube
                .lyrics(
                    endpoint = nextResult.lyricsEndpoint
                        ?: throw IllegalStateException("Lyrics endpoint not found")
                ).getOrThrow() ?: throw IllegalStateException("Lyrics unavailable")
        }
}
