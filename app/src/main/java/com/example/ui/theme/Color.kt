package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================================
// Curated Live Accent Palettes — Flagship Consumer Messenger Colors
// =====================================================================
@Immutable
enum class UniversAccentTheme(
    val label: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryAccent: Color,
    val gradientStartLight: Color,
    val gradientEndLight: Color,
    val gradientStartDark: Color,
    val gradientEndDark: Color,
    val containerLight: Color,
    val onContainerLight: Color,
    val containerDark: Color,
    val onContainerDark: Color
) {
    OCEAN(
        label = "Ocean",
        primaryLight = Color(0xFF007AFF),
        primaryDark = Color(0xFF38BDF8),
        secondaryAccent = Color(0xFF00C6A7),
        gradientStartLight = Color(0xFF007AFF),
        gradientEndLight = Color(0xFF00B4D8),
        gradientStartDark = Color(0xFF0284C7),
        gradientEndDark = Color(0xFF0D9488),
        containerLight = Color(0xFFE6F4FF),
        onContainerLight = Color(0xFF004578),
        containerDark = Color(0xFF0D2840),
        onContainerDark = Color(0xFFBAE6FD)
    ),
    EMERALD(
        label = "Emerald",
        primaryLight = Color(0xFF059669),
        primaryDark = Color(0xFF34D399),
        secondaryAccent = Color(0xFF06B6D4),
        gradientStartLight = Color(0xFF059669),
        gradientEndLight = Color(0xFF14B8A6),
        gradientStartDark = Color(0xFF047857),
        gradientEndDark = Color(0xFF0F766E),
        containerLight = Color(0xFFE6F9F1),
        onContainerLight = Color(0xFF065F46),
        containerDark = Color(0xFF0B2E24),
        onContainerDark = Color(0xFFA7F3D0)
    ),
    CORAL(
        label = "Coral",
        primaryLight = Color(0xFFE11D48),
        primaryDark = Color(0xFFFB7185),
        secondaryAccent = Color(0xFFFF7A45),
        gradientStartLight = Color(0xFFE11D48),
        gradientEndLight = Color(0xFFFF6B4A),
        gradientStartDark = Color(0xFFBE123C),
        gradientEndDark = Color(0xFFEA580C),
        containerLight = Color(0xFFFFECEF),
        onContainerLight = Color(0xFF881337),
        containerDark = Color(0xFF3A1220),
        onContainerDark = Color(0xFFFECDD3)
    ),
    AMBER(
        label = "Amber",
        primaryLight = Color(0xFFD97706),
        primaryDark = Color(0xFFFBBF24),
        secondaryAccent = Color(0xFFF97316),
        gradientStartLight = Color(0xFFD97706),
        gradientEndLight = Color(0xFFF97316),
        gradientStartDark = Color(0xFFB45309),
        gradientEndDark = Color(0xFFEA580C),
        containerLight = Color(0xFFFFF5E0),
        onContainerLight = Color(0xFF78350F),
        containerDark = Color(0xFF36220B),
        onContainerDark = Color(0xFFFDE68A)
    ),
    COBALT(
        label = "Cobalt",
        primaryLight = Color(0xFF2563EB),
        primaryDark = Color(0xFF60A5FA),
        secondaryAccent = Color(0xFFEC4899),
        gradientStartLight = Color(0xFF2563EB),
        gradientEndLight = Color(0xFF6366F1),
        gradientStartDark = Color(0xFF1D4ED8),
        gradientEndDark = Color(0xFF4F46E5),
        containerLight = Color(0xFFEBF2FF),
        onContainerLight = Color(0xFF1E3A8A),
        containerDark = Color(0xFF112247),
        onContainerDark = Color(0xFFBFDBFE)
    );

    fun primary(isDark: Boolean): Color = if (isDark) primaryDark else primaryLight

    fun brandGradient(isDark: Boolean): Brush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(gradientStartDark, gradientEndDark)
        } else {
            listOf(gradientStartLight, gradientEndLight)
        }
    )

    fun outgoingBubbleBrush(isDark: Boolean): Brush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(gradientStartDark, gradientEndDark)
        } else {
            listOf(gradientStartLight, gradientEndLight)
        }
    )
}

// =====================================================================
// Univers Chat Signature Palette — Crisp Porcelain & Electric Sapphire
// =====================================================================
val TelegramBlue = Color(0xFF007AFF)
val TelegramBlueDeep = Color(0xFF005EC4)
val TelegramBlueBright = Color(0xFF00C6A7)
val TelegramSteelBlue = Color(0xFF0284C7)
val TelegramSteelBlueDark = Color(0xFF0369A1)

val TelegramIndigo = Color(0xFF2563EB)
val TelegramTeal = Color(0xFF0D9488)
val TelegramGreen = Color(0xFF10B981)
val TelegramTickGreen = Color(0xFF34D399)
val TelegramRed = Color(0xFFFF3B30)
val TelegramOrange = Color(0xFFFF9500)
val TelegramPink = Color(0xFFFF2D55)
val TelegramViolet = Color(0xFF5856D6)

