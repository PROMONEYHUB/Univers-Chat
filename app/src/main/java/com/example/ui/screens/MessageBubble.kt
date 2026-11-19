package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.audio.VoicePlaybackState
import com.example.audio.formatDurationSec
import com.example.data.model.Message
import com.example.data.model.ReactionSummary
import com.example.data.model.UserProfile
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

val QuickReactionEmojis = Message.DEFAULT_TAPBACK_ORDER
val IdleVoicePlaybackState = VoicePlaybackState()

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    isRead: Boolean = false,
    isFirstInCluster: Boolean = true,
    isLastInCluster: Boolean = true,
    onLongPressOrTapActions: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onInspectReactions: (() -> Unit)? = null,
    onVotePoll: (Int) -> Unit,
    onOpenImage: ((Bitmap) -> Unit)? = null,
    onOpenImageUrl: ((String) -> Unit)? = null,
    voicePlaybackState: VoicePlaybackState = IdleVoicePlaybackState,
    onToggleVoicePlayback: ((Message) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    val haptics = rememberIosHaptics()

    val bubbleShape = remember(isOwnMessage, isFirstInCluster, isLastInCluster) {
        if (isOwnMessage) {
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
        isDark -> Color(0xFFBAE6FD)
        else -> Color.White.copy(alpha = 0.92f)
    }
    val accentBarColor = when {
        isOwnMessage -> Color.White.copy(alpha = 0.85f)
        else -> TelegramBlue
    }

    val reactionSummaries = remember(message.reactions, currentUserId) {
        message.reactionSummaries(currentUserId = currentUserId)
    }

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
                        elevation = if (isOwnMessage) 3.dp else if (isDark) 1.dp else 1.5.dp,
                        shape = bubbleShape,
                        ambientColor = if (isOwnMessage) TelegramBlue else Color.Black,
                        spotColor = if (isOwnMessage) TelegramBlue else Color.Black,
                        clip = false
                    )
                    .clip(bubbleShape)
                    .background(bubbleBrush)
                    .border(
                        width = 0.75.dp,
                        color = when {
                            isOwnMessage -> Color.White.copy(alpha = 0.18f)
                            isDark -> Color.White.copy(alpha = 0.08f)
                            else -> Color(0xFFE2EAF4)
                        },
                        shape = bubbleShape
                    )
                    .combinedClickable(
                        onClick = {
                            haptics.lightTap()
                            onLongPressOrTapActions()
                        },
                        onDoubleClick = {
                            haptics.selectionTick()
                            onToggleReaction("❤️")
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
                        val imageBitmap = remember(message.imageBase64) {
                            ImageUtils.decodeBase64ToImageBitmap(message.imageBase64)
                        }
                        if (imageBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(204.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = {
                                            val bmp = ImageUtils.decodeBase64ToBitmap(message.imageBase64)
                                            if (onOpenImage != null && bmp != null) onOpenImage(bmp)
                                            else onLongPressOrTapActions()
                                        },
                                        onLongClick = onLongPressOrTapActions
                                    )
                            ) {
                                Image(
                                    bitmap = imageBitmap,
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

                    val remoteAudioUrl = remember(message.audioUrl, message.text, message.type) {
                        resolveVoiceAudioUrl(message)
                    }
                    val remoteImageUrl = remember(message.text, message.type, message.imageBase64, remoteAudioUrl) {
                        if (message.imageBase64.isNullOrBlank() && remoteAudioUrl == null && message.type != Message.TYPE_AUDIO) {
                            extractRemoteImageUrl(message.text)
                        } else {
                            null
                        }
                    }
                    if (remoteImageUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(204.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.08f))
                                .combinedClickable(
                                    onClick = {
                                        if (onOpenImageUrl != null) onOpenImageUrl(remoteImageUrl)
                                        else onLongPressOrTapActions()
                                    },
                                    onLongClick = onLongPressOrTapActions
                                )
                                .testTag("message_remote_image")
                        ) {
                            AsyncImage(
                                model = remoteImageUrl,
                                contentDescription = "Shared photo",
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
                    }

                    when {
                        message.type == Message.TYPE_AUDIO || remoteAudioUrl != null -> {
                            VoiceMessageBubbleContent(
                                message = message,
                                resolvedAudioUrl = remoteAudioUrl,
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
                        message.text.isNotBlank() && remoteImageUrl == null -> {
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
                        if (message.isEncrypted) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "End-to-end encrypted",
                                tint = subtleColor,
                                modifier = Modifier
                                    .size(11.dp)
                                    .testTag("message_e2ee_badge")
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        if (message.isEdited) {
                            Text(
                                text = "edited  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtleColor
                            )
                        }
                        val formattedClock = remember(message.createdAt) {
                            formatMessageClock(message.createdAt)
                        }
                        Text(
                            text = formattedClock,
                            style = MaterialTheme.typography.labelSmall,
                            color = subtleColor
                        )
                        if (isOwnMessage) {
                            if (isRead) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = "Read",
                                    tint = tickColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Read",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tickColor,
                                    modifier = Modifier.testTag("message_read_status")
                                )
                            } else {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Done,
                                    contentDescription = "Sent",
                                    tint = subtleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (reactionSummaries.isNotEmpty()) {
                TapbackReactionBubblesRow(
                    messageId = message.id,
                    summaries = reactionSummaries,
                    isOwnMessage = isOwnMessage,
                    isDark = isDark,
                    onToggleReaction = { emoji ->
                        haptics.selectionTick()
                        onToggleReaction(emoji)
                    },
                    onLongPressInspect = {
                        haptics.impactMedium()
                        onInspectReactions?.invoke() ?: onLongPressOrTapActions()
                    },
                    modifier = Modifier
                        .offset(y = (-7).dp)
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TapbackReactionBubblesRow(
    messageId: String,
    summaries: List<ReactionSummary>,
    isOwnMessage: Boolean,
    isDark: Boolean,
    onToggleReaction: (String) -> Unit,
    onLongPressInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp, if (isOwnMessage) Alignment.End else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag("reaction_bubbles_row_$messageId")
    ) {
        summaries.forEach { summary ->
            TapbackEmojiPill(
                messageId = messageId,
                summary = summary,
                isDark = isDark,
                onClick = { onToggleReaction(summary.emoji) },
                onLongClick = onLongPressInspect
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TapbackEmojiPill(
    messageId: String,
    summary: ReactionSummary,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val popScale = remember(summary.emoji) { Animatable(0.76f) }
    LaunchedEffect(summary.count, summary.reactedByCurrentUser) {
        popScale.snapTo(0.84f)
        popScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val containerColor = when {
        summary.reactedByCurrentUser -> TelegramBlue
        isDark -> Color(0xFF1E293B)
        else -> Color.White
    }
    val borderColor = when {
        summary.reactedByCurrentUser -> Color.White.copy(alpha = if (isDark) 0.35f else 0.9f)
        isDark -> Color.White.copy(alpha = 0.14f)
        else -> Color(0xFFDCE6F4)
    }
    val countColor = when {
        summary.reactedByCurrentUser -> Color.White
        isDark -> Color(0xFFE2E8F0)
        else -> Color(0xFF1E293B)
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        shadowElevation = if (summary.reactedByCurrentUser) 3.dp else 2.dp,
        modifier = Modifier
            .graphicsLayer {
                scaleX = popScale.value
                scaleY = popScale.value
            }
            .clip(CircleShape)
            .border(width = 1.25.dp, color = borderColor, shape = CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${summary.emoji} tapback, ${summary.count} ${if (summary.count == 1) "reaction" else "reactions"}"
            }
            .testTag("reaction_bubble_${messageId}_${summary.emoji}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = summary.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedContent(
                targetState = summary.count,
                transitionSpec = {
                    (slideInVertically { -it / 2 } + fadeIn(tween(150)))
                        .togetherWith(slideOutVertically { it / 2 } + fadeOut(tween(120)))
                },
                label = "reaction_count_${summary.emoji}"
            ) { count ->
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = countColor
                )
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
    resolvedAudioUrl: String?,
    isOwnMessage: Boolean,
    accentColor: Color,
    textColor: Color,
    subtleColor: Color,
    playbackState: VoicePlaybackState,
    onTogglePlay: () -> Unit
) {
    val isThisMessage = playbackState.activeMessageId == message.id
    val isPlaying = isThisMessage && playbackState.isPlaying
    val isBuffering = isThisMessage && playbackState.isBuffering
    val targetProgress = if (isThisMessage) playbackState.progress else 0f
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "voice_progress")
    val hasPlayableAudio = !resolvedAudioUrl.isNullOrBlank() || !message.audioBase64.isNullOrBlank()
    val totalSec = when {
        isThisMessage && playbackState.durationSec > 0 -> playbackState.durationSec
        message.audioDurationSec > 0 -> message.audioDurationSec
        else -> 0
    }
    val displaySec = if (isThisMessage && playbackState.positionSec > 0) {
        playbackState.positionSec
    } else {
        totalSec
    }
    val barHeights = remember(message.id) {
        val seed = message.id.hashCode()
        List(24) { idx ->
            val v = ((seed ushr (idx % 16)) xor (idx * 73 + 19)) and 0xFF
            0.26f + (v % 74) / 100f
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(min = 216.dp, max = 276.dp)
            .padding(vertical = 2.dp)
            .testTag("voice_bubble_${message.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = hasPlayableAudio,
                    onClick = onTogglePlay
                )
                .testTag("play_voice_${message.id}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause voice message" else "Play voice message",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                val activeCount = (animatedProgress * barHeights.size).toInt()
                barHeights.forEachIndexed { idx, fraction ->
                    val filled = isThisMessage && (idx < activeCount || (isPlaying && idx == activeCount))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceIn(0.22f, 1f))
                            .clip(CircleShape)
                            .background(
                                if (filled) accentColor else accentColor.copy(alpha = 0.28f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBuffering) "Loading audio…" else "Voice message",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (displaySec > 0) {
                    Text(
                        text = formatDurationSec(displaySec),
                        style = MaterialTheme.typography.labelSmall,
                        color = subtleColor
                    )
                }
            }
        }
    }
}

@Composable
fun MessageActionsDialog(
    message: Message,
    currentUserId: String,
    isOwnMessage: Boolean,
    isPinnedInChat: Boolean = false,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onInspectReactions: (() -> Unit)? = null,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null
) {
    val haptics = rememberIosHaptics()
    val audioLink = resolveVoiceAudioUrl(message)
    val hasCopyableText = (message.text.isNotBlank() && message.type != Message.TYPE_AUDIO) || audioLink != null
    val copyActionLabel = if (audioLink != null) "Copy audio link" else "Copy"
    val myCurrentReaction = message.reactions[currentUserId]
    val totalReactions = message.reactions.size

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Floating Tapback Pill Bar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.9.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .testTag("tapback_picker_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickReactionEmojis.forEach { emoji ->
                        val isSelected = myCurrentReaction == emoji
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "tapback_option_scale_$emoji"
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                TelegramBlue
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .clickable {
                                    haptics.selectionTick()
                                    onReact(emoji)
                                    onDismiss()
                                }
                                .semantics {
                                    contentDescription = if (isSelected) {
                                        "Remove $emoji tapback"
                                    } else {
                                        "React with $emoji"
                                    }
                                }
                                .testTag("tapback_option_$emoji")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            // Message Actions Card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraLarge
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (totalReactions > 0 && onInspectReactions != null) {
                        ActionRow(
                            icon = Icons.Rounded.EmojiEmotions,
                            label = "Reactions ($totalReactions)",
                            tint = TelegramBlue,
                            onClick = {
                                haptics.lightTap()
                                onDismiss()
                                onInspectReactions()
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

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
                            label = copyActionLabel,
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
}

@Composable
fun MessageReactionsDetailDialog(
    message: Message,
    currentUserId: String,
    currentUserProfile: UserProfile?,
    peopleMap: Map<String, UserProfile>,
    participantNames: Map<String, String>,
    participantPhotos: Map<String, String>,
    onToggleReaction: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberIosHaptics()
    val summaries = remember(message.reactions, currentUserId) {
        message.reactionSummaries(currentUserId = currentUserId)
    }
    var selectedEmojiFilter by remember(message.id) { mutableStateOf<String?>(null) }

    val filteredEntries = remember(summaries, selectedEmojiFilter) {
        val activeSummaries = if (selectedEmojiFilter == null) {
            summaries
        } else {
            summaries.filter { it.emoji == selectedEmojiFilter }
        }
        activeSummaries.flatMap { summary ->
            summary.userIds.map { uid -> uid to summary.emoji }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.extraLarge
                )
                .testTag("reactions_detail_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Tapback Reactions (${message.reactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Per-emoji count summary filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allSelected = selectedEmojiFilter == null
                    Surface(
                        shape = CircleShape,
                        color = if (allSelected) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                haptics.selectionTick()
                                selectedEmojiFilter = null
                            }
                    ) {
                        Text(
                            text = "All ${message.reactions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (allSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    summaries.forEach { summary ->
                        val selected = selectedEmojiFilter == summary.emoji
                        Surface(
                            shape = CircleShape,
                            color = if (selected) TelegramBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.selectionTick()
                                    selectedEmojiFilter = if (selected) null else summary.emoji
                                }
                        ) {
                            Text(
                                text = "${summary.emoji} ${summary.count}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredEntries.isEmpty()) {
                    Text(
                        text = "No reactions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filteredEntries.forEach { (uid, emoji) ->
                            val isMe = uid == currentUserId
                            val profile = if (isMe) currentUserProfile else peopleMap[uid]
                            val name = when {
                                isMe -> "You"
                                !profile?.displayName.isNullOrBlank() -> profile!!.displayName
                                !participantNames[uid].isNullOrBlank() -> participantNames[uid]!!
                                else -> "Member"
                            }
                            val photo = profile?.photoUrl?.takeIf { it.isNotBlank() }
                                ?: participantPhotos[uid]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isMe) {
                                            Modifier.clickable {
                                                haptics.selectionTick()
                                                onToggleReaction(emoji)
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = name,
                                    photoUrl = photo,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isMe) {
                                        Text(
                                            text = "Tap to remove",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
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

internal fun resolveVoiceAudioUrl(message: Message): String? {
    val explicit = message.audioUrl?.trim()?.takeIf {
        it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
    }
    if (explicit != null) return explicit
    return extractRemoteAudioUrl(message.text)
}

internal fun extractRemoteAudioUrl(rawText: String): String? {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty() || trimmed.contains(' ') || trimmed.contains('\n')) return null
    val lower = trimmed.lowercase()
    if (!lower.startsWith("https://") && !lower.startsWith("http://")) return null
    val pathWithoutQuery = lower.substringBefore('?')
    val hasAudioExtension = pathWithoutQuery.endsWith(".m4a") ||
        pathWithoutQuery.endsWith(".aac") ||
        pathWithoutQuery.endsWith(".mp3") ||
        pathWithoutQuery.endsWith(".ogg") ||
        pathWithoutQuery.endsWith(".opus") ||
        pathWithoutQuery.endsWith(".wav") ||
        pathWithoutQuery.endsWith(".3gp")
    val isVoiceStoragePath = (lower.contains("firebasestorage.googleapis.com") ||
        lower.contains(".firebasestorage.app")) &&
        (lower.contains("voice_messages") || hasAudioExtension)
    return if (hasAudioExtension || isVoiceStoragePath) trimmed else null
}

private fun extractRemoteImageUrl(rawText: String): String? {
    if (extractRemoteAudioUrl(rawText) != null) return null
    val trimmed = rawText.trim()
    if (trimmed.isEmpty() || trimmed.contains(' ') || trimmed.contains('\n')) return null
    val lower = trimmed.lowercase()
    if (!lower.startsWith("https://") && !lower.startsWith("http://")) return null
    val isFirebaseStorage = lower.contains("firebasestorage.googleapis.com") ||
        lower.contains(".firebasestorage.app")
    val pathWithoutQuery = lower.substringBefore('?')
    val hasImageExtension = pathWithoutQuery.endsWith(".jpg") ||
        pathWithoutQuery.endsWith(".jpeg") ||
        pathWithoutQuery.endsWith(".png") ||
        pathWithoutQuery.endsWith(".webp") ||
        pathWithoutQuery.endsWith(".gif")
    return if (isFirebaseStorage || hasImageExtension) trimmed else null
}

