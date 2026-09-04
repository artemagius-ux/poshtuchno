package io.github.artemagius.poshtuchno.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Локаль для форматирования внутри composable-функций.
 *
 * Через [LocalLocale], а не `Locale.getDefault()`: смена языка системы должна
 * перерисовывать экран, а прямой вызов getDefault не является observable state.
 */
@Composable
fun currentLocale(): Locale = LocalLocale.current.platformLocale

@Composable
fun rememberDateFormatter(pattern: String): DateTimeFormatter {
    val locale = currentLocale()
    return remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
}

/** «сентябрь» -> «Сентябрь». */
@Composable
fun String.titlecaseFirst(): String {
    val locale = currentLocale()
    return replaceFirstChar { it.titlecase(locale) }
}
