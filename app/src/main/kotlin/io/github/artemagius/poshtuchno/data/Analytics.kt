package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.DailyTotal
import java.time.LocalDate

/**
 * Расчёты для вкладок. Вынесены в чистые функции без Android-зависимостей,
 * чтобы покрыть их обычными юнит-тестами.
 */
object Analytics {

    /**
     * Раскладывает суммы по дням периода: дни без трат получают ноль.
     * Нужно для графика — иначе столбики «съезжаются» и врут по датам.
     */
    fun fillMissingDays(
        totals: List<DailyTotal>,
        from: LocalDate,
        to: LocalDate,
    ): List<DayAmount> {
        require(!to.isBefore(from)) { "to must not be before from" }
        val byDay = totals.associate { it.day to it.totalKopecks }
        val result = ArrayList<DayAmount>()
        var day = from
        while (!day.isAfter(to)) {
            result += DayAmount(day, byDay[day.toString()] ?: 0L)
            day = day.plusDays(1)
        }
        return result
    }

    /** Сколько в среднем в день, считая все дни периода, а не только активные. */
    fun averagePerDay(totalKopecks: Long, dayCount: Int): Long =
        if (dayCount <= 0) 0 else totalKopecks / dayCount

    /**
     * Прогноз траты за месяц при текущем темпе.
     * Считаем по прошедшим дням включительно: в первый день месяца прогноз
     * равен трате этого дня, умноженной на длину месяца.
     */
    fun projectedMonthTotal(totalKopecks: Long, dayOfMonth: Int, daysInMonth: Int): Long {
        if (dayOfMonth <= 0 || daysInMonth <= 0) return 0
        return totalKopecks * daysInMonth / dayOfMonth
    }

    /** Изменение в процентах относительно предыдущего периода. */
    fun percentChange(current: Long, previous: Long): Int? {
        if (previous <= 0) return null
        val delta = (current - previous).toDouble() / previous
        return Math.round(delta * 100).toInt()
    }

    /** Доли категорий в процентах, сумма всегда 100 при непустом вводе. */
    fun shares(amounts: List<Long>): List<Int> {
        val total = amounts.sum()
        if (total <= 0) return amounts.map { 0 }
        val raw = amounts.map { it * 100.0 / total }
        val floored = raw.map { it.toInt() }.toMutableList()
        var remainder = 100 - floored.sum()
        // Остаток распределяем по наибольшим дробным частям — так сумма ровно 100.
        val order = raw.indices.sortedByDescending { raw[it] - floored[it] }
        var i = 0
        while (remainder > 0 && order.isNotEmpty()) {
            floored[order[i % order.size]] += 1
            remainder -= 1
            i += 1
        }
        return floored
    }
}

data class DayAmount(
    val date: LocalDate,
    val totalKopecks: Long,
)
