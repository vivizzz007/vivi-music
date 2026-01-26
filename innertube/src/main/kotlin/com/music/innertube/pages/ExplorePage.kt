package com.music.innertube.pages

import com.music.innertube.models.AlbumItem

data class ExplorePage(val newReleaseAlbums: List<AlbumItem>, val moodAndGenres: List<MoodAndGenres.Item>)
