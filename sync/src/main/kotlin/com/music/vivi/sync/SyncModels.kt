/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.sync

import kotlinx.serialization.Serializable

/**
 * Wire protocol between the client and the sync relay server.
 * A single generic envelope is used for every message; the [type] field
 * selects how the remaining fields are interpreted.
 */
object SyncMessageTypes {
    // Client -> Server
    const val PAIR_REQUEST = "pair_request"
    const val PAIR_JOIN = "pair_join"
    const val SYNC_PUSH = "sync_push"
    const val SYNC_PULL = "sync_pull"
    const val UNPAIR = "unpair"
    const val PING = "ping"

    // Server -> Client
    const val PAIR_CODE = "pair_code"
    const val PAIR_JOINED = "pair_joined"
    const val PAIR_ERROR = "pair_error"
    const val SYNC = "sync"
    const val NO_SNAPSHOT = "no_snapshot"
    const val PONG = "pong"
}

@Serializable
data class SyncEnvelope(
    val type: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val code: String? = null,
    val pairId: String? = null,
    val peerDeviceId: String? = null,
    val peerDeviceName: String? = null,
    val fromDeviceId: String? = null,
    val snapshot: SyncSnapshot? = null,
    val message: String? = null,
)

/**
 * The unit of synchronization: a full snapshot of the shared state of one device.
 * Last-write-wins by [updatedAt]. [settings] is a key -> value map of the shared
 * preferences subset (key names match the Android DataStore preference names).
 */
@Serializable
data class SyncSnapshot(
    val deviceId: String = "",
    val deviceName: String = "",
    val updatedAt: Long = 0L,
    val settings: Map<String, String> = emptyMap(),
    val playback: PlaybackSnapshot? = null,
    val library: LibrarySnapshot? = null,
)

/**
 * Playback / "resume where you left off" state. Queue items are reduced to
 * [TrackRef] (just enough metadata to re-fetch the full track from YouTube).
 */
@Serializable
data class PlaybackSnapshot(
    val trackId: String? = null,
    val trackTitle: String? = null,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val queue: List<TrackRef> = emptyList(),
    val queueIndex: Int = -1,
    val queueTitle: String? = null,
)

@Serializable
data class TrackRef(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val thumbnail: String? = null,
    val durationMs: Long = 0L,
)

/**
 * Library sync payload (liked songs / albums / artists / saved playlists).
 * Reserved for Phase 2 — kept in the schema now so the protocol does not break.
 */
@Serializable
data class LibrarySnapshot(
    val songIds: List<String> = emptyList(),
    val albumIds: List<String> = emptyList(),
    val artistIds: List<String> = emptyList(),
    val playlistIds: List<String> = emptyList(),
)
