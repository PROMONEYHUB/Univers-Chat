package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramGreen
import com.example.ui.theme.TelegramSteelBlue
import com.example.ui.theme.TelegramTickGreen
import com.example.ui.viewmodel.ChatFilter

@Composable
fun TelegramFolderTabsBar(
    selectedFilter: ChatFilter,
    unreadConvCount: Int,
    onSelectFilter: (ChatFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val barBg = if (isDark) TelegramDarkBar else TelegramSteelBlue

    Surface(
        color = barBg,
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(ChatFilter.entries, key = { it.name }) { filter ->
                val isSelected = filter == selectedFilter
                val label = if (filter == ChatFilter.DIRECT) "Personal" else filter.label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 68.dp, minHeight = 44.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .clickable {
                            haptics.selectionTick()
                            onSelectFilter(filter)
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("filter_chip_${filter.name.lowercase()}")
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f)
                        )
                        if (filter == ChatFilter.UNREAD && unreadConvCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (unreadConvCount > 99) "99+" else unreadConvCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TelegramBlue else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun TelegramStoriesRow(
    people: List<UserProfile>,
    onPersonClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty()) return
    val haptics = rememberIosHaptics()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(people, key = { it.userId }) { person ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.lightTap()
                            onPersonClick(person)
                        }
                        .padding(vertical = 2.dp)
                ) {
                    UserAvatar(
                        name = person.displayName,
                        photoUrl = person.photoUrl,
                        size = 52.dp,
                        showPresence = true,
                        isOnline = person.isOnline,
                        showStoryRing = person.isOnline
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = person.displayName.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (person.isOnline) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramConversationRow(
    conversation: Conversation,
    currentUserId: String,
    otherProfile: UserProfile?,
    isSelected: Boolean,
    showBottomDivider: Boolean = true,
    typingText: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val isGroup = conversation.type == Conversation.TYPE_GROUP
    val isSelfChat = !isGroup && conversation.participantIds.distinct().size <= 1
    val title = if (!isGroup && !isSelfChat && !otherProfile?.displayName.isNullOrBlank()) {
        otherProfile!!.displayName
    } else {
        conversation.displayTitleFor(currentUserId)
    }
    val otherId = conversation.otherParticipantId(currentUserId)
    val avatarUrl = when {
        isGroup || isSelfChat -> null
        otherProfile != null -> otherProfile.photoUrl
        otherId != null -> conversation.participantPhotos[otherId]
        else -> null
    }
    val unreadCount = conversation.unreadCountFor(currentUserId)
    val isPinned = conversation.isPinnedBy(currentUserId)
    val isMuted = conversation.isMutedBy(currentUserId)
    val isOutgoing = conversation.lastMessageSenderId == currentUserId && conversation.lastMessageText.isNotBlank()
    val timeText = formatRelativeTime(conversation.lastMessageAt ?: conversation.updatedAt ?: conversation.createdAt)
    val isImagePreview = conversation.lastMessageType == Message.TYPE_IMAGE
    val isPollPreview = conversation.lastMessageType == Message.TYPE_POLL
    val isVoicePreview = conversation.lastMessageType == Message.TYPE_AUDIO

    val rowBg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.48f)
        isPinned -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.25f else 0.30f)
        else -> MaterialTheme.colorScheme.background
    }

    val previewText = when {
        conversation.lastMessageText.isBlank() -> if (isGroup) "Group created" else if (isSelfChat) "Cloud notes & saved media" else "Tap to send a message"
        else -> conversation.lastMessageText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = {
                    haptics.lightTap()
                    onClick()
                },
                onLongClick = {
                    haptics.impactMedium()
                    onLongClick()
                }
            )
            .testTag("conversation_card_${conversation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = title,
                photoUrl = avatarUrl,
                size = 54.dp,
                isGroup = isGroup,
                isSavedMessages = isSelfChat,
                showPresence = !isGroup && !isSelfChat && otherProfile != null,
                isOnline = otherProfile?.isOnline == true,
                showStoryRing = false
            )

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isGroup) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isMuted) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Rounded.NotificationsOff,
                                contentDescription = "Muted",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOutgoing && typingText == null) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Sent",
                                tint = if (isDark) MaterialTheme.colorScheme.primary else TelegramTickGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (unreadCount > 0L && !isMuted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (typingText != null) {
                            TypingWaveDots(
                                dotColor = MaterialTheme.colorScheme.primary,
                                dotSize = 5.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = typingText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            if (isImagePreview || isPollPreview || isVoicePreview) {
                                Icon(
                                    imageVector = when {
                                        isImagePreview -> Icons.Rounded.PhotoCamera
                                        isVoicePreview -> Icons.Rounded.Mic
                                        else -> Icons.Rounded.Poll
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            val senderPrefix = when {
                                isOutgoing && conversation.lastMessageText.isNotBlank() -> "You: "
                                isGroup && conversation.lastMessageSenderName.isNotBlank() && conversation.lastMessageText.isNotBlank() ->
                                    "${conversation.lastMessageSenderName.substringBefore(" ")}: "
                                else -> ""
                            }
                            val formattedPreview = buildAnnotatedString {
                                if (senderPrefix.isNotEmpty()) {
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    ) {
                                        append(senderPrefix)
                                    }
                                }
                                append(previewText)
                            }
                            Text(
                                text = formattedPreview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    when {
                        unreadCount > 0L -> {
                            Surface(
                                shape = CircleShape,
                                color = if (isMuted) {
                                    MaterialTheme.colorScheme.outline
                                } else if (isDark) {
                                    TelegramBlue
                                } else {
                                    TelegramGreen
                                },
                                modifier = Modifier.defaultMinSize(minWidth = 24.dp)
                            ) {
                                Text(
                                    text = if (unreadCount > 99L) "99+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        isPinned -> {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showBottomDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 81.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteConversationItem(
    conversation: Conversation,
    currentUserId: String,
    otherProfile: UserProfile?,
    isSelected: Boolean,
    isPendingDelete: Boolean,
    showBottomDivider: Boolean = true,
    typingText: String? = null,
    onOpen: () -> Unit,
    onLongPress: () -> Unit = {},
    onDeleteOrLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isLeaveGroup = conversation.type == Conversation.TYPE_GROUP && conversation.createdBy != currentUserId
    var deleteTriggered by remember(conversation.id) { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            value == SwipeToDismissBoxValue.EndToStart
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.45f }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart &&
            dismissState.currentValue == SwipeToDismissBoxValue.Settled
        ) {
            haptics.selectionTick()
        }
    }

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && !deleteTriggered) {
            deleteTriggered = true
            haptics.impactMedium()
            onDeleteOrLeave()
        }
    }

    LaunchedEffect(isPendingDelete) {
        if (!isPendingDelete && deleteTriggered) {
            deleteTriggered = false
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier.testTag("swipe_conversation_${conversation.id}"),
        backgroundContent = {
            val isTriggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val iconScale by animateFloatAsState(
                targetValue = if (isTriggered) 1.15f else 0.92f,
                label = "swipe_delete_icon_scale"
            )
            val bgAlpha by animateFloatAsState(
                targetValue = if (isTriggered) 0.96f else 0.78f,
                label = "swipe_delete_bg_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                IosRed.copy(alpha = bgAlpha * 0.85f),
                                IosRed.copy(alpha = bgAlpha)
                            )
                        )
                    )
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                ) {
                    Text(
                        text = if (isLeaveGroup) "Leave" else "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = if (isLeaveGroup) Icons.AutoMirrored.Rounded.ExitToApp else Icons.Rounded.DeleteOutline,
                        contentDescription = if (isLeaveGroup) "Leave group" else "Delete chat",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        TelegramConversationRow(
            conversation = conversation,
            currentUserId = currentUserId,
            otherProfile = otherProfile,
            isSelected = isSelected,
            showBottomDivider = showBottomDivider,
            typingText = typingText,
            onClick = onOpen,
            onLongClick = onLongPress
        )
    }
}

@Composable
fun EmptyChatsView(
    isFiltered: Boolean,
    onStartDirect: () -> Unit,
    onCreateGroup: () -> Unit
) {
    val haptics = rememberIosHaptics()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_chats),
            contentDescription = stringResource(R.string.empty_chats_desc),
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (isFiltered) "No matching chats" else stringResource(R.string.empty_chats_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isFiltered) "Try a different search query or switch folder tabs." else stringResource(R.string.empty_chats_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!isFiltered) {
            Spacer(modifier = Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        haptics.lightTap()
                        onStartDirect()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Message", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        haptics.lightTap()
                        onCreateGroup()
                    },
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
