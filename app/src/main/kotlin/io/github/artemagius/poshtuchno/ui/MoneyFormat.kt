package io.github.artemagius.poshtuchno.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.artemagius.poshtuchno.data.Money

/**
 * Показывать ли копейки. Настройка живёт в композиции, чтобы её не приходилось
 * протаскивать параметром через каждый экран и каждую карточку.
 */
val LocalShowKopecks = staticCompositionLocalOf { true }

/** Форматирование суммы с учётом настройки копеек. */
@Composable
@ReadOnlyComposable
fun money(kopecks: Long, withCurrency: Boolean = true): String =
    Money.format(kopecks, withCurrency = withCurrency, showKopecks = LocalShowKopecks.current)
