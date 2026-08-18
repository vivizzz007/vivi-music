package com.music.vivi.desktop

import com.music.vivi.sync.SyncEnvelope
import com.music.vivi.sync.SyncMessageTypes
import com.music.vivi.sync.SyncSnapshot
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.random.Random

/**
 * Local, in-process WebSocket relay for offline (same Wi-Fi / LAN) device
 * pairing. It speaks the exact same `SyncEnvelope` protocol as
 * `sync-server/server.js`, so the existing `SyncClient` works unchanged:
 * the desktop connects to `ws://localhost:<port>` and the phone connects to
 * `ws://<desktop-lan-ip>:<port>`.
 *
 * State is in-memory only: a LAN relay lives while the desktop app is running,
 * which is enough because both peers must be online at the same time.
 */
class LanSyncRelay {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // deviceId -> live WebSocket session
    private val sockets = ConcurrentHashMap<String, WebSocketSession>()
    // deviceId -> device name (from the last envelope that carried one)
    private val deviceNames = ConcurrentHashMap<String, String>()
    // deviceId -> pairId ("" when unpaired)
    private val devicePair = ConcurrentHashMap<String, String>()
    // pairId -> (requester, joiner)
    private val pairs = ConcurrentHashMap<String, Pair<String, String>>()
    // deviceId -> last pushed snapshot (mailbox)
    private val mailboxes = ConcurrentHashMap<String, SyncSnapshot>()
    // code -> pending pair request
    private val pending = ConcurrentHashMap<String, PendingPair>()
    // deviceId -> pending grace-period unpair job (cancelled if it reconnects)
    private val pendingDisconnects = ConcurrentHashMap<String, Job>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Serializes start/stop so a rapid "Stop → Start" can't interleave and corrupt state. */
    private val startStopMutex = Mutex()

    private var server: EmbeddedServer<*, *>? = null
    private var jmdns: JmDNS? = null
    private var serviceInfo: ServiceInfo? = null

    @Volatile
    var port: Int = 0
        private set

    private data class PendingPair(
        val pairId: String,
        val deviceId: String,
        val deviceName: String,
        val createdAt: Long,
    )

    fun isRunning(): Boolean = server != null

    /** Starts the relay on [requestedPort] (0 = ephemeral). Returns the bound port. */
    suspend fun start(requestedPort: Int = 0): Int = startStopMutex.withLock {
        server?.let { return@withLock port }
        val created = embeddedServer(Netty, port = requestedPort, host = "0.0.0.0") {
            install(WebSockets)
            routing {
                webSocket("/") { handleSession(this) }
            }
        }
        created.start(wait = false)
        server = created
        // `resolvedConnectors()` can briefly be empty right after a non-blocking
        // start; treat that (or a failed bind) as an error instead of letting
        // `.first()` throw NoSuchElementException and crash the app.
        val bound = runCatching { created.engine.resolvedConnectors().first().port }.getOrNull() ?: 0
        if (bound <= 0) {
            runCatching { created.stop(100, 100) }
            server = null
            error("LAN relay failed to bind a port")
        }
        port = bound
        startMdns(bound)
        bound
    }

    suspend fun stop() = startStopMutex.withLock {
        val s = server ?: return@withLock
        // Tell any connected peers they are no longer paired before the socket
        // closes, so the phone clears its local pairing state.
        broadcastUnpaired()
        server = null
        sockets.clear()
        deviceNames.clear()
        devicePair.clear()
        pairs.clear()
        mailboxes.clear()
        pending.clear()
        pendingDisconnects.values.forEach { it.cancel() }
        pendingDisconnects.clear()
        runCatching { s.stop(500, 1000) }
        stopMdns()
        port = 0
    }

    private suspend fun broadcastUnpaired() {
        val envelope = SyncEnvelope(type = SyncMessageTypes.PAIR_ERROR, message = "Device was unpaired")
        for (session in sockets.values) {
            send(session, envelope)
        }
    }

    /**
     * Best-effort notification for JVM shutdown (desktop window closed). Runs on
     * the shutdown-hook thread, so it blocks until the frames are flushed or fail.
     */
    fun shutdownNotify() {
        if (server == null) return
        val sessions = sockets.values.toList()
        if (sessions.isEmpty()) return
        val text = json.encodeToString(
            SyncEnvelope.serializer(),
            SyncEnvelope(type = SyncMessageTypes.PAIR_ERROR, message = "Device was unpaired"),
        )
        runBlocking {
            sessions.forEach { session -> runCatching { session.send(Frame.Text(text)) } }
        }
    }

