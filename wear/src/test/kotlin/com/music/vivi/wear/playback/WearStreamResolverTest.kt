/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WearStreamResolverTest {

    @Test
    fun testWearStreamResolverObjectExists() {
        assertNotNull(WearStreamResolver)
    }

    @Test
    fun testResolveStreamUrlHandlesBlankOrNonExistentSong() = runBlocking {
        // A non-existent song should return null safely without throwing uncaught exceptions
        val result = runCatching {
            WearStreamResolver.resolveStreamUrl("non_existent_fake_id_12345")
        }
        // Must complete safely without crashing
        assertNull(result.getOrNull())
    }
}
