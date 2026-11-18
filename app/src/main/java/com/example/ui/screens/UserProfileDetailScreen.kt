package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.components.ImageUtils
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramSteelBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDetailScreen(
    profile: UserProfile?,
    isBlocked: Boolean,
    onBack: () -> Unit,
    onStartChat: (UserProfile) -> Unit,
    onVoiceCall: (UserProfile) -> Unit,
    onVideoCall: (UserProfile) -> Unit,
    onToggleBlock: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val cosmicHeaderBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)
    val sectionSpacerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    var showFullPhoto by remember { mutableStateOf(false) }

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
                    IconButton(
                        onClick = {
                            haptics.lightTap()
                            onBack()
                        },
                        modifier = Modifier.testTag("user_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            )
        }
    ) { innerPadding ->
        if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This user profile is unavailable.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Univers Cosmic Hero Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cosmicHeaderBrush)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = profile.photoUrl.isNotBlank()) {
                            haptics.lightTap()
                            showFullPhoto = true
                        }
                        .testTag("user_profile_avatar")
                ) {
                    UserAvatar(
                        name = profile.displayName,
                        photoUrl = profile.photoUrl,
                        size = 104.dp,
                        showPresence = !isBlocked,
                        isOnline = profile.isOnline,
                        showStoryRing = !isBlocked && profile.isOnline
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = profile.displayName.ifBlank { "Univers Member" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val presenceText = when {
                    isBlocked -> "blocked"
                    profile.isOnline -> "online"
                    profile.lastSeenAt != null -> "last seen ${formatRelativeTime(profile.lastSeenAt)}"
                    else -> "last seen recently"
                }
                Text(
                    text = presenceText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (profile.isOnline && !isBlocked) Color(0xFFB3E5FC) else Color.White.copy(alpha = 0.80f),
                    fontWeight = if (profile.isOnline && !isBlocked) FontWeight.SemiBold else FontWeight.Normal
                )

                if (profile.statusVibe.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = profile.statusVibe,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Primary Action Strip (Message / Call / Video)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileQuickActionTile(
                        icon = Icons.AutoMirrored.Rounded.Send,
                        label = "Message",
                        enabled = true,
                        onClick = {
                            haptics.lightTap()
                            onStartChat(profile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("user_profile_message_button")
                    )
                    ProfileQuickActionTile(
                        icon = Icons.Rounded.Call,
                        label = "Voice Call",
                        enabled = !isBlocked,
                        onClick = {
                            haptics.lightTap()
                            onVoiceCall(profile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("user_profile_voice_call_button")
                    )
                    ProfileQuickActionTile(
                        icon = Icons.Rounded.Videocam,
                        label = "Video Call",
                        enabled = !isBlocked,
                        onClick = {
                            haptics.lightTap()
                            onVideoCall(profile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("user_profile_video_call_button")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(sectionSpacerBg)
            )

            // Info Section (Bio, Username, Status)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Info",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)
                )

                UserProfileInfoRow(
                    icon = Icons.Outlined.Info,
                    value = profile.bio.ifBlank { "No bio yet" },
                    label = "Bio",
                    isPlaceholder = profile.bio.isBlank()
                )

                if (profile.handle.isNotBlank()) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                    UserProfileInfoRow(
                        icon = Icons.Outlined.AlternateEmail,
                        value = "@${profile.handle}",
                        label = "Username · Tap to copy",
                        onClick = {
                            haptics.lightTap()
                            val text = "@${profile.handle}"
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (profile.statusVibe.isNotBlank()) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                    UserProfileInfoRow(
                        icon = Icons.Outlined.Mood,
                        value = profile.statusVibe,
                        label = "Status vibe"
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(sectionSpacerBg)
            )

            // Privacy Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable {
                            haptics.impactMedium()
                            onToggleBlock(profile)
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("user_profile_block_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = if (isBlocked) TelegramBlue else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = if (isBlocked) "Unblock ${profile.displayName}" else "Block ${profile.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isBlocked) TelegramBlue else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showFullPhoto && profile != null && profile.photoUrl.isNotBlank()) {
        AvatarFullScreenDialog(
            displayName = profile.displayName,
            photoUrl = profile.photoUrl,
            onDismiss = { showFullPhoto = false }
        )
    }
}

@Composable
private fun ProfileQuickActionTile(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UserProfileInfoRow(
    icon: ImageVector,
    value: String,
    label: String,
    isPlaceholder: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Medium,
                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AvatarFullScreenDialog(
    displayName: String,
    photoUrl: String,
    onDismiss: () -> Unit,
    onChangePhoto: (() -> Unit)? = null,
    onRemovePhoto: (() -> Unit)? = null
) {
    val decodedBitmap = remember(photoUrl) {
        if (ImageUtils.isDataUri(photoUrl)) ImageUtils.decodeDataUriToBitmap(photoUrl) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(onClick = onDismiss)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayName.ifBlank { "Profile Photo" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_avatar_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close photo preview",
                        tint = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A2430)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    decodedBitmap != null -> {
                        Image(
                            bitmap = decodedBitmap.asImageBitmap(),
                            contentDescription = "$displayName profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    photoUrl.isNotBlank() && !ImageUtils.isDataUri(photoUrl) -> {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "$displayName profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (onChangePhoto != null || onRemovePhoto != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onChangePhoto != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onChangePhoto()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelegramBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Photo", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (onRemovePhoto != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onRemovePhoto()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
