package io.github.artemagius.poshtuchno.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.Analytics
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.CategoryBreakdown
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class AnalyticsPeriod(val label: String) {
    Week("Неделя"),
    Month("Месяц"),
    Year("Год"),
}

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.Month,
    val title: String = "",
    val totalKopecks: Long = 0,
    val previousTotalKopecks: Long = 0,
    val changePercent: Int? = null,
    val purchaseCount: Int = 0,
    val activeDayCount: Int = 0,
    val averagePerActiveDayKopecks: Long = 0,
    val averagePurchaseKopecks: Long = 0,
    val breakdown: List<CategoryBreakdown> = emptyList(),
    val shares: List<Int> = emptyList(),
    val days: List<DayAmount> = emptyList(),
    val topPurchases: List<PurchaseListItem> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Вкладка «Аналитика»: разрез по категориям, столбики по дням,
 * сравнение с предыдущим таким же периодом и самые крупные траты.
 */
class AnalyticsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val period = MutableStateFlow(AnalyticsPeriod.Month)

    private data class Numbers(
        val total: Long,
        val previous: Long,
        val purchases: Int,
        val activeDays: Int,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = period.flatMapLatest { selected ->
        val today = LocalDate.now()
        val tz = Periods.sqliteOffset()
        val range = rangeFor(selected, today)
        val previousRange = previousRangeFor(selected, today)
        val (chartFrom, chartTo) = chartBoundsFor(selected, today)

        val numbers = combine(
            repository.observeTotal(range),
            repository.observeTotal(previousRange),
            repository.observePurchaseCount(range),
            repository.observeActiveDayCount(range, tz),
        ) { total, previous, count, activeDays ->
            Numbers(total, previous, count, activeDays)
        }

        combine(
            numbers,
            repository.observeCategoryBreakdown(range),
            repository.observeDailyTotals(range, tz),
            repository.observeTopPurchases(range, limit = 5),
        ) { n, breakdown, daily, top ->
            AnalyticsUiState(
                period = selected,
                title = titleFor(selected, today),
                totalKopecks = n.total,
                previousTotalKopecks = n.previous,
                changePercent = Analytics.percentChange(n.total, n.previous),
                purchaseCount = n.purchases,
                activeDayCount = n.activeDays,
                averagePerActiveDayKopecks = Analytics.averagePerDay(n.total, n.activeDays),
                averagePurchaseKopecks = if (n.purchases > 0) n.total / n.purchases else 0,
                breakdown = breakdown,
                shares = Analytics.shares(breakdown.map { it.totalKopecks }),
                days = Analytics.fillMissingDays(daily, chartFrom, chartTo),
                topPurchases = top,
                loaded = true,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun selectPeriod(value: AnalyticsPeriod) {
        period.value = value
    }

    private fun rangeFor(period: AnalyticsPeriod, today: LocalDate): LongRange = when (period) {
        AnalyticsPeriod.Week -> Periods.weekRange(today)
        AnalyticsPeriod.Month -> Periods.monthRange(today)
        AnalyticsPeriod.Year -> Periods.yearRange(today)
    }

    private fun previousRangeFor(period: AnalyticsPeriod, today: LocalDate): LongRange = when (period) {
        AnalyticsPeriod.Week -> Periods.weekRange(today.minusWeeks(1))
        AnalyticsPeriod.Month -> Periods.monthRange(today.minusMonths(1))
        AnalyticsPeriod.Year -> Periods.yearRange(today.minusYears(1))
    }

    /**
     * Границы столбчатого графика. Для года по дням рисовать бессмысленно,
     * поэтому там показываем последние 30 дней.
     */
    private fun chartBoundsFor(period: AnalyticsPeriod, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            AnalyticsPeriod.Week -> today.with(java.time.DayOfWeek.MONDAY) to today
            AnalyticsPeriod.Month -> today.withDayOfMonth(1) to today
            AnalyticsPeriod.Year -> today.minusDays(29) to today
        }

    private fun titleFor(period: AnalyticsPeriod, today: LocalDate): String = when (period) {
        AnalyticsPeriod.Week -> "Эта неделя"
        AnalyticsPeriod.Month -> Periods.monthName(today)
        AnalyticsPeriod.Year -> today.year.toString()
    }
}
