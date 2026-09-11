/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearAuthPreferencesTest {

    private lateinit var context: Context
    private lateinit var preferences: WearAuthPreferences

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        preferences = WearAuthPreferences(context)
        preferences.clearCredentials()
    }

    @After
    fun tearDown() = runBlocking {
        preferences.clearCredentials()
    }

    @Test
    fun testSaveAndReadCredentials() = runBlocking {
        val testCookie = "SAPISID=12345; HSID=67890; SSID=abcde"
        val testDataSyncId = "test_data_sync_id"
        val testVisitorData = "Cgt2aXNpdG9yX2RhdGE"
        val testAccountName = "Jane Doe"
        val testAccountEmail = "jane.doe@example.com"
        val testChannelHandle = "@janedoe"

        preferences.saveCredentials(
            cookie = testCookie,
            dataSyncId = testDataSyncId,
            visitorData = testVisitorData,
            accountName = testAccountName,
            accountEmail = testAccountEmail,
            accountChannelHandle = testChannelHandle,
        )

        assertEquals(testCookie, preferences.innerTubeCookie.first())
        assertEquals(testDataSyncId, preferences.dataSyncId.first())
        assertEquals(testVisitorData, preferences.visitorData.first())
        assertEquals(testAccountName, preferences.accountName.first())
        assertEquals(testAccountEmail, preferences.accountEmail.first())
        assertEquals(testChannelHandle, preferences.accountChannelHandle.first())
    }

    @Test
    fun testClearCredentials() = runBlocking {
        preferences.saveCredentials(
            cookie = "cookie_to_clear",
            dataSyncId = "sync_to_clear",
            visitorData = "visitor_to_clear",
            accountName = "To Clear",
            accountEmail = "clear@example.com",
        )

        assertEquals("cookie_to_clear", preferences.innerTubeCookie.first())

        preferences.clearCredentials()

        assertNull(preferences.innerTubeCookie.first())
        assertNull(preferences.dataSyncId.first())
        assertNull(preferences.visitorData.first())
        assertNull(preferences.accountName.first())
        assertNull(preferences.accountEmail.first())
        assertNull(preferences.accountChannelHandle.first())
    }

    @Test
    fun testSaveOptionalFieldsAsNull() = runBlocking {
        preferences.saveCredentials(
            cookie = "only_cookie",
            dataSyncId = null,
            visitorData = null,
            accountName = null,
            accountEmail = null,
        )

        assertEquals("only_cookie", preferences.innerTubeCookie.first())
        assertNull(preferences.dataSyncId.first())
        assertNull(preferences.visitorData.first())
        assertNull(preferences.accountName.first())
        assertNull(preferences.accountEmail.first())
    }
}
