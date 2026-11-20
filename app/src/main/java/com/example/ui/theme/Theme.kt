package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class IosSurfaceColors(
    val groupedBackground: Color,
    val elevatedCard: Color,
    val secondaryCard: Color,
    val frostedGlass: Color,
    val separatorHairline: Color,
    val inputUnderline: Color
)

private val LightIosSurfaces = IosSurfaceColors(
    groupedBackground = Color(0xFFFFFFFF),
    elevatedCard = Color(0xFFF7F8FA),
    secondaryCard = Color(0xFFEDEFF3),
    frostedGlass = Color(0xF5FFFFFF),
    separatorHairline = Color(0xFFE6E8EE),
    inputUnderline = Color(0xFFD1D5DE)
)

private val DarkIosSurfaces = IosSurfaceColors(
    groupedBackground = Color(0xFF090B0E),
    elevatedCard = Color(0xFF14161B),
    secondaryCard = Color(0xFF1E2128),
    frostedGlass = Color(0xF0121419),
    separatorHairline = Color(0xFF232730),
    inputUnderline = Color(0xFF303540)
)

val LocalIsTelegramDark = compositionLocalOf { true }
val LocalUniversAccent = compositionLocalOf { UniversAccentTheme.OCEAN }
val LocalIosSurfaces = compositionLocalOf { DarkIosSurfaces }

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = TelegramDarkBlue,
    onPrimary = Color.White,
    primaryContainer = IosBlueContainerDark,
    onPrimaryContainer = IosOnBlueContainerDark,
    secondary = Color(0xFF8A909E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E2128),
    onSecondaryContainer = Color(0xFFD8DCE3),
    tertiary = TelegramGreen,
    onTertiary = Color.White,
    tertiaryContainer = IosGreenContainerDark,
    onTertiaryContainer = IosOnGreenContainerDark,
    background = TelegramDarkBackground,
    onBackground = TelegramDarkLabel,
    surface = TelegramDarkCardSurface,
    onSurface = TelegramDarkLabel,
    surfaceVariant = TelegramDarkFill,
    onSurfaceVariant = TelegramDarkSecondaryLabel,
    surfaceDim = Color(0xFF090B0E),
    surfaceBright = Color(0xFF22262E),
    surfaceContainerLowest = Color(0xFF090B0E),
    surfaceContainerLow = Color(0xFF0F1116),
    surfaceContainer = Color(0xFF14161B),
    surfaceContainerHigh = Color(0xFF1B1E24),
    surfaceContainerHighest = Color(0xFF242830),
    outline = Color(0xFF343945),
    outlineVariant = TelegramDarkDivider,
    error = IosDarkRed,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = IosOnRedContainerDark
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = TelegramBlueContainerLight,
    onPrimaryContainer = TelegramOnBlueContainerLight,
    secondary = Color(0xFF5A6270),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBEEF2),
    onSecondaryContainer = Color(0xFF232730),
    tertiary = Color(0xFF28A745),
    onTertiary = Color.White,
    tertiaryContainer = TelegramGreenContainerLight,
    onTertiaryContainer = TelegramOnGreenContainerLight,
    background = TelegramLightBackground,
    onBackground = TelegramLightLabel,
    surface = TelegramLightCardSurface,
    onSurface = TelegramLightLabel,
    surfaceVariant = TelegramLightFill,
    onSurfaceVariant = TelegramLightSecondaryLabel,
    surfaceDim = Color(0xFFE6E9EF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainer = Color(0xFFF5F6F8),
    surfaceContainerHigh = Color(0xFFEDEFF3),
    surfaceContainerHighest = Color(0xFFE4E7ED),
    outline = Color(0xFFC8CDD6),
    outlineVariant = TelegramLightDivider,
    error = TelegramRed,
    onError = Color.White,
    errorContainer = TelegramRedContainerLight,
    onErrorContainer = TelegramOnRedContainerLight
)

val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: UniversAccentTheme = UniversAccentTheme.OCEAN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dynamic.copy(
            background = baseScheme.background,
            onBackground = baseScheme.onBackground,
            surface = baseScheme.surface,
            onSurface = baseScheme.onSurface,
            surfaceVariant = baseScheme.surfaceVariant,
            onSurfaceVariant = baseScheme.onSurfaceVariant,
            surfaceDim = baseScheme.surfaceDim,
            surfaceBright = baseScheme.surfaceBright,
            surfaceContainerLowest = baseScheme.surfaceContainerLowest,
            surfaceContainerLow = baseScheme.surfaceContainerLow,
            surfaceContainer = baseScheme.surfaceContainer,
            surfaceContainerHigh = baseScheme.surfaceContainerHigh,
            surfaceContainerHighest = baseScheme.surfaceContainerHighest,
            outlineVariant = baseScheme.outlineVariant
        )
    } else {
        baseScheme
    }

    val iosSurfaces = if (darkTheme) DarkIosSurfaces else LightIosSurfaces

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalIsTelegramDark provides darkTheme,
        LocalUniversAccent provides accentTheme,
        LocalIosSurfaces provides iosSurfaces
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = PulseShapes,
            content = content
        )
    }
}

@Composable
fun PulseChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: UniversAccentTheme = UniversAccentTheme.OCEAN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AppTheme(
        darkTheme = darkTheme,
        accentTheme = accentTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
