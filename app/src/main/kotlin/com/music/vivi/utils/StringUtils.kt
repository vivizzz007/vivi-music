package com.music.vivi.utils

import java.math.BigInteger
import java.security.MessageDigest

fun makeTimeString(duration: Long?): String {
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

fun md5(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
}


fun formatCount(count: Long): String {
    if (count < 1000) return count.toString()
    val exp = (Math.log(count.toDouble()) / Math.log(1000.0)).toInt()
    return String.format("%.1f%c", count / Math.pow(1000.0, exp.toDouble()), "kMGTPE"[exp - 1])
}

fun abbreviateMonthlyListeners(input: String, isCompact: Boolean = false): String {
    // Input is typically "1,234,567 monthly listeners"
    // We want to extract the number, format it, and append text based on isCompact
    
    val numberString = input.filter { it.isDigit() }
    val number = numberString.toLongOrNull() ?: return input

    val formattedNumber = formatCount(number)
    
    return if (isCompact) {
        formattedNumber
    } else {
        "$formattedNumber monthly listeners"
    }
}

fun joinByBullet(vararg str: String?) =
    str
        .filterNot {
            it.isNullOrEmpty()
        }.joinToString(separator = " • ")
