package io.github.artemagius.poshtuchno.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.Analytics
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.DeletedPurchase
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val dayTotalKopecks: Long = 0,
    val weekTotalKopecks: Long = 0,
    val monthTotalKopecks: Long = 0,
    val monthLimitKopecks: Long? = null,
    val monthProjectionKopecks: Long = 0,
    val averagePerDayKopecks: Long = 0,
    val dayPurchases: List<PurchaseListItem> = emptyList(),
    val last14Days: List<DayAmount> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Вкладка «Сегодня».
 *
 * Диапазоны пересчитываются из [today], который обновляется при каждом возврате
 * на экран: иначе приложение, провисевшее в памяти через полночь, продолжало бы
 * показывать вчерашний день.
 */
class TodayViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    private data class Totals(
        val day: Long,
        val week: Long,
        val month: Long,
        val limit: Long?,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = today.flatMapLatest { date ->
        val tz = Periods.sqliteOffset()
        val chartFrom = date.minusDays(13)
        val chartRange = Periods.dayRange(chartFrom).first..Periods.dayRange(date).last

        val totals = combine(
            repository.observeTotal(Periods.dayRange(date)),
            repository.observeTotal(Periods.weekRange(date)),
            repository.observeTotal(Periods.monthRange(date)),
            repository.observeMonthlyLimit(),
        ) { day, week, month, budget ->
            Totals(day, week, month, budget?.limitKopecks)
        }

        combine(
            totals,
            repository.observePurchases(Periods.dayRange(date)),
            repository.observeDailyTotals(chartRange, tz),
        ) { t, purchases, daily ->
            TodayUiState(
                date = date,
                dayTotalKopecks = t.day,
                weekTotalKopecks = t.week,
                monthTotalKopecks = t.month,
                monthLimitKopecks = t.limit,
                monthProjectionKopecks = Analytics.projectedMonthTotal(
                    totalKopecks = t.month,
                    dayOfMonth = date.dayOfMonth,
                    daysInMonth = YearMonth.from(date).lengthOfMonth(),
                ),
                averagePerDayKopecks = Analytics.averagePerDay(t.month, date.dayOfMonth),
                dayPurchases = purchases,
                last14Days = Analytics.fillMissingDays(daily, chartFrom, date),
                loaded = true,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private val _message = MutableStateFlow<TodayMessage?>(null)
    val message: StateFlow<TodayMessage?> = _message.asStateFlow()

    private var lastDeleted: DeletedPurchase? = null

    /** Вызывается при возврате на экран: подхватывает смену суток. */
    fun refreshDate() {
        val now = LocalDate.now()
        if (today.value != now) today.value = now
    }

    fun deletePurchase(id: Long) {
        viewModelScope.launch {
            lastDeleted = repository.deletePurchase(id)
            if (lastDeleted != null) _message.value = TodayMessage.Deleted
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted ?: return
        viewModelScope.launch {
            repository.restorePurchase(deleted)
            lastDeleted = null
            _message.value = null
        }
    }

    fun setMonthlyLimit(kopecks: Long?) {
        viewModelScope.launch { repository.setMonthlyLimit(kopecks) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

enum class TodayMessage { Deleted }
