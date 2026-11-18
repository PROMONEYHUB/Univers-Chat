package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PrivateUser
import com.example.data.model.UserProfile
import com.example.ui.components.ImageUtils
import com.example.ui.components.LocalTelegramWallpaper
import com.example.ui.components.TelegramWallpaperStyle
import com.example.ui.components.UniversOrbitLoader
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueBright
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramGreen
import com.example.ui.theme.TelegramSteelBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class VibePreset(val emoji: String, val label: String)

private val StatusVibePresets = listOf(
    VibePreset("✨", "Available"),
    VibePreset("🎯", "Focusing"),
    VibePreset("☕", "Coffee run"),
    VibePreset("🎧", "In a meeting"),
    VibePreset("💬", "Up for chat")
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    wallpaperStyle: TelegramWallpaperStyle = LocalTelegramWallpaper.current,
    onSelectWallpaper: (TelegramWallpaperStyle) -> Unit = {},
    onOpenSavedMessages: (() -> Unit)? = null,
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
    val scope = rememberCoroutineScope()
    val haptics = rememberIosHaptics()
    val clipboardManager = LocalClipboardManager.current
    val isDark = LocalIsTelegramDark.current
    val cosmicHeaderBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)
    val sectionSpacerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

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
            TopAppBar(
                modifier = Modifier.background(cosmicHeaderBrush),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.my_profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Univers Cosmic Profile Hero Band with Uploadable Avatar & Live Bio Preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cosmicHeaderBrush)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(108.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
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
                            size = 96.dp,
                            showPresence = true,
                            isOnline = isOnline,
                            showStoryRing = isOnline
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
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Accessible 48dp touch target hosting the camera badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !photoBusy) { launchAvatarPicker() }
                            .testTag("upload_profile_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TelegramBlue,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = stringResource(R.string.upload_profile_photo),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayName.ifBlank { "Univers Member" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = buildString {
                        append(if (isOnline) "online" else "last seen recently")
                        if (handle.isNotBlank()) {
                            append(" · @$handle")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (bio.trim().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bio.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.90f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (statusVibe.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = statusVibe,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Profile Photo Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = launchAvatarPicker,
                        enabled = !photoBusy,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("profile_set_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = null,
                            tint = TelegramBlue,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentPhotoUrl.isBlank()) {
                                stringResource(R.string.upload_profile_photo)
                            } else {
                                stringResource(R.string.change_profile_photo)
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = TelegramBlue
                        )
                    }

                    if (currentPhotoUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                haptics.impactMedium()
                                onRemoveProfilePhoto()
                            },
                            enabled = !photoBusy,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("remove_profile_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.remove_profile_photo),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(sectionSpacerBg)
            )

            // Edit Profile Section (Display Name, Short Bio, Username, Status Vibe)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 18.dp)
            ) {
                TelegramSectionLabel("Edit Profile")

                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(60) },
                        label = { Text("Display Name") },
                        placeholder = { Text("Your name shown to others") },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (displayName.trim().isEmpty()) {
                                        "Display name is required"
                                    } else {
                                        "Visible across chats and contacts"
                                    },
                                    color = if (displayName.trim().isEmpty()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text("${displayName.length}/60")
                            }
                        },
                        isError = displayName.trim().isEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.take(160) },
                        label = { Text("Bio") },
                        placeholder = { Text(stringResource(R.string.profile_bio_placeholder)) },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("A short bio shown on your profile")
                                Text("${bio.length}/160")
                            }
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(32) },
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        supportingText = {
                            Text(
                                text = if (handle.trim().length < 2) {
                                    "Username must be at least 2 characters"
                                } else {
                                    "Contacts can find you with @$handle"
                                }
                            )
                        },
                        isError = handle.trim().length < 2,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_handle_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusVibePresets.forEach { preset ->
                            val selected = statusVibe == preset.label
                            Surface(
                                shape = CircleShape,
                                color = if (selected) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        haptics.selectionTick()
                                        statusVibe = preset.label
                                    }
                            ) {
                                Text(
                                    text = "${preset.emoji} ${preset.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = statusVibe,
                        onValueChange = { statusVibe = it.take(60) },
                        label = { Text("Status vibe") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TelegramSwitchRow(
                    icon = Icons.Outlined.Visibility,
                    title = "Show Online Status",
                    subtitle = "Display \"online\" badge to contacts",
                    checked = isOnline,
                    onCheckedChange = { isOnline = it }
                )

                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 58.dp)
                )

                TelegramSwitchRow(
                    icon = Icons.Outlined.Public,
                    title = "Discoverable in Contacts",
                    subtitle = "Allow members to find you by @username",
                    checked = isDiscoverable,
                    onCheckedChange = { isDiscoverable = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        haptics.success()
                        onSaveProfile(displayName, handle, bio, statusVibe, isOnline, isDiscoverable)
                    },
                    enabled = !isBusy && displayName.trim().isNotEmpty() && handle.trim().length >= 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(48.dp)
                        .testTag("save_profile_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Saving…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(sectionSpacerBg)
            )

            // Account Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TelegramSectionLabel("Account")
                TelegramInfoRow(
                    icon = Icons.Outlined.AlternateEmail,
                    title = "@${handle.ifBlank { "member" }}",
                    subtitle = "Username · Tap to copy",
                    onClick = {
                        haptics.lightTap()
                        val h = "@${handle.ifBlank { "member" }}"
                        clipboardManager.setText(AnnotatedString(h))
                        Toast.makeText(context, "Copied $h", Toast.LENGTH_SHORT).show()
                    }
                )
                if (!privateUser?.email.isNullOrBlank()) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                    TelegramInfoRow(
                        icon = Icons.Outlined.Info,
                        title = privateUser?.email.orEmpty(),
                        subtitle = "Signed-in account"
                    )
                }
                if (onOpenSavedMessages != null) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                    TelegramInfoRow(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = "Saved Messages",
                        subtitle = "Open your private cloud storage thread",
                        onClick = {
                            haptics.lightTap()
                            onOpenSavedMessages()
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(sectionSpacerBg)
            )

            // Chat Appearance & Wallpaper Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TelegramSectionLabel("Chat Settings")

                TelegramSwitchRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Night Mode",
                    subtitle = if (isNightMode) "Univers Midnight theme active" else "Univers Daylight theme active",
                    checked = isNightMode,
                    onCheckedChange = { onToggleNightMode() },
                    modifier = Modifier.testTag("settings_night_mode_switch")
                )

                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 58.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Wallpaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(
                            text = "Chat Wallpaper",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = wallpaperStyle.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 58.dp, end = 16.dp, top = 4.dp, bottom = 14.dp)
                ) {
                    items(TelegramWallpaperStyle.entries, key = { it.name }) { style ->
                        val selected = wallpaperStyle == style
                        val swatchColors = if (isNightMode) {
                            listOf(style.darkTop, style.darkBottom)
                        } else {
                            listOf(style.lightTop, style.lightBottom)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptics.selectionTick()
                                    onSelectWallpaper(style)
                                }
                                .padding(4.dp)
                                .testTag("wallpaper_chip_${style.name}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(swatchColors))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) TelegramBlue else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = TelegramBlue,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Selected wallpaper",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = style.label.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 58.dp)
                )

                TelegramSwitchRow(
                    icon = Icons.Outlined.DoneAll,
                    title = "Send Read Receipts",
                    subtitle = "Clear unread badges & show ✓✓ read status",
                    checked = readReceipts,
                    onCheckedChange = { onUpdatePrivacy(it, notifications) }
                )

                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 58.dp)
                )

                TelegramSwitchRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications & Sounds",
                    subtitle = "Real-time message alerts & inline reply",
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onRequestNotificationPermission)
                            .testTag("enable_notifications_banner")
                    ) {
                        Text(
                            text = "Phone notification permission is off. Tap here to allow notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(sectionSpacerBg)
            )

            // Privacy & Sign Out
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 24.dp)
            ) {
                TelegramSectionLabel("Privacy & Security")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable {
                            haptics.lightTap()
                            showBlockedDialog = true
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("manage_blocked_users_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocked Users",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (blockedUserIds.isEmpty()) "None" else "${blockedUserIds.size} blocked",
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        haptics.impactMedium()
                        onSignOut()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
private fun TelegramSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun TelegramInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TelegramSwitchRow(
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
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
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
                checkedThumbColor = Color.White,
                checkedTrackColor = TelegramGreen,
                checkedBorderColor = TelegramGreen
            )
        )
    }
}
