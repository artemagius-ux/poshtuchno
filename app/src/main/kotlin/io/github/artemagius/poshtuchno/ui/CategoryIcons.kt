package io.github.artemagius.poshtuchno.ui

import androidx.annotation.DrawableRes
import io.github.artemagius.poshtuchno.R

/**
 * Иконки категорий: в базе лежит короткое имя, здесь — сопоставление с ресурсом.
 * Набор можно менять без миграции базы.
 */
object CategoryIcons {

    @DrawableRes
    val fallback: Int = R.drawable.ic_cat_other

    private val byName: Map<String, Int> = mapOf(
        "cart" to R.drawable.ic_cat_cart,
        "cafe" to R.drawable.ic_cat_cafe,
        "transport" to R.drawable.ic_cat_transport,
        "home" to R.drawable.ic_cat_home,
        "health" to R.drawable.ic_cat_health,
        "clothes" to R.drawable.ic_cat_clothes,
        "fun" to R.drawable.ic_cat_fun,
        "subscription" to R.drawable.ic_cat_subscription,
        "other" to R.drawable.ic_cat_other,
    )

    /** Имена в порядке показа в выборе иконки. */
    val names: List<String> = byName.keys.toList()

    @DrawableRes
    operator fun get(name: String?): Int = byName[name] ?: fallback
}
