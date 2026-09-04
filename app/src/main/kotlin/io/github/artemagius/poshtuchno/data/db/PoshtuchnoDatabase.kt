package io.github.artemagius.poshtuchno.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        TagEntity::class,
        ShopEntity::class,
        ProductEntity::class,
        ProductTagCrossRef::class,
        ProductGroupEntity::class,
        ProductGroupMember::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        BudgetEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PoshtuchnoDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun productDao(): ProductDao
    abstract fun productGroupDao(): ProductGroupDao
    abstract fun budgetDao(): BudgetDao
    abstract fun statsDao(): StatsDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        /**
         * v1 -> v2: у товара появились разобранные характеристики (объём, вес,
         * упаковка, ключ сопоставления) и таблицы автоматических подкатегорий.
         *
         * Миграция написана вручную, а не через destructive fallback: у человека
         * в базе уже лежат его траты, терять их при обновлении нельзя.
         */
        private val migration1to2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product ADD COLUMN matchKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE product ADD COLUMN tokens TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE product ADD COLUMN unitKind TEXT NOT NULL DEFAULT 'PIECE'")
                db.execSQL("ALTER TABLE product ADD COLUMN volumeMl INTEGER")
                db.execSQL("ALTER TABLE product ADD COLUMN weightG INTEGER")
                db.execSQL("ALTER TABLE product ADD COLUMN packCount INTEGER")
                // Уникальный индекс по matchKey нельзя создать сразу: у старых
                // записей ключ пустой и совпадёт. Заполняем ключ значением id,
                // приложение перезапишет его при первом сопоставлении.
                db.execSQL("UPDATE product SET matchKey = 'legacy-' || id")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_matchKey ON product (matchKey)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS product_group (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        token TEXT NOT NULL,
                        title TEXT NOT NULL,
                        hidden INTEGER NOT NULL DEFAULT 0,
                        pinned INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_group_token ON product_group (token)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS product_group_member (
                        groupId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        PRIMARY KEY (groupId, productId),
                        FOREIGN KEY (groupId) REFERENCES product_group(id) ON DELETE CASCADE,
                        FOREIGN KEY (productId) REFERENCES product(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_product_group_member_productId " +
                        "ON product_group_member (productId)",
                )
            }
        }

        @Volatile
        private var instance: PoshtuchnoDatabase? = null

        fun get(context: Context): PoshtuchnoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PoshtuchnoDatabase::class.java,
                    "poshtuchno.db",
                )
                    .addMigrations(migration1to2)
                    .build()
                    .also { instance = it }
            }
    }
}
