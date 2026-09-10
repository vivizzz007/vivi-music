/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import androidx.media3.datasource.cache.CacheDataSource
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WearDownloadCacheTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WearDownloadCache.release()
    }

    @After
    fun tearDown() {
        WearDownloadCache.release()
    }

    @Test
    fun testCacheDirectories() {
        val downloadDir = WearDownloadCache.getDownloadCacheDir(context)
        val playerCacheDir = WearDownloadCache.getPlayerCacheDir(context)

        assertEquals(File(context.filesDir, "downloads"), downloadDir)
        assertEquals(File(context.filesDir, "player_cache"), playerCacheDir)
    }

    @Test
    fun testCacheInitializationAndSizing() {
        assertFalse(WearDownloadCache.isInitialized)

        WearDownloadCache.initialize(context)
        assertTrue(WearDownloadCache.isInitialized)

        assertNotNull(WearDownloadCache.downloadCache)
        assertNotNull(WearDownloadCache.playerCache)
        assertEquals(256 * 1024 * 1024L, WearDownloadCache.PLAYER_CACHE_MAX_BYTES)
    }

    @Test
    fun testCacheDataSourceFactoryCreation() {
        WearDownloadCache.initialize(context)

        val layeredFactory = WearDownloadCache.getCacheDataSourceFactory(context)
        assertNotNull(layeredFactory)
        assertTrue(layeredFactory is CacheDataSource.Factory)

        val downloadOnlyFactory = WearDownloadCache.getDownloadOnlyDataSourceFactory()
        assertNotNull(downloadOnlyFactory)
        assertTrue(downloadOnlyFactory is CacheDataSource.Factory)
    }

    @Test
    fun testCacheRelease() {
        WearDownloadCache.initialize(context)
        assertTrue(WearDownloadCache.isInitialized)

        WearDownloadCache.release()
        assertFalse(WearDownloadCache.isInitialized)
    }
}
