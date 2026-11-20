package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Videocam
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
import com.example.ui.components.UniversMotion
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.TelegramGreen

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

    val needsAttentionBanner = !hasPhoneNotificationPermission || !notificationsEnabled

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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (totalUnreadMessages > 0L) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = if (totalUnreadMessages > 99L) "99+" else totalUnreadMessages.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                    text = "Mark all read",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Only show a clean notice when system permission or alerts are actually disabled
            if (needsAttentionBanner) {
                item(key = "notification_permission_notice", contentType = "notice_card") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!hasPhoneNotificationPermission) "Notifications are off" else "Alerts are paused",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (!hasPhoneNotificationPermission) {
                                        "Allow permission to receive messages and calls."
                                    } else {
                                        "Turn alerts back on to get notified."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    haptics.lightTap()
                                    if (!hasPhoneNotificationPermission) {
                                        onRequestNotificationPermission()
                                    } else {
                                        onToggleNotificationsEnabled(true)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("notif_enable_cta_button")
                            ) {
                                Text("Enable", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Activity Filter Pills
            item(key = "notification_filter_bar", contentType = "filter_bar") {
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
                            targetValue = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                            animationSpec = tween(160),
                            label = "notif_filter_bg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(160),
                            label = "notif_filter_fg"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .bouncyPressScale(0.96f)
                                .defaultMinSize(minHeight = 34.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .clickable {
                                    haptics.selectionTick()
                                    selectedFilterName = filter.name
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("notif_filter_${filter.name.lowercase()}")
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor
                            )
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }
                }
            }

            // Unread Messages Feed
            if (selectedFilter == NotificationFeedFilter.ALL || selectedFilter == NotificationFeedFilter.MESSAGES) {
                if (unreadConversations.isNotEmpty()) {
                    item(key = "section_unread_header", contentType = "section_header") {
                        NotificationSectionHeader(title = "Unread")
                    }
                    items(
                        items = unreadConversations,
                        key = { "unread_${it.id}" },
                        contentType = { "unread_card" }
                    ) { conv ->
                        val otherId = conv.otherParticipantId(currentUserId)
                        val otherProfile = otherId?.let { peopleMap[it] }
                        val title = if (conv.type != Conversation.TYPE_GROUP) {
                            otherProfile?.displayName?.takeIf { it.isNotBlank() }
                                ?: conv.displayTitleFor(currentUserId)
                        } else {
                            conv.displayTitleFor(currentUserId)
                        }
                        val avatarUrl = if (conv.type == Conversation.TYPE_GROUP) null else otherProfile?.photoUrl
                        val unreadCount = conv.unreadCountFor(currentUserId)
                        val preview = if (showMessagePreviews) {
                            conv.lastMessageText.ifBlank { "New message" }
                        } else {
                            "New message"
                        }
                        val timeText = remember(conv.lastMessageAt, conv.updatedAt) {
                            formatRelativeTime(conv.lastMessageAt ?: conv.updatedAt)
                        }

                        NotificationActivityCard(
                            title = title,
                            subtitle = preview,
                            timeLabel = timeText,
                            avatarName = title,
                            avatarUrl = avatarUrl,
                            isGroup = conv.type == Conversation.TYPE_GROUP,
                            isOnline = otherProfile?.isOnline == true,
                            trailingBadgeText = if (unreadCount > 99L) "99+" else unreadCount.toString(),
                            onClick = {
                                haptics.lightTap()
                                onOpenConversation(conv.id)
                            },
                            testTag = "notif_unread_card_${conv.id}",
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                placementSpec = UniversMotion.ListPlacementSpring
                            )
                        )
                    }
                }
            }

            // Recent Calls Feed
            if (selectedFilter == NotificationFeedFilter.ALL || selectedFilter == NotificationFeedFilter.CALLS) {
                if (recentCalls.isNotEmpty()) {
                    item(key = "section_calls_header", contentType = "section_header") {
                        NotificationSectionHeader(title = "Recent calls")
                    }
                    items(
                        items = recentCalls,
                        key = { "call_${it.id}" },
                        contentType = { "call_card" }
                    ) { call ->
                        val peerId = call.peerIdFor(currentUserId)
                        val peerName = call.peerNameFor(currentUserId)
                        val peerPhoto = call.peerPhotoUrlFor(currentUserId)
                        val isIncoming = call.calleeId == currentUserId
                        val isMissed = isIncoming && (call.status == CallSession.STATUS_MISSED || call.status == CallSession.STATUS_DECLINED || call.status == CallSession.STATUS_RINGING)
                        val callStatusText = when {
                            isMissed -> if (call.isVideoCall) "Missed video call" else "Missed voice call"
                            isIncoming -> if (call.isVideoCall) "Incoming video call" else "Incoming voice call"
                            else -> if (call.isVideoCall) "Outgoing video call" else "Outgoing voice call"
                        }
                        val timeText = remember(call.createdAt) {
                            formatRelativeTime(call.createdAt)
                        }

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
                            },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                placementSpec = UniversMotion.ListPlacementSpring
                            )
                        )
                    }
                }
            }

            // Muted Chats Section
            if (selectedFilter == NotificationFeedFilter.MUTED) {
                if (mutedConversations.isNotEmpty()) {
                    item(key = "section_muted_header", contentType = "section_header") {
                        NotificationSectionHeader(title = "Muted")
                    }
                    items(
                        items = mutedConversations,
                        key = { "muted_${it.id}" },
                        contentType = { "muted_card" }
                    ) { conv ->
                        val otherId = conv.otherParticipantId(currentUserId)
                        val otherProfile = otherId?.let { peopleMap[it] }
                        val title = conv.displayTitleFor(currentUserId)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                    placementSpec = UniversMotion.ListPlacementSpring
                                )
                                .fillMaxWidth()
                                .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                                    size = 42.dp,
                                    isGroup = conv.type == Conversation.TYPE_GROUP
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Muted",
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
                                    Text("Unmute", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Clean Empty State
            val isFeedEmpty = when (selectedFilter) {
                NotificationFeedFilter.ALL -> unreadConversations.isEmpty() && recentCalls.isEmpty()
                NotificationFeedFilter.MESSAGES -> unreadConversations.isEmpty()
                NotificationFeedFilter.CALLS -> recentCalls.isEmpty()
                NotificationFeedFilter.MUTED -> mutedConversations.isEmpty()
            }
            if (isFeedEmpty) {
                item(key = "notif_empty_state", contentType = "empty_state") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedFilter == NotificationFeedFilter.MUTED) {
                                    Icons.Outlined.NotificationsOff
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when (selectedFilter) {
                                    NotificationFeedFilter.MUTED -> "No muted chats"
                                    NotificationFeedFilter.CALLS -> "No recent calls"
                                    else -> "No unread notifications"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (selectedFilter) {
                                    NotificationFeedFilter.MUTED -> "Long-press a chat to mute notifications."
                                    NotificationFeedFilter.CALLS -> "Recent voice and video calls will appear here."
                                    else -> "New messages and missed calls appear here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Preferences
            item(key = "notif_preferences_header", contentType = "section_header") {
                Spacer(modifier = Modifier.height(6.dp))
                NotificationSectionHeader(title = "Preferences")
            }

            item(key = "notif_preferences_card", contentType = "preferences_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NotificationPrefToggleRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Push alerts",
                            subtitle = "Notify for new messages and calls",
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
                            modifier = Modifier.padding(start = 54.dp, end = 16.dp)
                        )
                        NotificationPrefToggleRow(
                            icon = Icons.Outlined.MarkChatUnread,
                            title = "In-app banners",
                            subtitle = "Show banner at the top while using the app",
                            checked = inAppBannersEnabled,
                            onCheckedChange = onToggleInAppBanners,
                            testTag = "notif_banners_switch"
                        )
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 54.dp, end = 16.dp)
                        )
                        NotificationPrefToggleRow(
                            icon = Icons.Outlined.Visibility,
                            title = "Message previews",
                            subtitle = "Show message text inside notifications",
                            checked = showMessagePreviews,
                            onCheckedChange = onToggleShowMessagePreviews,
                            testTag = "notif_previews_switch"
                        )
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 54.dp, end = 16.dp)
                        )
                        NotificationPrefToggleRow(
                            icon = Icons.Outlined.Vibration,
                            title = "Vibration",
                            subtitle = "Vibrate on alerts and actions",
                            checked = vibrateEnabled,
                            onCheckedChange = onToggleVibrate,
                            testTag = "notif_vibrate_switch"
                        )
                    }
                }
            }

            item(key = "notif_bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
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
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                size = 46.dp,
                isGroup = isGroup,
                showPresence = !isGroup,
                isOnline = isOnline
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trailingBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
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
    onCallBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                size = 44.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isMissed) Icons.AutoMirrored.Rounded.CallMissed else Icons.AutoMirrored.Rounded.CallReceived,
                        contentDescription = null,
                        tint = if (isMissed) MaterialTheme.colorScheme.error else TelegramGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$statusText · $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onCallBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                    contentDescription = "Call back $peerName",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationPrefToggleRow(
    icon: ImageVector,
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
            .defaultMinSize(minHeight = 58.dp)
            .clickable {
                haptics.selectionTick()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
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
