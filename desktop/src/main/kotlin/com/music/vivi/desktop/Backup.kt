package com.music.vivi.desktop

import com.music.vivi.sync.SyncedPlaylist
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Comprehensive backup/restore for the desktop edition.
 *
 * A backup is a ZIP archive with two entries:
 *   - `settings.json`  -> the full [DesktopSyncState] (settings, library,
 *     account/login, dev options, update + notification prefs, …).
 *   - `playlists.json` -> the local playlists ([SyncedPlaylist] list).
 *
 * This mirrors the mobile app's approach (which zips its datastore + Room DB).
 * Backups use the `.vivide.backup` extension. Old single-JSON backups produced
 * by the previous `exportSettings` (`.backup`) are still recognized on import.
 */
object BackupManager {
    private const val SETTINGS_ENTRY = "settings.json"
    private const val PLAYLISTS_ENTRY = "playlists.json"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    /** Auto backups live under `~/.vivimusic/backups/`. */
    private val autoDir: File = File(System.getProperty("user.home"), ".vivimusic/backups").apply { mkdirs() }

    private val timestamp: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    // ------------------------------------------------------------------
    // Manual export / import
    // ------------------------------------------------------------------

    /** Writes a full backup (settings + playlists) to [file]. */
    fun export(file: File): Boolean = runCatching {
        ZipOutputStream(FileOutputStream(file).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
            zip.write(json.encodeToString(DesktopSyncState.serializer(), DesktopSettings.load()).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(PLAYLISTS_ENTRY))
            val playlists = json.encodeToString(ListSerializer(SyncedPlaylist.serializer()), PlaylistStore.toSynced())
            zip.write(playlists.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        true
    }.getOrDefault(false)

    /**
     * Restores a backup from [file]. Accepts the new ZIP format or a legacy
     * bare-JSON settings file. The current device id and first-launch date are
     * preserved, and any stale pairing is dropped.
     */
    fun import(file: File): Boolean = runCatching {
        if (isZip(file)) importZip(file) else importLegacy(file)
        true
    }.getOrDefault(false)

    private fun isZip(file: File): Boolean = runCatching {
        FileInputStream(file).use { input ->
            val header = ByteArray(2)
            input.read(header) == 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
        }
    }.getOrDefault(false)

    private fun importZip(file: File) {
        ZipInputStream(FileInputStream(file).buffered()).use { zip ->
            var entry = zip.nextEntry
            var found = false
            while (entry != null) {
                val bytes = zip.readBytes()
                when (entry.name) {
                    SETTINGS_ENTRY -> {
                        val imported = json.decodeFromString(DesktopSyncState.serializer(), bytes.decodeToString())
                        applyImportedSettings(imported)
                        found = true
                    }
                    PLAYLISTS_ENTRY -> {
                        val playlists = json.decodeFromString(ListSerializer(SyncedPlaylist.serializer()), bytes.decodeToString())
                        PlaylistStore.replaceAll(playlists)
                        found = true
                    }
                }
                entry = zip.nextEntry
            }
            if (!found) throw IllegalStateException("No backup entries found in archive")
        }
    }

    private fun importLegacy(file: File) {
        val imported = json.decodeFromString(DesktopSyncState.serializer(), file.readText())
        applyImportedSettings(imported)
    }

    /** Preserves this machine's identity + first-launch date, drops pairing. */
    private fun applyImportedSettings(imported: DesktopSyncState) {
        DesktopSettings.update { current ->
            imported.copy(
                deviceId = current.deviceId,
                firstLaunchDate = current.firstLaunchDate,
                pairId = "",
            )
        }
    }

    // ------------------------------------------------------------------
    // Automatic backups
    // ------------------------------------------------------------------

    /** Runs an automatic backup of [type] (`weekly` / `before_update` / …). */
    fun autoBackup(type: String): File? = runCatching {
        val file = File(autoDir, "auto_backup_${type}_$timestamp.vivide.backup")
        if (export(file)) {
            cleanup(type)
            file
        } else {
            null
        }
    }.getOrNull()

    /** Lists automatic backups, newest first. */
    fun listAuto(): List<File> = autoDir.listFiles { f ->
        f.isFile && f.name.startsWith("auto_backup_") && f.name.endsWith(".vivide.backup")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun deleteAuto(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    /**
     * Runs the scheduled automatic backup if the weekly toggle is on and the
     * most recent weekly backup is older than 7 days. Called at startup and on
     * a periodic tick so backups happen even without a reboot.
     */
    fun maybeRunScheduled() {
        val s = DesktopSettings.load()
        if (!s.autoBackupEnabled || !s.autoBackupWeekly) return
        val last = listAuto()
            .filter { it.name.contains("_weekly_") }
            .maxOfOrNull { it.lastModified() }
        val now = System.currentTimeMillis()
        if (last == null || now - last >= 7L * 24 * 3600 * 1000) {
            autoBackup("weekly")
        }
    }

    /** Keeps the 5 newest backups of [type]; older ones are deleted. */
    private fun cleanup(type: String) {
        val backups = listAuto().filter { it.name.startsWith("auto_backup_${type}_") }
        if (backups.size > 5) {
            backups.drop(5).forEach { runCatching { it.delete() } }
        }
    }
}
