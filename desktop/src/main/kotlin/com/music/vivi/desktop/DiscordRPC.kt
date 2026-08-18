package com.music.vivi.desktop

import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal Discord Rich Presence client speaking the local IPC protocol
 * (pipe `discord-ipc-0`, handshake op 0, SET_ACTIVITY op 1).
 *
 * Windows uses a named pipe through JNA (like [SystemVolume]); other
 * platforms currently no-op (Discord IPC over AF_UNIX needs a native
 * binding we don't ship). Safe to call from any thread; failures are
 * swallowed and retried while enabled.
 */
object DiscordRPC {

    @Volatile var enabled: Boolean = false
        set(value) {
            field = value
            if (value) ensureRunning() else stop()
        }

    @Volatile var clientId: String = ""

    @Volatile private var details: String? = null
    @Volatile private var state: String? = null
    @Volatile private var largeImage: String? = null
    @Volatile private var startTs: Long? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private val connected = AtomicBoolean(false)

    // --- JNA pointers to kernel32 functions (loaded lazily, Windows only) ---
    private class Win32 {
        lateinit var createFileW: Function
        lateinit var writeFile: Function
        lateinit var readFile: Function
        lateinit var closeHandle: Function
        lateinit var getLastError: Function
    }

    private var win: Win32? = null

    private fun ensureWin32(): Win32? {
        val os = System.getProperty("os.name", "").lowercase()
        if (!os.contains("win")) return null
        win?.let { return it }
        return try {
            Win32().apply {
                createFileW = Function.getFunction("kernel32", "CreateFileW")
                writeFile = Function.getFunction("kernel32", "WriteFile")
                readFile = Function.getFunction("kernel32", "ReadFile")
                closeHandle = Function.getFunction("kernel32", "CloseHandle")
                getLastError = Function.getFunction("kernel32", "GetLastError")
            }.also { win = it }
        } catch (_: Throwable) {
            null
        }
    }

    fun updateActivity(details: String?, state: String?, largeImage: String? = "vivimusic", startTs: Long? = System.currentTimeMillis()) {
        this.details = details
        this.state = state
        this.largeImage = largeImage
        this.startTs = startTs
        if (enabled) ensureRunning()
    }

    fun clear() {
        details = null
        state = null
        updateActivity(null, null, null, null)
    }

    private fun ensureRunning() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive && enabled) {
                try {
                    runLoop()
                } catch (_: Throwable) {
                    // Pipe closed / Discord not running: wait and retry.
                }
                delay(15_000)
            }
        }
    }

    private fun stop() {
        connected.set(false)
        job?.cancel()
        job = null
    }

    private suspend fun runLoop() {
        val w = ensureWin32()
        if (w == null) {
            delay(60_000)
            return
        }
        val pipe = "\\\\.\\pipe\\discord-ipc-0"
        val GENERIC_READ = 0x80000000L
        val GENERIC_WRITE = 0x40000000L
        val OPEN_EXISTING = 3
        val handle = w.createFileW.invokeLong(
            arrayOf(
                Memory((Native.WCHAR_SIZE * (pipe.length + 1)).toLong()).apply {
                    setWideString(0L, pipe)
                },
                GENERIC_READ or GENERIC_WRITE,
                0,
                Pointer.NULL,
                OPEN_EXISTING,
                0,
                Pointer.NULL,
            )
        )
        if (handle == -1L || handle == 0L) {
            // Discord not running.
            delay(15_000)
            return
        }
        connected.set(true)
        try {
            writeFrame(w, handle, 0, """{"v":1,"client_id":"${clientId.ifBlank { "0" }}" }""")
            // Read the READY frame (op 1) so the pipe is in a clean state.
            readFrame(w, handle)

            while (connected.get()) {
                val payload = buildString {
                    append("""{"cmd":"SET_ACTIVITY","args":{""")
                    append(""""pid":${ProcessHandle.current().pid()},""")
                    append(""""activity":{""")
                    details?.let {
                        append(""""details":${jsonQuote(it)},""")
                    }
                    state?.let {
                        append(""""state":${jsonQuote(it)},""")
                    }
                    append(""""timestamps":{"start":${startTs ?: System.currentTimeMillis()}},""")
                    append(""""assets":{"large_image":${jsonQuote(largeImage ?: "vivimusic")}}""")
                    append("""}},"nonce":"${java.util.UUID.randomUUID()}",""")
                }
                writeFrame(w, handle, 1, payload)
                delay(15_000)
            }
        } finally {
            w.closeHandle.invoke(arrayOf(handle))
            connected.set(false)
        }
    }

    private fun jsonQuote(s: String): String {
        val esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return "\"$esc\""
    }

    private fun writeFrame(w: Win32, handle: Long, op: Int, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val buf = Memory((8 + bytes.size).toLong())
        buf.setInt(0L, op)
        buf.setInt(4L, bytes.size)
        buf.write(8L, bytes, 0, bytes.size)
        val written = Memory(Native.SIZE_T_SIZE.toLong())
        w.writeFile.invoke(arrayOf(handle, buf, buf.size().toLong(), written, Pointer.NULL))
    }

    private fun readFrame(w: Win32, handle: Long): String {
        val header = Memory(8L)
        val read = Memory(Native.SIZE_T_SIZE.toLong())
        w.readFile.invoke(arrayOf(handle, header, 8L, read, Pointer.NULL))
        val len = header.getInt(4)
        if (len <= 0 || len > 1 shl 20) return ""
        val body = Memory(len.toLong())
        w.readFile.invoke(arrayOf(handle, body, len.toLong(), read, Pointer.NULL))
        return body.getString(0, "UTF-8")
    }
}
