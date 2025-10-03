package com.music.vivi.update.updatetime

// VersionUtils.kt

object VersionUtils {

    /**
     * Compares two version strings (e.g., "v3.0.9" vs "v2.1.1") to see if version1 is newer
     * than version2. It handles the optional 'v' prefix.
     * * @param version1 The version to check (e.g., the latest remote version).
     * @param version2 The baseline version (e.g., the current installed version).
     * @return true if version1 is strictly newer than version2, false otherwise (older or equal).
     */
    fun isNewerVersion(version1: String, version2: String): Boolean {

        // Clean the 'v' prefix and split the strings by the dot separator.
        // If a part isn't an integer, mapNotNull removes it, but we can safely default to 0.
        val parts1 = version1.trimStart('v').split('.').map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.trimStart('v').split('.').map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            // Get the part number, defaulting to 0 if the version string is shorter
            // (e.g., "1.2" is treated as "1.2.0" when comparing to "1.2.1").
            val part1 = parts1.getOrElse(i) { 0 }
            val part2 = parts2.getOrElse(i) { 0 }

            when {
                // If V1's part is greater, V1 is newer
                part1 > part2 -> return true
                // If V1's part is smaller, V1 is older (or V2 is newer)
                part1 < part2 -> return false
            }
            // If parts are equal, continue to the next part (minor, patch, etc.)
        }

        // If the loop finishes, the versions are identical
        return false
    }
}