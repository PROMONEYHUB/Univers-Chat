package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default visual tokens and brushes for [GlassmorphismCard].
 */
object GlassmorphismDefaults {
    val DefaultShape: Shape = RoundedCornerShape(22.dp)
    val DefaultBlurRadius: Dp = 20.dp
    val DefaultBorderWidth: Dp = 0.9.dp
    val HighlightedBorderWidth: Dp = 1.2.dp

    /**
     * Diagonal specular glass rim brush that catches light along the top-left edge
     * and blends softly toward the bottom-right corner.
     */
    @Composable
    fun rememberBorderBrush(
        highlighted: Boolean = false,
        accentColor: Color = MaterialTheme.colorScheme.primary
    ): Brush {
        val isDark = isSystemInDarkTheme()
        return remember(isDark, highlighted, accentColor) {
            val topLeft = when {
                highlighted && isDark -> Color.White.copy(alpha = 0.48f)
                highlighted -> Color.White.copy(alpha = 0.96f)
                isDark -> Color.White.copy(alpha = 0.26f)
                else -> Color.White.copy(alpha = 0.90f)
            }
            val mid = when {
                highlighted -> accentColor.copy(alpha = if (isDark) 0.44f else 0.36f)
                isDark -> Color.White.copy(alpha = 0.10f)
                else -> Color.White.copy(alpha = 0.40f)
            }
            val bottomRight = when {
                highlighted -> Color.White.copy(alpha = if (isDark) 0.26f else 0.74f)
                isDark -> Color.White.copy(alpha = 0.18f)
                else -> Color.White.copy(alpha = 0.64f)
            }
            Brush.linearGradient(colors = listOf(topLeft, mid, bottomRight))
        }
    }

    /**
     * Frosted vertical fill gradient for the blurred glass backdrop layer.
     */
    @Composable
    fun rememberBackdropBrush(
        highlighted: Boolean = false,
        containerColor: Color? = null,
        tintAlpha: Float? = null
    ): Brush {
        val isDark = isSystemInDarkTheme()
        return remember(isDark, highlighted, containerColor, tintAlpha) {
            if (containerColor != null) {
                val topColor = if (tintAlpha != null) {
                    containerColor.copy(alpha = tintAlpha.coerceIn(0f, 1f))
                } else {
                    containerColor
                }
                val bottomColor = topColor.copy(
                    alpha = (topColor.alpha * 0.88f).coerceAtLeast(0.04f)
                )
                Brush.verticalGradient(colors = listOf(topColor, bottomColor))
            } else {
                val topAlpha = (tintAlpha ?: when {
                    highlighted && isDark -> 0.17f
                    highlighted -> 0.60f
                    isDark -> 0.11f
                    else -> 0.46f
                }).coerceIn(0f, 1f)
                val bottomAlpha = (topAlpha * 0.56f).coerceAtLeast(0.04f)
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = topAlpha),
                        Color.White.copy(alpha = bottomAlpha)
                    )
                )
            }
        }
    }
}

/**
 * Reusable glassmorphism card with a blurred translucent backdrop layer, soft specular sheen,
 * and crisp border around sharp foreground content.
 *
 * @param modifier Modifier applied to the outer card container.
 * @param shape Clip and border shape of the card.
 * @param blurRadius Blur radius applied exclusively to the frosted backdrop layer.
 * @param borderWidth Optional explicit border width; defaults to [GlassmorphismDefaults.HighlightedBorderWidth]
 * when [highlighted] is true or [GlassmorphismDefaults.DefaultBorderWidth] otherwise.
 * @param borderBrush Optional custom border brush. Ignored if [borderColor] is provided.
 * @param borderColor Optional solid border color override.
 * @param containerColor Optional custom base tint color (e.g., smoky dark surface for Dynamic Island pills).
 * @param highlighted Emphasizes the specular rim and frosted fill for active or unread states.
 * @param tintAlpha Optional alpha override for the frosted backdrop layer.
 * @param contentPadding Padding applied inside the card around [content].
 * @param onClick Optional click handler. When non-null, attaches clipped ripple feedback and
 * enforces a minimum 48.dp touch target.
 * @param enabled Whether clicks are enabled when [onClick] is provided.
 * @param content Sharp foreground content rendered above the blurred glass backdrop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassmorphismDefaults.DefaultShape,
    blurRadius: Dp = GlassmorphismDefaults.DefaultBlurRadius,
    borderWidth: Dp? = null,
    borderBrush: Brush? = null,
    borderColor: Color? = null,
    containerColor: Color? = null,
    highlighted: Boolean = false,
    tintAlpha: Float? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = MaterialTheme.colorScheme.primary
    val defaultRimBrush = GlassmorphismDefaults.rememberBorderBrush(
        highlighted = highlighted,
        accentColor = accentColor
    )
    val resolvedBorderBrush = remember(borderColor, borderBrush, defaultRimBrush) {
        when {
            borderColor != null -> SolidColor(borderColor)
            borderBrush != null -> borderBrush
            else -> defaultRimBrush
        }
    }
    val resolvedBorderWidth = borderWidth ?: if (highlighted) {
        GlassmorphismDefaults.HighlightedBorderWidth
    } else {
        GlassmorphismDefaults.DefaultBorderWidth
    }
    val backdropBrush = GlassmorphismDefaults.rememberBackdropBrush(
        highlighted = highlighted,
        containerColor = containerColor,
        tintAlpha = tintAlpha
    )
    val highlightBloomColor = if (isDark) {
        Color.White.copy(alpha = if (highlighted) 0.11f else 0.07f)
    } else {
        Color.White.copy(alpha = if (highlighted) 0.42f else 0.28f)
    }
    val secondaryBloomColor = accentColor.copy(
        alpha = when {
            highlighted && isDark -> 0.16f
            highlighted -> 0.12f
            isDark -> 0.05f
            else -> 0.04f
        }
    )

    val interactiveModifier = when {
        onLongClick != null || onClick != null -> {
            Modifier
                .minimumInteractiveComponentSize()
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .combinedClickable(
                    enabled = enabled,
                    onClick = { onClick?.invoke() },
                    onLongClick = onLongClick
                )
        }
        else -> Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(interactiveModifier)
            .background(Color.Transparent)
            .border(
                width = resolvedBorderWidth,
                brush = resolvedBorderBrush,
                shape = shape
            )
    ) {
        // Blurred frosted backdrop layer kept strictly behind foreground content
        val blurModifier = if (blurRadius > 0.dp) {
            Modifier.blur(
                radius = blurRadius,
                edgeTreatment = BlurredEdgeTreatment.Rectangle
            )
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .then(blurModifier)
                .background(backdropBrush)
                .drawBehind {
                    val primaryRadius = size.maxDimension * 0.78f
                    if (primaryRadius > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(highlightBloomColor, Color.Transparent),
                                center = Offset(size.width * 0.14f, size.height * 0.08f),
                                radius = primaryRadius
                            ),
                            radius = primaryRadius,
                            center = Offset(size.width * 0.14f, size.height * 0.08f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(secondaryBloomColor, Color.Transparent),
                                center = Offset(size.width * 0.90f, size.height * 0.92f),
                                radius = primaryRadius * 0.72f
                            ),
                            radius = primaryRadius * 0.72f,
                            center = Offset(size.width * 0.90f, size.height * 0.92f)
                        )
                    }
                }
        )

        // Subtle crisp top specular glaze so edges remain luminous even on pre-API 31 devices
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = if (isDark) 0.06f else 0.18f),
                            0.35f to Color.Transparent,
                            1f to Color.Transparent
                        )
                    )
                }
        )

        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
