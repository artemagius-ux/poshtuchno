package io.github.artemagius.poshtuchno.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Границы периодов в местной таймзоне.
 *
 * Все запросы к базе используют полуинтервал [from, to): это избавляет от
 * пограничных ошибок с последней миллисекундой суток.
 */
object Periods {

    fun monthRange(date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val month = YearMonth.from(date)
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from until to
    }

    fun weekRange(date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val monday = date.with(DayOfWeek.MONDAY)
        val from = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = monday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from until to
    }

    fun dayRange(date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from until to
    }

    fun yearRange(date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val from = date.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.withDayOfYear(1).plusYears(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from until to
    }

    fun toLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun toLocalDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()

    /** Название месяца в именительном падеже: «Сентябрь». */
    fun monthName(date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String =
        date.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { it.titlecase(locale) }
}
