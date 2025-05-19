@file:Suppress("NAME_SHADOWING")

package com.music.vivi.ui.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.ArtistEntity
import com.music.vivi.db.entities.Song
import com.music.vivi.db.entities.SongEntity
import com.music.vivi.models.toMediaMetadata
import com.music.vivi.constants.ScannerSensitivity
import com.music.vivi.utils.LmImageCacheMgr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.Locale

const val sdcardRoot = "/storage/emulated/0/"
val testScanPaths = arrayListOf("Music")
var directoryUID = 0
var cachedDirectoryTree: DirectoryTree? = null
var imageCache: LmImageCacheMgr = LmImageCacheMgr()

val projection = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.DISPLAY_NAME,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ARTIST_ID,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.ALBUM_ID,
    MediaStore.Audio.Media.RELATIVE_PATH,
)

class DirectoryTree(path: String) {
    var currentDir = path
    var parent: String = ""
    var subdirs = ArrayList<DirectoryTree>()
    var files = ArrayList<Song>()
    val uid = directoryUID

    init {
        directoryUID++
    }

    constructor(path: String, files: ArrayList<Song>) : this(path) {
        this.files = files
    }

    fun insert(path: String, song: Song) {
        if (path.indexOf('/') == -1) {
            files.add(song)
            println(path)
            return
        }
        var tmppath = path
        if (path[path.length - 1] == '/') {
            tmppath = path.substring(0, path.length - 1)
        }
        val subdirPath = tmppath.substringBefore('/')
        var existingSubdir: DirectoryTree? = null
        subdirs.forEach { subdir ->
            if (subdir.currentDir == subdirPath) {
                existingSubdir = subdir
                return@forEach
            }
        }
        if (existingSubdir == null) {
            val tree = DirectoryTree(subdirPath)
            tree.parent = "$parent/$currentDir"
            tree.insert(tmppath.substringAfter('/'), song)
            subdirs.add(tree)

        } else {
            existingSubdir!!.insert(tmppath.substringAfter('/'), song)
        }
    }

    fun toList(): List<Song> {
        val songs = ArrayList<Song>()
        fun traverseHELPME(tree: DirectoryTree, result: ArrayList<Song>) {
            result.addAll(tree.files)
            tree.subdirs.forEach { traverseHELPME(it, result) }
        }
        traverseHELPME(this, songs)
        return songs
    }

    fun toFlattenedTree(): DirectoryTree {
        val result = DirectoryTree(sdcardRoot)
        getSubdirsRecursive(this, result.subdirs)
        return result
    }

    private fun getSubdirsRecursive(it: DirectoryTree, result: ArrayList<DirectoryTree>) {
        if (it.files.size > 0) {
            result.add(DirectoryTree(it.currentDir, it.files))
        }

        if (it.subdirs.size > 0) {
            it.subdirs.forEach { getSubdirsRecursive(it, result) }
        }
    }

    fun copy(
        currentDir: String = this.currentDir,
        subdirs: List<DirectoryTree> = this.subdirs,
        files: List<Song> = this.files
    ): DirectoryTree {
        return DirectoryTree(currentDir).apply {
            this.subdirs = subdirs.toMutableList() as ArrayList<DirectoryTree>
            this.files = files.toMutableList() as ArrayList<Song>
        }
    }
}

fun refreshLocal(context: Context, database: MusicDatabase) =
    refreshLocal(context, database, testScanPaths)

fun refreshLocal(
    context: Context,
    database: MusicDatabase,
    scanPaths: ArrayList<String>
): MutableStateFlow<DirectoryTree> {
    val newDirectoryStructure = DirectoryTree(sdcardRoot)
    var existingSongs: List<Song>
    runBlocking(Dispatchers.IO) {
        existingSongs = database.allLocalSongsData().first()
    }
    val contentResolver: ContentResolver = context.contentResolver
    val cursor = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?",
        scanPaths.map { "$sdcardRoot$it%" }.toTypedArray(),
        null
    )
    cursor?.use { cursor ->
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

        while (cursor.moveToNext()) {
            val name = cursor.getString(nameColumn)
            val path = cursor.getString(pathColumn)
            val possibleMatch =
                existingSongs.firstOrNull { it.song.localPath == "$sdcardRoot$path$name" }

            if (possibleMatch != null) {
                newDirectoryStructure.insert("$path$name", possibleMatch)
            }

        }
    }

    cachedDirectoryTree = newDirectoryStructure
    return MutableStateFlow(newDirectoryStructure)
}

fun scanLocal(context: Context) =
    scanLocal(context, testScanPaths)


