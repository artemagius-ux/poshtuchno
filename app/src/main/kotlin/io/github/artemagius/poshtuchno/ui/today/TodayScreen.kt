package io.github.artemagius.poshtuchno.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.charts.Sparkline
import io.github.artemagius.poshtuchno.ui.components.AppCard
import io.github.artemagius.poshtuchno.ui.components.EmptyPlaceholder
import io.github.artemagius.poshtuchno.ui.components.PurchaseRow
import io.github.artemagius.poshtuchno.ui.components.SectionHeader
import io.github.artemagius.poshtuchno.ui.components.SwipeToDelete
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.rememberDateFormatter
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import io.github.artemagius.poshtuchno.ui.titlecaseFirst
import java.time.LocalDate

@Composable
fun TodayScreen(
    state: TodayUiState,
    onLimitClick: () -> Unit,
    onDeletePurchase: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TodayCard(state) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "За неделю",
                    value = money(state.weekTotalKopecks),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "В среднем в день",
                    value = money(state.averagePerDayKopecks),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { MonthCard(state, onLimitClick) }

        item { SectionHeader(title = "Траты за день") }

        if (state.dayPurchases.isEmpty()) {
            item {
                EmptyPlaceholder(
                    illustration = R.drawable.il_empty_receipt,
                    title = if (state.loaded) "Сегодня трат ещё нет" else "Загрузка",
                    subtitle = if (state.loaded) "Нажми «Трата», чтобы записать первую" else null,
                )
            }
        } else {
            items(state.dayPurchases, key = { it.id }) { purchase ->
                SwipeToDelete(onDelete = { onDeletePurchase(purchase.id) }) {
                    PurchaseRow(purchase = purchase, showDate = false)
                }
            }
        }
    }
}

/**
 * Главная карточка: сумма за сегодня. Единственный жирный элемент —
 * само число, всё остальное обычным весом.
 */
@Composable
private fun TodayCard(state: TodayUiState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = rememberDateFormatter("EEEE, d MMMM").format(state.date).titlecaseFirst(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = money(state.dayTotalKopecks),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "потрачено сегодня",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.last14Days.size >= 2) {
            Spacer(Modifier.height(20.dp))
            Sparkline(
                values = state.last14Days.map { it.totalKopecks },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "последние 14 дней",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, contentPadding = 16.dp) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun MonthCard(state: TodayUiState, onLimitClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "За месяц",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = money(state.monthTotalKopecks),
                    style = MaterialTheme.typography.displaySmall,
                )
            }
            TextButton(onClick = onLimitClick) {
                Text(if (state.monthLimitKopecks == null) "Задать лимит" else "Лимит")
            }
        }

        val limit = state.monthLimitKopecks
        if (limit != null && limit > 0) {
            Spacer(Modifier.height(14.dp))
            val over = state.monthTotalKopecks > limit
            LinearProgressIndicator(
                progress = { (state.monthTotalKopecks.toFloat() / limit).coerceIn(0f, 1f) },
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (over) {
                    "Перерасход ${money(state.monthTotalKopecks - limit)}"
                } else {
                    "Осталось ${money(limit - state.monthTotalKopecks)} из ${money(limit)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (over) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        if (state.monthProjectionKopecks > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "При таком темпе за месяц выйдет ${money(state.monthProjectionKopecks)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayPreview() {
    val now = System.currentTimeMillis()
    val base = LocalDate.now()
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
        TodayScreen(
            state = TodayUiState(
                date = base,
                dayTotalKopecks = 54_300,
                weekTotalKopecks = 412_000,
                monthTotalKopecks = 1_782_000,
                monthLimitKopecks = 3_000_000,
                monthProjectionKopecks = 2_100_000,
                averagePerDayKopecks = 47_600,
                dayPurchases = listOf(
                    PurchaseListItem(1, now, 42_350, "Пятёрочка", null, 4, "Продукты", "cart", 0),
                    PurchaseListItem(2, now - 3_600_000, 11_900, "Энергетик", null, 1, "Продукты", "cart", 0),
                ),
                last14Days = (0..13).map { DayAmount(base.minusDays((13 - it).toLong()), 20_000L + it * 5_000) },
                loaded = true,
            ),
            onLimitClick = {},
            onDeletePurchase = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
