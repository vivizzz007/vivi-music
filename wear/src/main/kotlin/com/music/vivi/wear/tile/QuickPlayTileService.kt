/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.music.vivi.wear.R
import com.music.vivi.wear.WearApp
import com.music.vivi.wear.ui.WearMainActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

/**
 * Wear OS Quick-Launch Tile for Vivi Music.
 *
 * Shows a glanceable tile with the most recently downloaded song title
 * and a play button that launches into the app.
 */
class QuickPlayTileService : TileService() {

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val ID_PLAY_ICON = "play_icon"
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest) =
        com.google.common.util.concurrent.Futures.immediateFuture(buildTile())

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest) =
        com.google.common.util.concurrent.Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .addIdToImageMapping(
                    ID_PLAY_ICON,
                    ResourceBuilders.ImageResource.Builder()
                        .setAndroidResourceByResId(
                            ResourceBuilders.AndroidImageResourceByResId.Builder()
                                .setResourceId(R.drawable.ic_play_tile)
                                .build(),
                        )
                        .build(),
                )
                .build(),
        )

    private fun buildTile(): TileBuilders.Tile {
        val recentTitle = runBlocking {
            try {
                WearApp.instance.database.downloadedSongsByCreateDateAsc()
                    .firstOrNull()
                    ?.lastOrNull()
                    ?.song?.title
            } catch (_: Exception) {
                null
            }
        } ?: getString(R.string.tile_quick_launch)

        val launchIntent = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(WearMainActivity::class.java.name)
                    .build(),
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId("launch_vivi")
            .setOnClick(launchIntent)
            .build()

        val layout = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        LayoutElementBuilders.Image.Builder()
                            .setResourceId(ID_PLAY_ICON)
                            .setWidth(dp(32f))
                            .setHeight(dp(32f))
                            .build(),
                    )
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(dp(4f))
                            .build(),
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(getString(R.string.tile_quick_launch))
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setColor(argb(0xFFBB86FC.toInt()))
                                    .setSize(sp(14f))
                                    .build(),
                            )
                            .build(),
                    )
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(dp(2f))
                            .build(),
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(recentTitle)
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setColor(argb(0xFFCAC4D0.toInt()))
                                    .setSize(sp(11f))
                                    .build(),
                            )
                            .setMaxLines(1)
                            .build(),
                    )
                    .build(),
            )
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(layout)
                            .build(),
                    )
                    .build(),
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(300_000L)
            .build()
    }
}
