package io.github.artemagius.poshtuchno.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import io.github.artemagius.poshtuchno.data.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Violet40,
    onPrimary = Neutral100,
    primaryContainer = Violet90,
    onPrimaryContainer = Violet10,
    inversePrimary = Violet80,
    secondary = Muted40,
    onSecondary = Neutral100,
    secondaryContainer = Muted90,
    onSecondaryContainer = Muted20,
    tertiary = Plum40,
    onTertiary = Neutral100,
    tertiaryContainer = Plum90,
    onTertiaryContainer = Plum30,
    error = Red40,
    onError = Neutral100,
    errorContainer = Red90,
    onErrorContainer = Red10,
    // Фон чуть темнее карточек: так тень читается, а карточка «всплывает».
    background = Neutral96,
    onBackground = Neutral10,
    surface = Neutral96,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral98,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Neutral0,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral94,
)

private val DarkScheme = darkColorScheme(
    primary = Violet80,
    onPrimary = Violet20,
    primaryContainer = Violet30,
    onPrimaryContainer = Violet90,
    inversePrimary = Violet40,
    secondary = Muted80,
    onSecondary = Muted20,
    secondaryContainer = Muted30,
    onSecondaryContainer = Muted90,
    tertiary = Plum80,
    onTertiary = Plum30,
    tertiaryContainer = Plum30,
    onTertiaryContainer = Plum90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceContainerLowest = Neutral0,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Neutral0,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
)

/** Скругления одинаковые у всех карточек — смешанных радиусов быть не должно. */
private val PoshtuchnoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalChartColors = staticCompositionLocalOf { ChartPalette }

@Composable
fun PoshtuchnoTheme(
    themeMode: ThemeMode = ThemeMode.Auto,
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
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val chartColors: List<Color> = if (useDynamic) {
        // В dynamic color строим ряд из системного акцента по светлоте,
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
