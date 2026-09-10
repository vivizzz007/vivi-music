/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearAudioRouterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testBluetoothDisconnectedWhenNoDevices() {
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { emptyList() },
        )

        assertFalse(router.isBluetoothAudioConnected())
        assertEquals(BluetoothOutputState.Disconnected, router.bluetoothState.value)
    }

    @Test
    fun testBluetoothDisconnectedWhenBuiltinSpeakerOnly() {
        val speakerDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            productName = "Built-in Speaker",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(speakerDevice) },
        )

        assertFalse(router.isBluetoothAudioConnected())
    }

    @Test
    fun testBluetoothConnectedWithA2DP() {
        val btDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Galaxy Buds Pro",
            address = "AA:BB:CC:DD:EE:FF",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDevice) },
        )

        assertTrue(router.isBluetoothAudioConnected())
        assertEquals(btDevice, router.getConnectedBluetoothDevice())
        assertEquals("Galaxy Buds Pro", router.getConnectedDeviceName())
    }

    @Test
    fun testBluetoothConnectedWithBleHeadset() {
        val bleDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLE_HEADSET,
            productName = "Pixel Buds Pro",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(bleDevice) },
        )

        assertTrue(router.isBluetoothAudioConnected())
        assertEquals("Pixel Buds Pro", router.getConnectedDeviceName())
    }

    @Test
    fun testBluetoothConnectedWithHearingAid() {
        val hearingAidDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_HEARING_AID,
            productName = "Phonak Hearing Aid",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(hearingAidDevice) },
        )

        assertTrue(router.isBluetoothAudioConnected())
        assertEquals("Phonak Hearing Aid", router.getConnectedDeviceName())
    }

    @Test
    fun testBluetoothConnectedWithBleSpeaker() {
        val bleSpeaker = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLE_SPEAKER,
            productName = "JBL Flip BLE",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(bleSpeaker) },
        )

        assertTrue(router.isBluetoothAudioConnected())
        assertEquals("JBL Flip BLE", router.getConnectedDeviceName())
    }

    @Test
    fun testAudioDeviceCallbackOnAddedAndRemoved() {
        var isConnected = false
        val btDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Galaxy Buds 2",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = {
                if (isConnected) listOf(btDevice) else emptyList()
            },
        )

        var connectCalled = false
        var disconnectCalled = false
        router.onBluetoothConnected = { connectCalled = true }
        router.onBluetoothDisconnected = { disconnectCalled = true }

        // Trigger device added
        isConnected = true
        router.handleDevicesAdded(listOf(btDevice))
        assertTrue(connectCalled)
        val state = router.bluetoothState.value
        assertTrue(state is BluetoothOutputState.Connected)
        assertEquals("Galaxy Buds 2", (state as BluetoothOutputState.Connected).deviceName)

        // Trigger device removed
        isConnected = false
        router.handleDevicesRemoved(listOf(btDevice))
        assertTrue(disconnectCalled)
        assertEquals(BluetoothOutputState.Disconnected, router.bluetoothState.value)
    }

    @Test
    fun testOpenBluetoothSettingsIntent() {
        val router = WearAudioRouter(context)
        val intent = router.openBluetoothSettingsIntent()

        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, intent.action)
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun testAudioRouteLabelAndSpeakerStatusWhenDisconnected() {
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { emptyList() },
        )

        assertTrue(router.isUsingSpeaker())
        assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
    }

    @Test
    fun testAudioRouteLabelAndSpeakerStatusWhenBluetoothConnected() {
        val btDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Galaxy Buds Pro",
        )
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDevice) },
        )

        assertFalse(router.isUsingSpeaker())
        assertEquals("Galaxy Buds Pro", router.getCurrentAudioRouteLabel())
    }
}
