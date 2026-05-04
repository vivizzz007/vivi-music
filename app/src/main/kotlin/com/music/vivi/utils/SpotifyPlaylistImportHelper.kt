/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.db.MusicDatabase
import com.music.vivi.db.entities.PlaylistEntity
import com.music.vivi.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * Creates a new local playlist and fills it by matching Spotify embed track rows to YouTube Music.
 */
object SpotifyPlaylistImportHelper {

    data class ImportResult(
        val playlistId: String,
        val savedCount: Int,
        val skippedCount: Int,
    )

    fun isSpotifyPlaylistInput(text: String): Boolean =
        SpotifyPlaylistFetcher.extractPlaylistId(text.trim()) != null

    /**
     * @param syncToYouTube If true, creates a matching YouTube Music playlist (account playlist) and
     * uploads resolved tracks there, and stores its [PlaylistEntity.browseId] on the local row.
     */
    suspend fun importFromSpotifyUrl(
        rawInput: String,
        database: MusicDatabase,
        syncToYouTube: Boolean = false,
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        val id = SpotifyPlaylistFetcher.extractPlaylistId(rawInput.trim())
            ?: return@withContext Result.failure(IllegalStateException("Not a Spotify playlist URL."))
        val source = SpotifyPlaylistFetcher.fetch(id).getOrElse { return@withContext Result.failure(it) }

        val entity = PlaylistEntity(
            name = source.title,
            thumbnailUrl = source.thumbnailUrl,
            bookmarkedAt = LocalDateTime.now(),
            isEditable = true,
        )
        // Must complete before Flow collect: database.query {} only schedules work on an executor
        // and returns immediately, which used to race playlist(...).firstOrNull() → hang or null.
        database.withTransaction {
            insert(entity)
        }
        val playlist = database.playlist(entity.id).firstOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Could not create the local playlist."))

        val tracks = source.tracks
        val semaphore = Semaphore(4)
        val indexedResults = coroutineScope {
            tracks.mapIndexed { index, track ->
                async {
                    semaphore.withPermit {
                        val query = buildString {
                            append(track.title)
                            if (track.artistLine.isNotBlank()) {
                                append(" ")
                                append(track.artistLine)
                            }
                        }
                        index to resolveSongId(query, database)
                    }
                }
            }.awaitAll()
        }

        val idsInOrder = indexedResults
            .sortedBy { it.first }
            .mapNotNull { it.second }
        if (idsInOrder.isNotEmpty()) {
            database.addSongToPlaylist(playlist, idsInOrder)
        }

        if (syncToYouTube) {
            runCatching {
                val browseId = YouTube.createPlaylist(source.title)
                database.withTransaction {
                    update(entity.copy(browseId = browseId))
                }
                if (idsInOrder.isNotEmpty()) {
                    val uploadSem = Semaphore(4)
                    coroutineScope {
                        idsInOrder.map { videoId ->
                            async {
                                uploadSem.withPermit {
                                    YouTube.addToPlaylist(browseId, videoId).onFailure { reportException(it) }
                                }
                            }
                        }.awaitAll()
                    }
                }
            }.onFailure { reportException(it) }
        }

        Result.success(
            ImportResult(
                playlistId = entity.id,
                savedCount = idsInOrder.size,
                skippedCount = tracks.size - idsInOrder.size,
            ),
        )
    }

    private suspend fun resolveSongId(query: String, database: MusicDatabase): String? =
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                ?.items
                ?.firstOrNull { it is SongItem } as? SongItem
        }.onFailure { reportException(it) }
            .getOrNull()
            ?.let { song ->
                val meta = song.toMediaMetadata()
                database.query { insert(meta) }
                song.id
            }
}
