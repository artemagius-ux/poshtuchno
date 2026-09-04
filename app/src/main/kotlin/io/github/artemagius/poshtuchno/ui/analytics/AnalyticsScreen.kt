package io.github.artemagius.poshtuchno.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.db.CategoryBreakdown
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.charts.DailyBarChart
import io.github.artemagius.poshtuchno.ui.charts.DonutChart
import io.github.artemagius.poshtuchno.ui.charts.DonutLegend
import io.github.artemagius.poshtuchno.ui.charts.DonutSlice
import io.github.artemagius.poshtuchno.ui.components.EmptyPlaceholder
import io.github.artemagius.poshtuchno.ui.components.PurchaseRow
import io.github.artemagius.poshtuchno.ui.components.SectionHeader
import io.github.artemagius.poshtuchno.ui.model.displayName
import io.github.artemagius.poshtuchno.ui.model.resolveColor
import io.github.artemagius.poshtuchno.ui.theme.LocalChartColors
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val selectedDayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onPeriodSelect: (AnalyticsPeriod) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val palette = LocalChartColors.current
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    val slices = state.breakdown.mapIndexed { index, item ->
        DonutSlice(
            label = item.displayName(),
            amountKopecks = item.totalKopecks,
            color = item.resolveColor(palette, index),
        )
    }

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
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AnalyticsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { onPeriodSelect(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AnalyticsPeriod.entries.size,
                        ),
                        label = { Text(period.label) },
                    )
                }
            }
        }

        item { SummaryCard(state) }

        if (state.days.size >= 2) {
            item {
                DaysCard(
                    days = state.days,
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = it },
                )
            }
        }

        item { SectionHeader(title = "По категориям") }

        if (slices.isEmpty()) {
            item {
                EmptyPlaceholder(
                    title = if (state.loaded) "За период трат нет" else "Загрузка…",
                    subtitle = if (state.loaded) "Выбери другой период или добавь траты" else null,
                )
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DonutChart(
                            slices = slices,
                            totalKopecks = state.totalKopecks,
                            centerLabel = state.title.lowercase(Locale.getDefault()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                        )
                        Spacer(Modifier.height(20.dp))
                        DonutLegend(slices = slices, shares = state.shares)
                    }
                }
            }
        }

        if (state.topPurchases.isNotEmpty()) {
            item { SectionHeader(title = "Самые крупные траты") }
            items(state.topPurchases, key = { it.id }) { purchase ->
                PurchaseRow(
                    purchase = purchase,
                    accent = purchase.topCategoryColorArgb
                        ?.takeIf { it != 0 }
                        ?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primary,
                    icon = purchase.topCategoryIcon,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: AnalyticsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Money.format(state.totalKopecks),
                style = MaterialTheme.typography.displaySmall,
            )

            val change = state.changePercent
            if (change != null) {
                Spacer(Modifier.height(6.dp))
                val grew = change > 0
                Text(
                    text = when {
                        change == 0 -> "Столько же, сколько в прошлом периоде"
                        grew -> "На $change% больше прошлого периода"
                        else -> "На ${-change}% меньше прошлого периода"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        change == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        grew -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricColumn(
                    label = "Трат",
                    value = state.purchaseCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricColumn(
                    label = "Средний чек",
                    value = Money.format(state.averagePurchaseKopecks, withCurrency = false),
                    modifier = Modifier.weight(1f),
                )
                MetricColumn(
                    label = "В активный день",
                    value = Money.format(state.averagePerActiveDayKopecks, withCurrency = false),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DaysCard(
    days: List<DayAmount>,
    selectedDay: LocalDate?,
    onDaySelected: (LocalDate?) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "По дням", style = MaterialTheme.typography.titleMedium)
                val selected = days.firstOrNull { it.date == selectedDay }
                Text(
                    text = if (selected != null) {
                        "${selectedDayFormatter.format(selected.date)}: ${Money.format(selected.totalKopecks)}"
                    } else {
                        "нажми на столбик"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            DailyBarChart(
                days = days,
                highlighted = selectedDay,
                onDaySelected = onDaySelected,
                labelEvery = if (days.size > 14) 5 else 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsPreview() {
    val now = System.currentTimeMillis()
    val base = LocalDate.now().withDayOfMonth(1)
    PoshtuchnoTheme(dynamicColor = false) {
        AnalyticsScreen(
            state = AnalyticsUiState(
                title = "Сентябрь",
                totalKopecks = 1_782_000,
                changePercent = 12,
                purchaseCount = 34,
                activeDayCount = 18,
                averagePerActiveDayKopecks = 99_000,
                averagePurchaseKopecks = 52_400,
                breakdown = listOf(
                    CategoryBreakdown(1, "Продукты", "cart", 0xFF43A047.toInt(), 980_000, 42),
                    CategoryBreakdown(2, "Кафе", "cafe", 0, 420_000, 8),
                    CategoryBreakdown(3, "Транспорт", "transport", 0, 220_000, 12),
                    CategoryBreakdown(4, "Дом", "home", 0, 162_000, 3),
                ),
                shares = listOf(55, 24, 12, 9),
                days = (0..20).map { DayAmount(base.plusDays(it.toLong()), if (it % 5 == 0) 0 else 40_000L + it * 3_000) },
                topPurchases = listOf(
                    PurchaseListItem(1, now, 342_000, "Большая закупка", "Лента", 24, "Продукты", "cart", 0xFF43A047.toInt()),
                ),
                loaded = true,
            ),
            onPeriodSelect = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
