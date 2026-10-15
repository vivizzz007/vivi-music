/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
/**
 * VIVI-LYRICS (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 */

package com.music.musixmatch

import com.music.musixmatch.models.RichSyncEntry
import com.music.musixmatch.models.WordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusixmatchTest {

    @Test
    fun testConvertRichSyncToLrc() {
        val entries = listOf(
            RichSyncEntry(
                ts = 32.24,
                te = 34.033,
                l = listOf(
                    WordEntry("This", 0.0),
                    WordEntry(" ", 0.391),
                    WordEntry("is", 0.436),
                    WordEntry(" ", 0.559),
                    WordEntry("the", 0.627),
                    WordEntry(" ", 0.9),
                    WordEntry("end", 1.287)
                ),
                x = "This is the end"
            )
        )

        val lrc = Musixmatch.convertRichSyncToLrc(entries)
        
        // Expected formatted LRC (inline RichSync):
        // [00:32.240]<00:32.240>This <00:32.676>is <00:32.867>the <00:33.527>end
        val expected = "[00:32.240]<00:32.240>This <00:32.676>is <00:32.867>the <00:33.527>end\n"
        assertEquals(expected, lrc)
    }

    @Test
    fun testConvertSubtitleJsonToLrc() {
        val json = """[{"text":"This is the first line","time":{"total":12.345}},{"text":"Second line","time":{"total":15.0}}]"""
        val lrc = Musixmatch.convertSubtitleJsonToLrc(json)
        
        val expected = "[00:12.345]This is the first line\n[00:15.000]Second line\n"
        assertEquals(expected, lrc)
    }
    
    @Test
    fun testMultiArtistClean() {
        val input = "Taylor Swift, Ed Sheeran & Someone Else (feat. Guest) [Remix]"
        val cleaned = Musixmatch.cleanText(input)
        
        // Should preserve space for each word separated by comma or ampersand
        val expected = "taylor swift ed sheeran someone else"
        assertEquals(expected, cleaned)
    }

    @Test
    fun testConvertRichSyncToLrcMultipleEntries() {
        val entries = listOf(
            RichSyncEntry(
                ts = 0.0,
                te = 2.0,
                l = listOf(
                    WordEntry("Hello", 0.0),
                    WordEntry(" ", 0.3),
                    WordEntry("world", 0.5)
                ),
                x = "Hello world"
            ),
            RichSyncEntry(
                ts = 3.0,
                te = 5.0,
                l = listOf(
                    WordEntry("Good", 0.0),
                    WordEntry(" ", 0.2),
                    WordEntry("bye", 0.4)
                ),
                x = "Good bye"
            )
        )

        val lrc = Musixmatch.convertRichSyncToLrc(entries)
        val lines = lrc.trimEnd().split("\n")
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("[00:00.000]"))
        assertTrue(lines[1].startsWith("[00:03.000]"))
    }

    @Test
    fun testConvertRichSyncSkipsEmptyLines() {
        val entries = listOf(
            RichSyncEntry(
                ts = 1.0,
                te = 2.0,
                l = listOf(WordEntry(" ", 0.0)),
                x = "   "
            ),
            RichSyncEntry(
                ts = 3.0,
                te = 4.0,
                l = listOf(WordEntry("word", 0.0)),
                x = "word"
            )
        )

        val lrc = Musixmatch.convertRichSyncToLrc(entries)
        val lines = lrc.trimEnd().split("\n")
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("word"))
    }

    @Test
    fun testCleanTextRemovesFeatAndBrackets() {
        val input = "Song Title (feat. Artist) [Live Version]"
        val cleaned = Musixmatch.cleanText(input)
        assertEquals("song title", cleaned)
    }

    @Test
    fun debugKaliUchis() = kotlinx.coroutines.runBlocking {
        println("STARTING DEBUG KALI UCHIS")
        Musixmatch.getAllLyrics("all i can say", "kali uchis", 258) {
            println("LYRICS FOUND:")
            println(it.take(100))
        }
        println("FINISHED DEBUG KALI UCHIS")
    }
}
