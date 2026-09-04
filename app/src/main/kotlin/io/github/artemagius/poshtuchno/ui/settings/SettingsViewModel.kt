package io.github.artemagius.poshtuchno.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val monthLimitKopecks: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val monthTotalKopecks: Long = 0,
)

class SettingsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.observeMonthlyLimit(),
        repository.observeCategories(),
        repository.observeTotal(Periods.monthRange()),
    ) { budget, categories, total ->
        SettingsUiState(
            monthLimitKopecks = budget?.limitKopecks,
            categories = categories,
            monthTotalKopecks = total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setMonthlyLimit(kopecks: Long?) {
        viewModelScope.launch { repository.setMonthlyLimit(kopecks) }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.upsertCategory(category) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }
}
