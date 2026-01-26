package com.music.vivi.lyrics

import kotlinx.coroutines.flow.MutableStateFlow

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<WordEntry>? = null,
    val isInstrumental: Boolean = false,
    val romanizedTextFlow: MutableStateFlow<String?> = MutableStateFlow(null),
) : Comparable<LyricsEntry> {
    data class WordEntry(val time: Long, val text: String, val duration: Long? = null)

    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}
