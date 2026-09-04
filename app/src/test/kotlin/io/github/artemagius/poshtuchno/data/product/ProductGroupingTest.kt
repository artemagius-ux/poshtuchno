package io.github.artemagius.poshtuchno.data.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductGroupingTest {

    private fun product(id: Long, name: String) = ProductGrouping.ProductInfo(
        id = id,
        name = name,
        tokens = ProductNameParser.tokenize(name),
    )

    @Test
    fun `repeated word becomes a group`() {
        val products = listOf(
            product(1, "Энергет.напиток BURN Original 0,449л"),
            product(2, "Напиток энерг. ADRENALINE RUSH 0,5л"),
            product(3, "Энергетик FLASH UP 0,45л"),
        )

        val groups = ProductGrouping.suggest(products)
        val energy = groups.firstOrNull { it.token == "энергетик" }

        assertTrue(groups.toString(), energy != null)
        assertEquals(3, energy!!.productCount)
        assertEquals(listOf(1L, 2L, 3L), energy.productIds)
    }

    @Test
    fun `brand across different products becomes a group`() {
        val products = listOf(
            product(1, "Молоко Простоквашино 3,2% 930мл"),
            product(2, "Кефир Простоквашино 1% 900мл"),
            product(3, "Сметана Простоквашино 15% 300г"),
        )

        val groups = ProductGrouping.suggest(products)
        assertTrue(groups.toString(), groups.any { it.token == "простоквашино" && it.productCount == 3 })
    }

    @Test
    fun `single occurrence does not create a group`() {
        val products = listOf(
            product(1, "Хлеб Бородинский 400г"),
            product(2, "Молоко 1л"),
        )
        val groups = ProductGrouping.suggest(products)
        assertTrue(groups.toString(), groups.none { it.token == "бородинский" })
    }

    @Test
    fun `empty input yields no groups`() {
        assertTrue(ProductGrouping.suggest(emptyList()).isEmpty())
    }

    @Test
    fun `groups are sorted by product count`() {
        val products = listOf(
            product(1, "Энергетик BURN 0,449л"),
            product(2, "Энергетик ADRENALINE 0,5л"),
            product(3, "Энергетик FLASH 0,45л"),
            product(4, "Молоко Домик в деревне 1л"),
            product(5, "Молоко Простоквашино 1л"),
        )
        val groups = ProductGrouping.suggest(products)
        assertEquals("энергетик", groups.first().token)
    }

    @Test
    fun `duplicate coverage keeps the longer word`() {
        // «энергетик» и «напиток» покрывают одни и те же товары —
        // остаётся более конкретное слово.
        val products = listOf(
            product(1, "Энергетический напиток BURN 0,449л"),
            product(2, "Энергетический напиток ADRENALINE 0,5л"),
        )
        val groups = ProductGrouping.suggest(products)
        val sameMembers = groups.filter { it.productIds == listOf(1L, 2L) }
        assertEquals(sameMembers.toString(), 1, sameMembers.size)
        assertEquals("энергетик", sameMembers.first().token)
    }

    @Test
    fun `minProducts threshold is respected`() {
        val products = listOf(
            product(1, "Энергетик BURN 0,449л"),
            product(2, "Энергетик ADRENALINE 0,5л"),
        )
        assertTrue(ProductGrouping.suggest(products, minProducts = 3).isEmpty())
        assertTrue(ProductGrouping.suggest(products, minProducts = 2).isNotEmpty())
    }

    @Test
    fun `match key ignores word order`() {
        val a = ProductNameParser.parse("Энергет.напиток BURN 0,449л")
        val b = ProductNameParser.parse("BURN напиток энергетический 449мл")
        assertEquals(ProductGrouping.matchKey(a), ProductGrouping.matchKey(b))
    }

    @Test
    fun `match key separates different volumes`() {
        val small = ProductNameParser.parse("Энергетик BURN 0,449л")
        val big = ProductNameParser.parse("Энергетик BURN 1л")
        assertTrue(ProductGrouping.matchKey(small) != ProductGrouping.matchKey(big))
    }

    @Test
    fun `match key separates different brands`() {
        val burn = ProductNameParser.parse("Энергетик BURN 0,449л")
        val adrenaline = ProductNameParser.parse("Энергетик ADRENALINE 0,449л")
        assertTrue(ProductGrouping.matchKey(burn) != ProductGrouping.matchKey(adrenaline))
    }

    @Test
    fun `match key separates pack from single`() {
        val single = ProductNameParser.parse("Пиво светлое 0,5л")
        val pack = ProductNameParser.parse("Пиво светлое 6x0,5л")
        assertTrue(ProductGrouping.matchKey(single) != ProductGrouping.matchKey(pack))
    }
}
