package com.music.vivi.ui.component.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.innertube.pages.MoodAndGenres
import com.music.vivi.R
import com.music.vivi.constants.MoodAndGenresButtonHeight
import com.music.vivi.ui.component.MoodAndGenresButton
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.ui.component.shimmer.ShimmerHost
import com.music.vivi.ui.component.shimmer.TextPlaceholder
import com.music.vivi.utils.ImmutableList

/**
 * Section displaying the "Moods & Genres" buttons.
 * Usually displayed at the bottom of the home screen or in the Explore tab.
 */
internal fun LazyListScope.homeMoodAndGenres(
    moodAndGenres: ImmutableList<MoodAndGenres.Item>?,
    isLoading: Boolean,
    navController: NavController,
) {
    moodAndGenres?.let { list ->
        item(key = "mood_and_genres_title") {
            NavigationTitle(
                title = stringResource(R.string.mood_and_genres),
                onClick = {
                    navController.navigate("mood_and_genres")
                },
                modifier = Modifier
            )
        }
        item(key = "mood_and_genres_list") {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(4),
                contentPadding = PaddingValues(6.dp),
                modifier = Modifier
                    .height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp)
            ) {
                items(list) {
                    MoodAndGenresButton(
                        title = it.title,
                        onClick = {
                            navController.navigate(
                                "youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}"
                            )
                        },
                        modifier = Modifier
                            .padding(6.dp)
                            .width(180.dp)
                    )
                }
            }
        }
    }

    if (isLoading && moodAndGenres == null) {
        item(key = "mood_and_genres_shimmer") {
            ShimmerHost(
                modifier = Modifier
            ) {
                TextPlaceholder(
                    height = 36.dp,
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                        .width(250.dp)
                )

                repeat(4) {
                    Row {
                        repeat(2) {
                            TextPlaceholder(
                                height = MoodAndGenresButtonHeight,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(200.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
