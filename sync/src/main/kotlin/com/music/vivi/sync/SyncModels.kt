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
    /** Sender's clock (epoch millis) when this envelope was produced (PING/PONG). */
    val timestampMs: Long? = null,
    /** Echo of the PING's [timestampMs] back to the client, so it can measure RTT. */
    val echoTimestampMs: Long? = null,
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
    /**
     * The sender's clock (epoch millis, expressed in the shared relay-time
     * reference frame via the estimated clock offset) at the moment [positionMs]
     * was sampled. The receiver uses it to extrapolate the live position while
     * playing (`positionMs + elapsed`), so a seek/play event stays accurate even
     * after the network latency. `0` means unknown (older peer): fall back to
     * [positionMs] directly.
     */
    val positionAtMs: Long = 0L,
    val isPlaying: Boolean = false,
    /** In-app player volume (0f..1f): syncs the VIVI volume slider between the
     *  two devices (mobile playerVolume <-> desktop player volume). */
    val volume: Float? = null,
    /** Native OS system volume (0f..1f): Android STREAM_MUSIC <-> desktop OS
     *  master volume. Null when the sender can't read its system volume. */
    val systemVolume: Float? = null,
    /** Repeat mode: "OFF" / "ALL" / "ONE". Null = not set (older peer). */
    val repeatMode: String? = null,
    /** Shuffle enabled. Null = not set (older peer). */
    val isShuffle: Boolean? = null,
    val queue: List<TrackRef> = emptyList(),
    val queueIndex: Int = -1,
    val queueTitle: String? = null,
    /**
     * Epoch millis (shared relay-time reference frame) when the queue/index was
     * last changed locally. `0` means unknown (older peer) and the queue is then
     * applied unconditionally. Used for last-write-wins queue merging, mirroring
     * the playlists' [SyncedPlaylist.updatedAt].
     */
    val queueUpdatedAt: Long = 0L,
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
 */
@Serializable
data class LibrarySnapshot(
    val songIds: List<String> = emptyList(),
    val albumIds: List<String> = emptyList(),
    val artistIds: List<String> = emptyList(),
    val playlistIds: List<String> = emptyList(),
    /** Full local-playlist state (name + ordered songs), incl. deletion tombstones. */
    val playlists: List<SyncedPlaylist> = emptyList(),
)

/** A song as stored inside a synced playlist (enough metadata to render it). */
@Serializable
data class SyncedSong(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val thumbnail: String? = null,
)

/**
 * A user-created playlist shared across devices. [updatedAt] (epoch millis) is
 * the local edit timestamp used for last-write-wins merging; [deleted] is a
 * deletion tombstone (the playlist was removed at [updatedAt]).
 */
@Serializable
data class SyncedPlaylist(
    val id: String,
    val name: String = "",
    val songs: List<SyncedSong> = emptyList(),
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,
)
