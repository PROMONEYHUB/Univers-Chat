package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalIsTelegramDark = compositionLocalOf { false }
val LocalUniversAccent = compositionLocalOf { UniversAccentTheme.OCEAN }

private fun buildDarkColorScheme(accent: UniversAccentTheme): ColorScheme = darkColorScheme(
    primary = accent.primaryDark,
    onPrimary = Color(0xFF04131E),
    primaryContainer = accent.containerDark,
    onPrimaryContainer = accent.onContainerDark,
    secondary = accent.secondaryAccent,
    onSecondary = Color(0xFF041A17),
    secondaryContainer = Color(0xFF112634),
    onSecondaryContainer = Color(0xFFCFFAFE),
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
    outline = Color(0xFF2E3C50),
    outlineVariant = IosDarkHairline,
    error = IosDarkRed,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = IosOnRedContainerDark
)

private fun buildLightColorScheme(accent: UniversAccentTheme): ColorScheme = lightColorScheme(
    primary = accent.primaryLight,
    onPrimary = Color.White,
    primaryContainer = accent.containerLight,
    onPrimaryContainer = accent.onContainerLight,
    secondary = accent.secondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F6F4),
    onSecondaryContainer = Color(0xFF064E46),
    tertiary = TelegramGreen,
    onTertiary = Color.White,
    tertiaryContainer = TelegramGreenContainerLight,
    onTertiaryContainer = TelegramOnGreenContainerLight,
    background = TelegramLightBackground,
    onBackground = TelegramLightLabel,
    surface = TelegramLightCardSurface,
    onSurface = TelegramLightLabel,
    surfaceVariant = TelegramLightFill,
    onSurfaceVariant = TelegramLightSecondaryLabel,
    outline = Color(0xFFCBD5E1),
    outlineVariant = TelegramLightDivider,
    error = TelegramRed,
    onError = Color.White,
    errorContainer = TelegramRedContainerLight,
    onErrorContainer = TelegramOnRedContainerLight
)

val PulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulseChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: UniversAccentTheme = UniversAccentTheme.OCEAN,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(accentTheme)
    } else {
        buildLightColorScheme(accentTheme)
    }

    CompositionLocalProvider(
        LocalIsTelegramDark provides darkTheme,
        LocalUniversAccent provides accentTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = PulseShapes,
            content = content
        )
    }
}
