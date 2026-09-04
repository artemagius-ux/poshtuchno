package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.BudgetEntity
import io.github.artemagius.poshtuchno.data.db.BudgetPeriod
import io.github.artemagius.poshtuchno.data.db.BudgetScope
import io.github.artemagius.poshtuchno.data.db.CategoryBreakdown
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.data.db.DailyTotal
import io.github.artemagius.poshtuchno.data.db.GroupTotal
import io.github.artemagius.poshtuchno.data.db.PoshtuchnoDatabase
import io.github.artemagius.poshtuchno.data.db.PricePoint
import io.github.artemagius.poshtuchno.data.db.ProductEntity
import io.github.artemagius.poshtuchno.data.db.ProductGroupEntity
import io.github.artemagius.poshtuchno.data.db.ProductSuggestion
import io.github.artemagius.poshtuchno.data.db.ProductTotal
import io.github.artemagius.poshtuchno.data.db.PurchaseEntity
import io.github.artemagius.poshtuchno.data.db.PurchaseItemEntity
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import io.github.artemagius.poshtuchno.data.db.PurchaseSource
import kotlinx.coroutines.flow.Flow

/**
 * Единая точка доступа к данным для UI.
 *
 * Пока приложение маленькое, репозиторий один. Когда появятся чеки и позиции,
 * его имеет смысл разбить по областям (покупки / товары / статистика).
 */
class ExpenseRepository(private val db: PoshtuchnoDatabase) {

    private val matcher = ProductMatcher(db.productDao(), db.productGroupDao())

    // --- категории ---

    fun observeCategories(): Flow<List<CategoryEntity>> = db.categoryDao().observeAll()

    fun observeFrequentCategories(since: Long, limit: Int = 8): Flow<List<CategoryEntity>> =
        db.categoryDao().observeFrequent(since, limit)

    suspend fun upsertCategory(category: CategoryEntity): Long = db.categoryDao().upsert(category)

    suspend fun deleteCategory(id: Long) = db.categoryDao().deleteById(id)

    // --- траты ---

    fun observeTotal(range: LongRange): Flow<Long> =
        db.purchaseDao().observeTotalBetween(range.first, range.last + 1)

    fun observeRecent(limit: Int = 50): Flow<List<PurchaseListItem>> =
        db.purchaseDao().observeRecent(limit)

    fun observePurchases(range: LongRange): Flow<List<PurchaseListItem>> =
        db.purchaseDao().observeBetween(range.first, range.last + 1)

    suspend fun addQuickExpense(
        totalKopecks: Long,
        categoryId: Long?,
        note: String? = null,
        purchasedAt: Long = System.currentTimeMillis(),
    ): Long = db.purchaseDao().insertQuickExpense(
        purchasedAt = purchasedAt,
        totalKopecks = totalKopecks,
        categoryId = categoryId,
        note = note?.trim()?.takeIf { it.isNotEmpty() },
    )

    /**
     * Удаляет покупку и возвращает её вместе с позициями,
     * чтобы UI мог предложить отмену действия.
     */
    suspend fun deletePurchase(id: Long): DeletedPurchase? {
        val dao = db.purchaseDao()
        val purchase = dao.getById(id) ?: return null
        val items = dao.itemsOf(id)
        dao.delete(purchase)
        return DeletedPurchase(purchase, items)
    }

    suspend fun restorePurchase(deleted: DeletedPurchase) {
        db.purchaseDao().restore(deleted.purchase, deleted.items)
    }

    // --- лимит ---

    fun observeMonthlyLimit(): Flow<BudgetEntity?> =
        db.budgetDao().observeOverall(BudgetPeriod.MONTH)

    suspend fun setMonthlyLimit(limitKopecks: Long?) {
        val dao = db.budgetDao()
        if (limitKopecks == null || limitKopecks <= 0) {
            dao.clearOverall(BudgetPeriod.MONTH)
            return
        }
        val existing = dao.getOverall(BudgetPeriod.MONTH)
        dao.upsert(
            existing?.copy(limitKopecks = limitKopecks) ?: BudgetEntity(
                scope = BudgetScope.TOTAL,
                scopeId = null,
                period = BudgetPeriod.MONTH,
                limitKopecks = limitKopecks,
            ),
        )
    }

    // --- позиции покупки ---

