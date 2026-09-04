package io.github.artemagius.poshtuchno.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sortOrder, name")
    suspend fun getAll(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    /**
     * Категории, которыми пользовались недавно — они идут первыми в быстром вводе.
     * Порядок: сначала по числу использований за период, потом по свежести.
     */
    @Query(
        """
        SELECT c.* FROM category c
        JOIN purchase_item i ON i.categoryId = c.id
        JOIN purchase p ON p.id = i.purchaseId
        WHERE p.purchasedAt >= :since
        GROUP BY c.id
        ORDER BY COUNT(i.id) DESC, MAX(p.purchasedAt) DESC
        LIMIT :limit
        """,
    )
    fun observeFrequent(since: Long, limit: Int): Flow<List<CategoryEntity>>
}

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    /** Одна быстрая трата без разбора позиций: сумма + категория. */
    @Transaction
    suspend fun insertQuickExpense(
        purchasedAt: Long,
        totalKopecks: Long,
        categoryId: Long?,
        note: String?,
    ): Long {
        val purchaseId = insert(
            PurchaseEntity(
                purchasedAt = purchasedAt,
                totalKopecks = totalKopecks,
                source = PurchaseSource.MANUAL,
                note = note,
            ),
        )
        insertItems(
            listOf(
                PurchaseItemEntity(
                    purchaseId = purchaseId,
                    categoryId = categoryId,
                    rawName = note.orEmpty(),
                    unitPriceKopecks = totalKopecks,
                    sumKopecks = totalKopecks,
                ),
            ),
        )
        return purchaseId
    }

    /** Восстановление после удаления: покупка и её позиции возвращаются с теми же id. */
    @Transaction
    suspend fun restore(purchase: PurchaseEntity, items: List<PurchaseItemEntity>) {
        insert(purchase)
        insertItems(items)
    }

    @Query(
        """
        SELECT COALESCE(SUM(totalKopecks), 0) FROM purchase
        WHERE purchasedAt >= :fromInclusive AND purchasedAt < :toExclusive
        """,
    )
    fun observeTotalBetween(fromInclusive: Long, toExclusive: Long): Flow<Long>

    @Query(
        """
        SELECT p.id AS id,
               p.purchasedAt AS purchasedAt,
               p.totalKopecks AS totalKopecks,
               p.note AS note,
               s.name AS shopName,
               (SELECT COUNT(*) FROM purchase_item i WHERE i.purchaseId = p.id) AS itemCount,
               (
                   SELECT c.name FROM purchase_item i
                   LEFT JOIN category c ON c.id = i.categoryId
                   WHERE i.purchaseId = p.id
                   ORDER BY i.sumKopecks DESC
                   LIMIT 1
               ) AS topCategoryName
        FROM purchase p
        LEFT JOIN shop s ON s.id = p.shopId
        ORDER BY p.purchasedAt DESC, p.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<PurchaseListItem>>

    @Query("SELECT * FROM purchase WHERE id = :id")
    suspend fun getById(id: Long): PurchaseEntity?

    @Query("SELECT * FROM purchase_item WHERE purchaseId = :purchaseId")
    suspend fun itemsOf(purchaseId: Long): List<PurchaseItemEntity>

    @Delete
    suspend fun delete(purchase: PurchaseEntity)

    @Query("DELETE FROM purchase WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Защита от повторного импорта одного и того же фискального чека. */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM purchase
            WHERE fiscalNumber = :fn AND fiscalDocument = :fd AND fiscalSign = :fp
        )
        """,
    )
    suspend fun existsFiscal(fn: String, fd: String, fp: String): Boolean
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: ProductEntity): Long

    @Query("SELECT * FROM product WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query(
        """
        SELECT * FROM product
        WHERE canonicalName LIKE '%' || :query || '%'
        ORDER BY canonicalName
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int = 20): List<ProductEntity>

    /** Последняя известная цена товара — подставляется при вводе новой позиции. */
    @Query(
        """
        SELECT i.unitPriceKopecks FROM purchase_item i
        JOIN purchase p ON p.id = i.purchaseId
        WHERE i.productId = :productId
        ORDER BY p.purchasedAt DESC
        LIMIT 1
        """,
    )
    suspend fun lastPrice(productId: Long): Long?
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE scope = 'TOTAL' AND period = :period LIMIT 1")
    fun observeOverall(period: BudgetPeriod = BudgetPeriod.MONTH): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE scope = 'TOTAL' AND period = :period LIMIT 1")
    suspend fun getOverall(period: BudgetPeriod = BudgetPeriod.MONTH): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budget WHERE scope = 'TOTAL' AND period = :period")
    suspend fun clearOverall(period: BudgetPeriod = BudgetPeriod.MONTH)
}

@Dao
interface StatsDao {
    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName,
               COALESCE(SUM(i.sumKopecks), 0) AS totalKopecks
        FROM purchase_item i
        JOIN purchase p ON p.id = i.purchaseId
        LEFT JOIN category c ON c.id = i.categoryId
        WHERE p.purchasedAt >= :fromInclusive AND p.purchasedAt < :toExclusive
        GROUP BY c.id
        ORDER BY totalKopecks DESC
        """,
    )
    fun observeByCategory(fromInclusive: Long, toExclusive: Long): Flow<List<CategoryTotal>>

    /** Сумма по тегу — то, что показывает виджет-счётчик. */
    @Query(
        """
        SELECT COALESCE(SUM(i.sumKopecks), 0) FROM purchase_item i
        JOIN purchase p ON p.id = i.purchaseId
        JOIN product_tag pt ON pt.productId = i.productId
        WHERE pt.tagId = :tagId
          AND p.purchasedAt >= :fromInclusive AND p.purchasedAt < :toExclusive
        """,
    )
    fun observeTagTotal(tagId: Long, fromInclusive: Long, toExclusive: Long): Flow<Long>
}

data class CategoryTotal(
    val categoryId: Long?,
    val categoryName: String?,
    val totalKopecks: Long,
)

/** Проекция для списка на главном экране. */
data class PurchaseListItem(
    val id: Long,
    val purchasedAt: Long,
    val totalKopecks: Long,
    val note: String?,
    val shopName: String?,
    val itemCount: Int,
    val topCategoryName: String?,
)
