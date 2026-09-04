package io.github.artemagius.poshtuchno.data

import io.github.artemagius.poshtuchno.data.db.UnitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityTest {

    @Test
    fun `formats whole count`() {
        assertEquals("1", Quantity.formatCount(1000))
        assertEquals("6", Quantity.formatCount(6000))
    }

    @Test
    fun `formats fractional count`() {
        assertEquals("2,5", Quantity.formatCount(2500))
        assertEquals("0,25", Quantity.formatCount(250))
    }

    @Test
    fun `formats pieces`() {
        assertEquals("4 шт", Quantity.formatPieces(4000))
    }

    @Test
    fun `formats millilitres below litre`() {
        assertEquals("500 мл", Quantity.formatVolume(500))
        assertEquals("999 мл", Quantity.formatVolume(999))
    }

    @Test
    fun `formats litres`() {
        assertEquals("1 л", Quantity.formatVolume(1000))
        assertEquals("1,5 л", Quantity.formatVolume(1500))
        assertEquals("12 л", Quantity.formatVolume(12_000))
    }

    @Test
    fun `formats zero volume`() {
        assertEquals("0 мл", Quantity.formatVolume(0))
    }

    @Test
    fun `formats grams and kilograms`() {
        assertEquals("450 г", Quantity.formatWeight(450))
        assertEquals("1 кг", Quantity.formatWeight(1000))
        assertEquals("1,2 кг", Quantity.formatWeight(1200))
    }

    @Test
    fun `total uses volume for drinks`() {
        assertEquals(
            "2,6 л",
            Quantity.formatTotal(UnitKind.VOLUME, quantityMilli = 6000, volumeMl = 2694, weightG = 0),
        )
    }

    @Test
    fun `total uses weight for weighted goods`() {
        assertEquals(
            "1,2 кг",
            Quantity.formatTotal(UnitKind.WEIGHT, quantityMilli = 3000, volumeMl = 0, weightG = 1200),
        )
    }

    @Test
    fun `total uses pieces otherwise`() {
        assertEquals(
            "3 шт",
            Quantity.formatTotal(UnitKind.PIECE, quantityMilli = 3000, volumeMl = 0, weightG = 0),
        )
    }

    @Test
    fun `packaging shows single size`() {
        assertEquals("449 мл", Quantity.formatPackaging(449, null, null))
        assertEquals("400 г", Quantity.formatPackaging(null, 400, null))
    }

    @Test
    fun `packaging shows multipack`() {
        assertEquals("6 × 500 мл", Quantity.formatPackaging(500, null, 6))
    }

    @Test
    fun `packaging without size falls back to count`() {
        assertEquals("10 шт", Quantity.formatPackaging(null, null, 10))
        assertNull(Quantity.formatPackaging(null, null, null))
    }

    @Test
    fun `unit price per litre`() {
        // 100 ₽ за 500 мл -> 200 ₽/л
        assertEquals("200\u00A0\u20BD/л", Quantity.formatUnitPrice(10_000, 500, null))
    }

    @Test
    fun `unit price per kilogram`() {
        // 250 ₽ за 500 г -> 500 ₽/кг
        assertEquals("500\u00A0\u20BD/кг", Quantity.formatUnitPrice(25_000, null, 500))
    }

    @Test
    fun `unit price is null without size`() {
        assertNull(Quantity.formatUnitPrice(10_000, null, null))
        assertNull(Quantity.formatUnitPrice(0, 500, null))
    }
}
