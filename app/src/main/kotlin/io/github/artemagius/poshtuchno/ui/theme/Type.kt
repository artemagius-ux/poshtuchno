package io.github.artemagius.poshtuchno.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Типографика с двумя отличиями от дефолтной M3:
 * display-стили плотнее (крупные суммы не должны разъезжаться) и
 * у них отключён лишний отступ сверху, чтобы цифры вставали ровно по сетке.
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
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1).sp,
        lineHeightStyle = tightLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = tightLineHeight,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
        lineHeightStyle = tightLineHeight,
    ),
    headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = default.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = default.labelMedium.copy(letterSpacing = 0.4.sp),
)
