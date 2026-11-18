package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueBright
import com.example.ui.theme.TelegramDarkChatCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class TelegramWallpaperStyle(
    val label: String,
    val lightTop: Color,
    val lightMid: Color,
    val lightBottom: Color,
    val darkTop: Color,
    val darkBottom: Color
) {
    CLASSIC_BOTANICAL(
        label = "Telegram Classic",
        lightTop = Color(0xFFD5E4C3),
        lightMid = Color(0xFFBBD7A8),
        lightBottom = Color(0xFFA2C99A),
        darkTop = Color(0xFF0E1621),
        darkBottom = Color(0xFF132030)
    ),
    OCEAN_SKY(
        label = "Azure Sky",
        lightTop = Color(0xFFD3E9FB),
        lightMid = Color(0xFFB6DAF8),
        lightBottom = Color(0xFFA0CEF5),
        darkTop = Color(0xFF0D1926),
        darkBottom = Color(0xFF14283D)
    ),
    LAVENDER_DUSK(
        label = "Lavender Dusk",
        lightTop = Color(0xFFE7DDF8),
        lightMid = Color(0xFFD7C8F4),
        lightBottom = Color(0xFFC8B4EE),
        darkTop = Color(0xFF141526),
        darkBottom = Color(0xFF1E1E38)
    ),
    SAND_MINIMAL(
        label = "Warm Sand",
        lightTop = Color(0xFFF3EBDD),
        lightMid = Color(0xFFE8DCC7),
        lightBottom = Color(0xFFDECDB2),
        darkTop = Color(0xFF15181C),
        darkBottom = Color(0xFF1B2026)
    );

    fun next(): TelegramWallpaperStyle {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

@Stable
class TelegramPreferencesState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // null = follow system, true = force night, false = force day
    var nightModeOverride by mutableStateOf<Boolean?>(
        when (prefs.getInt(KEY_NIGHT_MODE, -1)) {
            1 -> true
            0 -> false
            else -> null
        }
    )
        private set

    var wallpaperStyle by mutableStateOf(
        runCatching {
            TelegramWallpaperStyle.valueOf(
                prefs.getString(KEY_WALLPAPER, TelegramWallpaperStyle.CLASSIC_BOTANICAL.name)
                    ?: TelegramWallpaperStyle.CLASSIC_BOTANICAL.name
            )
        }.getOrDefault(TelegramWallpaperStyle.CLASSIC_BOTANICAL)
    )
        private set

    fun toggleNightMode(currentlyDark: Boolean) {
        val next = !currentlyDark
        nightModeOverride = next
        prefs.edit().putInt(KEY_NIGHT_MODE, if (next) 1 else 0).apply()
    }

    fun setNightMode(night: Boolean?) {
        nightModeOverride = night
        val stored = when (night) {
            true -> 1
            false -> 0
            null -> -1
        }
        prefs.edit().putInt(KEY_NIGHT_MODE, stored).apply()
    }

    fun selectWallpaper(style: TelegramWallpaperStyle) {
        wallpaperStyle = style
        prefs.edit().putString(KEY_WALLPAPER, style.name).apply()
    }

    fun cycleWallpaper() {
        selectWallpaper(wallpaperStyle.next())
    }

    private companion object {
        const val PREFS_NAME = "univers_telegram_prefs"
        const val KEY_NIGHT_MODE = "night_mode_override"
        const val KEY_WALLPAPER = "wallpaper_style"
    }
}

val LocalTelegramWallpaper = compositionLocalOf { TelegramWallpaperStyle.CLASSIC_BOTANICAL }

/**
 * Iconic Telegram gradient chat wallpaper with repeating geometric & messaging doodles.
 */
@Composable
fun TelegramChatWallpaper(
    modifier: Modifier = Modifier,
    style: TelegramWallpaperStyle = LocalWallpaperResolved()
) {
    val isDark = LocalIsTelegramDark.current
    val gradientBrush = if (isDark) {
        Brush.verticalGradient(colors = listOf(style.darkTop, style.darkBottom))
    } else {
        Brush.verticalGradient(colors = listOf(style.lightTop, style.lightMid, style.lightBottom))
    }
    val doodleColor = if (isDark) {
        Color(0xFF5288C1).copy(alpha = 0.08f)
    } else {
        Color(0xFF2B4A2F).copy(alpha = 0.07f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) TelegramDarkChatCanvas else style.lightMid)
            .background(gradientBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = 112.dp.toPx()
            val stepY = 112.dp.toPx()
            val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            var row = 0
            var y = 28.dp.toPx()
            while (y < size.height + stepY) {
                val offsetX = if (row % 2 == 1) stepX * 0.5f else 0f
                var col = 0
                var x = -20.dp.toPx() + offsetX
                while (x < size.width + stepX) {
                    val center = Offset(x, y)
                    when ((row * 3 + col) % 6) {
                        0 -> drawPaperPlaneDoodle(center, 15.dp.toPx(), doodleColor, stroke)
                        1 -> drawStarDoodle(center, 12.dp.toPx(), doodleColor, stroke)
                        2 -> drawBubbleDoodle(center, 14.dp.toPx(), doodleColor, stroke)
                        3 -> drawMoonDoodle(center, 11.dp.toPx(), doodleColor, stroke)
                        4 -> drawBoltDoodle(center, 13.dp.toPx(), doodleColor, stroke)
                        else -> drawHeartDoodle(center, 12.dp.toPx(), doodleColor, stroke)
                    }
                    x += stepX
                    col++
                }
                y += stepY
                row++
            }
        }
    }
}

