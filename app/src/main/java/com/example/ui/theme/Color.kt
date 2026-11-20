package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

// =====================================================================
// Single Restrained Brand Accent — Zero Gradients, Zero Rainbow Themes
// =====================================================================
@Immutable
enum class UniversAccentTheme(
    val label: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryAccent: Color,
    val containerLight: Color,
    val onContainerLight: Color,
    val containerDark: Color,
    val onContainerDark: Color
) {
    OCEAN(
        label = "Default",
        primaryLight = Color(0xFF2B5CD9),
        primaryDark = Color(0xFF4C82F7),
        secondaryAccent = Color(0xFF5E6673),
        containerLight = Color(0xFFEBEEF2),
        onContainerLight = Color(0xFF111318),
        containerDark = Color(0xFF1D2129),
        onContainerDark = Color(0xFFF4F5F7)
    );

    fun primary(isDark: Boolean): Color = if (isDark) primaryDark else primaryLight

    fun brandGradient(isDark: Boolean): Brush = SolidColor(primary(isDark))

    fun outgoingBubbleBrush(isDark: Boolean): Brush = SolidColor(
        if (isDark) TelegramOutgoingBubbleDark else primaryLight
    )
}

// =====================================================================
// True Neutral Light Palette — Clean Native Surface Hierarchy
// =====================================================================
val TelegramBlue = Color(0xFF2B5CD9)
val TelegramBlueDeep = Color(0xFF1E46B0)
val TelegramBlueBright = Color(0xFF4C82F7)
val TelegramSteelBlue = Color(0xFF3A5A80)
val TelegramSteelBlueDark = Color(0xFF28415E)

val TelegramIndigo = Color(0xFF4F5D8C)
val TelegramTeal = Color(0xFF356B75)
val TelegramGreen = Color(0xFF22A947)
val TelegramTickGreen = Color(0xFF22A947)
val TelegramRed = Color(0xFFDC2626)
val TelegramOrange = Color(0xFFD97706)
val TelegramPink = Color(0xFFB8546C)
val TelegramViolet = Color(0xFF5C588C)

val TelegramBlueContainerLight = Color(0xFFEBEEF2)
val TelegramOnBlueContainerLight = Color(0xFF111318)
val TelegramRedContainerLight = Color(0xFFFDECEC)
val TelegramOnRedContainerLight = Color(0xFF8C1D18)
val TelegramGreenContainerLight = Color(0xFFE8F5EC)
val TelegramOnGreenContainerLight = Color(0xFF14532D)

val TelegramLightBackground = Color(0xFFFFFFFF)
val TelegramLightGroupedBackground = Color(0xFFF4F5F7)
val TelegramLightCardSurface = Color(0xFFF7F8FA)
val TelegramLightFill = Color(0xFFEDEFF3)
val TelegramLightLabel = Color(0xFF111318)
val TelegramLightSecondaryLabel = Color(0xFF666D7A)
val TelegramLightDivider = Color(0xFFE6E8EE)

val TelegramOutgoingBubbleLight = Color(0xFF1E222B)
val TelegramOutgoingTextLight = Color(0xFFFFFFFF)
val TelegramOutgoingSubtleLight = Color(0xFFD0D5DD)
val TelegramIncomingBubbleLight = Color(0xFFF2F4F7)

// =====================================================================
// True Neutral Dark Palette — Matte Charcoal & Crisp Contrast
// =====================================================================
val TelegramDarkBlue = Color(0xFF4C82F7)
val TelegramDarkBlueDeep = Color(0xFF2B5CD9)
val TelegramDarkBar = Color(0xFF0E1015)
val TelegramDarkBackground = Color(0xFF090B0E)
val TelegramDarkCardSurface = Color(0xFF13151A)
val TelegramDarkChatCanvas = Color(0xFF090B0E)
val TelegramDarkFill = Color(0xFF1C1F26)
val TelegramDarkLabel = Color(0xFFF4F5F7)
val TelegramDarkSecondaryLabel = Color(0xFF8A909E)
val TelegramDarkDivider = Color(0xFF22262F)

