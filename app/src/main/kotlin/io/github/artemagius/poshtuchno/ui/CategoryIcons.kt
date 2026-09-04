package io.github.artemagius.poshtuchno.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Имена иконок категорий отвязаны от ресурсов: в базе лежит строка,
 * а сопоставление живёт здесь. Набор можно менять без миграции.
 *
 * Пока используем material-icons-core — в нём нет тематических иконок
 * вроде «продукты» или «медицина», поэтому подбираем ближайшие по смыслу.
 * Свои vector drawable добавим, когда дойдём до полировки.
 */
object CategoryIcons {

    private val byName: Map<String, ImageVector> = mapOf(
        "cart" to Icons.Default.ShoppingCart,
        "cafe" to Icons.Default.Star,
        "transport" to Icons.Default.Place,
        "home" to Icons.Default.Home,
        "health" to Icons.Default.Favorite,
        "clothes" to Icons.Default.Person,
        "fun" to Icons.Default.PlayArrow,
        "subscription" to Icons.Default.Phone,
        "calendar" to Icons.Default.DateRange,
        "tools" to Icons.Default.Build,
    )

    val fallback: ImageVector = Icons.Default.Star

    operator fun get(name: String?): ImageVector = byName[name] ?: fallback
}
