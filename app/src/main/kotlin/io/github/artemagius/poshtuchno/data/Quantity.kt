package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.UnitKind

/**
 * Человеческое представление количеств: «6 шт», «2,5 л», «450 г».
 *
 * Количество внутри хранится в тысячных, объём в миллилитрах, вес в граммах —
 * форматирование в одном месте, чтобы единицы не разъезжались по экранам.
 */
object Quantity {

    /** Количество единиц: 1000 -> «1», 2500 -> «2,5». */
    fun formatCount(quantityMilli: Long): String {
        val whole = quantityMilli / 1000
        val fraction = quantityMilli % 1000
        return if (fraction == 0L) {
            whole.toString()
        } else {
            val trimmed = fraction.toString().padStart(3, '0').trimEnd('0')
            "$whole,$trimmed"
        }
    }

    fun formatPieces(quantityMilli: Long): String = "${formatCount(quantityMilli)} шт"

    /** Объём: 500 -> «500 мл», 1500 -> «1,5 л». */
    fun formatVolume(ml: Long): String = when {
        ml <= 0 -> "0 мл"
        ml < 1000 -> "$ml мл"
        else -> {
            val litres = ml / 1000
            val rest = (ml % 1000) / 100
            if (rest == 0L) "$litres л" else "$litres,$rest л"
        }
    }

    /** Вес: 450 -> «450 г», 1200 -> «1,2 кг». */
    fun formatWeight(grams: Long): String = when {
        grams <= 0 -> "0 г"
        grams < 1000 -> "$grams г"
        else -> {
            val kg = grams / 1000
            val rest = (grams % 1000) / 100
            if (rest == 0L) "$kg кг" else "$kg,$rest кг"
        }
    }

    /**
     * Основная метрика товара: для напитков литры, для весового граммы,
     * для остального штуки. Именно то, что интересно видеть в сводке.
     */
    fun formatTotal(
        unitKind: UnitKind,
        quantityMilli: Long,
        volumeMl: Long,
        weightG: Long,
    ): String = when (unitKind) {
        UnitKind.VOLUME -> formatVolume(volumeMl)
        UnitKind.WEIGHT -> formatWeight(weightG)
        UnitKind.PIECE -> formatPieces(quantityMilli)
    }

    /** Размер упаковки в названии товара: «0,449 л», «6 × 0,5 л», «400 г». */
    fun formatPackaging(volumeMl: Int?, weightG: Int?, packCount: Int?): String? {
        val size = when {
            volumeMl != null -> formatVolume(volumeMl.toLong())
            weightG != null -> formatWeight(weightG.toLong())
            else -> null
        } ?: return packCount?.let { "$it шт" }
        return if (packCount != null && packCount > 1) "$packCount × $size" else size
    }

    /** Цена за литр или килограмм — по ней видно, что выгоднее брать. */
    fun formatUnitPrice(priceKopecks: Long, volumeMl: Int?, weightG: Int?): String? {
        if (priceKopecks <= 0) return null
        volumeMl?.takeIf { it > 0 }?.let { ml ->
            val perLitre = priceKopecks * 1000 / ml
            return "${Money.format(perLitre)}/л"
        }
        weightG?.takeIf { it > 0 }?.let { g ->
            val perKg = priceKopecks * 1000 / g
            return "${Money.format(perKg)}/кг"
        }
        return null
    }
}
