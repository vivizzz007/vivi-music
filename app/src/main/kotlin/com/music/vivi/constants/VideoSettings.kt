package com.music.vivi.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// Video Quality Settings
enum class VideoQuality {
    AUTO,
    P144,
    P240,
    P360,
    P480,
    P720,
    P1080,
    P1440,
    P2160
}

val VideoQualityKey = stringPreferencesKey("videoQuality")
val VideoQualityDefaultValue = VideoQuality.AUTO.name

val EnableVideoModeKey = booleanPreferencesKey("enableVideoMode")
val EnableVideoModeDefaultValue = true

// WiFi Fast Mode Settings
val WiFiFastModeKey = booleanPreferencesKey("wifiFastMode")
val WiFiFastModeDefaultValue = true