@OptIn(ExperimentalCoroutinesApi::class)
fun scanLocal(
    context: Context,
    scanPaths: ArrayList<String>
): MutableStateFlow<DirectoryTree> {
    val newDirectoryStructure = DirectoryTree(sdcardRoot)
    val contentResolver: ContentResolver = context.contentResolver
    val cursor = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?",
        scanPaths.map { "$sdcardRoot$it%" }.toTypedArray(),
        null
    )

    val scannerJobs = ArrayList<Deferred<Song>>()
    runBlocking {
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIDColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getInt(durationColumn)
                val artist = cursor.getString(artistColumn)
                val artistID = cursor.getString(artistIdColumn)
                val albumID = cursor.getString(albumIDColumn)
                val album = cursor.getString(albumColumn)
                val path = cursor.getString(pathColumn)
                fun advancedScan(): Song {
                    val artists = ArrayList<ArtistEntity>()
                    artists.add(ArtistEntity(artistID, artist))
                    return Song(
                        SongEntity(
                            id.toString(),
                            title,
                            (duration / 1000),
                            albumId = albumID,
                            albumName = album,
                            isLocal = true,
                            inLibrary = LocalDateTime.now(),
                            localPath = "$sdcardRoot$path$name"
                        ),
                        artists,
                    )
                }

                scannerJobs.add(async(Dispatchers.IO) { advancedScan() })
            }
        }

        scannerJobs.awaitAll()
    }
    scannerJobs.forEach { it ->
        val song = it.getCompleted()
        song.song.localPath?.let {
            newDirectoryStructure.insert(
                it.substringAfter(sdcardRoot), song
            )
        }
    }

    cachedDirectoryTree = newDirectoryStructure
    return MutableStateFlow(newDirectoryStructure)
}

fun syncDB(
    database: MusicDatabase,
    directoryStructure: List<Song>,
    matchStrength: ScannerSensitivity,
    strictFileNames: Boolean
) {
    println(directoryStructure.size)

    database.transaction {
        directoryStructure.forEach { song ->
            val querySong = database.searchSongs(song.song.title.substringBeforeLast('.'))
            CoroutineScope(Dispatchers.IO).launch {
                val songMatch = querySong.first().filter {
                    if (strictFileNames &&
                        (it.song.localPath?.substringBeforeLast('/') !=
                                song.song.localPath?.substringBeforeLast('/'))
                    ) {
                        return@filter false
                    }
                    return@filter compareSong(it, song, matchStrength, strictFileNames)
                }
                if (songMatch.isNotEmpty()) {
                    println(song.song.title)
                    database.update(song.song)
                } else {
                    println(song.song.title + song.artists.first().name + song.artists.first().id)
                    println(database.insert(song.toMediaMetadata()))
                }
            }
        }
    }

}

fun compareArtist(a: List<ArtistEntity>, b: List<ArtistEntity>): Boolean {
    if (a.isEmpty() && b.isEmpty()) {
        return true
    } else if (a.isEmpty() || b.isEmpty()) {
        return false
    }
    if (a.size != b.size) {
        return false
    }
    val matchingArtists = a.filter { artist ->
        b.any { it.name.lowercase(Locale.getDefault()) == artist.name.lowercase(Locale.getDefault()) }
    }
    return matchingArtists.size == a.size
}
fun compareSong(
    a: Song,
    b: Song,
    matchStrength: ScannerSensitivity = ScannerSensitivity.LEVEL_2,
    strictFileNames: Boolean = false
): Boolean {
    if (strictFileNames &&
        (a.song.localPath?.substringAfterLast('/') !=
                b.song.localPath?.substringAfterLast('/'))
    ) {
        return false
    }
    fun closeEnough(): Boolean {
        return a.song.localPath == b.song.localPath
    }
    return when (matchStrength) {
        ScannerSensitivity.LEVEL_1 -> a.song.title == b.song.title
        ScannerSensitivity.LEVEL_2 -> closeEnough() || (a.song.title == b.song.title &&
                compareArtist(a.artists, b.artists))

        ScannerSensitivity.LEVEL_3 -> closeEnough() || (a.song.title == b.song.title &&
                compareArtist(a.artists, b.artists))
    }
}
object CachedBitmap {
    var path: String? = null
    var image: Bitmap? = null
    fun cache(path: String, image: Bitmap?) {
        if (image == null) {
            return
        }

        this.path = path
        this.image = image
        bitmapCache.add(this)
    }

}

var bitmapCache = ArrayList<CachedBitmap>()
fun getDirectorytree(): DirectoryTree? {
    if (cachedDirectoryTree == null) {
        return null
    }
    return cachedDirectoryTree
}