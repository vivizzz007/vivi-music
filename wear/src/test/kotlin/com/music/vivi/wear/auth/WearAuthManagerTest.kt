/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.music.innertube.YouTube
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearAuthManagerTest {

    private lateinit var context: Context
    private lateinit var preferences: WearAuthPreferences
    private lateinit var authManager: WearAuthManager

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        preferences = WearAuthPreferences(context)
        preferences.clearCredentials()
        authManager = WearAuthManager(context, preferences)
    }

    @After
    fun tearDown() = runBlocking {
        authManager.logout()
    }

    @Test
    fun testInitialStateIsLoggedOutWhenNoCredentials() = runBlocking {
        authManager.initFromDataStore()
        assertTrue("Auth state should be LoggedOut", authManager.authState.value is WearAuthState.LoggedOut)
        assertFalse("isAccountConnected should be false", authManager.isAccountConnected.value)
    }

    @Test
    fun testInitFromDataStoreRestoresExistingSession() = runBlocking {
        preferences.saveCredentials(
            cookie = "SAVED_COOKIE_XYZ",
            dataSyncId = "SYNC_ID_123",
            visitorData = "VISITOR_DATA_456",
            accountName = "Test User",
            accountEmail = "test@example.com",
        )

        authManager.initFromDataStore()

        val state = authManager.authState.value
        assertTrue("Auth state should be LoggedIn", state is WearAuthState.LoggedIn)
        assertEquals("Test User", (state as WearAuthState.LoggedIn).name)
        assertEquals("test@example.com", state.email)
        assertEquals("SAVED_COOKIE_XYZ", YouTube.cookie)
    }

    @Test
    fun testLogoutClearsCredentialsAndResetsState() = runBlocking {
        preferences.saveCredentials(
            cookie = "COOKIE_TO_LOGOUT",
            accountName = "Logging Out",
        )
        authManager.initFromDataStore()
        assertTrue(authManager.authState.value is WearAuthState.LoggedIn)

        authManager.logout()

        assertTrue(authManager.authState.value is WearAuthState.LoggedOut)
        assertNull(YouTube.cookie)
        assertNull(YouTube.visitorData)
        assertNull(YouTube.dataSyncId)
        assertNull(preferences.innerTubeCookie.first())
    }

    @Test
    fun testStartAndStopPairingLifecycle() {
        val info = authManager.startPairing(port = 0)
        assertNotNull(info)
        val state = authManager.authState.value
        assertTrue("State should be Pairing", state is WearAuthState.Pairing)
        val pairingState = state as WearAuthState.Pairing
        assertEquals(info.ip, pairingState.ip)
        assertEquals(info.port, pairingState.port)
        assertEquals(info.pin, pairingState.pin)

        authManager.stopPairing()
        assertTrue("State should transition to LoggedOut after stopPairing", authManager.authState.value is WearAuthState.LoggedOut)
    }
}
