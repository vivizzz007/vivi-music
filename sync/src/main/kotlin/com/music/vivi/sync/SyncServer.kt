/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.sync

/**
 * Default relay endpoint for device sync.
 *
 * NOTE: this is a placeholder. Deploy the server in `sync-server/` to your own
 * Render / Hugging Face account and point `DeviceSyncServerUrlKey` (Android) or
 * the desktop settings to your real URL. See `sync-server/README.md`.
 */
object SyncServer {
    const val DEFAULT_URL = "wss://vivimusic-device-sync.onrender.com"
}
