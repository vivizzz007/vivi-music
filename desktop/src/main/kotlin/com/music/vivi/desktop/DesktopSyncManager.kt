package com.music.vivi.desktop

import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncClient
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncEvent
import com.music.vivi.sync.SyncSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Persistent device-sync bridge for the desktop edition.
 *
 * Unlike the previous ad-hoc pairing UI (which lived inside the Settings
 * screen and was torn down on navigation), this manager lives for the whole
 * app lifetime and owns:
 *
 *  - the [SyncClient] (cloud relay) and the [LanSyncRelay] (same-Wi-Fi),
 *  - the pairing state (device id / pair id, persisted in [DesktopSettings]),
 *  - pushing the current playback + settings snapshot on change,
 *  - applying incoming snapshots (playback is forwarded to the player,
 *    settings are forwarded to the theme/language layer).
 *
 * Echo suppression: applying a remote snapshot makes the local player/settings
 * state change, which would otherwise be pushed straight back to the peer and
 * ping-pong forever. After receiving a snapshot we therefore suppress our own
 * pushes for a short window.
 */
class DesktopSyncManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val relay = LanSyncRelay()

    private val _connectionState = MutableStateFlow(SyncConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _pairCode = MutableStateFlow("")
    val pairCode: StateFlow<String> = _pairCode.asStateFlow()

    /** Epoch millis when the current pairing code expires (0 = no active code). */
    private val _pairCodeExpiresAt = MutableStateFlow(0L)
    val pairCodeExpiresAt: StateFlow<Long> = _pairCodeExpiresAt.asStateFlow()

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    private val _lanRunning = MutableStateFlow(false)
    val lanRunning: StateFlow<Boolean> = _lanRunning.asStateFlow()

    private val _lanAddress = MutableStateFlow("")
    val lanAddress: StateFlow<String> = _lanAddress.asStateFlow()

    private val _syncedSettings = MutableStateFlow<Map<String, String>>(emptyMap())
    val syncedSettings: StateFlow<Map<String, String>> = _syncedSettings.asStateFlow()

    /** Incoming playback snapshots to apply to the player. */
    private val _incomingPlayback = MutableSharedFlow<PlaybackSnapshot>(extraBufferCapacity = 8)
    val incomingPlayback: SharedFlow<PlaybackSnapshot> = _incomingPlayback.asSharedFlow()

    /** Incoming settings snapshots to apply to the theme/language layer. */
    private val _incomingSettings = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 8)
    val incomingSettings: SharedFlow<Map<String, String>> = _incomingSettings.asSharedFlow()

    /** Incoming library snapshots to apply to the local library. */
    private val _incomingLibrary = MutableSharedFlow<LibrarySnapshot?>(extraBufferCapacity = 8)
    val incomingLibrary: SharedFlow<LibrarySnapshot?> = _incomingLibrary.asSharedFlow()

    private val _syncedLibrary = MutableStateFlow<LibrarySnapshot?>(null)
    val syncedLibrary: StateFlow<LibrarySnapshot?> = _syncedLibrary.asStateFlow()

    private var client: SyncClient? = null

    private var lastPlayback: PlaybackSnapshot? = null
    private var lastSettings: Map<String, String> = emptyMap()
    private var lastLibrary: LibrarySnapshot? = null

    @Volatile
    private var suppressPushUntil = 0L

    init {
        lastLibrary = DesktopSettings.load().library
        // Best-effort: when the desktop window is closed while the LAN relay is
        // running, tell any connected phone it is no longer paired (so the phone
        // does not keep showing "paired" for a relay that no longer exists).
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { relay.shutdownNotify() } })
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    fun connect(serverUrl: String) {
        val url = serverUrl.trim()
        if (url.isEmpty()) return
        teardownClient()
        val created = SyncClient(
            serverUrl = url,
            deviceId = DesktopSettings.newDeviceId(),
            deviceName = "Desktop",
        )
        client = created
        scope.launch { created.connectionState.collect { _connectionState.value = it } }
        scope.launch { created.events.collect { handleEvent(it) } }
        created.connect()
        // Persist only real relay URLs; the ephemeral localhost LAN address
        // must not become the saved default.
        if (!url.startsWith("ws://localhost")) {
            DesktopSettings.save(DesktopSettings.load().copy(serverUrl = url))
        }
    }

    fun disconnect() {
        teardownClient()
        _status.value = ""
    }

    /** Starts the local LAN relay, connects the desktop to it, and generates
     *  a pairing code so the phone can pair right away. */
    fun startLan() {
        scope.launch {
            val port = relay.start()
            _lanRunning.value = true
            _lanAddress.value = "ws://${lanIpAddress()}:$port"
            connect("ws://localhost:$port")
            requestPairingCode()
        }
    }

    fun stopLan() {
        // Notify the phone it is unpaired, then tear down the relay.
        scope.launch { relay.stop() }
        _lanRunning.value = false
        _lanAddress.value = ""
        teardownClient()
        // Stopping the LAN server unpairs both sides.
        DesktopSettings.save(DesktopSettings.load().copy(pairId = ""))
        _paired.value = false
        _pairCode.value = ""
        _pairCodeExpiresAt.value = 0L
    }

    fun requestPairingCode() {
        ensureConnected()
        client?.requestPairingCode()
    }

    fun joinPair(code: String) {
        ensureConnected()
        client?.joinPair(code)
    }

    fun unpair() {
        client?.unpair()
        DesktopSettings.save(DesktopSettings.load().copy(pairId = ""))
        _paired.value = false
        _pairCode.value = ""
        _pairCodeExpiresAt.value = 0L
    }

    /** Update the local playback snapshot and push it to the peer (if paired). */
    fun updatePlayback(playback: PlaybackSnapshot?) {
        lastPlayback = playback
        pushSnapshot()
    }

    /** Update the local settings snapshot and push it to the peer (if paired). */
    fun updateSettings(settings: Map<String, String>) {
        lastSettings = settings
        pushSnapshot()
    }

    /** Update the local library snapshot and push it to the peer (if paired). */
    fun updateLibrary(library: LibrarySnapshot?) {
        lastLibrary = library
        pushSnapshot()
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private fun ensureConnected() {
        val c = client
        if (c == null) {
            val saved = DesktopSettings.load().serverUrl
            if (saved.isNotBlank()) connect(saved)
            return
        }
        if (c.connectionState.value != SyncConnectionState.CONNECTED) c.connect()
    }

    private fun teardownClient() {
        client?.disconnect()
        client = null
        _connectionState.value = SyncConnectionState.DISCONNECTED
    }

    private fun pushSnapshot() {
        if (System.currentTimeMillis() < suppressPushUntil) return
        val c = client ?: return
        if (c.connectionState.value != SyncConnectionState.CONNECTED) return
        if (!_paired.value) return
        c.pushSnapshot(
            SyncSnapshot(
                deviceId = c.deviceId,
                deviceName = "Desktop",
                updatedAt = System.currentTimeMillis(),
                settings = lastSettings,
                playback = lastPlayback,
                library = lastLibrary,
            )
        )
    }

    private fun handleEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.Connected -> {
                _status.value = "Connected"
                if (DesktopSettings.load().pairId.isNotEmpty()) client?.pullSnapshot()
            }
            is SyncEvent.Disconnected -> _status.value = "Disconnected"
            is SyncEvent.PairCode -> {
                _pairCode.value = event.code
                _pairCodeExpiresAt.value = System.currentTimeMillis() + PAIR_CODE_TTL_MS
                _status.value = event.code
            }
            is SyncEvent.Paired -> {
                _paired.value = true
                _pairCode.value = ""
                _pairCodeExpiresAt.value = 0L
                DesktopSettings.save(DesktopSettings.load().copy(pairId = event.pairId))
                _status.value = "Paired with ${event.peerDeviceName}"
                pushSnapshot()
            }
            is SyncEvent.NoSnapshot -> {
                // Reconnect with a persisted pairId: the relay confirms we are
                // still paired (just no mailbox snapshot yet).
                _paired.value = true
                _status.value = "Paired"
            }
            is SyncEvent.SnapshotReceived -> {
                _paired.value = true
                suppressPushUntil = System.currentTimeMillis() + ECHO_SUPPRESS_MS
                _status.value = "Snapshot received"
                if (event.snapshot.settings.isNotEmpty()) {
                    _syncedSettings.value = event.snapshot.settings
                    DesktopSettings.save(DesktopSettings.load().copy(settings = event.snapshot.settings))
                    scope.launch { _incomingSettings.emit(event.snapshot.settings) }
                }
                event.snapshot.playback?.let { pb ->
                    scope.launch { _incomingPlayback.emit(pb) }
                }
                event.snapshot.library?.let { lib ->
                    _syncedLibrary.value = lib
                    DesktopSettings.save(DesktopSettings.load().copy(library = lib))
                    scope.launch { _incomingLibrary.emit(lib) }
                }
            }
            is SyncEvent.Error -> {
                _status.value = event.message
                if (event.message.contains("unpaired", ignoreCase = true) ||
                    event.message.contains("not paired", ignoreCase = true)
                ) {
                    // The peer unpaired us, or the relay no longer knows this pair.
                    _paired.value = false
                    _pairCode.value = ""
                    _pairCodeExpiresAt.value = 0L
                    DesktopSettings.save(DesktopSettings.load().copy(pairId = ""))
                }
            }
        }
    }

    companion object {
        private const val ECHO_SUPPRESS_MS = 1500L

        /** Pairing codes are valid for 5 minutes (matches the relay servers). */
        const val PAIR_CODE_TTL_MS = 5 * 60 * 1000L
    }
}
