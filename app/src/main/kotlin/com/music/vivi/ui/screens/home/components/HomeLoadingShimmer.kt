package com.music.vivi.ui.component.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.innertube.pages.HomePage
import com.music.vivi.ui.component.shimmer.GridItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder

/**
 * Loading state placeholder (Shimmer) for the Home screen.
 * Shows a skeleton UI while data is being fetched.
 */
internal fun LazyListScope.homeLoadingShimmer(isLoading: Boolean, homePage: HomePage?) {
    if (isLoading || (homePage?.continuation != null && homePage.sections.isNotEmpty())) {
        item(key = "loading_shimmer") {
            ShimmerHost(
                modifier = Modifier
            ) {
                TextPlaceholder(
                    height = 36.dp,
                    modifier = Modifier
                        .padding(12.dp)
                        .width(250.dp)
                )
                LazyRow(
                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
                ) {
                    items(4) {
                        GridItemPlaceHolder()
                    }
                }
            }
        }
    }
}
