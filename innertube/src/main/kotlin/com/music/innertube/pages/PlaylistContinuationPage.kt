package com.music.innertube.pages

import com.music.innertube.models.SongItem

data class PlaylistContinuationPage(val songs: List<SongItem>, val continuation: String?)
