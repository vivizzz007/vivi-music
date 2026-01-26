package com.music.vivi.extensions

/**
 * Executes the given block and returns its result, or null if an exception occurs.
 * Useful for non-critical operations where failure is acceptable.
 */
fun <T> tryOrNull(block: () -> T): T? = try {
    block()
} catch (e: Exception) {
    null
}