    /**
     * Сохраняет покупку с разбором по позициям.
     *
     * Каждая позиция сопоставляется с карточкой товара: одинаковый товар,
     * написанный по-разному, склеивается в одну карточку. После сохранения
     * пересчитываются автоматические подкатегории.
     */
    suspend fun addItemizedPurchase(
        items: List<NewItem>,
        shopId: Long? = null,
        purchasedAt: Long = System.currentTimeMillis(),
        source: PurchaseSource = PurchaseSource.MANUAL,
        note: String? = null,
        fiscal: FiscalMarks? = null,
    ): Long {
        require(items.isNotEmpty()) { "purchase must have at least one item" }

        val total = items.sumOf { it.sumKopecks }
        val purchaseId = db.purchaseDao().insert(
            PurchaseEntity(
                shopId = shopId,
                purchasedAt = purchasedAt,
                totalKopecks = total,
                source = source,
                fiscalNumber = fiscal?.fn,
                fiscalDocument = fiscal?.fd,
                fiscalSign = fiscal?.fp,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )

        val rows = items.map { item ->
            val productId = matcher.resolve(item.name, item.categoryId)
            PurchaseItemEntity(
                purchaseId = purchaseId,
                productId = productId,
                categoryId = item.categoryId,
                rawName = item.name,
                quantityMilli = item.quantityMilli,
                unitPriceKopecks = item.unitPriceKopecks,
                sumKopecks = item.sumKopecks,
            )
        }
        db.purchaseDao().insertItems(rows)
        matcher.refreshGroups()
        return purchaseId
    }

    suspend fun productSuggestions(query: String, limit: Int = 12): List<ProductSuggestion> =
        db.productDao().suggestions(query.trim(), limit)

    suspend fun findProductByBarcode(barcode: String): ProductEntity? =
        db.productDao().findByBarcode(barcode)

    fun observePriceHistory(productId: Long): Flow<List<PricePoint>> =
        db.productDao().observePriceHistory(productId)

    suspend fun isFiscalDuplicate(fn: String, fd: String, fp: String): Boolean =
        db.purchaseDao().existsFiscal(fn, fd, fp)

    // --- автоматические подкатегории ---

    fun observeProductGroups(): Flow<List<ProductGroupEntity>> =
        db.productGroupDao().observeVisible()

    fun observeAllProductGroups(): Flow<List<ProductGroupEntity>> =
        db.productGroupDao().observeAll()

    fun observeGroupTotals(range: LongRange): Flow<List<GroupTotal>> =
        db.productGroupDao().observeGroupTotals(range.first, range.last + 1)

    fun observeGroupProducts(groupId: Long, range: LongRange): Flow<List<ProductTotal>> =
        db.productGroupDao().observeGroupProducts(groupId, range.first, range.last + 1)

    suspend fun renameGroup(id: Long, title: String) = db.productGroupDao().rename(id, title)

    suspend fun setGroupHidden(id: Long, hidden: Boolean) =
        db.productGroupDao().setHidden(id, hidden)

    suspend fun setGroupPinned(id: Long, pinned: Boolean) =
        db.productGroupDao().setPinned(id, pinned)

    suspend fun refreshProductGroups() = matcher.refreshGroups()

    // --- аналитика ---

    fun observeDailyTotals(range: LongRange, tzOffset: String): Flow<List<DailyTotal>> =
        db.analyticsDao().observeDailyTotals(range.first, range.last + 1, tzOffset)

    fun observeCategoryBreakdown(range: LongRange): Flow<List<CategoryBreakdown>> =
        db.analyticsDao().observeCategoryBreakdown(range.first, range.last + 1)

    fun observeTopPurchases(range: LongRange, limit: Int = 5): Flow<List<PurchaseListItem>> =
        db.analyticsDao().observeTopPurchases(range.first, range.last + 1, limit)

    fun observePurchaseCount(range: LongRange): Flow<Int> =
        db.analyticsDao().observePurchaseCount(range.first, range.last + 1)

    fun observeActiveDayCount(range: LongRange, tzOffset: String): Flow<Int> =
        db.analyticsDao().observeActiveDayCount(range.first, range.last + 1, tzOffset)

    fun observeEarliestPurchase(): Flow<Long?> = db.analyticsDao().observeEarliestPurchase()

    /** Заполняет базу стартовыми категориями, если она пуста. */
    suspend fun seedIfEmpty() {
        val dao = db.categoryDao()
        if (dao.count() == 0) dao.insertAll(DefaultCategories.all)
    }
}

data class DeletedPurchase(
    val purchase: PurchaseEntity,
    val items: List<PurchaseItemEntity>,
)

/** Позиция, которую пользователь добавляет в покупку. */
data class NewItem(
    val name: String,
    val unitPriceKopecks: Long,
    /** Количество в тысячных: 1 шт -> 1000, 0,25 кг -> 250. */
    val quantityMilli: Long = 1000,
    val categoryId: Long? = null,
) {
    val sumKopecks: Long get() = unitPriceKopecks * quantityMilli / 1000
}

/** Реквизиты фискального чека — по ним ловятся повторные импорты. */
data class FiscalMarks(
    val fn: String,
    val fd: String,
    val fp: String,
)
