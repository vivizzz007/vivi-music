package com.music.vivi.lyrics

import com.music.vivi.lyrics.LyricsParser.RomanizationOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsParserTest {

    private val testScope = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher())

    @Test
    fun `parse returns empty list for null lyrics`() {
        val result = LyricsParser.parse(null, testScope, RomanizationOptions())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse returns correct entries for plain lyrics`() {
        val text = """
            Line 1
            Line 2
        """.trimIndent()

        val result = LyricsParser.parse(text, testScope, RomanizationOptions())

        assertEquals(2, result.size)
        assertEquals("Line 1", result[0].text)
        assertEquals(0L, result[0].time) // Plain lyrics usually have generated timestamps or 0
        assertEquals("Line 2", result[1].text)
    }

    @Test
    fun `parse handles LRC format correctly`() {
        // [00:01.00]Hello
        // [00:02.50]World
        val lrc = "[00:01.00]Hello\n[00:02.50]World"

        val result = LyricsParser.parse(lrc, testScope, RomanizationOptions())

        // Parser adds HEAD_LYRICS_ENTRY at start for synced lyrics
        assertEquals(3, result.size)

        assertEquals(LyricsEntry.HEAD_LYRICS_ENTRY, result[0])

        assertEquals(1000L, result[1].time)
        assertEquals("Hello", result[1].text)

        assertEquals(2500L, result[2].time)
        assertEquals("World", result[2].text)
    }
}
