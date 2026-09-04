package io.github.artemagius.poshtuchno.data.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductNameParserTest {

    @Test
    fun `parses volume in litres`() {
        val parsed = ProductNameParser.parse("Напиток энерг. ADRENALINE RUSH 0,5 л")
        assertEquals(500, parsed.volumeMl)
        assertEquals(ProductNameParser.UnitKind.Volume, parsed.unitKind)
    }

    @Test
    fun `parses volume in millilitres`() {
        val parsed = ProductNameParser.parse("Энергет.напиток BURN Original ж/б 449мл")
        assertEquals(449, parsed.volumeMl)
    }

    @Test
    fun `parses volume with dot separator`() {
        assertEquals(1500, ProductNameParser.parse("Вода Святой Источник 1.5л").volumeMl)
    }

    @Test
    fun `parses weight in grams`() {
        assertEquals(400, ProductNameParser.parse("Сыр Российский 400 г").weightG)
    }

    @Test
    fun `parses weight in kilograms`() {
        assertEquals(1200, ProductNameParser.parse("Картофель 1,2 кг").weightG)
    }

    @Test
    fun `parses pack count`() {
        val parsed = ProductNameParser.parse("Пиво светлое 6x0,5л")
        assertEquals(6, parsed.packCount)
        assertEquals(500, parsed.volumeMl)
        assertEquals(3000, parsed.totalVolumeMl)
    }

    @Test
    fun `single item has no pack count`() {
        assertNull(ProductNameParser.parse("Молоко 1л").packCount)
    }

    @Test
    fun `total volume equals volume without pack`() {
        assertEquals(1000, ProductNameParser.parse("Молоко 1л").totalVolumeMl)
    }

    @Test
    fun `piece item has no size`() {
        val parsed = ProductNameParser.parse("Батон нарезной")
        assertNull(parsed.volumeMl)
        assertNull(parsed.weightG)
        assertEquals(ProductNameParser.UnitKind.Piece, parsed.unitKind)
    }

    @Test
    fun `clean name drops size and percent`() {
        val parsed = ProductNameParser.parse("Молоко Простоквашино 3,2% 930 мл")
        assertTrue(parsed.cleanName, !parsed.cleanName.contains("930"))
        assertTrue(parsed.cleanName, !parsed.cleanName.contains("%"))
        assertTrue(parsed.cleanName, parsed.cleanName.contains("Простоквашино"))
    }

    @Test
    fun `abbreviations expand to full words`() {
        val tokens = ProductNameParser.tokenize("Энергет.напиток BURN")
        assertTrue(tokens.toString(), tokens.contains("энергетик"))
        assertTrue(tokens.toString(), tokens.contains("напиток"))
    }

    @Test
    fun `different abbreviations produce same token`() {
        val a = ProductNameParser.tokenize("Напиток энерг. ADRENALINE")
        val b = ProductNameParser.tokenize("Энергетический напиток BURN")
        assertTrue(a.contains("энергетик"))
        assertTrue(b.contains("энергетик"))
    }

    @Test
    fun `packaging words are dropped`() {
        val tokens = ProductNameParser.tokenize("Кола ж/б 0,33л")
        assertTrue(tokens.toString(), !tokens.contains("жб"))
        assertTrue(tokens.toString(), !tokens.contains("ж"))
        assertTrue(tokens.toString(), tokens.contains("кола"))
    }

    @Test
    fun `numbers are dropped from tokens`() {
        val tokens = ProductNameParser.tokenize("Печенье Юбилейное 112 г")
        assertTrue(tokens.toString(), tokens.none { it.all(Char::isDigit) })
    }

    @Test
    fun `tokens are unique`() {
        val tokens = ProductNameParser.tokenize("Молоко молоко Молоко")
        assertEquals(listOf("молоко"), tokens)
    }

    @Test
    fun `latin word is detected as brand`() {
        assertEquals("Burn", ProductNameParser.parse("Энергет.напиток BURN Original 0,449л").brand)
    }

    @Test
    fun `uppercase cyrillic word is detected as brand`() {
        assertEquals("Черноголовка", ProductNameParser.parse("Лимонад ЧЕРНОГОЛОВКА 1,5л").brand)
    }

    @Test
    fun `empty name is handled`() {
        val parsed = ProductNameParser.parse("   ")
        assertEquals("", parsed.cleanName)
        assertTrue(parsed.tokens.isEmpty())
    }
}
