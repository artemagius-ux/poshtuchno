package io.github.artemagius.poshtuchno.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SqliteOffsetTest {

    @Test
    fun `moscow offset is plus three`() {
        assertEquals("+03:00", Periods.sqliteOffset(ZoneId.of("Europe/Moscow")))
    }

    @Test
    fun `utc offset is zero`() {
        assertEquals("+00:00", Periods.sqliteOffset(ZoneId.of("UTC")))
    }

    @Test
    fun `negative offset keeps sign`() {
        // Нью-Йорк зимой: -05:00.
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        assertEquals("-05:00", Periods.sqliteOffset(ZoneId.of("America/New_York"), winter))
    }

    @Test
    fun `half hour offset is formatted`() {
        // Индия: +05:30.
        assertEquals("+05:30", Periods.sqliteOffset(ZoneId.of("Asia/Kolkata")))
    }

    @Test
    fun `dst is respected`() {
        val summer = Instant.parse("2026-07-15T12:00:00Z")
        assertEquals("-04:00", Periods.sqliteOffset(ZoneId.of("America/New_York"), summer))
    }
}
