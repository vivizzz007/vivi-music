package com.music.vivi.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.Locale

/**
 * Reads and writes the OS system volume (0f..1f) so it can be synchronized
 * with the Android system (STREAM_MUSIC) volume. Each platform uses a
 * best-effort native path:
 *
 * - Windows: WinMM `waveOutGetVolume`/`waveOutSetVolume`. These functions take
 *   an *open* device handle, so we open the default wave device first. They
 *   control the legacy "wave" volume, which Windows routes to the default
 *   output device's session (not the 100%-always master mixer slider).
 * - Linux: PulseAudio/PipeWire via `pactl`, with an ALSA `amixer` fallback.
 * - macOS: `osascript` (AppleScript) system output volume.
 *
 * Every call is guarded; failures no-op instead of crashing playback.
 */
object SystemVolume {

    private val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)

    /** Best-effort read of the current system volume (0f..1f), or null if unavailable. */
    fun get(): Float? = when {
        os.contains("win") -> runCatching { WindowsVolume.get() }.getOrNull()
        os.contains("mac") -> runCatching { MacVolume.get() }.getOrNull()
        os.contains("linux") -> runCatching { LinuxVolume.get() }.getOrNull()
        else -> null
    }

    /** Best-effort write of the system volume (0f..1f). */
    fun set(v: Float) {
        val c = v.coerceIn(0f, 1f)
        when {
            os.contains("win") -> runCatching { WindowsVolume.set(c) }
            os.contains("mac") -> runCatching { MacVolume.set(c) }
            os.contains("linux") -> runCatching { LinuxVolume.set(c) }
        }
    }
}

/**
 * Windows wave volume via WinMM.
 *
 * `waveOutGetVolume`/`waveOutSetVolume` require a valid `HWAVEOUT` handle; the
 * `WAVE_MAPPER` constant is only valid for `waveOutOpen`, not for the get/set
 * calls (passing it as a handle made every call fail, so volume never synced).
 * We therefore open the default wave device with a minimal PCM format, apply
 * the operation, and close it again.
 */
private object WindowsVolume {

    @Structure.FieldOrder(
        "wFormatTag", "nChannels", "nSamplesPerSec", "nAvgBytesPerSec",
        "nBlockAlign", "wBitsPerSample", "cbSize",
    )
    class WaveFormatEx : Structure() {
        @JvmField var wFormatTag: Short = 1 // WAVE_FORMAT_PCM
        @JvmField var nChannels: Short = 2
        @JvmField var nSamplesPerSec: Int = 44100
        @JvmField var nAvgBytesPerSec: Int = 44100 * 4
        @JvmField var nBlockAlign: Short = 4
        @JvmField var wBitsPerSample: Short = 16
        @JvmField var cbSize: Short = 0
    }

    private interface Winmm : StdCallLibrary {
        // `uDeviceID` is UINT_PTR (pointer-sized), so it must be mapped as a
        // Pointer: WAVE_MAPPER is (UINT_PTR)-1 (all bits set).
        //
        // NOTE: `waveOutOpen` takes no string argument, so winmm.dll exports it
        // with NO A/W suffix — the `waveOutOpenW`/`waveOutOpenA` names exist only
        // as C header macros, not as DLL symbols. Looking up `waveOutOpenW` threw
        // "Error looking up function" and crashed the app at startup.
        fun waveOutOpen(phwo: PointerByReference, uDeviceID: Pointer, pwfx: Pointer?, dwCallback: Pointer?, dwInstance: Pointer?, fdwOpen: Int): Int
        fun waveOutGetVolume(hwo: Pointer, pdwVolume: IntByReference): Int
        fun waveOutSetVolume(hwo: Pointer, dwVolume: Int): Int
        fun waveOutClose(hwo: Pointer): Int
    }

    private val winmm: Winmm? = runCatching { Native.load("winmm", Winmm::class.java) }.getOrNull()

    /** Opens the default wave device, runs [block], and closes it. */
    private inline fun withDevice(block: (Pointer) -> Unit) {
        val mm = winmm ?: return
        val format = WaveFormatEx()
        format.write()
        val ref = PointerByReference()
        val opened = runCatching {
            mm.waveOutOpen(ref, Pointer(-1L) /* WAVE_MAPPER */, format.pointer, null, null, 0)
        }.getOrNull() ?: return
        if (opened != 0) return
        try {
            block(ref.value)
        } finally {
            runCatching { mm.waveOutClose(ref.value) }
        }
    }

    fun get(): Float? {
        val mm = winmm ?: return null
        var result: Float? = null
        withDevice { hwo ->
            val vol = IntByReference()
            val rc = runCatching { mm.waveOutGetVolume(hwo, vol) }.getOrNull() ?: return@withDevice
            if (rc == 0) {
                val v = vol.value
                val left = (v and 0xFFFF) / 65535f
                val right = ((v ushr 16) and 0xFFFF) / 65535f
                result = ((left + right) / 2f).coerceIn(0f, 1f)
            }
        }
        return result
    }

    fun set(v: Float) {
        val mm = winmm ?: return
        val word = (v.coerceIn(0f, 1f) * 65535f).toInt().coerceIn(0, 65535)
        val dw = (word and 0xFFFF) or (word shl 16)
        withDevice { hwo -> runCatching { mm.waveOutSetVolume(hwo, dw) } }
    }
}

/** macOS system output volume via AppleScript. */
private object MacVolume {
    fun get(): Float = runCatching {
        val out = exec("osascript", "-e", "output volume of (get volume settings)")
        out.trim().toFloatOrNull()?.div(100f) ?: 0.5f
    }.getOrDefault(0.5f)

    fun set(v: Float) {
        val pct = (v * 100f).toInt().coerceIn(0, 100)
        runCatching { exec("osascript", "-e", "set volume output volume $pct") }
    }
}

/** Linux system volume via PulseAudio/PipeWire, with an ALSA fallback. */
private object LinuxVolume {
    fun get(): Float = runCatching {
        val out = exec("pactl", "get-sink-volume", "@DEFAULT_SINK@")
        Regex("(\\d+)%").find(out)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
            ?: 0.5f
    }.getOrElse { amixerGet() }

    fun set(v: Float) {
        val pct = (v * 100f).toInt().coerceIn(0, 100)
        runCatching { exec("pactl", "set-sink-volume", "@DEFAULT_SINK@", "$pct%") }
            .onFailure { runCatching { exec("amixer", "sset", "Master", "$pct%") } }
    }

    private fun amixerGet(): Float = runCatching {
        val out = exec("amixer", "sget", "Master")
        Regex("(\\d+)%").find(out)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
            ?: 0.5f
    }.getOrDefault(0.5f)
}

private fun exec(vararg cmd: String): String =
    ProcessBuilder(*cmd).redirectErrorStream(true).start().inputStream.bufferedReader().readText()
