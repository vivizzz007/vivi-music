package com.music.vivi.desktop

import com.music.innertube.YouTube
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the desktop's guest (logged-out) YouTube identity.
 *
 * The desktop cannot generate PoTokens (that needs a WebView, which the Android
 * app has but the JVM desktop does not). Like the Android app's
 * `BotDetectionMitigator`, it instead keeps a fresh `visitorData` for guest
 * requests. Without one, YouTube omits the `X-Goog-Visitor-Id` header, flags the
 * requests as bots, and the googlevideo CDN answers 403 on the audio stream.
 */
object GuestSession {
    private val mutex = Mutex()

    @Volatile private var lastRefreshAttemptAt = 0L
    @Volatile private var lastRefreshSucceeded = false

    /**
     * A failed fetch must not be re-attempted on every single play: retrying too
     * eagerly adds a slow network round-trip (and holds the [mutex]) each time.
     */
    private const val REFRESH_COOLDOWN_MS = 30_000L

    /** Ensures a guest visitorData is loaded (persisted value first, then fresh). */
    suspend fun ensure() {
        mutex.withLock {
            if (LoginManager.isLoggedIn()) return@withLock
            if (!YouTube.visitorData.isNullOrBlank()) return@withLock
            val saved = DesktopSettings.load().visitorData
            if (saved.isNotBlank()) {
                YouTube.visitorData = saved
                return@withLock
            }
            refresh()
        }
    }

    /** Clears and re-fetches the guest visitorData (bot-detection rotation). */
    suspend fun rotate() {
        mutex.withLock {
            if (LoginManager.isLoggedIn()) return@withLock
            refresh()
        }
    }

    private suspend fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshAttemptAt < REFRESH_COOLDOWN_MS && !lastRefreshSucceeded) return
        lastRefreshAttemptAt = now
        YouTube.visitorData = null
        val result = YouTube.visitorData()
        lastRefreshSucceeded = result.isSuccess
        result.onSuccess { v ->
            YouTube.visitorData = v
            // Persist so it survives restarts. LoginManager.restore() only acts
            // when logged in, so this never collides with account credentials.
            DesktopSettings.update { it.copy(visitorData = v) }
        }
    }
}
