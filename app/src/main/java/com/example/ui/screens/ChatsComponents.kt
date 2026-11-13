package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.PulseBrushes

@Composable
fun ActivePeopleStoryStrip(
    people: List<UserProfile>,
    onNewGroupClick: () -> Unit,
    onPersonClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "quick_new_group") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNewGroupClick)
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GroupAdd,
                        contentDescription = "New group",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "New Group",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(people, key = { it.userId }) { person ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPersonClick(person) }
                    .padding(vertical = 4.dp)
            ) {
                UserAvatar(
                    name = person.displayName,
                    photoUrl = person.photoUrl,
                    size = 54.dp,
                    showPresence = true,
                    isOnline = person.isOnline,
                    showStoryRing = person.isOnline
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = person.displayName.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (person.isOnline) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GroupedConversationRow(
    conversation: Conversation,
    currentUserId: String,
    otherProfile: UserProfile?,
    isSelected: Boolean,
    showBottomDivider: Boolean,
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
    val timeText = formatRelativeTime(conversation.lastMessageAt ?: conversation.updatedAt ?: conversation.createdAt)
    val preview = when {
        conversation.lastMessageText.isBlank() -> if (isGroup) "Channel created · Say hello!" else "Say hello!"
        conversation.lastMessageSenderId == currentUserId -> "You: ${conversation.lastMessageText}"
        isGroup && conversation.lastMessageSenderName.isNotBlank() ->
            "${conversation.lastMessageSenderName.substringBefore(" ")}: ${conversation.lastMessageText}"
        else -> conversation.lastMessageText
    }
    val isImagePreview = conversation.lastMessageType == Message.TYPE_IMAGE
    val isPollPreview = conversation.lastMessageType == Message.TYPE_POLL

    val rowBg by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        },
        label = "conv_row_bg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .testTag("conversation_card_${conversation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Classic iMessage Blue Unread Dot
            Box(
                modifier = Modifier.width(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (unreadCount > 0L) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            UserAvatar(
                name = title,
                photoUrl = avatarUrl,
                size = 52.dp,
                isGroup = isGroup,
                showPresence = !isGroup && otherProfile != null,
                isOnline = otherProfile?.isOnline == true,
                showStoryRing = !isGroup && otherProfile?.isOnline == true && unreadCount > 0L
            )

            Spacer(modifier = Modifier.width(12.dp))

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
                            fontWeight = if (unreadCount > 0L) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isGroup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${conversation.participantIds.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (unreadCount > 0L) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (unreadCount > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isImagePreview || isPollPreview) {
                            Icon(
                                imageVector = if (isImagePreview) Icons.Rounded.Image else Icons.Rounded.Poll,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (unreadCount > 0L) FontWeight.Medium else FontWeight.Normal,
                            color = if (unreadCount > 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (unreadCount > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PulseBrushes.UnreadBadgeGradient)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
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

        if (showBottomDivider) {
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 84.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun EmptyChatsView(
    isFiltered: Boolean,
    onStartDirect: () -> Unit,
    onCreateGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_chats),
            contentDescription = stringResource(R.string.empty_chats_desc),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(175.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (isFiltered) "No matching conversations" else stringResource(R.string.empty_chats_title),
            style = MaterialTheme.typography.titleLarge,
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onStartDirect, shape = CircleShape) {
                    Text("Direct Chat")
                }
                OutlinedButton(onClick = onCreateGroup, shape = CircleShape) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group")
                }
            }
        }
    }
}
