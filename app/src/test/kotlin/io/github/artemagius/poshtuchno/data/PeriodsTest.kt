package io.github.artemagius.poshtuchno.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PeriodsTest {

    private val moscow = ZoneId.of("Europe/Moscow")

    @Test
    fun `month range covers whole month`() {
        val range = Periods.monthRange(LocalDate.of(2026, 9, 15), moscow)
        val expectedFrom = LocalDate.of(2026, 9, 1).atStartOfDay(moscow).toInstant().toEpochMilli()
        val expectedToExclusive = LocalDate.of(2026, 10, 1).atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(expectedFrom, range.first)
        // LongRange включает last, поэтому граница на 1 мс меньше исключающей.
        assertEquals(expectedToExclusive - 1, range.last)
    }

    @Test
    fun `month range handles december rollover`() {
        val range = Periods.monthRange(LocalDate.of(2026, 12, 31), moscow)
        val expectedToExclusive = LocalDate.of(2027, 1, 1).atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(expectedToExclusive - 1, range.last)
    }

    @Test
    fun `month range covers february in leap year`() {
        val range = Periods.monthRange(LocalDate.of(2028, 2, 10), moscow)
        val from = LocalDate.of(2028, 2, 1).atStartOfDay(moscow).toInstant().toEpochMilli()
        val toExclusive = LocalDate.of(2028, 3, 1).atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(from, range.first)
        assertEquals(29L * 24 * 60 * 60 * 1000, toExclusive - from)
    }

    @Test
    fun `week range starts on monday`() {
        // 2026-09-04 — пятница.
        val range = Periods.weekRange(LocalDate.of(2026, 9, 4), moscow)
        val monday = LocalDate.of(2026, 8, 31).atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(monday, range.first)
        assertEquals(7L * 24 * 60 * 60 * 1000 - 1, range.last - range.first)
    }

    @Test
    fun `week range on sunday keeps current week`() {
        // 2026-09-06 — воскресенье, неделя должна начинаться 31 августа.
        val range = Periods.weekRange(LocalDate.of(2026, 9, 6), moscow)
        val monday = LocalDate.of(2026, 8, 31).atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(monday, range.first)
    }

    @Test
    fun `day range is 24 hours`() {
        val range = Periods.dayRange(LocalDate.of(2026, 9, 4), moscow)
        assertEquals(24L * 60 * 60 * 1000 - 1, range.last - range.first)
    }

    @Test
    fun `boundary timestamp belongs to exactly one month`() {
        val september = Periods.monthRange(LocalDate.of(2026, 9, 1), moscow)
        val october = Periods.monthRange(LocalDate.of(2026, 10, 1), moscow)
        val firstOctoberMillis = LocalDate.of(2026, 10, 1).atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(false, firstOctoberMillis in september)
        assertEquals(true, firstOctoberMillis in october)
    }

    @Test
    fun `roundtrip through localDate keeps the date`() {
        val date = LocalDate.of(2026, 9, 4)
        val millis = date.atStartOfDay(moscow).toInstant().toEpochMilli()
        assertEquals(date, Periods.toLocalDate(millis, moscow))
    }
}
