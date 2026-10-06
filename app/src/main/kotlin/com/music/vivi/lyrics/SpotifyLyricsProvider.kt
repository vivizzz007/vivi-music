package com.music.vivi.lyrics

import android.content.Context
import com.music.spotify.Spotify
import com.music.vivi.constants.EnableSpotifyLyricsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get

object SpotifyLyricsProvider : LyricsProvider {
    override val name = "Spotify"

    // Defaulting to false, or true? Usually third-party ones default to true if possible. 
    // Since this requires user login to work properly, we'll default it to false and let users turn it on.
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSpotifyLyricsKey] ?: false

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        // We first need a trackId. 
        // Spotify search will look for the exact match
        val query = "$title $artist"
        val trackId = Spotify.searchTrack(query)?.id 
            ?: throw IllegalStateException("Track not found on Spotify")
            
        val lyrics = Spotify.getColorLyrics(trackId) 
            ?: throw IllegalStateException("Lyrics not available on Spotify")
            
        if (lyrics.isBlank()) throw IllegalStateException("Empty lyrics")
        lyrics
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, duration, album).onSuccess(callback)
    }
}
