/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.vivi.betterlyrics.BetterLyrics
import com.music.vivi.constants.EnableBetterLyricsPortatoKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object BetterLyricsPortatoLyricsProvider : LyricsProvider {
    override val name = "BetterLyricsPortato"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsPortatoKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getPortatoLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllPortatoLyrics(title, artist, duration, album, callback)
    }
}

