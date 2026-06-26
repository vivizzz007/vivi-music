package com.music.vivi.vivimusic.release

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.SongItem
import com.music.vivi.constants.NewReleaseNotificationsKey
import com.music.vivi.constants.TasteBasedReleaseNotificationsKey
import com.music.vivi.db.InternalDatabase
import com.music.vivi.utils.dataStore
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class NewReleaseCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val prefs = context.getSharedPreferences("new_release_prefs", Context.MODE_PRIVATE)

    private data class ReleaseInfo(
        val id: String,
        val title: String,
        val type: String,
        val deepLinkUrl: String,
        val thumbnailUrl: String? = null
    )

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("Starting new release check work")

        // Read both preference flags from DataStore
        val settings = context.dataStore.data.first()
        val bookmarkedEnabled = settings[NewReleaseNotificationsKey] ?: true
        val tasteBasedEnabled = settings[TasteBasedReleaseNotificationsKey] ?: false

        if (!bookmarkedEnabled && !tasteBasedEnabled) {
            Timber.tag(TAG).d("Both notification sources disabled, skipping")
            return Result.success()
        }

        val database = InternalDatabase.newInstance(context)

        try {
            // Build deduplicated artist list from enabled sources
            val artistMap = mutableMapOf<String, Pair<String, String>>() // id -> (name, artistId)

            if (bookmarkedEnabled) {
                val bookmarked = database.artistsBookmarkedByCreateDateAsc().first()
                bookmarked
                    .filter { it.artist.isYouTubeArtist && !it.artist.isPrivatelyOwnedArtist }
                    .forEach { artist ->
                        artistMap[artist.id] = Pair(artist.artist.name ?: "Unknown", artist.id)
                    }
                Timber.tag(TAG).d("Added %d bookmarked artists", artistMap.size)
            }

            if (tasteBasedEnabled) {
                val sixMonthsAgo = LocalDateTime.now()
                    .minusMonths(6)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

                val topArtists = database.mostPlayedArtists(
                    fromTimeStamp = sixMonthsAgo,
                    limit = 10
                ).first()

                var addedCount = 0
                topArtists
                    .filter { it.artist.isYouTubeArtist && !it.artist.isPrivatelyOwnedArtist }
                    .forEach { artist ->
                        if (!artistMap.containsKey(artist.id)) {
                            artistMap[artist.id] = Pair(artist.artist.name ?: "Unknown", artist.id)
                            addedCount++
                        }
                    }
                Timber.tag(TAG).d("Added %d taste-based artists (top 10 over last 6 months)", addedCount)
            }

            if (artistMap.isEmpty()) {
                Timber.tag(TAG).d("No eligible YouTube artists found")
                return Result.success()
            }

            Timber.tag(TAG).d("Checking releases for %d total artists", artistMap.size)

            for ((artistId, artistInfo) in artistMap) {
                val artistName = artistInfo.first
                Timber.tag(TAG).d("Checking releases for artist: %s (%s)", artistName, artistId)

                val artistPageResult = YouTube.artist(artistId)
                if (artistPageResult.isFailure) {
                    Timber.tag(TAG).w("Failed to fetch artist page for %s", artistName)
                    continue
                }

                val artistPage = artistPageResult.getOrNull() ?: continue
                val currentReleases = mutableListOf<ReleaseInfo>()

                // Gather songs and albums from sections
                for (section in artistPage.sections) {
                    for (item in section.items) {
                        when (item) {
                            is SongItem -> {
                                currentReleases.add(
                                    ReleaseInfo(
                                        id = item.id,
                                        title = item.title,
                                        type = "Song",
                                        deepLinkUrl = "vivimusic://watch?v=${item.id}",
                                        thumbnailUrl = item.thumbnail
                                    )
                                )
                            }
                            is AlbumItem -> {
                                currentReleases.add(
                                    ReleaseInfo(
                                        id = item.browseId,
                                        title = item.title,
                                        type = "Album",
                                        deepLinkUrl = "vivimusic://browse/${item.browseId}",
                                        thumbnailUrl = item.thumbnail
                                    )
                                )
                            }
                            else -> {} // Ignore ArtistItem, PlaylistItem, etc.
                        }
                    }
                }

                if (currentReleases.isEmpty()) continue

                val seenKey = "seen_${artistId}"
                val seenIds = prefs.getStringSet(seenKey, null)?.toMutableSet() ?: mutableSetOf()
                val currentIds = currentReleases.map { it.id }.toSet()

                // Determine which releases we haven't seen before
                val unseenReleases = currentReleases.filter { it.id !in seenIds }

                if (unseenReleases.isNotEmpty()) {
                    // Get the timestamp when we first started tracking this artist.
                    // We only notify about releases we detect AFTER we started tracking.
                    val trackingSinceKey = "tracking_since_${artistId}"
                    val trackingSince = prefs.getLong(trackingSinceKey, -1L)

                    if (trackingSince == -1L) {
                        // Very first time seeing this artist — record when we started tracking.
                        // Don't notify yet: we don't know if these releases are actually "new"
                        // or have been out for months.
                        Timber.tag(TAG).d(
                            "First time tracking %s — recording %d releases as baseline",
                            artistName, currentIds.size
                        )
                        prefs.edit()
                            .putStringSet(seenKey, currentIds)
                            .putLong(trackingSinceKey, System.currentTimeMillis())
                            .apply()
                    } else {
                        // We were already tracking this artist. Any IDs not in our saved set
                        // are genuinely new since last check — notify the user.
                        Timber.tag(TAG).d(
                            "Found %d new releases for %s",
                            unseenReleases.size, artistName
                        )
                        for (release in unseenReleases) {
                            NewReleaseNotificationHelper.showReleaseNotification(
                                context = context,
                                artistName = artistName,
                                releaseTitle = release.title,
                                releaseType = release.type,
                                deepLinkUrl = release.deepLinkUrl,
                                thumbnailUrl = release.thumbnailUrl
                            )
                        }
                        // Mark newly found releases as seen so we don't notify again
                        seenIds.addAll(unseenReleases.map { it.id })
                        prefs.edit().putStringSet(seenKey, seenIds).apply()
                    }
                } else {
                    Timber.tag(TAG).d("No new releases for %s", artistName)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error occurred during release check work")
            return Result.retry()
        } finally {
            database.close()
        }
    }

    companion object {
        private const val TAG = "NewReleaseCheckWorker"
        const val UNIQUE_WORK_NAME = "NewReleaseCheckWork"

        fun schedule(context: Context) {
            Timber.tag(TAG).d("Enqueuing periodic release check worker")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NewReleaseCheckWorker>(
                24, TimeUnit.HOURS,
                2, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            // UPDATE replaces any previously-stuck/misconfigured periodic work and resets the timer.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            Timber.tag(TAG).d("Cancelling periodic release check worker")
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        /**
         * Clears the seen-releases baseline and tracking timestamps so the worker treats
         * everything as unseen and re-snapshots on next run. Useful after a fresh install
         * or for testing.
         */
        fun clearSeenReleases(context: Context) {
            context.getSharedPreferences("new_release_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            Timber.tag(TAG).d("Cleared all seen release data — will re-snapshot on next run")
        }
    }
}
