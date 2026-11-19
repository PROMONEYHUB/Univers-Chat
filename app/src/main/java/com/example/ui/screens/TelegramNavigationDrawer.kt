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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
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
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.BadgeAmber
import com.example.ui.theme.BadgeCoral
import com.example.ui.theme.BadgeCyan
import com.example.ui.theme.BadgeEmerald
import com.example.ui.theme.BadgeRose
import com.example.ui.theme.BadgeSapphire
import com.example.ui.theme.BadgeSlateBlue
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent

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
    onNotificationsClick: (() -> Unit)? = null,
    unreadNotificationsCount: Long = 0L,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val accent = LocalUniversAccent.current
    val headerBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.width(308.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBrush)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
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
                                size = 64.dp,
                                showPresence = true,
                                isOnline = currentUserProfile?.isOnline != false,
                                showStoryRing = true
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    haptics.selectionTick()
                                    onToggleNightMode()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("drawer_night_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = if (isDark) "Switch to Day theme" else "Switch to Night theme",
                                    tint = Color.White,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = currentUserProfile?.displayName?.ifBlank { "Univers Member" } ?: "Univers Member",
                        style = MaterialTheme.typography.titleLarge,
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

            Spacer(modifier = Modifier.height(10.dp))

            TelegramDrawerItem(
                icon = Icons.Rounded.AccountCircle,
                badgeColor = accent.primary(isDark),
                label = "My Profile",
                testTag = "drawer_my_profile",
                onClick = {
                    haptics.lightTap()
                    onSettingsClick()
                }
            )
            if (onNotificationsClick != null) {
                TelegramDrawerItem(
                    icon = Icons.Rounded.NotificationsActive,
                    badgeColor = BadgeCoral,
                    label = "Notifications",
                    trailingBadge = if (unreadNotificationsCount > 0L) {
                        if (unreadNotificationsCount > 99L) "99+" else unreadNotificationsCount.toString()
                    } else null,
                    trailingBadgeColor = BadgeCoral,
                    testTag = "drawer_notifications",
                    onClick = {
                        haptics.lightTap()
                        onNotificationsClick()
                    }
                )
            }
            TelegramDrawerItem(
                icon = Icons.Rounded.GroupAdd,
                badgeColor = BadgeEmerald,
                label = "New Group",
                testTag = "drawer_new_group",
                onClick = {
                    haptics.lightTap()
                    onNewGroupClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Rounded.Person,
                badgeColor = BadgeSapphire,
                label = "Contacts",
                testTag = "drawer_contacts",
                onClick = {
                    haptics.lightTap()
                    onContactsClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Rounded.Call,
                badgeColor = BadgeCyan,
                label = "Calls",
                testTag = "drawer_calls",
                onClick = {
                    haptics.lightTap()
                    onCallsClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Rounded.Bookmark,
                badgeColor = BadgeAmber,
                label = "Saved Messages",
                testTag = "drawer_saved_messages",
                onClick = {
                    haptics.lightTap()
                    onSavedMessagesClick()
                }
            )
            TelegramDrawerItem(
                icon = Icons.Rounded.Settings,
                badgeColor = BadgeSlateBlue,
                label = "Settings",
                testTag = "drawer_settings",
                onClick = {
                    haptics.lightTap()
                    onSettingsClick()
                }
            )

            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )

            TelegramDrawerItem(
                icon = Icons.Rounded.Wallpaper,
                badgeColor = accent.primary(isDark),
                label = "Chat Wallpaper",
                trailingBadge = wallpaperStyle.label.substringBefore(" "),
                testTag = "drawer_wallpaper",
                onClick = {
                    haptics.selectionTick()
                    onCycleWallpaperClick()
                }
            )

            TelegramDrawerItem(
                icon = Icons.AutoMirrored.Rounded.Logout,
                badgeColor = BadgeRose,
                label = "Log Out",
                tint = MaterialTheme.colorScheme.error,
                testTag = "drawer_sign_out",
                onClick = {
                    haptics.impactMedium()
                    onSignOutClick()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Univers Chat · End-to-End Encrypted",
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
    badgeColor: Color,
    label: String,
    trailingBadge: String? = null,
    trailingBadgeColor: Color? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(badgeColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = trailingBadgeColor ?: MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (trailingBadgeColor != null) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}
