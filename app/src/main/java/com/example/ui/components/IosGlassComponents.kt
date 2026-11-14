package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosPink
import com.example.ui.theme.IosTeal

/**
 * Blurred iOS-style atmospheric liquid wallpaper backdrop rendered behind transparent glass surfaces.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryAlpha = if (isDark) 0.52f else 0.36f
    val secondaryAlpha = if (isDark) 0.42f else 0.28f
    val accentAlpha = if (isDark) 0.32f else 0.22f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 68.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IosBlue.copy(alpha = primaryAlpha), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.08f),
                    radius = size.width * 0.64f
                ),
                radius = size.width * 0.64f,
                center = Offset(size.width * 0.86f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraViolet.copy(alpha = secondaryAlpha), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.19f),
                    radius = size.width * 0.58f
                ),
                radius = size.width * 0.58f,
                center = Offset(size.width * 0.10f, size.height * 0.19f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraCyan.copy(alpha = accentAlpha), Color.Transparent),
                    center = Offset(size.width * 0.48f, size.height * 0.02f),
                    radius = size.width * 0.50f
                ),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.48f, size.height * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IosTeal.copy(alpha = accentAlpha * 0.9f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.56f),
                    radius = size.width * 0.54f
                ),
                radius = size.width * 0.54f,
                center = Offset(size.width * 0.90f, size.height * 0.56f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IosPink.copy(alpha = accentAlpha * 0.78f), Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.78f),
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.14f, size.height * 0.78f)
            )
        }
    }
}

/**
 * Subtle white glassmorphic border brush mimicking specular frosted glass edges in iOS.
 */
@Composable
fun rememberGlassRimBrush(
    highlighted: Boolean = false
): Brush = GlassmorphismDefaults.rememberBorderBrush(highlighted = highlighted)

/**
 * Transparent iOS glassmorphic surface backed by [GlassmorphismCard].
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    highlighted: Boolean = false,
    tintAlpha: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassmorphismCard(
        modifier = modifier,
        shape = shape,
        highlighted = highlighted,
        tintAlpha = tintAlpha,
        content = content
    )
}

@Composable
fun GlassIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.55f),
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .border(
                width = 0.75.dp,
                color = if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.82f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
