package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CallSession
import com.example.data.model.CallTarget
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.BadgeAmber
import com.example.ui.theme.BadgeCoral
import com.example.ui.theme.BadgeCyan
import com.example.ui.theme.BadgeEmerald
import com.example.ui.theme.BadgeSapphire
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent
import com.example.ui.theme.TelegramGreen
import com.example.ui.theme.TelegramOrange
import com.example.ui.theme.TelegramRed

private enum class NotificationFeedFilter(val label: String) {
    ALL("All"),
    MESSAGES("Unread"),
    CALLS("Calls"),
    MUTED("Muted")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    currentUserId: String,
    conversations: List<Conversation>,
    people: List<UserProfile>,
    callHistory: List<CallSession>,
    blockedUserIds: Set<String>,
    notificationsEnabled: Boolean,
    hasPhoneNotificationPermission: Boolean,
    inAppBannersEnabled: Boolean,
    showMessagePreviews: Boolean,
    vibrateEnabled: Boolean,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onToggleMuteConversation: (Conversation) -> Unit,
    onMarkAllRead: () -> Unit,
    onCallBack: (target: CallTarget, isVideoCall: Boolean) -> Unit,
    onToggleNotificationsEnabled: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleInAppBanners: (Boolean) -> Unit,
    onToggleShowMessagePreviews: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val accent = LocalUniversAccent.current

    var selectedFilterName by rememberSaveable { mutableStateOf(NotificationFeedFilter.ALL.name) }
    val selectedFilter = remember(selectedFilterName) { NotificationFeedFilter.valueOf(selectedFilterName) }

    val peopleMap = remember(people) { people.associateBy { it.userId } }

    val unreadConversations = remember(conversations, currentUserId, blockedUserIds) {
        conversations
            .filter { conv ->
                val otherId = conv.otherParticipantId(currentUserId)
                (otherId == null || otherId !in blockedUserIds) && conv.unreadCountFor(currentUserId) > 0L
            }
            .sortedByDescending { (it.lastMessageAt ?: it.updatedAt)?.toDate()?.time ?: 0L }
    }

    val mutedConversations = remember(conversations, currentUserId) {
        conversations.filter { it.isMutedBy(currentUserId) }
    }

    val recentCalls = remember(callHistory, currentUserId, blockedUserIds) {
        callHistory
            .filter { session ->
                val peerId = session.peerIdFor(currentUserId)
                peerId.isNotBlank() && peerId !in blockedUserIds
            }
            .take(10)
    }