    private suspend fun handleSession(session: WebSocketSession) {
        var deviceId: String? = null
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                val envelope = runCatching { json.decodeFromString<SyncEnvelope>(frame.readText()) }.getOrNull() ?: continue
                val id = envelope.deviceId ?: continue
                deviceId = id
                sockets[id] = session
                // The device is (back) online: cancel any pending unpair.
                pendingDisconnects.remove(id)?.cancel()
                devicePair.putIfAbsent(id, "")
                envelope.deviceName?.takeIf { it.isNotBlank() }?.let { deviceNames[id] = it }
                handle(envelope, session)
            }
        } catch (_: Exception) {
            // connection closed — fall through to cleanup
        } finally {
            deviceId?.let { id -> onSocketClosed(id, session) }
        }
    }

    private suspend fun handle(msg: SyncEnvelope, session: WebSocketSession) {
        when (msg.type) {
            SyncMessageTypes.PING -> send(
                session,
                SyncEnvelope(
                    type = SyncMessageTypes.PONG,
                    timestampMs = System.currentTimeMillis(),
                    echoTimestampMs = msg.timestampMs,
                ),
            )
            SyncMessageTypes.PAIR_REQUEST -> handlePairRequest(session, msg)
            SyncMessageTypes.PAIR_JOIN -> handlePairJoin(session, msg)
            SyncMessageTypes.SYNC_PUSH -> handleSyncPush(msg)
            SyncMessageTypes.SYNC_PULL -> handleSyncPull(session, msg)
            SyncMessageTypes.UNPAIR -> handleUnpair(msg)
            else -> send(session, SyncEnvelope(type = SyncMessageTypes.PAIR_ERROR, message = "Unknown message type"))
        }
    }

    private suspend fun handlePairRequest(session: WebSocketSession, msg: SyncEnvelope) {
        val deviceId = msg.deviceId ?: return
        val code = generateCode()
        val pairId = UUID.randomUUID().toString()
        pending[code] = PendingPair(pairId, deviceId, msg.deviceName ?: "Device", System.currentTimeMillis())
        send(session, SyncEnvelope(type = SyncMessageTypes.PAIR_CODE, code = code, pairId = pairId))
    }

    private suspend fun handlePairJoin(session: WebSocketSession, msg: SyncEnvelope) {
        val deviceId = msg.deviceId ?: return
        val code = msg.code ?: return sendPairError(session, "Invalid or expired code")
        val p = pending.remove(code)
        if (p == null || System.currentTimeMillis() - p.createdAt > PAIR_CODE_TTL_MS) {
            return sendPairError(session, "Invalid or expired code")
        }
        if (p.deviceId == deviceId) {
            return sendPairError(session, "Cannot pair with yourself")
        }

        val requester = p.deviceId
        val joiner = deviceId

        // A device belongs to exactly one pair: break any previous pairings.
        for (id in listOf(requester, joiner)) {
            val old = devicePair[id]
            if (old != null && old.isNotEmpty()) pairs.remove(old)
        }

        val pairId = p.pairId
        pairs[pairId] = requester to joiner
        devicePair[requester] = pairId
        devicePair[joiner] = pairId

        sockets[requester]?.let {
            send(it, SyncEnvelope(type = SyncMessageTypes.PAIR_JOINED, pairId = pairId, peerDeviceId = joiner, peerDeviceName = deviceName(joiner)))
        }
        send(session, SyncEnvelope(type = SyncMessageTypes.PAIR_JOINED, pairId = pairId, peerDeviceId = requester, peerDeviceName = deviceName(requester)))
    }

    private suspend fun handleSyncPush(msg: SyncEnvelope) {
        val deviceId = msg.deviceId ?: return
        val snapshot = msg.snapshot ?: return
        mailboxes[deviceId] = snapshot
        val peer = peerOf(deviceId) ?: return
        sockets[peer]?.let { send(it, SyncEnvelope(type = SyncMessageTypes.SYNC, fromDeviceId = deviceId, snapshot = snapshot)) }
    }

    private suspend fun handleSyncPull(session: WebSocketSession, msg: SyncEnvelope) {
        val deviceId = msg.deviceId ?: return
        val peer = peerOf(deviceId) ?: return sendPairError(session, "Not paired")
        val snapshot = mailboxes[peer]
        if (snapshot != null) {
            send(session, SyncEnvelope(type = SyncMessageTypes.SYNC, fromDeviceId = peer, snapshot = snapshot))
        } else {
            send(session, SyncEnvelope(type = SyncMessageTypes.NO_SNAPSHOT))
        }
    }

    private suspend fun handleUnpair(msg: SyncEnvelope) {
        val deviceId = msg.deviceId ?: return
        val pairId = devicePair[deviceId] ?: return
        if (pairId.isEmpty()) return
        val pair = pairs.remove(pairId) ?: return
        val peer = if (pair.first == deviceId) pair.second else pair.first
        devicePair[deviceId] = ""
        devicePair[peer] = ""
        mailboxes.remove(deviceId)
        mailboxes.remove(peer)
        sockets[peer]?.let { send(it, SyncEnvelope(type = SyncMessageTypes.PAIR_ERROR, message = "Device was unpaired")) }
    }

    /**
     * A device's socket closed (e.g. the app was closed). Give it a short
     * grace period to reconnect before unpairing, so a transient network blip
     * doesn't tear down a healthy pairing. Only the device's live socket
     * matters: if a newer socket already replaced this one, do nothing.
     */
    private fun onSocketClosed(deviceId: String, session: WebSocketSession) {
        if (sockets[deviceId] !== session) return
        sockets.remove(deviceId)
        val job = scope.launch {
            delay(UNPAIR_GRACE_MS)
            if (sockets.containsKey(deviceId)) return@launch // reconnected
            handleDisconnect(deviceId)
        }
        pendingDisconnects[deviceId] = job
    }

    /** Clears the pair for [deviceId] and notifies the still-connected peer. */
    private suspend fun handleDisconnect(deviceId: String) {
        val pairId = devicePair[deviceId] ?: return
        if (pairId.isEmpty()) return
        val pair = pairs.remove(pairId) ?: return
        val peer = if (pair.first == deviceId) pair.second else pair.first
        devicePair[deviceId] = ""
        devicePair[peer] = ""
        mailboxes.remove(deviceId)
        mailboxes.remove(peer)
        sockets[peer]?.let { send(it, SyncEnvelope(type = SyncMessageTypes.PAIR_ERROR, message = "Device was unpaired")) }
    }

    private suspend fun startMdns(port: Int) {
        stopMdns()
        withContext(Dispatchers.IO) {
            runCatching {
                val ip = lanIpAddress()
                if (ip == "127.0.0.1") return@runCatching
                val j = JmDNS.create(InetAddress.getByName(ip))
                val s = ServiceInfo.create(MDNS_TYPE, MDNS_NAME, port, "VIVI Music DE")
                j.registerService(s)
                jmdns = j
                serviceInfo = s
            }
        }
    }

    private fun stopMdns() {
        runCatching { serviceInfo?.let { jmdns?.unregisterService(it) } }
        runCatching { jmdns?.close() }
        jmdns = null
        serviceInfo = null
    }

    private fun peerOf(deviceId: String): String? {
        val pairId = devicePair[deviceId] ?: return null
        if (pairId.isEmpty()) return null
        val pair = pairs[pairId] ?: return null
        return if (pair.first == deviceId) pair.second else pair.first
    }

    private fun deviceName(deviceId: String): String = deviceNames[deviceId] ?: "Device"

    private fun generateCode(): String {
        var code: String
        do {
            code = (100000 + Random.nextInt(900000)).toString()
        } while (pending.containsKey(code))
        return code
    }

    private suspend fun sendPairError(session: WebSocketSession, message: String) {
        send(session, SyncEnvelope(type = SyncMessageTypes.PAIR_ERROR, message = message))
    }

    private suspend fun send(session: WebSocketSession, envelope: SyncEnvelope) {
        runCatching {
            session.send(Frame.Text(json.encodeToString(SyncEnvelope.serializer(), envelope)))
        }
    }

    companion object {
        private const val PAIR_CODE_TTL_MS = 5 * 60 * 1000L

        /** How long to wait for a reconnect before unpairing a dropped socket. */
        private const val UNPAIR_GRACE_MS = 15_000L

        /** mDNS service type advertised so the phone can discover this relay. */
        const val MDNS_TYPE = "_vivimusic._tcp.local."
        const val MDNS_NAME = "VIVI Music DE"
    }
}

