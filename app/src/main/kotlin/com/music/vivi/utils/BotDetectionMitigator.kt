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
 *
 * Key improvements:
 * - Locale preservation: snapshot region/language before rotation and restore it
 *   to ensure new visitorData is issued for the user's correct country.
 * - Immediate rotation: removed thresholds/cooldowns for faster playback recovery.
 * - Surgical rotation: only clears visitorData, not the entire session.
 */
object BotDetectionMitigator {
    private const val TAG = "BotDetectionMitigator"

    private val failureCount = AtomicInteger(0)

    // Error reasons that indicate geographic restriction – NOT a bot signal.
    // IMPORTANT: Keep these specific to avoid false positives.
    private val GEO_ERROR_SIGNATURES = listOf(
        "not available in your country",
        "not available in your region",
        "not available in this country",
        "not available in this region",
        "geo-restricted",
        "GEO_RESTRICTED",
        "NOT_AVAILABLE_IN_THIS_COUNTRY",
        "only available in certain countries",
        "country restriction",
        "region restriction",
    )

    // Error reasons that strongly suggest bot / IP flagging by YouTube.
    private val BOT_ERROR_SIGNATURES = listOf(
        "Sign in to confirm",
        "confirm you're not a bot",
        "automated queries",
        "Error 2000",
        "403",
        "This content isn't available on this device",
    )

    /**
     * Call this when a playback error occurs.
     * Returns true if rotation might help (looks like bot detection).
     */
    fun notifyPlaybackFailure(isLoggedIn: Boolean, errorMessage: String? = null): Boolean {
        if (isLoggedIn) return false
        if (isGeoError(errorMessage)) return false

        failureCount.incrementAndGet()
        return true
    }

    /**
     * Call this when a track starts playing successfully.
     */
    fun notifyPlaybackSuccess() {
        failureCount.set(0)
    }

    /**
     * Rotates the guest session by obtaining a fresh visitorData token while preserving locale.
     */
    suspend fun rotateGuestSession() {
        Timber.tag(TAG).i("Rotating guest session to bypass bot detection...")
        PlaybackLogManager.log(
            PlaybackLogLevel.BOT, 
            "Rotating guest session", 
            "Bypassing bot detection by refreshing visitorData (locale preserved)"
        )
        
        withContext(Dispatchers.IO) {
            // Snapshot locale so the new token is issued for the user's actual region.
            val currentLocale = YouTube.locale

            // Clear only visitorData - minimal session change
            YouTube.visitorData = null
            
            YouTube.refreshVisitorData().onSuccess { newData ->
                Timber.tag(TAG).i("New visitorData obtained successfully for region ${currentLocale.gl}.")
                
                // Persist to DataStore
                CipherDeobfuscator.appContext?.dataStore?.edit { settings ->
                    settings[VisitorDataKey] = newData
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to refresh visitorData during rotation")
                // Restore locale context if refresh failed
                YouTube.locale = currentLocale
            }
        }
        
        failureCount.set(0)
    }

    /**
     * Returns true if message matches known geographic restriction patterns.
     */
    fun isGeoError(message: String?): Boolean {
        if (message == null) return false
        val lower = message.lowercase()
        return GEO_ERROR_SIGNATURES.any { lower.contains(it.lowercase()) }
    }

    /**
     * Returns true if message matches known bot-detection signatures.
     */
    fun isBotDetectionError(message: String?): Boolean {
        if (message == null) return false
        val lower = message.lowercase()
        return BOT_ERROR_SIGNATURES.any { lower.contains(it.lowercase()) }
    }

    fun reset() {
        failureCount.set(0)
    }
}
