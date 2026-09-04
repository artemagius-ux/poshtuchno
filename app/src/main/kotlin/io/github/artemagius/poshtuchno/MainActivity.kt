package io.github.artemagius.poshtuchno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.artemagius.poshtuchno.ui.Tab
import io.github.artemagius.poshtuchno.ui.analytics.AnalyticsScreen
import io.github.artemagius.poshtuchno.ui.analytics.AnalyticsViewModel
import io.github.artemagius.poshtuchno.ui.components.MonthLimitDialog
import io.github.artemagius.poshtuchno.ui.history.HistoryMessage
import io.github.artemagius.poshtuchno.ui.history.HistoryScreen
import io.github.artemagius.poshtuchno.ui.history.HistoryViewModel
import io.github.artemagius.poshtuchno.ui.quickadd.QuickAddSheet
import io.github.artemagius.poshtuchno.ui.quickadd.QuickAddViewModel
import io.github.artemagius.poshtuchno.ui.settings.SettingsScreen
import io.github.artemagius.poshtuchno.ui.settings.SettingsViewModel
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import io.github.artemagius.poshtuchno.ui.today.TodayMessage
import io.github.artemagius.poshtuchno.ui.today.TodayScreen
import io.github.artemagius.poshtuchno.ui.today.TodayViewModel
import io.github.artemagius.poshtuchno.ui.viewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as PoshtuchnoApp).container
        setContent {
            PoshtuchnoTheme {
                PoshtuchnoApp(container)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoshtuchnoApp(container: AppContainer) {
    val repository = container.expenseRepository

    val todayVm: TodayViewModel = viewModel(factory = viewModelFactory { TodayViewModel(repository) })
    val historyVm: HistoryViewModel = viewModel(factory = viewModelFactory { HistoryViewModel(repository) })
    val analyticsVm: AnalyticsViewModel = viewModel(factory = viewModelFactory { AnalyticsViewModel(repository) })
    val settingsVm: SettingsViewModel = viewModel(factory = viewModelFactory { SettingsViewModel(repository) })
    val quickAddVm: QuickAddViewModel = viewModel(factory = viewModelFactory { QuickAddViewModel(repository) })

    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var limitDialogVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val todayState by todayVm.uiState.collectAsState()
    val historyState by historyVm.uiState.collectAsState()
    val analyticsState by analyticsVm.uiState.collectAsState()
    val settingsState by settingsVm.uiState.collectAsState()
    val quickAdd by quickAddVm.state.collectAsState()
    val quickCategories by quickAddVm.categories.collectAsState()

    // Дата пересчитывается при возврате в приложение: за ночь «сегодня» меняется.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) todayVm.refreshDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val todayMessage by todayVm.message.collectAsState()
    LaunchedEffect(todayMessage) {
        if (todayMessage == TodayMessage.Deleted) {
            val result = snackbarHostState.showSnackbar("Трата удалена", actionLabel = "Отменить")
            if (result == SnackbarResult.ActionPerformed) todayVm.undoDelete() else todayVm.consumeMessage()
        }
    }

    val historyMessage by historyVm.message.collectAsState()
    LaunchedEffect(historyMessage) {
        if (historyMessage == HistoryMessage.Deleted) {
            val result = snackbarHostState.showSnackbar("Трата удалена", actionLabel = "Отменить")
            if (result == SnackbarResult.ActionPerformed) historyVm.undoDelete() else historyVm.consumeMessage()
        }
    }

    val savedId by quickAddVm.saved.collectAsState()
    LaunchedEffect(savedId) {
        if (savedId != null) {
            snackbarHostState.showSnackbar("Трата записана")
            quickAddVm.consumeSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(tab.label) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {
                            Icon(
                                painter = painterResource(entry.icon),
                                contentDescription = entry.label,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = { Text(entry.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != Tab.Settings) {
                ExtendedFloatingActionButton(
                    onClick = quickAddVm::open,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    text = { Text("Трата") },
                )
            }
        },
    ) { padding ->
        // FAB перекрывает низ списка, поэтому добавляем к нижнему отступу его высоту.
        val listPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + if (tab != Tab.Settings) 72.dp else 0.dp,
        )

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "tabs",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                Tab.Today -> TodayScreen(
                    state = todayState,
                    onLimitClick = { limitDialogVisible = true },
                    onDeletePurchase = todayVm::deletePurchase,
                    contentPadding = listPadding,
                )

                Tab.History -> HistoryScreen(
                    state = historyState,
                    onPreviousMonth = historyVm::previousMonth,
                    onNextMonth = historyVm::nextMonth,
                    onQueryChange = historyVm::onQueryChange,
                    onDeletePurchase = historyVm::deletePurchase,
                    contentPadding = listPadding,
                )

                Tab.Analytics -> AnalyticsScreen(
                    state = analyticsState,
                    onPeriodSelect = analyticsVm::selectPeriod,
                    contentPadding = listPadding,
                )

                Tab.Settings -> SettingsScreen(
                    state = settingsState,
                    onLimitClick = { limitDialogVisible = true },
                    onSaveCategory = settingsVm::saveCategory,
                    onDeleteCategory = settingsVm::deleteCategory,
                    contentPadding = listPadding,
                )
            }
        }
    }

    if (quickAdd.visible) {
        QuickAddSheet(
            sheetState = sheetState,
            amount = quickAdd.amount,
            categories = quickCategories,
            selectedCategoryId = quickAdd.selectedCategoryId,
            note = quickAdd.note,
            onDigit = quickAddVm::onDigit,
            onSeparator = quickAddVm::onSeparator,
            onBackspace = quickAddVm::onBackspace,
            onCategorySelect = quickAddVm::onCategorySelect,
            onNoteChange = quickAddVm::onNoteChange,
            onSave = quickAddVm::save,
            onDismiss = quickAddVm::close,
        )
    }

    if (limitDialogVisible) {
        MonthLimitDialog(
            currentLimitKopecks = todayState.monthLimitKopecks ?: settingsState.monthLimitKopecks,
            onDismiss = { limitDialogVisible = false },
            onConfirm = { limit ->
                todayVm.setMonthlyLimit(limit)
                limitDialogVisible = false
            },
        )
    }
}
