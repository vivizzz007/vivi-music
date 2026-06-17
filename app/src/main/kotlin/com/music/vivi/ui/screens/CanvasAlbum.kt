/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.music.vivi.applecanvas.AppleMusicCanvasProvider
import com.music.vivi.canvas.CanvasArtwork
import com.music.vivi.canvas.TidalCanvasProvider
import com.music.vivi.ui.player.CanvasArtworkPlaybackCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun rememberAlbumCanvas(
    albumTitle: String?,
    artistName: String?,
    firstSongTitle: String? = null,
): CanvasArtwork? {
    val cacheKey = remember(albumTitle, artistName) {
        if (albumTitle != null && artistName != null) "album|$albumTitle|$artistName" else null
    }

    var canvasArtwork by remember(cacheKey) {
        mutableStateOf(cacheKey?.let { CanvasArtworkPlaybackCache.get(it) })
    }
    
    val storefront = remember {
        val country = Locale.getDefault().country
        if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }

    LaunchedEffect(albumTitle, artistName, firstSongTitle) {
        if (canvasArtwork != null || cacheKey == null) return@LaunchedEffect
        if (albumTitle.isNullOrBlank() || artistName.isNullOrBlank()) {
            canvasArtwork = null
            return@LaunchedEffect
        }

        val fetched = withContext(Dispatchers.IO) {
            // Search variants
            val searchTasks = listOf(
                albumTitle to artistName
            ).filter { (s, a) -> s.isNotBlank() && a.isNotBlank() }

            searchTasks.firstNotNullOfOrNull { (s, a) ->
                AppleMusicCanvasProvider.getByAlbumArtist(
                    album = s,
                    artist = a,
                    storefront = storefront
                )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                    ?: TidalCanvasProvider.getByAlbumArtist(
                        album = s,
                        artist = a
                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            }
        }

        // Artist and Album validation check (matches Thumbnail.kt logic)
        var validated = fetched?.let { artwork ->
            val resultArtist = artwork.artist
            val canvasAlbumName = artwork.albumName

            // Check artist name (all requested artists must match exactly, splits multi-artists)
            val artistMatches = if (resultArtist != null && artistName.isNotBlank()) {
                val requestedList = splitAndNormalizeArtists(artistName)
                val resultList = splitAndNormalizeArtists(resultArtist)
                requestedList.isNotEmpty() && resultList.isNotEmpty() &&
                requestedList.all { req -> resultList.any { res -> res == req } }
            } else true

            // Check album name (raw exact comparison)
            val albumMatches = if (canvasAlbumName != null && albumTitle.isNotBlank()) {
                canvasAlbumName.trim().equals(albumTitle.trim(), ignoreCase = true)
            } else false

            println("AlbumCanvasValidation: artistMatches=$artistMatches, albumMatches=$albumMatches")
            println("  Requested: Album='$albumTitle', Artists='$artistName'")
            println("  Returned: Album='$canvasAlbumName', Artists='$resultArtist'")

            if (artistMatches && albumMatches) {
                println("AlbumCanvasValidation: Match SUCCESS for '${artwork.name}'")
                artwork
            } else {
                println("AlbumCanvasValidation: Match FAILED for '${artwork.name}' by '${artwork.artist}'")
                null
            }
        }

        if (validated == null) {
            val tidalFetched = withContext(Dispatchers.IO) {
                TidalCanvasProvider.getByAlbumArtist(
                    album = albumTitle,
                    artist = artistName
                )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
            }
            validated = tidalFetched?.let { artwork ->
                val resultArtist = artwork.artist
                val canvasAlbumName = artwork.albumName

                // Check artist name (all requested artists must match exactly, splits multi-artists)
                val artistMatches = if (resultArtist != null && artistName.isNotBlank()) {
                    val requestedList = splitAndNormalizeArtists(artistName)
                    val resultList = splitAndNormalizeArtists(resultArtist)
                    requestedList.isNotEmpty() && resultList.isNotEmpty() &&
                    requestedList.all { req -> resultList.any { res -> res == req } }
                } else true

                // Check album name (raw exact comparison)
                val albumMatches = if (canvasAlbumName != null && albumTitle.isNotBlank()) {
                    canvasAlbumName.trim().equals(albumTitle.trim(), ignoreCase = true)
                } else false

                println("AlbumCanvasValidation (Tidal): artistMatches=$artistMatches, albumMatches=$albumMatches")
                println("  Requested: Album='$albumTitle', Artists='$artistName'")
                println("  Returned: Album='$canvasAlbumName', Artists='$resultArtist'")

                if (artistMatches && albumMatches) {
                    println("AlbumCanvasValidation (Tidal): Match SUCCESS for '${artwork.name}'")
                    artwork
                } else {
                    println("AlbumCanvasValidation (Tidal): Match FAILED for '${artwork.name}' by '${artwork.artist}'")
                    null
                }
            }
        }

        if (validated != null) {
            canvasArtwork = validated
            CanvasArtworkPlaybackCache.put(cacheKey, validated)
        }
    }

    return canvasArtwork
}
private fun splitAndNormalizeArtists(raw: String): List<String> {
    return raw.split(
        Regex(
            "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
            RegexOption.IGNORE_CASE,
        )
    ).map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
}
