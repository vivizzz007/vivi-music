package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.GraphicsEnvironment
import java.io.File
import java.lang.management.ManagementFactory

/** Where the live dev-tools stats are shown. */
enum class DevToolsMode { OVERLAY, WINDOW }

/**
 * Developer options gate (enabled by tapping the About "version code" seven
 * times). Persisted in [DesktopSettings] and shared across the whole app so
 * both the overlay (main window) and the dedicated window can react to it.
 */
object DeveloperOptions {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _mode = MutableStateFlow(DevToolsMode.OVERLAY)
    val mode: StateFlow<DevToolsMode> = _mode.asStateFlow()

    fun load() {
        val s = DesktopSettings.load()
        _enabled.value = s.developerOptions
        _mode.value = runCatching { DevToolsMode.valueOf(s.devToolsMode) }.getOrDefault(DevToolsMode.OVERLAY)
        if (_enabled.value) SystemMonitor.start() else SystemMonitor.stop()
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        DesktopSettings.save(DesktopSettings.load().copy(developerOptions = value))
        if (value) SystemMonitor.start() else SystemMonitor.stop()
    }

    fun setMode(value: DevToolsMode) {
        _mode.value = value
        DesktopSettings.save(DesktopSettings.load().copy(devToolsMode = value.name))
    }
}

/** One snapshot of the metrics shown by the dev tools. */
data class SystemStats(
    val cpuProcess: Double,       // 0..1, or -1 when unknown
    val cpuSystem: Double,        // 0..1, or -1 when unknown
    val heapUsedBytes: Long,
    val heapMaxBytes: Long,
    val sysRamUsedBytes: Long,    // -1 when unknown
    val sysRamTotalBytes: Long,   // -1 when unknown
    val threadCount: Int,
    val netDownBps: Long,         // -1 when unknown
    val netUpBps: Long,           // -1 when unknown
    val netDownTotalBytes: Long,  // -1 when unknown
    val netUpTotalBytes: Long,    // -1 when unknown
    val availableProcessors: Int,
    val uptimeMs: Long,
    val osName: String,
    val javaVersion: String,
    val gpuDevice: String,
)

/**
 * Samples JVM + OS metrics on a fixed interval and exposes the latest snapshot
 * as a [StateFlow]. CPU / memory come from the JDK's [com.sun.management.OperatingSystemMXBean];
 * network totals are read best-effort from the OS (see [readNetworkTotals]);
 * GPU usage is not measurable cross-platform, so only the device id is shown.
 */
object SystemMonitor {
    private val osBean: com.sun.management.OperatingSystemMXBean? =
        ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean

    private val threadBean = ManagementFactory.getThreadMXBean()
    private val runtimeBean = ManagementFactory.getRuntimeMXBean()
    private val runtime = Runtime.getRuntime()

