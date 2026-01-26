package com.music.vivi.ui.component.album

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.db.entities.Song
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.MenuState
import com.music.vivi.ui.menu.SelectionSongMenu
import com.music.vivi.ui.utils.ItemWrapper
import com.music.vivi.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumTopBar(
    selection: Boolean,
    wrappedSongs: List<ItemWrapper<Song>>,
    onSelectionChange: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onBack: () -> Unit,
    navController: NavController,
    menuState: MenuState,
    transparentAppBar: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            if (selection) {
                val count = wrappedSongs.count { it.isSelected }
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (selection) {
                        onSelectionChange(false)
                    } else {
                        onBack()
                    }
                },
                onLongClick = {
                    if (!selection) {
                        navController.backToMain()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (selection) R.drawable.close else R.drawable.arrow_back
                    ),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (selection) {
                val count = wrappedSongs.count { it.isSelected }
                IconButton(
                    onClick = {
                        if (count == wrappedSongs.size) {
                            onDeselectAll()
                        } else {
                            onSelectAll()
                        }
                    },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }
                                    .map { it.item },
                                onDismiss = menuState::dismiss,
                                clearAction = { onSelectionChange(false) }
                            )
                        }
                    },
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            }
        },
        colors = if (transparentAppBar && !selection) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
        scrollBehavior = scrollBehavior
    )
}
