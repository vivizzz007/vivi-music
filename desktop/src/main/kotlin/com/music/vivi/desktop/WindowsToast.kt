package com.music.vivi.desktop

import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Native Windows toast notifications (the ones that land in the Action Center /
 * notification history).
 *
 * `java.awt.SystemTray` uses legacy `Shell_NotifyIcon` balloons, which Windows
 * 10/11 no longer surfaces in the Action Center. The proper mechanism is a
 * WinRT toast (`Windows.UI.Notifications`) shown against a registered
 * AppUserModelID (AUMID).
 *
 * Because WinRT COM interop is impractical to marshal by hand from the JVM,
 * this uses a small PowerShell helper (Windows always ships PowerShell 5.1+).
 * It does two things:
 *
 *  1. Registers the AUMID by creating a Start-menu shortcut to the installed
 *     `.exe` and setting `System.AppUserModel.ID` on it (via the shell
 *     property store, in an inline C# `Add-Type` block).
 *  2. Shows the toast with a foreground activation `launch` argument
 *     (`--open=<section>`), so clicking the toast can open a specific screen.
 *
 * All calls are guarded and run on a daemon thread; failures no-op instead of
 * crashing. Only active when packaged by jpackage (`jpackage.app-path` is set).
 */
object WindowsToast {

    private const val AUMID = "PiBOH.VIVIMusicDE"
    private val os = System.getProperty("os.name", "").lowercase()

    @Volatile
    private var registered = false

    private val appExe: String?
        get() = System.getProperty("jpackage.app-path").orEmpty().ifBlank { null }

    /** True on a packaged Windows build (the only case where toasts can work). */
    fun isAvailable(): Boolean = os.contains("win") && appExe != null

    /** Appends a diagnostic line to `~/.vivimusic/native-notify.log`. */
    fun log(msg: String) {
        runCatching {
            val file = File(System.getProperty("user.home"), ".vivimusic/native-notify.log")
            file.parentFile?.mkdirs()
            file.appendText("[${java.time.LocalDateTime.now()}] $msg\n")
        }
    }

    /** Shows a native toast with [title] and [message], opening [section] on click. */
    fun show(title: String, message: String, section: String?) {
        log("show: isAvailable=${isAvailable()} os=$os appExe=$appExe title=\"$title\" section=$section")
        if (!isAvailable()) return
        Thread {
            runCatching {
                ensureRegistered()
                val logo = extractLogo()
                val ok = runPowerShell(toastScript(title, message, section, logo))
                log("show: toast powershell result=$ok")
            }.onFailure { log("show: exception=$it") }
        }.apply { isDaemon = true; name = "VIVI-Toast" }.start()
    }

    /** Registers the AUMID shortcut once per process (idempotent). */
    private fun ensureRegistered() {
        if (registered) return
        synchronized(this) {
            if (registered) return
            val exe = appExe ?: return
            log("ensureRegistered: registering AUMID for exe=$exe")
            registered = runCatching { runPowerShell(registrationScript(exe)) }.getOrDefault(false)
            log("ensureRegistered: result=$registered")
        }
    }

    /** Extracts the bundled logo to a real file so the toast can reference it via `file://`. */
    private fun extractLogo(): File? {
        val target = File(System.getProperty("user.home"), ".vivimusic/logo_vmde.png")
        if (target.exists()) return target
        val stream = WindowsToast::class.java.getResourceAsStream("/images/logo_vmde.png") ?: return null
        return runCatching {
            target.parentFile?.mkdirs()
            stream.use { input -> target.outputStream().use { input.copyTo(it) } }
            target
        }.getOrNull()
    }

    private fun runPowerShell(script: String): Boolean {
        val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        return runCatching {
            val proc = ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encoded,
            ).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().readText()
            val finished = proc.waitFor(20, TimeUnit.SECONDS)
            val ok = finished && proc.exitValue() == 0
            if (!ok) {
                println("[WindowsToast] powershell failed: ${output.trim()}")
                log("runPowerShell: failed (finished=$finished) output=${output.trim()}")
            }
            ok
        }.getOrDefault(false)
    }

    private fun registrationScript(exe: String): String {
        val csharp = """
using System;
using System.Runtime.InteropServices;

public static class AumidSetter {
    [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IPropertyStore {
        [PreserveSig] int GetCount(out uint cProps);
        [PreserveSig] int GetAt(uint iProp, out PropertyKey pkey);
        [PreserveSig] int GetValue(ref PropertyKey key, out PropVariant pv);
        [PreserveSig] int SetValue(ref PropertyKey key, ref PropVariant pv);
        [PreserveSig] int Commit();
    }
    [StructLayout(LayoutKind.Sequential)]
    struct PropertyKey { public Guid fmtid; public uint pid; }
    [StructLayout(LayoutKind.Sequential)]
    struct PropVariant {
        public ushort vt;
        public ushort wReserved1;
        public ushort wReserved2;
        public ushort wReserved3;
        public IntPtr p;
        public int reserved2;
    }
    [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
    static extern int SHGetPropertyStoreFromParsingName(string pszPath, IntPtr pbc, ref Guid riid, out IPropertyStore ppv);

    public static void Set(string lnkPath, string appId) {
        Guid riid = new Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99");
        IPropertyStore store;
        int hr = SHGetPropertyStoreFromParsingName(lnkPath, IntPtr.Zero, ref riid, out store);
        if (hr != 0) throw new Exception("SHGetPropertyStoreFromParsingName failed: 0x" + hr.ToString("X"));
        PropertyKey key = new PropertyKey { fmtid = new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3"), pid = 5 };
        PropVariant pv = new PropVariant { vt = 31 };
        pv.p = Marshal.StringToCoTaskMemUni(appId);
        try {
            hr = store.SetValue(ref key, ref pv);
            if (hr != 0) throw new Exception("SetValue failed: 0x" + hr.ToString("X"));
            hr = store.Commit();
            if (hr != 0) throw new Exception("Commit failed: 0x" + hr.ToString("X"));
        } finally {
            Marshal.FreeCoTaskMem(pv.p);
            Marshal.ReleaseComObject(store);
        }
    }
}
        """.trimIndent()

        return """
${'$'}ErrorActionPreference = 'Stop'
${'$'}exe = '$exe'
${'$'}lnkDir = Join-Path ${'$'}env:APPDATA 'Microsoft\Windows\Start Menu\Programs\VIVI Music'
New-Item -ItemType Directory -Force -Path ${'$'}lnkDir | Out-Null
${'$'}lnk = Join-Path ${'$'}lnkDir 'VIVI Music.lnk'
${'$'}sh = New-Object -ComObject WScript.Shell
${'$'}sc = ${'$'}sh.CreateShortcut(${'$'}lnk)
${'$'}sc.TargetPath = ${'$'}exe
${'$'}sc.WorkingDirectory = Split-Path ${'$'}exe
${'$'}sc.Description = 'VIVI Music DE'
${'$'}sc.Save()
Add-Type -TypeDefinition @'
$csharp
'@
[AumidSetter]::Set(${'$'}lnk, '$AUMID')
        """.trimIndent()
    }

    private fun toastScript(title: String, message: String, section: String?, logo: File?): String {
        val launch = section?.let { "--open=$it" } ?: "--open="
        val logoElement = logo?.let {
            val uri = "file:///" + it.absolutePath.replace('\\', '/').removePrefix("/")
            "<image placement=\"appLogoOverride\" src=\"$uri\" hint-crop=\"circle\"/>"
        } ?: ""
        val xml = "<toast activationType=\"foreground\" launch=\"$launch\" duration=\"short\">" +
            "<visual><binding template=\"ToastGeneric\">" +
            "<text>${escapeXml(title)}</text><text>${escapeXml(message)}</text>$logoElement" +
            "</binding></visual></toast>"

        return """
${'$'}ErrorActionPreference = 'Stop'
[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null
[Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
${'$'}xml = New-Object Windows.Data.Xml.Dom.XmlDocument
${'$'}xml.LoadXml(@'
$xml
'@)
${'$'}toast = New-Object Windows.UI.Notifications.ToastNotification ${'$'}xml
[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('$AUMID').Show(${'$'}toast)
        """.trimIndent()
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
