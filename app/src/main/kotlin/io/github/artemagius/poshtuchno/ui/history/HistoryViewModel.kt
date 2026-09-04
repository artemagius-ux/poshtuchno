package io.github.artemagius.poshtuchno.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayGroup(
    val date: LocalDate,
    val totalKopecks: Long,
    val purchases: List<PurchaseListItem>,
)

data class HistoryUiState(
    val monthOffset: Int = 0,
    val monthTitle: String = "",
    val monthTotalKopecks: Long = 0,
    val purchaseCount: Int = 0,
    val groups: List<DayGroup> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val query: String = "",
    val loaded: Boolean = false,
)

/**
 * История с перелистыванием месяцев.
 *
 * Группировка по дням делается в памяти, а не в SQL: список за месяц заведомо
 * небольшой, а перевод миллисекунд в календарную дату уже есть в [Periods].
 */
class HistoryViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private data class Inputs(val offset: Int, val query: String, val earliest: Long?)

    private val monthOffset = MutableStateFlow(0)
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(
        monthOffset,
        query,
        repository.observeEarliestPurchase(),
    ) { offset, text, earliest -> Inputs(offset, text, earliest) }
        .flatMapLatest { inputs ->
            repository.observePurchases(Periods.monthRangeAt(inputs.offset))
                .map { purchases -> buildState(inputs, purchases) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private fun buildState(inputs: Inputs, purchases: List<PurchaseListItem>): HistoryUiState {
        val filtered = if (inputs.query.isBlank()) {
            purchases
        } else {
            val needle = inputs.query.trim().lowercase()
            purchases.filter { purchase ->
                listOfNotNull(purchase.note, purchase.shopName, purchase.topCategoryName)
                    .any { it.lowercase().contains(needle) }
            }
        }

        val groups = filtered
            .groupBy { Periods.toLocalDate(it.purchasedAt) }
            .entries
            .sortedByDescending { it.key }
            .map { (date, items) ->
                DayGroup(
                    date = date,
                    totalKopecks = items.sumOf { it.totalKopecks },
                    purchases = items,
                )
            }

        val earliestMonth = inputs.earliest?.let { Periods.toLocalDate(it).withDayOfMonth(1) }
        val shownMonth = LocalDate.now().withDayOfMonth(1).plusMonths(inputs.offset.toLong())

        return HistoryUiState(
            monthOffset = inputs.offset,
            monthTitle = Periods.monthTitle(inputs.offset),
            monthTotalKopecks = filtered.sumOf { it.totalKopecks },
            purchaseCount = filtered.size,
            groups = groups,
            canGoBack = earliestMonth != null && shownMonth.isAfter(earliestMonth),
            canGoForward = inputs.offset < 0,
            query = inputs.query,
            loaded = true,
        )
    }

    private val _message = MutableStateFlow<HistoryMessage?>(null)
    val message: StateFlow<HistoryMessage?> = _message.asStateFlow()

    private var lastDeleted: DeletedPurchase? = null

    fun previousMonth() {
        monthOffset.value -= 1
    }

    fun nextMonth() {
        if (monthOffset.value < 0) monthOffset.value += 1
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun deletePurchase(id: Long) {
        viewModelScope.launch {
            lastDeleted = repository.deletePurchase(id)
            if (lastDeleted != null) _message.value = HistoryMessage.Deleted
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

    fun consumeMessage() {
        _message.value = null
    }
}

enum class HistoryMessage { Deleted }
