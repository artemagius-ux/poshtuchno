package io.github.artemagius.poshtuchno.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Автоматические подкатегории: слова, повторяющиеся у разных товаров.
 */
@Dao
interface ProductGroupDao {

    @Query("SELECT * FROM product_group WHERE hidden = 0 ORDER BY pinned DESC, title")
    fun observeVisible(): Flow<List<ProductGroupEntity>>

    @Query("SELECT * FROM product_group ORDER BY hidden, pinned DESC, title")
    fun observeAll(): Flow<List<ProductGroupEntity>>

    @Query("SELECT * FROM product_group WHERE token = :token LIMIT 1")
    suspend fun findByToken(token: String): ProductGroupEntity?

    @Upsert
    suspend fun upsert(group: ProductGroupEntity): Long

    @Query("UPDATE product_group SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE product_group SET hidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("UPDATE product_group SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembers(members: List<ProductGroupMember>)

    @Query("DELETE FROM product_group_member WHERE groupId = :groupId")
    suspend fun clearMembers(groupId: Long)

    /** Пересоздаёт состав группы: список товаров мог измениться. */
    @Transaction
    suspend fun replaceMembers(groupId: Long, productIds: List<Long>) {
        clearMembers(groupId)
        addMembers(productIds.map { ProductGroupMember(groupId = groupId, productId = it) })
    }

    /** Группы, в которые входит товар — показываются на карточке товара. */
    @Query(
        """
        SELECT g.* FROM product_group g
        JOIN product_group_member m ON m.groupId = g.id
        WHERE m.productId = :productId AND g.hidden = 0
        ORDER BY g.title
        """,
    )
    suspend fun groupsOf(productId: Long): List<ProductGroupEntity>

    /**
     * Сводка по группе за период: сколько раз брали, на сколько денег,
     * сколько всего литров и граммов.
     *
     * Литры и граммы считаются с учётом упаковки и количества: 2 упаковки
     * по 6 банок 0,5 л дают 6 литров, а не 0,5.
     */
    @Query(
        """
        SELECT g.id AS groupId,
               g.title AS title,
               COUNT(i.id) AS purchaseCount,
               COALESCE(SUM(i.sumKopecks), 0) AS totalKopecks,
               COALESCE(SUM(i.quantityMilli), 0) AS totalQuantityMilli,
               COALESCE(SUM(
                   CASE WHEN p.volumeMl IS NOT NULL
                        THEN p.volumeMl * COALESCE(p.packCount, 1) * i.quantityMilli / 1000
                   END
               ), 0) AS totalVolumeMl,
               COALESCE(SUM(
                   CASE WHEN p.weightG IS NOT NULL
                        THEN p.weightG * COALESCE(p.packCount, 1) * i.quantityMilli / 1000
                   END
               ), 0) AS totalWeightG,
               COUNT(DISTINCT p.id) AS productCount
        FROM product_group g
        JOIN product_group_member m ON m.groupId = g.id
        JOIN product p ON p.id = m.productId
        JOIN purchase_item i ON i.productId = p.id
        JOIN purchase pur ON pur.id = i.purchaseId
        WHERE g.hidden = 0
          AND pur.purchasedAt >= :fromInclusive AND pur.purchasedAt < :toExclusive
        GROUP BY g.id
        HAVING COUNT(i.id) > 0
        ORDER BY totalKopecks DESC
        """,
    )
    fun observeGroupTotals(fromInclusive: Long, toExclusive: Long): Flow<List<GroupTotal>>

    /** Товары внутри группы с их количествами — «BURN 4 шт, Adrenaline 2 шт». */
    @Query(
        """
        SELECT p.id AS productId,
               p.canonicalName AS name,
               p.brand AS brand,
               p.volumeMl AS volumeMl,
               p.weightG AS weightG,
               p.packCount AS packCount,
               COUNT(i.id) AS purchaseCount,
               COALESCE(SUM(i.quantityMilli), 0) AS totalQuantityMilli,
               COALESCE(SUM(i.sumKopecks), 0) AS totalKopecks,
               MIN(i.unitPriceKopecks) AS minPriceKopecks,
               MAX(i.unitPriceKopecks) AS maxPriceKopecks
        FROM product_group_member m
        JOIN product p ON p.id = m.productId
        JOIN purchase_item i ON i.productId = p.id
        JOIN purchase pur ON pur.id = i.purchaseId
        WHERE m.groupId = :groupId
          AND pur.purchasedAt >= :fromInclusive AND pur.purchasedAt < :toExclusive
        GROUP BY p.id
        ORDER BY totalKopecks DESC
        """,
    )
    fun observeGroupProducts(
        groupId: Long,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<ProductTotal>>
}

data class GroupTotal(
    val groupId: Long,
    val title: String,
    val purchaseCount: Int,
    val totalKopecks: Long,
    val totalQuantityMilli: Long,
    val totalVolumeMl: Long,
    val totalWeightG: Long,
    val productCount: Int,
)

data class ProductTotal(
    val productId: Long,
    val name: String,
    val brand: String?,
    val volumeMl: Int?,
    val weightG: Int?,
    val packCount: Int?,
    val purchaseCount: Int,
    val totalQuantityMilli: Long,
    val totalKopecks: Long,
    val minPriceKopecks: Long,
    val maxPriceKopecks: Long,
)
