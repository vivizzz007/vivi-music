/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class DayUsageData(
    val dayName: String,
    val timestamp: Long,
    val totalMs: Long,
    val songsMs: Long,
    val artistsMs: Long,
    val albumsMs: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListeningSummaryViewModel @Inject constructor(
    val database: MusicDatabase
) : ViewModel() {

    val weekOffset = MutableStateFlow(0)

    private fun weekStartMs(offset: Int): Long {
        val monday = LocalDate.now()
            .minusWeeks(offset.toLong())
            .with(DayOfWeek.MONDAY)
        return monday.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    private val dayMs = 24L * 60 * 60 * 1000

    /** Today's total play time in ms */
    val todayPlayTimeMs: Flow<Long> = run {
        val todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val todayEnd = todayStart + dayMs
        database.getPlayTimeForDay(todayStart, todayEnd)
    }

    /** Per-day breakdown for the selected week (Mon–Sun), updates when weekOffset changes */
    val weekDailyData = weekOffset.flatMapLatest { offset ->
        val weekStart = weekStartMs(offset)
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        val dayFlows: List<Flow<DayUsageData>> = (0 until 7).map { dayIndex ->
            val from = weekStart + dayIndex * dayMs
            val to = from + dayMs
            val name = dayNames[dayIndex]
            combine(
                database.getPlayTimeForDay(from, to),
                database.getSongsPlayTimeForDay(from, to),
                database.getArtistPlayTimeForDay(from, to),
                database.getAlbumPlayTimeForDay(from, to)
            ) { total: Long, songs: Long, artists: Long, albums: Long ->
                DayUsageData(name, from, total, songs, artists, albums)
            }
        }

        combine(dayFlows[0], dayFlows[1], dayFlows[2], dayFlows[3], dayFlows[4], dayFlows[5], dayFlows[6]) {
                arr -> arr.toList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList<DayUsageData>())

    /** Total play time for the selected week */
    val weekTotalMs = weekDailyData
        .map { days -> days.sumOf { it.totalMs } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    fun goToPreviousWeek() { weekOffset.value++ }
    fun goToNextWeek() { if (weekOffset.value > 0) weekOffset.value-- }
    fun isCurrentWeek(): Boolean = weekOffset.value == 0
    fun setWeekFromEpoch(epochMilli: Long) {
        val selectedDate = Instant.ofEpochMilli(epochMilli).atZone(ZoneOffset.UTC).toLocalDate()
        val currentMonday = LocalDate.now().with(DayOfWeek.MONDAY)
        val selectedMonday = selectedDate.with(DayOfWeek.MONDAY)
        val weeksDiff = ChronoUnit.WEEKS.between(selectedMonday, currentMonday).toInt()
        weekOffset.value = weeksDiff.coerceAtLeast(0)
    }
}
