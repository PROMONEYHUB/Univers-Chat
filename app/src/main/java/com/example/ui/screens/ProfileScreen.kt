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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PrivateUser
import com.example.data.model.UserProfile
import com.example.ui.components.ImageUtils
import com.example.ui.components.LocalTelegramWallpaper
import com.example.ui.components.TelegramWallpaperStyle
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
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
import com.example.ui.theme.TelegramGreen
import com.example.ui.theme.UniversAccentTheme
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
    accentTheme: UniversAccentTheme = LocalUniversAccent.current,
    onSelectAccentTheme: (UniversAccentTheme) -> Unit = {},
    onOpenNotifications: (() -> Unit)? = null,
    unreadNotificationsCount: Long = 0L,
    onOpenSavedMessages: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var displayName by remember(userProfile?.userId) { mutableStateOf(userProfile?.displayName.orEmpty()) }
    var handle by remember(userProfile?.userId) { mutableStateOf(userProfile?.handle.orEmpty()) }
    var bio by remember(userProfile?.userId) { mutableStateOf(userProfile?.bio.orEmpty()) }
    var statusVibe by remember(userProfile?.userId) { mutableStateOf(userProfile?.statusVibe ?: "Available") }
    var isOnline by remember(userProfile?.userId) { mutableStateOf(userProfile?.isOnline ?: true) }
    var isDiscoverable by remember(userProfile?.userId) { mutableStateOf(userProfile?.isDiscoverable ?: true) }
    var isEditProfileExpanded by rememberSaveable { mutableStateOf(false) }

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
    val accentPrimary = accentTheme.primary(isDark)

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
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 10.dp
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
                    Surface(
                        shape = CircleShape,
                        color = accentPrimary.copy(alpha = if (isDark) 0.18f else 0.11f),
                        modifier = Modifier
                            .bouncyPressScale(0.94f)
                            .clip(CircleShape)
                            .clickable {
                                haptics.lightTap()
                                isEditProfileExpanded = !isEditProfileExpanded
                            }
                            .testTag("toggle_edit_profile_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = accentPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isEditProfileExpanded) "Done" else "Edit",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flagship Profile Hero Card
            val heroShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(
                        if (isDark) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF141C27),
                                    accentPrimary.copy(alpha = 0.22f),
                                    Color(0xFF101722)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color.White,
                                    accentPrimary.copy(alpha = 0.08f),
                                    Color(0xFFF7FAFF)
                                )
                            )
                        }
                    )
                    .border(
                        width = 0.85.dp,
                        color = accentPrimary.copy(alpha = if (isDark) 0.28f else 0.18f),
                        shape = heroShape
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(106.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(94.dp)
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
                                size = 94.dp,
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

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !photoBusy) { launchAvatarPicker() }
                                .testTag("upload_profile_photo_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(accentTheme.brandGradient(isDark))
                                    .border(
                                        2.dp,
                                        if (isDark) Color(0xFF141C27) else Color.White,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = stringResource(R.string.upload_profile_photo),
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayName.ifBlank { "Univers Member" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (handle.isNotBlank()) {
                            Text(
                                text = "@$handle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = accentPrimary
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = (if (isOnline) TelegramGreen else MaterialTheme.colorScheme.outline)
                                .copy(alpha = if (isDark) 0.20f else 0.14f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) TelegramGreen else MaterialTheme.colorScheme.outline)
                                )
                                Text(
                                    text = statusVibe.ifBlank { if (isOnline) "Online" else "Away" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOnline) TelegramGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (bio.trim().isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bio.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentPrimary.copy(alpha = if (isDark) 0.18f else 0.10f),
                            modifier = Modifier
                                .bouncyPressScale(0.95f)
                                .clip(CircleShape)
                                .clickable(enabled = !photoBusy) { launchAvatarPicker() }
                                .testTag("profile_set_photo_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddAPhoto,
                                    contentDescription = null,
                                    tint = accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (currentPhotoUrl.isBlank()) "Add Photo" else "Change Photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentPrimary
                                )
                            }
                        }

                        if (currentPhotoUrl.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.11f),
                                modifier = Modifier
                                    .bouncyPressScale(0.95f)
                                    .clip(CircleShape)
                                    .clickable(enabled = !photoBusy) {
                                        haptics.impactMedium()
                                        onRemoveProfilePhoto()
                                    }
                                    .testTag("remove_profile_photo_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Remove",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Collapsible Edit Profile Card
            AnimatedVisibility(
                visible = isEditProfileExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SettingsGroupCard(title = "EDIT PROFILE") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                            shape = RoundedCornerShape(14.dp),
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
                                    Text("Shown on your profile card")
                                    Text("${bio.length}/160")
                                }
                            },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
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
                                    color = if (selected) accentPrimary else MaterialTheme.colorScheme.surfaceVariant,
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
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = statusVibe,
                            onValueChange = { statusVibe = it.take(60) },
                            label = { Text("Status Message") },
                            placeholder = { Text("Available, Focusing, In a meeting…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_status_input")
                        )

                        Button(
                            onClick = {
                                haptics.success()
                                onSaveProfile(displayName, handle, bio, statusVibe, isOnline, isDiscoverable)
                                isEditProfileExpanded = false
                            },
                            enabled = !isBusy && displayName.trim().isNotEmpty() && handle.trim().length >= 2,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(accentTheme.brandGradient(isDark))
                                .testTag("save_profile_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Saving…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Appearance & Live Colors Card
            SettingsGroupCard(title = "APPEARANCE & COLORS") {
                SettingsBadgeSwitchRow(
                    icon = Icons.Rounded.DarkMode,
                    badgeColor = BadgeSlateBlue,
                    title = "Night Mode",
                    subtitle = if (isNightMode) "Obsidian OLED dark active" else "Crisp Porcelain daylight active",
                    checked = isNightMode,
                    onCheckedChange = { onToggleNightMode() },
                    modifier = Modifier.testTag("settings_night_mode_switch")
                )

                SettingsInsetDivider()

                // Live Accent Theme Picker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsSquircleBadge(icon = Icons.Rounded.Palette, color = accentPrimary)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = accentTheme.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = accentPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        UniversAccentTheme.entries.forEach { themeOption ->
                            val selected = accentTheme == themeOption
                            val orbPrimary = themeOption.primary(isDark)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .bouncyPressScale(0.92f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptics.selectionTick()
                                        onSelectAccentTheme(themeOption)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("accent_theme_${themeOption.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (selected) {
                                                Modifier.border(2.5.dp, orbPrimary, CircleShape)
                                            } else Modifier
                                        )
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(themeOption.brandGradient(isDark))
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "${themeOption.label} selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = themeOption.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) orbPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                SettingsInsetDivider()

                // Chat Wallpaper Gallery with Live Mini Bubble Previews
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        SettingsSquircleBadge(icon = Icons.Rounded.Wallpaper, color = BadgeCyan)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Chat Wallpaper",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = wallpaperStyle.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = accentPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .bouncyPressScale(0.94f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptics.selectionTick()
                                        onSelectWallpaper(style)
                                    }
                                    .padding(2.dp)
                                    .testTag("wallpaper_chip_${style.name}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 74.dp, height = 96.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.verticalGradient(swatchColors))
                                        .border(
                                            width = if (selected) 2.5.dp else 0.8.dp,
                                            color = if (selected) accentPrimary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(7.dp)
                                ) {
                                    // Mini chat bubble preview inside each wallpaper swatch
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(38.dp)
                                                .height(11.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isNightMode) Color.White.copy(alpha = 0.16f)
                                                    else Color.White.copy(alpha = 0.92f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .width(42.dp)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentTheme.brandGradient(isNightMode))
                                        )
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isNightMode) Color.White.copy(alpha = 0.14f)
                                                    else Color.White.copy(alpha = 0.86f)
                                                )
                                        )
                                    }

                                    if (selected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accentPrimary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected wallpaper",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = style.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Notifications & Activity Center Card
            SettingsGroupCard(title = "NOTIFICATIONS & SOUNDS") {
                if (onOpenNotifications != null) {
                    SettingsBadgeNavigationRow(
                        icon = Icons.Rounded.NotificationsActive,
                        badgeColor = BadgeCoral,
                        title = "Notifications Center",
                        subtitle = if (notifications && hasPhoneNotificationPermission) {
                            "Manage alerts, banners & unread feed"
                        } else {
                            "Notifications paused · Tap to configure"
                        },
                        trailingPill = when {
                            unreadNotificationsCount > 0L -> if (unreadNotificationsCount > 99L) "99+" else "$unreadNotificationsCount"
                            !hasPhoneNotificationPermission -> "Off"
                            else -> null
                        },
                        trailingPillColor = if (!hasPhoneNotificationPermission) BadgeAmber else BadgeCoral,
                        onClick = {
                            haptics.lightTap()
                            onOpenNotifications()
                        },
                        modifier = Modifier.testTag("settings_open_notifications_row")
                    )
                    SettingsInsetDivider()
                }

                SettingsBadgeSwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    badgeColor = BadgeAmber,
                    title = "Instant Push Alerts",
                    subtitle = "Real-time message & call notifications",
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
                        shape = RoundedCornerShape(14.dp),
                        color = BadgeCoral.copy(alpha = if (isDark) 0.18f else 0.11f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onRequestNotificationPermission)
                            .testTag("enable_notifications_banner")
                    ) {
                        Text(
                            text = "Phone notification permission is off. Tap here to allow instant alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BadgeCoral,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Privacy & Security Card
            SettingsGroupCard(title = "PRIVACY & SECURITY") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleBadge(icon = Icons.Rounded.Shield, color = BadgeEmerald)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End-to-End Encryption",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Direct chats & calls secured with device keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = BadgeEmerald.copy(alpha = if (isDark) 0.2f else 0.14f)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BadgeEmerald,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                SettingsInsetDivider()

                SettingsBadgeSwitchRow(
                    icon = Icons.Rounded.DoneAll,
                    badgeColor = BadgeSapphire,
                    title = "Send Read Receipts",
                    subtitle = "Clear unread badges & show ✓✓ read status",
                    checked = readReceipts,
                    onCheckedChange = { onUpdatePrivacy(it, notifications) }
                )

                SettingsInsetDivider()

                SettingsBadgeSwitchRow(
                    icon = Icons.Rounded.Visibility,
                    badgeColor = BadgeEmerald,
                    title = "Show Online Status",
                    subtitle = "Display live \"online\" dot to contacts",
                    checked = isOnline,
                    onCheckedChange = { newVal ->
                        isOnline = newVal
                        onSaveProfile(displayName, handle, bio, statusVibe, newVal, isDiscoverable)
                    }
                )

                SettingsInsetDivider()

                SettingsBadgeSwitchRow(
                    icon = Icons.Rounded.Public,
                    badgeColor = BadgeCyan,
                    title = "Discoverable in Contacts",
                    subtitle = "Allow people to find you by @username",
                    checked = isDiscoverable,
                    onCheckedChange = { newVal ->
                        isDiscoverable = newVal
                        onSaveProfile(displayName, handle, bio, statusVibe, isOnline, newVal)
                    }
                )

                SettingsInsetDivider()

                SettingsBadgeNavigationRow(
                    icon = Icons.Rounded.Block,
                    badgeColor = BadgeRose,
                    title = "Blocked Users",
                    subtitle = if (blockedUserIds.isEmpty()) "No blocked accounts" else "${blockedUserIds.size} blocked",
                    onClick = {
                        haptics.lightTap()
                        showBlockedDialog = true
                    },
                    modifier = Modifier.testTag("manage_blocked_users_row")
                )
            }

            // Account & Cloud Card
            SettingsGroupCard(title = "ACCOUNT & CLOUD") {
                if (onOpenSavedMessages != null) {
                    SettingsBadgeNavigationRow(
                        icon = Icons.Rounded.Bookmark,
                        badgeColor = accentPrimary,
                        title = "Saved Messages",
                        subtitle = "Your private encrypted cloud notes",
                        onClick = {
                            haptics.lightTap()
                            onOpenSavedMessages()
                        }
                    )
                    SettingsInsetDivider()
                }

                SettingsBadgeNavigationRow(
                    icon = Icons.Rounded.AlternateEmail,
                    badgeColor = BadgeSapphire,
                    title = "@${handle.ifBlank { "member" }}",
                    subtitle = "Username · Tap to copy",
                    showChevron = false,
                    onClick = {
                        haptics.lightTap()
                        val h = "@${handle.ifBlank { "member" }}"
                        clipboardManager.setText(AnnotatedString(h))
                        Toast.makeText(context, "Copied $h", Toast.LENGTH_SHORT).show()
                    }
                )

                if (!privateUser?.email.isNullOrBlank()) {
                    SettingsInsetDivider()
                    SettingsBadgeNavigationRow(
                        icon = Icons.Rounded.Info,
                        badgeColor = BadgeSlateBlue,
                        title = privateUser?.email.orEmpty(),
                        subtitle = "Signed-in account",
                        showChevron = false,
                        onClick = {}
                    )
                }
            }

            // Log Out Pill Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.14f else 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyPressScale(0.97f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        haptics.impactMedium()
                        onSignOut()
                    }
                    .testTag("sign_out_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsTelegramDark.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 7.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.7.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.78f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingsSquircleBadge(
    icon: ImageVector,
    color: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun SettingsInsetDivider() {
    val isDark = LocalIsTelegramDark.current
    HorizontalDivider(
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.7f),
        modifier = Modifier.padding(start = 64.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsBadgeNavigationRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    trailingPill: String? = null,
    trailingPillColor: Color = BadgeCoral,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsSquircleBadge(icon = icon, color = badgeColor)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
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
        if (!trailingPill.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = trailingPillColor
            ) {
                Text(
                    text = trailingPill,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsBadgeSwitchRow(
    icon: ImageVector,
    badgeColor: Color,
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
            .defaultMinSize(minHeight = 58.dp)
            .clickable {
                haptics.selectionTick()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsSquircleBadge(icon = icon, color = badgeColor)
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
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
                checkedThumbColor = Color.White,
                checkedTrackColor = TelegramGreen,
                checkedBorderColor = TelegramGreen
            )
        )
    }
}
