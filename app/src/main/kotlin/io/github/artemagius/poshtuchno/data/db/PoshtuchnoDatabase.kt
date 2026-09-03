package io.github.artemagius.poshtuchno.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryEntity::class,
        TagEntity::class,
        ShopEntity::class,
        ProductEntity::class,
        ProductTagCrossRef::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        BudgetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PoshtuchnoDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun productDao(): ProductDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var instance: PoshtuchnoDatabase? = null

        fun get(context: Context): PoshtuchnoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PoshtuchnoDatabase::class.java,
                    "poshtuchno.db",
                ).build().also { instance = it }
            }
    }
}
