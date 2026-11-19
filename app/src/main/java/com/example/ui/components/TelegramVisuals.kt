package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent
import com.example.ui.theme.TelegramDarkChatCanvas
import com.example.ui.theme.UniversAccentTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class TelegramWallpaperStyle(
    val label: String,
    val lightTop: Color,
    val lightMid: Color,
    val lightBottom: Color,
    val darkTop: Color,
    val darkBottom: Color,
    val orbPrimary: Color,
    val orbSecondary: Color
) {
    COSMIC_AURORA(
        label = "Ocean Midnight",
        lightTop = Color(0xFFEAF4FC),
        lightMid = Color(0xFFDCEEFB),
        lightBottom = Color(0xFFCDE6F8),
        darkTop = Color(0xFF070C14),
        darkBottom = Color(0xFF0D1B2A),
        orbPrimary = Color(0xFF00C6A7),
        orbSecondary = Color(0xFF007AFF)
    ),
    CLASSIC_BOTANICAL(
        label = "Emerald Forest",
        lightTop = Color(0xFFE5F3E6),
        lightMid = Color(0xFFD2E9D5),
        lightBottom = Color(0xFFBEDDC2),
        darkTop = Color(0xFF061210),
        darkBottom = Color(0xFF0D241F),
        orbPrimary = Color(0xFF10B981),
        orbSecondary = Color(0xFF06B6D4)
    ),
    OCEAN_SKY(
        label = "Arctic Glacier",
        lightTop = Color(0xFFE9F2FA),
        lightMid = Color(0xFFD8E9F7),
        lightBottom = Color(0xFFC5DFF5),
        darkTop = Color(0xFF08101C),
        darkBottom = Color(0xFF102238),
        orbPrimary = Color(0xFF38BDF8),
        orbSecondary = Color(0xFF2563EB)
    ),
    LAVENDER_DUSK(
        label = "Sunset Rose",
        lightTop = Color(0xFFFDF0ED),
        lightMid = Color(0xFFF9E2DC),
        lightBottom = Color(0xFFF4D3CB),
        darkTop = Color(0xFF140B10),
        darkBottom = Color(0xFF24131D),
        orbPrimary = Color(0xFFFF6B4A),
        orbSecondary = Color(0xFFE11D48)
    ),
    SAND_MINIMAL(
        label = "Warm Linen",
        lightTop = Color(0xFFF8F3EA),
        lightMid = Color(0xFFEFE6D6),
        lightBottom = Color(0xFFE6D9C2),
        darkTop = Color(0xFF12110E),
        darkBottom = Color(0xFF1D1A14),
        orbPrimary = Color(0xFFF59E0B),
        orbSecondary = Color(0xFFD97706)
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
                prefs.getString(KEY_WALLPAPER, TelegramWallpaperStyle.COSMIC_AURORA.name)
                    ?: TelegramWallpaperStyle.COSMIC_AURORA.name
            )
        }.getOrDefault(TelegramWallpaperStyle.COSMIC_AURORA)
    )
        private set

    var accentTheme by mutableStateOf(
        runCatching {
            UniversAccentTheme.valueOf(
                prefs.getString(KEY_ACCENT_THEME, UniversAccentTheme.OCEAN.name)
                    ?: UniversAccentTheme.OCEAN.name
            )
        }.getOrDefault(UniversAccentTheme.OCEAN)
    )
        private set

    var inAppBannersEnabled by mutableStateOf(prefs.getBoolean(KEY_IN_APP_BANNERS, true))
        private set

    var showMessagePreviews by mutableStateOf(prefs.getBoolean(KEY_MESSAGE_PREVIEWS, true))
        private set

    var vibrateEnabled by mutableStateOf(prefs.getBoolean(KEY_VIBRATE, true))
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

    fun selectAccentTheme(theme: UniversAccentTheme) {
        accentTheme = theme
        prefs.edit().putString(KEY_ACCENT_THEME, theme.name).apply()
    }

    fun updateInAppBanners(enabled: Boolean) {
        inAppBannersEnabled = enabled
        prefs.edit().putBoolean(KEY_IN_APP_BANNERS, enabled).apply()
    }

    fun updateShowMessagePreviews(enabled: Boolean) {
        showMessagePreviews = enabled
        prefs.edit().putBoolean(KEY_MESSAGE_PREVIEWS, enabled).apply()
    }

    fun updateVibrate(enabled: Boolean) {
        vibrateEnabled = enabled
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "univers_telegram_prefs"
        const val KEY_NIGHT_MODE = "night_mode_override"
        const val KEY_WALLPAPER = "wallpaper_style"
        const val KEY_ACCENT_THEME = "accent_theme"
        const val KEY_IN_APP_BANNERS = "in_app_banners_enabled"
        const val KEY_MESSAGE_PREVIEWS = "show_message_previews"
        const val KEY_VIBRATE = "vibrate_enabled"
    }
}

val LocalTelegramWallpaper = compositionLocalOf { TelegramWallpaperStyle.COSMIC_AURORA }

/**
 * Refined atmospheric chat wallpaper with cached soft radial luminescence and subtle geometric linework.
 */