val TelegramBlueContainerLight = Color(0xFFE6F4FF)
val TelegramOnBlueContainerLight = Color(0xFF004578)
val TelegramRedContainerLight = Color(0xFFFFEBEE)
val TelegramOnRedContainerLight = Color(0xFF7F1D1D)
val TelegramGreenContainerLight = Color(0xFFE6F9F1)
val TelegramOnGreenContainerLight = Color(0xFF065F46)

val TelegramLightBackground = Color(0xFFF3F6FA)
val TelegramLightGroupedBackground = Color(0xFFEBF0F6)
val TelegramLightCardSurface = Color(0xFFFFFFFF)
val TelegramLightFill = Color(0xFFE9EFF6)
val TelegramLightLabel = Color(0xFF0F172A)
val TelegramLightSecondaryLabel = Color(0xFF5E6C84)
val TelegramLightDivider = Color(0xFFE2E8F0)

val TelegramOutgoingBubbleLight = Color(0xFF007AFF)
val TelegramOutgoingTextLight = Color(0xFFFFFFFF)
val TelegramOutgoingSubtleLight = Color(0xFFE0F2FE)
val TelegramIncomingBubbleLight = Color(0xFFFFFFFF)

// =====================================================================
// Univers Chat Signature Palette — True OLED Obsidian & Luminous Azure
// =====================================================================
val TelegramDarkBlue = Color(0xFF38BDF8)
val TelegramDarkBlueDeep = Color(0xFF0284C7)
val TelegramDarkBar = Color(0xFF0E141D)
val TelegramDarkBackground = Color(0xFF090C11)
val TelegramDarkCardSurface = Color(0xFF121821)
val TelegramDarkChatCanvas = Color(0xFF070A0F)
val TelegramDarkFill = Color(0xFF19222E)
val TelegramDarkLabel = Color(0xFFF1F5F9)
val TelegramDarkSecondaryLabel = Color(0xFF8B9BB4)
val TelegramDarkDivider = Color(0xFF1E293B)

val TelegramOutgoingBubbleDark = Color(0xFF0284C7)
val TelegramOutgoingSubtleDark = Color(0xFFCCFBF1)
val TelegramIncomingBubbleDark = Color(0xFF161F2B)

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
val IosDarkRed = Color(0xFFFF6961)
val IosDarkOrange = Color(0xFFFBBF24)
val IosDarkPink = Color(0xFFF472B6)

val IosBlueContainerDark = Color(0xFF0D2840)
val IosOnBlueContainerDark = Color(0xFFBAE6FD)
val IosRedContainerDark = Color(0xFF3E141B)
val IosOnRedContainerDark = Color(0xFFFFCDD2)
val IosGreenContainerDark = Color(0xFF0B2E24)
val IosOnGreenContainerDark = Color(0xFFA7F3D0)

val IosDarkBackground = TelegramDarkBackground
val IosDarkCardSurface = TelegramDarkCardSurface
val IosDarkBubbleIncoming = TelegramIncomingBubbleDark
val IosDarkFill = TelegramDarkFill
val IosDarkLabel = TelegramDarkLabel
val IosDarkSecondaryLabel = TelegramDarkSecondaryLabel
val IosDarkSeparator = TelegramDarkDivider
val IosDarkHairline = Color(0xFF1E293B)

// Semantic Aliases
val OnlinePresenceGreen = TelegramGreen
val AwayPresenceAmber = TelegramOrange
val ErrorRed = TelegramRed
val ErrorContainerLight = TelegramRedContainerLight
val ErrorContainerDark = IosRedContainerDark

// Call Stage & Accent Surface Tokens
val DynamicIslandDarkSurface = Color(0xFF0E141D)
val DynamicIslandDarkBorder = Color(0xFF253244)
val AuroraCyan = Color(0xFF00D4B4)
val AuroraViolet = Color(0xFF007AFF)

// Settings Squircle Badge Colors (Flagship iOS / Telegram vibrant accents)
val BadgeSapphire = Color(0xFF007AFF)
val BadgeCoral = Color(0xFFFF3B30)
val BadgeEmerald = Color(0xFF10B981)
val BadgeAmber = Color(0xFFF59E0B)
val BadgeCyan = Color(0xFF06B6D4)
val BadgeSlateBlue = Color(0xFF3B82F6)
val BadgeRose = Color(0xFFE11D48)

// Google Sign-In Mark Tokens
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

object PulseBrushes {
    val BrandGradient: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF00B4D8))
    )

    val OutgoingBubbleLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF00B4D8))
    )

    val OutgoingBubbleDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF0284C7), Color(0xFF0D9488))
    )

    val StoryRingActive: Brush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF00C6A7),
            Color(0xFF0088FF),
            Color(0xFF38BDF8),
            Color(0xFF10B981),
            Color(0xFF00C6A7)
        )
    )

    val UnreadBadgeGradient: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF007AFF), Color(0xFF00C6A7))
    )

    val SavedNotesGradientLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFE6F4FF), Color(0xFFF0FDFA))
    )

    val SavedNotesGradientDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF0D2840), Color(0xFF0B242B))
    )

    val SpotlightCardLight: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFFF0F8FF), Color(0xFFFFFFFF))
    )

    val SpotlightCardDark: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF141E2B), Color(0xFF0E141D))
    )
}
