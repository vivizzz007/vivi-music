/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.playback.download

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.music.vivi.constants.SongSortType
import com.music.vivi.db.MusicDatabase
import com.music.vivi.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State of a single song download, mirroring the subset of Media3's [Download] used by the UI
 * (icons/filters compare against `Download.STATE_*`), so existing `download?.state == Download.STATE_COMPLETED`
 * call sites keep working unchanged even though this is no longer backed by Media3's DownloadManager.
 */
data class SongDownload(
    val songId: String,
    val state: Int,
    /** 0-100, mirrors Media3's `Download.percentDownloaded`. Only meaningful while STATE_DOWNLOADING. */
    val progress: Float = 0f,
)

/**
 * Public entry point for triggering/observing song downloads, replacing the old Media3-based
 * [com.music.vivi.playback.DownloadUtil] for anything UI-triggered. Writes real audio files via
 * [SongDownloadWorker] into either a user-chosen SAF folder or the app's default external music
 * directory. Pre-existing downloads made by the old Media3 engine keep working untouched — this
 * class only reports NEW downloads' in-flight progress, but its `downloads`/`getDownload` surface
 * unifies both by reading completion straight from the database (`SongEntity.isDownloaded`, which
 * both engines write).
 */
@Singleton
class SongDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads: StateFlow<Map<String, SongDownload>> = combine(
        workManager.getWorkInfosByTagFlow(DOWNLOAD_TAG),
        database.downloadedSongs(SongSortType.CREATE_DATE, true),
    ) { workInfos, completedSongs ->
        val result = mutableMapOf<String, SongDownload>()
        completedSongs.forEach { result[it.id] = SongDownload(it.id, Download.STATE_COMPLETED) }
        workInfos.forEach { info ->
            val songId = info.tags.firstOrNull { it.startsWith(SONG_ID_TAG_PREFIX) }
                ?.removePrefix(SONG_ID_TAG_PREFIX) ?: return@forEach
            val state = when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> Download.STATE_QUEUED
                WorkInfo.State.RUNNING -> Download.STATE_DOWNLOADING
                // Completion is sourced from the DB (`completedSongs`) so it stays accurate even
                // after WorkManager keeps a stale SUCCEEDED WorkInfo around for a removed download.
                WorkInfo.State.SUCCEEDED -> null
                WorkInfo.State.FAILED -> Download.STATE_FAILED
                WorkInfo.State.CANCELLED -> null
            } ?: return@forEach
            // Don't let a stale FAILED/QUEUED work entry override an already-completed DB state.
            if (result[songId]?.state != Download.STATE_COMPLETED) {
                val progress = if (state == Download.STATE_DOWNLOADING) {
                    info.progress.getInt(SongDownloadWorker.KEY_PROGRESS, 0).toFloat()
                } else 0f
                result[songId] = SongDownload(songId, state, progress)
            }
        }
        result
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun getDownload(songId: String): Flow<SongDownload?> = downloads.map { it[songId] }

    fun download(mediaMetadata: MediaMetadata) {
        val request = OneTimeWorkRequestBuilder<SongDownloadWorker>()
            .addTag(DOWNLOAD_TAG)
            .addTag("$SONG_ID_TAG_PREFIX${mediaMetadata.id}")
            .setInputData(
                workDataOf(
                    SongDownloadWorker.KEY_SONG_ID to mediaMetadata.id,
                    SongDownloadWorker.KEY_TITLE to mediaMetadata.title,
                    SongDownloadWorker.KEY_ARTIST to mediaMetadata.artists.joinToString(", ") { it.name },
                ),
            )
            .build()
        workManager.enqueueUniqueWork(uniqueWorkName(mediaMetadata.id), ExistingWorkPolicy.KEEP, request)
    }

    fun cancelOrRemove(songId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(songId))
        scope.launch {
            val localFileUri = database.song(songId).first()?.song?.localFileUri
            if (localFileUri != null) {
                LocalDownloadFile.delete(context, localFileUri.toUri())
            }
            database.updateDownloadedInfo(songId, false, null, null)
        }
    }

    private fun uniqueWorkName(songId: String) = "song_download_$songId"

    companion object {
        private const val DOWNLOAD_TAG = "song_download"
        private const val SONG_ID_TAG_PREFIX = "song_id:"
    }
}
