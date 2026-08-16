/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.devicesync

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.music.vivi.constants.AppLanguageKey
import com.music.vivi.constants.AudioNormalizationKey
import com.music.vivi.constants.AudioQuality
import com.music.vivi.constants.AudioQualityKey
import com.music.vivi.constants.ContentCountryKey
import com.music.vivi.constants.ContentLanguageKey
import com.music.vivi.constants.CrossfadeDurationKey
import com.music.vivi.constants.CrossfadeEnabledKey
import com.music.vivi.constants.DarkModeKey
import com.music.vivi.constants.DeviceSyncDeviceIdKey
import com.music.vivi.constants.DeviceSyncEnabledKey
import com.music.vivi.constants.DeviceSyncPairIdKey
import com.music.vivi.constants.DeviceSyncServerUrlKey
import com.music.vivi.constants.DynamicThemeKey
import com.music.vivi.constants.EnableDiscordRPCKey
import com.music.vivi.constants.EnableKugouKey
import com.music.vivi.constants.EnableLastFMScrobblingKey
import com.music.vivi.constants.EnableListenTogetherKey
import com.music.vivi.constants.EnableLrcLibKey
import com.music.vivi.constants.EnableMusixmatchKey
import com.music.vivi.constants.EnablePaxsenixKey
import com.music.vivi.constants.EnableSimpMusicKey
import com.music.vivi.constants.EnableYouLyPlusKey
import com.music.vivi.constants.LyricsRomanizeChineseKey
import com.music.vivi.constants.LyricsRomanizeJapaneseKey
import com.music.vivi.constants.LyricsRomanizeKoreanKey
import com.music.vivi.constants.PreferredLyricsProvider
import com.music.vivi.constants.PreferredLyricsProviderKey
import com.music.vivi.constants.PureBlackKey
import com.music.vivi.constants.SYSTEM_DEFAULT
import com.music.vivi.constants.SaavnAudioQuality
import com.music.vivi.constants.SaavnAudioQualityKey
import com.music.vivi.constants.SelectedFontKey
import com.music.vivi.constants.SelectedThemeColorKey
import com.music.vivi.constants.SkipSilenceKey
import com.music.vivi.constants.SuggestionRegionKey
import com.music.vivi.constants.TranslateLanguageKey
import com.music.vivi.constants.TranslateLyricsKey
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.models.MediaMetadata
import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncClient
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncEvent
import com.music.vivi.sync.SyncServer
import com.music.vivi.sync.SyncSnapshot
import com.music.vivi.sync.SyncedPlaylist
import com.music.vivi.sync.SyncedSong
import com.music.vivi.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the shared `sync` module with the Android app.
 *
 * - Observes the shared preferences subset and pushes a snapshot when it changes.
 * - Applies incoming snapshots (settings + a pending playback resume).
 * - Exposes pairing helpers (`createPairingCode` / `joinPair`) for the UI.
 *
 * Playback (queue + position) is captured via [pushPlayback], called from the
 * player layer; the incoming resume is exposed through [pendingPlayback].
 */
