package io.github.artemagius.poshtuchno.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.R
import io.github.artemagius.poshtuchno.data.AppSettings
import io.github.artemagius.poshtuchno.data.ThemeMode
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.ui.CategoryIcons
import io.github.artemagius.poshtuchno.ui.components.AppCard
import io.github.artemagius.poshtuchno.ui.components.CategoryAvatar
import io.github.artemagius.poshtuchno.ui.components.SectionHeader
import io.github.artemagius.poshtuchno.ui.money
import io.github.artemagius.poshtuchno.ui.theme.PoshtuchnoTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLimitClick: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onCloseAfterSaveChange: (Boolean) -> Unit,
    onShowKopecksChange: (Boolean) -> Unit,
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
            top = 4.dp + contentPadding.calculateTopPadding(),
            bottom = 24.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader(title = "Внешний вид") }

        item {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Тема", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.settings.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            label = { Text(mode.label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.settings.themeMode) {
                        ThemeMode.Auto -> "Следует настройке устройства"
                        ThemeMode.Light -> "Всегда светлая"
                        ThemeMode.Dark -> "Всегда тёмная"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                SettingSwitch(
                    title = "Цвета из обоев",
                    subtitle = "Взять акцент из системной палитры вместо фирменного",
                    checked = state.settings.dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                )
            }
        }

        item { SectionHeader(title = "Ввод трат") }

        item {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onLimitClick)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Лимит на месяц", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        val limit = state.monthLimitKopecks
                        Text(
                            text = if (limit != null) {
                                "${money(limit)} · потрачено ${money(state.monthTotalKopecks)}"
                            } else {
                                "Не задан"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                SettingSwitch(
                    title = "Закрывать после сохранения",
                    subtitle = "Записал трату — приложение сразу свернулось",
                    checked = state.settings.closeAfterSave,
                    onCheckedChange = onCloseAfterSaveChange,
                )

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                SettingSwitch(
                    title = "Показывать копейки",
                    subtitle = "Иначе суммы округляются до рубля",
                    checked = state.settings.showKopecks,
                    onCheckedChange = onShowKopecksChange,
                )
            }
        }

        item {
            SectionHeader(
                title = "Категории",
                action = { TextButton(onClick = { creating = true }) { Text("Добавить") } },
            )
        }

        items(state.categories, key = { it.id }) { category ->
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = category },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryAvatar(icon = category.icon, size = 40.dp)
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .weight(1f),
                    )
                    IconButton(onClick = { deleting = category }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Удалить категорию",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Все данные хранятся только на этом устройстве. " +
                    "Приложение ничего не отправляет в сеть.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, start = 4.dp, end = 4.dp),
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
            text = { Text("Траты останутся, но потеряют категорию. Отменить это будет нельзя.") },
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
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                Text("Иконка", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoryIcons.names.forEach { candidate ->
                        val selected = candidate == icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape,
                                )
                                .clickable { icon = candidate },
                            contentAlignment = Alignment.Center,
                        ) {
                            CategoryAvatar(icon = candidate, size = 38.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        category?.copy(name = name.trim(), icon = icon)
                            ?: CategoryEntity(name = name.trim(), icon = icon),
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
    PoshtuchnoTheme(themeMode = ThemeMode.Light) {
        SettingsScreen(
            state = SettingsUiState(
                settings = AppSettings(),
                monthLimitKopecks = 3_000_000,
                monthTotalKopecks = 1_782_000,
                categories = listOf(
                    CategoryEntity(1, "Продукты", "cart"),
                    CategoryEntity(2, "Кафе", "cafe"),
                ),
            ),
            onLimitClick = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onCloseAfterSaveChange = {},
            onShowKopecksChange = {},
            onSaveCategory = {},
            onDeleteCategory = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}
