/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.R
import com.music.vivi.constants.PlaylistSongSortType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
inline fun <reified T : Enum<T>> SortHeader(
    sortType: T,
    sortDescending: Boolean,
    crossinline onSortTypeChange: (T) -> Unit,
    crossinline onSortDescendingChange: (Boolean) -> Unit,
    crossinline sortTypeText: (T) -> Int,
    modifier: Modifier = Modifier,
    showDescending: Boolean? = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val displayDescending = showDescending == true && sortType != PlaylistSongSortType.CUSTOM

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { menuExpanded = !menuExpanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.widthIn(min = 120.dp)
            ) {
                Text(
                    text = stringResource(sortTypeText(sortType)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        trailingButton = {
            if (displayDescending) {
                val description = "Toggle sort order"
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(description) } },
                    state = rememberTooltipState(),
                ) {
                    SplitButtonDefaults.TrailingButton(
                        checked = sortDescending,
                        onCheckedChange = { onSortDescendingChange(it) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.semantics {
                            stateDescription = if (sortDescending) "Descending" else "Ascending"
                            contentDescription = description
                        },
                    ) {
                        val rotation: Float by animateFloatAsState(
                            targetValue = if (sortDescending) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer {
                                    this.rotationZ = rotation
                                },
                            contentDescription = null,
                        )
                    }
                }
            } else {
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.semantics {
                        stateDescription = if (menuExpanded) "Expanded" else "Collapsed"
                        contentDescription = "Show sort options"
                    },
                ) {
                    val rotation: Float by animateFloatAsState(
                        targetValue = if (menuExpanded) 180f else 0f,
                        label = "Trailing Icon Rotation",
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = null,
                    )
                }
            }
        },
        modifier = modifier.padding(vertical = 8.dp)
    )

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier.widthIn(min = 172.dp),
    ) {
        enumValues<T>().forEach { type ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(sortTypeText(type)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(
                            if (sortType == type) R.drawable.radio_button_checked
                            else R.drawable.radio_button_unchecked
                        ),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onSortTypeChange(type)
                    menuExpanded = false
                },
            )
        }
    }
}