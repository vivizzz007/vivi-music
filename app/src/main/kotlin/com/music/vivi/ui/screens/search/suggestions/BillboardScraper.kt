/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search.suggestions

import android.util.Log
import org.jsoup.Jsoup
import java.lang.Exception

object BillboardScraper {
    private const val TAG = "BillboardScraper"
    private const val ARTIST_100_URL = "https://www.billboard.com/charts/artist-100/"

    fun fetchHot100(regionSlug: String = "system"): List<BillboardTrack> {
        val tracks = mutableListOf<BillboardTrack>()
        try {
            val resolvedSlug = if (regionSlug == "system") {
                resolveSystemRegion()
            } else {
                regionSlug
            }
            val url = "https://www.billboard.com/charts/$resolvedSlug/"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .get()

            val rows = doc.select("ul.o-chart-results-list-row")
            rows.take(30).forEachIndexed { index, row ->
                try {
                    val rankText = row.select("li.o-chart-results-list__item > span.c-label").first()?.text()?.trim()
                    val rank = rankText?.toIntOrNull() ?: (index + 1)
                    val title = row.select("h3#title-of-a-story").first()?.text()?.trim() ?: "Unknown Title"
                    val artist = row.select("h3#title-of-a-story + span.c-label").first()?.text()?.trim() ?: 
                                 row.select("span.c-label.a-no-trucate").first()?.text()?.trim() ?: "Unknown Artist"
                    val imgElement = row.select("img.c-lazy-image__img").first()
                    val thumbnail = imgElement?.attr("data-lazy-src")?.takeIf { it.isNotEmpty() } ?: 
                                    imgElement?.attr("src")?.takeIf { it.isNotEmpty() }

                    val labels = row.select("li.o-chart-results-list__item > span.c-label")
                    // The first label is rank, so we take subsequent ones for stats if they are in the list
                    // Actually, Billboard rows have many labels. Let's be more specific or just try to find them.
                    val lastWeek = labels.getOrNull(1)?.text()?.trim()?.toIntOrNull()
                    val peakPos = labels.getOrNull(2)?.text()?.trim()?.toIntOrNull()
                    val weeksOnChart = labels.getOrNull(3)?.text()?.trim()?.toIntOrNull()

                    // Generate a mock trend based on rank
                    val seed = (title.hashCode() + artist.hashCode()).toLong()
                    val random = java.util.Random(seed)
                    val trend = List(7) { 
                        val base = (101 - rank).toFloat() / 100f
                        (base + (random.nextFloat() * 0.2f - 0.1f)).coerceIn(0f, 1f)
                    }

                    tracks.add(BillboardTrack(rank, title, artist, thumbnail, lastWeek, peakPos, weeksOnChart, trend))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Hot 100 row $index", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Hot 100", e)
        }
        return tracks
    }

    fun fetchArtist100(): List<BillboardArtist> {
        val artists = mutableListOf<BillboardArtist>()
        try {
            val doc = Jsoup.connect(ARTIST_100_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .get()

            var rows = doc.select("ul.o-chart-results-list-row")
            if (rows.isEmpty()) rows = doc.select("ul.o-chart-results-list-row-container")
            
            rows.take(30).forEachIndexed { index, row ->
                try {
                    val rankText = row.select("span.c-label.a-font-primary-bold-l").first()?.text()?.trim() ?:
                                   row.select("li.o-chart-results-list__item > span.c-label").first()?.text()?.trim()
                    val rank = rankText?.toIntOrNull() ?: (index + 1)
                    val name = row.select("h3#title-of-a-story").first()?.text()?.trim() ?:
                               row.select("h3.c-title").first()?.text()?.trim() ?: "Unknown Artist"
                    val imgElement = row.select("img.c-lazy-image__img").first()
                    val thumbnail = imgElement?.attr("data-lazy-src")?.takeIf { it.isNotEmpty() } ?: 
                                    imgElement?.attr("src")?.takeIf { it.isNotEmpty() }

                    artists.add(BillboardArtist(rank, name, thumbnail))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing artist row $index", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Artist 100", e)
        }
        return artists
    }

    private fun resolveSystemRegion(): String {
        val countryCode = java.util.Locale.getDefault().country
        return when (countryCode.uppercase()) {
            "IN" -> "india-songs-hotw"
            "US" -> "hot-100"
            "GB" -> "u-k-songs-hotw"
            "CA" -> "canadian-hot-100"
            "JP" -> "japan-hot-100"
            "KR" -> "south-korea-songs-hotw"
            "AU" -> "australia-songs-hotw"
            "BR" -> "brazil-songs-hotw"
            "DE" -> "germany-songs-hotw"
            "FR" -> "france-songs-hotw"
            "IT" -> "italy-songs-hotw"
            "ES" -> "spain-songs-hotw"
            "MX" -> "mexico-songs-hotw"
            else -> "hot-100" // Default to Hot 100 if no specific regional chart
        }
    }
}
