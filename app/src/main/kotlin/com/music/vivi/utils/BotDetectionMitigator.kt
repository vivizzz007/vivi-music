/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import androidx.datastore.preferences.core.edit
import com.music.innertube.YouTube
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.utils.cipher.CipherDeobfuscator
import com.music.vivi.utils.PlaybackLogManager
import com.music.vivi.utils.PlaybackLogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages bot detection mitigation by tracking playback failures and
 * rotating guest identities (visitorData) when necessary.
 */
object BotDetectionMitigator {
    private const val TAG = "BotDetectionMitigator"
    private val failureCount = AtomicInteger(0)
    private const val FAILURE_THRESHOLD = 2

    /**
     * Call this when a playback error (like Error 2000/403) occurs.
     */
    suspend fun notifyPlaybackFailure(isLoggedIn: Boolean) {
        if (isLoggedIn) return // Antibot rotation is primarily for guest users

        val currentFailures = failureCount.incrementAndGet()
        Timber.tag(TAG).w("Playback failure reported. Consecutive failures: $currentFailures")

        if (currentFailures >= FAILURE_THRESHOLD) {
            rotateGuestSession()
        }
    }

    /**
     * Call this when a track starts playing successfully.
     */
    fun notifyPlaybackSuccess() {
        if (failureCount.get() > 0) {
            Timber.tag(TAG).d("Playback success reported. Resetting failure count.")
            failureCount.set(0)
        }
    }

    /**
     * Forces a rotation of the guest session identifiers.
     */
    suspend fun rotateGuestSession() {
        Timber.tag(TAG).i("Rotating guest session to bypass bot detection...")
        PlaybackLogManager.log(PlaybackLogLevel.BOT, "Rotating guest session", "Bypassing bot detection by refreshing visitorData")
        
        withContext(Dispatchers.IO) {
            // 1. Clear current session
            YouTube.clearGuestSession()
            
            // 2. Fetch fresh visitorData
            YouTube.refreshVisitorData().onSuccess { newData ->
                Timber.tag(TAG).i("New visitorData obtained successfully.")
                
                // 3. Persist to DataStore to ensure consistency across app restarts
                CipherDeobfuscator.appContext?.dataStore?.edit { settings ->
                    settings[VisitorDataKey] = newData
                }
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed to refresh visitorData during rotation")
            }
        }
        
        failureCount.set(0)
    }
}
