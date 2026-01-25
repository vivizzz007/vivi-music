package com.music.vivi.ui.component.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.R

@Composable
fun LyricsActionButtons(
    isSelectionModeActive: Boolean,
    selectedCount: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelectionModeActive) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // Row containing both close and share buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button (circular, right side of share)
                Box(
                    modifier = Modifier
                        .size(48.dp) // Larger for better touch target
                        .background(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share button (rectangular with text)
                Row(
                    modifier = Modifier
                        .background(
                            color = if (selectedCount > 0) {
                                Color.White.copy(alpha = 0.9f) // White background when active
                            } else {
                                Color.White.copy(alpha = 0.5f)
                            }, // Lighter white when inactive
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable(enabled = selectedCount > 0, onClick = onShare)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(R.string.share_selected),
                        tint = Color.Black, // Black icon on white background
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.share),
                        color = Color.Black, // Black text on white background
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
