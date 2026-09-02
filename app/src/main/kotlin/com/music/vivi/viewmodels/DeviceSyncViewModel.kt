/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import com.music.vivi.devicesync.DeviceSyncManager
import com.music.vivi.sync.PlaybackSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin ViewModel wrapper over the [DeviceSyncManager] singleton, so the
 * "Devices" settings screen can observe and drive Android <-> desktop pairing.
 */
@HiltViewModel
class DeviceSyncViewModel @Inject constructor(
    private val manager: DeviceSyncManager,
) : ViewModel() {

    val paired: StateFlow<Boolean> = manager.paired
    val status: StateFlow<String> = manager.status
    val peerDeviceName: StateFlow<String> = manager.peerDeviceName
    val pendingPlayback: StateFlow<PlaybackSnapshot?> = manager.pendingPlayback

    fun createPairingCode() = manager.createPairingCode()

    fun joinPair(code: String) = manager.joinPair(code)

    fun unpair() = manager.unpair()

    fun saveServerUrl(value: String) = manager.saveServerUrl(value)
}
