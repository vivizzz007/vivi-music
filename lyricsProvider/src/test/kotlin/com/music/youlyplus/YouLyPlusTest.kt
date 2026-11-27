/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.youlyplus

import com.music.youlyplus.models.LyricsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouLyPlusTest {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `convertToLrc emits bg and agent markers for line and word synced lyrics`() {
        val payload = """
            {
              "type": "Word",
              "lyrics": [
                {
                  "text": "Lead line",
                  "time": 1000,
                  "duration": 2000,
                  "element": { "singer": "v1", "isBackground": false },
                  "syllabus": [
                    { "text": "Lead ", "time": 1000, "duration": 1000, "isBackground": false },
                    { "text": "line", "time": 2000, "duration": 1000, "isBackground": false }
                  ]
                },
                {
                  "text": "Backing vocal",
                  "time": 3500,
                  "duration": 1500,
                  "element": { "singer": "v2", "isBackground": true }
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<LyricsResponse>(payload)
        val lrc = with(YouLyPlus) { response.lyrics?.convertToLrc() }

        assertNotNull(lrc)
        val lines = lrc!!.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("[00:01.000]{agent:v1}<00:01.000>Lead <00:02.000>line"))
        assertEquals("[00:03.500]{bg}{agent:v2}Backing vocal", lines[1])
    }
}

