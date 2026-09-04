package io.github.artemagius.poshtuchno.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.ui.CategoryIcons
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

/**
 * Лист быстрого ввода: сумма своей клавиатурой, категория одним тапом, заметка по желанию.
 *
 * Своя клавиатура, а не системная, по двум причинам: сумма — единственное
 * обязательное поле, и кнопка «Готово» должна быть под большим пальцем.
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                label = { Text("Заметка") },
                placeholder = { Text("Необязательно") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            NumberPad(
                onDigit = onDigit,
                onSeparator = onSeparator,
                onBackspace = onBackspace,
            )

            Button(
                onClick = onSave,
                enabled = amount.kopecks > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun AmountDisplay(amount: AmountInput) {
    val shown = amount.display()
    Text(
        text = "$shown\u00A0\u20BD",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            // Экранный ридер должен произносить сумму целиком, а не по символам.
            .clearAndSetSemantics {
                contentDescription = "Сумма: ${Money.format(amount.kopecks)}"
            },
    )
}

@Composable
private fun CategoryChips(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    Icon(
                        imageVector = CategoryIcons[category.icon],
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
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
                    PadButton(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onDigit(key[0]) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton(
                label = ",",
                modifier = Modifier.weight(1f),
                onClick = onSeparator,
                description = "Запятая",
            )
            PadButton(
                label = "0",
                modifier = Modifier.weight(1f),
                onClick = { onDigit('0') },
            )
            FilledTonalButton(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Удалить цифру")
            }
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
        modifier = modifier.height(56.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            modifier = if (description != null) {
                Modifier.clearAndSetSemantics { contentDescription = description }
            } else {
                Modifier
            },
        )
    }
}

/** Тот же контент без bottom sheet — для превью и тестов вёрстки. */
@Composable
private fun QuickAddContentPreview(
    amount: AmountInput,
    categories: List<CategoryEntity>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        AmountDisplay(amount)
        CategoryChips(categories, categories.firstOrNull()?.id) {}
        NumberPad({}, {}, {})
        TextButton(onClick = {}) { Text("Сохранить") }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddPreview() {
    PoshtuchnoTheme(dynamicColor = false) {
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
