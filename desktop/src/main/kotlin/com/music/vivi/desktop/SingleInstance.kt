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
}
