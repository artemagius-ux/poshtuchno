package io.github.artemagius.poshtuchno.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Neutral100,
    primaryContainer = Emerald90,
    onPrimaryContainer = Emerald10,
    secondary = Sage40,
    onSecondary = Neutral100,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Amber40,
    onTertiary = Neutral100,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Neutral100,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Neutral0,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Emerald80,
)

private val DarkScheme = darkColorScheme(
    primary = Emerald80,
    onPrimary = Emerald20,
    primaryContainer = Emerald30,
    onPrimaryContainer = Emerald90,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Neutral0,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Emerald40,
)

private val PoshtuchnoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Палитра графиков доступна через CompositionLocal: она не входит в ColorScheme,
 * но должна одинаково работать и в статической теме, и в dynamic color.
 */
val LocalChartColors = staticCompositionLocalOf { ChartPalette }

@Composable
fun PoshtuchnoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val chartColors: List<Color> = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // В dynamic color первые цвета берём из системной палитры, чтобы графики
        // не выбивались из общего тона, остальные добираем из своей.
        listOf(colorScheme.primary, colorScheme.tertiary, colorScheme.secondary) +
            ChartPalette.drop(3)
    } else {
        ChartPalette
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
