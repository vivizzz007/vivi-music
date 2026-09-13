/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.music.vivi.constants.SongSortType
import com.music.vivi.db.entities.AlbumArtistMap
import com.music.vivi.db.entities.AlbumEntity
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.db.entities.PlaylistSongMap
import com.music.vivi.db.entities.SongAlbumMap
import com.music.vivi.db.entities.SongArtistMap
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.db.entities.SpeedDialItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
import java.io.File
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InternalDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: InternalDatabase
    private lateinit var dao: DatabaseDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDatabaseInitializationAndVersion() {
        assertEquals("song.db", InternalDatabase.DB_NAME)
        assertNotNull(db)
        assertNotNull(dao)
        assertNotNull(db.speedDialDao)
        val version = db.openHelper.readableDatabase.version
        assertEquals(34, version)
    }

    @Test
    fun testInsertAndRetrieveSongWithRelations() = runBlocking {
        val now = LocalDateTime.of(2026, 9, 9, 20, 0)
        val artist = ArtistEntity(id = "artist_1", name = "Test Artist")
        val album = AlbumEntity(id = "album_1", title = "Test Album", year = 2026, songCount = 1, duration = 240)
        val song = SongEntity(
            id = "song_1",
            title = "Test Track",
            duration = 240,
            albumId = "album_1",
            albumName = "Test Album",
            inLibrary = now
        )

        dao.insert(artist)
        dao.insert(album)
        dao.insert(song)
        dao.insert(SongArtistMap(songId = "song_1", artistId = "artist_1", position = 0))
        dao.insert(SongAlbumMap(songId = "song_1", albumId = "album_1", index = 0))
        dao.insert(AlbumArtistMap(albumId = "album_1", artistId = "artist_1", order = 0))

        val retrievedSong = dao.song("song_1").first()
        assertNotNull(retrievedSong)
        assertEquals("Test Track", retrievedSong!!.song.title)
        assertEquals(1, retrievedSong.artists.size)
        assertEquals("Test Artist", retrievedSong.artists[0].name)
        assertNotNull(retrievedSong.album)
        assertEquals("Test Album", retrievedSong.album!!.title)

        val allSongs = dao.songs(SongSortType.CREATE_DATE, false).first()
        assertEquals(1, allSongs.size)
        assertEquals("song_1", allSongs[0].id)

        val retrievedArtist = dao.artist("artist_1").first()
        assertNotNull(retrievedArtist)
        assertEquals("Test Artist", retrievedArtist!!.artist.name)

        val retrievedAlbum = dao.album("album_1").first()
        assertNotNull(retrievedAlbum)
        assertEquals("Test Album", retrievedAlbum!!.album.title)
    }

    @Test
    fun testPlaylistAndSongMapping() = runBlocking {
        val playlist = PlaylistEntity(id = "pl_1", name = "My Playlist")
        val song = SongEntity(id = "song_pl_1", title = "Playlist Song", duration = 180)

        dao.insert(playlist)
        dao.insert(song)
        dao.insert(PlaylistSongMap(playlistId = "pl_1", songId = "song_pl_1", position = 0))

        val playlistSongs = dao.playlistSongs("pl_1").first()
        assertEquals(1, playlistSongs.size)
        assertEquals("song_pl_1", playlistSongs[0].song.id)
        assertEquals("Playlist Song", playlistSongs[0].song.title)
    }

    @Test
    fun testArtistPlaylistSongs() = runBlocking {
        val artist = ArtistEntity(id = "artist_queen", name = "Queen")
        val otherArtist = ArtistEntity(id = "artist_other", name = "Other Artist")
        val playlist1 = PlaylistEntity(id = "pl_rock", name = "Classic Rock", isEditable = true)
        val playlist2 = PlaylistEntity(id = "pl_favs", name = "My Favorites", isEditable = true)
        val song1 = SongEntity(id = "song_bohemian", title = "Bohemian Rhapsody", duration = 354)
        val song2 = SongEntity(id = "song_champions", title = "We Are The Champions", duration = 179)
        val songOtherArtist = SongEntity(id = "song_other", title = "Other Song", duration = 200)

        dao.insert(artist)
        dao.insert(otherArtist)
        dao.insert(playlist1)
        dao.insert(playlist2)
        dao.insert(song1)
        dao.insert(song2)
        dao.insert(songOtherArtist)

        dao.insert(SongArtistMap(songId = "song_bohemian", artistId = "artist_queen", position = 0))
        dao.insert(SongArtistMap(songId = "song_champions", artistId = "artist_queen", position = 0))
        dao.insert(SongArtistMap(songId = "song_other", artistId = "artist_other", position = 0))

        // song1 in playlist 1, song2 in playlist 2, and song1 also duplicated in playlist 2
        dao.insert(PlaylistSongMap(playlistId = "pl_rock", songId = "song_bohemian", position = 0))
        dao.insert(PlaylistSongMap(playlistId = "pl_favs", songId = "song_champions", position = 0))
        dao.insert(PlaylistSongMap(playlistId = "pl_favs", songId = "song_bohemian", position = 1))
        dao.insert(PlaylistSongMap(playlistId = "pl_rock", songId = "song_other", position = 1))

        val queenPlaylistSongs = dao.artistPlaylistSongs("artist_queen", "Queen").first()
        // Should find Bohemian Rhapsody and We Are The Champions (distinct, ordered by title)
        assertEquals(2, queenPlaylistSongs.size)
        assertEquals("Bohemian Rhapsody", queenPlaylistSongs[0].song.title)
        assertEquals("We Are The Champions", queenPlaylistSongs[1].song.title)

        val count = dao.artistPlaylistSongsCount("artist_queen", "Queen").first()
        assertEquals(2, count)
    }

    @Test
    fun testSpeedDialDaoOperations() = runBlocking {
        val speedDial = db.speedDialDao
        val item = SpeedDialItem(
            id = "sd_song_1",
            title = "Pinned Song",
            type = "SONG"
        )

        speedDial.insert(item)
        val all = speedDial.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Pinned Song", all[0].title)

        val isPinned = speedDial.isPinned("sd_song_1").first()
        assertTrue(isPinned)

        speedDial.delete("sd_song_1")
        val afterDelete = speedDial.getAll().first()
        assertTrue(afterDelete.isEmpty())

        val isPinnedAfter = speedDial.isPinned("sd_song_1").first()
        assertFalse(isPinnedAfter)
    }

    @Test
    fun testSchemaFilesContinuityAndHash() {
        val schemasDir = File("schemas/com.music.vivi.db.InternalDatabase")
        val altSchemasDir = File("core-database/schemas/com.music.vivi.db.InternalDatabase")
        val dir = if (schemasDir.exists()) schemasDir else altSchemasDir
        assertTrue("Schemas directory must exist: ${dir.absolutePath}", dir.exists())

        // Verify all 34 version schemas exist
        for (v in 1..34) {
            val schemaFile = File(dir, "$v.json")
            assertTrue("Schema file $v.json must exist", schemaFile.exists())
            val content = schemaFile.readText()
            val json = JSONObject(content)
            val dbObj = json.getJSONObject("database")
            assertEquals(v, dbObj.getInt("version"))
        }

        // Verify latest schema v34 identity hash
        val schema34File = File(dir, "34.json")
        val json34 = JSONObject(schema34File.readText())
        val dbObj34 = json34.getJSONObject("database")
        assertEquals("19aa47f2f3d3b94ea27011eeb3b536d5", dbObj34.getString("identityHash"))
    }
}
