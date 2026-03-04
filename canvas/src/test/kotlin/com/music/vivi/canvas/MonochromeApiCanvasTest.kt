package com.music.vivi.canvas

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonochromeApiCanvasTest {

    @Test
    fun testFormatVideoUrl() {
        val videoId = "00000000-0000-0000-0000-000000000000"
        val expected = "https://resources.tidal.com/videos/00000000/0000/0000/0000/000000000000/1280x1280.mp4"
        val actual = MonochromeApiCanvas.formatVideoUrl(videoId)
        assertEquals(expected, actual)
    }

    @Test
    fun testCacheBehavior() = runBlocking {
        // This test would require mocking the HttpClient which is set by lazy
        // In a real project, we'd use a test HttpClient engine
    }
}
