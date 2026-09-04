package io.github.artemagius.poshtuchno.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.DeletedPurchase
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.quickadd.AmountInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val monthTotalKopecks: Long = 0,
    val monthLimitKopecks: Long? = null,
    val monthName: String = "",
    val recent: List<PurchaseListItem> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
)

data class QuickAddState(
    val visible: Boolean = false,
    val amount: AmountInput = AmountInput(),
    val selectedCategoryId: Long? = null,
    val note: String = "",
)

class HomeViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val monthRange = Periods.monthRange()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTotal(monthRange),
        repository.observeMonthlyLimit(),
        repository.observeRecent(),
        repository.observeCategories(),
    ) { total, limit, recent, categories ->
        HomeUiState(
            monthTotalKopecks = total,
            monthLimitKopecks = limit?.limitKopecks,
            monthName = Periods.monthName(LocalDate.now()),
            recent = recent,
            categories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(monthName = Periods.monthName(LocalDate.now())),
    )

    /**
     * Категории для чипов: сначала те, которыми пользовались за последний месяц,
     * остальные дополняют список до шести.
     */
    val quickCategories: StateFlow<List<CategoryEntity>> = combine(
        repository.observeFrequentCategories(since = monthRange.first),
        repository.observeCategories(),
    ) { frequent, all ->
        val result = LinkedHashMap<Long, CategoryEntity>()
        frequent.forEach { result[it.id] = it }
        all.forEach { if (result.size < QUICK_CATEGORY_COUNT) result.putIfAbsent(it.id, it) }
        result.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _quickAdd = MutableStateFlow(QuickAddState())
    val quickAdd: StateFlow<QuickAddState> = _quickAdd.asStateFlow()

    private val _messages = MutableStateFlow<HomeMessage?>(null)
    val messages: StateFlow<HomeMessage?> = _messages.asStateFlow()

    private var lastDeleted: DeletedPurchase? = null

    fun openQuickAdd() {
        _quickAdd.value = QuickAddState(
            visible = true,
            selectedCategoryId = quickCategories.value.firstOrNull()?.id,
        )
    }

    fun closeQuickAdd() {
        _quickAdd.update { it.copy(visible = false) }
    }

    fun onDigit(digit: Char) = _quickAdd.update { it.copy(amount = it.amount.appendDigit(digit)) }

    fun onSeparator() = _quickAdd.update { it.copy(amount = it.amount.appendSeparator()) }

    fun onBackspace() = _quickAdd.update { it.copy(amount = it.amount.backspace()) }

    fun onCategorySelect(id: Long) = _quickAdd.update { it.copy(selectedCategoryId = id) }

    fun onNoteChange(note: String) = _quickAdd.update { it.copy(note = note) }

    fun saveQuickAdd() {
        val state = _quickAdd.value
        val kopecks = state.amount.kopecks
        if (kopecks <= 0) return
        viewModelScope.launch {
            repository.addQuickExpense(
                totalKopecks = kopecks,
                categoryId = state.selectedCategoryId,
                note = state.note,
            )
            _quickAdd.value = QuickAddState(visible = false)
        }
    }

    fun deletePurchase(id: Long) {
        viewModelScope.launch {
            lastDeleted = repository.deletePurchase(id)
            if (lastDeleted != null) _messages.value = HomeMessage.PurchaseDeleted
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted ?: return
        viewModelScope.launch {
            repository.restorePurchase(deleted)
            lastDeleted = null
            _messages.value = null
        }
    }

    fun setMonthlyLimit(kopecks: Long?) {
        viewModelScope.launch { repository.setMonthlyLimit(kopecks) }
    }

    fun consumeMessage() {
        _messages.value = null
    }

    companion object {
        private const val QUICK_CATEGORY_COUNT = 6

        fun factory(repository: ExpenseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(repository) as T
            }
    }
}

enum class HomeMessage { PurchaseDeleted }
