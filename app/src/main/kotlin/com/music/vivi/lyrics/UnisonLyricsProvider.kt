/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.lyrics

import android.content.Context
import com.music.unison.Unison
import com.music.vivi.constants.EnableUnisonKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object UnisonLyricsProvider : LyricsProvider {
    override val name = "Unison"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableUnisonKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = Unison.getLyrics(
        title = title,
        artist = artist,
        duration = duration,
        album = album,
        videoId = id.takeIf { it.isNotBlank() },
    )

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        Unison.getAllLyrics(
            title = title,
            artist = artist,
            duration = duration,
            album = album,
            videoId = id.takeIf { it.isNotBlank() },
            callback = callback,
        )
    }
}
