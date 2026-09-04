package io.github.artemagius.poshtuchno.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.GroupTotal
import io.github.artemagius.poshtuchno.data.db.ProductTotal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductsPeriod(val label: String) {
    Month("Месяц"),
    Year("Год"),
    All("Всё время"),
}

data class ProductsUiState(
    val period: ProductsPeriod = ProductsPeriod.Month,
    val groups: List<GroupTotal> = emptyList(),
    val expandedGroupId: Long? = null,
    val expandedProducts: List<ProductTotal> = emptyList(),
    val query: String = "",
    val loaded: Boolean = false,
)

/**
 * Вкладка «Товары»: автоматические подкатегории с количествами.
 *
 * Здесь виден ответ на вопрос «сколько литров энергетиков и каких брендов»:
 * группа даёт суммарный объём, а раскрытие — разбивку по конкретным товарам.
 */
class ProductsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val period = MutableStateFlow(ProductsPeriod.Month)
    private val expanded = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")

    private fun rangeFor(value: ProductsPeriod): LongRange = when (value) {
        ProductsPeriod.Month -> Periods.monthRange()
        ProductsPeriod.Year -> Periods.yearRange()
        // «Всё время» — от эпохи до конца текущего года, покрывает любую историю.
        ProductsPeriod.All -> 0L..Periods.yearRange().last
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProductsUiState> = combine(period, expanded, query) { p, e, q ->
        Triple(p, e, q)
    }.flatMapLatest { (selectedPeriod, expandedId, text) ->
        val range = rangeFor(selectedPeriod)
        val productsFlow = if (expandedId != null) {
            repository.observeGroupProducts(expandedId, range)
        } else {
            flowOf(emptyList())
        }

        combine(repository.observeGroupTotals(range), productsFlow) { groups, products ->
            val needle = text.trim().lowercase()
            ProductsUiState(
                period = selectedPeriod,
                groups = if (needle.isEmpty()) {
                    groups
                } else {
                    groups.filter { it.title.lowercase().contains(needle) }
                },
                expandedGroupId = expandedId,
                expandedProducts = products,
                query = text,
                loaded = true,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsUiState())

    fun selectPeriod(value: ProductsPeriod) {
        period.value = value
    }

    fun toggleGroup(groupId: Long) {
        expanded.value = if (expanded.value == groupId) null else groupId
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun renameGroup(id: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.renameGroup(id, title.trim()) }
    }

    fun hideGroup(id: Long) {
        viewModelScope.launch { repository.setGroupHidden(id, true) }
    }

    /** Пересобрать группы вручную — если пользователь правил названия товаров. */
    fun refresh() {
        viewModelScope.launch { repository.refreshProductGroups() }
    }
}
