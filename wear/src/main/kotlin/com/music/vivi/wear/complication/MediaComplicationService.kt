/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.ui.WearMainActivity
import kotlinx.coroutines.flow.firstOrNull

/**
 * Watch face complication showing current/recent playback state.
 *
 * Displays the most recently downloaded song title, or "Vivi Music" as default.
 * Tapping opens [WearMainActivity].
 */
class MediaComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(getString(R.string.app_name)).build(),
            contentDescription = PlainComplicationText.Builder(
                getString(R.string.complication_media_label),
            ).build(),
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val recentTitle = try {
            WearApp.instance.database.downloadedSongsByCreateDateAsc()
                .firstOrNull()
                ?.lastOrNull()
                ?.song?.title
        } catch (_: Exception) {
            null
        } ?: getString(R.string.app_name)

        val launchIntent = Intent(this, WearMainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val tapAction = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(recentTitle).build(),
            contentDescription = PlainComplicationText.Builder(
                getString(R.string.complication_media_label),
            ).build(),
        )
            .setTapAction(tapAction)
            .build()
    }
}
