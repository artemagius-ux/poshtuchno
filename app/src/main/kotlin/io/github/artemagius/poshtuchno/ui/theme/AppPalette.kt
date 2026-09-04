package io.github.artemagius.poshtuchno.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Цветовая схема приложения. Каждая палитра строится из одного акцентного
 * рода: карточки и фон остаются нейтральными, меняется только акцент и
 * оттенки графиков. Так любая тема остаётся «финтех-спокойной».
 */
enum class AppPalette(
    val label: String,
    /** Цвет для превью в настройках. */
    val swatch: Color,
    private val lightAccent: Accent,
    private val darkAccent: Accent,
    val chart: List<Color>,
) {
    Violet(
        label = "Ворон",
        swatch = Color(0xFF6750A4),
        lightAccent = Accent(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            container = Color(0xFFE9DDFF),
            onContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF332D41),
            tertiary = Color(0xFF7D5260),
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF633B48),
        ),
        darkAccent = Accent(
            primary = Color(0xFFCFBCFF),
            onPrimary = Color(0xFF381E72),
            container = Color(0xFF4F378B),
            onContainer = Color(0xFFE9DDFF),
            secondary = Color(0xFFCBC2DB),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
        ),
        chart = listOf(
            Color(0xFF6750A4), Color(0xFF8168C4), Color(0xFF9A82DB), Color(0xFFB19CE8),
            Color(0xFFC5B4F0), Color(0xFF7D5260), Color(0xFF9E7482), Color(0xFFBFA0AB),
        ),
    ),

    Teal(
        label = "Бирюза",
        swatch = Color(0xFF00696D),
        lightAccent = Accent(
            primary = Color(0xFF00696D),
            onPrimary = Color(0xFFFFFFFF),
            container = Color(0xFF97F1F4),
            onContainer = Color(0xFF002021),
            secondary = Color(0xFF4A6363),
            secondaryContainer = Color(0xFFCCE8E7),
            onSecondaryContainer = Color(0xFF051F1F),
            tertiary = Color(0xFF4B607C),
            tertiaryContainer = Color(0xFFD3E4FF),
            onTertiaryContainer = Color(0xFF041C35),
        ),
        darkAccent = Accent(
            primary = Color(0xFF7BD4D8),
            onPrimary = Color(0xFF003739),
            container = Color(0xFF004F52),
            onContainer = Color(0xFF97F1F4),
            secondary = Color(0xFFB0CCCB),
            secondaryContainer = Color(0xFF324B4B),
            onSecondaryContainer = Color(0xFFCCE8E7),
            tertiary = Color(0xFFB3C8E9),
            tertiaryContainer = Color(0xFF334863),
            onTertiaryContainer = Color(0xFFD3E4FF),
        ),
        chart = listOf(
            Color(0xFF00838A), Color(0xFF17A2A8), Color(0xFF45BFC4), Color(0xFF7FD6D9),
            Color(0xFFA8E4E6), Color(0xFF4B607C), Color(0xFF6E82A0), Color(0xFF9AAAC2),
        ),
    ),

    Indigo(
        label = "Индиго",
        swatch = Color(0xFF3F51B5),
        lightAccent = Accent(
            primary = Color(0xFF3F51B5),
            onPrimary = Color(0xFFFFFFFF),
            container = Color(0xFFDDE1FF),
            onContainer = Color(0xFF00105C),
            secondary = Color(0xFF5B5D72),
            secondaryContainer = Color(0xFFE0E0F9),
            onSecondaryContainer = Color(0xFF181A2C),
            tertiary = Color(0xFF76546D),
            tertiaryContainer = Color(0xFFFFD7F1),
            onTertiaryContainer = Color(0xFF2C1228),
        ),
        darkAccent = Accent(
            primary = Color(0xFFB9C3FF),
            onPrimary = Color(0xFF001E6C),
            container = Color(0xFF263692),
            onContainer = Color(0xFFDDE1FF),
            secondary = Color(0xFFC4C5DD),
            secondaryContainer = Color(0xFF434659),
            onSecondaryContainer = Color(0xFFE0E0F9),
            tertiary = Color(0xFFE5BAD8),
            tertiaryContainer = Color(0xFF5C3D55),
            onTertiaryContainer = Color(0xFFFFD7F1),
        ),
        chart = listOf(
            Color(0xFF3F51B5), Color(0xFF5C6BC0), Color(0xFF7986CB), Color(0xFF9FA8DA),
            Color(0xFFC5CAE9), Color(0xFF76546D), Color(0xFF9A7690), Color(0xFFBFA0B5),
        ),
    ),

    Forest(
        label = "Лес",
        swatch = Color(0xFF2E6B3E),
        lightAccent = Accent(
            primary = Color(0xFF2E6B3E),
            onPrimary = Color(0xFFFFFFFF),
            container = Color(0xFFB1F1B9),
            onContainer = Color(0xFF00210A),
            secondary = Color(0xFF52634F),
            secondaryContainer = Color(0xFFD5E8CF),
            onSecondaryContainer = Color(0xFF101F10),
            tertiary = Color(0xFF39656B),
            tertiaryContainer = Color(0xFFBCEBF1),
            onTertiaryContainer = Color(0xFF001F23),
        ),
        darkAccent = Accent(
            primary = Color(0xFF96D49F),
            onPrimary = Color(0xFF00391A),
            container = Color(0xFF13512A),
            onContainer = Color(0xFFB1F1B9),
            secondary = Color(0xFFB9CCB4),
            secondaryContainer = Color(0xFF3A4B38),
            onSecondaryContainer = Color(0xFFD5E8CF),
            tertiary = Color(0xFFA1CED5),
            tertiaryContainer = Color(0xFF1F4D53),
            onTertiaryContainer = Color(0xFFBCEBF1),
        ),
        chart = listOf(
            Color(0xFF2E6B3E), Color(0xFF43855A), Color(0xFF5FA075), Color(0xFF85BC96),
            Color(0xFFAED6BA), Color(0xFF39656B), Color(0xFF60888D), Color(0xFF93B3B7),
        ),
    ),

    Sunset(
        label = "Закат",
        swatch = Color(0xFFB0431E),
        lightAccent = Accent(
            primary = Color(0xFFB0431E),
            onPrimary = Color(0xFFFFFFFF),
            container = Color(0xFFFFDBCF),
            onContainer = Color(0xFF3B0900),
            secondary = Color(0xFF77574B),
            secondaryContainer = Color(0xFFFFDBCF),
            onSecondaryContainer = Color(0xFF2C150D),
            tertiary = Color(0xFF6A5D2F),
            tertiaryContainer = Color(0xFFF3E1A7),
            onTertiaryContainer = Color(0xFF221B00),
        ),
        darkAccent = Accent(
            primary = Color(0xFFFFB59C),
            onPrimary = Color(0xFF5F1600),
            container = Color(0xFF872F0B),
            onContainer = Color(0xFFFFDBCF),
            secondary = Color(0xFFE7BEAF),
            secondaryContainer = Color(0xFF5D4035),
            onSecondaryContainer = Color(0xFFFFDBCF),
            tertiary = Color(0xFFD6C58D),
            tertiaryContainer = Color(0xFF514619),
            onTertiaryContainer = Color(0xFFF3E1A7),
        ),
        chart = listOf(
            Color(0xFFB0431E), Color(0xFFCB5B31), Color(0xFFE1774B), Color(0xFFEE9A75),
            Color(0xFFF6BFA4), Color(0xFF6A5D2F), Color(0xFF917F43), Color(0xFFB7A672),
        ),
    ),

    Slate(
        label = "Графит",
        swatch = Color(0xFF4A5B6B),
        lightAccent = Accent(
            primary = Color(0xFF3F5A70),
            onPrimary = Color(0xFFFFFFFF),
            container = Color(0xFFC6E7FF),
            onContainer = Color(0xFF001E2C),
            secondary = Color(0xFF52606D),
            secondaryContainer = Color(0xFFD5E4F1),
            onSecondaryContainer = Color(0xFF0F1D28),
            tertiary = Color(0xFF67587A),
            tertiaryContainer = Color(0xFFEDDCFF),
            onTertiaryContainer = Color(0xFF231533),
        ),
        darkAccent = Accent(
            primary = Color(0xFFA6C9E2),
            onPrimary = Color(0xFF0A3247),
            container = Color(0xFF264A5F),
            onContainer = Color(0xFFC6E7FF),
            secondary = Color(0xFFB9C8D5),
            secondaryContainer = Color(0xFF3A4855),
            onSecondaryContainer = Color(0xFFD5E4F1),
            tertiary = Color(0xFFD1BFE7),
            tertiaryContainer = Color(0xFF4F4061),
            onTertiaryContainer = Color(0xFFEDDCFF),
        ),
        chart = listOf(
            Color(0xFF3F5A70), Color(0xFF577489), Color(0xFF7590A3), Color(0xFF9AACBB),
            Color(0xFFBFC9D3), Color(0xFF67587A), Color(0xFF897C9A), Color(0xFFAFA5BC),
        ),
    ),
    ;

    fun scheme(dark: Boolean): ColorScheme = if (dark) {
        darkColorScheme(
            primary = darkAccent.primary,
            onPrimary = darkAccent.onPrimary,
            primaryContainer = darkAccent.container,
            onPrimaryContainer = darkAccent.onContainer,
            inversePrimary = lightAccent.primary,
            secondary = darkAccent.secondary,
            onSecondary = darkAccent.onPrimary,
            secondaryContainer = darkAccent.secondaryContainer,
            onSecondaryContainer = darkAccent.onSecondaryContainer,
            tertiary = darkAccent.tertiary,
            onTertiary = darkAccent.onPrimary,
            tertiaryContainer = darkAccent.tertiaryContainer,
            onTertiaryContainer = darkAccent.onTertiaryContainer,
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
    } else {
        lightColorScheme(
            primary = lightAccent.primary,
            onPrimary = lightAccent.onPrimary,
            primaryContainer = lightAccent.container,
            onPrimaryContainer = lightAccent.onContainer,
            inversePrimary = darkAccent.primary,
            secondary = lightAccent.secondary,
            onSecondary = lightAccent.onPrimary,
            secondaryContainer = lightAccent.secondaryContainer,
            onSecondaryContainer = lightAccent.onSecondaryContainer,
            tertiary = lightAccent.tertiary,
            onTertiary = lightAccent.onPrimary,
            tertiaryContainer = lightAccent.tertiaryContainer,
            onTertiaryContainer = lightAccent.onTertiaryContainer,
            error = Red40,
            onError = Neutral100,
            errorContainer = Red90,
            onErrorContainer = Red10,
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
    }

    data class Accent(
        val primary: Color,
        val onPrimary: Color,
        val container: Color,
        val onContainer: Color,
        val secondary: Color,
        val secondaryContainer: Color,
        val onSecondaryContainer: Color,
        val tertiary: Color,
        val tertiaryContainer: Color,
        val onTertiaryContainer: Color,
    )

    companion object {
        fun parse(value: String?): AppPalette =
            entries.firstOrNull { it.name == value } ?: Violet
    }
}
