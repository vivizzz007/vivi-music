/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.music.innertube.models.AccountInfo
import com.music.vivi.wear.WearApp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber

object WearBluetoothSyncManager {
    const val PATH_AUTH_REQUEST = "/vivi/auth/request"
    const val PATH_AUTH_RESPONSE = "/vivi/auth/response"
    const val PATH_AUTH_PUSH = "/vivi/auth/push"

    @Volatile
    private var pendingDeferred: CompletableDeferred<Result<AccountInfo>>? = null

    /**
     * Called when auth JSON data is received over Bluetooth from the phone.
     */
    suspend fun handleAuthPayload(jsonStr: String): Result<AccountInfo> {
        return try {
            val json = JSONObject(jsonStr)
            if (json.has("error")) {
                val err = json.getString("error")
                val result = Result.failure<AccountInfo>(IllegalStateException(err))
                pendingDeferred?.complete(result)
                return result
            }

            val cookie = json.optString("cookie")
            if (cookie.isBlank()) {
                val result = Result.failure<AccountInfo>(IllegalArgumentException("Empty cookie received from phone"))
                pendingDeferred?.complete(result)
                return result
            }

            val dataSyncId = json.optString("dataSyncId").takeIf { it.isNotBlank() }
            val visitorData = json.optString("visitorData").takeIf { it.isNotBlank() }
            val accountName = json.optString("accountName").takeIf { it.isNotBlank() }
            val accountEmail = json.optString("accountEmail").takeIf { it.isNotBlank() }

            val authManager = WearApp.instance.authManager
            val res = authManager.validateAndSaveSession(
                cookie = cookie,
                dataSyncId = dataSyncId,
                visitorData = visitorData,
                accountName = accountName,
                accountEmail = accountEmail,
            )
            pendingDeferred?.complete(res)
            res
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Bluetooth auth payload")
            val result = Result.failure<AccountInfo>(e)
            pendingDeferred?.complete(result)
            result
        }
    }

    /**
     * Queries connected phone nodes and sends a request for YouTube Music credentials.
     * Waits up to [timeoutMs] for the phone's response.
     */
    suspend fun requestSyncFromPhone(
        context: Context,
        timeoutMs: Long = 10000L,
    ): Result<AccountInfo> = withContext(Dispatchers.IO) {
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        val connectedNodes: List<Node> = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to query connected Bluetooth nodes")
            return@withContext Result.failure(Exception("Bluetooth service unavailable: ${e.message}"))
        }

        if (connectedNodes.isEmpty()) {
            return@withContext Result.failure(Exception("No connected phone found via Bluetooth. Please make sure Bluetooth is enabled and your watch is connected to your phone."))
        }

        val deferred = CompletableDeferred<Result<AccountInfo>>()
        pendingDeferred = deferred

        try {
            var sentAny = false
            for (node in connectedNodes) {
                try {
                    Tasks.await(messageClient.sendMessage(node.id, PATH_AUTH_REQUEST, ByteArray(0)))
                    sentAny = true
                    Timber.i("Sent auth sync request to node: %s", node.displayName)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to send auth request to node: %s", node.displayName)
                }
            }

            if (!sentAny) {
                return@withContext Result.failure(Exception("Failed to send message to connected phone."))
            }

            val result = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }

            result ?: Result.failure(Exception("Phone did not respond within ${timeoutMs / 1000}s. Make sure Vivi Music is installed and logged in on your phone."))
        } finally {
            if (pendingDeferred === deferred) {
                pendingDeferred = null
            }
        }
    }
}
