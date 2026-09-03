package com.music.vivi.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.music.vivi.R

enum class LogoPreset(val id: String, val displayName: String, @DrawableRes val drawableRes: Int) {
    DEFAULT("default", "Vivi Music", R.drawable.icon),
    MODERN("modern", "Vivi Music", R.drawable.icon),
    WAVE("wave", "Vivi Music", R.drawable.icon),
    NEON("neon", "Vivi Music", R.drawable.icon),
    CUSTOM("custom", "Vivi Music", R.drawable.icon);

    companion object {
        fun fromId(id: String): LogoPreset = DEFAULT
    }
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(id = R.drawable.icon),
        contentDescription = contentDescription,
        modifier = modifier.clip(CircleShape),
        contentScale = contentScale
    )
}
