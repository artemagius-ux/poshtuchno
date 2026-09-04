package io.github.artemagius.poshtuchno.data

/** Форматирование сумм. Внутри всё в копейках, наружу — рубли. */
object Money {
    /**
     * @param showKopecks когда false, сумма округляется до рубля.
     *   Округление, а не отбрасывание: иначе «99,90 ₽» превратилось бы в «99 ₽».
     */
    fun format(kopecks: Long, withCurrency: Boolean = true, showKopecks: Boolean = true): String {
        val sign = if (kopecks < 0) "-" else ""
        val abs = kotlin.math.abs(kopecks)
        val rubles: Long
        val cents: Long
        if (showKopecks) {
            rubles = abs / 100
            cents = abs % 100
        } else {
            rubles = (abs + 50) / 100
            cents = 0
        }
        val grouped = rubles.toString()
            .reversed()
            .chunked(3)
            .joinToString("\u00A0")
            .reversed()
        val body = if (cents == 0L) grouped else "$grouped,${cents.toString().padStart(2, '0')}"
        return if (withCurrency) "$sign$body\u00A0\u20BD" else "$sign$body"
    }

    /** Разбор пользовательского ввода: "123", "123,45", "123.4" -> копейки. */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace('\u00A0', ' ').replace(" ", "").replace(',', '.')
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        val rubles = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val cents = when {
            parts.size == 1 -> 0L
            else -> parts[1].take(2).padEnd(2, '0').toLongOrNull() ?: return null
        }
        return rubles * 100 + cents
    }
}
