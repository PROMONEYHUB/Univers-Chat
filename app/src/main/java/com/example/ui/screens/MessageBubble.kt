package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Message
import com.example.ui.components.ImageUtils
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatMessageClock
import com.example.ui.theme.IosDarkBubbleIncoming
import com.example.ui.theme.IosLightBubbleIncoming
import com.example.ui.theme.PulseBrushes

val QuickReactionEmojis = listOf("❤️", "👍", "😂", "🔥", "😮", "🙌")

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    isFirstInCluster: Boolean = true,
    isLastInCluster: Boolean = true,
    onLongPressOrTapActions: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onVotePoll: (Int) -> Unit,
    onOpenImage: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptics = LocalHapticFeedback.current
    val bubbleShape = if (isOwnMessage) {
        RoundedCornerShape(
            topStart = 20.dp,
            bottomStart = 20.dp,
            topEnd = if (isFirstInCluster) 20.dp else 6.dp,
            bottomEnd = if (isLastInCluster) 4.dp else 6.dp
        )
    } else {
        RoundedCornerShape(
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            topStart = if (isFirstInCluster) 20.dp else 6.dp,
            bottomStart = if (isLastInCluster) 4.dp else 6.dp
        )
    }

    val incomingBubbleColor = if (isDark) IosDarkBubbleIncoming else IosLightBubbleIncoming
    val bubbleBrush: Brush = if (isOwnMessage) {
        if (isDark) PulseBrushes.OutgoingBubbleDark else PulseBrushes.OutgoingBubbleLight
    } else {
        SolidColor(incomingBubbleColor)
    }
    val textColor = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.onSurface
    val subtleColor = if (isOwnMessage) {
        Color.White.copy(alpha = 0.80f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val groupedReactions = remember(message.reactions) {
        message.reactions.values.groupingBy { it }.eachCount().entries.toList()
    }
    val myReaction = message.reactions[currentUserId]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = if (isFirstInCluster) 8.dp else 2.dp,
                bottom = if (isLastInCluster) 4.dp else 1.dp
            ),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOwnMessage && isGroup) {
            if (isLastInCluster) {
                UserAvatar(
                    name = message.senderName,
                    photoUrl = message.senderPhotoUrl,
                    size = 30.dp
                )
            } else {
                Spacer(modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 314.dp)
        ) {
            if (!isOwnMessage && isGroup && isFirstInCluster) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 3.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBrush)
                    .combinedClickable(
                        onClick = onLongPressOrTapActions,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressOrTapActions()
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (!message.replyToPreview.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .height(IntrinsicSize.Min)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isOwnMessage) Color.White.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(
                                        if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = message.replyToSenderName ?: "Reply",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = message.replyToPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtleColor,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    if (message.type == Message.TYPE_IMAGE && !message.imageBase64.isNullOrBlank()) {
                        val bitmap = remember(message.imageBase64) {
                            ImageUtils.decodeBase64ToBitmap(message.imageBase64)
                        }
                        if (bitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(196.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (onOpenImage != null) onOpenImage(bitmap)
                                            else onLongPressOrTapActions()
                                        },
                                        onLongClick = onLongPressOrTapActions
                                    )
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = message.text.ifBlank { "Shared photo" },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ZoomIn,
                                            contentDescription = "Expand photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    if (message.type == Message.TYPE_POLL) {
                        PollBubbleContent(
                            message = message,
                            currentUserId = currentUserId,
                            isOwnMessage = isOwnMessage,
                            textColor = textColor,
                            subtleColor = subtleColor,
                            onVotePoll = onVotePoll
                        )
                    } else if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (message.isEdited) {
                            Text(
                                text = "Edited · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtleColor
                            )
                        }
                        Text(
                            text = formatMessageClock(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = subtleColor
                        )
                    }
                }
            }

            // iOS Tapback-style Reaction Pills
            if (groupedReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedReactions.forEach { (emoji, count) ->
                        val isMine = myReaction == emoji
                        Surface(
                            shape = CircleShape,
                            color = if (isMine) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    width = 0.8.dp,
                                    color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { onToggleReaction(emoji) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PollBubbleContent(
    message: Message,
    currentUserId: String,
    isOwnMessage: Boolean,
    textColor: Color,
    subtleColor: Color,
    onVotePoll: (Int) -> Unit
) {
    val totalVotes = message.pollVotes.size
    val myVote = message.pollVotes[currentUserId]?.toIntOrNull()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.Poll,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message.pollQuestion ?: message.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        message.pollOptions.forEachIndexed { index, option ->
            val votesForOption = message.pollVotes.values.count { it == index.toString() }
            val targetFraction = if (totalVotes > 0) votesForOption.toFloat() / totalVotes.toFloat() else 0f
            val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "poll_progress")
            val isSelected = myVote == index

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isOwnMessage) {
                    Color.White.copy(alpha = if (isSelected) 0.28f else 0.14f)
                } else {
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onVotePoll(index) }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Your vote",
                                    tint = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                        Text(
                            text = if (totalVotes > 0) "${(targetFraction * 100).toInt()}%" else "0%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subtleColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        color = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary,
                        trackColor = if (isOwnMessage) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "$totalVotes vote${if (totalVotes == 1) "" else "s"} · Tap an option to vote",
        style = MaterialTheme.typography.labelSmall,
        color = subtleColor
    )
}

@Composable
fun MessageActionsDialog(
    message: Message,
    isOwnMessage: Boolean,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.extraLarge
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Tapback",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickReactionEmojis.forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onReact(emoji)
                                    onDismiss()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))

                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.Reply,
                    label = "Reply",
                    onClick = {
                        onReply()
                        onDismiss()
                    }
                )

                if (isOwnMessage && message.type == Message.TYPE_TEXT) {
                    ActionRow(
                        icon = Icons.Rounded.Edit,
                        label = "Edit message",
                        onClick = {
                            onEdit()
                            onDismiss()
                        }
                    )
                }

                if (isOwnMessage) {
                    ActionRow(
                        icon = Icons.Rounded.Delete,
                        label = "Delete message",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDelete()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = tint
        )
    }
}
