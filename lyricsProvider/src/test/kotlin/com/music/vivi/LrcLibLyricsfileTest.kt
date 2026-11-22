/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi

import com.music.lrclib.LrcLib
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcLibLyricsfileTest {

    @Test
    fun testParseWordSyncedLyricsfile() {
        val yaml = """
            version: '1.0'
            metadata:
              title: Flower In A Hurricane
              artist: Justice
              duration_ms: 224000
            lines:
              - text: 'Only love never hesitates'
                start_ms: 65000
                end_ms: 68360
                words:
                  - text: 'Only '
                    start_ms: 65000
                    end_ms: 65500
                  - text: 'love '
                    start_ms: 65500
                    end_ms: 66000
                  - text: 'never '
                    start_ms: 66000
                    end_ms: 67000
                  - text: 'hesitates'
                    start_ms: 67000
                    end_ms: 68360
              - text: 'Only trust has a price to pay'
                start_ms: 68360
                end_ms: 72370
                words:
                  - text: 'Only '
                    start_ms: 68360
                    end_ms: 69000
                  - text: 'trust'
                    start_ms: 69000
                    end_ms: 72370
            plain: |
              Only love never hesitates
              Only trust has a price to pay
        """.trimIndent()

        val lrc = LrcLib.parseLyricsfileToWordSyncLrc(yaml)
        assertNotNull(lrc)
        assertTrue(lrc!!.contains("[01:05.00]Only love never hesitates"))
        assertTrue(lrc.contains("<Only :65.0:65.5|love :65.5:66.0|never :66.0:67.0|hesitates:67.0:68.36>"))
        assertTrue(lrc.contains("[01:08.36]Only trust has a price to pay"))
        assertTrue(lrc.contains("<Only :68.36:69.0|trust:69.0:72.37>"))
    }

    @Test
    fun testParseTrack21483195Format() {
        // Real LRCLIB track 21483195 format: 0 indent on lines, words without end_ms, escaped single quotes
        val yaml = """
            version: '1.0'
            metadata:
              title: Ariana Grande - Love Me Harder
              artist: Ariana Grande - Love Me Harder
              duration_ms: 236000
            lines:
            - text: Tell me something, I need to know
              start_ms: 3200
              end_ms: 7368
              words:
              - text: 'Tell '
                start_ms: 3200
              - text: 'me '
                start_ms: 3907
              - text: 'something, '
                start_ms: 4154
              - text: 'I '
                start_ms: 5472
              - text: 'need '
                start_ms: 5766
              - text: 'to '
                start_ms: 6216
              - text: know
                start_ms: 6606
            - text: Then take my breath and never let it go
              start_ms: 7920
              end_ms: 11864
              words:
              - text: 'Then '
                start_ms: 7920
              - text: 'take '
                start_ms: 8230
            - text: '''Cause if you want to keep me'
              start_ms: 41150
              end_ms: 43834
              words:
              - text: '''Cause '
                start_ms: 41150
              - text: 'if '
                start_ms: 41601
            plain: |-
              Tell me something, I need to know
        """.trimIndent()

        val lrc = LrcLib.parseLyricsfileToWordSyncLrc(yaml)
        assertNotNull(lrc)
        assertTrue(lrc!!.contains("[00:03.20]Tell me something, I need to know"))
        // Check that end time was derived from the next word's start time and line's end_ms
        assertTrue(lrc.contains("<Tell :3.2:3.907|me :3.907:4.154|something, :4.154:5.472|I :5.472:5.766|need :5.766:6.216|to :6.216:6.606|know:6.606:7.368>"))
        assertTrue(lrc.contains("[00:07.92]Then take my breath and never let it go"))
        // Check unescaping of double single-quotes
        assertTrue(lrc.contains("[00:41.15]'Cause if you want to keep me"))
        assertTrue(lrc.contains("<'Cause :41.15:41.601|if :41.601:43.834>"))
    }

    @Test
    fun testParseLineOnlyLyricsfileReturnsNull() {
        val yaml = """
            version: '1.0'
            metadata:
              title: Some Song
              artist: Some Artist
            lines:
              - text: Line one without words
                start_ms: 10000
                end_ms: 15000
              - text: Line two without words
                start_ms: 15000
                end_ms: 20000
        """.trimIndent()

        // When there are no word timings, it should return null so LrcLib falls back to syncedLyrics
        val lrc = LrcLib.parseLyricsfileToWordSyncLrc(yaml)
        assertNull(lrc)
    }

    @Test
    fun testParseEmptyLyricsfileReturnsNull() {
        assertNull(LrcLib.parseLyricsfileToWordSyncLrc(""))
        assertNull(LrcLib.parseLyricsfileToWordSyncLrc("   "))
    }
}

