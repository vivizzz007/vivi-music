package com.music.vivi.lyrics

data class LyricsEntry(
    val time: Long,
    val text: String,
    var isTranslation: Boolean = false
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}