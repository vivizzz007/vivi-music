package com.music.vivi.lyrics

import android.content.Context
import com.maloy.lrclib.LrcLib
import com.maloy.muzza.constants.EnableLrcLibKey
import com.maloy.muzza.utils.dataStore
import com.maloy.muzza.utils.get

/**
 * Source: https://github.com/Malopieds/Muzza
 */
object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = LrcLib.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        LrcLib.getAllLyrics(title, artist, duration, null, callback)
    }
}
