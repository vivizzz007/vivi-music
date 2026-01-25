package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.constants.EnableSimpMusicKey
import com.music.vivi.lyrics.simpmusic.SimpMusicLyrics
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object SimpMusicLyricsProvider : LyricsProvider {
    override val name = "SimpMusic"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        SimpMusicLyrics.getLyrics(id, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        SimpMusicLyrics.getAllLyrics(id, duration, callback)
    }
}
