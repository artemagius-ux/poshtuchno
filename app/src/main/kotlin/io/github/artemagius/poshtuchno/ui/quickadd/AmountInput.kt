package io.github.artemagius.poshtuchno.ui.quickadd

/**
 * Состояние цифрового ввода суммы.
 *
 * Держим сумму строкой, а не числом: пользователь может печатать «12,», и это
 * промежуточное состояние надо показывать как есть. Разбор в копейки — в самом конце.
 */
data class AmountInput(
    val digits: String = "",
    val fraction: String? = null,
) {
    val hasValue: Boolean get() = digits.isNotEmpty()

    val kopecks: Long
        get() {
            val rubles = digits.toLongOrNull() ?: 0L
            val cents = fraction?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0L
            return rubles * 100 + cents
        }

    /** То, что показывается на экране во время набора. */
    fun display(): String {
        if (digits.isEmpty() && fraction == null) return "0"
        val grouped = digits.ifEmpty { "0" }
            .reversed()
            .chunked(3)
            .joinToString("\u00A0")
            .reversed()
        return when (fraction) {
            null -> grouped
            else -> "$grouped,$fraction"
        }
    }

    fun appendDigit(digit: Char): AmountInput {
        require(digit.isDigit())
        return when {
            fraction != null -> if (fraction.length >= 2) this else copy(fraction = fraction + digit)
            // Не даём набрать бесконечно длинное число: 10 знаков это 99 999 999,99 ₽.
            digits.length >= 8 -> this
            digits.isEmpty() && digit == '0' -> this
            else -> copy(digits = digits + digit)
        }
    }

    fun appendSeparator(): AmountInput = when (fraction) {
        null -> copy(digits = digits.ifEmpty { "0" }, fraction = "")
        else -> this
    }

    fun backspace(): AmountInput = when {
        fraction != null && fraction.isNotEmpty() -> copy(fraction = fraction.dropLast(1))
        fraction != null -> copy(fraction = null)
        digits.isNotEmpty() -> copy(digits = digits.dropLast(1))
        else -> this
    }

    fun clear(): AmountInput = AmountInput()

    companion object {
        fun ofKopecks(kopecks: Long): AmountInput {
            val rubles = kopecks / 100
            val cents = kopecks % 100
            return AmountInput(
                digits = rubles.toString(),
                fraction = if (cents == 0L) null else cents.toString().padStart(2, '0'),
            )
        }
    }
}
