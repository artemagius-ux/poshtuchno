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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.artemagius.poshtuchno.data.AppSettings
import io.github.artemagius.poshtuchno.ui.LocalShowKopecks
import io.github.artemagius.poshtuchno.ui.Tab
import io.github.artemagius.poshtuchno.ui.additem.AddItemsScreen
import io.github.artemagius.poshtuchno.ui.additem.AddItemsViewModel
import io.github.artemagius.poshtuchno.ui.analytics.AnalyticsScreen
import io.github.artemagius.poshtuchno.ui.analytics.AnalyticsViewModel
import io.github.artemagius.poshtuchno.ui.components.MonthLimitDialog
import io.github.artemagius.poshtuchno.ui.history.HistoryMessage
import io.github.artemagius.poshtuchno.ui.history.HistoryScreen
import io.github.artemagius.poshtuchno.ui.history.HistoryViewModel
import io.github.artemagius.poshtuchno.ui.products.ProductsScreen
import io.github.artemagius.poshtuchno.ui.products.ProductsViewModel
import io.github.artemagius.poshtuchno.ui.quickadd.QuickAddSheet
import io.github.artemagius.poshtuchno.ui.quickadd.QuickAddViewModel
import io.github.artemagius.poshtuchno.ui.scan.ScanReceiptScreen
import io.github.artemagius.poshtuchno.ui.settings.SettingsScreen
import io.github.artemagius.poshtuchno.ui.settings.SettingsViewModel
import io.github.artemagius.poshtuchno.ui.theme.AppPalette
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
            val settings by container.settingsRepository.settings
                .collectAsState(initial = AppSettings())

            PoshtuchnoTheme(
                themeMode = settings.themeMode,
                palette = AppPalette.parse(settings.palette),
                dynamicColor = settings.dynamicColor,
            ) {
                CompositionLocalProvider(LocalShowKopecks provides settings.showKopecks) {
                    PoshtuchnoRoot(
                        container = container,
                        settings = settings,
                        onCloseRequested = { finish() },
                    )
                }
            }
        }
    }
}

/** Куда ведёт кнопка «+»: список вкладок или полноэкранные потоки. */
private enum class Screen { Tabs, AddItems, Scan }

