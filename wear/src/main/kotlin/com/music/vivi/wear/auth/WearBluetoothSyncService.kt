/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Service that receives Bluetooth auth payloads pushed or responded by the companion phone.
 */
class WearBluetoothSyncService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        if (path == WearBluetoothSyncManager.PATH_AUTH_RESPONSE || path == WearBluetoothSyncManager.PATH_AUTH_PUSH) {
            val jsonStr = String(messageEvent.data, Charsets.UTF_8)
            Timber.i("Received auth payload via Bluetooth from node %s", messageEvent.sourceNodeId)
            scope.launch {
                WearBluetoothSyncManager.handleAuthPayload(jsonStr)
            }
        }
    }
}
