package io.github.artemagius.poshtuchno.ui.quickadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountInputTest {

    private fun type(keys: String): AmountInput =
        keys.fold(AmountInput()) { acc, c ->
            when (c) {
                ',' -> acc.appendSeparator()
                '<' -> acc.backspace()
                else -> acc.appendDigit(c)
            }
        }

    @Test
    fun `digits accumulate into rubles`() {
        assertEquals(12_300L, type("123").kopecks)
    }

    @Test
    fun `separator starts kopecks`() {
        assertEquals(12_345L, type("123,45").kopecks)
    }

    @Test
    fun `single kopeck digit is padded`() {
        assertEquals(12_340L, type("123,4").kopecks)
    }

    @Test
    fun `third kopeck digit is ignored`() {
        assertEquals(12_345L, type("123,456").kopecks)
    }

    @Test
    fun `second separator is ignored`() {
        val input = type("12,3,4")
        assertEquals("12,34", input.display())
    }

    @Test
    fun `leading zero is rejected`() {
        assertFalse(type("0").hasValue)
        assertEquals(0L, type("0").kopecks)
    }

    @Test
    fun `zero is allowed after separator`() {
        assertEquals(5L, type("0,05").kopecks)
    }

    @Test
    fun `digits are capped to eight`() {
        val input = type("1234567890")
        assertEquals("12345678", input.digits)
    }

    @Test
    fun `backspace removes kopecks then separator then digits`() {
        assertEquals("12,3", type("12,34<").display())
        // Копейки стёрты, но режим ввода копеек сохраняется: можно набрать заново.
        assertEquals("12,", type("12,34<<").display())
        assertEquals("12", type("12,34<<<").display())
        assertEquals("1", type("12,34<<<<").display())
    }

    @Test
    fun `backspace on empty input is a no-op`() {
        assertEquals(AmountInput(), AmountInput().backspace())
    }

    @Test
    fun `display groups thousands`() {
        assertEquals("1\u00A0234\u00A0567", type("1234567").display())
    }

    @Test
    fun `empty input displays zero`() {
        assertEquals("0", AmountInput().display())
        assertFalse(AmountInput().hasValue)
    }

    @Test
    fun `separator on empty input yields zero rubles`() {
        val input = type(",")
        assertEquals("0,", input.display())
        assertTrue(input.hasValue)
    }

    @Test
    fun `ofKopecks roundtrips`() {
        assertEquals(12_345L, AmountInput.ofKopecks(12_345).kopecks)
        assertEquals("123,45", AmountInput.ofKopecks(12_345).display())
        assertEquals("123", AmountInput.ofKopecks(12_300).display())
    }

    @Test
    fun `ofKopecks pads leading zero in kopecks`() {
        assertEquals("123,05", AmountInput.ofKopecks(12_305).display())
    }
}
