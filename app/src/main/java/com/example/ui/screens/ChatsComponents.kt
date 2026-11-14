package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.components.GlassIconBadge
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.IosRed
import com.example.ui.theme.OnlinePresenceGreen
import com.example.ui.theme.PulseBrushes

@Composable
fun ActivePeopleStoryStrip(
    people: List<UserProfile>,
    onNewGroupClick: () -> Unit,
    onPersonClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "quick_new_group") {
            GlassmorphismCard(
                shape = RoundedCornerShape(22.dp),
                highlighted = true,
                onClick = {
                    haptics.lightTap()
                    onNewGroupClick()
                },
                modifier = Modifier
                    .width(76.dp)
                    .height(94.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseBrushes.BrandGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = "New channel",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Channel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        items(people, key = { it.userId }) { person ->
            GlassmorphismCard(
                shape = RoundedCornerShape(22.dp),
                highlighted = person.isOnline,
                onClick = {
                    haptics.lightTap()
                    onPersonClick(person)
                },
                modifier = Modifier
                    .width(76.dp)
                    .height(94.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        UserAvatar(
                            name = person.displayName,
                            photoUrl = person.photoUrl,
                            size = 46.dp,
                            showPresence = true,
                            isOnline = person.isOnline,
                            showStoryRing = person.isOnline
                        )
                        if (person.isOnline) {
                            Surface(
                                shape = CircleShape,
                                color = OnlinePresenceGreen,
                                modifier = Modifier
                                    .offset(y = 4.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = person.displayName.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (person.isOnline) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SpotlightCarousel(
    conversations: List<Conversation>,
    currentUserId: String,
    peopleMap: Map<String, UserProfile>,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversations.isEmpty()) return
    val haptics = rememberIosHaptics()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconBadge(
                icon = Icons.Rounded.Bolt,
                contentDescription = null,
                size = 22.dp,
                iconSize = 13.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "JUMP BACK IN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations, key = { "spot_${it.id}" }) { conversation ->
                val isGroup = conversation.type == Conversation.TYPE_GROUP
                val title = conversation.displayTitleFor(currentUserId)
                val otherId = conversation.otherParticipantId(currentUserId)
                val otherProfile = otherId?.let { peopleMap[it] }
                val unreadCount = conversation.unreadCountFor(currentUserId)
                val avatarUrl = when {
                    isGroup -> null
                    otherProfile != null -> otherProfile.photoUrl
                    otherId != null -> conversation.participantPhotos[otherId]
                    else -> null
                }
                val preview = when {
                    conversation.lastMessageText.isBlank() -> if (isGroup) "Channel active · Tap to chat" else "Tap to start chatting"
                    conversation.lastMessageSenderId == currentUserId -> "You: ${conversation.lastMessageText}"
                    else -> conversation.lastMessageText
                }

                GlassmorphismCard(
                    shape = RoundedCornerShape(24.dp),
                    highlighted = unreadCount > 0L,
                    onClick = {
                        haptics.lightTap()
                        onOpenConversation(conversation.id)
                    },
                    modifier = Modifier
                        .width(222.dp)
                        .testTag("spotlight_card_${conversation.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            UserAvatar(
                                name = title,
                                photoUrl = avatarUrl,
                                size = 40.dp,
                                isGroup = isGroup,
                                showPresence = !isGroup && otherProfile != null,
                                isOnline = otherProfile?.isOnline == true,
                                showStoryRing = unreadCount > 0L
                            )

                            if (unreadCount > 0L) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(PulseBrushes.UnreadBadgeGradient)
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (unreadCount > 99L) "99+ new" else "$unreadCount new",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                GlassIconBadge(
                                    icon = Icons.Rounded.ArrowOutward,
                                    contentDescription = "Open conversation",
                                    size = 30.dp,
                                    iconSize = 16.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernConversationCard(
    conversation: Conversation,
    currentUserId: String,
    otherProfile: UserProfile?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isGroup = conversation.type == Conversation.TYPE_GROUP
    val title = conversation.displayTitleFor(currentUserId)
    val otherId = conversation.otherParticipantId(currentUserId)
    val avatarUrl = when {
        isGroup -> null
        otherProfile != null -> otherProfile.photoUrl
        otherId != null -> conversation.participantPhotos[otherId]
        else -> null
    }
    val unreadCount = conversation.unreadCountFor(currentUserId)
    val isOutgoing = conversation.lastMessageSenderId == currentUserId && conversation.lastMessageText.isNotBlank()
    val timeText = formatRelativeTime(conversation.lastMessageAt ?: conversation.updatedAt ?: conversation.createdAt)
    val preview = when {
        conversation.lastMessageText.isBlank() -> if (isGroup) "Channel created · Say hello!" else "Say hello!"
        isOutgoing -> conversation.lastMessageText
        isGroup && conversation.lastMessageSenderName.isNotBlank() ->
            "${conversation.lastMessageSenderName.substringBefore(" ")}: ${conversation.lastMessageText}"
        else -> conversation.lastMessageText
    }
    val haptics = rememberIosHaptics()
    val isImagePreview = conversation.lastMessageType == Message.TYPE_IMAGE
    val isPollPreview = conversation.lastMessageType == Message.TYPE_POLL

    GlassmorphismCard(
        shape = RoundedCornerShape(22.dp),
        highlighted = isSelected || unreadCount > 0L,
        tintAlpha = when {
            isSelected -> 0.62f
            unreadCount > 0L -> 0.54f
            else -> null
        },
        onClick = {
            haptics.lightTap()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation_card_${conversation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(
                    name = title,
                    photoUrl = avatarUrl,
                    size = 54.dp,
                    isGroup = isGroup,
                    showPresence = !isGroup && otherProfile != null,
                    isOnline = otherProfile?.isOnline == true,
                    showStoryRing = (!isGroup && otherProfile?.isOnline == true) || unreadCount > 0L
                )
                if (isGroup) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Text(
                            text = "${conversation.participantIds.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

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
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (unreadCount > 0L) FontWeight.ExtraBold else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isGroup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Channel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (unreadCount > 0L) FontWeight.Bold else FontWeight.Medium,
                            color = if (unreadCount > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
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
                        if (isOutgoing && !isImagePreview && !isPollPreview) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Sent",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        if (isImagePreview || isPollPreview) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isImagePreview) Icons.Rounded.PhotoCamera else Icons.Rounded.Poll,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isImagePreview) "Photo" else "Poll",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (unreadCount > 0L) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (unreadCount > 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (unreadCount > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PulseBrushes.UnreadBadgeGradient)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99L) "99+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
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
    onOpen: () -> Unit,
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
                targetValue = if (isTriggered) 1.18f else 0.92f,
                label = "swipe_delete_icon_scale"
            )
            val bgAlpha by animateFloatAsState(
                targetValue = if (isTriggered) 0.95f else 0.72f,
                label = "swipe_delete_bg_alpha"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isTriggered) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.35f),
                label = "swipe_delete_border"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                IosRed.copy(alpha = bgAlpha * 0.78f),
                                IosRed.copy(alpha = bgAlpha)
                            )
                        )
                    )
                    .border(
                        width = 0.9.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(22.dp)
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
                        contentDescription = if (isLeaveGroup) "Leave channel" else "Delete conversation",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        ModernConversationCard(
            conversation = conversation,
            currentUserId = currentUserId,
            otherProfile = otherProfile,
            isSelected = isSelected,
            onClick = onOpen
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
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassmorphismCard(
            shape = RoundedCornerShape(28.dp),
            highlighted = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_chats),
                    contentDescription = stringResource(R.string.empty_chats_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = if (isFiltered) "No matching conversations" else stringResource(R.string.empty_chats_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isFiltered) "Try clearing your search or switching tabs." else stringResource(R.string.empty_chats_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (!isFiltered) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                haptics.lightTap()
                                onStartDirect()
                            },
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Direct Chat", fontWeight = FontWeight.Bold)
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
    }
}
