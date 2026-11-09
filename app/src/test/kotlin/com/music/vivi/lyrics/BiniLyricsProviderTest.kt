package com.music.vivi.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiniLyricsProviderTest {
    @Test
    fun beginAttributeInAnyPosition() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p ttm:agent="v1" begin="0:05.000" end="0:08.000"><span ttm:role="x-bg" begin="0:05.000" end="0:06.000">hello</span></p></div></body></tt>"""
        val lrc = BiniLyricsProvider.ttmlToLrc(ttml)
        assertTrue(lrc.contains("[00:05.00]"))
        assertTrue(lrc.contains("hello"))
    }

    @Test
    fun singleQuotesSupported() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin='0:10.000' end='0:12.000'><span begin='0:10.000' end='0:11.000'>hi</span></p></div></body></tt>"""
        val lrc = BiniLyricsProvider.ttmlToLrc(ttml)
        assertTrue(lrc.contains("[00:10.00]"))
        assertTrue(lrc.contains("hi"))
    }

    @Test
    fun plainTextParagraph() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="0:01.000" end="0:03.000">plain line</p></div></body></tt>"""
        assertEquals("[00:01.00]plain line", BiniLyricsProvider.ttmlToLrc(ttml))
    }

    @Test
    fun emptyAndMalformedReturnBlank() {
        assertEquals("", BiniLyricsProvider.ttmlToLrc(""))
        assertEquals("", BiniLyricsProvider.ttmlToLrc("not xml"))
        assertEquals("", BiniLyricsProvider.ttmlToLrc("""<tt><body><div></div></body></tt>"""))
    }

    @Test
    fun wordLevelTimestamps() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="0:05.500" end="0:08.000"><span begin="0:05.500" end="0:06.500">hello</span> <span begin="0:06.500" end="0:08.000">world</span></p></div></body></tt>"""
        val lrc = BiniLyricsProvider.ttmlToLrc(ttml)
        assertTrue(lrc.startsWith("[00:05.50]"))
        assertTrue(lrc.contains("<00:05.50> hello"))
        assertTrue(lrc.contains("<00:06.50> world"))
    }
}