    val totalUnreadMessages = remember(unreadConversations, currentUserId) {
        unreadConversations.sumOf { it.unreadCountFor(currentUserId) }
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
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                onBack()
                            },
                            modifier = Modifier.testTag("notifications_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (totalUnreadMessages > 0L) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accent.brandGradient(isDark))
                                        .padding(horizontal = 9.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (totalUnreadMessages > 99L) "99+" else totalUnreadMessages.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (unreadConversations.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    haptics.success()
                                    onMarkAllRead()
                                },
                                modifier = Modifier.testTag("notifications_mark_all_read")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Read all",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Live Delivery Status Banner
            item(key = "notification_health_card") {
                NotificationHealthHeroCard(
                    notificationsEnabled = notificationsEnabled,
                    hasPermission = hasPhoneNotificationPermission,
                    onRequestPermission = {
                        haptics.lightTap()
                        onRequestNotificationPermission()
                    },
                    onEnableMaster = {
                        haptics.selectionTick()
                        if (!hasPhoneNotificationPermission) {
                            onRequestNotificationPermission()
                        }
                        onToggleNotificationsEnabled(true)
                    }
                )
            }

            // 2. Activity Filter Pills
            item(key = "notification_filter_bar") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(NotificationFeedFilter.entries, key = { it.name }) { filter ->
                        val selected = selectedFilter == filter
                        val count = when (filter) {
                            NotificationFeedFilter.ALL -> unreadConversations.size + recentCalls.size
                            NotificationFeedFilter.MESSAGES -> unreadConversations.size
                            NotificationFeedFilter.CALLS -> recentCalls.size
                            NotificationFeedFilter.MUTED -> mutedConversations.size
                        }
                        val bgColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            animationSpec = tween(180),
                            label = "notif_filter_bg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "notif_filter_fg"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .bouncyPressScale(0.95f)
                                .defaultMinSize(minHeight = 38.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = if (selected) accent.brandGradient(isDark)
                                    else Brush.linearGradient(listOf(bgColor, bgColor))
                                )
                                .border(
                                    width = if (selected) 0.dp else 0.8.dp,
                                    color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptics.selectionTick()
                                    selectedFilterName = filter.name
                                }
                                .padding(horizontal = 15.dp, vertical = 8.dp)
                                .testTag("notif_filter_${filter.name.lowercase()}")
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                            if (count > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (selected) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Unread Messages Feed
            if (selectedFilter == NotificationFeedFilter.ALL || selectedFilter == NotificationFeedFilter.MESSAGES) {
                if (unreadConversations.isNotEmpty()) {
                    item(key = "section_unread_header") {
                        NotificationSectionHeader(title = "Unread Messages", badge = "${unreadConversations.size}")
                    }
                    items(unreadConversations, key = { "unread_${it.id}" }) { conv ->
                        val otherId = conv.otherParticipantId(currentUserId)
                        val otherProfile = otherId?.let { peopleMap[it] }
                        val title = if (conv.type != Conversation.TYPE_GROUP && !otherProfile?.displayName.isNullOrBlank()) {
                            otherProfile!!.displayName
                        } else {
                            conv.displayTitleFor(currentUserId)
                        }
                        val avatarUrl = if (conv.type == Conversation.TYPE_GROUP) null else otherProfile?.photoUrl
                        val unreadCount = conv.unreadCountFor(currentUserId)
                        val preview = if (showMessagePreviews) {
                            conv.lastMessageText.ifBlank { "New message" }
                        } else {
                            "New encrypted message"
                        }
                        val timeText = formatRelativeTime(conv.lastMessageAt ?: conv.updatedAt)

                        NotificationActivityCard(
                            title = title,
                            subtitle = preview,
                            timeLabel = timeText,
                            avatarName = title,
                            avatarUrl = avatarUrl,
                            isGroup = conv.type == Conversation.TYPE_GROUP,
                            isOnline = otherProfile?.isOnline == true,
                            trailingBadgeText = if (unreadCount > 99L) "99+" else unreadCount.toString(),
                            accentTint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                haptics.lightTap()
                                onOpenConversation(conv.id)
                            },
                            testTag = "notif_unread_card_${conv.id}"
                        )
                    }
                }
            }

            // 4. Recent Calls Feed
            if (selectedFilter == NotificationFeedFilter.ALL || selectedFilter == NotificationFeedFilter.CALLS) {
                if (recentCalls.isNotEmpty()) {
                    item(key = "section_calls_header") {
                        NotificationSectionHeader(title = "Recent Call Alerts", badge = "${recentCalls.size}")
                    }
                    items(recentCalls, key = { "call_${it.id}" }) { call ->
                        val peerId = call.peerIdFor(currentUserId)
                        val peerName = call.peerNameFor(currentUserId)
                        val peerPhoto = call.peerPhotoUrlFor(currentUserId)
                        val isIncoming = call.calleeId == currentUserId
                        val isMissed = isIncoming && (call.status == CallSession.STATUS_MISSED || call.status == CallSession.STATUS_DECLINED || call.status == CallSession.STATUS_RINGING)
                        val callStatusText = when {
                            isMissed -> if (call.isVideoCall) "Missed HD Video Call" else "Missed HD Voice Call"
                            isIncoming -> if (call.isVideoCall) "Incoming HD Video Call" else "Incoming HD Voice Call"
                            else -> if (call.isVideoCall) "Outgoing HD Video Call" else "Outgoing HD Voice Call"
                        }
                        val timeText = formatRelativeTime(call.createdAt)

                        CallNotificationCard(
                            peerName = peerName,
                            peerPhoto = peerPhoto,
                            statusText = callStatusText,
                            timeText = timeText,
                            isMissed = isMissed,
                            isVideo = call.isVideoCall,
                            onCallBack = {
                                haptics.lightTap()
                                onCallBack(
                                    CallTarget(
                                        userId = peerId,
                                        displayName = peerName,
                                        photoUrl = peerPhoto,
                                        conversationId = call.conversationId
                                    ),
                                    call.isVideoCall
                                )
                            }
                        )
                    }
                }
            }

            // 5. Muted Chats Section
            if (selectedFilter == NotificationFeedFilter.MUTED) {
                if (mutedConversations.isNotEmpty()) {
                    item(key = "section_muted_header") {
                        NotificationSectionHeader(title = "Muted Conversations", badge = "${mutedConversations.size}")
                    }
                    items(mutedConversations, key = { "muted_${it.id}" }) { conv ->
                        val otherId = conv.otherParticipantId(currentUserId)
                        val otherProfile = otherId?.let { peopleMap[it] }
                        val title = conv.displayTitleFor(currentUserId)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = title,
                                    photoUrl = otherProfile?.photoUrl,
                                    size = 44.dp,
                                    isGroup = conv.type == Conversation.TYPE_GROUP
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Alerts silenced",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        haptics.selectionTick()
                                        onToggleMuteConversation(conv)
                                    }
                                ) {
                                    Text("Unmute", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 6. Clean Empty State when active filter has no items
            val isFeedEmpty = when (selectedFilter) {
                NotificationFeedFilter.ALL -> unreadConversations.isEmpty() && recentCalls.isEmpty()
                NotificationFeedFilter.MESSAGES -> unreadConversations.isEmpty()
                NotificationFeedFilter.CALLS -> recentCalls.isEmpty()
                NotificationFeedFilter.MUTED -> mutedConversations.isEmpty()
            }
            if (isFeedEmpty) {
                item(key = "notif_empty_state") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(accent.brandGradient(isDark)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (selectedFilter) {
                                        NotificationFeedFilter.MUTED -> Icons.Rounded.NotificationsOff
                                        else -> Icons.Rounded.NotificationsNone
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = when (selectedFilter) {
                                    NotificationFeedFilter.MUTED -> "No muted chats"
                                    NotificationFeedFilter.CALLS -> "No recent call alerts"
                                    else -> "You're all caught up"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (selectedFilter) {
                                    NotificationFeedFilter.MUTED -> "Long-press any chat to silence notifications for that thread."
                                    NotificationFeedFilter.CALLS -> "Incoming and missed HD voice & video calls will appear here."
                                    else -> "New messages, replies, and missed calls will appear here in real time."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 7. Alert & Sound Preferences Group
            item(key = "notif_preferences_header") {
                Spacer(modifier = Modifier.height(4.dp))
                NotificationSectionHeader(title = "Alert Preferences", badge = null)
            }

            item(key = "notif_preferences_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NotificationPrefToggleRow(
                            icon = Icons.Rounded.NotificationsActive,
                            badgeColor = BadgeCoral,
                            title = "Push & Background Alerts",
                            subtitle = "Instant message & incoming call delivery",
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasPhoneNotificationPermission) {
                                    onRequestNotificationPermission()
                                }
                                onToggleNotificationsEnabled(enabled)
                            },
                            testTag = "notif_master_switch"
                        )
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 66.dp)
                        )
                        NotificationPrefToggleRow(
                            icon = Icons.Rounded.MarkChatUnread,
                            badgeColor = BadgeSapphire,
                            title = "Floating In-App Banners",
                            subtitle = "Dynamic top pill when messages arrive",
                            checked = inAppBannersEnabled,
                            onCheckedChange = onToggleInAppBanners,
                            testTag = "notif_banners_switch"
                        )
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 66.dp)
                        )
                        NotificationPrefToggleRow(
                            icon = Icons.Rounded.Visibility,
                            badgeColor = BadgeCyan,
                            title = "Message Previews",
                            subtitle = "Show sender text inside notification pills",
                            checked = showMessagePreviews,
                            onCheckedChange = onToggleShowMessagePreviews,
                            testTag = "notif_previews_switch"
                        )
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 66.dp)
                        )
                        NotificationPrefToggleRow(
                            icon = Icons.Rounded.Vibration,
                            badgeColor = BadgeAmber,
                            title = "Vibration & Haptics",
                            subtitle = "Tactile feedback on alerts & actions",
                            checked = vibrateEnabled,
                            onCheckedChange = onToggleVibrate,
                            testTag = "notif_vibrate_switch"
                        )
                    }
                }
            }

