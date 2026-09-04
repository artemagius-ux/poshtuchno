package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.CategoryEntity

/**
 * Стартовый набор категорий. Создаётся один раз при первом запуске,
 * дальше пользователь правит его сам.
 *
 * icon — имя из [io.github.artemagius.poshtuchno.ui.CategoryIcons], а не ресурс,
 * чтобы набор иконок можно было менять без миграции базы.
 */
object DefaultCategories {

    val all: List<CategoryEntity> = listOf(
        CategoryEntity(name = "Продукты", icon = "cart", colorArgb = 0xFF43A047.toInt(), sortOrder = 0),
        CategoryEntity(name = "Кафе и еда вне дома", icon = "cafe", colorArgb = 0xFFEF6C00.toInt(), sortOrder = 1),
        CategoryEntity(name = "Транспорт", icon = "transport", colorArgb = 0xFF1E88E5.toInt(), sortOrder = 2),
        CategoryEntity(name = "Дом", icon = "home", colorArgb = 0xFF6D4C41.toInt(), sortOrder = 3),
        CategoryEntity(name = "Здоровье", icon = "health", colorArgb = 0xFFE53935.toInt(), sortOrder = 4),
        CategoryEntity(name = "Одежда", icon = "clothes", colorArgb = 0xFF8E24AA.toInt(), sortOrder = 5),
        CategoryEntity(name = "Развлечения", icon = "fun", colorArgb = 0xFFD81B60.toInt(), sortOrder = 6),
        CategoryEntity(name = "Связь и подписки", icon = "subscription", colorArgb = 0xFF00897B.toInt(), sortOrder = 7),
        CategoryEntity(name = "Прочее", icon = "other", colorArgb = 0xFF546E7A.toInt(), sortOrder = 8),
    )
}
