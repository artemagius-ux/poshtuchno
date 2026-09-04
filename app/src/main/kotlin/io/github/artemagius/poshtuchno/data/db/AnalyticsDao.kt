package io.github.artemagius.poshtuchno.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Запросы для вкладок «Сегодня» и «Аналитика».
 *
 * Группировка по дням делается в SQL через strftime с явным сдвигом таймзоны:
 * иначе сутки считались бы по UTC и вечерние траты попадали бы в следующий день.
 * Сдвиг передаётся строкой вида '+03:00' — SQLite умеет её применять напрямую.
 */
@Dao
interface AnalyticsDao {

    @Query(
        """
        SELECT strftime('%Y-%m-%d', p.purchasedAt / 1000, 'unixepoch', :tzOffset) AS day,
               SUM(p.totalKopecks) AS totalKopecks,
               COUNT(*) AS purchaseCount
        FROM purchase p
        WHERE p.purchasedAt >= :fromInclusive AND p.purchasedAt < :toExclusive
        GROUP BY day
        ORDER BY day
        """,
    )
    fun observeDailyTotals(fromInclusive: Long, toExclusive: Long, tzOffset: String): Flow<List<DailyTotal>>

    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS categoryName,
               c.icon AS categoryIcon,
               c.colorArgb AS categoryColorArgb,
               SUM(i.sumKopecks) AS totalKopecks,
               COUNT(i.id) AS itemCount
        FROM purchase_item i
        JOIN purchase p ON p.id = i.purchaseId
        LEFT JOIN category c ON c.id = i.categoryId
        WHERE p.purchasedAt >= :fromInclusive AND p.purchasedAt < :toExclusive
        GROUP BY c.id
        HAVING SUM(i.sumKopecks) > 0
        ORDER BY totalKopecks DESC
        """,
    )
    fun observeCategoryBreakdown(fromInclusive: Long, toExclusive: Long): Flow<List<CategoryBreakdown>>

    /** Самые крупные траты периода — что именно «съело» бюджет. */
    @Query(
        """
        SELECT p.id AS id,
               p.purchasedAt AS purchasedAt,
               p.totalKopecks AS totalKopecks,
               p.note AS note,
               s.name AS shopName,
               (
                   SELECT c.name FROM purchase_item i
                   LEFT JOIN category c ON c.id = i.categoryId
                   WHERE i.purchaseId = p.id
                   ORDER BY i.sumKopecks DESC LIMIT 1
               ) AS topCategoryName,
               (
                   SELECT c.icon FROM purchase_item i
                   LEFT JOIN category c ON c.id = i.categoryId
                   WHERE i.purchaseId = p.id
                   ORDER BY i.sumKopecks DESC LIMIT 1
               ) AS topCategoryIcon,
               (
                   SELECT c.colorArgb FROM purchase_item i
                   LEFT JOIN category c ON c.id = i.categoryId
                   WHERE i.purchaseId = p.id
                   ORDER BY i.sumKopecks DESC LIMIT 1
               ) AS topCategoryColorArgb,
               (SELECT COUNT(*) FROM purchase_item i WHERE i.purchaseId = p.id) AS itemCount
        FROM purchase p
        LEFT JOIN shop s ON s.id = p.shopId
        WHERE p.purchasedAt >= :fromInclusive AND p.purchasedAt < :toExclusive
        ORDER BY p.totalKopecks DESC
        LIMIT :limit
        """,
    )
    fun observeTopPurchases(fromInclusive: Long, toExclusive: Long, limit: Int): Flow<List<PurchaseListItem>>

    @Query(
        """
        SELECT COUNT(*) FROM purchase
        WHERE purchasedAt >= :fromInclusive AND purchasedAt < :toExclusive
        """,
    )
    fun observePurchaseCount(fromInclusive: Long, toExclusive: Long): Flow<Int>

    /** Дни, в которые вообще были траты — нужны для средней суммы за активный день. */
    @Query(
        """
        SELECT COUNT(DISTINCT strftime('%Y-%m-%d', purchasedAt / 1000, 'unixepoch', :tzOffset))
        FROM purchase
        WHERE purchasedAt >= :fromInclusive AND purchasedAt < :toExclusive
        """,
    )
    fun observeActiveDayCount(fromInclusive: Long, toExclusive: Long, tzOffset: String): Flow<Int>

    /** Первая покупка в базе — по ней понятно, с какого месяца показывать историю. */
    @Query("SELECT MIN(purchasedAt) FROM purchase")
    fun observeEarliestPurchase(): Flow<Long?>
}

data class DailyTotal(
    val day: String,
    val totalKopecks: Long,
    val purchaseCount: Int,
)

data class CategoryBreakdown(
    val categoryId: Long?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColorArgb: Int?,
    val totalKopecks: Long,
    val itemCount: Int,
)
