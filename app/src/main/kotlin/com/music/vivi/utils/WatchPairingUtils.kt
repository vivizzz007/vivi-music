/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import android.widget.Toast
import com.music.vivi.R
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.VisitorDataKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

object WatchPairingUtils {

    /**
     * Sends YouTube Music session credentials from the phone to the Wear OS watch pairing server.
     *
     * @param targetHost IP address or host:port of the watch (e.g. "192.168.1.142" or "192.168.1.142:8888")
     * @param targetPort Port of the watch pairing server (default 8888)
     * @param pin 4-digit pairing PIN shown on the watch
     * @param cookie InnerTubeCookie from the phone
     * @param visitorData Optional visitorData token
     * @param dataSyncId Optional dataSyncId token
     * @return true if credentials were accepted by the watch
     */
    suspend fun sendCredentialsToWatch(
        targetHost: String,
        targetPort: Int = 8888,
        pin: String,
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmedHost = targetHost.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")

        val actualHost = if (trimmedHost.contains(":")) trimmedHost.substringBefore(":") else trimmedHost
        val actualPort = if (trimmedHost.contains(":")) trimmedHost.substringAfter(":").toIntOrNull() ?: targetPort else targetPort

        try {
            val endpointUrl = URL("http://$actualHost:$actualPort/api/auth")
            val conn = endpointUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 6000
            conn.readTimeout = 12000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")

            val payload = JSONObject().apply {
                put("pin", pin.trim())
                put("cookie", cookie.trim())
                if (!visitorData.isNullOrBlank()) put("visitorData", visitorData.trim())
                if (!dataSyncId.isNullOrBlank()) put("dataSyncId", dataSyncId.trim())
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            Timber.i("Watch pairing HTTP response code: %d from %s:%d", code, actualHost, actualPort)
            code in 200..299
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to watch pairing server at %s:%d", actualHost, actualPort)
            false
        }
    }

    /**
     * Helper to grab credentials from phone DataStore and transmit to watch.
     */
    suspend fun pairWithWatch(
        context: Context,
        host: String,
        port: Int = 8888,
        pin: String,
        onResult: (Boolean) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val cookie = context.dataStore.get(InnerTubeCookieKey, "")
        val visitorData = context.dataStore.get(VisitorDataKey, "")
        val dataSyncId = context.dataStore.get(DataSyncIdKey, "")

        if (cookie.isBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.login_first_to_pair_watch, Toast.LENGTH_SHORT).show()
                onResult(false)
            }
            return@withContext
        }

        val success = sendCredentialsToWatch(
            targetHost = host,
            targetPort = port,
            pin = pin,
            cookie = cookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
        )

        withContext(Dispatchers.Main) {
            if (success) {
                Toast.makeText(context, R.string.watch_paired_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.watch_pairing_failed, Toast.LENGTH_SHORT).show()
            }
            onResult(success)
        }
    }
}
