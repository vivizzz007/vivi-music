/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class WearDownloadServiceTest {

    @Test
    fun testServiceConstants() {
        assertEquals("wear_downloads", WearDownloadService.CHANNEL_ID)
        assertEquals(1001, WearDownloadService.NOTIFICATION_ID)
        assertEquals(1002, WearDownloadService.JOB_ID)
        assertEquals(1000L, WearDownloadService.FOREGROUND_NOTIFICATION_UPDATE_INTERVAL)
        assertEquals(
            "com.music.vivi.wear.action.CANCEL_ALL_DOWNLOADS",
            WearDownloadService.ACTION_CANCEL_ALL,
        )
    }
}
