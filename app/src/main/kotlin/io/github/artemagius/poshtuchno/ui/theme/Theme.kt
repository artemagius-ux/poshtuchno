package io.github.artemagius.poshtuchno.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BrandGreen = Color(0xFF00695C)
private val BrandGreenLight = Color(0xFF4DB6AC)
private val BrandAmber = Color(0xFFFFB300)

private val LightScheme = lightColorScheme(
    primary = BrandGreen,
    secondary = BrandGreenLight,
    tertiary = BrandAmber,
)

private val DarkScheme = darkColorScheme(
    primary = BrandGreenLight,
    secondary = BrandGreen,
    tertiary = BrandAmber,
)

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

    MaterialTheme(colorScheme = colorScheme, content = content)
}
