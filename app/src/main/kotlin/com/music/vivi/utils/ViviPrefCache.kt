/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

object ViviPrefCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var preferences: Preferences? = null
    @Volatile
    private var isStarted = false

    fun start(context: Context) {
        if (isStarted) return
        synchronized(this) {
            if (isStarted) return
            isStarted = true
            scope.launch {
                // Keep the fast path alive: if the DataStore flow fails (e.g. a
                // file issue right after an in-place update), retry instead of
                // dying permanently — otherwise every synchronous read falls
                // back to the slow runBlocking path and stalls startup.
                while (isActive) {
                    try {
                        context.dataStore.data.collect { prefs ->
                            preferences = prefs
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "ViviPrefCache: DataStore collect failed, retrying")
                    }
                    delay(1000)
                }
            }
        }
    }

    fun <T> get(key: Preferences.Key<T>): T? {
        return preferences?.get(key)
    }

    fun isInitialized(): Boolean {
        return preferences != null
    }
}
