package io.github.artemagius.poshtuchno.data.receipt

/**
 * Разбор QR-кода фискального чека (ФНС).
 *
 * Формат — строка вида
 * `t=20260904T1923&s=1498.00&fn=9282440300669857&i=25151&fp=1186123459&n=1`
 *
 * В QR лежат только реквизиты: дата, сумма, номер фискального накопителя,
 * номер документа и фискальный признак. Списка товаров там нет — его отдают
 * только сервисы ФНС или платные API. Поэтому QR закрывает шапку покупки
 * и защиту от повторного импорта, а позиции остаются за пользователем.
 */
object ReceiptQr {

    data class Receipt(
        /** Дата и время покупки, epoch millis в местной таймзоне. */
        val purchasedAt: Long,
        val totalKopecks: Long,
        /** Номер фискального накопителя. */
        val fn: String,
        /** Номер фискального документа. */
        val fd: String,
        /** Фискальный признак. */
        val fp: String,
        /** Тип чека: 1 — приход, 2 — возврат прихода и так далее. */
        val operationType: Int?,
        val raw: String,
    )

    /**
     * @return разобранный чек или null, если строка не похожа на фискальный QR.
     */
    fun parse(raw: String, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Receipt? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        val params = text
            .split('&')
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) return@mapNotNull null
                part.substring(0, index).lowercase() to part.substring(index + 1)
            }
            .toMap()

        val time = params["t"] ?: return null
        val sum = params["s"] ?: return null
        val fn = params["fn"] ?: return null
        val fd = params["i"] ?: return null
        val fp = params["fp"] ?: return null

        val purchasedAt = parseTimestamp(time, zone) ?: return null
        val kopecks = parseSum(sum) ?: return null
        if (fn.isBlank() || fd.isBlank() || fp.isBlank()) return null

        return Receipt(
            purchasedAt = purchasedAt,
            totalKopecks = kopecks,
            fn = fn,
            fd = fd,
            fp = fp,
            operationType = params["n"]?.toIntOrNull(),
            raw = text,
        )
    }

    /**
     * Время в QR приходит как `20260904T1923` или `20260904T192355`,
     * иногда с дефисами и двоеточиями. Секунды опциональны.
     */
    private fun parseTimestamp(value: String, zone: java.time.ZoneId): Long? {
        val digits = value.filter { it.isDigit() }
        if (digits.length < 12) return null
        return runCatching {
            val year = digits.substring(0, 4).toInt()
            val month = digits.substring(4, 6).toInt()
            val day = digits.substring(6, 8).toInt()
            val hour = digits.substring(8, 10).toInt()
            val minute = digits.substring(10, 12).toInt()
            val second = if (digits.length >= 14) digits.substring(12, 14).toInt() else 0
            java.time.LocalDateTime.of(year, month, day, hour, minute, second)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    /** Сумма в QR — рубли с точкой: «1498.00». Иногда встречается запятая. */
    private fun parseSum(value: String): Long? {
        val cleaned = value.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        val rubles = parts[0].toLongOrNull() ?: return null
        if (rubles < 0) return null
        val kopecks = when (parts.size) {
            1 -> 0L
            else -> parts[1].take(2).padEnd(2, '0').toLongOrNull() ?: return null
        }
        return rubles * 100 + kopecks
    }
}