@Composable
private fun LocalWallpaperResolved(): TelegramWallpaperStyle = LocalTelegramWallpaper.current

private fun DrawScope.drawPaperPlaneDoodle(center: Offset, r: Float, color: Color, stroke: Stroke) {
    rotate(-18f, pivot = center) {
        val path = Path().apply {
            moveTo(center.x + r, center.y)
            lineTo(center.x - r * 0.85f, center.y - r * 0.72f)
            lineTo(center.x - r * 0.42f, center.y)
            lineTo(center.x - r * 0.85f, center.y + r * 0.72f)
            close()
        }
        drawPath(path, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(center.x - r * 0.42f, center.y),
            end = Offset(center.x + r * 0.55f, center.y),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawStarDoodle(center: Offset, r: Float, color: Color, stroke: Stroke) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val px = center.x + (radius * cos(angle)).toFloat()
        val py = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color = color, style = stroke)
}

private fun DrawScope.drawBubbleDoodle(center: Offset, r: Float, color: Color, stroke: Stroke) {
    val path = Path().apply {
        moveTo(center.x - r * 0.8f, center.y - r * 0.55f)
        lineTo(center.x + r * 0.8f, center.y - r * 0.55f)
        lineTo(center.x + r * 0.8f, center.y + r * 0.35f)
        lineTo(center.x - r * 0.1f, center.y + r * 0.35f)
        lineTo(center.x - r * 0.55f, center.y + r * 0.75f)
        lineTo(center.x - r * 0.55f, center.y + r * 0.35f)
        lineTo(center.x - r * 0.8f, center.y + r * 0.35f)
        close()
    }
    drawPath(path, color = color, style = stroke)
}

private fun DrawScope.drawMoonDoodle(center: Offset, r: Float, color: Color, stroke: Stroke) {
    drawCircle(color = color, radius = r * 0.72f, center = center, style = stroke)
    drawCircle(color = color, radius = r * 0.25f, center = Offset(center.x + r * 0.9f, center.y - r * 0.7f), style = stroke)
}

private fun DrawScope.drawBoltDoodle(center: Offset, r: Float, color: Color, stroke: Stroke) {
    val path = Path().apply {
        moveTo(center.x + r * 0.2f, center.y - r)
        lineTo(center.x - r * 0.55f, center.y + r * 0.1f)
        lineTo(center.x + r * 0.05f, center.y + r * 0.1f)
        lineTo(center.x - r * 0.2f, center.y + r)
        lineTo(center.x + r * 0.55f, center.y - r * 0.1f)
        lineTo(center.x - r * 0.05f, center.y - r * 0.1f)
        close()
    }
    drawPath(path, color = color, style = stroke)
}

private fun DrawScope.drawHeartDoodle(center: Offset, r: Float, color: Color, stroke: Stroke) {
    val path = Path().apply {
        moveTo(center.x, center.y + r * 0.72f)
        cubicTo(
            center.x - r * 1.15f, center.y + r * 0.05f,
            center.x - r * 0.85f, center.y - r * 0.85f,
            center.x, center.y - r * 0.25f
        )
        cubicTo(
            center.x + r * 0.85f, center.y - r * 0.85f,
            center.x + r * 1.15f, center.y + r * 0.05f,
            center.x, center.y + r * 0.72f
        )
    }
    drawPath(path, color = color, style = stroke)
}

@Composable
fun TelegramLogoMark(
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.img_universchat_logo),
        contentDescription = "Univers Chat logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}

@Composable
fun SavedMessagesAvatar(
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(TelegramBlueBright, TelegramBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Bookmark,
            contentDescription = "Saved Messages",
            tint = Color.White,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun TelegramCheckTicks(
    isDeliveredOrRead: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if (isDeliveredOrRead) Icons.Rounded.DoneAll else Icons.Rounded.Done,
        contentDescription = if (isDeliveredOrRead) "Delivered and read" else "Sent",
        tint = tint,
        modifier = modifier.size(size)
    )
}