@Singleton
class DeviceSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var client: SyncClient? = null
    private var started = false

    /** Set while applying a remote snapshot, to avoid echoing it back. */
    @Volatile
    private var applyingRemote = false

    /** While set, playback pushes are suppressed (avoids echoing a snapshot back). */
    @Volatile
    private var suppressPlaybackPushUntil = 0L

    /** While set, library pushes are suppressed (avoids echoing an applied snapshot). */
    @Volatile
    private var suppressLibraryPushUntil = 0L

    private var lastPlayback: PlaybackSnapshot? = null

    private var lastLibrary: LibrarySnapshot? = null

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _peerDeviceName = MutableStateFlow("")
    val peerDeviceName: StateFlow<String> = _peerDeviceName.asStateFlow()

    private val _pendingPlayback = MutableStateFlow<PlaybackSnapshot?>(null)
    val pendingPlayback: StateFlow<PlaybackSnapshot?> = _pendingPlayback.asStateFlow()

    private val _syncedLibrary = MutableStateFlow<LibrarySnapshot?>(null)
    val syncedLibrary: StateFlow<LibrarySnapshot?> = _syncedLibrary.asStateFlow()

    fun start() {
        if (started) return
        started = true
        scope.launch { observeLifecycle() }
        scope.launch { observeSettingsAndPush() }
        scope.launch { observeLibraryAndPush() }
    }

    // ---------------------------------------------------------------------
    // Public pairing / sync API (for the settings UI)
    // ---------------------------------------------------------------------

    fun createPairingCode() {
        scope.launch {
            ensureClient()
            client?.requestPairingCode()
        }
    }

    fun joinPair(code: String) {
        scope.launch {
            ensureClient()
            client?.joinPair(code)
        }
    }

    fun unpair() {
        scope.launch {
            client?.unpair()
            context.dataStore.edit {
                it.remove(DeviceSyncPairIdKey)
                it[DeviceSyncEnabledKey] = false
            }
            _paired.value = false
            _peerDeviceName.value = ""
        }
    }

    /** Capture the current queue + position from the player and sync it. */
    fun pushPlayback(playback: PlaybackSnapshot) {
        if (System.currentTimeMillis() < suppressPlaybackPushUntil) return
        lastPlayback = if (playback.positionAtMs == 0L) {
            playback.copy(positionAtMs = serverNowMs())
        } else {
            playback
        }
        scope.launch { pushCurrentSnapshot() }
    }

    /** Estimated clock offset to the relay server (see [SyncClient.serverOffsetMs]). */
    val serverOffsetMs: Long get() = client?.serverOffsetMs ?: 0L

    /** Current epoch millis in the shared relay-time reference frame. */
    private fun serverNowMs(): Long = System.currentTimeMillis() + serverOffsetMs

    /**
     * Live position of a received snapshot: extrapolates `positionMs + elapsed`
     * while the peer is playing, using the timestamp and the shared clock
     * reference frame (falls back to the raw position otherwise).
     */
    fun effectivePosition(snapshot: PlaybackSnapshot): Long {
        val base = snapshot.positionMs.coerceAtLeast(0L)
        val at = snapshot.positionAtMs
        if (at <= 0L || !snapshot.isPlaying) return base
        val elapsed = (serverNowMs() - at).coerceAtLeast(0L)
        return (base + elapsed).coerceAtLeast(0L)
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private suspend fun observeLifecycle() {
        context.dataStore.data
            .map { prefs ->
                val enabled = prefs[DeviceSyncEnabledKey] ?: false
                val pairId = prefs[DeviceSyncPairIdKey].orEmpty()
                enabled to pairId
            }
            .distinctUntilChanged()
            .collect { (enabled, pairId) ->
                _paired.value = enabled && pairId.isNotEmpty()
                if (enabled && pairId.isNotEmpty()) {
                    ensureClient()
                } else if (!enabled) {
                    teardownClient()
                }
            }
    }

    private suspend fun observeSettingsAndPush() {
        context.dataStore.data
            .map { readSettings(it) }
            .distinctUntilChanged()
            .collect { settings ->
                if (applyingRemote || !_paired.value) return@collect
                pushCurrentSnapshot(settings)
            }
    }

    /** Observe the local library (liked songs / albums / artists / playlists) and push it. */
    private suspend fun observeLibraryAndPush() {
        combine(
            database.likedSongsByCreateDateAsc(),
            database.albumsLikedByCreateDateAsc(),
            database.artistsBookmarkedByCreateDateAsc(),
            database.playlistsByCreateDateAsc(),
        ) { songs, albums, artists, playlists ->
            PlaylistLibraryInput(
                songIds = songs.map { it.song.id },
                albumIds = albums.map { it.album.id },
                artistIds = artists.map { it.artist.id },
                playlists = playlists,
            )
        }
            .distinctUntilChanged()
            .collect { input ->
                // Resolve each playlist's ordered songs (the combine transform is
                // non-suspend, so this happens here in the suspend collector).
                val syncedPlaylists = input.playlists.map { p ->
                    val songs = database.playlistSongs(p.playlist.id).first()
                    SyncedPlaylist(
                        id = p.playlist.id,
                        name = p.playlist.name,
                        songs = songs.map { ps ->
                            SyncedSong(
                                id = ps.song.song.id,
                                title = ps.song.song.title,
                                artist = ps.song.artists.joinToString(", ") { it.name },
                                thumbnail = ps.song.song.thumbnailUrl,
                            )
                        },
                        updatedAt = p.playlist.lastUpdateTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L,
                    )
                }
                lastLibrary = LibrarySnapshot(
                    songIds = input.songIds,
                    albumIds = input.albumIds,
                    artistIds = input.artistIds,
                    playlistIds = input.playlists.map { it.playlist.id },
                    playlists = syncedPlaylists,
                )
                if (!applyingRemote && _paired.value && System.currentTimeMillis() >= suppressLibraryPushUntil) {
                    pushCurrentSnapshot()
                }
            }
    }

    private suspend fun ensureClient() {
        val prefs = context.dataStore.data.first()
        val url = prefs[DeviceSyncServerUrlKey] ?: SyncServer.DEFAULT_URL
        val deviceId = resolveDeviceId(prefs)

        val existing = client
        if (existing != null && existing.serverUrl == url && existing.deviceId == deviceId) {
            if (existing.connectionState.value != SyncConnectionState.CONNECTED) existing.connect()
            return
        }

        teardownClient()
        val created = SyncClient(url, deviceId, defaultDeviceName())
        client = created
        scope.launch { created.events.collect { handleEvent(it) } }
        created.connect()
    }

    private fun teardownClient() {
        client?.disconnect()
        client = null
    }

    private suspend fun resolveDeviceId(prefs: Preferences): String {
        prefs[DeviceSyncDeviceIdKey]?.takeIf { it.isNotEmpty() }?.let { return it }
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { it[DeviceSyncDeviceIdKey] = id }
        return id
    }

    private fun defaultDeviceName(): String {
        val name = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        return name.ifBlank { "Android" }
    }

    private suspend fun handleEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.Connected -> {
                _status.value = "Connected"
                client?.pullSnapshot()
            }
            is SyncEvent.PairCode -> _status.value = event.code
            is SyncEvent.Paired -> {
                _paired.value = true
                _peerDeviceName.value = event.peerDeviceName
                context.dataStore.edit {
                    it[DeviceSyncPairIdKey] = event.pairId
                    it[DeviceSyncEnabledKey] = true
                }
                _status.value = "Paired with ${event.peerDeviceName}"
                pushCurrentSnapshot()
            }
            is SyncEvent.SnapshotReceived -> applySnapshot(event.snapshot)
            is SyncEvent.Disconnected -> _status.value = "Disconnected"
            is SyncEvent.NoSnapshot -> Unit
            is SyncEvent.Error -> {
                _status.value = event.message
                // The peer unpaired us, or the relay no longer knows about this
                // pair (e.g. the desktop's LAN relay was stopped/restarted). Drop
                // the local pairing so the UI stops claiming we are paired.
                if (event.message.contains("unpaired", ignoreCase = true) ||
                    event.message.contains("not paired", ignoreCase = true)
                ) {
                    context.dataStore.edit {
                        it.remove(DeviceSyncPairIdKey)
                        it[DeviceSyncEnabledKey] = false
                    }
                    _paired.value = false
                    _peerDeviceName.value = ""
                }
            }
        }
    }

    private suspend fun applySnapshot(snapshot: SyncSnapshot) {
        val current = client ?: return
        if (snapshot.deviceId == current.deviceId) return

        applyingRemote = true
        try {
            snapshot.settings.forEach { (key, value) -> applySetting(key, value) }
            snapshot.deviceName.takeIf { it.isNotBlank() }?.let { _peerDeviceName.value = it }
            if (snapshot.playback != null) {
                suppressPlaybackPushUntil = System.currentTimeMillis() + 1500L
                _pendingPlayback.value = snapshot.playback
            }
            snapshot.library?.let { lib ->
                _syncedLibrary.value = lib
                if (lib.playlists.isNotEmpty()) {
                    applyRemotePlaylists(lib.playlists)
                    suppressLibraryPushUntil = System.currentTimeMillis() + 2000L
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "DeviceSync: failed to apply snapshot")
        } finally {
            applyingRemote = false
        }
    }

    /**
     * Applies the peer's playlist list to the local library, per-playlist
     * last-write-wins by [SyncedPlaylist.updatedAt]. Deletion tombstones remove
     * the local playlist only when they are newer than the local edit.
     */
    private suspend fun applyRemotePlaylists(remote: List<SyncedPlaylist>) {
        val now = LocalDateTime.now()
        for (r in remote) {
            val local = database.playlist(r.id).first()
            val localUpdatedAt = local?.playlist?.lastUpdateTime
                ?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L

            if (r.deleted) {
                if (local != null && r.updatedAt > localUpdatedAt) {
                    database.delete(local.playlist)
                }
                continue
            }
            if (r.updatedAt <= localUpdatedAt) continue // local is newer/equal: keep it

            if (local == null) {
                database.insert(
                    PlaylistEntity(
                        id = r.id,
                        name = r.name,
                        bookmarkedAt = now,
                        lastUpdateTime = now,
                    )
                )
            } else {
                database.update(local.playlist.copy(name = r.name, lastUpdateTime = now))
            }

            // Replace the playlist's songs with the remote order.
            database.clearPlaylist(r.id)
            r.songs.forEachIndexed { index, s ->
                // Ensure the song + artist rows exist so the playlist renders.
                database.insert(
                    MediaMetadata(
                        id = s.id,
                        title = s.title,
                        artists = listOf(MediaMetadata.Artist(null, s.artist)),
                        duration = -1,
                        thumbnailUrl = s.thumbnail,
                    )
                )
                database.insert(
                    PlaylistSongMap(
                        playlistId = r.id,
                        songId = s.id,
                        position = index,
                    )
                )
            }
        }
    }

    private suspend fun pushCurrentSnapshot(settings: Map<String, String>? = null) {
        val current = client ?: return
        if (current.connectionState.value != SyncConnectionState.CONNECTED) return

        val prefs = context.dataStore.data.first()
        current.pushSnapshot(
            SyncSnapshot(
                deviceId = current.deviceId,
                deviceName = defaultDeviceName(),
                updatedAt = System.currentTimeMillis(),
                settings = settings ?: readSettings(prefs),
                playback = lastPlayback,
                library = lastLibrary,
            )
        )
    }

    // ---------------------------------------------------------------------
    // Shared settings mapping (key name <-> typed preference)
    // ---------------------------------------------------------------------

    private fun readSettings(prefs: Preferences): Map<String, String> = buildMap {
        // Theme
        put(DynamicThemeKey.name, (prefs[DynamicThemeKey] ?: true).toString())
        put(SelectedThemeColorKey.name, (prefs[SelectedThemeColorKey] ?: 0).toString())
        put(DarkModeKey.name, prefs[DarkModeKey] ?: "AUTO")
        put(PureBlackKey.name, (prefs[PureBlackKey] ?: false).toString())
        put(SelectedFontKey.name, prefs[SelectedFontKey] ?: "system")
        // Language / content
        put(AppLanguageKey.name, prefs[AppLanguageKey] ?: SYSTEM_DEFAULT)
        put(ContentLanguageKey.name, prefs[ContentLanguageKey] ?: SYSTEM_DEFAULT)
        put(ContentCountryKey.name, prefs[ContentCountryKey] ?: SYSTEM_DEFAULT)
        put(SuggestionRegionKey.name, prefs[SuggestionRegionKey] ?: "system")
        // Audio quality
        put(AudioQualityKey.name, prefs[AudioQualityKey] ?: AudioQuality.AUTO.name)
        put(AudioNormalizationKey.name, (prefs[AudioNormalizationKey] ?: false).toString())
        put(SaavnAudioQualityKey.name, prefs[SaavnAudioQualityKey] ?: SaavnAudioQuality.QUALITY_320.name)
        put(CrossfadeEnabledKey.name, (prefs[CrossfadeEnabledKey] ?: false).toString())
        put(CrossfadeDurationKey.name, (prefs[CrossfadeDurationKey] ?: 0f).toString())
        put(SkipSilenceKey.name, (prefs[SkipSilenceKey] ?: false).toString())
        // Lyrics
        put(PreferredLyricsProviderKey.name, prefs[PreferredLyricsProviderKey] ?: PreferredLyricsProvider.LRCLIB.name)
        put(TranslateLyricsKey.name, (prefs[TranslateLyricsKey] ?: false).toString())
        put(TranslateLanguageKey.name, prefs[TranslateLanguageKey] ?: "en")
        put(LyricsRomanizeJapaneseKey.name, (prefs[LyricsRomanizeJapaneseKey] ?: false).toString())
        put(LyricsRomanizeKoreanKey.name, (prefs[LyricsRomanizeKoreanKey] ?: false).toString())
        put(LyricsRomanizeChineseKey.name, (prefs[LyricsRomanizeChineseKey] ?: false).toString())
        put(EnableKugouKey.name, (prefs[EnableKugouKey] ?: false).toString())
        put(EnableLrcLibKey.name, (prefs[EnableLrcLibKey] ?: false).toString())
        put(EnableMusixmatchKey.name, (prefs[EnableMusixmatchKey] ?: false).toString())
        put(EnableSimpMusicKey.name, (prefs[EnableSimpMusicKey] ?: false).toString())
        put(EnableYouLyPlusKey.name, (prefs[EnableYouLyPlusKey] ?: false).toString())
        put(EnablePaxsenixKey.name, (prefs[EnablePaxsenixKey] ?: false).toString())
        // Integrations
        put(EnableLastFMScrobblingKey.name, (prefs[EnableLastFMScrobblingKey] ?: false).toString())
        put(EnableDiscordRPCKey.name, (prefs[EnableDiscordRPCKey] ?: false).toString())
        put(EnableListenTogetherKey.name, (prefs[EnableListenTogetherKey] ?: false).toString())
    }

    private suspend fun applySetting(key: String, value: String) {
        context.dataStore.edit { prefs ->
            when (key) {
                DynamicThemeKey.name -> prefs[DynamicThemeKey] = value.toBooleanStrictOrNull() ?: return@edit
                SelectedThemeColorKey.name -> prefs[SelectedThemeColorKey] = value.toIntOrNull() ?: return@edit
                DarkModeKey.name -> prefs[DarkModeKey] = value
                PureBlackKey.name -> prefs[PureBlackKey] = value.toBooleanStrictOrNull() ?: return@edit
                SelectedFontKey.name -> prefs[SelectedFontKey] = value
                AppLanguageKey.name -> prefs[AppLanguageKey] = value
                ContentLanguageKey.name -> prefs[ContentLanguageKey] = value
                ContentCountryKey.name -> prefs[ContentCountryKey] = value
                SuggestionRegionKey.name -> prefs[SuggestionRegionKey] = value
                AudioQualityKey.name -> prefs[AudioQualityKey] = value
                AudioNormalizationKey.name -> prefs[AudioNormalizationKey] = value.toBooleanStrictOrNull() ?: return@edit
                SaavnAudioQualityKey.name -> prefs[SaavnAudioQualityKey] = value
                CrossfadeEnabledKey.name -> prefs[CrossfadeEnabledKey] = value.toBooleanStrictOrNull() ?: return@edit
                CrossfadeDurationKey.name -> prefs[CrossfadeDurationKey] = value.toFloatOrNull() ?: return@edit
                SkipSilenceKey.name -> prefs[SkipSilenceKey] = value.toBooleanStrictOrNull() ?: return@edit
                PreferredLyricsProviderKey.name -> prefs[PreferredLyricsProviderKey] = value
                TranslateLyricsKey.name -> prefs[TranslateLyricsKey] = value.toBooleanStrictOrNull() ?: return@edit
                TranslateLanguageKey.name -> prefs[TranslateLanguageKey] = value
                LyricsRomanizeJapaneseKey.name -> prefs[LyricsRomanizeJapaneseKey] = value.toBooleanStrictOrNull() ?: return@edit
                LyricsRomanizeKoreanKey.name -> prefs[LyricsRomanizeKoreanKey] = value.toBooleanStrictOrNull() ?: return@edit
                LyricsRomanizeChineseKey.name -> prefs[LyricsRomanizeChineseKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableKugouKey.name -> prefs[EnableKugouKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableLrcLibKey.name -> prefs[EnableLrcLibKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableMusixmatchKey.name -> prefs[EnableMusixmatchKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableSimpMusicKey.name -> prefs[EnableSimpMusicKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableYouLyPlusKey.name -> prefs[EnableYouLyPlusKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnablePaxsenixKey.name -> prefs[EnablePaxsenixKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableLastFMScrobblingKey.name -> prefs[EnableLastFMScrobblingKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableDiscordRPCKey.name -> prefs[EnableDiscordRPCKey] = value.toBooleanStrictOrNull() ?: return@edit
                EnableListenTogetherKey.name -> prefs[EnableListenTogetherKey] = value.toBooleanStrictOrNull() ?: return@edit
            }
        }
    }
}

/** Intermediate library state used to hand the flows over to the suspend collector. */
private data class PlaylistLibraryInput(
    val songIds: List<String>,
    val albumIds: List<String>,
    val artistIds: List<String>,
    val playlists: List<Playlist>,
)