@Composable
private fun PoshtuchnoRoot(
    container: AppContainer,
    settings: AppSettings,
    onCloseRequested: () -> Unit,
) {
    val repository = container.expenseRepository
    var screen by remember { mutableStateOf(Screen.Tabs) }

    val addItemsVm: AddItemsViewModel = viewModel(
        factory = viewModelFactory { AddItemsViewModel(repository) },
    )
    val addState by addItemsVm.state.collectAsState()
    val suggestions by addItemsVm.suggestions.collectAsState()
    val addSaved by addItemsVm.saved.collectAsState()

    LaunchedEffect(addSaved) {
        if (addSaved != null) {
            addItemsVm.consumeSaved()
            screen = Screen.Tabs
        }
    }

    when (screen) {
        Screen.Tabs -> TabsScreen(
            container = container,
            settings = settings,
            onCloseRequested = onCloseRequested,
            onAddItemsRequested = {
                addItemsVm.startManual()
                screen = Screen.AddItems
            },
            onScanRequested = { screen = Screen.Scan },
        )

        Screen.AddItems -> AddItemsScreen(
            state = addState,
            suggestions = suggestions,
            onClose = {
                addItemsVm.reset()
                screen = Screen.Tabs
            },
            onScanClick = { screen = Screen.Scan },
            onAddDraft = addItemsVm::addEmptyDraft,
            onRemoveDraft = addItemsVm::removeDraft,
            onEditDraft = addItemsVm::editDraft,
            onNameChange = addItemsVm::onNameChange,
            onPriceChange = addItemsVm::onPriceChange,
            onQuantityIncrement = addItemsVm::incrementQuantity,
            onQuantityDecrement = addItemsVm::decrementQuantity,
            onCategoryChange = addItemsVm::onCategoryChange,
            onApplySuggestion = addItemsVm::applySuggestion,
            onAddRemainder = { addItemsVm.addRemainderAsItem() },
            onSave = addItemsVm::save,
        )

        Screen.Scan -> ScanReceiptScreen(
            onResult = { receipt ->
                addItemsVm.startFromReceipt(receipt)
                screen = Screen.AddItems
            },
            onManualInstead = {
                addItemsVm.startManual()
                screen = Screen.AddItems
            },
            onClose = { screen = Screen.Tabs },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsScreen(
    container: AppContainer,
    settings: AppSettings,
    onCloseRequested: () -> Unit,
    onAddItemsRequested: () -> Unit,
    onScanRequested: () -> Unit,
) {
    val repository = container.expenseRepository

    val todayVm: TodayViewModel = viewModel(factory = viewModelFactory { TodayViewModel(repository) })
    val historyVm: HistoryViewModel = viewModel(factory = viewModelFactory { HistoryViewModel(repository) })
    val productsVm: ProductsViewModel = viewModel(factory = viewModelFactory { ProductsViewModel(repository) })
    val analyticsVm: AnalyticsViewModel = viewModel(factory = viewModelFactory { AnalyticsViewModel(repository) })
    val settingsVm: SettingsViewModel = viewModel(
        factory = viewModelFactory { SettingsViewModel(repository, container.settingsRepository) },
    )
    val quickAddVm: QuickAddViewModel = viewModel(factory = viewModelFactory { QuickAddViewModel(repository) })

    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var limitDialogVisible by remember { mutableStateOf(false) }
    var addMenuVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val quickSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addMenuState = rememberModalBottomSheetState()

    val todayState by todayVm.uiState.collectAsState()
    val historyState by historyVm.uiState.collectAsState()
    val productsState by productsVm.uiState.collectAsState()
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
            quickAddVm.consumeSaved()
            if (settings.closeAfterSave) {
                onCloseRequested()
            } else {
                snackbarHostState.showSnackbar("Трата записана")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tab.label, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 0.dp,
            ) {
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
                        colors = NavigationBarItemDefaults.colors(
                            // Активная вкладка — мягкая пилюля в цвете акцента.
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != Tab.Settings) {
                FloatingActionButton(
                    onClick = { addMenuVisible = true },
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Добавить",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
    ) { padding ->
        // FAB лежит отдельным слоем поверх списка, поэтому к нижнему отступу
        // добавляем его высоту с зазором — кнопка не должна накрывать контент.
        val listPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + if (tab != Tab.Settings) 84.dp else 8.dp,
        )

        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
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

                Tab.Products -> ProductsScreen(
                    state = productsState,
                    onPeriodSelect = productsVm::selectPeriod,
                    onToggleGroup = productsVm::toggleGroup,
                    onQueryChange = productsVm::onQueryChange,
                    onRenameGroup = productsVm::renameGroup,
                    onHideGroup = productsVm::hideGroup,
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
                    onThemeModeChange = settingsVm::setThemeMode,
                    onPaletteChange = settingsVm::setPalette,
                    onDynamicColorChange = settingsVm::setDynamicColor,
                    onCloseAfterSaveChange = settingsVm::setCloseAfterSave,
                    onShowKopecksChange = settingsVm::setShowKopecks,
                    onSaveCategory = settingsVm::saveCategory,
                    onDeleteCategory = settingsVm::deleteCategory,
                    contentPadding = listPadding,
                )
            }
        }
    }

    if (addMenuVisible) {
        ModalBottomSheet(
            onDismissRequest = { addMenuVisible = false },
            sheetState = addMenuState,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AddOption(
                    icon = R.drawable.ic_add,
                    title = "Быстрая трата",
                    subtitle = "Сумма и категория — пара секунд",
                    onClick = {
                        addMenuVisible = false
                        quickAddVm.open()
                    },
                )
                AddOption(
                    icon = R.drawable.ic_scan,
                    title = "Сканировать чек",
                    subtitle = "Дата, сумма и реквизиты из QR-кода",
                    onClick = {
                        addMenuVisible = false
                        onScanRequested()
                    },
                )
                AddOption(
                    icon = R.drawable.ic_tab_products,
                    title = "Покупка по позициям",
                    subtitle = "Разложить по товарам с подсказками",
                    onClick = {
                        addMenuVisible = false
                        onAddItemsRequested()
                    },
                )
            }
        }
    }

    if (quickAdd.visible) {
        QuickAddSheet(
            sheetState = quickSheetState,
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

@Composable
private fun AddOption(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
