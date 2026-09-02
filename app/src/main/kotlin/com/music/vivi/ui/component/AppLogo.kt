/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.music.vivi.R
import com.music.vivi.constants.AppLogoPresetKey
import com.music.vivi.constants.CustomLogoPathKey
import com.music.vivi.utils.rememberPreference
import java.io.File

enum class LogoPreset(val id: String, val displayName: String, @DrawableRes val drawableRes: Int) {
    DEFAULT("default", "Classic Vivi", R.drawable.icon),
    MODERN("modern", "Modern Disc", R.drawable.vivi_music_library_circle),
    WAVE("wave", "Minimalist Wave", R.drawable.vivimusicnotification),
    NEON("neon", "Vivi Neon", R.drawable.viviwrapped_v2),
    CUSTOM("custom", "Custom Logo", 0);

    companion object {
        fun fromId(id: String): LogoPreset = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val (presetId) = rememberPreference(AppLogoPresetKey, defaultValue = LogoPreset.DEFAULT.id)
    val (customPath) = rememberPreference(CustomLogoPathKey, defaultValue = "")

    val preset = remember(presetId) { LogoPreset.fromId(presetId) }

    if (preset == LogoPreset.CUSTOM && customPath.isNotBlank()) {
        val file = remember(customPath) { File(customPath) }
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
            return
        }
    }

    val resId = if (preset.drawableRes != 0) preset.drawableRes else R.drawable.icon
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
