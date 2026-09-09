package com.music.vivi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.vivi.R
import com.music.vivi.ui.components.ExpressiveBarChart
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.viewmodels.DayUsageData
import com.music.vivi.viewmodels.ListeningSummaryViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningSummaryScreen(
    navController: NavController,
    viewModel: ListeningSummaryViewModel = hiltViewModel()
) {
    val weekTotalMs by viewModel.weekTotalMs.collectAsState()
    val weekDailyData by viewModel.weekDailyData.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()

    var showDailyBreakdown by remember { mutableStateOf(true) }

    fun formatMs(ms: Long): String {
        val totalMinutes = ms / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    val weekLabel = if (weekOffset == 0) "This week" else "${weekOffset}w ago"

    var showDatePicker by remember { mutableStateOf(false) }

    // Hoist the state outside the conditional so switching to "year picker" mode doesn't destroy the state
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        LaunchedEffect(Unit) {
            val initial = LocalDate.now().minusWeeks(weekOffset.toLong())
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            datePickerState.selectedDateMillis = initial
        }
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setWeekFromEpoch(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Listening Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Usage Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Total listened",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatMs(weekTotalMs),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = { showDatePicker = true }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calendar_month),
                                    contentDescription = "Select Date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        ExpressiveBarChart(
                            days = weekDailyData.ifEmpty {
                                listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
                                    .map { DayUsageData(it, 0L, 0L, 0L, 0L, 0L) }
                            },
                            todayIndex = if (viewModel.weekOffset.value == 0) java.time.LocalDate.now().dayOfWeek.value - 1 else -1,
                            onDayClick = { timestamp ->
                                if (timestamp > 0L) {
                                    navController.navigate("detailed_listening_history/$timestamp")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Week navigation pill
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { viewModel.goToPreviousWeek() }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_chevron_left),
                                        contentDescription = "Previous week",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = weekLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { viewModel.goToNextWeek() }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_chevron_right),
                                        contentDescription = "Next week",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (viewModel.isCurrentWeek())
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Show daily breakdown toggle
            item {
                val containerColor by animateColorAsState(
                    targetValue = if (showDailyBreakdown) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    label = "containerColor"
                )
                val contentColor = if (showDailyBreakdown)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Card(
                    onClick = { showDailyBreakdown = !showDailyBreakdown },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Listening breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                        ModernSwitch(
                            checked = showDailyBreakdown,
                            onCheckedChange = { showDailyBreakdown = it }
                        )
                    }
                }
            }

            // Days Usage Breakdown - always visible (not gated by toggle here)
            item {
                val days = if (weekDailyData.isNotEmpty()) {
                    weekDailyData
                } else {
                    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                        .map { name -> DayUsageData(name, 0L, 0L, 0L, 0L, 0L) }
                }

                val expandedStates = remember(days.size) { days.map { mutableStateOf(false) } }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    days.forEachIndexed { index, dayData ->
                        val isFirst = index == 0
                        val isLast = index == days.size - 1
                        val isExpanded = expandedStates[index].value
                        val prevExpanded = if (index > 0) expandedStates[index - 1].value else false

                        val topRadius by animateDpAsState(
                            targetValue = if (isFirst || prevExpanded) 16.dp else 2.dp,
                            label = "topRadius_$index"
                        )
                        val bottomRadius by animateDpAsState(
                            targetValue = if (isExpanded || isLast) 16.dp else 2.dp,
                            label = "bottomRadius_$index"
                        )
                        val cardShape = RoundedCornerShape(
                            topStart = topRadius, topEnd = topRadius,
                            bottomStart = bottomRadius, bottomEnd = bottomRadius
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShape,
                            onClick = { expandedStates[index].value = !expandedStates[index].value },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayData.dayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = formatMs(dayData.totalMs),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            painter = painterResource(
                                                id = if (isExpanded) R.drawable.expand_less else R.drawable.expand_more
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    val breakdownItems = listOf(
                                        "Songs" to formatMs(dayData.songsMs),
                                        "Artists" to formatMs(dayData.artistsMs),
                                        "Albums" to formatMs(dayData.albumsMs)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        breakdownItems.forEachIndexed { i, (label, value) ->
                                            val subShape = when (i) {
                                                0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                                                breakdownItems.size - 1 -> RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                                                else -> RoundedCornerShape(2.dp)
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = subShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                tonalElevation = 0.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = value,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}
