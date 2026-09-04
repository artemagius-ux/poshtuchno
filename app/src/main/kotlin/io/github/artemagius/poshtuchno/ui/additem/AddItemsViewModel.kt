package io.github.artemagius.poshtuchno.ui.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.FiscalMarks
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.data.db.ProductSuggestion
import io.github.artemagius.poshtuchno.data.db.PurchaseSource
import io.github.artemagius.poshtuchno.data.receipt.ReceiptQr
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddItemsState(
    val drafts: List<ItemDraft> = emptyList(),
    val editingId: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    /** Сумма из QR чека: пока позиции не разложены, показываем остаток. */
    val receiptTotalKopecks: Long? = null,
    val receiptAt: Long? = null,
    val fiscal: FiscalMarks? = null,
    val duplicateReceipt: Boolean = false,
    val saving: Boolean = false,
) {
    val itemsTotalKopecks: Long get() = drafts.sumOf { it.sumKopecks }

    /** Сколько ещё не разложено по позициям. */
    val remainderKopecks: Long?
        get() = receiptTotalKopecks?.let { it - itemsTotalKopecks }

    val canSave: Boolean get() = drafts.any { it.isValid } && !saving
}

/**
 * Ввод покупки по позициям: вручную или после сканирования QR чека.
 */
class AddItemsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _state = MutableStateFlow(AddItemsState())
    val state: StateFlow<AddItemsState> = _state.asStateFlow()

    private val query = MutableStateFlow("")

    private val _saved = MutableStateFlow<Long?>(null)
    val saved: StateFlow<Long?> = _saved.asStateFlow()

    private var nextDraftId = 1L

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<ProductSuggestion>> = query
        // Подсказки бьют в базу на каждый символ, поэтому ждём паузу в наборе.
        .debounce(180)
        .mapLatest { text -> repository.productSuggestions(text) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    /** Начинает пустую покупку с одной строкой. */
    fun startManual() {
        nextDraftId = 1
        _state.value = AddItemsState(
            drafts = listOf(ItemDraft(id = nextDraftId++)),
            editingId = 1,
            categories = _state.value.categories,
        )
        query.value = ""
    }

    /**
     * Начинает покупку по данным QR чека. Позиции остаются за пользователем:
     * в QR их нет, там только шапка и реквизиты.
     */
    fun startFromReceipt(receipt: ReceiptQr.Receipt) {
        nextDraftId = 1
        viewModelScope.launch {
            val duplicate = repository.isFiscalDuplicate(receipt.fn, receipt.fd, receipt.fp)
            _state.value = AddItemsState(
                drafts = listOf(ItemDraft(id = nextDraftId++)),
                editingId = 1,
                categories = _state.value.categories,
                receiptTotalKopecks = receipt.totalKopecks,
                receiptAt = receipt.purchasedAt,
                fiscal = FiscalMarks(receipt.fn, receipt.fd, receipt.fp),
                duplicateReceipt = duplicate,
            )
        }
        query.value = ""
    }

    fun addEmptyDraft() {
        val draft = ItemDraft(id = nextDraftId++)
        _state.update { it.copy(drafts = it.drafts + draft, editingId = draft.id) }
        query.value = ""
    }

    fun removeDraft(id: Long) {
        _state.update { current ->
            val remaining = current.drafts.filterNot { it.id == id }
            current.copy(
                drafts = remaining,
                editingId = if (current.editingId == id) remaining.lastOrNull()?.id else current.editingId,
            )
        }
    }

    fun editDraft(id: Long?) {
        _state.update { it.copy(editingId = id) }
        query.value = id?.let { editing -> _state.value.drafts.firstOrNull { it.id == editing }?.name }.orEmpty()
    }

    fun onNameChange(id: Long, name: String) {
        updateDraft(id) { it.copy(name = name, productId = null, previousPriceKopecks = null) }
        query.value = name
    }

    fun onPriceChange(id: Long, kopecks: Long) = updateDraft(id) { it.copy(priceKopecks = kopecks) }

    fun onQuantityChange(id: Long, quantityMilli: Long) =
        updateDraft(id) { it.copy(quantityMilli = quantityMilli.coerceAtLeast(1)) }

    fun onCategoryChange(id: Long, categoryId: Long) =
        updateDraft(id) { it.copy(categoryId = categoryId) }

    /** Увеличивает количество на единицу — «взял ещё одну такую же». */
    fun incrementQuantity(id: Long) =
        updateDraft(id) { it.copy(quantityMilli = it.quantityMilli + 1000) }

    fun decrementQuantity(id: Long) =
        updateDraft(id) { it.copy(quantityMilli = (it.quantityMilli - 1000).coerceAtLeast(1000)) }

    /**
     * Подставляет товар из подсказки: название, прошлая цена и категория.
     * Если такой товар уже есть в списке — увеличивает его количество,
     * а не создаёт вторую строку.
     */
    fun applySuggestion(draftId: Long, suggestion: ProductSuggestion) {
        val current = _state.value
        val duplicate = current.drafts.firstOrNull {
            it.id != draftId && it.productId == suggestion.id
        }

        if (duplicate != null) {
            _state.update { state ->
                state.copy(
                    drafts = state.drafts
                        .filterNot { it.id == draftId }
                        .map {
                            if (it.id == duplicate.id) {
                                it.copy(quantityMilli = it.quantityMilli + 1000)
                            } else {
                                it
                            }
                        },
                    editingId = duplicate.id,
                )
            }
            query.value = duplicate.name
            return
        }

        updateDraft(draftId) {
            it.copy(
                name = suggestion.name,
                productId = suggestion.id,
                categoryId = it.categoryId ?: suggestion.categoryId,
                priceKopecks = if (it.priceKopecks > 0) {
                    it.priceKopecks
                } else {
                    suggestion.lastPriceKopecks ?: 0
                },
                previousPriceKopecks = suggestion.lastPriceKopecks,
            )
        }
        query.value = suggestion.name
    }

    /** Остаток чека одной строкой — удобно закрыть «прочее» после разбора. */
    fun addRemainderAsItem(name: String = "Прочее") {
        val remainder = _state.value.remainderKopecks ?: return
        if (remainder <= 0) return
        val draft = ItemDraft(id = nextDraftId++, name = name, priceKopecks = remainder)
        _state.update { it.copy(drafts = it.drafts + draft, editingId = null) }
    }

    fun save() {
        val current = _state.value
        val items = current.drafts.filter { it.isValid }.map { it.toNewItem() }
        if (items.isEmpty()) return

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = repository.addItemizedPurchase(
                items = items,
                purchasedAt = current.receiptAt ?: System.currentTimeMillis(),
                source = if (current.fiscal != null) PurchaseSource.QR else PurchaseSource.MANUAL,
                fiscal = current.fiscal,
            )
            _state.value = AddItemsState(categories = current.categories)
            _saved.value = id
        }
    }

    fun consumeSaved() {
        _saved.value = null
    }

    fun reset() {
        _state.value = AddItemsState(categories = _state.value.categories)
        query.value = ""
    }

    private fun updateDraft(id: Long, transform: (ItemDraft) -> ItemDraft) {
        _state.update { state ->
            state.copy(drafts = state.drafts.map { if (it.id == id) transform(it) else it })
        }
    }

    /** Категории, которыми пользовались недавно — идут первыми в выборе. */
    val frequentCategories: StateFlow<List<CategoryEntity>> = combine(
        repository.observeFrequentCategories(since = Periods.monthRange().first),
        repository.observeCategories(),
    ) { frequent, all ->
        val result = LinkedHashMap<Long, CategoryEntity>()
        frequent.forEach { result[it.id] = it }
        all.forEach { result.putIfAbsent(it.id, it) }
        result.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
