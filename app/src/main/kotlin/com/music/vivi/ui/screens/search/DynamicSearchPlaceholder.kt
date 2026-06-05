/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.music.vivi.LocalDatabase
import com.music.vivi.R
import com.music.vivi.constants.SearchSource
import kotlinx.coroutines.flow.first

@Composable
fun DynamicSearchPlaceholder(
    searchSource: SearchSource,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(),
    color: Color = Color.Unspecified
) {
    if (searchSource == SearchSource.LOCAL) {
        Text(
            text = stringResource(R.string.search_library),
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }

    val database = LocalDatabase.current
    val defaultPrompt = stringResource(R.string.search_yt_music)
    val promptWhatsOnMind = stringResource(R.string.search_prompt_whats_on_mind)
    val promptTodaysPick = stringResource(R.string.search_prompt_todays_pick)
    val promptTrending = stringResource(R.string.search_prompt_trending)
    val promptVibe = stringResource(R.string.search_prompt_vibe)
    val promptSongsArtists = stringResource(R.string.search_prompt_songs_artists)

    val templateSong = stringResource(R.string.search_prompt_template_song)
    val templateSongTry = stringResource(R.string.search_prompt_template_song_try)
    val templateArtist = stringResource(R.string.search_prompt_template_artist)

    val placeholders = remember { mutableStateListOf<String>() }

    // Initialize placeholders and fetch taste-based recommendations from the database
    LaunchedEffect(database) {
        val basePrompts = listOf(
            defaultPrompt,
            promptWhatsOnMind,
            promptTodaysPick,
            promptTrending,
            promptVibe,
            promptSongsArtists
        )
        placeholders.clear()
        placeholders.addAll(basePrompts)

        try {
            // Fetch top 5 liked songs to suggest searches
            val liked = database.likedSongsByRowIdAsc().first()
            if (liked.isNotEmpty()) {
                liked.shuffled().take(5).forEach { song ->
                    placeholders.add(templateSong.replace("%s", song.title))
                }
            }
        } catch (e: Exception) {
            // Ignore database/Room errors to ensure fallback placeholder always works
        }

        try {
            // Fetch top 5 quick picks
            val picks = database.quickPicks().first()
            if (picks.isNotEmpty()) {
                picks.shuffled().take(5).forEach { song ->
                    placeholders.add(templateSongTry.replace("%s", song.title))
                }
            }
        } catch (e: Exception) {
            // Ignore database/Room errors
        }

        try {
            // Fetch top 3 most listened artists
            val topArtists = database.allArtistsByPlayTime().first()
            if (topArtists.isNotEmpty()) {
                topArtists.shuffled().take(3).forEach { artist ->
                    placeholders.add(templateArtist.replace("%s", artist.title))
                }
            }
        } catch (e: Exception) {
            // Ignore database/Room errors
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(placeholders.size) {
        if (placeholders.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(10000L) // Rotate/cycle placeholders every 10 seconds
                currentIndex = (currentIndex + 1) % placeholders.size
            }
        }
    }

    val currentText = if (placeholders.isNotEmpty() && currentIndex < placeholders.size) {
        placeholders[currentIndex]
    } else {
        defaultPrompt
    }

    AnimatedContent(
        targetState = currentText,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "SearchPlaceholderAnimation"
    ) { text ->
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}
