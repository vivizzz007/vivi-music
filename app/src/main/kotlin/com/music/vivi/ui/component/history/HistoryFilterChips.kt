package com.music.vivi.ui.component.history

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.vivi.R
import com.music.vivi.constants.HistorySource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterChips(
    historySource: HistorySource,
    isLoggedIn: Boolean,
    onSourceSelected: (HistorySource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedChip = historySource

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chips = if (isLoggedIn) {
            listOf(
                HistorySource.LOCAL to stringResource(R.string.local_history),
                HistorySource.REMOTE to stringResource(R.string.remote_history)
            )
        } else {
            listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
        }

        chips.forEach { (source, label) ->
            val isSelected = selectedChip == source

            // Animate the corner radius based on selection
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "corner_radius"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onSourceSelected(source) },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(cornerRadius),
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            )
        }
    }
}
