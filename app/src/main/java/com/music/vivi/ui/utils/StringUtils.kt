package com.music.vivi.ui.utils

import kotlin.math.absoluteValue

fun formatFileSize(sizeBytes: Long): String {
    val prefix = if (sizeBytes < 0) "-" else ""
    var result: Long = sizeBytes.absoluteValue
    var suffix = "B"
    if (result > 900) {
        suffix = "KB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "MB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "GB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "TB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "PB"
        result /= 1024
    }
    return "$prefix$result $suffix"
}

fun numberToAlpha(l: Long): String {
    val alphabetMap = ('A'..'J').toList()
    val weh = if (l < 0) "0" else l.toString()
    val lengthStr = if (weh.length.toInt() < 10) {
        "0" + weh.length.toInt()
    } else {
        weh.length.toInt().toString()
    }

    return (lengthStr + weh + "\u0000").map {
        if (it == '\u0000') {
            "0"
        } else {
            alphabetMap[it.digitToInt()]
        }
    }.joinToString("")
}