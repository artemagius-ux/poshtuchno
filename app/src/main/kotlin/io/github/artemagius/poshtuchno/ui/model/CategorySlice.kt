package io.github.artemagius.poshtuchno.ui.model

import androidx.compose.ui.graphics.Color
import io.github.artemagius.poshtuchno.data.db.CategoryBreakdown

/** Категория с посчитанной суммой, долей и цветом для графика. */
data class CategorySlice(
    val categoryId: Long?,
    val name: String,
    val icon: String?,
    val color: Color,
    val totalKopecks: Long,
    val sharePercent: Int,
    val itemCount: Int,
)

/**
 * Цвет категории: если пользователь задал свой — берём его,
 * иначе назначаем из палитры по порядку убывания суммы.
 */
fun CategoryBreakdown.resolveColor(palette: List<Color>, index: Int): Color {
    val stored = categoryColorArgb
    return if (stored != null && stored != 0) Color(stored) else palette[index % palette.size]
}

fun CategoryBreakdown.displayName(): String = categoryName ?: "Без категории"
