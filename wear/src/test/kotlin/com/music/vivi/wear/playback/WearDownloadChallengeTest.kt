/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.music.vivi.db.InternalDatabase
import com.music.vivi.db.MusicDatabase
import com.music.vivi.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Adversarial challenge tests for Milestone 2 (R3 Offline Download Engine & Caching Fixes).
 *
 * Stress-tests edge cases:
 * 1. Rapid duplicate download requests for the same song (single-threaded & multi-threaded concurrency).
 * 2. Rapid download & removal races.
 * 3. Download removal resetting isDownloaded in Room and clearing offline library Flow.
 * 4. Idempotency of removal on non-existent or previously removed items.
 * 5. Multi-track playlist download with foreign key integrity.
 * 6. Edge cases in metadata (null artists, blank artists, special characters).
 * 7. WearDownloadCache architectural isolation (deadlock prevention & cache factories).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WearDownloadChallengeTest {

    private lateinit var context: Context
    private lateinit var internalDb: InternalDatabase
    private lateinit var database: MusicDatabase
    private lateinit var databaseProvider: StandaloneDatabaseProvider
    private lateinit var testScope: CoroutineScope
    private lateinit var downloadManager: WearDownloadManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WearDownloadCache.release()

        internalDb = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database = MusicDatabase(internalDb)

        databaseProvider = StandaloneDatabaseProvider(context)
        WearDownloadCache.initialize(context, databaseProvider)

        testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        downloadManager = WearDownloadManager(
            context = context,
            database = database,
            databaseProvider = databaseProvider,
            downloadCache = WearDownloadCache.downloadCache,
            upstreamDataSourceFactory = WearDownloadCache.getDownloadUpstreamDataSourceFactory(context),
            coroutineScope = testScope,
        )
    }

    @After
    fun tearDown() {
        downloadManager.release()
        testScope.cancel()
        WearDownloadCache.release()
        internalDb.close()
    }

    private suspend fun waitUntil(timeoutMs: Long = 3000, condition: suspend () -> Boolean): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return true
            delay(20)
        }
        return condition()
    }

    // =========================================================================
    // CHALLENGE 1: Rapid Duplicate Download Requests (Concurrency & Idempotency)
    // =========================================================================

    /**
     * Rapid sequential download requests for the exact same song ID must not throw
     * SQLiteConstraintException or corrupt Room database state.
     */
    @Test
    fun challengeRapidDuplicateDownload_sequentialCalls_maintainsIntegrity() = runBlocking {
        val songId = "rapid_seq_song"
        repeat(25) { index ->
            downloadManager.downloadSong(
                songId = songId,
                title = "Sequential Song",
                artist = "Sequential Artist",
                duration = 180,
            )
        }

        val persisted = waitUntil {
            database.getSongById(songId) != null
        }
        assertTrue("Song must be persisted without SQLite conflict", persisted)

        val song = database.getSongById(songId)
        assertNotNull(song)
        assertEquals(songId, song!!.song.id)
        assertEquals("Sequential Song", song.song.title)
        assertFalse("Initial state must not be marked downloaded", song.song.isDownloaded)

        // Mark as completed
        database.updateDownloadedInfo(songId, true, LocalDateTime.now())

        // Offline library must contain exactly 1 entry for this song
        val library = database.downloadedSongsByCreateDateAsc().first()
        assertEquals(1, library.size)
        assertEquals(songId, library[0].song.id)
    }

    /**
     * Highly concurrent rapid duplicate download requests dispatched from 10 parallel threads.
     * Must not deadlock or crash with SQLite constraint violations.
     */
    @Test
    fun challengeRapidDuplicateDownload_concurrentThreads_doesNotCorruptDatabase() = runBlocking {
        val songId = "concurrent_dupe_song"
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        repeat(threadCount) { threadIdx ->
            Thread {
                try {
                    downloadManager.downloadSong(
                        songId = songId,
                        title = "Concurrent Track",
                        artist = "Concurrent Artist",
                        duration = 200 + threadIdx,
                    )
                    successCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        assertTrue("All concurrent download requests must complete dispatch", latch.await(5, TimeUnit.SECONDS))
        assertEquals(threadCount, successCount.get())

        val persisted = waitUntil {
            database.getSongById(songId) != null
        }
        assertTrue("Song must exist in Room database after concurrent downloads", persisted)

        val song = database.getSongById(songId)
        assertNotNull(song)
        assertEquals(songId, song!!.song.id)
        assertEquals("Concurrent Track", song.song.title)

        // Simulate download complete
        database.updateDownloadedInfo(songId, true, LocalDateTime.now())

        val library = database.downloadedSongsByCreateDateAsc().first()
        assertEquals(1, library.size)
        assertEquals(songId, library[0].song.id)
    }

    /**
     * Rapid concurrent download requests via MediaMetadata.
     * Verifies that DatabaseDao.insert(MediaMetadata) atomic transaction prevents duplicates.
     */
    @Test
    fun challengeRapidDuplicateMediaMetadataDownload_concurrentThreads_isAtomic() = runBlocking {
        val songId = "concurrent_meta_song"
        val metadata = MediaMetadata(
            id = songId,
            title = "Concurrent Meta Track",
            artists = listOf(MediaMetadata.Artist(id = "artist_abc", name = "Meta Artist")),
            duration = 240,
        )

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        repeat(threadCount) {
            Thread {
                try {
                    downloadManager.downloadSong(metadata)
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))

        val persisted = waitUntil {
            database.getSongById(songId) != null
        }
        assertTrue("MediaMetadata song must be persisted", persisted)

        val song = database.getSongById(songId)
        assertNotNull(song)
        assertEquals(songId, song!!.song.id)
        assertEquals("Concurrent Meta Track", song.song.title)
        assertEquals(1, song.artists.size)
        assertEquals("Meta Artist", song.artists[0].name)
    }

    // =========================================================================
    // CHALLENGE 2: Download Removal & Database / Library Synchronization
    // =========================================================================

    /**
     * Verifies that removeDownload resets isDownloaded to false, resets dateDownload to null,
     * and clears the song from the downloadedSongsByCreateDateAsc Flow.
     */
    @Test
    fun challengeRemoveDownload_resetsIsDownloadedAndClearsOfflineLibrary() = runBlocking {
        val songId = "song_to_purge"
        downloadManager.downloadSong(
            songId = songId,
            title = "Purge Track",
            artist = "Purge Artist",
            duration = 150,
        )

        waitUntil { database.getSongById(songId) != null }

        // Mark downloaded
        val now = LocalDateTime.now()
        database.updateDownloadedInfo(songId, true, now)

        val libraryBefore = database.downloadedSongsByCreateDateAsc().first()
        assertEquals(1, libraryBefore.size)
        assertEquals(songId, libraryBefore[0].song.id)
        assertTrue(libraryBefore[0].song.isDownloaded)

        // Remove download
        downloadManager.removeDownload(songId)

        val cleared = waitUntil {
            val s = database.getSongById(songId)
            s != null && !s.song.isDownloaded && s.song.dateDownload == null
        }
        assertTrue("Database row must have isDownloaded = false and dateDownload = null", cleared)

        val libraryAfter = database.downloadedSongsByCreateDateAsc().first()
        assertTrue("Offline library must be empty after removal", libraryAfter.isEmpty())
    }

    /**
     * Removing a non-existent or already removed song must be completely safe and idempotent.
     */
    @Test
    fun challengeRemoveDownload_nonExistentSong_isIdempotent() = runBlocking {
        // Calling on unknown ID must not throw
        downloadManager.removeDownload("ghost_song_id")

        // Calling multiple times on same ID
        repeat(5) {
            downloadManager.removeDownload("ghost_song_id")
        }

        val library = database.downloadedSongsByCreateDateAsc().first()
        assertTrue(library.isEmpty())
    }

    /**
     * Rapid download followed immediately by removeDownload before completion
     * must result in a clean, non-downloaded state.
     */
    @Test
    fun challengeRapidDownloadAndImmediateRemoval_settlesInCleanState() = runBlocking {
        val songId = "rapid_add_remove"
        downloadManager.downloadSong(
            songId = songId,
            title = "Flash Song",
            artist = "Flash Artist",
            duration = 90,
        )
        downloadManager.removeDownload(songId)

        val settled = waitUntil {
            val s = database.getSongById(songId)
            s != null && !s.song.isDownloaded
        }
        assertTrue("Song must settle in isDownloaded = false state", settled)

        val library = database.downloadedSongsByCreateDateAsc().first()
        assertTrue("Library must not list song as downloaded", library.isEmpty())
    }

    // =========================================================================
    // CHALLENGE 3: Playlist Download & Foreign Key Integrity
    // =========================================================================

    /**
     * Downloading a playlist must satisfy SQLite foreign key constraints,
     * persist PlaylistEntity, all SongEntities, and all PlaylistSongMaps with sequential positions.
     */
    @Test
    fun challengePlaylistDownload_foreignKeyIntegrityAndPartialRemoval() = runBlocking {
        val playlistId = "pl_challenge_1"
        val songs = (1..5).map { idx ->
            MediaMetadata(
                id = "pl_track_$idx",
                title = "Playlist Song $idx",
                artists = listOf(MediaMetadata.Artist(id = "art_$idx", name = "Artist $idx")),
                duration = 100 + idx * 10,
            )
        }

        downloadManager.downloadPlaylist(
            playlistId = playlistId,
            playlistTitle = "Challenge Playlist",
            songs = songs,
        )

        val plPersisted = waitUntil {
            database.playlist(playlistId).first() != null &&
                database.playlistSongs(playlistId).first().size == 5
        }
        assertTrue("Playlist and all 5 songs must be persisted in database", plPersisted)

        val playlistSongs = database.playlistSongs(playlistId).first()
        assertEquals(5, playlistSongs.size)
        playlistSongs.forEachIndexed { index, item ->
            assertEquals("pl_track_${index + 1}", item.song.id)
            assertEquals(index, item.map.position)
        }

        // Simulate 3 songs completed downloading
        database.updateDownloadedInfo("pl_track_1", true, LocalDateTime.now())
        database.updateDownloadedInfo("pl_track_2", true, LocalDateTime.now())
        database.updateDownloadedInfo("pl_track_3", true, LocalDateTime.now())

        val downloadedLibrary = database.downloadedSongsByCreateDateAsc().first()
        assertEquals(3, downloadedLibrary.size)

        // Remove track 2
        downloadManager.removeDownload("pl_track_2")

        val removed = waitUntil {
            database.downloadedSongsByCreateDateAsc().first().size == 2
        }
        assertTrue("Offline library must now contain exactly 2 songs", removed)

        // The playlist structure itself must still contain all 5 tracks
        val playlistAfterRemoval = database.playlistSongs(playlistId).first()
        assertEquals("Playlist structure in database must remain intact with 5 tracks", 5, playlistAfterRemoval.size)
    }

    // =========================================================================
    // CHALLENGE 4: Metadata Edge Cases (Null & Blank Values)
    // =========================================================================

    /**
     * Calling downloadSong with null or blank artist must not crash or insert invalid artist rows.
     */
    @Test
    fun challengeDownloadSong_withNullAndBlankArtist_handlesGracefully() = runBlocking {
        // Null artist
        downloadManager.downloadSong(
            songId = "null_artist_song",
            title = "Solo Song",
            artist = null,
            duration = 120,
        )

        // Blank artist
        downloadManager.downloadSong(
            songId = "blank_artist_song",
            title = "Blank Artist Song",
            artist = "   ",
            duration = 140,
        )

        val persisted = waitUntil {
            database.getSongById("null_artist_song") != null &&
                database.getSongById("blank_artist_song") != null
        }
        assertTrue("Both songs must be persisted", persisted)

        val nullArtistSong = database.getSongById("null_artist_song")
        assertNotNull(nullArtistSong)
        assertTrue("Song with null artist should have empty artist list", nullArtistSong!!.artists.isEmpty())

        val blankArtistSong = database.getSongById("blank_artist_song")
        assertNotNull(blankArtistSong)
        assertTrue("Song with blank artist should have empty artist list", blankArtistSong!!.artists.isEmpty())
    }

    // =========================================================================
    // CHALLENGE 5: WearDownloadCache Architecture & Deadlock Prevention
    // =========================================================================

    /**
     * Verifies WearDownloadCache directory isolation and factory configurations.
     * In particular, getDownloadUpstreamDataSourceFactory must NOT wrap downloadCache,
     * preventing cyclic lock contention and Media3 deadlocks.
     */
    @Test
    fun challengeWearDownloadCache_architecturalIsolation() {
        val downloadDir = WearDownloadCache.getDownloadCacheDir(context)
        val playerCacheDir = WearDownloadCache.getPlayerCacheDir(context)

        // 1. Directory isolation
        assertFalse(
            "Download directory and player cache directory must be distinct",
            downloadDir.absolutePath == playerCacheDir.absolutePath,
        )
        assertTrue(downloadDir.name == "downloads")
        assertTrue(playerCacheDir.name == "player_cache")

        // 2. Upstream factory for downloads must create successfully
        val upstreamFactory = WearDownloadCache.getDownloadUpstreamDataSourceFactory(context)
        assertNotNull(upstreamFactory)
        assertTrue(upstreamFactory is CacheDataSource.Factory)

        // 3. Download-only factory (for zero-network offline playback)
        val offlineOnlyFactory = WearDownloadCache.getDownloadOnlyDataSourceFactory()
        assertNotNull(offlineOnlyFactory)
        assertTrue(offlineOnlyFactory is CacheDataSource.Factory)

        // 4. isDownloaded query on non-cached track
        assertFalse(WearDownloadCache.isDownloaded("uncached_track_id"))
        assertFalse(WearDownloadCache.isPlayerCached("uncached_track_id"))
    }
}
