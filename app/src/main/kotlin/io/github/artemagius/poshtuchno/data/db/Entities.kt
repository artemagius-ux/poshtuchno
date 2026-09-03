package io.github.artemagius.poshtuchno.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Денежные суммы храним в копейках (Long), чтобы не терять точность на Double.
 * Количество — в тысячных доли единицы (0.250 кг -> 250), тоже целым числом.
 */

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null,
    @ColumnInfo(defaultValue = "0") val colorArgb: Int = 0,
    val parentId: Long? = null,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
)

@Entity(tableName = "tag", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0") val colorArgb: Int = 0,
)

@Entity(tableName = "shop", indices = [Index(value = ["name"])])
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inn: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Entity(
    tableName = "product",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["canonicalName"]),
        Index(value = ["defaultCategoryId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["defaultCategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val canonicalName: String,
    val brand: String? = null,
    val barcode: String? = null,
    /** "шт", "кг", "л" — свободная строка, единицы задаёт пользователь. */
    val unit: String? = null,
    val defaultCategoryId: Long? = null,
)

@Entity(
    tableName = "product_tag",
    primaryKeys = ["productId", "tagId"],
    indices = [Index(value = ["tagId"])],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProductTagCrossRef(
    val productId: Long,
    val tagId: Long,
)

enum class PurchaseSource { MANUAL, QR, OCR, BARCODE }

@Entity(
    tableName = "purchase",
    indices = [Index(value = ["purchasedAt"]), Index(value = ["shopId"])],
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long? = null,
    /** Epoch millis момента покупки. */
    val purchasedAt: Long,
    /** Итог по чеку в копейках. */
    val totalKopecks: Long,
    val source: PurchaseSource = PurchaseSource.MANUAL,
    /** Реквизиты фискального чека — нужны для защиты от повторного импорта. */
    val fiscalNumber: String? = null,
    val fiscalDocument: String? = null,
    val fiscalSign: String? = null,
    val photoUri: String? = null,
    val note: String? = null,
)

@Entity(
    tableName = "purchase_item",
    indices = [Index(value = ["purchaseId"]), Index(value = ["productId"]), Index(value = ["categoryId"])],
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long? = null,
    val categoryId: Long? = null,
    /** Название как в чеке — сохраняем, чтобы улучшать сопоставление позже. */
    val rawName: String,
    /** Количество в тысячных: 1 шт -> 1000, 0.25 кг -> 250. */
    @ColumnInfo(defaultValue = "1000") val quantityMilli: Long = 1000,
    val unitPriceKopecks: Long,
    val sumKopecks: Long,
    @ColumnInfo(defaultValue = "0") val discountKopecks: Long = 0,
)

enum class BudgetScope { TOTAL, CATEGORY, TAG }

enum class BudgetPeriod { WEEK, MONTH, YEAR }

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scope: BudgetScope,
    /** id категории или тега; null для scope=TOTAL. */
    val scopeId: Long? = null,
    val period: BudgetPeriod = BudgetPeriod.MONTH,
    val limitKopecks: Long,
)