    // Declared before `_stats` on purpose: `emptyStats()` reads this during the
    // object's initialization, and reading a Kotlin `val` before its initializer
    // has run throws a NullPointerException at startup (the packaged launcher
    // then reports it as "Failed to launch JVM").
    val gpuDevice: String = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.getIDstring()
    }.getOrDefault("—")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _stats = MutableStateFlow(emptyStats())
    val stats: StateFlow<SystemStats> = _stats.asStateFlow()

    private var job: Job? = null

    private var lastRx = -1L
    private var lastTx = -1L
    private var lastSampleMs = 0L

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (true) {
                sample()
                delay(2000L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun emptyStats() = SystemStats(
        cpuProcess = -1.0, cpuSystem = -1.0,
        heapUsedBytes = 0, heapMaxBytes = 0,
        sysRamUsedBytes = -1, sysRamTotalBytes = -1,
        threadCount = 0,
        netDownBps = -1, netUpBps = -1, netDownTotalBytes = -1, netUpTotalBytes = -1,
        availableProcessors = runtime.availableProcessors(),
        uptimeMs = 0,
        osName = System.getProperty("os.name").orEmpty(),
        javaVersion = System.getProperty("java.version").orEmpty(),
        gpuDevice = gpuDevice,
    )

    private fun sample() {
        val cpuProcess = osBean?.processCpuLoad?.takeIf { it >= 0 } ?: -1.0
        val cpuSystem = osBean?.systemCpuLoad?.takeIf { it >= 0 } ?: -1.0
        val heapUsed = runtime.totalMemory() - runtime.freeMemory()
        val heapMax = runtime.maxMemory()
        val ramTotal = osBean?.totalMemorySize ?: -1
        val ramFree = osBean?.freeMemorySize ?: -1
        val ramUsed = if (ramTotal >= 0 && ramFree >= 0) ramTotal - ramFree else -1
        val threads = threadBean.threadCount

        val (rx, tx) = readNetworkTotals()
        val now = System.currentTimeMillis()
        var downBps = -1L
        var upBps = -1L
        if (lastRx >= 0 && rx >= 0 && tx >= 0 && now > lastSampleMs) {
            val dtSec = (now - lastSampleMs) / 1000.0
            if (dtSec > 0) {
                downBps = ((rx - lastRx) / dtSec).toLong().coerceAtLeast(0)
                upBps = ((tx - lastTx) / dtSec).toLong().coerceAtLeast(0)
            }
        }
        lastRx = rx
        lastTx = tx
        lastSampleMs = now

        _stats.value = SystemStats(
            cpuProcess = cpuProcess,
            cpuSystem = cpuSystem,
            heapUsedBytes = heapUsed,
            heapMaxBytes = heapMax,
            sysRamUsedBytes = ramUsed,
            sysRamTotalBytes = ramTotal,
            threadCount = threads,
            netDownBps = downBps,
            netUpBps = upBps,
            netDownTotalBytes = rx,
            netUpTotalBytes = tx,
            availableProcessors = runtime.availableProcessors(),
            uptimeMs = runtimeBean.uptime,
            osName = System.getProperty("os.name").orEmpty(),
            javaVersion = System.getProperty("java.version").orEmpty(),
            gpuDevice = gpuDevice,
        )
    }

    private fun readNetworkTotals(): Pair<Long, Long> = runCatching {
        when (Platform.os) {
            DesktopOs.LINUX -> linuxNetTotals()
            DesktopOs.MACOS -> macNetTotals()
            DesktopOs.WINDOWS -> windowsNetTotals()
        }
    }.getOrDefault(-1L to -1L)

    private fun linuxNetTotals(): Pair<Long, Long> {
        var rx = 0L
        var tx = 0L
        File("/proc/net/dev").readLines().drop(2).forEach { line ->
            val idx = line.indexOf(':')
            if (idx < 0) return@forEach
            val iface = line.substring(0, idx).trim()
            if (iface == "lo") return@forEach
            val fields = line.substring(idx + 1).trim().split(Regex("\\s+"))
            if (fields.size >= 9) {
                rx += fields[0].toLongOrNull() ?: 0L
                tx += fields[8].toLongOrNull() ?: 0L
            }
        }
        return rx to tx
    }

    private fun macNetTotals(): Pair<Long, Long> {
        val out = ProcessBuilder("netstat", "-ib").redirectErrorStream(true)
            .start().inputStream.bufferedReader().readText()
        var rx = 0L
        var tx = 0L
        var rxIdx = -1
        var txIdx = -1
        for (line in out.lines()) {
            if (line.contains("Ibytes") && line.contains("Obytes")) {
                val header = line.split(Regex("\\s+"))
                rxIdx = header.indexOfFirst { it == "Ibytes" }
                txIdx = header.indexOfFirst { it == "Obytes" }
                continue
            }
            if (rxIdx < 0) continue
            val fields = line.split(Regex("\\s+"))
            if (fields.size > maxOf(rxIdx, txIdx)) {
                rx += fields.getOrNull(rxIdx)?.toLongOrNull() ?: 0L
                tx += fields.getOrNull(txIdx)?.toLongOrNull() ?: 0L
            }
        }
        return rx to tx
    }

    private fun windowsNetTotals(): Pair<Long, Long> {
        val out = ProcessBuilder("netstat", "-e").redirectErrorStream(true)
            .start().inputStream.bufferedReader().readText()
        var rx = -1L
        var tx = -1L
        for (line in out.lines()) {
            if (line.trim().startsWith("Bytes")) {
                val m = Regex("(\\d+)\\s+(\\d+)").find(line)
                if (m != null) {
                    rx = m.groupValues[1].toLongOrNull() ?: -1L
                    tx = m.groupValues[2].toLongOrNull() ?: -1L
                }
            }
        }
        return rx to tx
    }
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    if (kb / 1024.0 < 1024) return "%.1f MB".format(kb / 1024.0)
    return "%.2f GB".format(kb / 1024.0 / 1024.0)
}

private fun formatSpeed(bps: Long): String = if (bps < 0) "—" else "${formatBytes(bps)}/s"

private fun pct(x: Double): String = if (x < 0) "—" else "%.1f%%".format(x * 100)

private fun formatUptime(ms: Long): String {
    if (ms < 0) return "—"
    val totalSec = ms / 1000
    val d = totalSec / 86400
    val h = (totalSec % 86400) / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (d > 0) "${d}d ${h}h" else if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/** Full dev-tools content, shared by the overlay and the dedicated window. */
@Composable
fun DevToolsPanel(syncManager: DesktopSyncManager?, language: String) {
    val stats by SystemMonitor.stats.collectAsState()
    val peerName = syncManager?.peerDeviceName?.collectAsState()?.value.orEmpty()
    val paired = syncManager?.paired?.collectAsState()?.value == true

    Column(Modifier.padding(12.dp)) {
        Text(Localization.get(language, "developer_options"), style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        StatRow(
            "${Localization.get(language, "cpu")} · ${Localization.get(language, "process")}",
            pct(stats.cpuProcess),
        )
        StatRow(
            "${Localization.get(language, "cpu")} · ${Localization.get(language, "system")}",
            pct(stats.cpuSystem),
        )
        StatRow(
            "${Localization.get(language, "memory")} · ${Localization.get(language, "heap")}",
            "${formatBytes(stats.heapUsedBytes)} / ${formatBytes(stats.heapMaxBytes)}",
        )
        StatRow(
            "${Localization.get(language, "memory")} · ${Localization.get(language, "system")}",
            if (stats.sysRamTotalBytes >= 0) {
                "${formatBytes(stats.sysRamUsedBytes)} / ${formatBytes(stats.sysRamTotalBytes)}"
            } else "—",
        )
        StatRow(
            Localization.get(language, "gpu"),
            stats.gpuDevice.ifBlank { "—" },
        )
        StatRow(
            "${Localization.get(language, "network")} ↓",
            formatSpeed(stats.netDownBps),
        )
        StatRow(
            "${Localization.get(language, "network")} ↑",
            formatSpeed(stats.netUpBps),
        )
        StatRow(
            Localization.get(language, "total_traffic"),
            "↓ ${formatBytes(stats.netDownTotalBytes)} · ↑ ${formatBytes(stats.netUpTotalBytes)}",
        )
        StatRow(
            Localization.get(language, "paired_device"),
            if (paired && peerName.isNotBlank()) peerName else Localization.get(language, "no_paired_device"),
        )
        StatRow(Localization.get(language, "threads"), stats.threadCount.toString())
        StatRow(Localization.get(language, "uptime"), formatUptime(stats.uptimeMs))
        StatRow(
            Localization.get(language, "system_info"),
            "${stats.osName} · Java ${stats.javaVersion} · ${stats.availableProcessors} cores",
        )
    }
}

/**
 * Compact, non-invasive overlay shown in a corner of the main window. Collapses
 * to a small pill and can be expanded to reveal the full [DevToolsPanel].
 */
@Composable
fun DevToolsOverlay(syncManager: DesktopSyncManager?, language: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val stats by SystemMonitor.stats.collectAsState()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "DEV",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "CPU ${pct(stats.cpuProcess)} · RAM ${formatBytes(stats.heapUsedBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (expanded) "−" else "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                DevToolsPanel(syncManager, language)
            }
        }
    }
}
