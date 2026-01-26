package com.music.vivi.ui.component.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.PlaylistItem
import com.music.vivi.LocalPlayerConnection
import com.music.vivi.R
import com.music.vivi.constants.ListThumbnailSize
import com.music.vivi.ui.component.LocalMenuState
import com.music.vivi.ui.component.NavigationTitle
import com.music.vivi.utils.ImmutableList
import kotlinx.coroutines.CoroutineScope

/**
 * Section displaying the user's YouTube Music playlists in a horizontal row.
 */
@Composable
internal fun HomeAccountPlaylists(
    accountPlaylists: ImmutableList<PlaylistItem>,
    accountName: String,
    accountImageUrl: String?,
    activeId: String?,
    activeAlbumId: String?,
    isPlaying: Boolean,
    navController: NavController,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    if (accountPlaylists.isEmpty()) return

    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val context = LocalContext.current

    NavigationTitle(
        label = stringResource(R.string.your_youtube_playlists),
        title = accountName,
        thumbnail = {
            if (accountImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(accountImageUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(accountImageUrl)
                        .crossfade(false)
                        .build(),
                    placeholder = painterResource(id = R.drawable.person),
                    error = painterResource(id = R.drawable.person),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(ListThumbnailSize)
                )
            }
        },
        onClick = {
            navController.navigate("account")
        },
        modifier = Modifier
    )

    LazyRow(
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            items = accountPlaylists.distinctBy { it.id },
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
