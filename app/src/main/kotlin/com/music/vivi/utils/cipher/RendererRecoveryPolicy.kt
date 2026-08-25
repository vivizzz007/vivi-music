/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils.cipher

class CipherRendererGoneException(message: String) : Exception(message)

class RendererRecoveryPolicy(
    private val maxConsecutiveFailures: Int = DEFAULT_MAX_CONSECUTIVE_FAILURES,
    private val backoffMs: Long = DEFAULT_BACKOFF_MS,
) {
    var consecutiveFailures: Int = 0
        private set

    var backoffUntilMs: Long = 0L
        private set

    fun shouldAttempt(nowMs: Long): Boolean =
        consecutiveFailures < maxConsecutiveFailures || nowMs >= backoffUntilMs

    fun onSuccess() {
        consecutiveFailures = 0
        backoffUntilMs = 0L
    }

    fun onFailure(nowMs: Long) {
        consecutiveFailures++
        if (consecutiveFailures >= maxConsecutiveFailures) {
            backoffUntilMs = nowMs + backoffMs
        }
    }

    companion object {
        const val DEFAULT_MAX_CONSECUTIVE_FAILURES = 3
        const val DEFAULT_BACKOFF_MS = 60_000L
    }
}
