package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PrivateUser
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosOrange
import com.example.ui.theme.IosPink
import com.example.ui.theme.OnlinePresenceGreen

private data class VibePreset(val emoji: String, val label: String)

private val StatusVibePresets = listOf(
    VibePreset("✨", "Available"),
    VibePreset("🎯", "Focusing"),
    VibePreset("☕", "Coffee run"),
    VibePreset("🎧", "In a meeting"),
    VibePreset("💬", "Up for chat")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    privateUser: PrivateUser?,
    isBusy: Boolean,
    onSaveProfile: (
        displayName: String,
        handle: String,
        bio: String,
        statusVibe: String,
        isOnline: Boolean,
        isDiscoverable: Boolean
    ) -> Unit,
    onUpdatePrivacy: (readReceipts: Boolean, notifications: Boolean) -> Unit,
    onSignOut: () -> Unit,
    hasPhoneNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    blockedUserIds: Set<String> = emptySet(),
    allPeople: List<UserProfile> = emptyList(),
    onUnblockUser: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var displayName by remember(userProfile?.userId) { mutableStateOf(userProfile?.displayName.orEmpty()) }
    var handle by remember(userProfile?.userId) { mutableStateOf(userProfile?.handle.orEmpty()) }
    var bio by remember(userProfile?.userId) { mutableStateOf(userProfile?.bio.orEmpty()) }
    var statusVibe by remember(userProfile?.userId) { mutableStateOf(userProfile?.statusVibe ?: "Available") }
    var isOnline by remember(userProfile?.userId) { mutableStateOf(userProfile?.isOnline ?: true) }
    var isDiscoverable by remember(userProfile?.userId) { mutableStateOf(userProfile?.isDiscoverable ?: true) }

    LaunchedEffect(userProfile) {
        if (userProfile != null && displayName.isEmpty() && handle.isEmpty()) {
            displayName = userProfile.displayName
            handle = userProfile.handle
            bio = userProfile.bio
            statusVibe = userProfile.statusVibe
            isOnline = userProfile.isOnline
            isDiscoverable = userProfile.isDiscoverable
        }
    }

    val context = LocalContext.current
    val haptics = rememberIosHaptics()
    val clipboardManager = LocalClipboardManager.current
    var showBlockedDialog by remember { mutableStateOf(false) }
    val readReceipts = privateUser?.readReceiptsEnabled ?: true
    val notifications = privateUser?.notificationsEnabled ?: true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Apple ID-Style Editorial Cover Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_profile_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(y = (-36).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(4.dp)
                        ) {
                            UserAvatar(
                                name = displayName.ifBlank { userProfile?.displayName ?: "You" },
                                photoUrl = userProfile?.photoUrl,
                                size = 80.dp,
                                showPresence = true,
                                isOnline = isOnline,
                                showStoryRing = isOnline
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isOnline) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) OnlinePresenceGreen else MaterialTheme.colorScheme.outline)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusVibe.ifBlank { if (isOnline) "Online" else "Away" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = displayName.ifBlank { "Univers Member" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = (-18).dp)
                    )
                    Text(
                        text = "@${handle.ifBlank { "member" }} · Tap to copy",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .offset(y = (-16).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.lightTap()
                                val h = "@${handle.ifBlank { "member" }}"
                                clipboardManager.setText(AnnotatedString(h))
                                Toast.makeText(context, "Copied $h", Toast.LENGTH_SHORT).show()
                            }
                    )
                    if (!privateUser?.email.isNullOrBlank()) {
                        Text(
                            text = privateUser?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.offset(y = (-12).dp)
                        )
                    }
                }
            }
        }

        // Status Vibe Presets
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                IosSectionHeaderRow(
                    icon = Icons.Rounded.AutoAwesome,
                    badgeColor = IosPink,
                    title = "Current Vibe"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusVibePresets.forEach { preset ->
                        val selected = statusVibe == preset.label
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.selectionTick()
                                    statusVibe = preset.label
                                }
                        ) {
                            Text(
                                text = "${preset.emoji} ${preset.label}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = statusVibe,
                    onValueChange = { statusVibe = it.take(60) },
                    label = { Text("Custom status vibe") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // iOS Settings Grouped Profile Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IosSectionHeaderRow(
                    icon = Icons.Rounded.Badge,
                    badgeColor = IosBlue,
                    title = "Public Directory Info"
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(60) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(32) },
                    label = { Text("Handle") },
                    prefix = { Text("@") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_handle_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(160) },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                IosToggleRow(
                    icon = Icons.Rounded.Visibility,
                    badgeColor = IosGreen,
                    title = "Show as online",
                    subtitle = "Let friends know when you're active",
                    checked = isOnline,
                    onCheckedChange = { isOnline = it }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IosToggleRow(
                    icon = Icons.Rounded.Public,
                    badgeColor = IosBlue,
                    title = "Discoverable in Contacts",
                    subtitle = "Allow signed-in members to find and message you",
                    checked = isDiscoverable,
                    onCheckedChange = { isDiscoverable = it }
                )

                Button(
                    onClick = {
                        haptics.success()
                        onSaveProfile(displayName, handle, bio, statusVibe, isOnline, isDiscoverable)
                    },
                    enabled = !isBusy && displayName.trim().isNotEmpty() && handle.trim().length >= 2,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Profile", fontWeight = FontWeight.Bold)
                }
            }
        }

        // iOS Settings Messaging Preferences Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Messaging Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IosToggleRow(
                    icon = Icons.Rounded.DoneAll,
                    badgeColor = IosIndigo,
                    title = "Send Read Receipts",
                    subtitle = "Clear unread counts when opening conversations",
                    checked = readReceipts,
                    onCheckedChange = { onUpdatePrivacy(it, notifications) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IosToggleRow(
                    icon = Icons.Rounded.NotificationsActive,
                    badgeColor = IosOrange,
                    title = "Real-Time Phone Alerts",
                    subtitle = "Head-up notifications, inline reply & sound for new messages",
                    checked = notifications,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasPhoneNotificationPermission) {
                            onRequestNotificationPermission()
                        }
                        onUpdatePrivacy(readReceipts, enabled)
                    }
                )
                if (notifications && !hasPhoneNotificationPermission) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onRequestNotificationPermission)
                            .testTag("enable_notifications_banner")
                    ) {
                        Text(
                            text = "Phone notification permission is off. Tap here to enable real-time alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.lightTap()
                            showBlockedDialog = true
                        }
                        .padding(vertical = 6.dp)
                        .testTag("manage_blocked_users_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocked Contacts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (blockedUserIds.isEmpty()) "No blocked contacts" else "${blockedUserIds.size} blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Manage blocked contacts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                haptics.impactMedium()
                onSignOut()
            },
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("sign_out_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showBlockedDialog) {
        BlockedUsersDialog(
            blockedUserIds = blockedUserIds,
            allPeople = allPeople,
            onDismiss = { showBlockedDialog = false },
            onUnblockUser = onUnblockUser
        )
    }
}

@Composable
private fun IosSectionHeaderRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IosToggleRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = rememberIosHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.selectionTick()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IosGreen,
                checkedBorderColor = IosGreen
            )
        )
    }
}
