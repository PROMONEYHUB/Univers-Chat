package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================================
// Telegram Official Palette — Day (Light Mode)
// =====================================================================
val TelegramBlue = Color(0xFF3390EC)
val TelegramBlueDeep = Color(0xFF2278D4)
val TelegramBlueBright = Color(0xFF2AABEE)
val TelegramSteelBlue = Color(0xFF517DA2)
val TelegramSteelBlueDark = Color(0xFF426A8D)

val TelegramIndigo = Color(0xFF5C6BC0)
val TelegramTeal = Color(0xFF26A69A)
val TelegramGreen = Color(0xFF4DCD5E)
val TelegramTickGreen = Color(0xFF4FAE4E)
val TelegramRed = Color(0xFFE53935)
val TelegramOrange = Color(0xFFFB8C00)
val TelegramPink = Color(0xFFEC407A)
val TelegramViolet = Color(0xFF8E24AA)

val TelegramBlueContainerLight = Color(0xFFE3F2FD)
val TelegramOnBlueContainerLight = Color(0xFF0D47A1)
val TelegramRedContainerLight = Color(0xFFFFEBEE)
val TelegramOnRedContainerLight = Color(0xFF7F1D1D)
val TelegramGreenContainerLight = Color(0xFFE8F5E9)
val TelegramOnGreenContainerLight = Color(0xFF1B5E20)

val TelegramLightBackground = Color(0xFFFFFFFF)
val TelegramLightGroupedBackground = Color(0xFFF0F2F5)
val TelegramLightCardSurface = Color(0xFFFFFFFF)
val TelegramLightFill = Color(0xFFEFF1F5)
val TelegramLightLabel = Color(0xFF222222)
val TelegramLightSecondaryLabel = Color(0xFF707579)
val TelegramLightDivider = Color(0xFFE4E7EB)

val TelegramOutgoingBubbleLight = Color(0xFFEEFFDE)
val TelegramOutgoingTextLight = Color(0xFF151C14)
val TelegramOutgoingSubtleLight = Color(0xFF5B7D56)
val TelegramIncomingBubbleLight = Color(0xFFFFFFFF)

// =====================================================================
// Telegram Official Palette — Night (Dark Mode)
// =====================================================================
val TelegramDarkBlue = Color(0xFF64B5F6)
val TelegramDarkBlueDeep = Color(0xFF3E8AD6)
val TelegramDarkBar = Color(0xFF212D3B)
val TelegramDarkBackground = Color(0xFF17212B)
val TelegramDarkCardSurface = Color(0xFF1D2733)
val TelegramDarkChatCanvas = Color(0xFF0E1621)
val TelegramDarkFill = Color(0xFF242F3D)
val TelegramDarkLabel = Color(0xFFF5F5F5)
val TelegramDarkSecondaryLabel = Color(0xFF7F91A4)
val TelegramDarkDivider = Color(0xFF101921)

val TelegramOutgoingBubbleDark = Color(0xFF2B5278)
val TelegramOutgoingSubtleDark = Color(0xFF8AB4DF)
val TelegramIncomingBubbleDark = Color(0xFF182533)

// =====================================================================
// Backward-Compatible Token Aliases (mapped cleanly to Telegram palette)
// =====================================================================
val IosBlue = TelegramBlue
val IosBlueDeep = TelegramBlueDeep
val IosBlueBright = TelegramBlueBright
val IosIndigo = TelegramIndigo
val IosTeal = TelegramTeal
val IosGreen = TelegramGreen
val IosRed = TelegramRed
val IosOrange = TelegramOrange
val IosPink = TelegramPink

val IosBlueContainerLight = TelegramBlueContainerLight
val IosOnBlueContainerLight = TelegramOnBlueContainerLight
val IosRedContainerLight = TelegramRedContainerLight
val IosOnRedContainerLight = TelegramOnRedContainerLight
val IosGreenContainerLight = TelegramGreenContainerLight
val IosOnGreenContainerLight = TelegramOnGreenContainerLight

val IosLightGroupedBackground = TelegramLightGroupedBackground
val IosLightCardSurface = TelegramLightCardSurface
val IosLightBubbleIncoming = TelegramIncomingBubbleLight
val IosLightFill = TelegramLightFill
val IosLightLabel = TelegramLightLabel
val IosLightSecondaryLabel = TelegramLightSecondaryLabel
val IosLightSeparator = TelegramLightDivider
val IosLightHairline = TelegramLightDivider

val IosDarkBlue = TelegramDarkBlue
val IosDarkBlueDeep = TelegramDarkBlueDeep
val IosDarkIndigo = TelegramIndigo
val IosDarkTeal = TelegramTeal
val IosDarkGreen = TelegramGreen
val IosDarkRed = Color(0xFFEF5350)
val IosDarkOrange = Color(0xFFFFA726)
val IosDarkPink = Color(0xFFF06292)

val IosBlueContainerDark = Color(0xFF1D3550)
val IosOnBlueContainerDark = Color(0xFFD6ECFF)
val IosRedContainerDark = Color(0xFF4E1B1E)
val IosOnRedContainerDark = Color(0xFFFFCDD2)
val IosGreenContainerDark = Color(0xFF163A24)
val IosOnGreenContainerDark = Color(0xFFC8E6C9)

val IosDarkBackground = TelegramDarkBackground
val IosDarkCardSurface = TelegramDarkCardSurface
val IosDarkBubbleIncoming = TelegramIncomingBubbleDark
val IosDarkFill = TelegramDarkFill
val IosDarkLabel = TelegramDarkLabel
val IosDarkSecondaryLabel = TelegramDarkSecondaryLabel
val IosDarkSeparator = TelegramDarkDivider
val IosDarkHairline = Color(0xFF233040)

// Semantic Aliases
val OnlinePresenceGreen = TelegramGreen
val AwayPresenceAmber = TelegramOrange
val ErrorRed = TelegramRed
val ErrorContainerLight = TelegramRedContainerLight
val ErrorContainerDark = IosRedContainerDark

// Call Stage & Accent Surface Tokens
val DynamicIslandDarkSurface = Color(0xFF17212B)
val DynamicIslandDarkBorder = Color(0xFF2B3A4D)
val AuroraCyan = Color(0xFF2AABEE)
val AuroraViolet = Color(0xFF3390EC)

// Google Sign-In Mark Tokens
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

object PulseBrushes {
    val BrandGradient: Brush = Brush.linearGradient(
        colors = listOf(TelegramBlueBright, TelegramBlue)
    )

    val OutgoingBubbleLight: Brush = Brush.verticalGradient(
        colors = listOf(TelegramOutgoingBubbleLight, TelegramOutgoingBubbleLight)
    )

    val OutgoingBubbleDark: Brush = Brush.verticalGradient(
        colors = listOf(TelegramOutgoingBubbleDark, TelegramOutgoingBubbleDark)
    )

    val StoryRingActive: Brush = Brush.sweepGradient(
        colors = listOf(TelegramBlueBright, TelegramBlue, TelegramGreen, TelegramBlueBright)
    )

    val UnreadBadgeGradient: Brush = Brush.linearGradient(
        colors = listOf(TelegramBlue, TelegramBlueBright)
    )

    val SavedNotesGradientLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFEAF5FF), Color(0xFFF2F8FF))
    )

    val SavedNotesGradientDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF1B2E45), Color(0xFF182533))
    )

    val SpotlightCardLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFF5F9FF), Color(0xFFFFFFFF))
    )

    val SpotlightCardDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF1D2733), Color(0xFF17212B))
    )
}
