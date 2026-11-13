package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================================
// Apple / iOS System Palette — Light Mode
// =====================================================================
val IosBlue = Color(0xFF007AFF)
val IosBlueDeep = Color(0xFF0056D6)
val IosBlueBright = Color(0xFF2894FF)
val IosIndigo = Color(0xFF5856D6)
val IosTeal = Color(0xFF30B0C7)
val IosGreen = Color(0xFF34C759)
val IosRed = Color(0xFFFF3B30)
val IosOrange = Color(0xFFFF9500)
val IosPink = Color(0xFFFF2D55)

val IosBlueContainerLight = Color(0xFFE3F0FF)
val IosOnBlueContainerLight = Color(0xFF002E69)
val IosRedContainerLight = Color(0xFFFFE5E4)
val IosOnRedContainerLight = Color(0xFF690005)
val IosGreenContainerLight = Color(0xFFDDF9E4)
val IosOnGreenContainerLight = Color(0xFF003914)

val IosLightGroupedBackground = Color(0xFFF2F2F7)   // systemGroupedBackground
val IosLightCardSurface = Color(0xFFFFFFFF)         // secondarySystemGroupedBackground
val IosLightBubbleIncoming = Color(0xFFE9E9EB)      // iMessage incoming gray bubble
val IosLightFill = Color(0xFFE5E5EA)                // tertiarySystemFill (search, segmented track)
val IosLightLabel = Color(0xFF1C1C1E)               // primary label
val IosLightSecondaryLabel = Color(0xFF6C6C70)      // accessible secondaryLabel
val IosLightSeparator = Color(0xFFD1D1D6)           // opaqueSeparator
val IosLightHairline = Color(0xFFE5E5EA)            // subtle card border / divider

// =====================================================================
// Apple / iOS System Palette — Dark Mode
// =====================================================================
val IosDarkBlue = Color(0xFF0A84FF)
val IosDarkBlueDeep = Color(0xFF0060DF)
val IosDarkIndigo = Color(0xFF5E5CE6)
val IosDarkTeal = Color(0xFF64D2FF)
val IosDarkGreen = Color(0xFF30D158)
val IosDarkRed = Color(0xFFFF453A)
val IosDarkOrange = Color(0xFFFF9F0A)
val IosDarkPink = Color(0xFFFF375F)

val IosBlueContainerDark = Color(0xFF0E2C59)
val IosOnBlueContainerDark = Color(0xFFD6E7FF)
val IosRedContainerDark = Color(0xFF5C1414)
val IosOnRedContainerDark = Color(0xFFFFDAD8)
val IosGreenContainerDark = Color(0xFF0B4620)
val IosOnGreenContainerDark = Color(0xFFC8F9D4)

val IosDarkBackground = Color(0xFF000000)           // True OLED iOS black
val IosDarkCardSurface = Color(0xFF1C1C1E)          // secondarySystemGroupedBackground
val IosDarkBubbleIncoming = Color(0xFF2C2C2E)       // iMessage dark incoming gray bubble
val IosDarkFill = Color(0xFF2C2C2E)                 // tertiarySystemFill
val IosDarkLabel = Color(0xFFFFFFFF)                // primary label
val IosDarkSecondaryLabel = Color(0xFFAEAEB2)       // secondaryLabel
val IosDarkSeparator = Color(0xFF48484A)
val IosDarkHairline = Color(0xFF2C2C2E)

// Semantic Aliases
val OnlinePresenceGreen = IosGreen
val AwayPresenceAmber = IosOrange
val ErrorRed = IosRed
val ErrorContainerLight = IosRedContainerLight
val ErrorContainerDark = IosRedContainerDark

// iOS-Inspired Gradients & Brushes
object PulseBrushes {
    val BrandGradient: Brush = Brush.linearGradient(
        colors = listOf(IosBlueBright, IosBlue, IosIndigo)
    )

    val OutgoingBubbleLight: Brush = Brush.verticalGradient(
        colors = listOf(IosBlueBright, IosBlue)
    )

    val OutgoingBubbleDark: Brush = Brush.verticalGradient(
        colors = listOf(IosDarkBlue, IosDarkBlueDeep)
    )

    val StoryRingActive: Brush = Brush.sweepGradient(
        colors = listOf(IosBlue, IosTeal, IosIndigo, IosPink, IosBlue)
    )

    val UnreadBadgeGradient: Brush = Brush.linearGradient(
        colors = listOf(IosRed, IosPink)
    )

    val SavedNotesGradientLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFEAF3FF), Color(0xFFF3EEFF))
    )

    val SavedNotesGradientDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF14223B), Color(0xFF231C3A))
    )
}
