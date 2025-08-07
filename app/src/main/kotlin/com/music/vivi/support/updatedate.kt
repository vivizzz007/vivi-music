package com.music.vivi.support

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private const val PREFS_NAME = "version_prefs"
    private const val KEY_LAST_VERSION = "last_version"
    private const val KEY_UPDATE_DATE = "update_date"

    /**
     * Checks if the app version has been updated and stores the update date
     * @param context Application context
     * @param currentVersion Current app version (e.g., "3.0.1")
     * @return Pair<Boolean, String?> - First: whether version was updated, Second: update date if updated
     */
    fun checkAndStoreVersionUpdate(context: Context, currentVersion: String): Pair<Boolean, String?> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastStoredVersion = prefs.getString(KEY_LAST_VERSION, null)

        // If no previous version stored, this is first launch
        if (lastStoredVersion == null) {
            storeVersionInfo(prefs, currentVersion, getCurrentDate())
            return Pair(false, null)
        }

        // Check if version has changed
        if (lastStoredVersion != currentVersion) {
            val updateDate = getCurrentDate()
            storeVersionInfo(prefs, currentVersion, updateDate)
            return Pair(true, updateDate)
        }

        // Version hasn't changed, return stored update date
        val storedUpdateDate = prefs.getString(KEY_UPDATE_DATE, null)
        return Pair(false, storedUpdateDate)
    }

    /**
     * Gets the stored update date for the current version
     * @param context Application context
     * @return String? - The date when current version was updated, null if no update recorded
     */
    fun getVersionUpdateDate(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_UPDATE_DATE, null)
    }

    /**
     * Gets the last stored version
     * @param context Application context
     * @return String? - The last stored version, null if none stored
     */
    fun getLastStoredVersion(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_VERSION, null)
    }

    /**
     * Manually set version update date (useful for testing or manual updates)
     * @param context Application context
     * @param version Version to store
     * @param date Date to store (format: "MMM dd, yyyy")
     */
    fun setVersionUpdateDate(context: Context, version: String, date: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        storeVersionInfo(prefs, version, date)
    }

    /**
     * Clears all stored version information
     * @param context Application context
     */
    fun clearVersionInfo(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Checks if current version is newer than stored version
     * @param context Application context
     * @param currentVersion Current version to compare
     * @return Boolean - true if current version is newer
     */
    fun isVersionNewer(context: Context, currentVersion: String): Boolean {
        val storedVersion = getLastStoredVersion(context) ?: return true
        return isNewerVersion(currentVersion, storedVersion)
    }

    /**
     * Compares two version strings
     * @param version1 First version (e.g., "3.0.1")
     * @param version2 Second version (e.g., "3.0.0")
     * @return Boolean - true if version1 is newer than version2
     */
    private fun isNewerVersion(version1: String, version2: String): Boolean {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0

            when {
                part1 > part2 -> return true
                part1 < part2 -> return false
            }
        }

        return false
    }

    /**
     * Stores version and date information in SharedPreferences
     */
    private fun storeVersionInfo(prefs: SharedPreferences, version: String, date: String) {
        prefs.edit().apply {
            putString(KEY_LAST_VERSION, version)
            putString(KEY_UPDATE_DATE, date)
            apply()
        }
    }

    /**
     * Gets current date in formatted string
     * @return String - Current date in "MMM dd, yyyy" format
     */
    private fun getCurrentDate(): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    /**
     * Formats a date string to a more readable format
     * @param dateString Date string to format
     * @return String - Formatted date string
     */
    fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}