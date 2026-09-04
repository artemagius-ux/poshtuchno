package io.github.artemagius.poshtuchno.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.artemagius.poshtuchno.data.AppSettings
import io.github.artemagius.poshtuchno.data.ExpenseRepository
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.SettingsRepository
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val monthLimitKopecks: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val monthTotalKopecks: Long = 0,
)

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        repository.observeMonthlyLimit(),
        repository.observeCategories(),
        repository.observeTotal(Periods.monthRange()),
    ) { settings, budget, categories, total ->
        SettingsUiState(
            settings = settings,
            monthLimitKopecks = budget?.limitKopecks,
            categories = categories,
            monthTotalKopecks = total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setMonthlyLimit(kopecks: Long?) {
        viewModelScope.launch { repository.setMonthlyLimit(kopecks) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setPalette(name: String) {
        viewModelScope.launch { settingsRepository.setPalette(name) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setCloseAfterSave(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCloseAfterSave(enabled) }
    }

    fun setShowKopecks(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowKopecks(enabled) }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.upsertCategory(category) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }
}
