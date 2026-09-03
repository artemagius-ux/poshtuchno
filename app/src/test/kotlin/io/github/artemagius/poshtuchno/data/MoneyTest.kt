package io.github.artemagius.poshtuchno.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parse accepts plain rubles`() {
        assertEquals(12300L, Money.parse("123"))
    }

    @Test
    fun `parse accepts comma and dot separators`() {
        assertEquals(12345L, Money.parse("123,45"))
        assertEquals(12345L, Money.parse("123.45"))
    }

    @Test
    fun `parse pads single decimal digit`() {
        assertEquals(12340L, Money.parse("123,4"))
    }

    @Test
    fun `parse truncates extra decimals`() {
        assertEquals(12345L, Money.parse("123,456"))
    }

    @Test
    fun `parse ignores spaces used as thousand separators`() {
        assertEquals(123456700L, Money.parse("1 234 567"))
    }

    @Test
    fun `parse rejects garbage`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1.2.3"))
    }

    @Test
    fun `format hides zero kopecks`() {
        assertEquals("123\u00A0\u20BD", Money.format(12300))
    }

    @Test
    fun `format keeps non-zero kopecks`() {
        assertEquals("123,45\u00A0\u20BD", Money.format(12345))
    }

    @Test
    fun `format pads leading zero in kopecks`() {
        assertEquals("123,05\u00A0\u20BD", Money.format(12305))
    }

    @Test
    fun `format groups thousands`() {
        assertEquals("1\u00A0234\u00A0567\u00A0\u20BD", Money.format(123456700))
    }

    @Test
    fun `format handles negative amounts`() {
        assertEquals("-123,45\u00A0\u20BD", Money.format(-12345))
    }

    @Test
    fun `format can omit currency`() {
        assertEquals("123,45", Money.format(12345, withCurrency = false))
    }
}
