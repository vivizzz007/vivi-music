/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import java.util.Locale
fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

@Suppress("DEPRECATION")
fun setAppLocale(context: Context, locale: Locale) {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

fun listItemShape(index: Int, count: Int, radius: Dp = 16.dp): Shape {
    return when {
        count <= 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(4.dp)
    }
}
