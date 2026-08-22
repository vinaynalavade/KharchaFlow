package com.vinaynalavade.expensetracker.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Modern Java 8+ / Kotlin Date and Time utilities for financial operations.
 */
object DateTimeUtils {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val SHORT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    fun currentEpochMillis(): Long = Instant.now().toEpochMilli()

    fun epochToInstant(epochMillis: Long): Instant = Instant.ofEpochMilli(epochMillis)

    fun epochToLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    fun epochToLocalDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val date = epochToLocalDate(epochMillis, zoneId)
        val today = LocalDate.now(zoneId)
        return when {
            date.isEqual(today) -> "Today"
            date.isEqual(today.minusDays(1)) -> "Yesterday"
            else -> date.format(DATE_FORMATTER)
        }
    }

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateTime = epochToLocalDateTime(epochMillis, zoneId)
        return dateTime.format(TIME_FORMATTER)
    }

    fun formatDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateTime = epochToLocalDateTime(epochMillis, zoneId)
        return dateTime.format(DATE_TIME_FORMATTER)
    }

    fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(MONTH_YEAR_FORMATTER)

    fun formatShortMonthYear(yearMonth: YearMonth): String = yearMonth.format(SHORT_MONTH_FORMATTER)

    fun getStartOfDayEpoch(localDate: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long =
        localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun getEndOfDayEpoch(localDate: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long =
        localDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant().toEpochMilli()

    fun getStartOfWeekEpoch(localDate: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val startOfWeek = localDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        return getStartOfDayEpoch(startOfWeek, zoneId)
    }

    fun getEndOfWeekEpoch(localDate: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val endOfWeek = localDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
        return getEndOfDayEpoch(endOfWeek, zoneId)
    }

    fun getStartOfMonthEpoch(yearMonth: YearMonth = YearMonth.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long =
        yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun getEndOfMonthEpoch(yearMonth: YearMonth = YearMonth.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long =
        yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant().toEpochMilli()
}
