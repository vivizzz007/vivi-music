package com.music.vivi.lyrics

import com.music.vivi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.music.vivi.lyrics.LyricsUtils.isBelarusian
import com.music.vivi.lyrics.LyricsUtils.isBulgarian
import com.music.vivi.lyrics.LyricsUtils.isChinese
import com.music.vivi.lyrics.LyricsUtils.isDevanagari
import com.music.vivi.lyrics.LyricsUtils.isJapanese
import com.music.vivi.lyrics.LyricsUtils.isKorean
import com.music.vivi.lyrics.LyricsUtils.isKyrgyz
import com.music.vivi.lyrics.LyricsUtils.isMacedonian
import com.music.vivi.lyrics.LyricsUtils.isRussian
import com.music.vivi.lyrics.LyricsUtils.isSerbian
import com.music.vivi.lyrics.LyricsUtils.isUkrainian
import com.music.vivi.lyrics.LyricsUtils.parseLyrics
import com.music.vivi.lyrics.LyricsUtils.romanizeCyrillic
import com.music.vivi.lyrics.LyricsUtils.romanizeDevanagari
import com.music.vivi.lyrics.LyricsUtils.romanizeJapanese
import com.music.vivi.lyrics.LyricsUtils.romanizeKorean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

public object LyricsParser {

    public data class RomanizationOptions(
        val japanese: Boolean = true,
        val korean: Boolean = true,
        val russian: Boolean = true,
        val ukrainian: Boolean = true,
        val serbian: Boolean = true,
        val bulgarian: Boolean = true,
        val belarusian: Boolean = true,
        val kyrgyz: Boolean = true,
        val macedonian: Boolean = true,
        val devanagari: Boolean = true,
        val cyrillicByLine: Boolean = false,
    )

    public fun parse(lyrics: String?, scope: CoroutineScope, options: RomanizationOptions): List<LyricsEntry> {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
            return emptyList()
        }

        val entries = if (lyrics.startsWith("[")) {
            val parsedLines = parseLyrics(lyrics)
            parsedLines.map { it.copy() }.let {
                listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + it
            }
        } else {
            lyrics.lines().mapIndexed { index, line ->
                LyricsEntry(index * 100L, line, null)
            }
        }

        // Determine language detection once if not per-line cyrillic
        val isRussianLyrics = options.russian && !options.cyrillicByLine && isRussian(lyrics)
        val isUkrainianLyrics = options.ukrainian && !options.cyrillicByLine && isUkrainian(lyrics)
        val isSerbianLyrics = options.serbian && !options.cyrillicByLine && isSerbian(lyrics)
        val isBulgarianLyrics = options.bulgarian && !options.cyrillicByLine && isBulgarian(lyrics)
        val isBelarusianLyrics = options.belarusian && !options.cyrillicByLine && isBelarusian(lyrics)
        val isKyrgyzLyrics = options.kyrgyz && !options.cyrillicByLine && isKyrgyz(lyrics)
        val isMacedonianLyrics = options.macedonian && !options.cyrillicByLine && isMacedonian(lyrics)

        entries.forEach { entry ->
            val text = entry.text

            if (options.japanese && isJapanese(text) && !isChinese(text)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeJapanese(text) }
            }

            if (options.korean && isKorean(text)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeKorean(text) }
            }

            if (options.devanagari && isDevanagari(text)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeDevanagari(text) }
            }

            // Cyrillic checks
            if (options.russian && (if (options.cyrillicByLine) isRussian(text) else isRussianLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            } else if (options.ukrainian && (if (options.cyrillicByLine) isUkrainian(text) else isUkrainianLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            } else if (options.serbian && (if (options.cyrillicByLine) isSerbian(text) else isSerbianLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            } else if (options.bulgarian && (if (options.cyrillicByLine) isBulgarian(text) else isBulgarianLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            } else if (options.belarusian && (if (options.cyrillicByLine) isBelarusian(text) else isBelarusianLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            } else if (options.kyrgyz && (if (options.cyrillicByLine) isKyrgyz(text) else isKyrgyzLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            } else if (options.macedonian && (if (options.cyrillicByLine) isMacedonian(text) else isMacedonianLyrics)) {
                scope.launch { entry.romanizedTextFlow.value = romanizeCyrillic(text) }
            }
        }

        return entries
    }
}
