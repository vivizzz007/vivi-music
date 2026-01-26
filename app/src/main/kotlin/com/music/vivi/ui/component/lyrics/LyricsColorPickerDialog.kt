package com.music.vivi.ui.component.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.vivi.R
import com.music.vivi.models.MediaMetadata
import com.music.vivi.ui.component.LyricsImageCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsColorPickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    lyricsText: String,
    mediaMetadata: MediaMetadata?,
    paletteColors: List<Color>,
    previewBackgroundColor: Color,
    onPreviewBackgroundColorChange: (Color) -> Unit,
    previewTextColor: Color,
    onPreviewTextColorChange: (Color) -> Unit,
    previewSecondaryTextColor: Color,
    onPreviewSecondaryTextColorChange: (Color) -> Unit,
    onShare: () -> Unit,
) {
    if (showDialog) {
        BasicAlertDialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.customize_colors),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .padding(8.dp)
                    ) {
                        if (mediaMetadata != null) {
                            LyricsImageCard(
                                lyricText = lyricsText,
                                mediaMetadata = mediaMetadata,
                                backgroundColor = previewBackgroundColor,
                                textColor = previewTextColor,
                                secondaryTextColor = previewSecondaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Background Color
                    Text(
                        text = stringResource(id = R.string.background_color),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        (
                            paletteColors + listOf(
                                Color(0xFF242424),
                                Color(0xFF121212),
                                Color.White,
                                Color.Black,
                                Color(0xFFF5F5F5)
                            )
                            ).distinct().take(8).forEach { color ->
                            ColorSwatch(
                                color = color,
                                isSelected = previewBackgroundColor == color,
                                onClick = { onPreviewBackgroundColorChange(color) }
                            )
                        }
                    }

                    // Text Color
                    Text(
                        text = stringResource(id = R.string.text_color),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        (
                            paletteColors + listOf(
                                Color.White,
                                Color.Black,
                                Color(0xFF1DB954)
                            )
                            ).distinct().take(8).forEach { color ->
                            ColorSwatch(
                                color = color,
                                isSelected = previewTextColor == color,
                                onClick = { onPreviewTextColorChange(color) }
                            )
                        }
                    }

                    // Secondary Text Color
                    Text(
                        text = stringResource(id = R.string.secondary_text_color),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        (
                            paletteColors.map { it.copy(alpha = 0.7f) } + listOf(
                                Color.White.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.7f),
                                Color(0xFF1DB954)
                            )
                            ).distinct().take(8).forEach { color ->
                            ColorSwatch(
                                color = color,
                                isSelected = previewSecondaryTextColor == color,
                                onClick = { onPreviewSecondaryTextColorChange(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.share))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(
                2.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
    )
}
