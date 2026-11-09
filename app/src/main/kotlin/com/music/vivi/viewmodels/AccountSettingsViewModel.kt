/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.App
import com.music.vivi.constants.AccountChannelHandleKey
import com.music.vivi.constants.AccountEmailKey
import com.music.vivi.constants.AccountNameKey
import com.music.vivi.constants.DataSyncIdKey
import com.music.vivi.constants.InnerTubeCookieKey
import com.music.vivi.constants.VisitorDataKey
import com.music.vivi.utils.SyncUtils
import com.music.vivi.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.datastore.preferences.core.edit

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val syncUtils: SyncUtils,
) : ViewModel() {

    /**
     * Live sync progress — observe this in the UI to show per-category status badges.
     */
    val syncState = syncUtils.syncState

    /**
     * Logout user and clear all synced content to prevent data mixing between accounts
     */
    fun logoutAndClearSyncedContent(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all YouTube Music synced content first
            syncUtils.clearAllSyncedContent()

            // Then clear account preferences
            App.forgetAccount(context)

            // Clear cookie in UI
            onCookieChange("")
        }
    }

    /**
     * Just logout without clearing library data
     */
    fun logoutKeepData(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            App.forgetAccount(context)
            withContext(Dispatchers.Main) {
                onCookieChange("")
            }
        }
    }

    // Individual force sync methods
    fun forceSyncSongs() {
        viewModelScope.launch(Dispatchers.IO) { 
            syncUtils.syncLikedSongsSuspend() 
            syncUtils.updateLastSyncTime()
        }
    }
    fun forceSyncAlbums() {
        viewModelScope.launch(Dispatchers.IO) { 
            syncUtils.syncLikedAlbumsSuspend() 
            syncUtils.updateLastSyncTime()
        }
    }
    fun forceSyncArtists() {
        viewModelScope.launch(Dispatchers.IO) { 
            syncUtils.syncArtistsSubscriptionsSuspend() 
            syncUtils.updateLastSyncTime()
        }
    }
    fun forceSyncPlaylists() {
        viewModelScope.launch(Dispatchers.IO) { 
            syncUtils.syncSavedPlaylistsSuspend() 
            syncUtils.updateLastSyncTime()
        }
    }

    /**
     * Bypass the 30-min cooldown and force-sync all library categories in parallel.
     * Liked songs, library songs, albums, artists and playlists all run concurrently.
     */
    fun forceSyncLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.forceSyncAll()
        }
    }

    /**
     * Save token credentials atomically to DataStore, then restart the app.
     * This ensures all writes complete before the process is killed,
     * preventing the race condition where Runtime.exit(0) kills the process
     * before async DataStore coroutines finish writing.
     */
    fun saveTokenAndRestart(
        context: Context,
        cookie: String,
        visitorData: String,
        dataSyncId: String,
        accountName: String,
        accountEmail: String,
        accountChannelHandle: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { settings ->
                settings[InnerTubeCookieKey] = cookie
                settings[VisitorDataKey] = visitorData
                settings[DataSyncIdKey] = dataSyncId
                settings[AccountNameKey] = accountName
                settings[AccountEmailKey] = accountEmail
                settings[AccountChannelHandleKey] = accountChannelHandle
            }
            withContext(Dispatchers.Main) {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                Runtime.getRuntime().exit(0)
            }
        }
    }
}
