package io.github.artemagius.poshtuchno.ui.additem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.Quantity
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.data.db.ProductSuggestion
import io.github.artemagius.poshtuchno.ui.components.AppCard
import io.github.artemagius.poshtuchno.ui.components.CategoryPicker
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

/**
 * Экран покупки по позициям. Открывается из «+ Чек» или после сканирования QR.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsScreen(
    state: AddItemsState,
    suggestions: List<ProductSuggestion>,
    onClose: () -> Unit,
    onScanClick: () -> Unit,
    onAddDraft: () -> Unit,
    onRemoveDraft: (Long) -> Unit,
    onEditDraft: (Long?) -> Unit,
    onNameChange: (Long, String) -> Unit,
    onPriceChange: (Long, Long) -> Unit,
    onQuantityIncrement: (Long) -> Unit,
    onQuantityDecrement: (Long) -> Unit,
    onCategoryChange: (Long, Long) -> Unit,
    onApplySuggestion: (Long, ProductSuggestion) -> Unit,
    onAddRemainder: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Покупка по позициям") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_left),
                            contentDescription = "Закрыть",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_scan),
                            contentDescription = "Сканировать чек",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            SaveBar(state = state, onSave = onSave)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.receiptTotalKopecks != null) {
                item { ReceiptCard(state, onAddRemainder) }
            }

            items(state.drafts, key = { it.id }) { draft ->
                DraftCard(
                    draft = draft,
                    expanded = state.editingId == draft.id,
                    categories = state.categories,
                    suggestions = if (state.editingId == draft.id) suggestions else emptyList(),
                    onExpand = { onEditDraft(draft.id) },
                    onCollapse = { onEditDraft(null) },
                    onRemove = { onRemoveDraft(draft.id) },
                    onNameChange = { onNameChange(draft.id, it) },
                    onPriceChange = { onPriceChange(draft.id, it) },
                    onIncrement = { onQuantityIncrement(draft.id) },
                    onDecrement = { onQuantityDecrement(draft.id) },
                    onCategoryChange = { onCategoryChange(draft.id, it) },
                    onApplySuggestion = { onApplySuggestion(draft.id, it) },
                )
            }

            item {
                FilledTonalButton(
                    onClick = onAddDraft,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ещё позиция")
                }
            }
        }
    }
}

@Composable
private fun ReceiptCard(state: AddItemsState, onAddRemainder: () -> Unit) {
    val total = state.receiptTotalKopecks ?: return
    val remainder = state.remainderKopecks ?: 0

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Чек отсканирован",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = money(total), style = MaterialTheme.typography.displaySmall)

        if (state.duplicateReceipt) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Этот чек уже добавлен раньше",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    remainder > 0 -> "Осталось разложить ${money(remainder)}"
                    remainder < 0 -> "Позиции превышают чек на ${money(-remainder)}"
                    else -> "Всё разложено"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (remainder < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            if (remainder > 0) {
                TextButton(onClick = onAddRemainder) { Text("В «Прочее»") }
            }
        }
    }
}

@Composable
private fun DraftCard(
    draft: ItemDraft,
    expanded: Boolean,
    categories: List<CategoryEntity>,
    suggestions: List<ProductSuggestion>,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onRemove: () -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (Long) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onCategoryChange: (Long) -> Unit,
    onApplySuggestion: (ProductSuggestion) -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        if (!expanded) {
            CollapsedDraft(draft = draft, onClick = onExpand, onRemove = onRemove)
            return@AppCard
        }

        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            placeholder = { Text("Название товара") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(suggestions, key = { it.id }) { suggestion ->
                    SuggestionChip(
                        onClick = { onApplySuggestion(suggestion) },
                        shape = MaterialTheme.shapes.small,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        border = null,
                        label = {
                            Column {
                                Text(
                                    text = suggestion.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val hint = buildList {
                                    suggestion.lastPriceKopecks?.let { add(Money.format(it)) }
                                    if (suggestion.timesBought > 1) add("×${suggestion.timesBought}")
                                }.joinToString(" · ")
                                if (hint.isNotEmpty()) {
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PriceField(
                priceKopecks = draft.priceKopecks,
                onPriceChange = onPriceChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            QuantityStepper(
                quantityMilli = draft.quantityMilli,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
            )
        }

        val change = draft.priceChangePercent()
        if (change != null && change != 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (change > 0) {
                    "Подорожало на $change% — было ${money(draft.previousPriceKopecks!!)}"
                } else {
                    "Подешевело на ${-change}% — было ${money(draft.previousPriceKopecks!!)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (change > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        CategoryPicker(
            categories = categories,
            selectedId = draft.categoryId,
            onSelect = onCategoryChange,
            columns = 4,
            maxLines = 2,
        )

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRemove) { Text("Убрать") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = money(draft.sumKopecks),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onCollapse, enabled = draft.isValid) { Text("Готово") }
            }
        }
    }
}

@Composable
private fun CollapsedDraft(draft: ItemDraft, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = draft.name.ifBlank { "Без названия" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(money(draft.priceKopecks))
                    if (draft.quantityMilli != 1000L) {
                        append(" × ")
                        append(Quantity.formatCount(draft.quantityMilli))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = money(draft.sumKopecks), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Убрать позицию",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PriceField(
    priceKopecks: Long,
    onPriceChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Показываем «сырой» текст, а не форматированный: иначе курсор прыгает
    // при каждом вводе символа из-за пересборки строки с пробелами.
    val text = if (priceKopecks == 0L) "" else Money.format(priceKopecks, withCurrency = false)
    OutlinedTextField(
        value = text,
        onValueChange = { input -> Money.parse(input)?.let(onPriceChange) ?: if (input.isBlank()) onPriceChange(0) else Unit },
        placeholder = { Text("Цена") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier,
    )
}

@Composable
private fun QuantityStepper(
    quantityMilli: Long,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrement, enabled = quantityMilli > 1000) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = Quantity.formatCount(quantityMilli),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(28.dp),
        )
        IconButton(onClick = onIncrement) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SaveBar(state: AddItemsState, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Итого",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = money(state.itemsTotalKopecks),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSave,
                enabled = state.canSave,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Сохранить покупку", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddItemsPreview() {
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
        AddItemsScreen(
            state = AddItemsState(
                drafts = listOf(
                    ItemDraft(id = 1, name = "Энергетик BURN 0,449л", priceKopecks = 11_900, quantityMilli = 2000),
                    ItemDraft(id = 2, name = "Молоко 1л", priceKopecks = 8_990, previousPriceKopecks = 7_990),
                ),
                editingId = 2,
                categories = listOf(
                    CategoryEntity(1, "Продукты", "cart"),
                    CategoryEntity(2, "Кафе", "cafe"),
                    CategoryEntity(3, "Транспорт", "transport"),
                    CategoryEntity(4, "Дом", "home"),
                ),
                receiptTotalKopecks = 45_000,
            ),
            suggestions = emptyList(),
            onClose = {},
            onScanClick = {},
            onAddDraft = {},
            onRemoveDraft = {},
            onEditDraft = {},
            onNameChange = { _, _ -> },
            onPriceChange = { _, _ -> },
            onQuantityIncrement = {},
            onQuantityDecrement = {},
            onCategoryChange = { _, _ -> },
            onApplySuggestion = { _, _ -> },
            onAddRemainder = {},
            onSave = {},
        )
    }
}
