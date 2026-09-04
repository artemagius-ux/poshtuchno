package io.github.artemagius.poshtuchno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.artemagius.poshtuchno.ui.home.HomeMessage
import io.github.artemagius.poshtuchno.ui.home.HomeScreen
import io.github.artemagius.poshtuchno.ui.home.HomeViewModel
import io.github.artemagius.poshtuchno.ui.home.MonthLimitDialog
import io.github.artemagius.poshtuchno.ui.quickadd.QuickAddSheet
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as PoshtuchnoApp).container
        setContent {
            PoshtuchnoTheme {
                PoshtuchnoRoot(
                    viewModel = viewModel(
                        factory = HomeViewModel.factory(container.expenseRepository),
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoshtuchnoRoot(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()
    val quickAdd by viewModel.quickAdd.collectAsState()
    val quickCategories by viewModel.quickCategories.collectAsState()
    val message by viewModel.messages.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var limitDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        when (message) {
            HomeMessage.PurchaseDeleted -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Трата удалена",
                    actionLabel = "Отменить",
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                } else {
                    viewModel.consumeMessage()
                }
            }
            null -> Unit
        }
    }

    HomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAddClick = viewModel::openQuickAdd,
        onLimitClick = { limitDialogVisible = true },
        onDeletePurchase = viewModel::deletePurchase,
    )

    if (quickAdd.visible) {
        QuickAddSheet(
            sheetState = sheetState,
            amount = quickAdd.amount,
            categories = quickCategories,
            selectedCategoryId = quickAdd.selectedCategoryId,
            note = quickAdd.note,
            onDigit = viewModel::onDigit,
            onSeparator = viewModel::onSeparator,
            onBackspace = viewModel::onBackspace,
            onCategorySelect = viewModel::onCategorySelect,
            onNoteChange = viewModel::onNoteChange,
            onSave = viewModel::saveQuickAdd,
            onDismiss = viewModel::closeQuickAdd,
        )
    }

    if (limitDialogVisible) {
        MonthLimitDialog(
            currentLimitKopecks = state.monthLimitKopecks,
            onDismiss = { limitDialogVisible = false },
            onConfirm = { limit ->
                viewModel.setMonthlyLimit(limit)
                limitDialogVisible = false
            },
        )
    }
}
