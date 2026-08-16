package com.music.vivi.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference
import java.util.Locale

/**
 * Reads and writes the OS system volume (0f..1f) so it can be synchronized
 * with the Android system (STREAM_MUSIC) volume. Each platform uses a
 * best-effort native path:
 *
 * - Windows: WinMM `waveOutGetVolume`/`waveOutSetVolume` on the wave mapper,
 *   which Windows routes to the default output device.
 * - Linux: PulseAudio/PipeWire via `pactl`, with an ALSA `amixer` fallback.
 * - macOS: `osascript` (AppleScript) system output volume.
 *
 * Every call is guarded; failures no-op instead of crashing playback.
 */
object SystemVolume {

    private val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)

    /** Best-effort read of the current system volume (0f..1f), or null if unavailable. */
    fun get(): Float? = when {
        os.contains("win") -> WindowsVolume.get()
        os.contains("mac") -> runCatching { MacVolume.get() }.getOrNull()
        os.contains("linux") -> runCatching { LinuxVolume.get() }.getOrNull()
        else -> null
    }

    /** Best-effort write of the system volume (0f..1f). */
    fun set(v: Float) {
        val c = v.coerceIn(0f, 1f)
        when {
            os.contains("win") -> WindowsVolume.set(c)
            os.contains("mac") -> runCatching { MacVolume.set(c) }
            os.contains("linux") -> runCatching { LinuxVolume.set(c) }
        }
    }
}

/** Windows wave-mapper volume via WinMM (legacy but maps to the default device). */
private object WindowsVolume {
    private interface Winmm : Library {
        fun waveOutGetVolume(hwo: Int, pdwVolume: IntByReference): Int
        fun waveOutSetVolume(hwo: Int, dwVolume: Int): Int
    }

    private val winmm: Winmm? = runCatching { Native.load("winmm", Winmm::class.java) }.getOrNull()
    private const val WAVE_MAPPER = -1

    fun get(): Float? {
        val mm = winmm ?: return null
        val ref = IntByReference()
        if (runCatching { mm.waveOutGetVolume(WAVE_MAPPER, ref) }.getOrDefault(-1) != 0) return null
        val vol = ref.value
        val left = (vol and 0xFFFF) / 65535f
        val right = ((vol ushr 16) and 0xFFFF) / 65535f
        return ((left + right) / 2f).coerceIn(0f, 1f)
    }

    fun set(v: Float) {
        val mm = winmm ?: return
        val word = (v * 65535f).toInt().coerceIn(0, 65535)
        val dw = (word and 0xFFFF) or (word shl 16)
        runCatching { mm.waveOutSetVolume(WAVE_MAPPER, dw) }
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
