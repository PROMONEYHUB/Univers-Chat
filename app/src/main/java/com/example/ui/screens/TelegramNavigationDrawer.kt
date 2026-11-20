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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
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
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark

@Composable
fun TelegramDrawerContent(
    currentUserProfile: UserProfile?,
    userEmailOrPhone: String?,
    onToggleNightMode: () -> Unit,
    onNewGroupClick: () -> Unit,
    onContactsClick: () -> Unit,
    onCallsClick: () -> Unit,
    onSavedMessagesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNotificationsClick: (() -> Unit)? = null,
    unreadNotificationsCount: Long = 0L,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                                size = 58.dp,
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
                                imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDark) "Switch to Light mode" else "Switch to Dark mode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = currentUserProfile?.displayName?.ifBlank { "Member" } ?: "Member",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            DrawerRowItem(
                icon = Icons.Outlined.AccountCircle,
                label = "My Profile",
                testTag = "drawer_my_profile",
                onClick = {
                    haptics.lightTap()
                    onSettingsClick()
                }
            )
            if (onNotificationsClick != null) {
                DrawerRowItem(
                    icon = Icons.Outlined.Notifications,
                    label = "Notifications",
                    trailingBadge = if (unreadNotificationsCount > 0L) {
                        if (unreadNotificationsCount > 99L) "99+" else unreadNotificationsCount.toString()
                    } else null,
                    testTag = "drawer_notifications",
                    onClick = {
                        haptics.lightTap()
                        onNotificationsClick()
                    }
                )
            }
            DrawerRowItem(
                icon = Icons.Outlined.GroupAdd,
                label = "New Group",
                testTag = "drawer_new_group",
                onClick = {
                    haptics.lightTap()
                    onNewGroupClick()
                }
            )
            DrawerRowItem(
                icon = Icons.Outlined.Person,
                label = "Contacts",
                testTag = "drawer_contacts",
                onClick = {
                    haptics.lightTap()
                    onContactsClick()
                }
            )
            DrawerRowItem(
                icon = Icons.Outlined.Call,
                label = "Calls",
                testTag = "drawer_calls",
                onClick = {
                    haptics.lightTap()
                    onCallsClick()
                }
            )
            DrawerRowItem(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Saved Messages",
                testTag = "drawer_saved_messages",
                onClick = {
                    haptics.lightTap()
                    onSavedMessagesClick()
                }
            )
            DrawerRowItem(
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
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )

            DrawerRowItem(
                icon = Icons.AutoMirrored.Rounded.Logout,
                label = "Log out",
                iconTint = MaterialTheme.colorScheme.error,
                labelTint = MaterialTheme.colorScheme.error,
                testTag = "drawer_sign_out",
                onClick = {
                    haptics.impactMedium()
                    onSignOutClick()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Univers Chat",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun DrawerRowItem(
    icon: ImageVector,
    label: String,
    trailingBadge: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelTint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = labelTint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
