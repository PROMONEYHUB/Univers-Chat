package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================================
// Univers Chat Signature Palette — Daylight (Porcelain & Electric Indigo)
// =====================================================================
val TelegramBlue = Color(0xFF5B5CE6)
val TelegramBlueDeep = Color(0xFF4338CA)
val TelegramBlueBright = Color(0xFF22D3EE)
val TelegramSteelBlue = Color(0xFF4F46E5)
val TelegramSteelBlueDark = Color(0xFF3730A3)

val TelegramIndigo = Color(0xFF6366F1)
val TelegramTeal = Color(0xFF06B6D4)
val TelegramGreen = Color(0xFF10B981)
val TelegramTickGreen = Color(0xFF34D399)
val TelegramRed = Color(0xFFEF4444)
val TelegramOrange = Color(0xFFF59E0B)
val TelegramPink = Color(0xFFEC4899)
val TelegramViolet = Color(0xFF8B5CF6)

val TelegramBlueContainerLight = Color(0xFFEEF0FF)
val TelegramOnBlueContainerLight = Color(0xFF2E2A8A)
val TelegramRedContainerLight = Color(0xFFFFEBEE)
val TelegramOnRedContainerLight = Color(0xFF7F1D1D)
val TelegramGreenContainerLight = Color(0xFFE6F9F1)
val TelegramOnGreenContainerLight = Color(0xFF065F46)

val TelegramLightBackground = Color(0xFFF5F6FA)
val TelegramLightGroupedBackground = Color(0xFFECEEF6)
val TelegramLightCardSurface = Color(0xFFFFFFFF)
val TelegramLightFill = Color(0xFFECEFF7)
val TelegramLightLabel = Color(0xFF111322)
val TelegramLightSecondaryLabel = Color(0xFF646881)
val TelegramLightDivider = Color(0xFFE3E6F0)

val TelegramOutgoingBubbleLight = Color(0xFF5B5CE6)
val TelegramOutgoingTextLight = Color(0xFFFFFFFF)
val TelegramOutgoingSubtleLight = Color(0xFFE0E4FF)
val TelegramIncomingBubbleLight = Color(0xFFFFFFFF)

// =====================================================================
// Univers Chat Signature Palette — Midnight (Deep Obsidian & Violet Glow)
// =====================================================================
val TelegramDarkBlue = Color(0xFF818CF8)
val TelegramDarkBlueDeep = Color(0xFF4F46E5)
val TelegramDarkBar = Color(0xFF121324)
val TelegramDarkBackground = Color(0xFF0A0B14)
val TelegramDarkCardSurface = Color(0xFF131526)
val TelegramDarkChatCanvas = Color(0xFF080912)
val TelegramDarkFill = Color(0xFF1B1E36)
val TelegramDarkLabel = Color(0xFFF3F4FA)
val TelegramDarkSecondaryLabel = Color(0xFF9498B4)
val TelegramDarkDivider = Color(0xFF1E213A)

val TelegramOutgoingBubbleDark = Color(0xFF5B4FE9)
val TelegramOutgoingSubtleDark = Color(0xFFD8DAFF)
val TelegramIncomingBubbleDark = Color(0xFF17192E)

// =====================================================================
// Backward-Compatible Token Aliases
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
val IosDarkRed = Color(0xFFF87171)
val IosDarkOrange = Color(0xFFFBBF24)
val IosDarkPink = Color(0xFFF472B6)

val IosBlueContainerDark = Color(0xFF1E1D46)
val IosOnBlueContainerDark = Color(0xFFE0E2FF)
val IosRedContainerDark = Color(0xFF4C191F)
val IosOnRedContainerDark = Color(0xFFFFCDD2)
val IosGreenContainerDark = Color(0xFF0E3728)
val IosOnGreenContainerDark = Color(0xFFBBF7D0)

val IosDarkBackground = TelegramDarkBackground
val IosDarkCardSurface = TelegramDarkCardSurface
val IosDarkBubbleIncoming = TelegramIncomingBubbleDark
val IosDarkFill = TelegramDarkFill
val IosDarkLabel = TelegramDarkLabel
val IosDarkSecondaryLabel = TelegramDarkSecondaryLabel
val IosDarkSeparator = TelegramDarkDivider
val IosDarkHairline = Color(0xFF232744)

// Semantic Aliases
val OnlinePresenceGreen = TelegramGreen
val AwayPresenceAmber = TelegramOrange
val ErrorRed = TelegramRed
val ErrorContainerLight = TelegramRedContainerLight
val ErrorContainerDark = IosRedContainerDark

// Call Stage & Accent Surface Tokens
val DynamicIslandDarkSurface = Color(0xFF111224)
val DynamicIslandDarkBorder = Color(0xFF262A4C)
val AuroraCyan = Color(0xFF22D3EE)
val AuroraViolet = Color(0xFF7C3AED)

// Google Sign-In Mark Tokens
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

object PulseBrushes {
    val BrandGradient: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF7C3AED))
    )

    val OutgoingBubbleLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF7C3AED))
    )

    val OutgoingBubbleDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF5B54EC), Color(0xFF7E3AF2))
    )

    val StoryRingActive: Brush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF22D3EE),
            Color(0xFF6366F1),
            Color(0xFFA855F7),
            Color(0xFF10B981),
            Color(0xFF22D3EE)
        )
    )

    val UnreadBadgeGradient: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    val SavedNotesGradientLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFEEF0FF), Color(0xFFF6F5FF))
    )

    val SavedNotesGradientDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF1E1D46), Color(0xFF13142B))
    )

    val SpotlightCardLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFF4F3FF), Color(0xFFFFFFFF))
    )

    val SpotlightCardDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF181A33), Color(0xFF101224))
    )
}
