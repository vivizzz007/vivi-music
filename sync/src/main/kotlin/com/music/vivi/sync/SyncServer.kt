/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.sync

/**
 * Default relay endpoint for device sync (Android app + desktop edition).
 *
 * NOTE: `sync-server/` must be deployed first (Render / Hugging Face). This
 * hostname currently has NO running service (Render answers with
 * "x-render-routing: no-server"), so pairing fails with "Connection failed"
 * until the relay is actually deployed. Once live, replace this value with the
 * real generated URL. It can also be overridden at runtime:
 * `DeviceSyncServerUrlKey` (Android) or the desktop "Relay server" field.
 */
object SyncServer {
    const val DEFAULT_URL = "wss://vivimusic-device-sync.onrender.com"
}
