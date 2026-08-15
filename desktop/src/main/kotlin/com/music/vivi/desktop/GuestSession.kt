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
        YouTube.visitorData = null
        YouTube.visitorData().onSuccess { v ->
            YouTube.visitorData = v
            // Persist so it survives restarts. LoginManager.restore() only acts
            // when logged in, so this never collides with account credentials.
            DesktopSettings.save(DesktopSettings.load().copy(visitorData = v))
        }
    }
}