@Composable
fun TelegramChatWallpaper(
    modifier: Modifier = Modifier,
    style: TelegramWallpaperStyle = LocalWallpaperResolved()
) {
    val isDark = LocalIsTelegramDark.current
    val baseColor = if (isDark) TelegramDarkChatCanvas else style.lightMid
    val doodleColor = if (isDark) {
        style.orbPrimary.copy(alpha = 0.06f)
    } else {
        style.orbSecondary.copy(alpha = 0.055f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
            .drawWithCache {
                val gradientBrush = if (isDark) {
                    Brush.verticalGradient(colors = listOf(style.darkTop, style.darkBottom))
                } else {
                    Brush.verticalGradient(colors = listOf(style.lightTop, style.lightMid, style.lightBottom))
                }
                val orb1Center = Offset(x = size.width * 0.18f, y = size.height * 0.18f)
                val orb2Center = Offset(x = size.width * 0.84f, y = size.height * 0.78f)
                val orb1Radius = (size.minDimension * 0.68f).coerceAtLeast(1f)
                val orb2Radius = (size.minDimension * 0.62f).coerceAtLeast(1f)
                val orb1Brush = Brush.radialGradient(
                    colors = listOf(
                        style.orbPrimary.copy(alpha = if (isDark) 0.12f else 0.14f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = orb1Radius
                )
                val orb2Brush = Brush.radialGradient(
                    colors = listOf(
                        style.orbSecondary.copy(alpha = if (isDark) 0.11f else 0.10f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = orb2Radius
                )

                val stepX = 120.dp.toPx()
                val stepY = 120.dp.toPx()
                val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val combinedPath = Path()
                val planetCenters = ArrayList<Offset>(16)
                val rPlanet = 13.dp.toPx()
                val rStar = 11.dp.toPx()
                val rBubble = 13.dp.toPx()
                val rMoon = 10.dp.toPx()
                val rBolt = 12.dp.toPx()
                val rHeart = 11.dp.toPx()
                val ringSize = Size(rPlanet * 2.3f, rPlanet * 0.64f)

                var row = 0
                var y = 28.dp.toPx()
                while (y < size.height + stepY) {
                    val offsetX = if (row % 2 == 1) stepX * 0.5f else 0f
                    var col = 0
                    var x = -20.dp.toPx() + offsetX
                    while (x < size.width + stepX) {
                        val center = Offset(x, y)
                        when ((row * 3 + col) % 6) {
                            0 -> {
                                combinedPath.addOval(Rect(center = center, radius = rPlanet * 0.55f))
                                planetCenters.add(center)
                            }
                            1 -> combinedPath.addStarDoodle(center, rStar)
                            2 -> combinedPath.addBubbleDoodle(center, rBubble)
                            3 -> combinedPath.addMoonDoodle(center, rMoon)
                            4 -> combinedPath.addBoltDoodle(center, rBolt)
                            else -> combinedPath.addHeartDoodle(center, rHeart)
                        }
                        x += stepX
                        col++
                    }
                    y += stepY
                    row++
                }

                onDrawBehind {
                    drawRect(brush = gradientBrush)
                    drawCircle(brush = orb1Brush, radius = orb1Radius, center = orb1Center)
                    drawCircle(brush = orb2Brush, radius = orb2Radius, center = orb2Center)
                    drawPath(path = combinedPath, color = doodleColor, style = stroke)
                    for (i in 0 until planetCenters.size) {
                        val center = planetCenters[i]
                        rotate(-24f, pivot = center) {
                            drawOval(
                                color = doodleColor,
                                topLeft = Offset(center.x - rPlanet * 1.15f, center.y - rPlanet * 0.32f),
                                size = ringSize,
                                style = stroke
                            )
                        }
                    }
                }
            }
    )
}

@Composable
private fun LocalWallpaperResolved(): TelegramWallpaperStyle = LocalTelegramWallpaper.current

private fun Path.addStarDoodle(center: Offset, r: Float) {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val px = center.x + (radius * cos(angle)).toFloat()
        val py = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

private fun Path.addBubbleDoodle(center: Offset, r: Float) {
    moveTo(center.x - r * 0.8f, center.y - r * 0.55f)
    lineTo(center.x + r * 0.8f, center.y - r * 0.55f)
    lineTo(center.x + r * 0.8f, center.y + r * 0.35f)
    lineTo(center.x - r * 0.1f, center.y + r * 0.35f)
    lineTo(center.x - r * 0.55f, center.y + r * 0.75f)
    lineTo(center.x - r * 0.55f, center.y + r * 0.35f)
    lineTo(center.x - r * 0.8f, center.y + r * 0.35f)
    close()
}

private fun Path.addMoonDoodle(center: Offset, r: Float) {
    addOval(Rect(center = center, radius = r * 0.72f))
    addOval(Rect(center = Offset(center.x + r * 0.9f, center.y - r * 0.7f), radius = r * 0.25f))
}

private fun Path.addBoltDoodle(center: Offset, r: Float) {
    moveTo(center.x + r * 0.2f, center.y - r)
    lineTo(center.x - r * 0.55f, center.y + r * 0.1f)
    lineTo(center.x + r * 0.05f, center.y + r * 0.1f)
    lineTo(center.x - r * 0.2f, center.y + r)
    lineTo(center.x + r * 0.55f, center.y - r * 0.1f)
    lineTo(center.x - r * 0.05f, center.y - r * 0.1f)
    close()
}

private fun Path.addHeartDoodle(center: Offset, r: Float) {
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
    val accent = LocalUniversAccent.current
    val isDark = LocalIsTelegramDark.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.brandGradient(isDark)),
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
