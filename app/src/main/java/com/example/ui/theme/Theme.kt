package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramDarkBlue,
    onPrimary = Color.White,
    primaryContainer = IosBlueContainerDark,
    onPrimaryContainer = IosOnBlueContainerDark,
    secondary = TelegramBlueBright,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3247),
    onSecondaryContainer = Color(0xFFD1E9FF),
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
    outline = Color(0xFF334357),
    outlineVariant = IosDarkHairline,
    error = IosDarkRed,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = IosOnRedContainerDark
)

private val TelegramLightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    onPrimary = Color.White,
    primaryContainer = TelegramBlueContainerLight,
    onPrimaryContainer = TelegramOnBlueContainerLight,
    secondary = TelegramSteelBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EEF5),
    onSecondaryContainer = Color(0xFF203A52),
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
    outline = Color(0xFFC5CCD4),
    outlineVariant = TelegramLightDivider,
    error = TelegramRed,
    onError = Color.White,
    errorContainer = TelegramRedContainerLight,
    onErrorContainer = TelegramOnRedContainerLight
)

val PulseShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun PulseChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TelegramDarkColorScheme else TelegramLightColorScheme

    CompositionLocalProvider(LocalIsTelegramDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = PulseShapes,
            content = content
        )
    }
}
