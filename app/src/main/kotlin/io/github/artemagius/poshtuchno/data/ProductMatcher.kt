package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.ProductDao
import io.github.artemagius.poshtuchno.data.db.ProductEntity
import io.github.artemagius.poshtuchno.data.db.ProductGroupDao
import io.github.artemagius.poshtuchno.data.db.ProductGroupEntity
import io.github.artemagius.poshtuchno.data.db.UnitKind
import io.github.artemagius.poshtuchno.data.product.ProductGrouping
import io.github.artemagius.poshtuchno.data.product.ProductNameParser

/**
 * Сопоставление названий из чека с карточками товаров и поддержка автогрупп.
 *
 * Ключевая логика приложения: одинаковый товар, написанный в разных чеках
 * по-разному, должен становиться одной карточкой, а повторяющиеся слова —
 * подкатегорией. Иначе после сканирования чеков список превратится в кашу
 * из почти одинаковых строк.
 */
class ProductMatcher(
    private val productDao: ProductDao,
    private val groupDao: ProductGroupDao,
) {

    /**
     * Находит существующий товар по названию или создаёт новый.
     * Возвращает id карточки, к которой надо привязать позицию чека.
     */
    suspend fun resolve(rawName: String, categoryId: Long? = null): Long {
        val parsed = ProductNameParser.parse(rawName)
        val key = ProductGrouping.matchKey(parsed)

        if (key.isNotEmpty()) {
            productDao.findByMatchKey(key)?.let { existing ->
                // Категорию дописываем, если у карточки её ещё нет.
                if (existing.defaultCategoryId == null && categoryId != null) {
                    productDao.update(existing.copy(defaultCategoryId = categoryId))
                }
                return existing.id
            }
        }

        val entity = ProductEntity(
            canonicalName = parsed.cleanName.ifBlank { rawName.trim() },
            brand = parsed.brand,
            matchKey = key.ifEmpty { "raw-${rawName.trim().lowercase()}" },
            tokens = parsed.tokens.joinToString(" "),
            unitKind = when (parsed.unitKind) {
                ProductNameParser.UnitKind.Volume -> UnitKind.VOLUME
                ProductNameParser.UnitKind.Weight -> UnitKind.WEIGHT
                ProductNameParser.UnitKind.Piece -> UnitKind.PIECE
            },
            volumeMl = parsed.volumeMl,
            weightG = parsed.weightG,
            packCount = parsed.packCount,
            defaultCategoryId = categoryId,
        )

        val id = productDao.insert(entity)
        // insert с IGNORE вернёт -1, если ключ уже занят: значит товар успели
        // создать параллельно, забираем существующий.
        return if (id > 0) id else productDao.findByMatchKey(entity.matchKey)?.id ?: id
    }

    /**
     * Пересчитывает автоматические подкатегории по всей базе товаров.
     *
     * Вызывается после добавления покупки: новый товар может как попасть
     * в существующую группу, так и создать новую вместе с уже имеющимися.
     * Ручные правки не теряются — переименование и скрытие живут в самой
     * записи группы, а обновляется только состав.
     */
    suspend fun refreshGroups(minProducts: Int = 2) {
        val products = productDao.getAll()
        if (products.isEmpty()) return

        val infos = products.map { product ->
            ProductGrouping.ProductInfo(
                id = product.id,
                name = product.canonicalName,
                tokens = product.tokens.split(' ').filter { it.isNotBlank() },
            )
        }

        ProductGrouping.suggest(infos, minProducts).forEach { suggestion ->
            val existing = groupDao.findByToken(suggestion.token)
            val groupId = if (existing != null) {
                existing.id
            } else {
                groupDao.upsert(
                    ProductGroupEntity(token = suggestion.token, title = suggestion.title),
                )
            }
            groupDao.replaceMembers(groupId, suggestion.productIds)
        }
    }
}
