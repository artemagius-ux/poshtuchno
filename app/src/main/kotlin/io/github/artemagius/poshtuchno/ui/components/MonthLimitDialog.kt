package io.github.artemagius.poshtuchno.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.artemagius.poshtuchno.data.Money

/** Диалог лимита на месяц. Пустое поле означает, что лимита нет. */
@Composable
fun MonthLimitDialog(
    currentLimitKopecks: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    var text by remember {
        mutableStateOf(
            currentLimitKopecks
                ?.takeIf { it > 0 }
                ?.let { Money.format(it, withCurrency = false, showKopecks = true) }
                .orEmpty(),
        )
    }
    val parsed = Money.parse(text)
    val isValid = text.isBlank() || (parsed != null && parsed > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Лимит на месяц") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Сумма") },
                    placeholder = { Text("Например, 30 000") },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = {
                        Text(
                            when {
                                !isValid -> "Введите сумму больше нуля"
                                text.isBlank() -> "Пустое поле — лимит выключен"
                                else -> "Прогресс появится на вкладке «Сегодня»"
                            },
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (text.isBlank()) null else parsed) },
                enabled = isValid,
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
