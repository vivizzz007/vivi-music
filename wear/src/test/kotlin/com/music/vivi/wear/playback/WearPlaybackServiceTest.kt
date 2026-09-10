/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearPlaybackServiceTest {

    @Test
    fun testBuildMediaItem() {
        val artworkUri = Uri.parse("https://example.com/artwork.jpg")
        val mediaItem = WearPlaybackService.buildMediaItem(
            songId = "video_12345",
            title = "Awesome Track",
            artist = "Vivi Artist",
            artworkUri = artworkUri,
        )

        assertEquals("video_12345", mediaItem.mediaId)
        assertEquals("vivi://song/video_12345", mediaItem.localConfiguration?.uri.toString())
        assertEquals("video_12345", mediaItem.localConfiguration?.customCacheKey)
        assertEquals("Awesome Track", mediaItem.mediaMetadata.title.toString())
        assertEquals("Vivi Artist", mediaItem.mediaMetadata.artist.toString())
        assertEquals(artworkUri, mediaItem.mediaMetadata.artworkUri)
    }

    @Test
    fun testBroadcastActionConstant() {
        assertEquals(
            "com.music.vivi.wear.action.BLUETOOTH_AUDIO_REQUIRED",
            WearPlaybackService.ACTION_BLUETOOTH_AUDIO_REQUIRED,
        )
    }

    @Test
    fun testBluetoothOutputStates() {
        val disconnectedState: BluetoothOutputState = BluetoothOutputState.Disconnected
        assertEquals(BluetoothOutputState.Disconnected, disconnectedState)

        val connectedState = BluetoothOutputState.Connected(
            deviceName = "Galaxy Buds Pro",
            deviceType = 8,
            address = "AA:BB:CC:DD:EE:FF",
        )
        assertEquals("Galaxy Buds Pro", connectedState.deviceName)
        assertEquals(8, connectedState.deviceType)
        assertEquals("AA:BB:CC:DD:EE:FF", connectedState.address)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testPlaybackCommandSucceedsWhenBluetoothDisconnected() {
        val router = WearAudioRouter(
            context = ApplicationProvider.getApplicationContext(),
            deviceProvider = { emptyList() },
        )
        // Verify Bluetooth is disconnected and watch speaker is active route
        org.junit.Assert.assertFalse(router.isBluetoothAudioConnected())
        org.junit.Assert.assertTrue(router.isUsingSpeaker())
        assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())

        // Verify sessionCallback in WearPlaybackService permits play commands without error
        val service = WearPlaybackService()
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        val session = unsafe.allocateInstance(androidx.media3.session.MediaSession::class.java) as androidx.media3.session.MediaSession
        val controller = unsafe.allocateInstance(androidx.media3.session.MediaSession.ControllerInfo::class.java) as androidx.media3.session.MediaSession.ControllerInfo

        val result = service.sessionCallback.onPlayerCommandRequest(
            session,
            controller,
            androidx.media3.common.Player.COMMAND_PLAY_PAUSE,
        )
        assertEquals(androidx.media3.session.SessionResult.RESULT_SUCCESS, result)
    }

    @Test
    fun testOnBluetoothDisconnectedAllowsWatchSpeakerRoute() {
        var disconnectedCallbackInvoked = false
        val router = WearAudioRouter(
            context = ApplicationProvider.getApplicationContext(),
            deviceProvider = { emptyList() },
        )
        router.onBluetoothDisconnected = {
            disconnectedCallbackInvoked = true
        }

        // Simulate Bluetooth removal event
        router.handleDevicesRemoved(emptyList())

        org.junit.Assert.assertTrue(disconnectedCallbackInvoked)
        org.junit.Assert.assertTrue(router.isUsingSpeaker())
        assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
    }
}
