package com.music.vivi.desktop

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/**
 * Cross-platform single-instance guard.
 *
 * Holds an exclusive OS-level file lock for the lifetime of the process. A
 * second launch (or one racing the first during startup) fails to acquire the
 * lock and exits, always keeping the instance that started first. The lock is
 * released automatically by the OS when the owning process exits, so no
 * cleanup is required on normal or abnormal termination.
 */
object SingleInstance {
    private var raf: RandomAccessFile? = null
    private var lock: FileLock? = null

    /**
     * @return true if this process acquired the lock (it is the first/only
     * instance), false if another instance already holds it.
     */
    fun acquire(): Boolean {
        return try {
            val lockFile = File(System.getProperty("user.home"), ".vivimusic/app.lock")
            lockFile.parentFile?.mkdirs()
            val file = RandomAccessFile(lockFile, "rw")
            val acquired = file.channel.tryLock()
            if (acquired == null) {
                // Another process already holds the lock → a running instance exists.
                file.close()
                false
            } else {
                raf = file
                lock = acquired
                true
            }
        } catch (_: Exception) {
            // Best-effort guard: if locking fails for an unexpected reason (e.g.
            // unsupported filesystem), allow startup rather than blocking the app.
            true
        }
    }

    /** Releases the held lock so a restarting instance can acquire it. */
    fun release() {
        runCatching { lock?.release() }
        runCatching { raf?.close() }
        lock = null
        raf = null
    }
}

/**
 * File-based command mailbox between instances: a second launch that loses the
 * single-instance lock writes its intent here, and the running instance polls
 * it (e.g. a toast click passes `--open=updates` to open the Updates screen).
 */
object AppCommand {
    private const val PREFIX = "--open="
    private val file = File(System.getProperty("user.home"), ".vivimusic/open-command.txt")

    /** Extracts the `--open=<section>` argument from the process args, if any. */
    fun parse(args: Array<String>): String? =
        args.firstOrNull { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.takeIf { it.isNotBlank() }

    /** Writes a command for the running instance to pick up. */
    fun write(section: String): Boolean = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(section)
    }.isSuccess

    /** Reads and clears a pending command, or null if there is none. */
    fun poll(): String? {
        if (!file.exists()) return null
        return runCatching {
            val value = file.readText().trim()
            file.delete()
            value.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

/**
 * Relaunches the app and exits the current process (used by "Restart now"
 * after restoring a backup). The single-instance lock is released first so the
 * new process isn't rejected as a duplicate of the still-running one.
 */
fun restartApplication() {
    val command = relaunchCommand()
    SingleInstance.release()
    if (command.isNotEmpty()) {
        runCatching { ProcessBuilder(command).start() }
    }
    kotlin.system.exitProcess(0)
}

private fun relaunchCommand(): List<String> {
    // Packaged (jpackage) app: the launcher exposes its own executable path via
    // jpackage.app-path — relaunch it directly.
    val appPath = System.getProperty("jpackage.app-path").orEmpty()
    if (appPath.isNotBlank()) return listOf(appPath)
    // Dev / IDE run: fall back to a fresh JVM with the same classpath.
    val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
    val classpath = System.getProperty("java.class.path").orEmpty()
    if (classpath.isBlank()) return emptyList()
    return listOf(javaBin, "-cp", classpath, "com.music.vivi.desktop.MainKt")
}
