package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.App
import com.music.vivi.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Account Settings screen.
 * Handles logout and account cleanup operations.
 */
@HiltViewModel
public class AccountSettingsViewModel @Inject constructor(private val syncUtils: SyncUtils) : ViewModel() {

    /**
     * Logout user and clear all synced content to prevent data mixing between accounts
     */
    public fun logoutAndClearSyncedContent(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all YouTube Music synced content first
            syncUtils.clearAllSyncedContent()

            // Then clear account preferences
            App.forgetAccount(context)

            // Clear cookie in UI
            onCookieChange("")
        }
    }
}
