package io.github.artemagius.poshtuchno.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

data class RecentExpense(
    val id: Long,
    val title: String,
    val amountKopecks: Long,
    val subtitle: String,
)

@Composable
fun HomeScreen(
    monthTotalKopecks: Long,
    monthLimitKopecks: Long?,
    recent: List<RecentExpense>,
    onAddClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Поштучно") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Трата") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthSummaryCard(monthTotalKopecks, monthLimitKopecks)

            Text(
                text = "Последние траты",
                style = MaterialTheme.typography.titleMedium,
            )

            if (recent.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recent, key = { it.id }) { expense ->
                        ExpenseRow(expense)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(totalKopecks: Long, limitKopecks: Long?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "В этом месяце",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Money.format(totalKopecks),
                style = MaterialTheme.typography.displaySmall,
            )
            if (limitKopecks != null && limitKopecks > 0) {
                val progress = (totalKopecks.toFloat() / limitKopecks).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Лимит ${Money.format(limitKopecks)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: RecentExpense) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(expense.title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = expense.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Money.format(expense.amountKopecks),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Пока пусто",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Нажми «Трата», чтобы записать первую покупку",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PoshtuchnoTheme(dynamicColor = false) {
        HomeScreen(
            monthTotalKopecks = 1_428_00,
            monthLimitKopecks = 30_000_00,
            recent = listOf(
                RecentExpense(1, "Пятёрочка", 42_350, "Продукты · сегодня"),
                RecentExpense(2, "Энергетик", 11_900, "Энергетики · вчера"),
            ),
            onAddClick = {},
        )
    }
}
