package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.pages.HistoryPage
import com.music.vivi.constants.HistorySource
import com.music.vivi.db.MusicDatabase
import com.music.vivi.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 * Groups playback history by Date (Today, Yesterday, This Week, etc.).
 * Supports determining history source (Local Database vs Remote YouTube History).
 */
@HiltViewModel
public class HistoryViewModel
@Inject
constructor(public val database: MusicDatabase) : ViewModel() {
    public var historySource: MutableStateFlow<HistorySource> = MutableStateFlow(HistorySource.LOCAL)

    private val today = LocalDate.now()
    private val thisMonday = today.with(DayOfWeek.MONDAY)
    private val lastMonday = thisMonday.minusDays(7)

    public val historyPage: MutableStateFlow<HistoryPage?> = MutableStateFlow(null)

    public val events: StateFlow<Map<DateAgo, List<com.music.vivi.db.entities.EventWithSong>>> =
        database
            .events()
            .map { events ->
                events
                    .groupBy {
                        val date = it.event.timestamp.toLocalDate()
                        val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
                        when {
                            daysAgo == 0 -> DateAgo.Today
                            daysAgo == 1 -> DateAgo.Yesterday
                            date >= thisMonday -> DateAgo.ThisWeek
                            date >= lastMonday -> DateAgo.LastWeek
                            else -> DateAgo.Other(date.withDayOfMonth(1))
                        }
                    }.toSortedMap(
                        compareBy { dateAgo ->
                            when (dateAgo) {
                                DateAgo.Today -> 0L
                                DateAgo.Yesterday -> 1L
                                DateAgo.ThisWeek -> 2L
                                DateAgo.LastWeek -> 3L
                                is DateAgo.Other -> ChronoUnit.DAYS.between(dateAgo.date, today)
                            }
                        }
                    ).mapValues { entry ->
                        entry.value.distinctBy { it.song.id }
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        fetchRemoteHistory()
    }

    public fun fetchRemoteHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.musicHistory().onSuccess {
                historyPage.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}

public sealed class DateAgo {
    public data object Today : DateAgo()

    public data object Yesterday : DateAgo()

    public data object ThisWeek : DateAgo()

    public data object LastWeek : DateAgo()

    public class Other(public val date: LocalDate) : DateAgo() {
        override fun equals(other: Any?): Boolean {
            if (other is Other) return date == other.date
            return super.equals(other)
        }

        override fun hashCode(): Int = date.hashCode()
    }
}