val TelegramOutgoingBubbleDark = Color(0xFF1F232C)
val TelegramOutgoingSubtleDark = Color(0xFF838B99)
val TelegramIncomingBubbleDark = Color(0xFF1F232C)

// =====================================================================
// Token Aliases (Semantic & Compatibility)
// =====================================================================
val IosBlue = TelegramBlue
val IosBlueDeep = TelegramBlueDeep
val IosBlueBright = TelegramBlueBright
val IosIndigo = TelegramIndigo
val IosTeal = TelegramTeal
val IosGreen = TelegramGreen
val IosRed = Color(0xFFE5484D)
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
val IosDarkRed = Color(0xFFE5484D)
val IosDarkOrange = Color(0xFFF59E0B)
val IosDarkPink = Color(0xFFD9778A)

val IosBlueContainerDark = Color(0xFF1D2129)
val IosOnBlueContainerDark = Color(0xFFF4F5F7)
val IosRedContainerDark = Color(0xFF2D1618)
val IosOnRedContainerDark = Color(0xFFFFC9C9)
val IosGreenContainerDark = Color(0xFF13261A)
val IosOnGreenContainerDark = Color(0xFFB7F0CA)

val IosDarkBackground = TelegramDarkBackground
val IosDarkCardSurface = TelegramDarkCardSurface
val IosDarkBubbleIncoming = TelegramIncomingBubbleDark
val IosDarkFill = TelegramDarkFill
val IosDarkLabel = TelegramDarkLabel
val IosDarkSecondaryLabel = TelegramDarkSecondaryLabel
val IosDarkSeparator = TelegramDarkDivider
val IosDarkHairline = TelegramDarkDivider

// Semantic Aliases
val OnlinePresenceGreen = TelegramGreen
val AwayPresenceAmber = TelegramOrange
val ErrorRed = IosRed
val ErrorContainerLight = TelegramRedContainerLight
val ErrorContainerDark = IosRedContainerDark

// Call Stage Neutral Surfaces
val DynamicIslandDarkSurface = Color(0xFF15181E)
val DynamicIslandDarkBorder = Color(0xFF282C36)
val AuroraCyan = Color(0xFF0A84FF)
val AuroraViolet = Color(0xFF4F5D8C)

// Muted Neutral Fallback Avatar Palette (Flat Solid Tones)
val AvatarToneSlate = Color(0xFF475569)
val AvatarToneSteelBlue = Color(0xFF3B5B84)
val AvatarToneSage = Color(0xFF3E6B5C)
val AvatarToneClay = Color(0xFF7A564C)
val AvatarToneIndigo = Color(0xFF4B5580)
val AvatarToneTeal = Color(0xFF366670)

// Neutralized Badge Aliases (Only used if referenced externally)
val BadgeSapphire = TelegramBlue
val BadgeCoral = IosRed
val BadgeEmerald = TelegramGreen
val BadgeAmber = TelegramOrange
val BadgeCyan = TelegramSteelBlue
val BadgeSlateBlue = TelegramIndigo
val BadgeRose = IosRed

object PulseBrushes {
    val BrandGradient: Brush = SolidColor(TelegramBlue)
    val OutgoingBubbleLight: Brush = SolidColor(TelegramOutgoingBubbleLight)
    val OutgoingBubbleDark: Brush = SolidColor(TelegramOutgoingBubbleDark)
    val UnreadBadgeGradient: Brush = SolidColor(TelegramBlue)
    val SavedNotesGradientLight: Brush = SolidColor(TelegramLightCardSurface)
    val SavedNotesGradientDark: Brush = SolidColor(TelegramDarkCardSurface)
    val SpotlightCardLight: Brush = SolidColor(TelegramLightCardSurface)
    val SpotlightCardDark: Brush = SolidColor(TelegramDarkCardSurface)
}
