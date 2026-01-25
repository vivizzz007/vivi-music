package com.music.vivi.lyrics

import android.content.Context
import com.music.innertube.YouTube
import com.music.vivi.constants.EnableYouTubeSubtitleKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object YouTubeSubtitleLyricsProvider : LyricsProvider {
    override val name = "YouTube Subtitle"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableYouTubeSubtitleKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        YouTube.transcript(id)
}
