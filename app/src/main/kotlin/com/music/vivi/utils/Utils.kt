package com.music.vivi.utils

import android.content.Context
import android.content.res.Configuration
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