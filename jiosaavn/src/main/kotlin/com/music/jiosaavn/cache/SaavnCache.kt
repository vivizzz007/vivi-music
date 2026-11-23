/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.jiosaavn.cache

import android.util.LruCache
import com.music.jiosaavn.models.SaavnSong

object SaavnCache {

    // Cache search queries -> list of songs (capacity: 100 queries)
    private val searchCache = LruCache<String, List<SaavnSong>>(100)

    // Cache songId:quality -> direct stream URL (capacity: 200 items)
    private val streamUrlCache = LruCache<String, String>(200)

    // Cache trackIdentifier (e.g. videoId) -> matched SaavnSong (capacity: 200 items)
    private val songMatchCache = LruCache<String, SaavnSong>(200)

    fun getSearchResults(query: String): List<SaavnSong>? {
        return searchCache.get(query.trim().lowercase())
    }

    fun putSearchResults(query: String, songs: List<SaavnSong>) {
        if (songs.isNotEmpty()) {
            searchCache.put(query.trim().lowercase(), songs)
        }
    }

    fun getStreamUrl(songId: String, quality: String): String? {
        return streamUrlCache.get("$songId:$quality")
    }

    fun putStreamUrl(songId: String, quality: String, url: String) {
        if (url.isNotBlank()) {
            streamUrlCache.put("$songId:$quality", url)
        }
    }

    fun getMatchedSong(identifier: String): SaavnSong? {
        return songMatchCache.get(identifier)
    }

    fun putMatchedSong(identifier: String, song: SaavnSong) {
        songMatchCache.put(identifier, song)
    }

    fun clear() {
        searchCache.evictAll()
        streamUrlCache.evictAll()
        songMatchCache.evictAll()
    }
}

