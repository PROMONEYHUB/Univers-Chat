package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.formatDurationSec
import com.example.ui.components.IosSearchField
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramIncomingBubbleDark
import com.example.ui.theme.TelegramIncomingBubbleLight

private val QuickRepliesList = listOf(
    "👋 Hey!",
    "Sounds good 👍",
    "On my way!",
    "🔥 Awesome",
    "Let's vote!"
)

@Composable
fun ChatRoomSearchBar(
    query: String,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            IosSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Search messages…",
                inputTestTag = "chat_room_search_input",
                matchCount = matchCount,
                showCancelWhenActive = true,
                onCancelClick = onClose,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun PinnedMessageBanner(
    senderName: String,
    preview: String,
    onJumpToPinned: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptics.lightTap()
                onJumpToPinned()
            }
            .testTag("pinned_message_banner")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(TelegramBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = TelegramBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (senderName.isNotBlank()) "Pinned Message · $senderName" else "Pinned Message",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TelegramBlue
                    )
                    Text(
                        text = preview.ifBlank { "Media attachment" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onUnpin()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("unpin_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Unpin message",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun IosQuickInsertTray(
    visible: Boolean,
    onPickPhoto: () -> Unit,
    onCreatePoll: () -> Unit,
    onRecordVoice: () -> Unit = {},
    onInsertQuickReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item(key = "tray_photo") {
                        Surface(
                            shape = CircleShape,
                            color = TelegramBlue.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.lightTap()
                                    onPickPhoto()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gallery",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TelegramBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item(key = "tray_poll") {
                        Surface(
                            shape = CircleShape,
                            color = IosIndigo.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.lightTap()
                                    onCreatePoll()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Poll,
                                    contentDescription = null,
                                    tint = IosIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Poll",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IosIndigo,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item(key = "tray_voice") {
                        Surface(
                            shape = CircleShape,
                            color = IosRed.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.lightTap()
                                    onRecordVoice()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = IosRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Voice",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IosRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    items(QuickRepliesList) { snippet ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptics.selectionTick()
                                    onInsertQuickReply(snippet)
                                }
                        ) {
                            Text(
                                text = snippet,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width flat Telegram bottom composer bar with Emoji/Quick-Tray toggle,
 * borderless Message input, Paperclip attachment, Poll shortcut, and Send/Mic toggle.
 */
@Composable
fun IosMessageComposerBar(
    draftText: String,
    onDraftChange: (String) -> Unit,
    canInteract: Boolean,
    isSending: Boolean,
    isTrayOpen: Boolean,
    onToggleTray: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenPoll: () -> Unit,
    onSend: () -> Unit,
    isRecordingAudio: Boolean = false,
    recordingSeconds: Int = 0,
    onStartVoiceRecord: () -> Unit = {},
    onCancelVoiceRecord: () -> Unit = {},
    onSendVoiceRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val hasDraftText = draftText.trim().isNotEmpty()
    val canSendText = canInteract && hasDraftText && !isSending
    val sendScale by animateFloatAsState(
        targetValue = if (canSendText || isRecordingAudio) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tg_send_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
            if (isRecordingAudio) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.lightTap()
                            onCancelVoiceRecord()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("cancel_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel voice recording",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(IosRed.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FiberManualRecord,
                            contentDescription = null,
                            tint = IosRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recording voice… ${formatDurationSec(recordingSeconds)} / 0:30",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IosRed
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledIconButton(
                        onClick = {
                            haptics.success()
                            onSendVoiceRecord()
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = TelegramBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send voice message"
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            haptics.lightTap()
                            onToggleTray()
                        },
                        enabled = canInteract && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_quick_tray_button")
                    ) {
                        Icon(
                            imageVector = if (isTrayOpen) Icons.Rounded.Close else Icons.Rounded.SentimentSatisfiedAlt,
                            contentDescription = if (isTrayOpen) "Close quick panel" else "Quick replies & options",
                            tint = if (isTrayOpen) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 128.dp)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (draftText.isEmpty()) {
                            Text(
                                text = "Message",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = draftText,
                            onValueChange = { onDraftChange(it.take(2000)) },
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(TelegramBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("message_composer_input")
                        )
                    }

                    if (!hasDraftText && !isSending) {
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                onOpenPoll()
                            },
                            enabled = canInteract,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("create_poll_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Poll,
                                contentDescription = "Create poll",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                onPickPhoto()
                            },
                            enabled = canInteract,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("attach_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AttachFile,
                                contentDescription = "Attach photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }

                    if (hasDraftText || isSending) {
                        IconButton(
                            onClick = onSend,
                            enabled = canSendText,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = sendScale
                                    scaleY = sendScale
                                }
                                .testTag("send_message_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = TelegramBlue,
                                    strokeWidth = 2.4.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send message",
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                haptics.impactMedium()
                                onStartVoiceRecord()
                            },
                            enabled = canInteract,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("record_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Record voice message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JumpToLatestButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                haptics.lightTap()
                onClick()
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(46.dp)
                .border(
                    width = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .testTag("jump_to_latest_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Jump to latest messages",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatRoomEmptyState(
    isSearchFiltered: Boolean,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1A2634).copy(alpha = 0.86f) else Color.Black.copy(alpha = 0.28f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSearchFiltered) Icons.Rounded.Search else Icons.Rounded.WavingHand,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isSearchFiltered) "No messages found" else "No messages here yet…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSearchFiltered) {
                        "Nothing matched \"$searchQuery\" in this chat."
                    } else {
                        "Send a message, attach a photo, record a voice note, or create a poll below."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DayDividerPill(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.26f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TypingWaveDots(
    dotColor: Color,
    dotSize: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "typing_wave_dots")
    val dotOffsets = listOf(0, 140, 280).map { delayMs ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 420,
                    delayMillis = delayMs,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "typing_dot_$delayMs"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotOffsets.forEach { phase ->
            val factor = phase.value
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        translationY = -factor * (dotSize.toPx() * 0.55f)
                        alpha = 0.42f + (factor * 0.58f)
                    }
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble(
    typerNames: List<String>,
    typerPhotoUrl: String?,
    isGroup: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    val label = when {
        typerNames.isEmpty() -> "typing"
        !isGroup -> "typing"
        typerNames.size == 1 -> "${typerNames[0]} is typing"
        typerNames.size == 2 -> "${typerNames[0]} and ${typerNames[1]} are typing"
        else -> "${typerNames[0]}, ${typerNames[1]} +${typerNames.size - 2} typing"
    }
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 4.dp,
        bottomEnd = 18.dp
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .testTag("typing_indicator_bubble"),
        verticalAlignment = Alignment.Bottom
    ) {
        UserAvatar(
            name = typerNames.firstOrNull().orEmpty().ifBlank { "Member" },
            photoUrl = typerPhotoUrl,
            size = 28.dp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (isDark) 0.dp else 1.5.dp,
                    shape = bubbleShape,
                    clip = false
                )
                .clip(bubbleShape)
                .background(if (isDark) TelegramIncomingBubbleDark else TelegramIncomingBubbleLight)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypingWaveDots(
                    dotColor = TelegramBlue,
                    dotSize = 6.dp
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
