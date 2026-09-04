package io.github.artemagius.poshtuchno.data.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяет, что товар, записанный быстрой тратой (короткое название без
 * размерности), сопоставляется с тем же товаром из чека и попадает в общую
 * группу. Именно этот путь раньше расходился: быстрый ввод не создавал товар.
 */
class QuickExpenseGroupingTest {

    private fun key(name: String) = ProductGrouping.matchKey(ProductNameParser.parse(name))

    private fun product(id: Long, name: String) = ProductGrouping.ProductInfo(
        id = id,
        name = name,
        tokens = ProductNameParser.tokenize(name),
    )

    @Test
    fun `short name from quick entry produces a stable key`() {
        assertEquals(key("Энергетик"), key("энергетик"))
        assertTrue(key("Энергетик").isNotEmpty())
    }

    @Test
    fun `quick entry name joins group with receipt items`() {
        val products = listOf(
            // из быстрой траты
            product(1, "Энергетик"),
            // из чека
            product(2, "Энергет.напиток BURN Original 0,449л"),
            product(3, "Напиток энерг. ADRENALINE RUSH 0,5л"),
        )

        val groups = ProductGrouping.suggest(products)
        val energy = groups.firstOrNull { it.token == "энергетик" }

        assertTrue(groups.toString(), energy != null)
        assertEquals(listOf(1L, 2L, 3L), energy!!.productIds)
    }

    @Test
    fun `quick entry with brand matches the same receipt product`() {
        // «Burn» из быстрой траты и «BURN» из чека без размера — один товар.
        assertEquals(key("Burn"), key("BURN"))
    }

    @Test
    fun `quick entry without size differs from sized receipt item`() {
        // Размер — часть товара: банка 0,449 и просто «энергетик» это разные
        // карточки, иначе цена за литр посчиталась бы неверно.
        assertTrue(key("Энергетик") != key("Энергетик 0,449л"))
    }

    @Test
    fun `case and spacing do not create duplicates`() {
        assertEquals(key("молоко  простоквашино"), key("Молоко Простоквашино"))
    }
}
