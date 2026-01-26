package com.music.vivi.ui.component.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.db.entities.Album
import com.music.vivi.db.entities.Artist
import com.music.vivi.db.entities.Playlist
import com.music.vivi.db.entities.Song
import com.music.vivi.models.SimilarRecommendation
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.utils.ImmutableList
import kotlinx.coroutines.CoroutineScope

/**
 * "Similar To..." sections.
 * Displays recommendations based on a specific artist or album the user likes (e.g. "Similar to Metallica", "More like Nevermind").
 */
internal fun LazyListScope.homeSimilarRecommendations(
    similarRecommendations: ImmutableList<SimilarRecommendation>,
    activeId: String?,
    activeAlbumId: String?,
    isPlaying: Boolean,
    navController: NavController,
    scope: CoroutineScope,
) {
    similarRecommendations.forEachIndexed { index, recommendation ->
        item(key = "similar_to_title_$index") {
            NavigationTitle(
                label = stringResource(R.string.similar_to),
                title = recommendation.title.title,
                thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                    {
                        val shape =
                            if (recommendation.title is Artist) {
                                CircleShape
                            } else {
                                RoundedCornerShape(
                                    ThumbnailCornerRadius
                                )
                            }
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(shape)
                        )
                    }
                },
                onClick = {
                    when (recommendation.title) {
                        is Song -> navController.navigate("album/${recommendation.title.album!!.id}")
                        is Album -> navController.navigate("album/${recommendation.title.id}")
                        is Artist -> navController.navigate("artist/${recommendation.title.id}")
                        is Playlist -> {}
                    }
                },
                modifier = Modifier.animateItem()
            )
        }

        item(key = "similar_to_list_$index") {
            // Need access to dependencies. But extension function cannot access Locals.
            // Locals like LocalPlayerConnection must be passed OR accessed in CONTENT lambda.
            // But LazyListScope is not @Composable context?
            // "item" block IS @Composable context.
            // So we can access Locals inside item { }.

            // However, arguments passed to function are evaluated at call site.
            // So we can pass dependencies.
            // I'll assume dependencies are passed or retrieved inside item.
            // BUT "item" block is unit-returning.

            val playerConnection = LocalPlayerConnection.current ?: return@item
            val menuState = LocalMenuState.current

            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier.animateItem()
            ) {
                items(recommendation.items) { item ->
                    HomeYTGridItem(
                        item = item,
                        isPlaying = isPlaying,
                        activeId = activeId,
                        activeAlbumId = activeAlbumId,
                        navController = navController,
                        menuState = menuState,
                        scope = scope,
                        playerConnection = playerConnection
                    )
                }
            }
        }
    }
}
