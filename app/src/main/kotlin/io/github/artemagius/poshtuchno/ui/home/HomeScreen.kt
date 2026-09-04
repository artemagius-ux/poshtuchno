package io.github.artemagius.poshtuchno.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.Periods
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onLimitClick: () -> Unit,
    onDeletePurchase: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Поштучно") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Трата") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                // Место под FAB, чтобы он не накрывал последнюю трату.
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                MonthSummaryCard(
                    monthName = state.monthName,
                    totalKopecks = state.monthTotalKopecks,
                    limitKopecks = state.monthLimitKopecks,
                    onLimitClick = onLimitClick,
                )
            }

            item {
                Text(
                    text = "Последние траты",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            if (state.recent.isEmpty()) {
                item { EmptyState() }
            } else {
                items(state.recent, key = { it.id }) { purchase ->
                    PurchaseRow(purchase = purchase, onDelete = { onDeletePurchase(purchase.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(
    monthName: String,
    totalKopecks: Long,
    limitKopecks: Long?,
    onLimitClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Money.format(totalKopecks),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )

            if (limitKopecks != null && limitKopecks > 0) {
                val progress = (totalKopecks.toFloat() / limitKopecks).coerceIn(0f, 1f)
                val over = totalKopecks > limitKopecks
                LinearProgressIndicator(
                    progress = { progress },
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (over) {
                            "Перерасход ${Money.format(totalKopecks - limitKopecks)}"
                        } else {
                            "Осталось ${Money.format(limitKopecks - totalKopecks)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (over) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    TextButton(onClick = onLimitClick) { Text("Лимит") }
                }
            } else {
                TextButton(
                    onClick = onLimitClick,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("Установить лимит")
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(purchase: PurchaseListItem, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = purchaseTitle(purchase),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            supportingContent = {
                Text(
                    text = purchaseSubtitle(purchase),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Money.format(purchase.totalKopecks),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.size(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить трату",
                        )
                    }
                }
            },
        )
    }
}

private fun purchaseTitle(purchase: PurchaseListItem): String = when {
    !purchase.note.isNullOrBlank() -> purchase.note
    !purchase.shopName.isNullOrBlank() -> purchase.shopName
    !purchase.topCategoryName.isNullOrBlank() -> purchase.topCategoryName
    else -> "Трата"
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")

private fun purchaseSubtitle(purchase: PurchaseListItem): String {
    val dateTime = Periods.toLocalDateTime(purchase.purchasedAt)
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()
    val whenText = when (date) {
        today -> "сегодня, ${timeFormatter.format(dateTime)}"
        today.minusDays(1) -> "вчера, ${timeFormatter.format(dateTime)}"
        else -> dateFormatter.format(date)
    }
    val category = purchase.topCategoryName
    return if (category.isNullOrBlank()) whenText else "$category · $whenText"
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Пока пусто",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Нажми «Трата», чтобы записать первую покупку",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val now = System.currentTimeMillis()
    PoshtuchnoTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                monthTotalKopecks = 1_428_50,
                monthLimitKopecks = 30_000_00,
                monthName = "Сентябрь",
                recent = listOf(
                    PurchaseListItem(1, now, 42_350, "Пятёрочка", null, 4, "Продукты"),
                    PurchaseListItem(2, now - 86_400_000, 11_900, null, null, 1, "Энергетики"),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onAddClick = {},
            onLimitClick = {},
            onDeletePurchase = {},
        )
    }
}
