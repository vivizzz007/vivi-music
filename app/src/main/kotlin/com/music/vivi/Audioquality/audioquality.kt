package com.music.vivi.Audioquality

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.vivi.R
import com.music.vivi.constants.AudioQuality

@Composable
fun getAudioQualityText(quality: AudioQuality): String {
    return when (quality) {
        AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
        AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
        AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
    }
}