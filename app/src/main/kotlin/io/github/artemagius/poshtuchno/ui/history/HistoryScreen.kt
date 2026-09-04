package io.github.artemagius.poshtuchno.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.components.DayHeader
import io.github.artemagius.poshtuchno.ui.components.EmptyPlaceholder
import io.github.artemagius.poshtuchno.ui.components.PurchaseRow
import io.github.artemagius.poshtuchno.ui.components.SwipeToDelete
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.getDefault())

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onQueryChange: (String) -> Unit,
    onDeletePurchase: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            MonthSwitcher(
                title = state.monthTitle,
                totalKopecks = state.monthTotalKopecks,
                purchaseCount = state.purchaseCount,
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            )
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Поиск по заметке или категории") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }

        if (state.groups.isEmpty()) {
            item {
                EmptyPlaceholder(
                    title = when {
                        !state.loaded -> "Загрузка…"
                        state.query.isNotBlank() -> "Ничего не нашлось"
                        else -> "В этом месяце трат нет"
                    },
                    subtitle = if (state.query.isBlank() && state.loaded) {
                        "Переключи месяц или добавь трату"
                    } else {
                        null
                    },
                )
            }
        } else {
            state.groups.forEach { group ->
                item(key = "header-${group.date}") {
                    DayHeader(
                        title = dayFormatter.format(group.date)
                            .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                        totalKopecks = group.totalKopecks,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
                items(
                    items = group.purchases,
                    key = { purchase -> purchase.id },
                ) { purchase ->
                    SwipeToDelete(onDelete = { onDeletePurchase(purchase.id) }) {
                        PurchaseRow(
                            purchase = purchase,
                            accent = purchase.topCategoryColorArgb
                                ?.takeIf { it != 0 }
                                ?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.primary,
                            icon = purchase.topCategoryIcon,
                            showDate = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    title: String,
    totalKopecks: Long,
    purchaseCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious, enabled = canGoBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "Предыдущий месяц",
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Money.format(totalKopecks),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = purchaseCountText(purchaseCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "Следующий месяц",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Русское склонение: 1 трата, 2 траты, 5 трат. */
internal fun purchaseCountText(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val word = when {
        mod100 in 11..14 -> "трат"
        mod10 == 1 -> "трата"
        mod10 in 2..4 -> "траты"
        else -> "трат"
    }
    return "$count $word"
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    val now = System.currentTimeMillis()
    PoshtuchnoTheme(dynamicColor = false) {
        HistoryScreen(
            state = HistoryUiState(
                monthTitle = "Сентябрь",
                monthTotalKopecks = 1_428_50,
                purchaseCount = 3,
                groups = listOf(
                    DayGroup(
                        date = LocalDate.now(),
                        totalKopecks = 54_250,
                        purchases = listOf(
                            PurchaseListItem(1, now, 42_350, "Пятёрочка", null, 4, "Продукты", "cart", 0xFF43A047.toInt()),
                            PurchaseListItem(2, now, 11_900, "Энергетик", null, 1, "Продукты", "cart", 0xFF43A047.toInt()),
                        ),
                    ),
                ),
                loaded = true,
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onQueryChange = {},
            onDeletePurchase = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
