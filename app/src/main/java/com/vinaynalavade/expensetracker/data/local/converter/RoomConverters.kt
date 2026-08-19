package com.vinaynalavade.expensetracker.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room Type Converters for modern date-time objects.
 */
class RoomConverters {

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun toEpochMilli(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
