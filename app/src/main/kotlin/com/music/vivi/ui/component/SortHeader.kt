package com.music.vivi.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.vivi.R
import com.music.vivi.constants.PlaylistSongSortType

/**
 * A header used in lists to handle sorting.
 * Provides a split button for easy toggling of sort order (ASC/DESC) and a dropdown for selecting sort type.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
public inline fun <reified T : Enum<T>> SortHeader(
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

    Box(modifier = modifier.padding(vertical = 8.dp).wrapContentSize()) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = { menuExpanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.widthIn(min = 120.dp)
                ) {
                    Text(
                        text = stringResource(sortTypeText(sortType)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            trailingButton = {
                if (displayDescending) {
                    SplitButtonDefaults.TrailingButton(
                        checked = !sortDescending,
                        onCheckedChange = { onSortDescendingChange(!sortDescending) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (!sortDescending) 180f else 0f,
                            label = "Sort Order Rotation"
                        )
                        Icon(
                            painter = painterResource(R.drawable.arrow_downward),
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer { rotationZ = rotation },
                            contentDescription = null
                        )
                    }
                } else {
                    SplitButtonDefaults.TrailingButton(
                        checked = menuExpanded,
                        onCheckedChange = { menuExpanded = it },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (menuExpanded) 180f else 0f,
                            label = "Dropdown Arrow Rotation"
                        )
                        Icon(
                            painter = painterResource(R.drawable.arrow_downward),
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer { rotationZ = rotation },
                            contentDescription = null
                        )
                    }
                }
            }
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.widthIn(min = 172.dp)
        ) {
            enumValues<T>().forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(sortTypeText(type)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(
                                if (sortType == type) {
                                    R.drawable.radio_button_checked
                                } else {
                                    R.drawable.radio_button_unchecked
                                }
                            ),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onSortTypeChange(type)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}
