/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.vivi.R
import com.music.vivi.constants.PlayerActionButton

fun getPlayerButtonIcon(button: PlayerActionButton): Int {
    return when (button) {
        PlayerActionButton.QUEUE -> R.drawable.queue_music
        PlayerActionButton.SLEEP_TIMER -> R.drawable.bedtime
        PlayerActionButton.LYRICS -> R.drawable.lyrics
        PlayerActionButton.COMMENTS -> R.drawable.chat_msg
        PlayerActionButton.SHUFFLE -> R.drawable.shuffle
        PlayerActionButton.REPEAT -> R.drawable.repeat
        PlayerActionButton.LIKE -> R.drawable.favorite
        PlayerActionButton.DOWNLOAD -> R.drawable.download
        PlayerActionButton.EQUALIZER -> R.drawable.equalizer
        PlayerActionButton.AUDIO_DEVICE -> R.drawable.speaker_apple
        PlayerActionButton.MORE_OPTIONS -> R.drawable.more_vert
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizePlayerButtonsScreen(
    initialButtons: List<PlayerActionButton>,
    onSave: (List<PlayerActionButton>) -> Unit,
    onDismiss: () -> Unit
) {
    val activeButtons = remember { mutableStateListOf(*initialButtons.toTypedArray()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Customize Player Buttons",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                onSave(activeButtons.toList())
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                activeButtons.clear()
                                activeButtons.addAll(PlayerActionButton.DEFAULT_BUTTONS)
                                selectedIndex = -1
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                        Button(
                            onClick = {
                                onSave(activeButtons.toList())
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Live Player Skeleton Preview
                Text(
                    text = "LIVE SKELETON PREVIEW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                PlayerSkeletonCard(
                    activeButtons = activeButtons,
                    selectedIndex = selectedIndex,
                    onSelectButton = { idx ->
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Active Buttons Reordering Controls
                Text(
                    text = "ACTIVE BUTTONS (TAP TO REORDER / REMOVE)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        activeButtons.forEachIndexed { index, button ->
                            val isSelected = index == selectedIndex
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        selectedIndex = if (isSelected) -1 else index
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(getPlayerButtonIcon(button)),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = button.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                // Move Up / Left
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = activeButtons.removeAt(index)
                                            activeButtons.add(index - 1, item)
                                            if (selectedIndex == index) selectedIndex = index - 1
                                            else if (selectedIndex == index - 1) selectedIndex = index
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "Move Left",
                                        tint = if (index > 0) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }

                                // Move Down / Right
                                IconButton(
                                    onClick = {
                                        if (index < activeButtons.lastIndex) {
                                            val item = activeButtons.removeAt(index)
                                            activeButtons.add(index + 1, item)
                                            if (selectedIndex == index) selectedIndex = index + 1
                                            else if (selectedIndex == index + 1) selectedIndex = index
                                        }
                                    },
                                    enabled = index < activeButtons.lastIndex,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Move Right",
                                        tint = if (index < activeButtons.lastIndex) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = {
                                        if (activeButtons.size > 1) {
                                            activeButtons.removeAt(index)
                                            if (selectedIndex == index) selectedIndex = -1
                                            else if (selectedIndex > index) selectedIndex--
                                        }
                                    },
                                    enabled = activeButtons.size > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        tint = if (activeButtons.size > 1) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Available to Add
                val availableToAdd = PlayerActionButton.entries.filter { it !in activeButtons }
                if (availableToAdd.isNotEmpty()) {
                    Text(
                        text = "ADD MORE BUTTONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableToAdd.forEach { button ->
                            AssistChip(
                                onClick = {
                                    activeButtons.add(button)
                                },
                                label = { Text(button.displayName) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(getPlayerButtonIcon(button)),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PlayerSkeletonCard(
    activeButtons: List<PlayerActionButton>,
    selectedIndex: Int,
    onSelectButton: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E242B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C3E50),
                            Color(0xFF1A252F),
                            Color(0xFF12171D)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar Header
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "civil",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Album Art Thumbnail Placeholder
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF3B4D61)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title, Artist, and Top Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "reflex",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Dzsúdló, Only U",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Download & Like Mock Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.favorite_border),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume / Seek Slider Mock
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0:11", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(text = "2:43", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                // Pause Pill Button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pause),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pause",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // REAL-TIME BOTTOM ACTION BAR SKELETON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeButtons.forEachIndexed { index, button ->
                    val isSelected = index == selectedIndex
                    val buttonShape = when {
                        activeButtons.size == 1 -> RoundedCornerShape(12.dp)
                        index == 0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                        index == activeButtons.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, bottomStart = 4.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(buttonShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.12f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = buttonShape
                            )
                            .clickable { onSelectButton(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(getPlayerButtonIcon(button)),
                            contentDescription = button.displayName,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
