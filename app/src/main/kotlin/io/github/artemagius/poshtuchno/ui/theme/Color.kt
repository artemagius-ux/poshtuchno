package io.github.artemagius.poshtuchno.ui.theme

import androidx.compose.ui.graphics.Color

// Фиолетовый акцент под маскота. Один цветовой род на всё приложение:
// иконки категорий, графики и активные состояния — оттенки этого семейства.
val Violet10 = Color(0xFF21005D)
val Violet20 = Color(0xFF381E72)
val Violet30 = Color(0xFF4F378B)
val Violet40 = Color(0xFF6750A4)
val Violet50 = Color(0xFF7F67BE)
val Violet60 = Color(0xFF9A82DB)
val Violet70 = Color(0xFFB69DF8)
val Violet80 = Color(0xFFCFBCFF)
val Violet90 = Color(0xFFE9DDFF)
val Violet95 = Color(0xFFF6EDFF)
val Violet99 = Color(0xFFFFFBFF)

// Вторичный — тот же фиолетовый, но приглушённый: для второстепенных плашек.
val Muted20 = Color(0xFF332D41)
val Muted30 = Color(0xFF4A4458)
val Muted40 = Color(0xFF625B71)
val Muted80 = Color(0xFFCBC2DB)
val Muted90 = Color(0xFFE8DEF8)

// Третичный — розово-сливовый, для редких акцентов вроде перерасхода в графике.
val Plum30 = Color(0xFF633B48)
val Plum40 = Color(0xFF7D5260)
val Plum80 = Color(0xFFEFB8C8)
val Plum90 = Color(0xFFFFD8E4)

val Red10 = Color(0xFF410002)
val Red20 = Color(0xFF690005)
val Red30 = Color(0xFF93000A)
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)

// Нейтральные с лёгким фиолетовым подтоном: карточки должны читаться как
// серые, но не «холодить» рядом с акцентом.
val Neutral0 = Color(0xFF000000)
val Neutral6 = Color(0xFF141218)
val Neutral10 = Color(0xFF1D1B20)
val Neutral12 = Color(0xFF211F26)
val Neutral17 = Color(0xFF2B2930)
val Neutral20 = Color(0xFF322F35)
val Neutral22 = Color(0xFF36343B)
val Neutral24 = Color(0xFF3B383E)
val Neutral90 = Color(0xFFE6E0E9)
val Neutral92 = Color(0xFFECE6F0)
val Neutral94 = Color(0xFFF3EDF7)
val Neutral96 = Color(0xFFF7F2FA)
val Neutral98 = Color(0xFFFEF7FF)
val Neutral100 = Color(0xFFFFFFFF)

val NeutralVariant30 = Color(0xFF49454F)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant60 = Color(0xFF938F99)
val NeutralVariant80 = Color(0xFFCAC4D0)
val NeutralVariant90 = Color(0xFFE7E0EC)

/**
 * Палитра графиков: один фиолетовый род от насыщенного к светлому.
 * Категории на кольце различаются светлотой, а не цветом — так диаграмма
 * не выглядит набором случайных кружков.
 */
val ChartPalette: List<Color> = listOf(
    Color(0xFF6750A4),
    Color(0xFF8168C4),
    Color(0xFF9A82DB),
    Color(0xFFB19CE8),
    Color(0xFFC5B4F0),
    Color(0xFF7D5260),
    Color(0xFF9E7482),
    Color(0xFFBFA0AB),
)
