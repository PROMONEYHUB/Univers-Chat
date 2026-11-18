package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.components.TelegramWallpaperStyle
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramSteelBlue

@Composable
fun TelegramDrawerContent(
    currentUserProfile: UserProfile?,
    userEmailOrPhone: String?,
    wallpaperStyle: TelegramWallpaperStyle,
    onToggleNightMode: () -> Unit,
    onNewGroupClick: () -> Unit,
    onContactsClick: () -> Unit,
    onCallsClick: () -> Unit,
    onSavedMessagesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCycleWallpaperClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val headerBg = if (isDark) TelegramDarkBar else TelegramSteelBlue

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.width(304.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Classic Telegram Steel-Blue Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.lightTap()
                                    onSettingsClick()
                                }
                        ) {
                            UserAvatar(
                                name = currentUserProfile?.displayName ?: "You",
                                photoUrl = currentUserProfile?.photoUrl,
                                size = 62.dp,
                                showPresence = false
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.selectionTick()
                                onToggleNightMode()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("drawer_night_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (isDark) "Switch to Day theme" else "Switch to Night theme",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = currentUserProfile?.displayName?.ifBlank { "Univers Member" } ?: "Univers Member",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val secondaryLine = buildString {
                        val handle = currentUserProfile?.handle.orEmpty()
                        if (handle.isNotBlank()) {
                            append("@$handle")
                        }
                        if (!userEmailOrPhone.isNullOrBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(userEmailOrPhone)
                        }
                    }.ifBlank { "Online" }
                    Text(
                        text = secondaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val bioPreview = currentUserProfile?.bio.orEmpty().trim()
                    if (bioPreview.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bioPreview,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TelegramDrawerItem(
                icon = Icons.Outlined.AccountCircle,
                label = "My Profile",
                testTag = "drawer_my_profile",
                onClick = {
                    haptics.lightTap()
                    onSettingsClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Outlined.GroupAdd,
                label = "New Group",
                testTag = "drawer_new_group",
                onClick = {
                    haptics.lightTap()
                    onNewGroupClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Outlined.PersonOutline,
                label = "Contacts",
                testTag = "drawer_contacts",
                onClick = {
                    haptics.lightTap()
                    onContactsClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Outlined.Call,
                label = "Calls",
                testTag = "drawer_calls",
                onClick = {
                    haptics.lightTap()
                    onCallsClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Saved Messages",
                testTag = "drawer_saved_messages",
                onClick = {
                    haptics.lightTap()
                    onSavedMessagesClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                testTag = "drawer_settings",
                onClick = {
                    haptics.lightTap()
                    onSettingsClick()
                }
            )

            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            TelegramDrawerItem(
                icon = Icons.Outlined.Wallpaper,
                label = "Chat Wallpaper",
                trailingBadge = wallpaperStyle.label,
                testTag = "drawer_wallpaper",
                onClick = {
                    haptics.selectionTick()
                    onCycleWallpaperClick()
                }
            )

            TelegramDrawerItem(
                icon = Icons.AutoMirrored.Rounded.Logout,
                label = "Log Out",
                tint = MaterialTheme.colorScheme.error,
                testTag = "drawer_sign_out",
                onClick = {
                    haptics.impactMedium()
                    onSignOutClick()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Univers Chat · Telegram-style Cloud Messenger",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun TelegramDrawerItem(
    icon: ImageVector,
    label: String,
    trailingBadge: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = tint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}
