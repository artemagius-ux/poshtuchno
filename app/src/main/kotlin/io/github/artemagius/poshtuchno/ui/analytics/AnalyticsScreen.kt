package io.github.artemagius.poshtuchno.ui.analytics

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.DayAmount
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.CategoryBreakdown
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.ui.charts.DailyBarChart
import io.github.artemagius.poshtuchno.ui.charts.DonutChart
import io.github.artemagius.poshtuchno.ui.charts.DonutLegend
import io.github.artemagius.poshtuchno.ui.charts.DonutSlice
import io.github.artemagius.poshtuchno.ui.components.AppCard
import io.github.artemagius.poshtuchno.ui.components.EmptyPlaceholder
import io.github.artemagius.poshtuchno.ui.components.PurchaseRow
import io.github.artemagius.poshtuchno.ui.components.SectionHeader
import io.github.artemagius.poshtuchno.ui.currentLocale
import io.github.artemagius.poshtuchno.ui.model.displayName
import io.github.artemagius.poshtuchno.ui.model.resolveColor
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.rememberDateFormatter
import io.github.artemagius.poshtuchno.ui.theme.LocalChartColors
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme
import java.time.LocalDate

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
            top = 4.dp + contentPadding.calculateTopPadding(),
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
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    illustration = R.drawable.il_empty_chart,
                    title = if (state.loaded) "За период трат нет" else "Загрузка",
                    subtitle = if (state.loaded) "Выбери другой период или добавь траты" else null,
                )
            }
        } else {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    DonutChart(
                        slices = slices,
                        totalKopecks = state.totalKopecks,
                        centerLabel = state.title.lowercase(currentLocale()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                    DonutLegend(slices = slices, shares = state.shares)
                }
            }
        }

        if (state.topPurchases.isNotEmpty()) {
            item { SectionHeader(title = "Самые крупные траты") }
            items(state.topPurchases, key = { it.id }) { purchase ->
                PurchaseRow(purchase = purchase)
            }
        }
    }
}

@Composable
private fun SummaryCard(state: AnalyticsUiState) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = money(state.totalKopecks),
            style = MaterialTheme.typography.displayMedium,
        )

        val change = state.changePercent
        if (change != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    change == 0 -> "Столько же, сколько в прошлом периоде"
                    change > 0 -> "На $change% больше прошлого периода"
                    else -> "На ${-change}% меньше прошлого периода"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    change == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    change > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricColumn(
                label = "Трат",
                value = state.purchaseCount.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricColumn(
                label = "Средний чек",
                value = money(state.averagePurchaseKopecks, withCurrency = false),
                modifier = Modifier.weight(1f),
            )
            MetricColumn(
                label = "В активный день",
                value = money(state.averagePerActiveDayKopecks, withCurrency = false),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(2.dp))
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
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "По дням", style = MaterialTheme.typography.titleSmall)
            val selected = days.firstOrNull { it.date == selectedDay }
            val formatter = rememberDateFormatter("d MMMM")
            Text(
                text = if (selected != null) {
                    "${formatter.format(selected.date)}: ${money(selected.totalKopecks)}"
                } else {
                    "нажми на столбик"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(18.dp))
        DailyBarChart(
            days = days,
            highlighted = selectedDay,
            onDaySelected = onDaySelected,
            labelEvery = if (days.size > 14) 5 else 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsPreview() {
    val now = System.currentTimeMillis()
    val base = LocalDate.now().withDayOfMonth(1)
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
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
                    CategoryBreakdown(1, "Продукты", "cart", 0, 980_000, 42),
                    CategoryBreakdown(2, "Кафе", "cafe", 0, 420_000, 8),
                    CategoryBreakdown(3, "Транспорт", "transport", 0, 220_000, 12),
                    CategoryBreakdown(4, "Дом", "home", 0, 162_000, 3),
                ),
                shares = listOf(55, 24, 12, 9),
                days = (0..20).map { DayAmount(base.plusDays(it.toLong()), if (it % 5 == 0) 0 else 40_000L + it * 3_000) },
                topPurchases = listOf(
                    PurchaseListItem(1, now, 342_000, "Большая закупка", "Лента", 24, "Продукты", "cart", 0),
                ),
                loaded = true,
            ),
            onPeriodSelect = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
