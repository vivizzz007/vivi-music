package com.music.vivi.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.music.vivi.R

/**
 * Sealed class defining the Main Bottom Navigation Tab routes.
 * Used for defining the bottom bar items.
 */
@Immutable
public sealed class Screens(
    @StringRes public val titleId: Int,
    @DrawableRes public val iconIdInactive: Int,
    @DrawableRes public val iconIdActive: Int,
    public val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library)
    }
}
