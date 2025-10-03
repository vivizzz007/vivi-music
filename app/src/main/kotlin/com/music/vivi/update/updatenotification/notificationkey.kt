package com.music.vivi.update.updatenotification

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey



// SharedPreferences constants for compatibility
const val PREFS_NAME = "update_settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"
const val KEY_UPDATE_INTERVAL = "update_check_interval"

// Default values
const val DEFAULT_UPDATE_INTERVAL = 4 // hours

/**
 * Get the update check interval from SharedPreferences
 */
fun getUpdateCheckInterval(context: Context): Int {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getInt(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL)
}

/**
 * Save the update check interval to SharedPreferences
 */
fun saveUpdateCheckInterval(context: Context, hours: Int) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putInt(KEY_UPDATE_INTERVAL, hours).apply()

    // Reschedule the update checker with new interval
    UpdateChecker.scheduleUpdateCheck(context, hours.toLong())
}

/**
 * Check if automatic updates are enabled
 */
fun isAutoUpdateEnabled(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
}

/**
 * Check if update notifications are enabled
 */
fun isUpdateNotificationsEnabled(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)
}

/**
 * Save update notification preference
 */
fun saveUpdateNotificationPreference(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
}