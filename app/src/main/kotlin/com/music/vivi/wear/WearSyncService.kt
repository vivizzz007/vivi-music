/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear

import android.content.Context
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class WearSyncService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_AUTH_REQUEST) {
            val sourceNodeId = messageEvent.sourceNodeId
            scope.launch {
                try {
                    val cookie = dataStore.get(InnerTubeCookieKey, "")
                    if (cookie.isNotBlank()) {
                        val visitorData = dataStore.get(VisitorDataKey, "")
                        val dataSyncId = dataStore.get(DataSyncIdKey, "")
                        val name = dataStore.get(AccountNameKey, "")
                        val email = dataStore.get(AccountEmailKey, "")

                        val json = JSONObject().apply {
                            put("cookie", cookie)
                            if (visitorData.isNotBlank()) put("visitorData", visitorData)
                            if (dataSyncId.isNotBlank()) put("dataSyncId", dataSyncId)
                            if (name.isNotBlank()) put("accountName", name)
                            if (email.isNotBlank()) put("accountEmail", email)
                        }.toString()

                        Tasks.await(
                            Wearable.getMessageClient(this@WearSyncService)
                                .sendMessage(sourceNodeId, PATH_AUTH_RESPONSE, json.toByteArray(Charsets.UTF_8)),
                            5,
                            TimeUnit.SECONDS
                        )
                        Timber.i("Sent auth credentials via Bluetooth to watch node: %s", sourceNodeId)
                    } else {
                        val errJson = JSONObject().apply {
                            put("error", "Not logged in on phone")
                        }.toString()
                        Tasks.await(
                            Wearable.getMessageClient(this@WearSyncService)
                                .sendMessage(sourceNodeId, PATH_AUTH_RESPONSE, errJson.toByteArray(Charsets.UTF_8)),
                            5,
                            TimeUnit.SECONDS
                        )
                        Timber.w("Watch requested auth via Bluetooth, but phone is not logged in")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error handling wear auth request via Bluetooth")
                }
            }
        }
    }

    companion object {
        const val PATH_AUTH_REQUEST = "/vivi/auth/request"
        const val PATH_AUTH_RESPONSE = "/vivi/auth/response"
        const val PATH_AUTH_PUSH = "/vivi/auth/push"

        suspend fun pushAuthToWatch(context: Context): Boolean {
            return try {
                val cookie = context.dataStore.get(InnerTubeCookieKey, "")
                if (cookie.isBlank()) return false
                val visitorData = context.dataStore.get(VisitorDataKey, "")
                val dataSyncId = context.dataStore.get(DataSyncIdKey, "")
                val name = context.dataStore.get(AccountNameKey, "")
                val email = context.dataStore.get(AccountEmailKey, "")

                val json = JSONObject().apply {
                    put("cookie", cookie)
                    if (visitorData.isNotBlank()) put("visitorData", visitorData)
                    if (dataSyncId.isNotBlank()) put("dataSyncId", dataSyncId)
                    if (name.isNotBlank()) put("accountName", name)
                    if (email.isNotBlank()) put("accountEmail", email)
                }.toString()

                val nodes: List<Node> = Tasks.await(Wearable.getNodeClient(context).connectedNodes, 5, TimeUnit.SECONDS)
                if (nodes.isEmpty()) return false
                val bytes = json.toByteArray(Charsets.UTF_8)
                val messageClient = Wearable.getMessageClient(context)
                var sentAny = false
                for (node in nodes) {
                    Tasks.await(messageClient.sendMessage(node.id, PATH_AUTH_PUSH, bytes), 5, TimeUnit.SECONDS)
                    sentAny = true
                    Timber.i("Pushed auth credentials to watch node: %s", node.displayName)
                }
                sentAny
            } catch (e: Exception) {
                Timber.w(e, "Failed to push auth to watch via Bluetooth")
                false
            }
        }
    }
}
