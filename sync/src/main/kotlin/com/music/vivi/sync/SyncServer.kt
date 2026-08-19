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

    /**
     * While a player is playing, re-push its position every this many millis so
     * the paired device auto-corrects drift (buffering / clock skew) instead of
     * waiting for the next discrete seek/play/track event.
     */
    const val RESYNC_TICK_MS = 5_000L

    /**
     * A received position within this tolerance of the local position is
     * treated as already in-sync: the seek is skipped (only play/pause is
     * applied) so periodic re-sync ticks don't cause audible seek glitches.
     *
     * 250ms proved too tight: on a jittery phone hotspot the estimated relay
     * clock offset fluctuates by hundreds of ms, so the follower kept jumping
     * forward every tick. 1s matches the "sync to the second" target and only
     * corrects genuine drift, not offset-measurement noise.
     */
    const val RESYNC_TOLERANCE_MS = 1000L
}
