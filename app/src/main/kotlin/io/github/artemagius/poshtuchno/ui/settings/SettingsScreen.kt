package io.github.artemagius.poshtuchno.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.Money
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.ui.CategoryIcons
import io.github.artemagius.poshtuchno.ui.components.CategoryAvatar
import io.github.artemagius.poshtuchno.ui.components.SectionHeader
import io.github.artemagius.poshtuchno.ui.theme.ChartPalette
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLimitClick: () -> Unit,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Лимит на месяц") },
                    supportingContent = {
                        Text(
                            text = state.monthLimitKopecks
                                ?.let { "${Money.format(it)} · потрачено ${Money.format(state.monthTotalKopecks)}" }
                                ?: "Не задан",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onLimitClick),
                )
            }
        }

        item {
            SectionHeader(
                title = "Категории",
                action = {
                    TextButton(onClick = { creating = true }) { Text("Добавить") }
                },
            )
        }

        items(state.categories, key = { it.id }) { category ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        CategoryAvatar(
                            icon = category.icon,
                            tint = category.colorArgb.takeIf { it != 0 }?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        IconButton(onClick = { deleting = category }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "Удалить категорию",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    modifier = Modifier.clickable { editing = category },
                )
            }
        }

        item {
            Text(
                text = "Все данные хранятся только на этом устройстве. " +
                    "Приложение ничего не отправляет в сеть.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    val target = editing
    if (target != null || creating) {
        CategoryEditorDialog(
            category = target,
            onDismiss = {
                editing = null
                creating = false
            },
            onConfirm = { updated ->
                onSaveCategory(updated)
                editing = null
                creating = false
            },
        )
    }

    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить «${category.name}»?") },
            text = {
                Text("Траты останутся, но потеряют категорию. Отменить это будет нельзя.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category.id)
                        deleting = null
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onConfirm: (CategoryEntity) -> Unit,
) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember { mutableStateOf(category?.icon ?: CategoryIcons.names.first()) }
    var color by remember {
        mutableStateOf(category?.colorArgb?.takeIf { it != 0 } ?: ChartPalette.first().toArgb())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Новая категория" else "Категория") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                Text("Иконка", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryIcons.names.forEach { candidate ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = if (candidate == icon) 2.dp else 0.dp,
                                    color = if (candidate == icon) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape,
                                )
                                .clickable { icon = candidate },
                            contentAlignment = Alignment.Center,
                        ) {
                            CategoryAvatar(icon = candidate, tint = Color(color), size = 36.dp)
                        }
                    }
                }

                Text("Цвет", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChartPalette.forEach { candidate ->
                        val argb = candidate.toArgb()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(candidate, CircleShape)
                                .border(
                                    width = if (argb == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { color = argb },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        category?.copy(name = name.trim(), icon = icon, colorArgb = color)
                            ?: CategoryEntity(name = name.trim(), icon = icon, colorArgb = color),
                    )
                },
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    PoshtuchnoTheme(dynamicColor = false) {
        SettingsScreen(
            state = SettingsUiState(
                monthLimitKopecks = 3_000_000,
                monthTotalKopecks = 1_782_000,
                categories = listOf(
                    CategoryEntity(1, "Продукты", "cart", 0xFF43A047.toInt()),
                    CategoryEntity(2, "Кафе", "cafe", 0xFFEF6C00.toInt()),
                ),
            ),
            onLimitClick = {},
            onSaveCategory = {},
            onDeleteCategory = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
