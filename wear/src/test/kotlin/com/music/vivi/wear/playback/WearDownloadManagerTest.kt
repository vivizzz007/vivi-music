/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WearDownloadManagerTest {

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

    @Test
    fun testDownloadSongPersistsSongAndArtist() = runBlocking {
        downloadManager.downloadSong(
            songId = "test_song_1",
            title = "Test Title",
            artist = "Test Artist",
            thumbnailUrl = "https://example.com/thumb.jpg",
            duration = 180,
        )

        val persisted = waitUntil {
            database.getSongById("test_song_1") != null
        }
        assertTrue("SongEntity must be persisted in Room", persisted)

        val song = database.getSongById("test_song_1")
        assertNotNull(song)
        assertEquals("test_song_1", song!!.song.id)
        assertEquals("Test Title", song.song.title)
        assertEquals(180, song.song.duration)
        assertEquals("https://example.com/thumb.jpg", song.song.thumbnailUrl)
        assertFalse(song.song.isDownloaded)
        assertEquals(1, song.artists.size)
        assertEquals("Test Artist", song.artists[0].name)

        // Before download completion, downloaded songs list is empty
        val downloadedBefore = database.downloadedSongsByCreateDateAsc().first()
        assertTrue("No songs should be marked downloaded yet", downloadedBefore.isEmpty())

        // Simulate download completion (as triggered by Media3 listener)
        val now = LocalDateTime.now()
        database.updateDownloadedInfo("test_song_1", true, now)

        val downloadedAfter = database.downloadedSongsByCreateDateAsc().first()
        assertEquals(1, downloadedAfter.size)
        assertEquals("test_song_1", downloadedAfter[0].song.id)
        assertEquals("Test Title", downloadedAfter[0].song.title)
        assertEquals("Test Artist", downloadedAfter[0].artists.firstOrNull()?.name)
        assertTrue(downloadedAfter[0].song.isDownloaded)
    }

    @Test
    fun testDownloadSongFromMediaMetadata() = runBlocking {
        val mediaMetadata = MediaMetadata(
            id = "meta_song_1",
            title = "Metadata Song Title",
            artists = listOf(
                MediaMetadata.Artist(id = "art_1", name = "Primary Artist"),
                MediaMetadata.Artist(id = "art_2", name = "Featured Artist"),
            ),
            duration = 210,
            thumbnailUrl = "https://example.com/meta_art.jpg",
            album = MediaMetadata.Album(id = "alb_1", title = "Meta Album"),
        )

        downloadManager.downloadSong(mediaMetadata)

        val persisted = waitUntil {
            database.getSongById("meta_song_1") != null
        }
        assertTrue("MediaMetadata song must be persisted in Room", persisted)

        val song = database.getSongById("meta_song_1")
        assertNotNull(song)
        assertEquals("meta_song_1", song!!.song.id)
        assertEquals("Metadata Song Title", song.song.title)
        assertEquals(210, song.song.duration)
        assertFalse(song.song.isDownloaded)
        assertEquals(2, song.artists.size)
        assertEquals("Primary Artist", song.artists[0].name)
        assertEquals("Featured Artist", song.artists[1].name)
    }

    @Test
    fun testDownloadPlaylistPersistsPlaylistAndAllSongs() = runBlocking {
        val songs = listOf(
            MediaMetadata(
                id = "pl_song_1",
                title = "Playlist Track 1",
                artists = listOf(MediaMetadata.Artist(id = "a1", name = "Artist 1")),
                duration = 150,
            ),
            MediaMetadata(
                id = "pl_song_2",
                title = "Playlist Track 2",
                artists = listOf(MediaMetadata.Artist(id = "a2", name = "Artist 2")),
                duration = 200,
            ),
        )

        downloadManager.downloadPlaylist(
            playlistId = "pl_workout",
            playlistTitle = "Workout Hits",
            songs = songs,
        )

        val playlistPersisted = waitUntil {
            database.playlist("pl_workout").first() != null
        }
        assertTrue("Playlist must be inserted into database", playlistPersisted)

        val playlist = database.playlist("pl_workout").first()
        assertNotNull(playlist)
        assertEquals("Workout Hits", playlist!!.playlist.name)

        val songsPersisted = waitUntil {
            val plSongs = database.playlistSongs("pl_workout").first()
            plSongs.size == 2
        }
        assertTrue("All songs must be mapped to playlist", songsPersisted)

        val playlistSongs = database.playlistSongs("pl_workout").first()
        assertEquals(2, playlistSongs.size)
        assertEquals("pl_song_1", playlistSongs[0].song.id)
        assertEquals(0, playlistSongs[0].map.position)
        assertEquals("pl_song_2", playlistSongs[1].song.id)
        assertEquals(1, playlistSongs[1].map.position)
    }

    @Test
    fun testRemoveDownloadResetsDatabaseDownloadedState() = runBlocking {
        downloadManager.downloadSong(
            songId = "song_to_remove",
            title = "Removable Track",
            artist = "Artist Removable",
            duration = 100,
        )

        waitUntil { database.getSongById("song_to_remove") != null }

        // Mark as downloaded
        database.updateDownloadedInfo("song_to_remove", true, LocalDateTime.now())
        assertEquals(1, database.downloadedSongsByCreateDateAsc().first().size)

        // Remove download
        downloadManager.removeDownload("song_to_remove")

        val removed = waitUntil {
            val s = database.getSongById("song_to_remove")
            s != null && !s.song.isDownloaded
        }
        assertTrue("isDownloaded must be reset to false upon removeDownload", removed)

        val downloadedList = database.downloadedSongsByCreateDateAsc().first()
        assertTrue("Downloaded songs list must be empty after removal", downloadedList.isEmpty())
    }
}
