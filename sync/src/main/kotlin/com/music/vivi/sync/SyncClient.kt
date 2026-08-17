/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min

enum class SyncConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

sealed class SyncEvent {
    data object Connected : SyncEvent()
    data object Disconnected : SyncEvent()
    data class PairCode(val code: String) : SyncEvent()
    data class Paired(val pairId: String, val peerDeviceId: String, val peerDeviceName: String) : SyncEvent()
    data class SnapshotReceived(val fromDeviceId: String, val snapshot: SyncSnapshot) : SyncEvent()

    /** Server confirmed the device is paired but the peer has not pushed a snapshot yet. */
    data object NoSnapshot : SyncEvent()
    data class Error(val message: String) : SyncEvent()
}

/**
 * WebSocket client for device pairing + snapshot sync.
 *
 * Pure JVM (OkHttp), so it runs unchanged on Android and on the Compose
 * Multiplatform desktop target. It only understands the wire protocol described
 * in `sync-server/README.md`; persistence of the device id and of the applied
 * state is the caller's responsibility.
 */
class SyncClient(
    val serverUrl: String,
    val deviceId: String = UUID.randomUUID().toString(),
    private val deviceName: String = "VIVI Music",
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(SyncConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectAttempts = 0
    private var manualClose = false

    private val pending = ArrayDeque<String>()

    /**
     * Estimated offset of this device's clock relative to the relay server's
     * clock (i.e. `localNow + offset ≈ serverNow`), measured from PING/PONG
     * round-trips and smoothed with an exponential moving average. Used to put
     * playback position timestamps in a shared reference frame so the two
     * devices stay in sync regardless of their local clock skew.
     */
    @Volatile
    private var serverOffsetMsEstimate = 0L

    /** True once a PING/PONG round-trip has measured the relay clock offset. */
    @Volatile
    private var offsetMeasured = false

    val serverOffsetMs: Long get() = serverOffsetMsEstimate

    /** Whether the relay clock offset has been measured (extrapolation is safe). */
    val hasServerOffset: Boolean get() = offsetMeasured

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _connectionState.value = SyncConnectionState.CONNECTED
            startPing()
            while (pending.isNotEmpty()) {
                webSocket.send(pending.removeFirst())
            }
            scope.launch { _events.emit(SyncEvent.Connected) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) = handle(text)

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) = handle(bytes.utf8())

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = handleDisconnect()

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = handleDisconnect()
    }

    fun connect() {
        if (_connectionState.value == SyncConnectionState.CONNECTED ||
            _connectionState.value == SyncConnectionState.CONNECTING
        ) return
        _connectionState.value = SyncConnectionState.CONNECTING
        doConnect()
    }

    fun disconnect() {
        manualClose = true
        pingJob?.cancel()
        pingJob = null
        webSocket?.close(1000, "bye")
        webSocket = null
        pending.clear()
        _connectionState.value = SyncConnectionState.DISCONNECTED
    }

    /** Ask the relay for a new pairing code (this device becomes the requester). */
    fun requestPairingCode() {
        send(
            SyncEnvelope(
                type = SyncMessageTypes.PAIR_REQUEST,
                deviceId = deviceId,
                deviceName = deviceName,
            )
        )
    }

    /** Join an existing pairing code shown by the other device. */
    fun joinPair(code: String) {
        send(
            SyncEnvelope(
                type = SyncMessageTypes.PAIR_JOIN,
                deviceId = deviceId,
                deviceName = deviceName,
                code = code.trim(),
            )
        )
    }

    /** Push the current local snapshot to the relay (forwarded to the peer, if online). */
    fun pushSnapshot(snapshot: SyncSnapshot) {
        send(
            SyncEnvelope(
                type = SyncMessageTypes.SYNC_PUSH,
                deviceId = deviceId,
                snapshot = snapshot,
            )
        )
    }

    /** Ask the relay for the peer's latest stored snapshot (mailbox). */
    fun pullSnapshot() {
        send(SyncEnvelope(type = SyncMessageTypes.SYNC_PULL, deviceId = deviceId))
    }

    fun unpair() {
        send(SyncEnvelope(type = SyncMessageTypes.UNPAIR, deviceId = deviceId))
    }

    private fun doConnect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = httpClient.newWebSocket(request, listener)
    }

    private fun send(envelope: SyncEnvelope) {
        val text = json.encodeToString(SyncEnvelope.serializer(), envelope)
        val ws = webSocket
        if (_connectionState.value == SyncConnectionState.CONNECTED && ws != null) {
            ws.send(text)
        } else {
            pending.addLast(text)
            if (_connectionState.value != SyncConnectionState.CONNECTING) connect()
        }
    }

    private fun handle(text: String) {
        val envelope = try {
            json.decodeFromString(SyncEnvelope.serializer(), text)
        } catch (e: Exception) {
            return
        }

        when (envelope.type) {
            SyncMessageTypes.PAIR_CODE -> {
                envelope.code?.let { scope.launch { _events.emit(SyncEvent.PairCode(it)) } }
            }
            SyncMessageTypes.PAIR_JOINED -> {
                scope.launch {
                    _events.emit(
                        SyncEvent.Paired(
                            pairId = envelope.pairId ?: "",
                            peerDeviceId = envelope.peerDeviceId ?: "",
                            peerDeviceName = envelope.peerDeviceName ?: "",
                        )
                    )
                }
            }
            SyncMessageTypes.PAIR_ERROR -> {
                envelope.message?.let { scope.launch { _events.emit(SyncEvent.Error(it)) } }
            }
            SyncMessageTypes.PONG -> {
                // Only the relay echoes the PING timestamps; older relays reply
                // with a bare `pong`, in which case the offset stays 0.
                val sentAt = envelope.echoTimestampMs
                val serverAt = envelope.timestampMs
                if (sentAt != null && serverAt != null) {
                    val rtt = System.currentTimeMillis() - sentAt
                    if (rtt >= 0) {
                        val offset = serverAt - (sentAt + rtt / 2)
                        // The first measurement sets the offset directly (the
                        // EMA starting from 0 only reaches a fraction of the
                        // true skew after one sample, so playback started right
                        // after pairing drifted and the two players fought).
                        serverOffsetMsEstimate = if (!offsetMeasured) {
                            offsetMeasured = true
                            offset
                        } else {
                            (serverOffsetMsEstimate * 3 + offset) / 4
                        }
                    }
                }
            }
            SyncMessageTypes.SYNC -> {
                envelope.snapshot?.let { snapshot ->
                    scope.launch {
                        _events.emit(SyncEvent.SnapshotReceived(envelope.fromDeviceId ?: "", snapshot))
                    }
                }
            }
            SyncMessageTypes.NO_SNAPSHOT -> {
                scope.launch { _events.emit(SyncEvent.NoSnapshot) }
            }
            else -> Unit
        }
    }

    private fun handleDisconnect() {
        if (_connectionState.value == SyncConnectionState.CONNECTED) {
            scope.launch { _events.emit(SyncEvent.Disconnected) }
        }
        pingJob?.cancel()
        pingJob = null
        if (manualClose) {
            _connectionState.value = SyncConnectionState.DISCONNECTED
            return
        }
        _connectionState.value = SyncConnectionState.ERROR
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        scope.launch {
            reconnectAttempts++
            val delayMs = min(2_000L * reconnectAttempts, 30_000L)
            delay(delayMs)
            if (!manualClose && _connectionState.value == SyncConnectionState.ERROR) {
                _connectionState.value = SyncConnectionState.CONNECTING
                doConnect()
            }
        }
    }

    private fun startPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            // First PING immediately so the clock offset converges right away.
            // The old code waited 25s for the first PING, so for the first
            // ~25s of a pairing the two devices extrapolated position from raw
            // local clocks and kept seeking each other back/forth by the skew.
            sendPing()
            while (true) {
                delay(25_000L)
                sendPing()
            }
        }
    }

    private fun sendPing() {
        val ws = webSocket
        if (_connectionState.value == SyncConnectionState.CONNECTED && ws != null) {
            ws.send(
                json.encodeToString(
                    SyncEnvelope.serializer(),
                    SyncEnvelope(
                        type = SyncMessageTypes.PING,
                        deviceId = deviceId,
                        timestampMs = System.currentTimeMillis(),
                    ),
                )
            )
        }
    }
}
