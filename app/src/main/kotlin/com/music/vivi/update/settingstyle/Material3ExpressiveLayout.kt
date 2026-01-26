package com.music.vivi.update.settingstyle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Material3ExpressiveSettingsGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    items: List<@Composable () -> Unit>,
) {
    val cornerRadius = 24.dp
    val connectionRadius = 4.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val shape = when {
                index == 0 -> RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius,
                    bottomStart = connectionRadius,
                    bottomEnd = connectionRadius
                )
                index == items.size - 1 -> RoundedCornerShape(
                    topStart = connectionRadius,
                    topEnd = connectionRadius,
                    bottomStart = cornerRadius,
                    bottomEnd = cornerRadius
                )
                else -> RoundedCornerShape(connectionRadius)
            }

            Box(
                modifier = Modifier
                    .background(containerColor, shape)
                    .clip(shape)
            ) {
                item()
            }
        }
    }
}
