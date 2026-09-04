package io.github.artemagius.poshtuchno.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Одна жирная роль в интерфейсе — суммы. Всё остальное обычного или среднего
 * веса, иерархия строится размером и цветом, а не набором толщин.
 */
private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private val default = Typography()

val PoshtuchnoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1).sp,
        lineHeightStyle = tightLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.6).sp,
        lineHeightStyle = tightLineHeight,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = tightLineHeight,
    ),
    headlineMedium = default.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = tightLineHeight,
    ),
    headlineSmall = default.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = tightLineHeight,
    ),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = default.titleSmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = default.labelMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
    labelSmall = default.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
    bodyLarge = default.bodyLarge,
    bodyMedium = default.bodyMedium,
    bodySmall = default.bodySmall,
)
