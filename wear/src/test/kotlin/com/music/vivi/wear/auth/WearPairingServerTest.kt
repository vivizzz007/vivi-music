/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class WearPairingServerTest {

    private lateinit var context: Context
    private var server: WearPairingServer? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        server?.stop()
        server = null
    }

    @Test
    fun testServerServesPairingHtmlPage() {
        val serverInstance = WearPairingServer(context)
        server = serverInstance
        val info = serverInstance.start(port = 0) // ephemeral port

        val url = URL("http://127.0.0.1:${info.port}/pair?pin=${info.pin}")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"

        val responseCode = conn.responseCode
        assertEquals("GET /pair must return 200 OK", 200, responseCode)

        val body = conn.inputStream.bufferedReader().use { it.readText() }
        assertTrue("Page must contain app branding", body.contains("Vivi Music"))
        assertTrue("Page must display watch PIN", body.contains(info.pin))
        assertTrue("Page must include deep link scheme", body.contains("vivimusic://pair"))
        assertTrue("Page must include credentials form", body.contains("InnerTube Cookie"))
    }

    @Test
    fun testServerRejectsPostWithInvalidPin() {
        val serverInstance = WearPairingServer(context)
        server = serverInstance
        val info = serverInstance.start(port = 0)

        val url = URL("http://127.0.0.1:${info.port}/api/auth")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")

        val payload = """{"pin":"9999","cookie":"test_cookie"}"""
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val responseCode = conn.responseCode
        assertEquals("Wrong PIN must return 401 Unauthorized", 401, responseCode)
    }

    @Test
    fun testServerAcceptsPostWithValidPinAndExtractsCredentials() = runBlocking {
        val receivedCredentials = CompletableDeferred<Triple<String, String?, String?>>()

        val serverInstance = WearPairingServer(context) { cookie, dataSyncId, visitorData ->
            receivedCredentials.complete(Triple(cookie, dataSyncId, visitorData))
        }
        server = serverInstance
        val info = serverInstance.start(port = 0)

        val url = URL("http://127.0.0.1:${info.port}/api/auth")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")

        val testCookie = "SAPISID=test12345; SSID=test6789"
        val testSyncId = "sync_9876"
        val testVisitor = "visitor_abc"

        val payload = """
            {
              "pin": "${info.pin}",
              "cookie": "$testCookie",
              "dataSyncId": "$testSyncId",
              "visitorData": "$testVisitor"
            }
        """.trimIndent()

        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val responseCode = conn.responseCode
        assertEquals("Valid credentials and PIN must return 200 OK", 200, responseCode)

        val (cookie, dataSyncId, visitorData) = withTimeout(5000) {
            receivedCredentials.await()
        }

        assertEquals(testCookie, cookie)
        assertEquals(testSyncId, dataSyncId)
        assertEquals(testVisitor, visitorData)
    }
}
