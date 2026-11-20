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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramDarkChatCanvas
import com.example.ui.theme.TelegramLightBackground

@Stable
class TelegramPreferencesState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Default to Dark (true); users can switch Dark / Light anytime in Settings
    var nightModeOverride by mutableStateOf<Boolean?>(
        when (prefs.getInt(KEY_NIGHT_MODE, 1)) {
            1 -> true
            0 -> false
            -1 -> null
            else -> true
        }
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
        const val KEY_IN_APP_BANNERS = "in_app_banners_enabled"
        const val KEY_MESSAGE_PREVIEWS = "show_message_previews"
        const val KEY_VIBRATE = "vibrate_enabled"
    }
}

/**
 * Distraction-free matte chat canvas — flat solid neutral surface with zero procedural doodles or glowing orbs.
 */
@Composable
fun TelegramChatWallpaper(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) TelegramDarkChatCanvas else TelegramLightBackground)
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
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Bookmark,
            contentDescription = "Saved Messages",
            tint = Color.White,
            modifier = Modifier.size(size * 0.46f)
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
