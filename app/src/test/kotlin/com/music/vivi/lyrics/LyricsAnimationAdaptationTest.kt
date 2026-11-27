package com.music.vivi.lyrics

import com.music.vivi.ui.player.lyricsanimation.toViviLyricLine
import com.music.vivi.ui.player.lyricsanimation.withInstrumentalGaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsAnimationAdaptationTest {

    @Test
    fun lineSyncedUsesWholeSentenceHighlightNotWordSync() {
        val parsed = LyricsUtils.parseLyrics("[00:10.00]Love again")
        val line = parsed.single().toViviLyricLine(13_000L)
        assertFalse(line.isWordSynced)
        assertEquals(0, parsed.withInstrumentalGaps().count { it.text.isEmpty() && it.time == 0L } - 1)
    }

    @Test
    fun richSyncSkipsBackgroundLineForFallbackEndTime() {
        val lrc = """
            [00:10.00]<00:10.00>Love <00:10.50>again
            [00:10.00]{bg}<00:10.00>ooh <00:11.00>
            [00:12.00]<00:12.00>next
        """.trimIndent()
        val parsed = LyricsUtils.parseLyrics(lrc)
        val mainWords = parsed.first { !it.isBackground }.words!!
        val lastWord = mainWords.last()
        assertTrue(lastWord.endTime > lastWord.startTime)
    }
}
