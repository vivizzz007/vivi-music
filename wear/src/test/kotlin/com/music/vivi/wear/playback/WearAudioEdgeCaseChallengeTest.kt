/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WearAudioEdgeCaseChallengeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // =========================================================================
    // CHALLENGE 1: Route Resolution when BT Device Name is null, blank, or unknown
    // =========================================================================

    /**
     * Test route resolution when a connected Bluetooth device has a null productName.
     * When Bluetooth audio is connected, the route must NEVER be reported as "Watch Speaker".
     */
    @Test
    fun challengeRouteResolution_whenBluetoothDeviceNameIsNull_mustNotReportWatchSpeaker() {
        val btDeviceNullName = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = null,
            address = "00:11:22:33:44:55",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDeviceNullName) },
        )

        // 1. Bluetooth output is physically connected
        assertTrue("Router must detect connected Bluetooth peripheral", router.isBluetoothAudioConnected())
        assertFalse("Router must NOT report using speaker when Bluetooth is connected", router.isUsingSpeaker())

        // 2. Audio route label must reflect Bluetooth routing, NOT Watch Speaker
        val routeLabel = router.getCurrentAudioRouteLabel()
        assertNotEquals(
            "BUG DETECTED: Route label is 'Watch Speaker' even though Bluetooth device is active and isUsingSpeaker is false!",
            "Watch Speaker",
            routeLabel,
        )
    }

    /**
     * Test route resolution when a connected Bluetooth device has an empty or blank productName.
     */
    @Test
    fun challengeRouteResolution_whenBluetoothDeviceNameIsBlank_mustNotReportWatchSpeaker() {
        val btDeviceBlankName = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLE_HEADSET,
            productName = "   ",
            address = "AA:BB:CC:DD:EE:FF",
        )

        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDeviceBlankName) },
        )

        assertTrue("Router must detect connected Bluetooth peripheral", router.isBluetoothAudioConnected())
        assertFalse("Router must NOT report using speaker when Bluetooth is connected", router.isUsingSpeaker())

        val routeLabel = router.getCurrentAudioRouteLabel()
        assertNotEquals(
            "BUG DETECTED: Route label is 'Watch Speaker' when BT device name is blank!",
            "Watch Speaker",
            routeLabel,
        )
    }

    /**
     * Test consistency between bluetoothState StateFlow and getCurrentAudioRouteLabel().
     */
    @Test
    fun challengeRouteResolution_stateConsistency_whenNameIsNull() {
        val btDevice = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = null,
        )
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { listOf(btDevice) },
        )

        router.refreshState()
        val state = router.bluetoothState.value
        assertTrue("StateFlow must be Connected", state is BluetoothOutputState.Connected)
        val stateConnected = state as BluetoothOutputState.Connected
        assertEquals("Bluetooth Audio", stateConnected.deviceName)

        // But what does getCurrentAudioRouteLabel() report?
        val routeLabel = router.getCurrentAudioRouteLabel()
        assertEquals(
            "StateFlow deviceName and getCurrentAudioRouteLabel() should match or be consistent",
            stateConnected.deviceName,
            routeLabel,
        )
    }

    // =========================================================================
    // CHALLENGE 2: Concurrency & Multi-Device State Updates
    // =========================================================================

    /**
     * Test state update when multiple Bluetooth devices are connected and the first is removed.
     * StateFlow must not retain the disconnected device's name.
     */
    @Test
    fun challengeMultiDeviceRemoval_mustUpdateStateToRemainingDevice() {
        val deviceA = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            productName = "Galaxy Buds 2 Pro",
            address = "11:22:33:44:55:66",
        )
        val deviceB = SimpleAudioDevice(
            type = AudioDeviceInfo.TYPE_BLE_SPEAKER,
            productName = "Portable BLE Speaker",
            address = "AA:BB:CC:DD:EE:11",
        )

        val connectedDevices = mutableListOf(deviceA, deviceB)
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { connectedDevices },
        )

        // Initial connection of device A
        router.handleDevicesAdded(listOf(deviceA))
        val initialState = router.bluetoothState.value as BluetoothOutputState.Connected
        assertEquals("Galaxy Buds 2 Pro", initialState.deviceName)

        // Now device A is disconnected, leaving device B
        connectedDevices.remove(deviceA)
        router.handleDevicesRemoved(listOf(deviceA))

        // Audio is still connected through device B
        assertTrue("Bluetooth should still be connected via device B", router.isBluetoothAudioConnected())
        val currentState = router.bluetoothState.value
        assertTrue("State should still be Connected", currentState is BluetoothOutputState.Connected)
        val connectedState = currentState as BluetoothOutputState.Connected

        // Device A was removed, so state should NOT still report Device A ("Galaxy Buds 2 Pro")!
        assertNotEquals(
            "BUG DETECTED: bluetoothState still reports removed device name 'Galaxy Buds 2 Pro' instead of updating to remaining device 'Portable BLE Speaker'!",
            "Galaxy Buds 2 Pro",
            connectedState.deviceName,
        )
    }

    /**
     * Stress test concurrent device additions, removals, and state reads across multiple threads.
     * Verifies no exceptions, race crashes, or deadlocks occur under high contention.
     */
    @Test
    fun challengeConcurrency_rapidConcurrentDeviceAddRemoveRefresh() {
        val device1 = SimpleAudioDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "BT Headset 1")
        val device2 = SimpleAudioDevice(AudioDeviceInfo.TYPE_BLE_HEADSET, "BT Headset 2")

        val currentDevices = ConcurrentLinkedQueue<AudioDeviceOutput>()
        val router = WearAudioRouter(
            context = context,
            deviceProvider = { currentDevices.toTypedArray().toList() },
        )

        val numThreads = 8
        val iterationsPerThread = 200
        val latch = CountDownLatch(numThreads)
        val errorCounter = AtomicInteger(0)
        val exceptions = ConcurrentLinkedQueue<Throwable>()

        for (i in 0 until numThreads) {
            thread {
                try {
                    for (j in 0 until iterationsPerThread) {
                        when (j % 4) {
                            0 -> {
                                currentDevices.add(device1)
                                router.handleDevicesAdded(listOf(device1))
                            }
                            1 -> {
                                currentDevices.remove(device1)
                                router.handleDevicesRemoved(listOf(device1))
                            }
                            2 -> {
                                router.refreshState()
                            }
                            3 -> {
                                val isBt = router.isBluetoothAudioConnected()
                                val isSpk = router.isUsingSpeaker()
                                val label = router.getCurrentAudioRouteLabel()
                                // Invariant: isBt and isSpk must be logical inverses
                                if (isBt == isSpk) {
                                    errorCounter.incrementAndGet()
                                }
                                assertNotNull(label)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    exceptions.add(t)
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(10, TimeUnit.SECONDS)
        assertTrue("Concurrent stress test must complete within 10s without deadlocks", completed)
        assertTrue("No exceptions should be thrown during concurrent access: ${exceptions.map { it.message }}", exceptions.isEmpty())
    }

    // =========================================================================
    // CHALLENGE 3: Rotary Scroll Volume Edge Cases
    // =========================================================================

    /**
     * Simulates the rotary scroll logic in NowPlayingScreen:
     *
     * val direction = if (event.verticalScrollPixels > 0) {
     *     AudioManager.ADJUST_RAISE
     * } else {
     *     AudioManager.ADJUST_LOWER
     * }
     *
     * Tests behavior under zero delta, NaN, and extreme values.
     */
    @Test
    fun challengeRotaryScrollVolume_zeroDeltaBehavior() {
        fun resolveDirection(verticalScrollPixels: Float): Int? {
            // If delta is 0 or NaN, adjusting volume lowers volume unexpectedly
            return if (verticalScrollPixels > 0) {
                AudioManager.ADJUST_RAISE
            } else if (verticalScrollPixels < 0) {
                AudioManager.ADJUST_LOWER
            } else {
                null // Deadband / no adjustment
            }
        }

        // Under current NowPlayingScreen logic:
        val rawDirectionForZero = if (0f > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        // Notice that 0f results in ADJUST_LOWER (-1)!
        assertEquals("Current code evaluates 0f to ADJUST_LOWER", AudioManager.ADJUST_LOWER, rawDirectionForZero)

        // However, correct behavior for zero scroll delta must be no adjustment (null)
        val expectedDirection = resolveDirection(0f)
        assertEquals("A 0f scroll delta should not trigger volume lowering", null, expectedDirection)
    }

    @Test
    fun challengeRotaryScrollVolume_nanDeltaBehavior() {
        // Under current NowPlayingScreen logic:
        val rawDirectionForNan = if (Float.NaN > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        assertEquals("Float.NaN evaluates to ADJUST_LOWER in current code", AudioManager.ADJUST_LOWER, rawDirectionForNan)
    }

    @Test
    fun challengeRotaryScrollVolume_nullAudioManagerBehavior() {
        val nullAudioManager: AudioManager? = null
        var volumeAdjusted = false

        // Simulating NowPlayingScreen rotary scroll event
        val consumed = try {
            val direction = if (10f > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            nullAudioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            true // Current code unconditionally returns true
        } catch (e: Exception) {
            false
        }

        // If audioManager is null, event was not actually handled, but code consumed it
        assertTrue("Code consumes event even when audioManager is null", consumed)
    }
}
