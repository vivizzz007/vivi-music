package com.music.vivi.ui.component.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.music.vivi.R
import com.music.vivi.db.entities.Artist
import com.music.vivi.ui.component.media.artists.ArtistListItem

/**
 * A list item representing an Artist in the Library.
 *
 * This component is stateless and delegates actions to the caller.
 *
 * @param artist The [Artist] entity to display.
 * @param onArtistClick Callback invoked when the item body is clicked.
 * @param onMenuClick Callback invoked when the "more" action (three dots) is clicked.
 * @param modifier Modifier to be applied to the layout.
 */
@Composable
fun LibraryArtistListItem(
    artist: Artist,
    onArtistClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArtistListItem(
        artist = artist,
        trailingContent = {
            IconButton(
                onClick = onMenuClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.options) // Ensure this string exists or use a generic one
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onArtistClick)
    )
}
