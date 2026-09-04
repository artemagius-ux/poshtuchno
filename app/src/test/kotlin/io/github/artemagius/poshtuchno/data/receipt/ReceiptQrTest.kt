package io.github.artemagius.poshtuchno.data.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class ReceiptQrTest {

    private val moscow = ZoneId.of("Europe/Moscow")

    @Test
    fun `parses standard receipt qr`() {
        val qr = "t=20260904T1923&s=1498.00&fn=9282440300669857&i=25151&fp=1186123459&n=1"
        val receipt = ReceiptQr.parse(qr, moscow)!!

        assertEquals(149_800L, receipt.totalKopecks)
        assertEquals("9282440300669857", receipt.fn)
        assertEquals("25151", receipt.fd)
        assertEquals("1186123459", receipt.fp)
        assertEquals(1, receipt.operationType)
    }

    @Test
    fun `parses timestamp in local zone`() {
        val qr = "t=20260904T1923&s=100.00&fn=1&i=2&fp=3&n=1"
        val receipt = ReceiptQr.parse(qr, moscow)!!
        val expected = java.time.LocalDateTime.of(2026, 9, 4, 19, 23)
            .atZone(moscow).toInstant().toEpochMilli()
        assertEquals(expected, receipt.purchasedAt)
    }

    @Test
    fun `parses timestamp with seconds`() {
        val qr = "t=20260904T192355&s=100.00&fn=1&i=2&fp=3&n=1"
        val receipt = ReceiptQr.parse(qr, moscow)!!
        val expected = java.time.LocalDateTime.of(2026, 9, 4, 19, 23, 55)
            .atZone(moscow).toInstant().toEpochMilli()
        assertEquals(expected, receipt.purchasedAt)
    }

    @Test
    fun `parses sum without kopecks`() {
        val receipt = ReceiptQr.parse("t=20260904T1923&s=1498&fn=1&i=2&fp=3", moscow)!!
        assertEquals(149_800L, receipt.totalKopecks)
    }

    @Test
    fun `parses sum with comma`() {
        val receipt = ReceiptQr.parse("t=20260904T1923&s=99,90&fn=1&i=2&fp=3", moscow)!!
        assertEquals(9_990L, receipt.totalKopecks)
    }

    @Test
    fun `parses sum with single decimal digit`() {
        val receipt = ReceiptQr.parse("t=20260904T1923&s=99.9&fn=1&i=2&fp=3", moscow)!!
        assertEquals(9_990L, receipt.totalKopecks)
    }

    @Test
    fun `handles parameters in any order`() {
        val qr = "fp=3&s=10.50&i=2&fn=1&t=20260904T1923"
        val receipt = ReceiptQr.parse(qr, moscow)!!
        assertEquals(1_050L, receipt.totalKopecks)
    }

    @Test
    fun `operation type is optional`() {
        val receipt = ReceiptQr.parse("t=20260904T1923&s=10.00&fn=1&i=2&fp=3", moscow)!!
        assertNull(receipt.operationType)
    }

    @Test
    fun `rejects missing fiscal number`() {
        assertNull(ReceiptQr.parse("t=20260904T1923&s=10.00&i=2&fp=3", moscow))
    }

    @Test
    fun `rejects missing sum`() {
        assertNull(ReceiptQr.parse("t=20260904T1923&fn=1&i=2&fp=3", moscow))
    }

    @Test
    fun `rejects arbitrary text`() {
        assertNull(ReceiptQr.parse("https://example.com/promo", moscow))
        assertNull(ReceiptQr.parse("", moscow))
    }

    @Test
    fun `rejects broken timestamp`() {
        assertNull(ReceiptQr.parse("t=2026&s=10.00&fn=1&i=2&fp=3", moscow))
    }

    @Test
    fun `rejects non numeric sum`() {
        assertNull(ReceiptQr.parse("t=20260904T1923&s=abc&fn=1&i=2&fp=3", moscow))
    }

    @Test
    fun `keeps raw string`() {
        val qr = "t=20260904T1923&s=10.00&fn=1&i=2&fp=3"
        assertEquals(qr, ReceiptQr.parse(qr, moscow)!!.raw)
    }
}
