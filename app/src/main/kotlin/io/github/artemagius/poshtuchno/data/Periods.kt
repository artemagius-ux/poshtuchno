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

    /**
     * Сдвиг таймзоны в формате, который понимает SQLite: '+03:00' или '-05:00'.
     * Нужен, чтобы группировка по дням в SQL шла по местным суткам, а не по UTC.
     */
    fun sqliteOffset(zone: ZoneId = ZoneId.systemDefault(), at: Instant = Instant.now()): String {
        val seconds = zone.rules.getOffset(at).totalSeconds
        val sign = if (seconds < 0) "-" else "+"
        val abs = kotlin.math.abs(seconds)
        val hours = abs / 3600
        val minutes = (abs % 3600) / 60
        return "%s%02d:%02d".format(sign, hours, minutes)
    }

    /** Диапазон месяца по смещению от текущего: 0 — этот месяц, -1 — прошлый. */
    fun monthRangeAt(monthOffset: Int, zone: ZoneId = ZoneId.systemDefault()): LongRange =
        monthRange(LocalDate.now(zone).withDayOfMonth(1).plusMonths(monthOffset.toLong()), zone)

    fun monthTitle(monthOffset: Int, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        val date = LocalDate.now(zone).withDayOfMonth(1).plusMonths(monthOffset.toLong())
        val name = monthName(date, locale)
        return if (date.year == LocalDate.now(zone).year) name else "$name ${date.year}"
    }

    /** Название месяца в именительном падеже: «Сентябрь». */
    fun monthName(date: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): String =
        date.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { it.titlecase(locale) }
}
