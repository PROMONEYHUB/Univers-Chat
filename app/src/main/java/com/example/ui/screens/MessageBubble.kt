package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PushPin
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.VoicePlaybackState
import com.example.audio.formatDurationSec
import com.example.data.model.Message
import com.example.ui.components.ImageUtils
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatMessageClock
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.PulseBrushes
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBlue
import com.example.ui.theme.TelegramIncomingBubbleDark
import com.example.ui.theme.TelegramIncomingBubbleLight
import com.example.ui.theme.TelegramOutgoingSubtleDark
import com.example.ui.theme.TelegramOutgoingSubtleLight
import com.example.ui.theme.TelegramOutgoingTextLight
import com.example.ui.theme.TelegramTickGreen

val QuickReactionEmojis = listOf("❤️", "👍", "🔥", "😂", "🎉", "😢")

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
    voicePlaybackState: VoicePlaybackState = VoicePlaybackState(),
    onToggleVoicePlayback: ((Message) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    val haptics = rememberIosHaptics()

    val bubbleShape = if (isOwnMessage) {
        RoundedCornerShape(
            topStart = 18.dp,
            bottomStart = 18.dp,
            topEnd = if (isFirstInCluster) 18.dp else 6.dp,
            bottomEnd = if (isLastInCluster) 4.dp else 6.dp
        )
    } else {
        RoundedCornerShape(
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            topStart = if (isFirstInCluster) 18.dp else 6.dp,
            bottomStart = if (isLastInCluster) 4.dp else 6.dp
        )
    }

    val bubbleBrush: Brush = if (isOwnMessage) {
        if (isDark) PulseBrushes.OutgoingBubbleDark else PulseBrushes.OutgoingBubbleLight
    } else {
        SolidColor(if (isDark) TelegramIncomingBubbleDark else TelegramIncomingBubbleLight)
    }

    val textColor = when {
        isOwnMessage && isDark -> Color.White
        isOwnMessage && !isDark -> TelegramOutgoingTextLight
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subtleColor = when {
        isOwnMessage && isDark -> TelegramOutgoingSubtleDark
        isOwnMessage && !isDark -> TelegramOutgoingSubtleLight
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val tickColor = when {
        !isOwnMessage -> subtleColor
        isDark -> TelegramDarkBlue
        else -> TelegramTickGreen
    }
    val accentBarColor = when {
        isOwnMessage && isDark -> TelegramDarkBlue
        isOwnMessage && !isDark -> TelegramTickGreen
        else -> TelegramBlue
    }

    val groupedReactions = remember(message.reactions) {
        message.reactions.values.groupingBy { it }.eachCount().entries.toList()
    }
    val myReaction = message.reactions[currentUserId]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = if (isFirstInCluster) 6.dp else 2.dp,
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
                    size = 32.dp
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 312.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isDark) 0.dp else 1.5.dp,
                        shape = bubbleShape,
                        clip = false
                    )
                    .clip(bubbleShape)
                    .background(bubbleBrush)
                    .combinedClickable(
                        onClick = {
                            haptics.lightTap()
                            onLongPressOrTapActions()
                        },
                        onLongClick = {
                            haptics.impactMedium()
                            onLongPressOrTapActions()
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (!isOwnMessage && isGroup && isFirstInCluster) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            color = TelegramBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    if (message.isForwarded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Forward,
                                contentDescription = null,
                                tint = accentBarColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Forwarded message",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentBarColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!message.replyToPreview.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .height(IntrinsicSize.Min)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentBarColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(accentBarColor)
                            )
                            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
                                Text(
                                    text = message.replyToSenderName ?: "Reply",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentBarColor
                                )
                                Text(
                                    text = message.replyToPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.85f),
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
                                    .height(204.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    when {
                        message.type == Message.TYPE_AUDIO -> {
                            VoiceMessageBubbleContent(
                                message = message,
                                isOwnMessage = isOwnMessage,
                                accentColor = accentBarColor,
                                textColor = textColor,
                                subtleColor = subtleColor,
                                playbackState = voicePlaybackState,
                                onTogglePlay = {
                                    haptics.lightTap()
                                    onToggleVoicePlayback?.invoke(message)
                                }
                            )
                        }
                        message.type == Message.TYPE_POLL -> {
                            PollBubbleContent(
                                message = message,
                                currentUserId = currentUserId,
                                accentColor = accentBarColor,
                                textColor = textColor,
                                subtleColor = subtleColor,
                                onVotePoll = onVotePoll
                            )
                        }
                        message.text.isNotBlank() -> {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (message.isEdited) {
                            Text(
                                text = "edited  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtleColor
                            )
                        }
                        Text(
                            text = formatMessageClock(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = subtleColor
                        )
                        if (isOwnMessage) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Delivered",
                                tint = tickColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (groupedReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedReactions.forEach { (emoji, count) ->
                        val isMine = myReaction == emoji
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isMine -> TelegramBlue
                                isOwnMessage && !isDark -> Color(0xFFD9F3C4)
                                isDark -> Color(0xFF213143)
                                else -> Color.White.copy(alpha = 0.94f)
                            },
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.selectionTick()
                                    onToggleReaction(emoji)
                                }
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
                                    color = if (isMine) Color.White else textColor
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
    accentColor: Color,
    textColor: Color,
    subtleColor: Color,
    onVotePoll: (Int) -> Unit
) {
    val haptics = rememberIosHaptics()
    val totalVotes = message.pollVotes.size
    val myVote = message.pollVotes[currentUserId]?.toIntOrNull()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.Poll,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Anonymous Poll",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = message.pollQuestion ?: message.text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = textColor
    )

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        message.pollOptions.forEachIndexed { index, option ->
            val votesForOption = message.pollVotes.values.count { it == index.toString() }
            val targetFraction = if (totalVotes > 0) votesForOption.toFloat() / totalVotes.toFloat() else 0f
            val animatedFraction by animateFloatAsState(targetValue = targetFraction, label = "poll_progress")
            val isSelected = myVote == index

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = if (isSelected) 0.20f else 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.selectionTick()
                        onVotePoll(index)
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
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
                                    tint = accentColor,
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
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(7.dp))
    Text(
        text = "$totalVotes vote${if (totalVotes == 1) "" else "s"} · Tap an option to vote",
        style = MaterialTheme.typography.labelSmall,
        color = subtleColor
    )
}

@Composable
private fun VoiceMessageBubbleContent(
    message: Message,
    isOwnMessage: Boolean,
    accentColor: Color,
    textColor: Color,
    subtleColor: Color,
    playbackState: VoicePlaybackState,
    onTogglePlay: () -> Unit
) {
    val isThisMessage = playbackState.activeMessageId == message.id
    val isPlaying = isThisMessage && playbackState.isPlaying
    val targetProgress = if (isThisMessage) playbackState.progress else 0f
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "voice_progress")
    val displaySec = if (isThisMessage && playbackState.positionSec > 0) {
        playbackState.positionSec
    } else {
        message.audioDurationSec.coerceAtLeast(1)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(min = 200.dp, max = 264.dp)
            .padding(vertical = 2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = !message.audioBase64.isNullOrBlank(),
                    onClick = onTogglePlay
                )
                .testTag("play_voice_${message.id}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause voice message" else "Play voice message",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voice message",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = formatDurationSec(displaySec),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtleColor
                )
            }
        }
    }
}

@Composable
fun MessageActionsDialog(
    message: Message,
    isOwnMessage: Boolean,
    isPinnedInChat: Boolean = false,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null
) {
    val haptics = rememberIosHaptics()
    val hasCopyableText = message.text.isNotBlank() && message.type != Message.TYPE_AUDIO
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
                    text = "React",
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
                                    haptics.selectionTick()
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
                        haptics.lightTap()
                        onReply()
                        onDismiss()
                    }
                )

                if (hasCopyableText && onCopy != null) {
                    ActionRow(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Copy",
                        onClick = {
                            haptics.lightTap()
                            onCopy()
                            onDismiss()
                        }
                    )
                }

                if (onForward != null) {
                    ActionRow(
                        icon = Icons.AutoMirrored.Rounded.Forward,
                        label = "Forward",
                        onClick = {
                            haptics.lightTap()
                            onForward()
                            onDismiss()
                        }
                    )
                }

                if (onTogglePin != null) {
                    ActionRow(
                        icon = Icons.Rounded.PushPin,
                        label = if (isPinnedInChat) "Unpin" else "Pin",
                        onClick = {
                            haptics.selectionTick()
                            onTogglePin()
                            onDismiss()
                        }
                    )
                }

                if (isOwnMessage && message.type == Message.TYPE_TEXT) {
                    ActionRow(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = {
                            haptics.lightTap()
                            onEdit()
                            onDismiss()
                        }
                    )
                }

                if (isOwnMessage) {
                    ActionRow(
                        icon = Icons.Rounded.Delete,
                        label = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            haptics.impactMedium()
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
