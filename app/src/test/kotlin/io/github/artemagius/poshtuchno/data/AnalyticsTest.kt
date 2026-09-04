package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.DailyTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AnalyticsTest {

    @Test
    fun `fillMissingDays adds zeros for days without purchases`() {
        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 5)
        val totals = listOf(
            DailyTotal("2026-09-02", 10_000, 1),
            DailyTotal("2026-09-05", 25_000, 2),
        )

        val result = Analytics.fillMissingDays(totals, from, to)

        assertEquals(5, result.size)
        assertEquals(listOf(0L, 10_000L, 0L, 0L, 25_000L), result.map { it.totalKopecks })
        assertEquals(from, result.first().date)
        assertEquals(to, result.last().date)
    }

    @Test
    fun `fillMissingDays keeps chronological order`() {
        val from = LocalDate.of(2026, 8, 30)
        val to = LocalDate.of(2026, 9, 2)
        val result = Analytics.fillMissingDays(emptyList(), from, to)
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 30),
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
            ),
            result.map { it.date },
        )
    }

    @Test
    fun `fillMissingDays handles single day`() {
        val day = LocalDate.of(2026, 9, 4)
        val result = Analytics.fillMissingDays(listOf(DailyTotal("2026-09-04", 500, 1)), day, day)
        assertEquals(1, result.size)
        assertEquals(500L, result.first().totalKopecks)
    }

    @Test
    fun `averagePerDay divides by day count`() {
        assertEquals(5_000L, Analytics.averagePerDay(50_000, 10))
    }

    @Test
    fun `averagePerDay returns zero for non-positive days`() {
        assertEquals(0L, Analytics.averagePerDay(50_000, 0))
        assertEquals(0L, Analytics.averagePerDay(50_000, -3))
    }

    @Test
    fun `projection extrapolates to full month`() {
        // 10 000 за 10 дней из 30 -> 30 000 за месяц.
        assertEquals(30_000L, Analytics.projectedMonthTotal(10_000, 10, 30))
    }

    @Test
    fun `projection on first day multiplies by month length`() {
        assertEquals(310_000L, Analytics.projectedMonthTotal(10_000, 1, 31))
    }

    @Test
    fun `projection on last day equals current total`() {
        assertEquals(90_000L, Analytics.projectedMonthTotal(90_000, 30, 30))
    }

    @Test
    fun `percentChange computes growth`() {
        assertEquals(50, Analytics.percentChange(150, 100))
    }

    @Test
    fun `percentChange computes decline`() {
        assertEquals(-25, Analytics.percentChange(75, 100))
    }

    @Test
    fun `percentChange is null without baseline`() {
        assertNull(Analytics.percentChange(100, 0))
        assertNull(Analytics.percentChange(100, -5))
    }

    @Test
    fun `shares sum to hundred`() {
        val shares = Analytics.shares(listOf(1L, 1L, 1L))
        assertEquals(100, shares.sum())
    }

    @Test
    fun `shares distribute remainder to largest fractions`() {
        // 1/3 каждому: 33 + 33 + 34 = 100.
        val shares = Analytics.shares(listOf(10L, 10L, 10L))
        assertEquals(100, shares.sum())
        assertEquals(3, shares.size)
    }

    @Test
    fun `shares are proportional`() {
        assertEquals(listOf(50, 30, 20), Analytics.shares(listOf(500L, 300L, 200L)))
    }

    @Test
    fun `shares of empty total are zeros`() {
        assertEquals(listOf(0, 0), Analytics.shares(listOf(0L, 0L)))
    }

    @Test
    fun `shares of single item is hundred`() {
        assertEquals(listOf(100), Analytics.shares(listOf(42L)))
    }
}
