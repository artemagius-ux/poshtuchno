package io.github.artemagius.poshtuchno.ui.today

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.charts.Sparkline
import io.github.artemagius.poshtuchno.ui.components.EmptyPlaceholder
import io.github.artemagius.poshtuchno.ui.components.PurchaseRow
import io.github.artemagius.poshtuchno.ui.components.SectionHeader
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayTitleFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

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
            top = 8.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DayHeroCard(state) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "За неделю",
                    value = Money.format(state.weekTotalKopecks),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "В среднем в день",
                    value = Money.format(state.averagePerDayKopecks),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { MonthCard(state, onLimitClick) }

        item {
            SectionHeader(
                title = "Траты за день",
                action = {
                    if (state.dayPurchases.isNotEmpty()) {
                        Text(
                            text = "${state.dayPurchases.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }

        if (state.dayPurchases.isEmpty()) {
            item {
                EmptyPlaceholder(
                    title = if (state.loaded) "Сегодня трат ещё нет" else "Загрузка…",
                    subtitle = if (state.loaded) "Нажми «Трата», чтобы записать первую" else null,
                )
            }
        } else {
            items(state.dayPurchases, key = { it.id }) { purchase ->
                PurchaseRow(
                    purchase = purchase,
                    accent = purchase.topCategoryColorArgb
                        ?.takeIf { it != 0 }
                        ?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primary,
                    icon = purchase.topCategoryIcon,
                    showDate = false,
                    onClick = null,
                )
            }
        }
    }
}

@Composable
private fun DayHeroCard(state: TodayUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = dayTitleFormatter.format(state.date)
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Money.format(state.dayTotalKopecks),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "потрачено сегодня",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )

            if (state.last14Days.size >= 2) {
                Spacer(Modifier.height(16.dp))
                Sparkline(
                    values = state.last14Days.map { it.totalKopecks },
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "последние 14 дней",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MonthCard(state: TodayUiState, onLimitClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                    Text(
                        text = Money.format(state.monthTotalKopecks),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                TextButton(onClick = onLimitClick) {
                    Text(if (state.monthLimitKopecks == null) "Задать лимит" else "Лимит")
                }
            }

            val limit = state.monthLimitKopecks
            if (limit != null && limit > 0) {
                val over = state.monthTotalKopecks > limit
                LinearProgressIndicator(
                    progress = { (state.monthTotalKopecks.toFloat() / limit).coerceIn(0f, 1f) },
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                )
                Text(
                    text = if (over) {
                        "Перерасход ${Money.format(state.monthTotalKopecks - limit)}"
                    } else {
                        "Осталось ${Money.format(limit - state.monthTotalKopecks)} из ${Money.format(limit)}"
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
                Text(
                    text = "При таком темпе за месяц выйдет ${Money.format(state.monthProjectionKopecks)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayPreview() {
    val now = System.currentTimeMillis()
    val base = LocalDate.now()
    PoshtuchnoTheme(dynamicColor = false) {
        TodayScreen(
            state = TodayUiState(
                date = base,
                dayTotalKopecks = 54_300,
                weekTotalKopecks = 412_000,
                monthTotalKopecks = 1_428_50,
                monthLimitKopecks = 3_000_000,
                monthProjectionKopecks = 2_100_000,
                averagePerDayKopecks = 47_600,
                dayPurchases = listOf(
                    PurchaseListItem(1, now, 42_350, "Пятёрочка", null, 4, "Продукты", "cart", 0xFF43A047.toInt()),
                    PurchaseListItem(2, now - 3_600_000, 11_900, "Энергетик", null, 1, "Продукты", "cart", 0xFF43A047.toInt()),
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
