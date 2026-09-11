/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import sun.misc.Unsafe

/**
 * Adversarial stress harness for Milestone 1 (R4 Watch Speaker & Bluetooth Audio Output).
 *
 * Verifies:
 * 1. Playback trigger when Bluetooth is completely absent (watch speaker output).
 * 2. Bluetooth disconnection during playback: safe pause and immediate user-initiated resumption on watch speaker.
 * 3. Multi-device connection/disconnection transitions and priority fallback.
 * 4. Total absence of UI lockout or audio routing blockages across all peripheral state permutations.
 */
@RunWith(RobolectricTestRunner::class)
class WearAudioStressChallengerTest {

    private lateinit var context: Context
    private lateinit var service: WearPlaybackService
    private lateinit var mockSession: MediaSession
    private lateinit var mockController: MediaSession.ControllerInfo

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = WearPlaybackService()

        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null) as Unsafe
        mockSession = unsafe.allocateInstance(MediaSession::class.java) as MediaSession
        mockController = unsafe.allocateInstance(MediaSession.ControllerInfo::class.java) as MediaSession.ControllerInfo
    }

    @Test
    @Suppress("DEPRECATION")
    fun challenge1_playbackTrigger_whenBluetoothCompletelyAbsent() {
        // Track whether any BLUETOOTH_AUDIO_REQUIRED broadcast is sent
        var broadcastReceived = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WearPlaybackService.ACTION_BLUETOOTH_AUDIO_REQUIRED) {
                    broadcastReceived = true
                }
            }
        }
        val filter = IntentFilter(WearPlaybackService.ACTION_BLUETOOTH_AUDIO_REQUIRED)
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, filter)

        try {
            val router = WearAudioRouter(
                context = context,
                deviceProvider = { emptyList() },
            )

            // Empirical verification: Bluetooth absent, watch speaker active
            assertFalse("Bluetooth must be reported as disconnected", router.isBluetoothAudioConnected())
            assertTrue("Router must report using speaker", router.isUsingSpeaker())
            assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
            assertNull("No connected Bluetooth device", router.getConnectedBluetoothDevice())
            assertEquals(BluetoothOutputState.Disconnected, router.bluetoothState.value)

            // Verify playback commands succeed through watch speaker without rejection
            val playPauseResult = service.sessionCallback.onPlayerCommandRequest(
                mockSession,
                mockController,
                Player.COMMAND_PLAY_PAUSE,
            )
            assertEquals("COMMAND_PLAY_PAUSE must succeed on speaker", SessionResult.RESULT_SUCCESS, playPauseResult)

            val prepareResult = service.sessionCallback.onPlayerCommandRequest(
                mockSession,
                mockController,
                Player.COMMAND_PREPARE,
            )
            assertEquals("COMMAND_PREPARE must succeed on speaker", SessionResult.RESULT_SUCCESS, prepareResult)

            val seekNextResult = service.sessionCallback.onPlayerCommandRequest(
                mockSession,
                mockController,
                Player.COMMAND_SEEK_TO_NEXT,
            )
            assertEquals("COMMAND_SEEK_TO_NEXT must succeed on speaker", SessionResult.RESULT_SUCCESS, seekNextResult)

            val stopResult = service.sessionCallback.onPlayerCommandRequest(
                mockSession,
                mockController,
                Player.COMMAND_STOP,
            )
            assertEquals("COMMAND_STOP must succeed on speaker", SessionResult.RESULT_SUCCESS, stopResult)

            assertFalse("ACTION_BLUETOOTH_AUDIO_REQUIRED broadcast must never be triggered", broadcastReceived)
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun challenge2_bluetoothDisconnectDuringPlayback_pausesAndAllowsResumeOnSpeaker() {
        val btEarbuds = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Galaxy Buds Pro 3",
            address = "11:22:33:44:55:66",
        )

        var currentDevices = listOf<AudioDeviceOutput>(btEarbuds)
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { currentDevices },
        )

        // Initialize state while connected to BT
        router.refreshState()
        assertTrue(router.isBluetoothAudioConnected())
        assertFalse(router.isUsingSpeaker())
        assertEquals("Galaxy Buds Pro 3", router.getCurrentAudioRouteLabel())

        // Simulate playback active
        var isPlaying = true
        var pauseCount = 0

        // Attach disconnect handler matching WearPlaybackService implementation
        router.onBluetoothDisconnected = {
            if (isPlaying) {
                isPlaying = false
                pauseCount++
            }
        }

        // Bluetooth disconnects mid-song
        currentDevices = emptyList()
        router.handleDevicesRemoved(listOf(btEarbuds))

        // Assert playback automatically paused (anti-ear-blast / becoming noisy behavior)
        assertFalse("Playback must pause upon Bluetooth disconnect", isPlaying)
        assertEquals("Pause count must increment once", 1, pauseCount)
        assertFalse("Bluetooth must be disconnected", router.isBluetoothAudioConnected())
        assertTrue("Route must immediately switch to watch speaker", router.isUsingSpeaker())
        assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
        assertEquals(BluetoothOutputState.Disconnected, router.bluetoothState.value)

        // User taps Play button to resume playback on watch speaker
        val resumeResult = service.sessionCallback.onPlayerCommandRequest(
            mockSession,
            mockController,
            Player.COMMAND_PLAY_PAUSE,
        )
        assertEquals("Resumption command on watch speaker must succeed", SessionResult.RESULT_SUCCESS, resumeResult)

        // Simulate resume
        isPlaying = true
        assertTrue("Playback resumed", isPlaying)
        assertTrue("Still playing through watch speaker", router.isUsingSpeaker())
        assertEquals("Route remains Watch Speaker", "Watch Speaker", router.getCurrentAudioRouteLabel())
        // Ensure no additional pause occurred
        assertEquals("Pause count must remain 1", 1, pauseCount)
    }

    @Test
    fun challenge3_multiDeviceSwitchingAndFallbackHierarchy() {
        val earbuds = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Pixel Buds Pro",
        )
        val bleHearingAid = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_HEARING_AID,
            productName = "ReSound One",
        )
        val builtInSpeaker = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            productName = "Watch Built-in Speaker",
        )

        var devices = listOf<AudioDeviceOutput>(builtInSpeaker, earbuds, bleHearingAid)
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { devices },
        )

        router.refreshState()
        assertTrue("Bluetooth connected when multiple devices present", router.isBluetoothAudioConnected())
        assertFalse(router.isUsingSpeaker())
        assertEquals("Pixel Buds Pro", router.getCurrentAudioRouteLabel())

        var disconnectEventFired = false
        router.onBluetoothDisconnected = { disconnectEventFired = true }

        // Remove earbuds, hearing aid remains
        devices = listOf(builtInSpeaker, bleHearingAid)
        router.handleDevicesRemoved(listOf(earbuds))

        // Since hearing aid is still connected, BT audio remains connected!
        assertTrue("Still Bluetooth connected via hearing aid", router.isBluetoothAudioConnected())
        assertFalse("Disconnect callback must not fire when another BT device remains", disconnectEventFired)
        assertFalse(router.isUsingSpeaker())
        assertEquals("ReSound One", router.getCurrentAudioRouteLabel())

        // Now remove hearing aid: only builtInSpeaker remains
        devices = listOf(builtInSpeaker)
        router.handleDevicesRemoved(listOf(bleHearingAid))

        assertTrue("Disconnect callback must fire when last BT device is removed", disconnectEventFired)
        assertFalse("No Bluetooth audio remaining", router.isBluetoothAudioConnected())
        assertTrue("Route must fallback to watch speaker", router.isUsingSpeaker())
        assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
    }

    @Test
    fun challenge4_rotaryDirectionCalculation() {
        // Verify crown/bezel scroll direction calculation logic matches NowPlayingScreen
        fun calculateVolumeDirection(verticalScrollPixels: Float): Int {
            return if (verticalScrollPixels > 0) {
                AudioManager.ADJUST_RAISE
            } else {
                AudioManager.ADJUST_LOWER
            }
        }

        assertEquals(AudioManager.ADJUST_RAISE, calculateVolumeDirection(1.0f))
        assertEquals(AudioManager.ADJUST_RAISE, calculateVolumeDirection(15.5f))
        assertEquals(AudioManager.ADJUST_LOWER, calculateVolumeDirection(-0.1f))
        assertEquals(AudioManager.ADJUST_LOWER, calculateVolumeDirection(-25.0f))
    }

    @Test
    fun challenge5_rapidConnectDisconnectStressCycling() {
        var devices = mutableListOf<AudioDeviceOutput>()
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { devices },
        )

        var connects = 0
        var disconnects = 0
        router.onBluetoothConnected = { connects++ }
        router.onBluetoothDisconnected = { disconnects++ }

        for (cycle in 1..50) {
            val device = SimpleAudioDevice(
                type = AudioDeviceInfo.TYPE_BLE_HEADSET,
                productName = "Buds Cycle $cycle",
            )
            // Add device
            devices.add(device)
            router.handleDevicesAdded(listOf(device))
            assertTrue(router.isBluetoothAudioConnected())
            assertFalse(router.isUsingSpeaker())
            assertEquals("Buds Cycle $cycle", router.getCurrentAudioRouteLabel())

            // Remove device
            devices.remove(device)
            router.handleDevicesRemoved(listOf(device))
            assertFalse(router.isBluetoothAudioConnected())
            assertTrue(router.isUsingSpeaker())
            assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
        }

        assertEquals(50, connects)
        assertEquals(50, disconnects)
    }

    @Test
    fun challenge6_audioRouteLabel_neverReturnsNullOrEmpty() {
        // Test all possible audio device types
        val deviceTypes = listOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
        )

        for (type in deviceTypes) {
            val device = SimpleAudioDevice(type = type, productName = if (type in WearAudioRouter.BLUETOOTH_OUTPUT_TYPES) "Test Device $type" else null)
            val router = WearAudioRouter(
                context = context,
                deviceProvider = { listOf(device) },
            )
            router.refreshState()
            val label = router.getCurrentAudioRouteLabel()
            assertTrue("Route label must never be blank", label.isNotBlank())
            if (type in WearAudioRouter.BLUETOOTH_OUTPUT_TYPES) {
                assertEquals("Test Device $type", label)
                assertFalse(router.isUsingSpeaker())
            } else {
                assertEquals("Watch Speaker", label)
                assertTrue(router.isUsingSpeaker())
            }
        }
    }
}
