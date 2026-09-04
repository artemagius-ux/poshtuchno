package io.github.artemagius.poshtuchno.ui

import androidx.annotation.DrawableRes
import io.github.artemagius.poshtuchno.R

/** Вкладки нижней навигации. */
enum class Tab(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
) {
    Today("today", "Сегодня", R.drawable.ic_tab_today),
    History("history", "История", R.drawable.ic_tab_history),
    Analytics("analytics", "Аналитика", R.drawable.ic_tab_analytics),
    Settings("settings", "Настройки", R.drawable.ic_tab_settings),
}
