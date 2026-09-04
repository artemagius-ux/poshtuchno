package io.github.artemagius.poshtuchno.ui.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.Quantity
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.GroupTotal
import io.github.artemagius.poshtuchno.data.db.ProductTotal
import io.github.artemagius.poshtuchno.data.db.UnitKind
import io.github.artemagius.poshtuchno.ui.components.AppCard
import io.github.artemagius.poshtuchno.ui.components.EmptyPlaceholder
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

@Composable
fun ProductsScreen(
    state: ProductsUiState,
    onPeriodSelect: (ProductsPeriod) -> Unit,
    onToggleGroup: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onRenameGroup: (Long, String) -> Unit,
    onHideGroup: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf<GroupTotal?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ProductsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { onPeriodSelect(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ProductsPeriod.entries.size,
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

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Поиск: энергетик, молоко…") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.groups.isEmpty()) {
            item {
                EmptyPlaceholder(
                    illustration = R.drawable.il_empty_receipt,
                    title = when {
                        !state.loaded -> "Загрузка"
                        state.query.isNotBlank() -> "Ничего не нашлось"
                        else -> "Групп пока нет"
                    },
                    subtitle = if (state.query.isBlank() && state.loaded) {
                        "Добавь покупку по позициям. Когда один и тот же вид товара " +
                            "встретится дважды, приложение соберёт его в группу само."
                    } else {
                        null
                    },
                )
            }
        } else {
            items(state.groups, key = { it.groupId }) { group ->
                GroupCard(
                    group = group,
                    expanded = state.expandedGroupId == group.groupId,
                    products = if (state.expandedGroupId == group.groupId) {
                        state.expandedProducts
                    } else {
                        emptyList()
                    },
                    onClick = { onToggleGroup(group.groupId) },
                    onRename = { renaming = group },
                    onHide = { onHideGroup(group.groupId) },
                )
            }
        }
    }

    renaming?.let { group ->
        RenameDialog(
            initial = group.title,
            onDismiss = { renaming = null },
            onConfirm = { title ->
                onRenameGroup(group.groupId, title)
                renaming = null
            },
        )
    }
}

@Composable
private fun GroupCard(
    group: GroupTotal,
    expanded: Boolean,
    products: List<ProductTotal>,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = groupSummary(group),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = money(group.totalKopecks), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (expanded) "свернуть" else "подробнее",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                products.forEach { product ->
                    ProductRow(product)
                }

                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onRename) { Text("Переименовать") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onHide) { Text("Скрыть группу") }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: ProductTotal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = productSummary(product),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = money(product.totalKopecks), style = MaterialTheme.typography.bodyMedium)
    }
}

/** «4 товара · 12 шт · 5,3 л» — состав группы одной строкой. */
private fun groupSummary(group: GroupTotal): String = buildList {
    add("${group.productCount} ${productWord(group.productCount)}")
    add(Quantity.formatPieces(group.totalQuantityMilli))
    if (group.totalVolumeMl > 0) add(Quantity.formatVolume(group.totalVolumeMl))
    if (group.totalWeightG > 0) add(Quantity.formatWeight(group.totalWeightG))
}.joinToString(" · ")

/** «6 шт · 2,7 л · 89–119 ₽» — что именно и по какой цене. */
private fun productSummary(product: ProductTotal): String = buildList {
    val unitKind = when {
        product.volumeMl != null -> UnitKind.VOLUME
        product.weightG != null -> UnitKind.WEIGHT
        else -> UnitKind.PIECE
    }
    add(Quantity.formatPieces(product.totalQuantityMilli))

    val packs = product.totalQuantityMilli * (product.packCount ?: 1)
    when (unitKind) {
        UnitKind.VOLUME -> product.volumeMl?.let {
            add(Quantity.formatVolume(it.toLong() * packs / 1000))
        }
        UnitKind.WEIGHT -> product.weightG?.let {
            add(Quantity.formatWeight(it.toLong() * packs / 1000))
        }
        UnitKind.PIECE -> Unit
    }

    Quantity.formatUnitPrice(product.minPriceKopecks, product.volumeMl, product.weightG)
        ?.let { add(it) }
}.joinToString(" · ")

private fun productWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "товаров"
        mod10 == 1 -> "товар"
        mod10 in 2..4 -> "товара"
        else -> "товаров"
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Название группы") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun ProductsPreview() {
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
        ProductsScreen(
            state = ProductsUiState(
                groups = listOf(
                    GroupTotal(1, "Энергетик", 14, 158_600, 14_000, 6_286, 0, 3),
                    GroupTotal(2, "Молоко", 6, 53_940, 6_000, 6_000, 0, 2),
                ),
                expandedGroupId = 1,
                expandedProducts = listOf(
                    ProductTotal(1, "Энергетик BURN Original", "Burn", 449, null, null, 8, 8_000, 95_200, 11_900, 12_900),
                    ProductTotal(2, "Напиток ADRENALINE RUSH", "Adrenaline", 500, null, null, 4, 4_000, 43_600, 10_900, 10_900),
                    ProductTotal(3, "Энергетик FLASH UP", "Flash", 450, null, null, 2, 2_000, 19_800, 9_900, 9_900),
                ),
                loaded = true,
            ),
            onPeriodSelect = {},
            onToggleGroup = {},
            onQueryChange = {},
            onRenameGroup = { _, _ -> },
            onHideGroup = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
