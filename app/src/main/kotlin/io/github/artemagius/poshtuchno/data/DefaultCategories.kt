package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.CategoryEntity

/**
 * Стартовый набор категорий. Создаётся один раз при первом запуске,
 * дальше пользователь правит его сам.
 *
 * icon — имя из [io.github.artemagius.poshtuchno.ui.CategoryIcons], а не ресурс,
 * чтобы набор иконок можно было менять без миграции базы. Цвет не задаём:
 * палитра приложения единая, категории различаются иконкой.
 */
object DefaultCategories {

    val all: List<CategoryEntity> = listOf(
        CategoryEntity(name = "Продукты", icon = "cart", sortOrder = 0),
        CategoryEntity(name = "Кафе", icon = "cafe", sortOrder = 1),
        CategoryEntity(name = "Транспорт", icon = "transport", sortOrder = 2),
        CategoryEntity(name = "Дом", icon = "home", sortOrder = 3),
        CategoryEntity(name = "Здоровье", icon = "health", sortOrder = 4),
        CategoryEntity(name = "Одежда", icon = "clothes", sortOrder = 5),
        CategoryEntity(name = "Развлечения", icon = "fun", sortOrder = 6),
        CategoryEntity(name = "Подписки", icon = "subscription", sortOrder = 7),
        CategoryEntity(name = "Прочее", icon = "other", sortOrder = 8),
    )
}
