/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.sponsorblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SponsorSegment(
    val category: String,
    val actionType: String,
    val startMs: Long,
    val endMs: Long,
    val uuid: String
)

object SponsorBlockApi {
    private const val DEFAULT_SERVER_URL = "https://sponsor.ajay.app"
    private val TAG = "SponsorBlockApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Memory LRU cache: videoId -> List<SponsorSegment>
    private val segmentCache = ConcurrentHashMap<String, List<SponsorSegment>>()

    suspend fun getSkipSegments(
        videoId: String,
        categories: List<String>,
        serverUrl: String = DEFAULT_SERVER_URL
    ): Result<List<SponsorSegment>> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        // Return cached result if available
        segmentCache[videoId]?.let { cached ->
            return@withContext Result.success(cached)
        }

        if (categories.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val baseUrl = if (serverUrl.isBlank()) DEFAULT_SERVER_URL else serverUrl.trimEnd('/')
        val categoriesJson = JSONArray(categories).toString()

        val httpUrl = (baseUrl + "/api/skipSegments")
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("videoID", videoId)
            ?.addQueryParameter("categories", categoriesJson)
            ?.build()
            ?: return@withContext Result.failure(Exception("Invalid SponsorBlock API URL"))

        try {
            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("User-Agent", "vivi-music/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.code == 404) {
                // 404 means no segments found for this video
                segmentCache[videoId] = emptyList()
                return@withContext Result.success(emptyList())
            }

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val jsonArray = JSONArray(responseBody)
            val segments = mutableListOf<SponsorSegment>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val category = obj.optString("category", "")
                val actionType = obj.optString("actionType", "skip")
                val segmentArray = obj.optJSONArray("segment")

                if (segmentArray != null && segmentArray.length() >= 2) {
                    val startSec = segmentArray.getDouble(0)
                    val endSec = segmentArray.getDouble(1)
                    val startMs = (startSec * 1000).toLong()
                    val endMs = (endSec * 1000).toLong()
                    val uuid = obj.optString("UUID", "")

                    if (endMs > startMs) {
                        segments.add(
                            SponsorSegment(
                                category = category,
                                actionType = actionType,
                                startMs = startMs,
                                endMs = endMs,
                                uuid = uuid
                            )
                        )
                    }
                }
            }

            val sortedSegments = segments.sortedBy { it.startMs }
            segmentCache[videoId] = sortedSegments
            Result.success(sortedSegments)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to fetch SponsorBlock segments for $videoId")
            Result.failure(e)
        }
    }

    fun clearCache() {
        segmentCache.clear()
    }
}
