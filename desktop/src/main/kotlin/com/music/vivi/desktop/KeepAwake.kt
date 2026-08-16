package com.music.vivi.desktop

import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Keeps the host display/system awake while the desktop is paired with a phone,
 * so the OS going to sleep can't tear down the sync socket (which would unpair
 * the two devices).
 *
 * - Windows: kernel32 `SetThreadExecutionState` with `ES_DISPLAY_REQUIRED |
 *   ES_SYSTEM_REQUIRED | ES_CONTINUOUS`; cleared with `ES_CONTINUOUS` alone.
 *   The request is per-thread, so every call is marshalled onto one dedicated
 *   thread that lives for the whole app.
 * - macOS: a `caffeinate -disu` child process, destroyed on release.
 * - Linux: best-effort no-op (would need systemd-inhibit / logind).
 *
 * Every call is guarded; failures no-op instead of crashing the app.
 */
object KeepAwake {

    private val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "VIVI-KeepAwake").apply { isDaemon = true }
    }

    @Volatile
    private var enabled = false

    private interface Kernel32 : StdCallLibrary {
        fun SetThreadExecutionState(esFlags: Int): Int
    }
    private val kernel32: Kernel32? =
        if (os.contains("win")) runCatching { Native.load("kernel32", Kernel32::class.java) }.getOrNull()
        else null

    private const val ES_CONTINUOUS = 0x80000000.toInt()
    private const val ES_SYSTEM_REQUIRED = 0x00000001
    private const val ES_DISPLAY_REQUIRED = 0x00000002

    @Volatile
    private var caffeinate: Process? = null

    init {
        // On exit, make sure a macOS caffeinate child isn't left running.
        Runtime.getRuntime().addShutdownHook(Thread {
            runCatching { caffeinate?.destroy() }
        })
    }

    /** Enable or disable keep-awake. Idempotent and non-blocking. */
    fun setEnabled(value: Boolean) {
        if (value == enabled) return
        enabled = value
        executor.submit { runCatching { apply(value) } }
    }

    private fun apply(value: Boolean) {
        when {
            os.contains("win") -> {
                val k = kernel32 ?: return
                val flags = if (value) {
                    ES_CONTINUOUS or ES_SYSTEM_REQUIRED or ES_DISPLAY_REQUIRED
                } else {
                    ES_CONTINUOUS
                }
                k.SetThreadExecutionState(flags)
            }
            os.contains("mac") -> {
                if (value) {
                    caffeinate?.destroy()
                    caffeinate = ProcessBuilder("caffeinate", "-disu").start()
                } else {
                    caffeinate?.destroy()
                    caffeinate = null
                }
            }
        }
    }
}
