/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.music.vivi.github.news.constants

import androidx.datastore.preferences.core.stringPreferencesKey

object AnnouncementConstants {
    const val REPO_OWNER = "ViviMusicGroup"
    const val REPO_NAME = "vivinews"
    const val BRANCH = "main"

    const val ANNOUNCEMENT_URL = "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/$BRANCH/News/1.md"

    val DismissedAnnouncementIdKey = stringPreferencesKey("dismissed_announcement_id")
}
