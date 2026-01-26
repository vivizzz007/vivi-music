package com.music.vivi.ui.component.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.pages.HomePage
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.constants.ThumbnailCornerRadius
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.utils.ImmutableList
import kotlinx.coroutines.CoroutineScope

/**
 * Renders dynamically provided sections from the InnerTube Home Endpoint.
 * These are the standard "Mixed for you", "New Releases", "Recommended music videos" shelves.
 */
internal fun LazyListScope.homeInnertubeSections(
    sections: ImmutableList<HomePage.Section>,
    activeId: String?,
    activeAlbumId: String?,
    isPlaying: Boolean,
    navController: NavController,
    scope: CoroutineScope,
) {
    sections.forEachIndexed { index, section ->
        item(key = "home_section_title_$index") {
            NavigationTitle(
                title = section.title,
                label = section.label,
                thumbnail = section.thumbnail?.let { thumbnailUrl ->
                    {
                        val shape =
                            if (section.endpoint?.isArtistEndpoint == true) {
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
                onClick = section.endpoint?.browseId?.let { browseId ->
                    {
                        if (browseId == "FEmusic_moods_and_genres") {
                            navController.navigate("mood_and_genres")
                        } else {
                            navController.navigate("browse/$browseId")
                        }
                    }
                },
                modifier = Modifier
            )
        }

        item(key = "home_section_list_$index") {
            val playerConnection = LocalPlayerConnection.current ?: return@item
            val menuState = LocalMenuState.current

            LazyRow(
                contentPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = section.items,
                    key = { it.id }
                ) { item ->
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
