package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PrivateUser
import com.example.data.model.UserProfile
import com.example.ui.components.ImageUtils
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    isUploadingPhoto: Boolean = false,
    onUploadProfilePhoto: (String) -> Unit = {},
    onRemoveProfilePhoto: () -> Unit = {},
    hasPhoneNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    blockedUserIds: Set<String> = emptySet(),
    allPeople: List<UserProfile> = emptyList(),
    onUnblockUser: (String) -> Unit = {},
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    isNightMode: Boolean = LocalIsTelegramDark.current,
    onToggleNightMode: () -> Unit = {},
    onOpenNotifications: (() -> Unit)? = null,
    unreadNotificationsCount: Long = 0L,
    onOpenSavedMessages: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var displayName by remember(userProfile?.userId) { mutableStateOf(userProfile?.displayName.orEmpty()) }
    var handle by remember(userProfile?.userId) { mutableStateOf(userProfile?.handle.orEmpty()) }
    var bio by remember(userProfile?.userId) { mutableStateOf(userProfile?.bio.orEmpty()) }
    var statusText by remember(userProfile?.userId) { mutableStateOf(userProfile?.statusVibe.orEmpty()) }
    var isOnline by remember(userProfile?.userId) { mutableStateOf(userProfile?.isOnline ?: true) }
    var isDiscoverable by remember(userProfile?.userId) { mutableStateOf(userProfile?.isDiscoverable ?: true) }
    var isEditProfileExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        if (userProfile != null && displayName.isEmpty() && handle.isEmpty()) {
            displayName = userProfile.displayName
            handle = userProfile.handle
            bio = userProfile.bio
            statusText = userProfile.statusVibe
            isOnline = userProfile.isOnline
            isDiscoverable = userProfile.isDiscoverable
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberIosHaptics()
    val clipboardManager = LocalClipboardManager.current

    var showBlockedDialog by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var isEncodingPhoto by remember { mutableStateOf(false) }

    val photoBusy = isUploadingPhoto || isEncodingPhoto
    val currentPhotoUrl = userProfile?.photoUrl.orEmpty()
    val readReceipts = privateUser?.readReceiptsEnabled ?: true
    val notifications = privateUser?.notificationsEnabled ?: true

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isEncodingPhoto = true
                val dataUri = withContext(Dispatchers.IO) {
                    ImageUtils.encodeAvatarToDataUri(context, uri)
                }
                isEncodingPhoto = false
                if (dataUri == null) {
                    Toast.makeText(
                        context,
                        "Could not process the selected photo. Please try another image.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    haptics.success()
                    onUploadProfilePhoto(dataUri)
                }
            }
        }
    }

    val launchAvatarPicker = {
        haptics.lightTap()
        avatarPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (showBackButton) 6.dp else 18.dp,
                            end = 10.dp,
                            top = 10.dp,
                            bottom = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                onBack()
                            },
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.my_profile_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            haptics.lightTap()
                            isEditProfileExpanded = !isEditProfileExpanded
                        },
                        modifier = Modifier.testTag("toggle_edit_profile_button")
                    ) {
                        Text(
                            text = if (isEditProfileExpanded) "Done" else "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Native horizontal profile header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.lightTap()
                        isEditProfileExpanded = !isEditProfileExpanded
                    }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !photoBusy) {
                                if (currentPhotoUrl.isNotBlank()) {
                                    haptics.lightTap()
                                    showPhotoPreview = true
                                } else {
                                    launchAvatarPicker()
                                }
                            }
                            .testTag("profile_avatar_preview")
                    ) {
                        UserAvatar(
                            name = displayName.ifBlank { userProfile?.displayName ?: "You" },
                            photoUrl = currentPhotoUrl,
                            size = 66.dp,
                            showPresence = false,
                            isOnline = isOnline
                        )
                        if (photoBusy) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.48f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !photoBusy) { launchAvatarPicker() }
                            .testTag("upload_profile_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = stringResource(R.string.upload_profile_photo),
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifBlank { "Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (handle.isNotBlank()) {
                        Text(
                            text = "@$handle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bio.trim().isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bio.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Collapsible Edit Profile Form
            AnimatedVisibility(
                visible = isEditProfileExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (!photoBusy) launchAvatarPicker() },
                            modifier = Modifier.testTag("profile_set_photo_button")
                        ) {
                            Text(
                                text = if (currentPhotoUrl.isBlank()) "Add photo" else "Change photo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (currentPhotoUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    if (!photoBusy) {
                                        haptics.impactMedium()
                                        onRemoveProfilePhoto()
                                    }
                                },
                                modifier = Modifier.testTag("remove_profile_photo_button")
                            ) {
                                Text(
                                    text = "Remove photo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(60) },
                        label = { Text("Name") },
                        placeholder = { Text("Your name") },
                        isError = displayName.trim().isEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input")
                    )

                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(32) },
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        isError = handle.trim().length < 2,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_handle_input")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.take(160) },
                        label = { Text("Bio") },
                        placeholder = { Text(stringResource(R.string.profile_bio_placeholder)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    Button(
                        onClick = {
                            haptics.success()
                            onSaveProfile(displayName, handle, bio, statusText, isOnline, isDiscoverable)
                            isEditProfileExpanded = false
                        },
                        enabled = !isBusy && displayName.trim().isNotEmpty() && handle.trim().length >= 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.background,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Saving…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // General
            SettingsSectionLabel("General")

            SettingsSwitchRow(
                icon = Icons.Outlined.DarkMode,
                title = "Dark mode",
                subtitle = if (isNightMode) "On" else "Off",
                checked = isNightMode,
                onCheckedChange = { onToggleNightMode() },
                modifier = Modifier.testTag("settings_night_mode_switch")
            )

            SettingsRowDivider()

            if (onOpenNotifications != null) {
                SettingsNavigationRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = if (notifications && hasPhoneNotificationPermission) {
                        "Alerts, previews and muted chats"
                    } else {
                        "Paused"
                    },
                    trailingBadge = when {
                        unreadNotificationsCount > 0L -> if (unreadNotificationsCount > 99L) "99+" else "$unreadNotificationsCount"
                        !hasPhoneNotificationPermission -> "Off"
                        else -> null
                    },
                    onClick = {
                        haptics.lightTap()
                        onOpenNotifications()
                    },
                    modifier = Modifier.testTag("settings_open_notifications_row")
                )
            } else {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "Receive message and call alerts",
                    checked = notifications,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasPhoneNotificationPermission) {
                            onRequestNotificationPermission()
                        }
                        onUpdatePrivacy(readReceipts, enabled)
                    }
                )
            }

            if (notifications && !hasPhoneNotificationPermission) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onRequestNotificationPermission)
                        .testTag("enable_notifications_banner")
                ) {
                    Text(
                        text = "Notification permission is off. Tap to enable.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Privacy
            SettingsSectionLabel("Privacy")

            SettingsSwitchRow(
                icon = Icons.Outlined.DoneAll,
                title = "Read receipts",
                subtitle = "Let others see when you read messages",
                checked = readReceipts,
                onCheckedChange = { onUpdatePrivacy(it, notifications) }
            )

            SettingsRowDivider()

            SettingsSwitchRow(
                icon = Icons.Outlined.Visibility,
                title = "Online status",
                subtitle = "Show when you are active",
                checked = isOnline,
                onCheckedChange = { newVal ->
                    isOnline = newVal
                    onSaveProfile(displayName, handle, bio, statusText, newVal, isDiscoverable)
                }
            )

            SettingsRowDivider()

            SettingsSwitchRow(
                icon = Icons.Outlined.Public,
                title = "Find by username",
                subtitle = "Allow others to search your @username",
                checked = isDiscoverable,
                onCheckedChange = { newVal ->
                    isDiscoverable = newVal
                    onSaveProfile(displayName, handle, bio, statusText, isOnline, newVal)
                }
            )

            SettingsRowDivider()

            SettingsNavigationRow(
                icon = Icons.Outlined.Block,
                title = "Blocked users",
                subtitle = if (blockedUserIds.isEmpty()) "None" else "${blockedUserIds.size} blocked",
                onClick = {
                    haptics.lightTap()
                    showBlockedDialog = true
                },
                modifier = Modifier.testTag("manage_blocked_users_row")
            )

            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Account
            SettingsSectionLabel("Account")

            SettingsNavigationRow(
                icon = Icons.Outlined.AlternateEmail,
                title = "@${handle.ifBlank { "member" }}",
                subtitle = "Tap to copy username",
                showChevron = false,
                onClick = {
                    haptics.lightTap()
                    val h = "@${handle.ifBlank { "member" }}"
                    clipboardManager.setText(AnnotatedString(h))
                    Toast.makeText(context, "Copied $h", Toast.LENGTH_SHORT).show()
                }
            )

            if (!privateUser?.email.isNullOrBlank()) {
                SettingsRowDivider()
                SettingsNavigationRow(
                    icon = Icons.Outlined.Info,
                    title = privateUser?.email.orEmpty(),
                    subtitle = "Account",
                    showChevron = false,
                    onClick = {}
                )
            }

            SettingsRowDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable {
                        haptics.impactMedium()
                        onSignOut()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("sign_out_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPhotoPreview && currentPhotoUrl.isNotBlank()) {
        AvatarFullScreenDialog(
            displayName = displayName.ifBlank { userProfile?.displayName ?: "Profile Photo" },
            photoUrl = currentPhotoUrl,
            onDismiss = { showPhotoPreview = false },
            onChangePhoto = launchAvatarPicker,
            onRemovePhoto = onRemoveProfilePhoto
        )
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
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRowDivider() {
    HorizontalDivider(
        thickness = 0.55.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 54.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingBadge: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable {
                haptics.selectionTick()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.onBackground,
                checkedBorderColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}
