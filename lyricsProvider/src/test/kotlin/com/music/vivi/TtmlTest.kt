/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi

import com.music.vivi.betterlyrics.TTMLParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtmlTest {
    @Test
    fun testUnisonTtml() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:ttp="http://www.w3.org/ns/ttml#parameter" xmlns:composer="https://composer.betterlyrics.org/ttml" ttp:timeBase="media" xml:lang="en" composer:timing="Word"><head><metadata><ttm:title>Avenoir - AFTER HOURS</ttm:title><ttm:agent xml:id="v1" type="person"><ttm:name>Voice 1</ttm:name></ttm:agent><ttm:agent xml:id="v1000" type="group"><ttm:name>Harmony</ttm:name></ttm:agent><ttm:agent xml:id="v2000" type="other"><ttm:name>Chorus</ttm:name></ttm:agent></metadata></head><body dur="2:53.026"><div><p begin="0:24.695" end="0:27.129" ttm:agent="v1"><span begin="0:24.695" end="0:24.774">we</span> <span begin="0:24.774" end="0:25.009">could</span> <span begin="0:25.009" end="0:25.341">meet</span> <span begin="0:25.341" end="0:25.503">o</span><span begin="0:25.503" end="0:25.695">ver</span> <span begin="0:25.695" end="0:25.998">wine</span> <span begin="0:25.998" end="0:26.170">it's</span> <span begin="0:26.170" end="0:26.324">a</span> <span begin="0:26.324" end="0:26.687">ha</span><span begin="0:26.687" end="0:27.129">bit</span></p><p begin="0:27.347" end="0:30.039" ttm:agent="v1"><span begin="0:27.347" end="0:27.634">me</span> <span begin="0:27.634" end="0:27.745">and</span> <span begin="0:27.745" end="0:28.012">you</span> <span begin="0:28.012" end="0:28.135">o</span><span begin="0:28.135" end="0:28.324">ver</span> <span begin="0:28.324" end="0:28.675">wine</span> <span begin="0:28.675" end="0:29.059">that's</span> <span begin="0:29.059" end="0:29.405">hap</span><span begin="0:29.405" end="0:29.585">pen</span><span begin="0:29.585" end="0:30.039">in'</span></p></div></body></tt>"""

        val parsed = TTMLParser.parseTTML(ttml)
        assertEquals(2, parsed.size)

        assertEquals("we could meet over wine it's a habit", parsed[0].text)
        assertEquals("v1", parsed[0].agent)
        assertFalse(parsed[0].isBackground)

        assertEquals("me and you over wine that's happenin'", parsed[1].text)
        assertTrue(parsed[0].words.isNotEmpty())
    }

    @Test
    fun testSyllableMerging() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="0:10.000" end="0:12.000"><span begin="0:10.000" end="0:10.500">hel</span><span begin="0:10.500" end="0:11.000">lo</span> <span begin="0:11.000" end="0:12.000">world</span></p></div></body></tt>"""

        val parsed = TTMLParser.parseTTML(ttml)
        assertEquals(1, parsed.size)
        assertEquals("hello world", parsed[0].text)
        assertEquals(2, parsed[0].words.size)
        assertEquals("hello", parsed[0].words[0].text)
        assertEquals("world", parsed[0].words[1].text)
    }

    @Test
    fun testBackgroundVocals() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata"><body><div><p begin="0:05.000" end="0:08.000" ttm:agent="v1"><span begin="0:05.000" end="0:06.000">main</span> <span begin="0:06.000" end="0:07.000">vocal</span><span begin="0:05.500" end="0:07.500" ttm:role="x-bg"><span begin="0:05.500" end="0:06.500">back</span> <span begin="0:06.500" end="0:07.500">vocal</span></span></p></div></body></tt>"""

        val parsed = TTMLParser.parseTTML(ttml)
        assertEquals(1, parsed.size)
        assertEquals(1, parsed[0].backgroundLines.size)
        assertTrue(parsed[0].backgroundLines[0].isBackground)
        assertEquals("back vocal", parsed[0].backgroundLines[0].text)
    }

    @Test
    fun testPlainTextParagraph() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="0:01.000" end="0:03.000">plain text line</p></div></body></tt>"""

        val parsed = TTMLParser.parseTTML(ttml)
        assertEquals(1, parsed.size)
        assertEquals("plain text line", parsed[0].text)
        assertTrue(parsed[0].words.isEmpty())
    }

    @Test
    fun testToLrcOutput() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="0:05.500" end="0:08.000"><span begin="0:05.500" end="0:06.500">hello</span> <span begin="0:06.500" end="0:08.000">world</span></p></div></body></tt>"""

        val parsed = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsed)
        assertTrue(lrc.contains("[00:05.50]"))
        assertTrue(lrc.contains("hello world"))
    }

    @Test
    fun testEmptyTtml() {
        val parsed = TTMLParser.parseTTML("<tt xmlns=\"http://www.w3.org/ns/ttml\"><body><div></div></body></tt>")
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun testMalformedTtml() {
        val parsed = TTMLParser.parseTTML("not xml at all")
        assertTrue(parsed.isEmpty())
    }
}
