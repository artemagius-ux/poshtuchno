package io.github.artemagius.poshtuchno.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickAddState(
    val visible: Boolean = false,
    val amount: AmountInput = AmountInput(),
    val selectedCategoryId: Long? = null,
    val note: String = "",
)

/**
 * Быстрый ввод живёт отдельно от вкладок: лист вызывается с любой из них,
 * и его состояние не должно сбрасываться при переключении вкладки.
 */
class QuickAddViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = combine(
        repository.observeFrequentCategories(since = Periods.monthRange().first),
        repository.observeCategories(),
    ) { frequent, all ->
        val result = LinkedHashMap<Long, CategoryEntity>()
        frequent.forEach { result[it.id] = it }
        all.forEach { if (result.size < CATEGORY_LIMIT) result.putIfAbsent(it.id, it) }
        result.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(QuickAddState())
    val state: StateFlow<QuickAddState> = _state.asStateFlow()

    private val _saved = MutableStateFlow<Long?>(null)
    val saved: StateFlow<Long?> = _saved.asStateFlow()

    fun open() {
        _state.value = QuickAddState(
            visible = true,
            selectedCategoryId = categories.value.firstOrNull()?.id,
        )
    }

    fun close() = _state.update { it.copy(visible = false) }

    fun onDigit(digit: Char) = _state.update { it.copy(amount = it.amount.appendDigit(digit)) }

    fun onSeparator() = _state.update { it.copy(amount = it.amount.appendSeparator()) }

    fun onBackspace() = _state.update { it.copy(amount = it.amount.backspace()) }

    fun onCategorySelect(id: Long) = _state.update { it.copy(selectedCategoryId = id) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val current = _state.value
        val kopecks = current.amount.kopecks
        if (kopecks <= 0) return
        viewModelScope.launch {
            val id = repository.addQuickExpense(
                totalKopecks = kopecks,
                categoryId = current.selectedCategoryId,
                note = current.note,
            )
            _state.value = QuickAddState(visible = false)
            _saved.value = id
        }
    }

    fun consumeSaved() {
        _saved.value = null
    }

    private companion object {
        const val CATEGORY_LIMIT = 8
    }
}
