package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.BudgetEntity
import io.github.artemagius.poshtuchno.data.db.BudgetPeriod
import io.github.artemagius.poshtuchno.data.db.BudgetScope
import io.github.artemagius.poshtuchno.data.db.CategoryBreakdown
import io.github.artemagius.poshtuchno.data.db.CategoryEntity
import io.github.artemagius.poshtuchno.data.db.DailyTotal
import io.github.artemagius.poshtuchno.data.db.PoshtuchnoDatabase
import io.github.artemagius.poshtuchno.data.db.PurchaseEntity
import io.github.artemagius.poshtuchno.data.db.PurchaseItemEntity
import io.github.artemagius.poshtuchno.data.db.PurchaseListItem
import kotlinx.coroutines.flow.Flow

/**
 * Единая точка доступа к данным для UI.
 *
 * Пока приложение маленькое, репозиторий один. Когда появятся чеки и позиции,
 * его имеет смысл разбить по областям (покупки / товары / статистика).
 */
class ExpenseRepository(private val db: PoshtuchnoDatabase) {

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
