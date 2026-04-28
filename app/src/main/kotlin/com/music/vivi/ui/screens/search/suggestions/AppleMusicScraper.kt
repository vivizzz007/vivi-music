/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

import android.util.Log
import org.json.JSONObject
import org.jsoup.Jsoup
import java.lang.Exception

object AppleMusicScraper {
    private const val TAG = "AppleMusicScraper"

    fun fetchTopSongs(countryCode: String = "us"): List<SuggestionTrack> {
        val tracks = mutableListOf<SuggestionTrack>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/$countryCode/music/most-played/100/songs.json"
            val response = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .execute()
                .body()
            
            val json = JSONObject(response)
            val results = json.getJSONObject("feed").getJSONArray("results")
            
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val rank = i + 1
                val title = item.getString("name")
                val artist = item.getString("artistName")
                val artwork = item.getString("artworkUrl100")
                    .replace(Regex("(\\d+)x(\\d+)"), "1400x1400")
                val appleUrl = item.getString("url")
                
                tracks.add(SuggestionTrack(rank, title, artist, artwork, appleUrl))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Apple Music Top Songs for $countryCode", e)
        }
        return tracks
    }

    fun fetchTopAlbums(countryCode: String = "us"): List<SuggestionAlbum> {
        val albums = mutableListOf<SuggestionAlbum>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/$countryCode/music/most-played/20/albums.json"
            val response = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .execute()
                .body()
            
            val json = JSONObject(response)
            val results = json.getJSONObject("feed").getJSONArray("results")
            
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val rank = i + 1
                val title = item.getString("name")
                val artist = item.getString("artistName")
                val artwork = item.getString("artworkUrl100")
                    .replace(Regex("(\\d+)x(\\d+)"), "1400x1400")
                val appleUrl = item.getString("url")
                
                albums.add(SuggestionAlbum(rank, title, artist, artwork, appleUrl))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Apple Music Top Albums for $countryCode", e)
        }
        return albums
    }

    fun fetchTopVideos(countryCode: String = "us"): List<SuggestionTrack> {
        val videos = mutableListOf<SuggestionTrack>()
        try {
            val url = "https://rss.applemarketingtools.com/api/v2/$countryCode/music/most-played/20/music-videos.json"
            val response = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .execute()
                .body()
            
            val json = JSONObject(response)
            val results = json.getJSONObject("feed").getJSONArray("results")
            
            val ids = mutableListOf<String>()
            val videoMap = mutableMapOf<String, SuggestionTrack>()

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val id = item.getString("id")
                val rank = i + 1
                val title = item.getString("name")
                val artist = item.getString("artistName")
                val artwork = item.getString("artworkUrl100")
                    .replace(Regex("(\\d+)x(\\d+)"), "1920x1080")
                val appleUrl = item.getString("url")
                
                ids.add(id)
                videoMap[id] = SuggestionTrack(rank, title, artist, artwork, appleUrl)
            }

            // Batch lookup for preview URLs removed

            videos.addAll(videoMap.values.sortedBy { it.rank })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Apple Music Top Videos for $countryCode", e)
        }
        return videos
    }

    fun getTrendingArtists(tracks: List<SuggestionTrack>): List<SuggestionArtist> {
        // Extract top unique artists from the tracks list
        val artistCounts = mutableMapOf<String, Int>()
        val artistImages = mutableMapOf<String, String?>()
        
        tracks.forEach { track ->
            // Handle multiple artists in string
            val mainArtist = track.artist.split(",", "&", "feat.", "ft.").first().trim()
            artistCounts[mainArtist] = (artistCounts[mainArtist] ?: 0) + 1
            if (artistImages[mainArtist] == null) {
                // Use a smaller but still high quality version for artist circles
                artistImages[mainArtist] = track.thumbnailUrl?.replace("1920x1080", "500x500")
            }
        }
        
        return artistCounts.toList()
            .sortedByDescending { it.second }
            .take(15)
            .mapIndexed { index, pair ->
                SuggestionArtist(index + 1, pair.first, artistImages[pair.first])
            }
    }
}
