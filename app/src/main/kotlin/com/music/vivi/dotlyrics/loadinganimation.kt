package com.music.vivi.dotlyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Music
import compose.icons.fontawesomeicons.solid.Save
import compose.icons.fontawesomeicons.solid.Search

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingCard(
    fetching: Pair<Boolean, String>,
    searching: Pair<Boolean, String>,
    colors: Pair<Color, Color>
) {
    AnimatedVisibility (
        visible = searching.first,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(200.dp),
                    color = colors.first
                )

                Icon(
                    imageVector = FontAwesomeIcons.Solid.Search,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colors.second
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = searching.second,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    AnimatedVisibility (
        visible = fetching.first,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(200.dp),
                    color = colors.first
                )

                Icon(
                    FontAwesomeIcons.Solid.Music,
                            contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = colors.second
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fetching.second,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}