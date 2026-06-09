package com.music.vivi.canvas

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TidalCanvasProviderTest {

    @Test
    fun testFormatVideoUrl() {
        val videoId = "00000000-0000-0000-0000-000000000000"
        val expected = "https://resources.tidal.com/videos/00000000/0000/0000/0000/000000000000/1280x1280.mp4"
        val actual = TidalCanvasProvider.formatVideoUrl(videoId)
        assertEquals(expected, actual)
    }
}
