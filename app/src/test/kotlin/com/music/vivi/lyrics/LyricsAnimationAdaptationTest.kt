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

    @Test
    fun bomPrefixedLrcIsDetectedAsSynced() {
        val lrc = "\uFEFF[00:05.00]Hello world"
        assertEquals(LyricsUtils.SyncType.LINE, LyricsUtils.getSyncType(lrc))
        val parsed = LyricsUtils.parseLyrics(lrc)
        assertEquals(1, parsed.size)
        assertEquals("Hello world", parsed[0].text)
    }

    @Test
    fun blankTimestampedLineIsPreservedForGaps() {
        val lrc = "[00:05.00]First\n[00:10.00]\n[00:20.00]Second"
        val parsed = LyricsUtils.parseLyrics(lrc)
        assertEquals(3, parsed.size)
        assertEquals("", parsed[1].text)
        assertEquals(10_000L, parsed[1].time)
    }

    @Test
    fun findActiveLineIndicesSkipsHeadSentinel() {
        val entries = listOf(
            LyricsEntry.HEAD_LYRICS_ENTRY,
            LyricsEntry(time = 5_000L, text = "First")
        )
        val active = LyricsUtils.findActiveLineIndices(entries, 1_000L)
        assertFalse(active.contains(0))
    }

    @Test
    fun multiTimestampLineDoesNotAttachWordTimingsToCopies() {
        val lrc = "[00:10.00][01:20.00]Chorus\n<Chorus:10.0:12.0>"
        val parsed = LyricsUtils.parseLyrics(lrc)
        assertEquals(2, parsed.size)
        assertTrue(parsed[0].words == null)
        assertTrue(parsed[1].words == null)
    }
}
