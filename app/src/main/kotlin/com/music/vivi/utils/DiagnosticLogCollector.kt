/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.provider.MediaStore
import androidx.media3.datasource.cache.SimpleCache
import androidx.sqlite.db.SupportSQLiteDatabase
import com.music.vivi.BuildConfig
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.PersistentQueueKey
import com.music.vivi.constants.ProxyEnabledKey
import com.music.vivi.constants.ProxyTypeKey
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.playback.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object DiagnosticLogCollector {

    suspend fun collectAndSaveLog(
        context: Context,
        database: MusicDatabase,
        service: MusicService?,
        playerCache: SimpleCache?,
        preRepairFormatCount: Long = -1L,
        preRepairLyricsCount: Long = -1L,
    ): String? {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val timestamp = LocalDateTime.now().format(formatter)
        val fileName = "repairlog-$timestamp.txt"

        // 1. SAFELY extract Player state on the MAIN thread to prevent ExoPlayer thread checks
        val playerStateLines = mutableListOf<String>()
        try {
            withContext(Dispatchers.Main) {
                if (service != null) {
                    playerStateLines.add("MusicService: Active")
                    val player = service.player
                    playerStateLines.add("Player Bound: ${player != null}")
                    if (player != null) {
                        playerStateLines.add("Playback State: ${player.playbackState}")
                        playerStateLines.add("Is Playing: ${player.isPlaying}")
                        playerStateLines.add("Current Media Item: ${player.currentMediaItem?.mediaMetadata?.title ?: "None"}")
                        playerStateLines.add("Current Media Artist: ${player.currentMediaItem?.mediaMetadata?.artist ?: "None"}")
                        playerStateLines.add("Current Media ID: ${player.currentMediaItem?.mediaId ?: "None"}")
                        playerStateLines.add("Current Window Index: ${player.currentMediaItemIndex} of ${player.mediaItemCount}")

                        val playerError = player.playerError
                        if (playerError != null) {
                            playerStateLines.add("Active Streaming/Player Error: YES")
                            playerStateLines.add("  Error Code: ${playerError.errorCode} (${playerError.errorCodeName})")
                            playerStateLines.add("  Error Message: ${playerError.message}")
                            playerStateLines.add("  Error Cause: ${playerError.cause?.message ?: playerError.cause}")
                        } else {
                            playerStateLines.add("Active Streaming/Player Error: None")
                        }

                        val totalItems = player.mediaItemCount
                        if (totalItems > 0) {
                            playerStateLines.add("")
                            playerStateLines.add("--- Current Queue Snapshot (surrounding playing item) ---")
                            val currentIndex = player.currentMediaItemIndex
                            val startIndex = (currentIndex - 5).coerceAtLeast(0)
                            val endIndex = (currentIndex + 10).coerceAtMost(totalItems - 1)
                            for (i in startIndex..endIndex) {
                                val item = player.getMediaItemAt(i)
                                val pointer = if (i == currentIndex) " -> [*CURRENT*]" else "    "
                                val title = item.mediaMetadata.title ?: "Unknown Title"
                                val artist = item.mediaMetadata.artist ?: "Unknown Artist"
                                playerStateLines.add("$pointer [$i] $title - $artist (id: ${item.mediaId})")
                            }
                        }
                    }
                } else {
                    playerStateLines.add("MusicService: Inactive / Not bound")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading player state on Main thread")
            playerStateLines.add("Error reading player state: ${e.message}")
        }

        // 2. Perform DB queries, logcat gathering, and file saving on Dispatchers.IO
        return withContext(Dispatchers.IO) {
            try {
                val sb = StringBuilder()
                sb.appendLine("==================================================")
                sb.appendLine("       VIVIMUSIC REPAIR & DIAGNOSTIC REPORT       ")
                sb.appendLine("==================================================")
                sb.appendLine("Timestamp: ${LocalDateTime.now()}")
                sb.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                sb.appendLine("Flavor: ${BuildConfig.FLAVOR}")
                sb.appendLine()

                sb.appendLine("----------------- DEVICE & OS -----------------")
                sb.appendLine("Manufacturer: ${Build.MANUFACTURER}")
                sb.appendLine("Brand: ${Build.BRAND}")
                sb.appendLine("Model: ${Build.MODEL} (${Build.DEVICE})")
                sb.appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                sb.appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                sb.appendLine()

                sb.appendLine("---------------- MEMORY & STORAGE --------------")
                val runtime = Runtime.getRuntime()
                val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
                val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
                val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
                val usedMemoryMb = totalMemoryMb - freeMemoryMb
                sb.appendLine("Heap Memory: Used ${usedMemoryMb}MB / Total ${totalMemoryMb}MB (Max ${maxMemoryMb}MB)")

                val internalDir = context.filesDir
                val freeSpaceMb = internalDir.freeSpace / (1024 * 1024)
                val totalSpaceMb = internalDir.totalSpace / (1024 * 1024)
                sb.appendLine("Internal Storage: Free ${freeSpaceMb}MB / Total ${totalSpaceMb}MB")
                sb.appendLine()

                sb.appendLine("----------- NETWORK & CONNECTIVITY -------------")
                try {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val activeNetwork = cm?.activeNetwork
                    val caps = cm?.getNetworkCapabilities(activeNetwork)
                    val transport = when {
                        caps == null -> "Offline / None"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile Data"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Transport"
                        else -> "Other"
                    }
                    sb.appendLine("Active Network Transport: $transport")
                    val isVpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                    sb.appendLine("VPN Active: ${if (isVpnActive) "Yes" else "No"}")
                    val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                    sb.appendLine("Internet Validated: ${if (isValidated) "Yes" else "No"}")
                    val isMetered = cm?.isActiveNetworkMetered == true
                    sb.appendLine("Metered Network: ${if (isMetered) "Yes" else "No"}")
                } catch (e: Exception) {
                    sb.appendLine("Network inspection error: ${e.message}")
                }

                try {
                    val prefs = withTimeoutOrNull(600L) { context.dataStore.data.first() }
                    val proxyEnabled = prefs?.get(ProxyEnabledKey) ?: false
                    val proxyType = prefs?.get(ProxyTypeKey) ?: "None"
                    sb.appendLine("Proxy Configured: ${if (proxyEnabled) "Enabled ($proxyType)" else "Disabled"}")
                } catch (e: Exception) {
                    sb.appendLine("Proxy inspection error: ${e.message}")
                }
                sb.appendLine()

                sb.appendLine("----------- SYSTEM & BATTERY RESTRICTIONS ------")
                try {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isIgnoringBatteryOptimizations = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                    sb.appendLine("Ignoring Battery Optimizations: ${if (isIgnoringBatteryOptimizations) "Yes (Unrestricted)" else "No (System may throttle/kill background playback)"}")
                } catch (e: Exception) {
                    sb.appendLine("Battery status error: ${e.message}")
                }
                sb.appendLine()

                sb.appendLine("----------- CONFIGURATION & ACCOUNT STATE ------")
                try {
                    val prefs = withTimeoutOrNull(600L) { context.dataStore.data.first() }
                    val audioQuality = prefs?.get(AudioQualityKey) ?: "Auto"
                    val audioNorm = prefs?.get(AudioNormalizationKey) ?: true
                    val skipSilence = prefs?.get(SkipSilenceKey) ?: false
                    val persistentQueue = prefs?.get(PersistentQueueKey) ?: true
                    sb.appendLine("Audio Quality: $audioQuality")
                    sb.appendLine("Audio Normalization: ${if (audioNorm) "Enabled" else "Disabled"}")
                    sb.appendLine("Skip Silence: ${if (skipSilence) "Enabled" else "Disabled"}")
                    sb.appendLine("Persistent Queue: ${if (persistentQueue) "Enabled" else "Disabled"}")

                    val cookie = prefs?.get(InnerTubeCookieKey)
                    val isLoggedIn = !cookie.isNullOrBlank()
                    sb.appendLine("YouTube Logged In: ${if (isLoggedIn) "Yes" else "No"}")
                    sb.appendLine("Session Cookie Present: ${if (!cookie.isNullOrBlank()) "Yes" else "No"}")
                    val visitorData = prefs?.get(VisitorDataKey)
                    sb.appendLine("Visitor Data Present: ${if (!visitorData.isNullOrBlank()) "Yes" else "No"}")
                } catch (e: Exception) {
                    sb.appendLine("Configuration error: ${e.message}")
                }
                sb.appendLine()

                sb.appendLine("------------- PLAYBACK & SERVICE STATE ---------")
                playerStateLines.forEach { sb.appendLine(it) }
                sb.appendLine()
                if (playerCache != null) {
                    sb.appendLine("PlayerCache Cached Keys: ${playerCache.keys.size}")
                    val cacheSpaceMb = playerCache.cacheSpace / (1024 * 1024)
                    sb.appendLine("PlayerCache Size: ${cacheSpaceMb}MB")
                }
                sb.appendLine()

                sb.appendLine("-------------- DATABASE SUMMARY ----------------")
                try {
                    val db = database.openHelper.readableDatabase
                    sb.appendLine("Total Songs: ${getTableCount(db, "song")}")
                    val downloadedSongs = try {
                        val cursor = db.query("SELECT COUNT(*) FROM song WHERE isDownloaded = 1")
                        cursor.use { if (it.moveToFirst()) it.getLong(0) else 0L }
                    } catch (e: Exception) { -1L }
                    sb.appendLine("Total Downloaded Songs: $downloadedSongs")
                    sb.appendLine("Total Playlists: ${getTableCount(db, "playlist")}")
                    sb.appendLine("Total Artists: ${getTableCount(db, "artist")}")
                    sb.appendLine("Total Albums: ${getTableCount(db, "album")}")
                    sb.appendLine("Search History Entries: ${getTableCount(db, "search_history")}")
                    val formatCount = if (preRepairFormatCount >= 0) preRepairFormatCount else getTableCount(db, "format")
                    val lyricsCount = if (preRepairLyricsCount >= 0) preRepairLyricsCount else getTableCount(db, "lyrics")
                    sb.appendLine("Pre-repair Format Cache Entries: $formatCount")
                    sb.appendLine("Pre-repair Lyrics Cache Entries: $lyricsCount")
                } catch (e: Exception) {
                    sb.appendLine("Database query error: ${e.message}")
                }
                sb.appendLine()

                sb.appendLine("------------- REPAIR ACTIONS APPLIED -----------")
                sb.appendLine("[x] Flushed MusicService in-memory stream URLs, retries & failed song blacklist")
                sb.appendLine("[x] Purged ExoPlayer SimpleCache audio chunks")
                sb.appendLine("[x] Purged SQLite format metadata cache")
                sb.appendLine("[x] Purged SQLite lyrics cache")
                sb.appendLine("[x] Cleared Coil image memory & disk cache")
                sb.appendLine("[x] Cleared search history")
                sb.appendLine()

                sb.appendLine("------------- PROCESS LOGCAT (RECENT) ----------")
                val logLines = captureProcessLogcat()
                if (logLines.isNotEmpty()) {
                    logLines.forEach { line ->
                        sb.appendLine(sanitizeLog(line))
                    }
                } else {
                    sb.appendLine("No logcat entries available for this process.")
                }
                sb.appendLine()
                sb.appendLine("================== END OF REPORT =================")

                val reportText = sb.toString()
                val savedResult = saveToVivimusicFolder(context, "repairtool", fileName, reportText)
                if (savedResult != null) fileName else null
            } catch (e: Exception) {
                Timber.e(e, "Failed to assemble and save diagnostic report")
                null
            }
        }
    }

    private fun getTableCount(db: SupportSQLiteDatabase, table: String): Long {
        return try {
            val cursor = db.query("SELECT COUNT(*) FROM `$table`")
            cursor.use {
                if (it.moveToFirst()) it.getLong(0) else 0L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    private fun captureProcessLogcat(): List<String> {
        val lines = mutableListOf<String>()
        try {
            val pid = Process.myPid()
            val process = ProcessBuilder("logcat", "-d", "-v", "time", "-t", "300", "--pid", pid.toString())
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(1200, TimeUnit.MILLISECONDS)
            if (finished) {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { lines.add(it) }
                    if (lines.size >= 300) break
                }
                reader.close()
            } else {
                process.destroyForcibly()
                lines.add("Logcat capture timed out after 1200ms")
            }
        } catch (e: Exception) {
            Timber.w(e, "DiagnosticLogCollector: Process logcat error")
            lines.add("Process logcat unavailable: ${e.message}")
        }
        return lines
    }

    private fun sanitizeLog(line: String): String {
        return line
            .replace(Regex("(?i)(authorization:\\s*Bearer\\s+)[A-Za-z0-9._~+/-]+"), "$1[REDACTED]")
            .replace(Regex("(?i)(visitorData=)[^&\\s]+"), "$1[REDACTED]")
            .replace(Regex("(?i)(access_token=)[^&\\s]+"), "$1[REDACTED]")
            .replace(Regex("(?i)(SAPISID=[^;\\s]+)"), "SAPISID=[REDACTED]")
    }

    fun saveToVivimusicFolder(
        context: Context,
        subfolder: String = "repairtool",
        fileName: String,
        content: String
    ): String? {
        val targetRelativePaths = listOf(
            "Download/vivimusic/$subfolder",
            "Download/vivimusic"
        )
        var savedPath: String? = null

        // 1. MediaStore on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            for (relPath in targetRelativePaths) {
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, relPath)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                        savedPath = "$relPath/$fileName"
                        Timber.d("DiagnosticLogCollector: MediaStore saved -> $uri at $relPath")
                        break
                    }
                } catch (e: Exception) {
                    Timber.w(e, "DiagnosticLogCollector: MediaStore failed for $relPath")
                }
            }
        }

        // 2. Direct File write fallback on public Downloads folder
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subDirs = listOf(
                File(downloadsDir, "vivimusic/$subfolder"),
                File(downloadsDir, "vivimusic")
            )
            for (dir in subDirs) {
                try {
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = File(dir, fileName)
                    file.writeText(content, Charsets.UTF_8)
                    MediaScannerConnection.scanFile(
                        context.applicationContext,
                        arrayOf(file.absolutePath),
                        arrayOf("text/plain"),
                        null
                    )
                    if (savedPath == null) {
                        savedPath = file.absolutePath
                    }
                    Timber.d("DiagnosticLogCollector: Direct file write succeeded -> ${file.absolutePath}")
                    break
                } catch (e: Exception) {
                    Timber.w(e, "DiagnosticLogCollector: Direct write failed for ${dir.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "DiagnosticLogCollector: Direct filesystem access error")
        }

        return savedPath
    }
}
