/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearAuthUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGeneratePairingPinFormat() {
        repeat(100) {
            val pin = WearAuthUtils.generatePairingPin()
            assertEquals("PIN must be exactly 4 digits", 4, pin.length)
            assertTrue("PIN must be all numeric digits: $pin", pin.all { it.isDigit() })
            val numericVal = pin.toInt()
            assertTrue("PIN must be in range 0..9999: $numericVal", numericVal in 0..9999)
        }
    }

    @Test
    fun testNormalizeDataSyncIdWithNull() {
        assertNull(WearAuthUtils.normalizeDataSyncId(null))
    }

    @Test
    fun testNormalizeDataSyncIdWithoutDelegationDelimiter() {
        val raw = "simple_datasync_id_12345"
        assertEquals(raw, WearAuthUtils.normalizeDataSyncId(raw))
    }

    @Test
    fun testNormalizeDataSyncIdWithDelegatedIdentifier() {
        val raw = "account_prefix||delegated_target_id"
        assertEquals("delegated_target_id", WearAuthUtils.normalizeDataSyncId(raw))
    }

    @Test
    fun testNormalizeDataSyncIdWithEmptyDelegatedIdentifierFallsBack() {
        val raw = "fallback_prefix||"
        assertEquals("fallback_prefix", WearAuthUtils.normalizeDataSyncId(raw))
    }

    @Test
    fun testNormalizeDataSyncIdWithEmptyPrefix() {
        val raw = "||delegated_primary"
        assertEquals("delegated_primary", WearAuthUtils.normalizeDataSyncId(raw))
    }

    @Test
    fun testGetLocalIpAddressDoesNotCrash() {
        val ip = WearAuthUtils.getLocalIpAddress(context)
        // May be null or IPv4 string under Robolectric test environment
        if (ip != null) {
            assertTrue("IP must match IPv4 structure: $ip", ip.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")))
        }
    }
}
