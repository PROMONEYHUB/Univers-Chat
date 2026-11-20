package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean native card defaults without frosted glass blooms or specular gradients.
 */
object GlassmorphismDefaults {
    val DefaultShape: Shape = RoundedCornerShape(16.dp)
    val DefaultBlurRadius: Dp = 0.dp
    val DefaultBorderWidth: Dp = 0.75.dp
    val HighlightedBorderWidth: Dp = 1.dp

    @Composable
    fun rememberBorderBrush(
        highlighted: Boolean = false,
        accentColor: Color = MaterialTheme.colorScheme.primary
    ): Brush {
        val color = if (highlighted) {
            accentColor.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
        return SolidColor(color)
    }

    @Composable
    fun rememberBackdropBrush(
        highlighted: Boolean = false,
        containerColor: Color? = null,
        tintAlpha: Float? = null
    ): Brush {
        val baseColor = containerColor ?: MaterialTheme.colorScheme.surface
        val resolved = if (tintAlpha != null) {
            baseColor.copy(alpha = tintAlpha.coerceIn(0f, 1f))
        } else {
            baseColor
        }
        return SolidColor(resolved)
    }
}

/**
 * Clean flat Material surface container keeping signature compatibility across screens.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassmorphismDefaults.DefaultShape,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp = GlassmorphismDefaults.DefaultBlurRadius,
    borderWidth: Dp? = null,
    borderBrush: Brush? = null,
    borderColor: Color? = null,
    containerColor: Color? = null,
    highlighted: Boolean = false,
    @Suppress("UNUSED_PARAMETER") tintAlpha: Float? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedContainer = containerColor ?: MaterialTheme.colorScheme.surface
    val resolvedBorderBrush = when {
        borderColor != null -> SolidColor(borderColor)
        borderBrush != null -> borderBrush
        highlighted -> SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        else -> SolidColor(MaterialTheme.colorScheme.outlineVariant)
    }
    val resolvedBorderWidth = borderWidth ?: if (highlighted) {
        GlassmorphismDefaults.HighlightedBorderWidth
    } else {
        GlassmorphismDefaults.DefaultBorderWidth
    }

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
            .background(resolvedContainer)
            .border(
                width = resolvedBorderWidth,
                brush = resolvedBorderBrush,
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}
