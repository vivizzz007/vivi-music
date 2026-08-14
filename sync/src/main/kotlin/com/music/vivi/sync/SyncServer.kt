/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.sync

/**
 * Default relay endpoint for device sync (Android app + desktop edition).
 *
 * This is the live relay deployed at Render (`sync-server/`). Both the Android
 * `DeviceSyncManager` and the desktop settings default to this URL; it can be
 * overridden with `DeviceSyncServerUrlKey` (Android) or the desktop "Relay
 * server" field.
 */
object SyncServer {
    const val DEFAULT_URL = "wss://vivimusic-device-sync.onrender.com"
}
