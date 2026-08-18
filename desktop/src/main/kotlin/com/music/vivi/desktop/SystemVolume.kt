package com.music.vivi.desktop

import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Reads and writes the OS **system** volume (0f..1f) so it can be synchronized
 * with the Android system (STREAM_MUSIC) volume. Each platform uses a
 * best-effort native path:
 *
 * - Windows: WASAPI `IAudioEndpointVolume` — controls the **master** volume
 *   (the speaker icon in the tray), NOT the per-app "VIVIMusic" mixer entry.
 *   The app's own session is additionally pinned to 100% so the mixer never
 *   quietly mutes VIVI under the master.
 * - Linux: PulseAudio/PipeWire via `pactl`, with an ALSA `amixer` fallback
 *   (already the default sink / master volume).
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

    /**
     * Windows master mute state (true = muted, false = unmuted, null = unknown).
     * The app only controls the master volume when it is NOT muted; a muted
     * master would swallow every volume write. Returns null on non-Windows or
     * when the WASAPI call fails, so callers can no-op safely.
     */
    fun isMuted(): Boolean? =
        if (os.contains("win")) runCatching { WindowsVolume.isMuted() }.getOrNull() else null

    /** Best-effort mute/unmute of the Windows master volume. */
    fun setMuted(muted: Boolean) {
        if (os.contains("win")) runCatching { WindowsVolume.setMuted(muted) }
    }

    /**
     * Startup guard: if the Windows master volume is muted, unmute it and put
     * it at 0% so a paired mobile device can control it (a muted master ignores
     * every volume write). Non-Windows and non-muted states are left untouched.
     */
    fun unmuteIfMuted() {
        if (!os.contains("win")) return
        val muted = runCatching { WindowsVolume.isMuted() }.getOrNull() ?: return
        if (muted) {
            WindowsVolume.setMuted(false)
            WindowsVolume.set(0f)
        }
    }
}

/**
 * Windows master volume via WASAPI (Core Audio).
 *
 * WinMM (`waveOutGetVolume`/`waveOutSetVolume`) only touches the calling
 * process's audio *session* on Vista+, i.e. the "VIVIMusic" entry in the
 * volume mixer — not the global volume. To move the master volume we talk to
 * Core Audio through COM:
 *
 * `CoCreateInstance(MMDeviceEnumerator)` → `GetDefaultAudioEndpoint(eRender,
 * eConsole)` → `Activate(IAudioEndpointVolume)` → `Get/SetMasterVolumeLevelScalar`.
 *
 * Core Audio objects are created and used on one dedicated MTA thread. The
 * Compose/AWT UI thread is an STA, and `CoInitializeEx(MTA)` there returns
 * `RPC_E_CHANGED_MODE`, so all COM work is marshaled onto that thread.
 */
