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
import org.junit.Assert.assertNull
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

    @Test
    fun testAudioRouteLabel_whenBluetoothDeviceNameIsNull_returnsBluetoothAudio() {
        val btDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = null,
        )
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDevice) },
        )

        assertTrue(router.isBluetoothAudioConnected())
        assertFalse(router.isUsingSpeaker())
        assertEquals("Bluetooth Audio", router.getConnectedDeviceName())
        assertEquals("Bluetooth Audio", router.getCurrentAudioRouteLabel())
    }

    @Test
    fun testAudioRouteLabel_whenBluetoothDeviceNameIsBlank_returnsBluetoothAudio() {
        val btDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLE_HEADSET,
            productName = "   ",
        )
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDevice) },
        )

        assertTrue(router.isBluetoothAudioConnected())
        assertFalse(router.isUsingSpeaker())
        assertEquals("Bluetooth Audio", router.getConnectedDeviceName())
        assertEquals("Bluetooth Audio", router.getCurrentAudioRouteLabel())
    }

    @Test
    fun testMultiDeviceRemoval_updatesStateToRemainingDevice() {
        val device1 = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Galaxy Buds Pro",
            address = "AA:BB:CC:DD:EE:11",
        )
        val device2 = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLE_SPEAKER,
            productName = "JBL Speaker",
            address = "AA:BB:CC:DD:EE:22",
        )

        val currentDevices = mutableListOf(device1, device2)
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { currentDevices },
        )

        router.handleDevicesAdded(listOf(device1))
        val state1 = router.bluetoothState.value as BluetoothOutputState.Connected
        assertEquals("Galaxy Buds Pro", state1.deviceName)

        // Remove device 1 while device 2 remains
        currentDevices.remove(device1)
        router.handleDevicesRemoved(listOf(device1))

        assertTrue(router.isBluetoothAudioConnected())
        val state2 = router.bluetoothState.value as BluetoothOutputState.Connected
        assertEquals("JBL Speaker", state2.deviceName)
        assertEquals("JBL Speaker", router.getCurrentAudioRouteLabel())
    }

    @Test
    fun testAudioRouteLabel_whenNoBluetoothDeviceConnected_returnsWatchSpeaker() {
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { emptyList() },
        )

        assertFalse(router.isBluetoothAudioConnected())
        assertTrue(router.isUsingSpeaker())
        assertEquals("Watch Speaker", router.getCurrentAudioRouteLabel())
        assertNull(router.getConnectedDeviceName())
    }
}
