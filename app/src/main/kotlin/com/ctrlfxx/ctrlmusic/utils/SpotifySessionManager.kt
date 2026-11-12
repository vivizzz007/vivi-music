/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.ctrlfxx.ctrlmusic.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.music.spotify.Spotify
import com.music.spotify.SpotifyAuth
import com.ctrlfxx.ctrlmusic.App
import com.ctrlfxx.ctrlmusic.constants.SpotifySessionKey
import com.ctrlfxx.ctrlmusic.models.SpotifySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

object SpotifySessionManager {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getValidAccessToken(context: Context = App.context): String? = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val sessionJson = prefs[SpotifySessionKey]
        if (sessionJson.isNullOrBlank()) return@withContext null

        val session = try {
            json.decodeFromString<SpotifySession>(sessionJson)
        } catch (e: Exception) {
            return@withContext null
        }

        if (session.accessToken != null && session.expiresAt > System.currentTimeMillis() + 60_000L) {
            Spotify.accessToken = session.accessToken
            return@withContext session.accessToken
        }

        // Need to refresh
        try {
            val token = SpotifyAuth.fetchAccessToken(session.spDc, session.spKey.orEmpty()).getOrThrow()
            Spotify.accessToken = token.accessToken
            
            val newSession = session.copy(
                accessToken = token.accessToken,
                expiresAt = token.accessTokenExpirationTimestampMs
            )
            
            context.dataStore.edit { editPrefs ->
                editPrefs[SpotifySessionKey] = json.encodeToString(newSession)
            }
            return@withContext token.accessToken
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh Spotify token")
            return@withContext null
        }
    }
}
