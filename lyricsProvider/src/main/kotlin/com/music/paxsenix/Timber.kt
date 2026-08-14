package com.music.paxsenix

/**
 * Minimal JVM-compatible logging shim.
 *
 * The lyricsProvider module originally depended on `com.jakewharton.timber:timber`,
 * which is an Android AAR (it references `android.util.Log`) and therefore cannot be
 * consumed by a plain JVM / desktop target. This object mirrors the small subset of
 * the Timber API used by Paxsenix so the module compiles on both Android and desktop.
 */
internal object Timber {
    fun d(message: String) = println("D/Paxsenix: $message")
    fun v(message: String) = println("V/Paxsenix: $message")
    fun w(message: String) = println("W/Paxsenix: $message")
    fun e(message: String) = System.err.println("E/Paxsenix: $message")
    fun e(t: Throwable, message: String) {
        System.err.println("E/Paxsenix: $message")
        t.printStackTrace()
    }
}
