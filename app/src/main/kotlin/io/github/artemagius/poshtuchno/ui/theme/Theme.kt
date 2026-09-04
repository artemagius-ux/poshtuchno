package io.github.artemagius.poshtuchno.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.ThemeMode

/** Скругления одинаковые у всех карточек — смешанных радиусов быть не должно. */
private val PoshtuchnoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalChartColors = staticCompositionLocalOf { AppPalette.Violet.chart }

@Composable
fun PoshtuchnoTheme(
    themeMode: ThemeMode = ThemeMode.Auto,
    palette: AppPalette = AppPalette.Violet,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Auto -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> palette.scheme(darkTheme)
    }

    val chartColors: List<Color> = if (useDynamic) {
        // В dynamic color ряд строим из системного акцента по прозрачности,
        // чтобы кольцо оставалось одноцветным по духу.
        listOf(
            colorScheme.primary,
            colorScheme.primary.copy(alpha = 0.82f),
            colorScheme.primary.copy(alpha = 0.64f),
            colorScheme.primary.copy(alpha = 0.48f),
            colorScheme.tertiary,
            colorScheme.tertiary.copy(alpha = 0.7f),
            colorScheme.secondary,
            colorScheme.secondary.copy(alpha = 0.7f),
        )
    } else {
        palette.chart
    }

    CompositionLocalProvider(LocalChartColors provides chartColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PoshtuchnoTypography,
            shapes = PoshtuchnoShapes,
            content = content,
        )
    }
}
