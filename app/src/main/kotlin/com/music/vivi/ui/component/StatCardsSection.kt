package com.music.vivi.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.R
import androidx.compose.material3.MaterialTheme

@Composable
fun StatCardsSection(
    topSize: String,
    navController: NavController,
    showLiked: Boolean,
    showDownloaded: Boolean,
    showTop: Boolean,
    showCached: Boolean,
    modifier: Modifier = Modifier,
) {
    val cards = mutableListOf<@Composable (Modifier) -> Unit>()

    if (showLiked) {
        cards.add { mod ->
            StatCard(
                title = stringResource(R.string.liked),
                icon = painterResource(R.drawable.favorite),
                onClick = { navController.navigate("auto_playlist/liked") },
                modifier = mod
            )
        }
    }
    if (showDownloaded) {
        cards.add { mod ->
            StatCard(
                title = stringResource(R.string.offline),
                icon = painterResource(R.drawable.offline),
                onClick = { navController.navigate("auto_playlist/downloaded") },
                modifier = mod
            )
        }
    }
    if (showCached) {
        cards.add { mod ->
            StatCard(
                title = stringResource(R.string.cached_playlist),
                icon = painterResource(R.drawable.cached),
                onClick = { navController.navigate("cache_playlist/cached") },
                modifier = mod
            )
        }
    }
    if (showTop) {
        cards.add { mod ->
            StatCard(
                title = stringResource(R.string.my_top) + " $topSize",
                countText = topSize,
                icon = painterResource(R.drawable.trending_up),
                onClick = { navController.navigate("top_playlist/$topSize") },
                modifier = mod
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { card ->
                    card(Modifier.weight(1f))
                }
                if (rowCards.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
