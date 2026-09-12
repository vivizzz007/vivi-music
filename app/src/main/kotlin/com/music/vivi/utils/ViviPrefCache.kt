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
import kotlinx.coroutines.launch

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
                context.dataStore.data.collect { prefs ->
                    preferences = prefs
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
