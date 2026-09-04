package io.github.artemagius.poshtuchno.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.ui.components.CategoryPicker
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

/**
 * Лист быстрого ввода: сумма своей клавиатурой, категория одним тапом, заметка по желанию.
 *
 * Содержимое умышленно не обёрнуто в verticalScroll, и выбор категории тоже без
 * своей прокрутки: любая вертикальная прокрутка внутри bottom sheet конфликтует
 * с его жестом перетаскивания, и лист начинает дёргаться вверх-вниз. Поэтому
 * набор помещается целиком, а категории показываются сеткой с поиском.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    sheetState: SheetState,
    amount: AmountInput,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    note: String,
    onDigit: (Char) -> Unit,
    onSeparator: () -> Unit,
    onBackspace: () -> Unit,
    onCategorySelect: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AmountDisplay(amount)

            if (categories.isNotEmpty()) {
                CategoryChips(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelect = onCategorySelect,
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text("Заметка (необязательно)") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            NumberPad(onDigit = onDigit, onSeparator = onSeparator, onBackspace = onBackspace)

            Button(
                onClick = onSave,
                enabled = amount.kopecks > 0,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Сохранить", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AmountDisplay(amount: AmountInput) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Сумма",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${amount.display()}\u00A0\u20BD",
            style = MaterialTheme.typography.displayMedium,
            color = if (amount.hasValue) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            },
            // Экранный ридер должен произносить сумму целиком, а не по символам.
            modifier = Modifier
                .padding(top = 2.dp)
                .clearAndSetSemantics {
                    contentDescription = "Сумма: ${Money.format(amount.kopecks)}"
                },
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long) -> Unit,
) {
    CategoryPicker(
        categories = categories,
        selectedId = selectedCategoryId,
        onSelect = onCategorySelect,
        columns = 4,
        maxLines = 2,
    )
}

@Composable
private fun NumberPad(
    onDigit: (Char) -> Unit,
    onSeparator: () -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    PadButton(label = key, modifier = Modifier.weight(1f)) { onDigit(key[0]) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton(
                label = ",",
                modifier = Modifier.weight(1f),
                description = "Запятая",
                onClick = onSeparator,
            )
            PadButton(label = "0", modifier = Modifier.weight(1f)) { onDigit('0') }
            PadButton(
                label = "⌫",
                modifier = Modifier.weight(1f),
                description = "Удалить цифру",
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun PadButton(
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .height(52.dp)
            .then(
                if (description != null) {
                    Modifier.clearAndSetSemantics { contentDescription = description }
                } else {
                    Modifier
                },
            ),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun QuickAddContentPreview(amount: AmountInput, categories: List<CategoryEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AmountDisplay(amount)
        CategoryChips(categories, categories.firstOrNull()?.id) {}
        NumberPad({}, {}, {})
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddPreview() {
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
        QuickAddContentPreview(
            amount = AmountInput(digits = "1428", fraction = "50"),
            categories = listOf(
                CategoryEntity(id = 1, name = "Продукты", icon = "cart"),
                CategoryEntity(id = 2, name = "Транспорт", icon = "transport"),
                CategoryEntity(id = 3, name = "Кафе", icon = "cafe"),
            ),
        )
    }
}