/**
 * Best-effort site-local IPv4 address of this machine that the phone can reach
 * (shown to the phone / used by mDNS).
 *
 * When the computer is connected to the phone's hotspot it is often multi-homed
 * (Wi-Fi + Ethernet + virtual adapters like VMware/Hyper-V). Picking "the first
 * site-local address" can therefore return a virtual adapter the phone can't
 * dial. We instead resolve the source address of the route to the outside
 * world: a UDP `connect` only resolves the route (no packets are sent), so it
 * returns the interface actually facing the hotspot/router even while offline.
 */
fun lanIpAddress(): String {
    runCatching {
        DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName("8.8.8.8"), 9)
            val local = socket.localAddress
            if (local is Inet4Address && !local.isLoopbackAddress && local.isSiteLocalAddress) {
                return local.hostAddress
            }
        }
    }
    // Fallback: prefer wireless/wlan interfaces, then any site-local IPv4.
    return runCatching {
        val interfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
        val wireless = interfaces.filter {
            val n = it.name.lowercase()
            n.contains("wlan") || n.contains("wi-fi") || n.contains("wireless") ||
                n.startsWith("wl") || n == "en0" || n == "en1"
        }
        (wireless + interfaces).flatMap { Collections.list(it.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull() ?: "127.0.0.1"
}
