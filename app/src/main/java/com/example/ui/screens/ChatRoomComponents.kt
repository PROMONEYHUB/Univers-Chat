package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.VoiceNoteRecorder
import com.example.audio.formatDurationSec
import com.example.ui.components.IosSearchField
import com.example.ui.components.UniversMotion
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.rememberIosHaptics
import com.example.ui.viewmodel.VoiceUploadState
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.IosIndigo
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent
import com.example.ui.theme.TelegramBlue



@Composable
fun ChatRoomSearchBar(
    query: String,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0B1018),
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
            HorizontalDivider(thickness = 0.6.dp, color = Color.White.copy(alpha = 0.06f))
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
    val accentPrimary = Color(0xFF4B8BF5)
    val pillShape = RoundedCornerShape(20.dp)

    Surface(
        shape = pillShape,
        color = Color(0xFF151923),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(pillShape)
            .border(
                width = 0.7.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = pillShape
            )
            .clickable {
                haptics.lightTap()
                onJumpToPinned()
            }
            .testTag("pinned_message_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(start = 12.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accentPrimary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = null,
                tint = accentPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (senderName.isNotBlank()) "Pinned · $senderName" else "Pinned Message",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentPrimary
                )
                Text(
                    text = preview.ifBlank { "Media attachment" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9EA7B8),
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
                    tint = Color(0xFF8C95A6),
                    modifier = Modifier.size(18.dp)
                )
            }
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
    val iconColor = Color(0xFFD9DFEB)
    val trayShape = RoundedCornerShape(24.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(170, easing = UniversMotion.FluidOutEasing)) +
            expandVertically(tween(220, easing = UniversMotion.FluidOutEasing)),
        exit = fadeOut(tween(130)) +
            shrinkVertically(tween(180, easing = UniversMotion.FluidOutEasing)),
        modifier = modifier
    ) {
        Surface(
            shape = trayShape,
            color = Color(0xFF131720),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(
                    width = 0.7.dp,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = trayShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2430),
                    modifier = Modifier
                        .bouncyPressScale(0.95f)
                        .clip(CircleShape)
                        .clickable {
                            haptics.lightTap()
                            onPickPhoto()
                        }
                        .testTag("attach_photo_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Attach photo",
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Photo",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE9EDF5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2430),
                    modifier = Modifier
                        .bouncyPressScale(0.95f)
                        .clip(CircleShape)
                        .clickable {
                            haptics.lightTap()
                            onRecordVoice()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Voice",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE9EDF5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2430),
                    modifier = Modifier
                        .bouncyPressScale(0.95f)
                        .clip(CircleShape)
                        .clickable {
                            haptics.lightTap()
                            onCreatePoll()
                        }
                        .testTag("create_poll_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Poll,
                            contentDescription = "Create poll",
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Poll",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE9EDF5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Univers Glass Capsule Composer Dock:
 * [+ New]  ( Type Message.. )  (Mic)  (Send)
 * Morphs seamlessly into a live 120fps Canvas waveform bar during voice recording.
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
    recordingAmplitude: Float = 0.2f,
    onStartVoiceRecord: () -> Unit = {},
    onCancelVoiceRecord: () -> Unit = {},
    onSendVoiceRecord: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val plusBlue = Color(0xFF5D7BE8)
    val sendActiveColor = Color(0xFF3B74C4)
    val hasDraftText = draftText.trim().isNotEmpty()
    val canSendText = canInteract && hasDraftText && !isSending

    val trayIconRotationState = animateFloatAsState(
        targetValue = if (isTrayOpen) 45f else 0f,
        animationSpec = UniversMotion.SnappyFloatSpring,
        label = "new_plus_rotation"
    )
    val sendScaleState = animateFloatAsState(
        targetValue = if (canSendText || isRecordingAudio) 1.02f else 0.98f,
        animationSpec = UniversMotion.SnappyFloatSpring,
        label = "univers_send_scale"
    )
    val smoothAmpState = animateFloatAsState(
        targetValue = recordingAmplitude.coerceIn(0.1f, 1f),
        animationSpec = tween(durationMillis = 90, easing = UniversMotion.FluidOutEasing),
        label = "voice_rec_amp"
    )
    val recFactors = remember {
        floatArrayOf(0.42f, 0.72f, 0.56f, 0.94f, 0.65f, 1.0f, 0.78f, 0.48f, 0.86f, 0.62f, 0.90f, 0.50f)
    }

    val dockShape = RoundedCornerShape(30.dp)
    val dockContainerColor = Color(0xFF131720)
    val innerFieldColor = Color(0xFF0E121A)
    val circleButtonColor = Color(0xFF1F2430)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dockShape)
                .background(dockContainerColor)
                .border(width = 0.65.dp, color = Color.White.copy(alpha = 0.05f), shape = dockShape)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            AnimatedContent(
                targetState = isRecordingAudio,
                transitionSpec = {
                    (fadeIn(tween(180, easing = UniversMotion.FluidOutEasing)) +
                        scaleIn(initialScale = 0.96f, animationSpec = UniversMotion.SnappyFloatSpring)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.97f, animationSpec = tween(120)))
                },
                label = "composer_recording_morph"
            ) { recording ->
                if (recording) {
                    val pulseTransition = rememberInfiniteTransition(label = "voice_rec_dot")
                    val dotAlphaState = pulseTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(550, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "voice_dot_alpha"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                tint = IosRed
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(IosRed.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FiberManualRecord,
                                contentDescription = null,
                                tint = IosRed,
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer { alpha = dotAlphaState.value }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatDurationSec(recordingSeconds)} / ${formatDurationSec(VoiceNoteRecorder.MAX_DURATION_SEC)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = IosRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                            ) {
                                val amp = smoothAmpState.value
                                val count = recFactors.size
                                val step = size.width / count
                                val barWidth = (step * 0.52f).coerceIn(2.dp.toPx(), 4.dp.toPx())
                                val centerY = size.height / 2f
                                val barColor = IosRed.copy(alpha = 0.86f)
                                for (idx in 0 until count) {
                                    val wave = ((amp * recFactors[idx]) + if ((recordingSeconds + idx) % 2 == 0) 0.12f else 0.04f)
                                        .coerceIn(0.18f, 1f)
                                    val halfH = (size.height * wave * 0.5f).coerceAtLeast(barWidth * 0.5f)
                                    val x = idx * step + step * 0.5f
                                    drawLine(
                                        color = barColor,
                                        start = Offset(x, centerY - halfH),
                                        end = Offset(x, centerY + halfH),
                                        strokeWidth = barWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .bouncyPressScale(0.90f)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.success()
                                    onSendVoiceRecord()
                                }
                                .testTag("send_voice_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(sendActiveColor)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send voice message",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left "+ New" action pill matching reference
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .bouncyPressScale(0.93f)
                                .heightIn(min = 48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canInteract && !isSending) {
                                    haptics.lightTap()
                                    onToggleTray()
                                }
                                .padding(horizontal = 10.dp)
                                .testTag("toggle_quick_tray_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = if (isTrayOpen) "Close quick panel" else "New attachment or quick reply",
                                tint = plusBlue,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = trayIconRotationState.value }
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE9EDF5)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Center recessed matte dark input pill ("Type Message..")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp, max = 112.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(innerFieldColor)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (draftText.isEmpty()) {
                                Text(
                                    text = "Type Message..",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6B7384),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = draftText,
                                onValueChange = { onDraftChange(it.take(2000)) },
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFF2F4F8)
                                ),
                                cursorBrush = SolidColor(plusBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("message_composer_input")
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Circular Matte Slate Mic Button (outlined mic icon)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .bouncyPressScale(0.90f)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canInteract && !isSending) {
                                    haptics.impactMedium()
                                    onStartVoiceRecord()
                                }
                                .testTag("record_voice_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(circleButtonColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = "Record voice message",
                                    tint = Color(0xFF8C95A6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Circular Matte Slate / Subtle Cobalt Send Button (zero neon shadow)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .bouncyPressScale(0.90f)
                                .size(48.dp)
                                .graphicsLayer {
                                    val s = sendScaleState.value
                                    scaleX = s
                                    scaleY = s
                                }
                                .clip(CircleShape)
                                .clickable(enabled = canSendText, onClick = onSend)
                                .testTag("send_message_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (canSendText) sendActiveColor else circleButtonColor)
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send message",
                                        tint = Color(0xFFF0F3F8),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
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
        enter = fadeIn(tween(160, easing = UniversMotion.FluidOutEasing)) +
            scaleIn(initialScale = 0.80f, animationSpec = UniversMotion.SnappyFloatSpring),
        exit = fadeOut(tween(130)) +
            scaleOut(targetScale = 0.82f, animationSpec = tween(130)),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                haptics.lightTap()
                onClick()
            },
            shape = CircleShape,
            color = Color(0xFF1A1F2B),
            modifier = Modifier
                .bouncyPressScale(0.92f)
                .size(48.dp)
                .border(
                    width = 0.7.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                )
                .testTag("jump_to_latest_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Jump to latest messages",
                    tint = Color(0xFFE5EAF3)
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF151923)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSearchFiltered) Icons.Rounded.Search else Icons.Rounded.WavingHand,
                            contentDescription = null,
                            tint = Color(0xFFE6EAF2),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isSearchFiltered) "No messages found" else "No messages yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F4F8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSearchFiltered) {
                        "Nothing matched \"$searchQuery\" in this chat."
                    } else {
                        "Send a message, photo, or voice note below."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF838E9E),
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
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF151923).copy(alpha = 0.85f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E97A8),
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
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        val factor = phase.value
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
    val label = when {
        typerNames.isEmpty() -> "typing..."
        !isGroup -> "typing..."
        typerNames.size == 1 -> "${typerNames[0]} is typing..."
        typerNames.size == 2 -> "${typerNames[0]} and ${typerNames[1]} are typing..."
        else -> "${typerNames[0]}, ${typerNames[1]} +${typerNames.size - 2} typing..."
    }
    val bubbleShape = RoundedCornerShape(24.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
            }
            .testTag("typing_indicator_bubble"),
        verticalAlignment = Alignment.Bottom
    ) {
        UserAvatar(
            name = typerNames.firstOrNull().orEmpty().ifBlank { "Member" },
            photoUrl = typerPhotoUrl,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(Color(0xFF1F232C))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypingWaveDots(
                    dotColor = Color(0xFF6B8FE8),
                    dotSize = 5.5.dp
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9EA7B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VoiceUploadStatusStrip(
    state: VoiceUploadState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when (state) {
            is VoiceUploadState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_upload_status_strip")
    ) {
        Column {
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
            when (state) {
                is VoiceUploadState.Uploading -> {
                    val percent = (state.progress * 100f).toInt().coerceIn(0, 100)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = TelegramBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (percent > 0) {
                                    "Uploading voice (${formatDurationSec(state.durationSec)})… $percent%"
                                } else {
                                    "Uploading voice (${formatDurationSec(state.durationSec)})…"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (state.progress > 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress.coerceIn(0f, 1f) },
                                    color = TelegramBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                LinearProgressIndicator(
                                    color = TelegramBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("cancel_voice_upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel voice upload",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is VoiceUploadState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("retry_voice_upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Retry",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dismiss_voice_upload_error_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss upload error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                VoiceUploadState.Idle -> Unit
            }
        }
    }
}

