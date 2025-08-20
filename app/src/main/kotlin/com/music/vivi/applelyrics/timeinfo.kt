package com.music.vivi.applelyrics

import com.music.vivi.db.entities.Song

object TimeUtils {

    /**
     * Calculates total duration from a list of songs and returns formatted string
     * Works with any type that has a duration property (Int or Long)
     */
    fun <T> calculateTotalDuration(
        songs: List<T>,
        durationExtractor: (T) -> Number, // Accepts both Int and Long
        countText: String = "songs" // Customizable count text
    ): String {
        if (songs.isEmpty()) return "0 $countText • 0m 0s"

        val totalDurationMs = songs.sumOf { durationExtractor(it).toLong() }
        val count = songs.size
        return "$count ${if (count == 1) countText.dropLast(1) else countText} • ${formatDuration(totalDurationMs)}"
    }

    /**
     * Formats milliseconds to human-readable time (h m s)
     */
    fun formatDuration(milliseconds: Long): String {
        if (milliseconds <= 0) return "0m 0s"

        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m ${seconds}s"
        } else {
            "${minutes}m ${seconds}s"
        }
    }
}