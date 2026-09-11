/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder

/**
 * Metadata descriptor for an active pairing server session.
 */
data class PairingServerInfo(
    val ip: String,
    val port: Int,
    val pin: String,
    val pairingUrl: String,
)

/**
 * Lightweight local HTTP server embedded in the Wear OS app for phone-to-watch auth pairing.
 *
 * Supported endpoints:
 * - `GET /pair?pin=XXXX` or `GET /` — Serves an interactive HTML pairing page with form fallback & deep links.
 * - `POST /api/auth` — Receives credentials payload (JSON or Form URL-encoded) and saves the session.
 * - `GET /api/auth?cookie=...&pin=...` — GET-based pairing endpoint for one-tap intents and deep links.
 * - `OPTIONS *` — CORS preflight for universal mobile browser support.
 */
class WearPairingServer(
    private val context: Context,
    private val onCredentialsReceived: suspend (cookie: String, dataSyncId: String?, visitorData: String?) -> Unit = { _, _, _ -> },
) {
    companion object {
        private const val TAG = "WearPairingServer"
        const val DEFAULT_PORT = 8888
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /** 4-digit pairing PIN generated via [WearAuthUtils]. */
    var pin: String = WearAuthUtils.generatePairingPin()
        private set

    /** Active server metadata, or null if stopped. */
    var pairingInfo: PairingServerInfo? = null
        private set

    /** Full URL encoded in the QR code after [start] is called. */
    val localUrl: String
        get() = pairingInfo?.pairingUrl ?: ""

    /** Active port or 0 if stopped. */
    val port: Int
        get() = pairingInfo?.port ?: 0

    /**
     * Starts the pairing HTTP server.
     *
     * @param port Target port (default 8888). Falls back to random ephemeral port if unavailable.
     * @param callback Optional credentials callback overriding constructor callback.
     * @return [PairingServerInfo] containing active IP, port, PIN, and URL.
     */
    fun start(
        port: Int = DEFAULT_PORT,
        callback: (suspend (cookie: String, dataSyncId: String?, visitorData: String?) -> Unit)? = null,
    ): PairingServerInfo {
        stop()

        pin = WearAuthUtils.generatePairingPin()
        val ss = try {
            if (port > 0) ServerSocket(port) else ServerSocket(0)
        } catch (_: Exception) {
            ServerSocket(0)
        }
        serverSocket = ss
        val boundPort = ss.localPort
        val ip = WearAuthUtils.getLocalIpAddress(context) ?: "127.0.0.1"
        val url = "http://$ip:$boundPort/pair?pin=$pin"

        val info = PairingServerInfo(
            ip = ip,
            port = boundPort,
            pin = pin,
            pairingUrl = url,
        )
        pairingInfo = info
        Log.i(TAG, "Pairing server listening at $url (PIN: $pin)")

        val activeCallback = callback ?: onCredentialsReceived
        serverJob = scope.launch {
            try {
                while (true) {
                    val client = ss.accept()
                    launch { handleClient(client, activeCallback) }
                }
            } catch (_: SocketException) {
                // Expected on stop()
            } catch (e: Exception) {
                Log.w(TAG, "Server loop exception: ${e.message}")
            }
        }

        return info
    }

    /**
     * Stops the pairing server and cleans up resources.
     */
    fun stop() {
        try {
            serverJob?.cancel()
            serverSocket?.close()
        } catch (_: Exception) {
        } finally {
            serverJob = null
            serverSocket = null
            pairingInfo = null
            Log.i(TAG, "Pairing server stopped")
        }
    }

    private suspend fun handleClient(
        socket: Socket,
        callback: (suspend (cookie: String, dataSyncId: String?, visitorData: String?) -> Unit)?,
    ) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.inputStream, Charsets.UTF_8))
                val writer = PrintWriter(s.outputStream, true)

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                val method = parts.getOrElse(0) { "GET" }
                val path = parts.getOrElse(1) { "/" }

                var contentLength = 0
                var contentType = ""
                var acceptHeader = ""

                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) break
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        val headerName = line.substring(0, colon).trim().lowercase()
                        val headerVal = line.substring(colon + 1).trim()
                        if (headerName == "content-length") {
                            contentLength = headerVal.toIntOrNull() ?: 0
                        } else if (headerName == "content-type") {
                            contentType = headerVal.lowercase()
                        } else if (headerName == "accept") {
                            acceptHeader = headerVal.lowercase()
                        }
                    }
                }

                if (method == "OPTIONS") {
                    sendCorsPreflight(writer)
                    return
                }

                when {
                    method == "POST" && path.startsWith("/api/auth") -> {
                        val body = CharArray(contentLength.coerceIn(0, 131072))
                        var totalRead = 0
                        while (totalRead < contentLength && totalRead < body.size) {
                            val count = reader.read(body, totalRead, contentLength - totalRead)
                            if (count == -1) break
                            totalRead += count
                        }
                        val bodyStr = String(body, 0, totalRead)
                        val isFormPost = contentType.contains("application/x-www-form-urlencoded")
                        val isHtmlPreferred = acceptHeader.contains("text/html") && !acceptHeader.contains("application/json")

                        handleAuthPost(bodyStr, isFormPost, isHtmlPreferred, writer, callback)
                    }
                    method == "GET" && path.startsWith("/api/auth") -> {
                        val params = parseQuery(path.substringAfter("?", ""))
                        val reqPin = params["pin"] ?: ""
                        val cookie = params["cookie"] ?: ""
                        if (reqPin.isNotBlank() && reqPin != pin) {
                            sendJson(writer, 401, """{"error":"Invalid pairing PIN"}""")
                        } else if (cookie.isBlank()) {
                            sendJson(writer, 400, """{"error":"Missing cookie"}""")
                        } else {
                            val dataSyncId = params["dataSyncId"]?.takeIf { it != "null" && it.isNotBlank() }
                            val visitorData = params["visitorData"]?.takeIf { it != "null" && it.isNotBlank() }
                            callback?.invoke(cookie, dataSyncId, visitorData)
                            sendJson(writer, 200, """{"status":"ok"}""")
                        }
                    }
                    method == "GET" && (path.startsWith("/pair") || path == "/") -> {
                        sendHtml(writer, buildPairingPage())
                    }
                    else -> sendJson(writer, 404, """{"error":"not found"}""")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client handling exception: ${e.message}")
        }
    }

    private suspend fun handleAuthPost(
        body: String,
        isForm: Boolean,
        isHtmlPreferred: Boolean,
        writer: PrintWriter,
        callback: (suspend (cookie: String, dataSyncId: String?, visitorData: String?) -> Unit)?,
    ) {
        var cookie = ""
        var reqPin = ""
        var visitorData: String? = null
        var dataSyncId: String? = null

        try {
            val trimmed = body.trim()
            if (trimmed.startsWith("[") && !isForm) {
                sendJson(writer, 401, """{"error":"Invalid pairing PIN"}""")
                return
            }

            if (!isForm && trimmed.startsWith("{")) {
                val json = Json.parseToJsonElement(trimmed) as? JsonObject
                if (json == null) {
                    sendJson(writer, 400, """{"error":"Malformed JSON"}""")
                    return
                }
                reqPin = json["pin"]?.jsonPrimitive?.content?.trim() ?: ""
                cookie = json["cookie"]?.jsonPrimitive?.content?.trim() ?: ""
                visitorData = (json["visitorData"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.trim()
                dataSyncId = (json["dataSyncId"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.trim()
            } else if (isForm || trimmed.contains("=") && !trimmed.startsWith("{")) {
                val params = parseQuery(trimmed)
                reqPin = params["pin"]?.trim() ?: ""
                cookie = params["cookie"]?.trim() ?: ""
                visitorData = params["visitorData"]?.takeIf { it.isNotBlank() }
                dataSyncId = params["dataSyncId"]?.takeIf { it.isNotBlank() }
            } else {
                sendJson(writer, 400, """{"error":"Malformed payload"}""")
                return
            }

            if (reqPin != pin) {
                if (isHtmlPreferred) {
                    sendHtml(writer, buildResultHtml(success = false, message = "Invalid Pairing PIN. Check the PIN shown on your watch."))
                } else {
                    sendJson(writer, 401, """{"error":"Invalid pairing PIN"}""")
                }
                return
            }

            if (cookie.isBlank()) {
                if (isHtmlPreferred) {
                    sendHtml(writer, buildResultHtml(success = false, message = "Cookie cannot be empty."))
                } else {
                    sendJson(writer, 400, """{"error":"Cookie cannot be empty"}""")
                }
                return
            }

            callback?.invoke(cookie, dataSyncId, visitorData)

            if (isHtmlPreferred) {
                sendHtml(writer, buildResultHtml(success = true, message = "✓ Watch paired successfully! You can now use your watch."))
            } else {
                sendJson(writer, 200, """{"status":"ok"}""")
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Invalid request"
            if (isHtmlPreferred) {
                sendHtml(writer, buildResultHtml(success = false, message = "Error: $errMsg"))
            } else {
                sendJson(writer, 400, """{"error":"$errMsg"}""")
            }
        }
    }

    private fun sendCorsPreflight(writer: PrintWriter) {
        writer.apply {
            println("HTTP/1.1 204 No Content")
            println("Access-Control-Allow-Origin: *")
            println("Access-Control-Allow-Methods: GET, POST, OPTIONS")
            println("Access-Control-Allow-Headers: Content-Type, Accept, Authorization")
            println("Connection: close")
            println()
            flush()
        }
    }

    private fun sendHtml(writer: PrintWriter, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        writer.apply {
            println("HTTP/1.1 200 OK")
            println("Content-Type: text/html; charset=utf-8")
            println("Content-Length: ${bytes.size}")
            println("Access-Control-Allow-Origin: *")
            println("Access-Control-Allow-Methods: GET, POST, OPTIONS")
            println("Access-Control-Allow-Headers: *")
            println("Connection: close")
            println()
            print(html)
            flush()
        }
    }

    private fun sendJson(writer: PrintWriter, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        writer.apply {
            val statusText = when (code) {
                200 -> "OK"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                404 -> "Not Found"
                else -> "Error"
            }
            println("HTTP/1.1 $code $statusText")
            println("Content-Type: application/json; charset=utf-8")
            println("Content-Length: ${bytes.size}")
            println("Access-Control-Allow-Origin: *")
            println("Access-Control-Allow-Methods: GET, POST, OPTIONS")
            println("Access-Control-Allow-Headers: *")
            println("Connection: close")
            println()
            print(json)
            flush()
        }
    }

    private fun buildResultHtml(success: Boolean, message: String): String {
        val color = if (success) "#03DAC6" else "#CF6679"
        val title = if (success) "Pairing Successful" else "Pairing Failed"
        return """<!DOCTYPE html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Vivi Music – $title</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;background:#000;color:#fff;max-width:480px;margin:0 auto;padding:32px 20px;text-align:center}
h1{color:#BB86FC;font-size:1.5rem}
.card{background:#1C1B1F;border:1px solid #49454F;border-radius:16px;padding:24px;margin:20px 0}
.msg{color:$color;font-size:1.2rem;font-weight:bold;line-height:1.5}
a.btn{display:inline-block;background:#BB86FC;color:#000;text-decoration:none;padding:12px 24px;border-radius:24px;font-weight:bold;margin-top:20px}
</style></head>
<body>
<h1>&#127925; Vivi Music</h1>
<div class="card">
<div class="msg">$message</div>
</div>
<a class="btn" href="/pair?pin=$pin">Back to Pairing Page</a>
</body></html>"""
    }

    private fun buildPairingPage(): String {
        val ip = pairingInfo?.ip ?: "127.0.0.1"
        val activePort = port
        val deepLink = "vivimusic://pair?host=$ip&port=$activePort&pin=$pin"

        return """<!DOCTYPE html>
<html><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Vivi Music – Watch Pairing</title>
<style>
  body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;background:#000;color:#fff;max-width:480px;margin:0 auto;padding:20px;text-align:center;box-sizing:border-box}
  h1{color:#BB86FC;margin-bottom:4px;font-size:1.6rem}
  h2{color:#E6E1E5;margin-top:0;font-size:1.1rem;font-weight:normal}
  p{color:#CAC4D0;font-size:.9rem;line-height:1.4}
  .pin-card{background:#1C1B1F;border:1px solid #49454F;border-radius:16px;padding:16px;margin:16px 0}
  .pin-title{font-size:.8rem;letter-spacing:.1rem;color:#938F99;text-transform:uppercase}
  .pin{font-size:2.4rem;letter-spacing:.4rem;color:#03DAC6;font-weight:bold;margin:8px 0}
  .btn-app{display:block;background:#03DAC6;color:#000;text-decoration:none;padding:14px 20px;border-radius:24px;font-weight:bold;font-size:1rem;margin:16px 0}
  .btn-app:hover{background:#70efde}
  .divider{display:flex;align-items:center;color:#79747E;font-size:.8rem;margin:20px 0}
  .divider::before,.divider::after{content:'';flex:1;border-bottom:1px solid #49454F;margin:0 8px}
  label{display:block;text-align:left;margin-top:12px;color:#CAC4D0;font-size:.85rem}
  input,textarea{width:100%;box-sizing:border-box;padding:12px;margin-top:6px;background:#1C1B1F;border:1px solid #49454F;color:#fff;border-radius:8px;font-size:.9rem}
  textarea{height:80px;resize:vertical}
  button{width:100%;background:#BB86FC;color:#000;border:none;padding:14px;border-radius:24px;font-size:1rem;font-weight:bold;cursor:pointer;margin-top:20px}
  button:hover{background:#d7b7fd}
  .status{margin-top:16px;font-size:.95rem;min-height:24px}
  .ok{color:#03DAC6;font-weight:bold} .err{color:#CF6679;font-weight:bold}
</style>
</head>
<body>
<h1>&#127925; Vivi Music</h1>
<h2>Watch Pairing</h2>
<p>Transfer your YouTube Music credentials to your smartwatch.</p>

<div class="pin-card">
  <div class="pin-title">Watch Pairing PIN</div>
  <div class="pin">$pin</div>
</div>

<a class="btn-app" href="$deepLink">&#128241; One-Tap Pair via Vivi Music App</a>

<div class="divider">OR PASTE SESSION CREDENTIALS</div>

<form method="POST" action="/api/auth" onsubmit="return submitPairing(event)">
  <label>Pairing PIN</label>
  <input type="text" name="pin" id="pin" value="$pin" maxlength="4" style="letter-spacing:0.2rem;font-weight:bold;">

  <label>InnerTube Cookie *</label>
  <textarea name="cookie" id="cookie" placeholder="Paste your InnerTubeCookie here..."></textarea>

  <label>VisitorData <span style="color:#79747E">(optional)</span></label>
  <input type="text" name="visitorData" id="visitorData" placeholder="visitorData token">

  <label>DataSyncId <span style="color:#79747E">(optional)</span></label>
  <input type="text" name="dataSyncId" id="dataSyncId" placeholder="dataSyncId token">

  <button type="submit">Pair with Watch</button>
</form>
<div class="status" id="status"></div>

<script>
async function submitPairing(event) {
  const pin = document.getElementById('pin').value.trim();
  const cookie = document.getElementById('cookie').value.trim();
  const visitorData = document.getElementById('visitorData').value.trim();
  const dataSyncId = document.getElementById('dataSyncId').value.trim();
  const statusEl = document.getElementById('status');

  if (!pin) {
    statusEl.innerHTML = '<span class="err">&#10007; PIN is required.</span>';
    event.preventDefault();
    return false;
  }
  if (!cookie) {
    statusEl.innerHTML = '<span class="err">&#10007; InnerTube Cookie is required.</span>';
    event.preventDefault();
    return false;
  }

  statusEl.textContent = 'Pairing with watch…';

  try {
    const res = await fetch('/api/auth', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({ pin, cookie, visitorData, dataSyncId })
    });
    const data = await res.json();
    if (res.ok && data.status === 'ok') {
      statusEl.innerHTML = '<span class="ok">&#10003; Watch paired successfully! You can now use your watch.</span>';
      event.preventDefault();
      return false;
    } else {
      statusEl.innerHTML = '<span class="err">&#10007; ' + (data.error || 'Pairing failed') + '</span>';
      event.preventDefault();
      return false;
    }
  } catch (err) {
    // If AJAX fetch fails due to browser restrictions, allow standard form submission fallback!
    statusEl.textContent = 'Retrying via standard HTTP post…';
    return true; // proceed with standard form POST
  }
}
</script>
</body></html>"""
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull { kv ->
            val eq = kv.indexOf('=')
            if (eq < 0) null
            else {
                val key = try { URLDecoder.decode(kv.substring(0, eq), "UTF-8") } catch (_: Exception) { kv.substring(0, eq) }
                val value = try { URLDecoder.decode(kv.substring(eq + 1), "UTF-8") } catch (_: Exception) { kv.substring(eq + 1) }
                key to value
            }
        }.toMap()
    }
}