            item(key = "notif_bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NotificationHealthHeroCard(
    notificationsEnabled: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onEnableMaster: () -> Unit
) {
    val isDark = LocalIsTelegramDark.current
    val pulseTransition = rememberInfiniteTransition(label = "notif_live_dot")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isAllActive = notificationsEnabled && hasPermission
    val gradientBrush = when {
        !hasPermission -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF4F5E), Color(0xFFFF8A3D))
        )
        !notificationsEnabled -> Brush.linearGradient(
            colors = if (isDark) listOf(Color(0xFF1B2432), Color(0xFF131A26))
            else listOf(Color(0xFF334155), Color(0xFF1E293B))
        )
        else -> Brush.linearGradient(
            colors = if (isDark) listOf(Color(0xFF0A2E26), Color(0xFF0B2436))
            else listOf(Color(0xFF059669), Color(0xFF0284C7))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradientBrush)
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = if (isDark) 0.14f else 0.24f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !hasPermission -> Icons.Rounded.NotificationsOff
                        isAllActive -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.NotificationsNone
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAllActive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = when {
                            !hasPermission -> "Alerts Permission Off"
                            !notificationsEnabled -> "Notifications Paused"
                            else -> "Real-Time Alerts Active"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        !hasPermission -> "Allow phone notifications so you never miss messages or calls."
                        !notificationsEnabled -> "Turn alerts back on to hear incoming messages & calls."
                        else -> "Instant encrypted push alerts & floating banners are ready."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
            if (!isAllActive) {
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = if (!hasPermission) onRequestPermission else onEnableMaster,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("notif_enable_cta_button")
                ) {
                    Text("Allow", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(
    title: String,
    badge: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!badge.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationActivityCard(
    title: String,
    subtitle: String,
    timeLabel: String,
    avatarName: String,
    avatarUrl: String?,
    isGroup: Boolean,
    isOnline: Boolean,
    trailingBadgeText: String,
    accentTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val accent = LocalUniversAccent.current
    val isDark = LocalIsTelegramDark.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(0.7.dp, accentTint.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = avatarName,
                photoUrl = avatarUrl,
                size = 48.dp,
                isGroup = isGroup,
                showPresence = !isGroup,
                isOnline = isOnline,
                showStoryRing = isOnline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 26.dp, minHeight = 26.dp)
                    .clip(CircleShape)
                    .background(accent.brandGradient(isDark))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trailingBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CallNotificationCard(
    peerName: String,
    peerPhoto: String?,
    statusText: String,
    timeText: String,
    isMissed: Boolean,
    isVideo: Boolean,
    onCallBack: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.7.dp,
                color = if (isMissed) TelegramRed.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = peerName,
                photoUrl = peerPhoto,
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isMissed) Icons.Rounded.CallMissed else Icons.Rounded.CallReceived,
                        contentDescription = null,
                        tint = if (isMissed) TelegramRed else TelegramGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$statusText · $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMissed) TelegramRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onCallBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMissed) TelegramRed.copy(alpha = 0.14f)
                        else BadgeEmerald.copy(alpha = 0.14f)
                    )
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                    contentDescription = "Call back $peerName",
                    tint = if (isMissed) TelegramRed else BadgeEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationPrefToggleRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val haptics = rememberIosHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 62.dp)
            .clickable {
                haptics.selectionTick()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.selectionTick()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
