package com.music.vivi.utils

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Formats a duration in milliseconds into a string (e.g., "1:23", "12:34", "1:23:45").
 *
 * @param duration Duration in milliseconds.
 * @return Formatted string (e.g. "MM:SS" or "H:MM:SS").
 */
public fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""
    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60
    return when {
        day > 0 -> "%d:%02d:%02d:%02d".format(day, hour, minute, sec)
        hour > 0 -> "%d:%02d:%02d".format(hour, minute, sec)
        else -> "%d:%02d".format(minute, sec)
    }
}

/**
 * Computes the MD5 hash of the input string.
 * Used for generating cache keys.
 */
public fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}

/**
 * Joins non-null/non-empty strings with a bullet separator (•).
 * Useful for subtitles like "Artist • Album • 2024".
 */
public fun joinByBullet(vararg str: String?): String = str
    .filterNot {
        it.isNullOrEmpty()
    }.joinToString(separator = " • ")
