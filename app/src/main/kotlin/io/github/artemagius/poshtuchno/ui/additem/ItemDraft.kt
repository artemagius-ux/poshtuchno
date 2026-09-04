package io.github.artemagius.poshtuchno.ui.additem

import io.github.artemagius.poshtuchno.data.NewItem

/**
 * Черновик позиции покупки.
 *
 * Хранит ввод как есть: пользователь может печатать «2» в количестве и ещё
 * не дописать цену. Проверка и перевод в [NewItem] — только на сохранении.
 */
data class ItemDraft(
    val id: Long,
    val name: String = "",
    val priceKopecks: Long = 0,
    /** Количество в тысячных: 1 шт -> 1000. */
    val quantityMilli: Long = 1000,
    val categoryId: Long? = null,
    /** id карточки товара, если позиция взята из подсказки. */
    val productId: Long? = null,
    /** Цена этого же товара в прошлый раз — показываем рядом для сравнения. */
    val previousPriceKopecks: Long? = null,
) {
    val sumKopecks: Long get() = priceKopecks * quantityMilli / 1000

    val isValid: Boolean get() = name.isNotBlank() && priceKopecks > 0

    fun toNewItem(): NewItem = NewItem(
        name = name.trim(),
        unitPriceKopecks = priceKopecks,
        quantityMilli = quantityMilli,
        categoryId = categoryId,
    )
}

/**
 * Разница с прошлой ценой в процентах — «подорожало на 12%».
 * null, когда сравнивать не с чем.
 */
fun ItemDraft.priceChangePercent(): Int? {
    val previous = previousPriceKopecks ?: return null
    if (previous <= 0 || priceKopecks <= 0) return null
    if (previous == priceKopecks) return 0
    return Math.round((priceKopecks - previous).toDouble() / previous * 100).toInt()
}