private object WindowsVolume {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "VIVI-CoreAudio").apply { isDaemon = true }
    }

    private interface Ole32 : StdCallLibrary {
        fun CoInitializeEx(pvReserved: Pointer?, dwCoInit: Int): Int
        fun CoUninitialize()
        fun CoCreateInstance(rclsid: Pointer, pUnkOuter: Pointer?, dwClsContext: Int, riid: Pointer, ppv: PointerByReference): Int
    }
    private val ole32: Ole32? = runCatching { Native.load("ole32", Ole32::class.java) }.getOrNull()

    private const val COINIT_MULTITHREADED = 0x0
    private const val CLSCTX_ALL = 0x17
    private const val S_OK = 0
    private const val S_FALSE = 1
    private const val RPC_E_CHANGED_MODE = 0x80010106.toInt()

    // EDataFlow.eRender / ERole.eConsole
    private const val E_RENDER = 0
    private const val E_CONSOLE = 0

    // COM interface vtable slots (after the 3 IUnknown methods at 0..2).
    // IMMDeviceEnumerator
    private const val ENUM_GET_DEFAULT_AUDIO_ENDPOINT = 4
    // IMMDevice
    private const val DEVICE_ACTIVATE = 3
    // IAudioEndpointVolume
    private const val EPV_SET_MASTER_SCALAR = 7
    private const val EPV_GET_MASTER_SCALAR = 9
    private const val EPV_SET_MUTE = 14
    private const val EPV_GET_MUTE = 15
    // IAudioSessionManager
    private const val ASM_GET_SIMPLE_VOLUME = 4
    // ISimpleAudioVolume
    private const val SAV_SET_MASTER = 3

    /** Builds a 16-byte little-endian GUID in native memory. */
    private fun guid(d1: Int, d2: Int, d3: Int, d4: ByteArray): Memory {
        val m = Memory(16)
        m.setInt(0, d1)
        m.setShort(4, d2.toShort())
        m.setShort(6, d3.toShort())
        m.write(8, d4, 0, 8)
        return m
    }

    private val CLSID_MMDeviceEnumerator = guid(
        0xBCDE0395.toInt(), 0xE52F, 0x467C,
        byteArrayOf(0x8E.toByte(), 0x3D, 0xC4.toByte(), 0x57, 0x92.toByte(), 0x91.toByte(), 0x69, 0x2E),
    )
    private val IID_IMMDeviceEnumerator = guid(
        0xA95664D2.toInt(), 0x9614, 0x4F35,
        byteArrayOf(0xA7.toByte(), 0x46, 0xDE.toByte(), 0x8D.toByte(), 0xB6.toByte(), 0x36, 0x17, 0xE6.toByte()),
    )
    private val IID_IAudioEndpointVolume = guid(
        0x5CDF2C82, 0x841E, 0x4546,
        byteArrayOf(0x97.toByte(), 0x22, 0x0C, 0xF7.toByte(), 0x40, 0x78, 0x22, 0x9A.toByte()),
    )
    private val IID_IAudioSessionManager = guid(
        0xBFA971F1.toInt(), 0x4D5E, 0x40BB,
        byteArrayOf(0x93.toByte(), 0x5E, 0x96.toByte(), 0x70, 0x39, 0xBF.toByte(), 0xBE.toByte(), 0xE4.toByte()),
    )
    private val IID_ISimpleAudioVolume = guid(
        0x87CE5498.toInt(), 0x68D6, 0x44E5,
        byteArrayOf(0x92.toByte(), 0x15, 0x6D, 0xA4.toByte(), 0x7E, 0xF8.toByte(), 0x83.toByte(), 0xD8.toByte()),
    )

    /** A raw COM interface pointer with stdcall vtable dispatch. */
    private class ComObject(val ptr: Pointer) {
        private val vtable: Pointer get() = ptr.getPointer(0)

        fun invoke(index: Int, vararg args: Any?): Int {
            val fn = Function.getFunction(vtable.getPointer((index * Native.POINTER_SIZE).toLong()), Function.ALT_CONVENTION)
            val all = arrayOfNulls<Any>(args.size + 1)
            all[0] = ptr
            args.forEachIndexed { i, a -> all[i + 1] = a }
            return fn.invoke(Integer::class.java, all) as Int
        }
    }

    // State owned by the single COM thread.
    private var endpointVolume: ComObject? = null
    private var simpleVolume: ComObject? = null

    private fun <T> runOnCom(block: () -> T?): T? =
        try {
            executor.submit<T?>(block).get(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        }

    fun get(): Float? = runOnCom { getOnCom() }

    fun set(v: Float) {
        runOnCom { setOnCom(v); null }
    }

    fun isMuted(): Boolean = runOnCom { getMuteOnCom() } ?: false

    fun setMuted(muted: Boolean) {
        runOnCom { setMuteOnCom(muted); null }
    }

    private fun initCom(): Boolean {
        val o = ole32 ?: return false
        val hr = runCatching { o.CoInitializeEx(null, COINIT_MULTITHREADED) }.getOrNull() ?: return false
        // S_OK / S_FALSE = ok, RPC_E_CHANGED_MODE = already initialized (ok).
        return hr == S_OK || hr == S_FALSE || hr == RPC_E_CHANGED_MODE
    }

    private fun ensureVolumes(): Boolean {
        val o = ole32 ?: return false
        if (!initCom()) return false
        if (endpointVolume != null && simpleVolume != null) return true
        return runCatching {
            val enumeratorRef = PointerByReference()
            check(o.CoCreateInstance(CLSID_MMDeviceEnumerator, null, CLSCTX_ALL, IID_IMMDeviceEnumerator, enumeratorRef) == S_OK)
            val enumerator = ComObject(enumeratorRef.value)

            val deviceRef = PointerByReference()
            check(enumerator.invoke(ENUM_GET_DEFAULT_AUDIO_ENDPOINT, E_RENDER, E_CONSOLE, deviceRef) == S_OK)
            val device = ComObject(deviceRef.value)

            val epRef = PointerByReference()
            check(device.invoke(DEVICE_ACTIVATE, IID_IAudioEndpointVolume, CLSCTX_ALL, null, epRef) == S_OK)
            endpointVolume = ComObject(epRef.value)

            val smRef = PointerByReference()
            check(device.invoke(DEVICE_ACTIVATE, IID_IAudioSessionManager, CLSCTX_ALL, null, smRef) == S_OK)
            val sm = ComObject(smRef.value)

            val svRef = PointerByReference()
            check(sm.invoke(ASM_GET_SIMPLE_VOLUME, null, 0, svRef) == S_OK)
            simpleVolume = ComObject(svRef.value)
            true
        }.getOrDefault(false)
    }

    /** Pin the app's own session ("VIVIMusic" in the mixer) to 100%. */
    private fun pinSessionToMax() {
        val sv = simpleVolume ?: return
        runCatching { sv.invoke(SAV_SET_MASTER, 1.0f, null) }
    }

    private fun getOnCom(): Float? {
        if (!ensureVolumes()) return null
        val ep = endpointVolume ?: return null
        pinSessionToMax()
        val ref = FloatByReference()
        val hr = runCatching { ep.invoke(EPV_GET_MASTER_SCALAR, ref) }.getOrNull() ?: return null
        if (hr != S_OK) return null
        return ref.value.coerceIn(0f, 1f)
    }

    private fun setOnCom(v: Float) {
        if (!ensureVolumes()) return
        val ep = endpointVolume ?: return
        pinSessionToMax()
        runCatching { ep.invoke(EPV_SET_MASTER_SCALAR, v.coerceIn(0f, 1f), null) }
    }

    private fun getMuteOnCom(): Boolean? {
        if (!ensureVolumes()) return null
        val ep = endpointVolume ?: return null
        val ref = IntByReference()
        val hr = runCatching { ep.invoke(EPV_GET_MUTE, ref) }.getOrNull() ?: return null
        if (hr != S_OK) return null
        return ref.value != 0
    }

    private fun setMuteOnCom(muted: Boolean) {
        if (!ensureVolumes()) return
        val ep = endpointVolume ?: return
        pinSessionToMax()
        runCatching { ep.invoke(EPV_SET_MUTE, if (muted) 1 else 0, null) }
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
