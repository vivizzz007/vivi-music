package com.music.vivi.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Type converters for Room Database.
 * Handles conversion between complex types (like LocalDateTime) and database primitives (Long).
 */
class Converters {
    /**
     * Converts a millisecond timestamp to a [LocalDateTime].
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? = if (value != null) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
    } else {
        null
    }

    /**
     * Converts a [LocalDateTime] to a millisecond timestamp.
     */
    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? = date?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
}
