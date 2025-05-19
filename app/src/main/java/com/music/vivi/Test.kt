package com.music.vivi

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Implementation of App Widget functionality.
 */
class Test : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context)
        }
    }

    override fun onEnabled(context: Context) { }

    override fun onDisabled(context: Context) { }
}

internal fun updateAppWidget(
    context: Context,
) {
    context.getString(R.string.appwidget_text)
}