/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.musixmatch

import com.music.musixmatch.models.RichSyncEntry
import java.util.Locale

object Musixmatch {
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null
    ): Result<String> = Result.failure(
        IllegalStateException("Musixmatch provider is not enabled in this build. Please configure MUSIXMATCH_CODE secret in GitHub Actions.")
    )

    private fun formatTime(timeMs: Long, isSyllable: Boolean): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return String.format(Locale.US, "%s%02d:%02d.%03d%s", prefix, minutes, seconds, millis, suffix)
    }

    internal fun convertRichSyncToLrc(entries: List<RichSyncEntry>): String {
        val sb = StringBuilder()
        for (entry in entries) {
            val lineText = entry.x.trim()
            if (lineText.isEmpty()) continue

            // Line header timestamp
            val lineTimeMs = (entry.ts * 1000).toLong()
            sb.append(formatTime(lineTimeMs, isSyllable = false))

            // Build inline syllable timings
            for (word in entry.l) {
                if (word.c.isBlank()) {
                    sb.append(word.c)
                } else {
                    val wordTimeMs = ((entry.ts + word.o) * 1000).toLong()
                    sb.append(formatTime(wordTimeMs, isSyllable = true))
                    sb.append(word.c)
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
