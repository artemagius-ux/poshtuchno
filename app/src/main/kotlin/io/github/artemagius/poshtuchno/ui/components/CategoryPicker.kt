package io.github.artemagius.poshtuchno.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.db.CategoryEntity

/**
 * Выбор категории сеткой с поиском.
 *
 * Горизонтальный список чипов не годился: категорий много, и пролистывать их
 * до нужной долго. Сетка показывает сразу восемь, а поиск отсекает остальное
 * за два-три символа.
 *
 * Раскладка через FlowRow, а не LazyVerticalGrid: сетка со своей прокруткой
 * внутри bottom sheet спорит с жестом перетаскивания листа. FlowRow не
 * прокручивается вовсе, высота ограничена числом строк.
 */
@Composable
fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    maxLines: Int = 2,
) {
    var query by remember { mutableStateOf("") }
    val searchable = categories.size > columns * maxLines

    // Выбранная категория всегда первая: она не должна выпадать из видимых строк.
    val filtered = remember(categories, query, selectedId) {
        val needle = query.trim().lowercase()
        val matched = if (needle.isEmpty()) {
            categories
        } else {
            categories.filter { it.name.lowercase().contains(needle) }
        }
        matched.sortedByDescending { it.id == selectedId }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (searchable) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Найти категорию") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }

        if (filtered.isEmpty()) {
            Text(
                text = "Ничего не нашлось",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            return@Column
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = columns,
            maxLines = maxLines,
        ) {
            filtered.forEach { category ->
                CategoryCell(
                    category = category,
                    selected = category.id == selectedId,
                    onClick = { onSelect(category.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape,
            ),
        ) {
            CategoryAvatar(
                icon = category.icon,
                container = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                size = 44.dp,
            )
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
