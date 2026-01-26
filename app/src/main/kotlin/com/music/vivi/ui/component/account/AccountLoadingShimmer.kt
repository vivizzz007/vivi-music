package com.music.vivi.ui.component.account

import androidx.compose.foundation.lazy.grid.LazyGridScope
import com.music.vivi.ui.component.shimmer.GridItemPlaceHolder
import com.music.vivi.ui.component.shimmer.ShimmerHost

fun LazyGridScope.accountLoadingShimmer(isLoading: Boolean) {
    if (isLoading) {
        items(8) {
            ShimmerHost {
                GridItemPlaceHolder(fillMaxWidth = true)
            }
        }
    }
}
