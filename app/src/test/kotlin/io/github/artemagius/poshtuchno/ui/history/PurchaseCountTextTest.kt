package io.github.artemagius.poshtuchno.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseCountTextTest {

    @Test
    fun `singular for one`() {
        assertEquals("1 трата", purchaseCountText(1))
    }

    @Test
    fun `plural genitive for two to four`() {
        assertEquals("2 траты", purchaseCountText(2))
        assertEquals("3 траты", purchaseCountText(3))
        assertEquals("4 траты", purchaseCountText(4))
    }

    @Test
    fun `plural for five and more`() {
        assertEquals("5 трат", purchaseCountText(5))
        assertEquals("10 трат", purchaseCountText(10))
        assertEquals("0 трат", purchaseCountText(0))
    }

    @Test
    fun `teens use plural form`() {
        assertEquals("11 трат", purchaseCountText(11))
        assertEquals("12 трат", purchaseCountText(12))
        assertEquals("14 трат", purchaseCountText(14))
    }

    @Test
    fun `twenty one uses singular`() {
        assertEquals("21 трата", purchaseCountText(21))
        assertEquals("101 трата", purchaseCountText(101))
    }

    @Test
    fun `twenty two uses genitive`() {
        assertEquals("22 траты", purchaseCountText(22))
        assertEquals("133 траты", purchaseCountText(133))
    }

    @Test
    fun `hundred eleven uses plural`() {
        assertEquals("111 трат", purchaseCountText(111))
    }
}
