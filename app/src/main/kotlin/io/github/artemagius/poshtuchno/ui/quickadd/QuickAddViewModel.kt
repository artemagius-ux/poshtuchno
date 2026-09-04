package io.github.artemagius.poshtuchno.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.data.db.ProductSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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

    /**
     * Подсказки по названию: то же, что при вводе по позициям.
     * Название в быстрой трате — это товар, поэтому подсказать прошлую цену
     * и категорию здесь так же полезно.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<ProductSuggestion>> = _state
        .map { it.note }
        .debounce(180)
        .mapLatest { text ->
            // Пустой запрос вернул бы всю историю — на пустом поле подсказки лишние.
            if (text.isBlank()) emptyList() else repository.productSuggestions(text, limit = 6)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Подставляет товар из истории: название, цену и категорию. */
    fun applySuggestion(suggestion: ProductSuggestion) {
        _state.update { current ->
            current.copy(
                note = suggestion.name,
                selectedCategoryId = suggestion.categoryId ?: current.selectedCategoryId,
                amount = if (current.amount.kopecks > 0) {
                    current.amount
                } else {
                    suggestion.lastPriceKopecks
                        ?.let { AmountInput.ofKopecks(it) }
                        ?: current.amount
                },
            )
        }
    }

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
