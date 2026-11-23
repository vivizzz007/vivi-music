/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.github.news.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.music.vivi.github.news.constants.AnnouncementConstants
import com.music.vivi.github.news.models.Announcement
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepository @Inject constructor() {

    private val httpClient = OkHttpClient.Builder().build()

    private val _currentAnnouncement = MutableStateFlow<Announcement?>(null)
    val currentAnnouncement: StateFlow<Announcement?> = _currentAnnouncement.asStateFlow()

    private var lastFetchTimeMs: Long = 0
    private val throttleIntervalMs: Long = 30 * 1000 // 30 seconds throttle for non-forced requests

    suspend fun refreshAnnouncement(context: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchTimeMs < throttleIntervalMs && _currentAnnouncement.value != null) {
            return@withContext
        }

        try {
            // Append timestamp to bypass GitHub Fastly CDN cache (max-age=300)
            val cacheBustUrl = "${AnnouncementConstants.ANNOUNCEMENT_URL}?t=$now"
            val request = Request.Builder()
                .url(cacheBustUrl)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404 || response.code == 410) {
                    // File was deleted on GitHub -> hide/vanish announcement immediately
                    _currentAnnouncement.value = null
                    lastFetchTimeMs = now
                    return@withContext
                }

                if (!response.isSuccessful) {
                    return@withContext
                }

                val body = response.body?.string() ?: ""
                val parsed = AnnouncementParser.parse(body)

                if (parsed == null || !parsed.isActive || parsed.isExpired) {
                    // File is empty, cleared, marked active: false, or expired -> vanish immediately
                    _currentAnnouncement.value = null
                    lastFetchTimeMs = now
                    return@withContext
                }

                // Check if user dismissed this announcement
                val dismissedId = context.dataStore[AnnouncementConstants.DismissedAnnouncementIdKey]
                if (dismissedId == parsed.id && parsed.isDismissible) {
                    _currentAnnouncement.value = null
                    return@withContext
                }

                _currentAnnouncement.value = parsed
                lastFetchTimeMs = now
            }
        } catch (_: Exception) {
            // Network failure or offline - ignore
        }
    }

    suspend fun getDetailContent(announcement: Announcement): String {
        return announcement.content
    }

    suspend fun dismissAnnouncement(context: Context, id: String) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[AnnouncementConstants.DismissedAnnouncementIdKey] = id
        }
        _currentAnnouncement.value = null
    }
}
