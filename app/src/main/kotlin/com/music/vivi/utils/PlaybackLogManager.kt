/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Log levels for playback diagnostics
 */
enum class PlaybackLogLevel {
    INFO,
    WARNING,
    ERROR,
    DEBUG,
    BOT // Special level for highlighting bot mitigation events
}

/**
 * Log entry for the playback diagnostics
 */
data class PlaybackLogEntry(
    val timestamp: String,
    val level: PlaybackLogLevel,
    val message: String,
    val details: String? = null
)

/**
 * Singleton manager to hold the global state of playback logs.
 * This is used for real-time debugging of stream resolution and bot detection.
 */
object PlaybackLogManager {
    private const val MAX_LOG_ENTRIES = 500
    
    private val _logs = MutableStateFlow<List<PlaybackLogEntry>>(emptyList())
    val logs: StateFlow<List<PlaybackLogEntry>> = _logs.asStateFlow()
    
    /**
     * Add a new log entry
     */
    fun log(level: PlaybackLogLevel, message: String, details: String? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val entry = PlaybackLogEntry(timestamp, level, message, details)
        
        // Use a list to ensure thread-safety during update
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(entry)
        
        // Take last N entries
        _logs.value = if (currentLogs.size > MAX_LOG_ENTRIES) {
            currentLogs.takeLast(MAX_LOG_ENTRIES)
        } else {
            currentLogs
        }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
}
