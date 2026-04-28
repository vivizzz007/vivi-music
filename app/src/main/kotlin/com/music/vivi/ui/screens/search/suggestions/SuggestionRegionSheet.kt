package com.music.vivi.ui.screens.search.suggestions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.constants.SuggestionRegionSlugToName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionRegionSheet(
    currentRegionSlug: String,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredRegions = remember(searchQuery) {
        SuggestionRegionSlugToName.toList()
            .filter { it.first != "system" } // Filter out system from the main list
            .filter { it.second.contains(searchQuery, ignoreCase = true) }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Choose Suggestions Region",
                style = MaterialTheme.typography.labelLarge
            )
            
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("Search regions...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {}
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                // System Default Section
                item {
                    Text(
                        text = "System",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
                item {
                    val isSelected = currentRegionSlug == "system"
                    RegionListItem(
                        headlineContent = { Text("System Default") },
                        selected = isSelected,
                        items = 1,
                        index = 0,
                        onClick = {
                            onRegionSelected("system")
                            scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                                if (!bottomSheetState.isVisible) {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
                
                item {
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    Text(
                        text = "Countries & Regions",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(
                            top = 14.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                    )
                }

                itemsIndexed(filteredRegions, key = { _, pair -> pair.first }) { index, (slug, name) ->
                    val isSelected = slug == currentRegionSlug
                    RegionListItem(
                        headlineContent = { Text(name) },
                        selected = isSelected,
                        items = filteredRegions.size,
                        index = index,
                        onClick = {
                            onRegionSelected(slug)
                            scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                                if (!bottomSheetState.isVisible) {
                                    onDismiss()
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(2.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    
    LaunchedEffect(searchQuery) {
        if (filteredRegions.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }
}

@Composable
fun RegionListItem(
    headlineContent: @Composable (() -> Unit),
    selected: Boolean,
    items: Int,
    index: Int,
    supportingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val top by animateDpAsState(
        if (isPressed) 36.dp
        else {
            if (items == 1 || index == 0) 20.dp
            else 4.dp
        },
        label = "top"
    )
    val bottom by animateDpAsState(
        if (isPressed) 36.dp
        else {
            if (items == 1 || index == items - 1) 20.dp
            else 4.dp
        },
        label = "bottom"
    )

    ListItem(
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected"
                )
            }
        } else null,
        colors =
            if (selected) ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.3f
                ), leadingIconColor = MaterialTheme.colorScheme.primary
            )
            else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .clip(
                if (selected) CircleShape
                else RoundedCornerShape(
                    topStart = top,
                    topEnd = top,
                    bottomStart = bottom,
                    bottomEnd = bottom
                )
            )
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
    )
}
