/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore instance for Wear OS authentication credentials.
 */
val Context.wearAuthDataStore: DataStore<Preferences> by preferencesDataStore(name = "wear_auth_preferences")

/**
 * Backward compatibility alias pointing to [wearAuthDataStore].
 */
val Context.wearDataStore: DataStore<Preferences>
    get() = wearAuthDataStore

/**
 * Preference keys and reactive DataStore accessor for YouTube Music session credentials on Wear OS.
 */
class WearAuthPreferences(private val context: Context) {

    val innerTubeCookie: Flow<String?> = context.wearAuthDataStore.data.map { it[InnerTubeCookieKey] }
    val dataSyncId: Flow<String?> = context.wearAuthDataStore.data.map { it[DataSyncIdKey] }
    val visitorData: Flow<String?> = context.wearAuthDataStore.data.map { it[VisitorDataKey] }
    val accountName: Flow<String?> = context.wearAuthDataStore.data.map { it[AccountNameKey] }
    val accountEmail: Flow<String?> = context.wearAuthDataStore.data.map { it[AccountEmailKey] }
    val accountChannelHandle: Flow<String?> = context.wearAuthDataStore.data.map { it[AccountChannelHandleKey] }

    /**
     * Persists authentication credentials and user profile information in DataStore.
     */
    suspend fun saveCredentials(
        cookie: String,
        dataSyncId: String? = null,
        visitorData: String? = null,
        accountName: String? = null,
        accountEmail: String? = null,
        accountChannelHandle: String? = null,
    ) {
        context.wearAuthDataStore.edit { prefs ->
            prefs[InnerTubeCookieKey] = cookie
            if (dataSyncId != null) prefs[DataSyncIdKey] = dataSyncId else prefs.remove(DataSyncIdKey)
            if (visitorData != null) prefs[VisitorDataKey] = visitorData else prefs.remove(VisitorDataKey)
            if (accountName != null) prefs[AccountNameKey] = accountName else prefs.remove(AccountNameKey)
            if (accountEmail != null) prefs[AccountEmailKey] = accountEmail else prefs.remove(AccountEmailKey)
            if (accountChannelHandle != null) {
                prefs[AccountChannelHandleKey] = accountChannelHandle
            } else {
                prefs.remove(AccountChannelHandleKey)
            }
        }
    }

    /**
     * Clears all session credentials and profile information from DataStore.
     */
    suspend fun clearCredentials() {
        context.wearAuthDataStore.edit { it.clear() }
    }

    companion object {
        val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
        val VisitorDataKey = stringPreferencesKey("visitorData")
        val DataSyncIdKey = stringPreferencesKey("dataSyncId")
        val AccountNameKey = stringPreferencesKey("accountName")
        val AccountEmailKey = stringPreferencesKey("accountEmail")
        val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")

        suspend fun saveCredentials(
            context: Context,
            cookie: String,
            dataSyncId: String? = null,
            visitorData: String? = null,
            accountName: String? = null,
            accountEmail: String? = null,
            accountChannelHandle: String? = null,
        ) {
            WearAuthPreferences(context).saveCredentials(
                cookie = cookie,
                dataSyncId = dataSyncId,
                visitorData = visitorData,
                accountName = accountName,
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
            )
        }

        suspend fun clearCredentials(context: Context) {
            WearAuthPreferences(context).clearCredentials()
        }
    }
}
