package io.github.artemagius.poshtuchno.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun purchaseSourceToString(value: PurchaseSource): String = value.name

    @TypeConverter
    fun stringToPurchaseSource(value: String): PurchaseSource =
        runCatching { PurchaseSource.valueOf(value) }.getOrDefault(PurchaseSource.MANUAL)

    @TypeConverter
    fun budgetScopeToString(value: BudgetScope): String = value.name

    @TypeConverter
    fun stringToBudgetScope(value: String): BudgetScope =
        runCatching { BudgetScope.valueOf(value) }.getOrDefault(BudgetScope.TOTAL)

    @TypeConverter
    fun budgetPeriodToString(value: BudgetPeriod): String = value.name

    @TypeConverter
    fun stringToBudgetPeriod(value: String): BudgetPeriod =
        runCatching { BudgetPeriod.valueOf(value) }.getOrDefault(BudgetPeriod.MONTH)

    @TypeConverter
    fun unitKindToString(value: UnitKind): String = value.name

    @TypeConverter
    fun stringToUnitKind(value: String): UnitKind =
        runCatching { UnitKind.valueOf(value) }.getOrDefault(UnitKind.PIECE)
}
