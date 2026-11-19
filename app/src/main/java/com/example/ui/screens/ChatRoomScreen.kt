package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.VoiceNotePlayer
import com.example.audio.VoiceNoteRecorder
import com.example.crypto.E2eeKeyManager
import com.example.crypto.SignalProtocolEngine
import com.example.data.model.CallTarget
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ImageUtils
import com.example.ui.components.TelegramChatWallpaper
import com.example.ui.components.UniversChatRoomSkeletonLoading
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDayHeader
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.isSameCalendarDay
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramSteelBlue
import com.example.ui.viewmodel.ChatRoomViewModel
import com.example.ui.viewmodel.VoiceUploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Immutable
private data class PreparedChatRoomRow(
    val key: String,
    val contentType: String,
    val originalMessage: Message,
    val displayMessage: Message,
    val isOwnMessage: Boolean,
    val isRead: Boolean,
    val showDayHeader: Boolean,
    val dayHeaderLabel: String,
    val isFirstInCluster: Boolean,
    val isLastInCluster: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: ChatRoomViewModel,
    currentUserProfile: UserProfile?,
    allPeople: List<UserProfile>,
    readReceiptsEnabled: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onStartCall: (target: CallTarget, isVideoCall: Boolean) -> Unit = { _, _ -> },
    allConversations: List<Conversation> = emptyList(),
    blockedUserIds: Set<String> = emptySet(),
    onTogglePinConversation: (Conversation) -> Unit = {},
    onToggleMuteConversation: (Conversation) -> Unit = {},
    onToggleBlockUser: (targetUserId: String, blocked: Boolean) -> Unit = { _, _ -> },
    onOpenUserProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptics = rememberIosHaptics()
    val scope = rememberCoroutineScope()
    val isDark = LocalIsTelegramDark.current
    val cosmicHeaderBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)

    val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val voiceUploadState by viewModel.voiceUploadState.collectAsStateWithLifecycle()

    var draftText by rememberSaveable(viewModel.conversationId) { mutableStateOf("") }
    var selectedMessageForActions by remember { mutableStateOf<Message?>(null) }
    var reactionDetailsMessageId by remember { mutableStateOf<String?>(null) }
    var messageToForward by remember { mutableStateOf<Message?>(null) }
    var lightboxState by remember { mutableStateOf<Triple<Bitmap, String, String>?>(null) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showDirectInfoDialog by remember { mutableStateOf(false) }
    var showE2eeSafetyDialog by remember { mutableStateOf(false) }
    var showSearchBar by rememberSaveable(viewModel.conversationId) { mutableStateOf(false) }
    var inChatSearchQuery by rememberSaveable(viewModel.conversationId) { mutableStateOf("") }
    var isTrayOpen by remember { mutableStateOf(false) }
    var pendingGroupCallVideo by remember { mutableStateOf<Boolean?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val voiceRecorder = remember(context) { VoiceNoteRecorder(context) }
    val voicePlayer = remember(context) { VoiceNotePlayer(context) }
    val voicePlaybackState by voicePlayer.state.collectAsStateWithLifecycle()
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingAmplitude by remember { mutableFloatStateOf(0.18f) }

    LaunchedEffect(voicePlaybackState.errorMessage) {
        val err = voicePlaybackState.errorMessage
        if (!err.isNullOrBlank()) {
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(viewModel.conversationId) {
        onDispose {
            viewModel.clearTypingStatus()
            voiceRecorder.cancel()
            voicePlayer.release()
        }
    }

    val conversation = (conversationState as? UiState.Success)?.data
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val peopleMap = remember(allPeople) { allPeople.associateBy { it.userId } }
    val otherId = conversation?.otherParticipantId(viewModel.currentUserId)
    val otherProfile = remember(otherId, allPeople) {
        otherId?.let { id -> allPeople.firstOrNull { it.userId == id } }
    }
    val isGroup = conversation?.type == Conversation.TYPE_GROUP
    val isSelfChat = !isGroup && (conversation?.participantIds?.distinct()?.size ?: 0) <= 1
    val peerE2eeKey = if (!isGroup) {
        (if (isSelfChat) currentUserProfile?.e2eeIdentityKey else otherProfile?.e2eeIdentityKey)
            ?.takeIf { it.isNotBlank() }
    } else {
        null
    }
    val isE2eeActive = !isGroup && !peerE2eeKey.isNullOrBlank()

    val decryptedMessages = remember(messages, peerE2eeKey, viewModel.currentUserId, conversation?.id) {
        val convId = conversation?.id ?: return@remember messages
        messages.map { msg ->
            if (!msg.isEncrypted) {
                msg
            } else {
                val plain = SignalProtocolEngine.decryptMessage(
                    context = context,
                    conversationId = convId,
                    currentUserId = viewModel.currentUserId,
                    senderId = msg.senderId,
                    ciphertextBase64 = msg.text,
                    ivBase64 = msg.encryptionIv,
                    ephemeralPublicKeyB64 = msg.ephemeralPublicKey,
                    senderIdentityKeyB64 = msg.senderIdentityKey,
                    peerPublicKeyB64 = peerE2eeKey
                )
                msg.copy(text = plain ?: "🔒 Encrypted message")
            }
        }
    }
    val photoMessages = remember(decryptedMessages) {
        decryptedMessages.filter { it.type == Message.TYPE_IMAGE && !it.imageBase64.isNullOrBlank() }
    }
    val filteredMessages = remember(decryptedMessages, showSearchBar, inChatSearchQuery) {
        if (!showSearchBar || inChatSearchQuery.isBlank()) decryptedMessages
        else decryptedMessages.filter { msg ->
            msg.text.contains(inChatSearchQuery, ignoreCase = true) ||
                msg.senderName.contains(inChatSearchQuery, ignoreCase = true) ||
                (msg.pollQuestion?.contains(inChatSearchQuery, ignoreCase = true) == true) ||
                msg.pollOptions.any { it.contains(inChatSearchQuery, ignoreCase = true) }
        }
    }

    val preparedRows = remember(
        filteredMessages,
        peopleMap,
        currentUserProfile,
        conversation?.lastReadAt,
        conversation?.participantIds,
        viewModel.currentUserId
    ) {
        filteredMessages.mapIndexed { index, msg ->
            val prev = filteredMessages.getOrNull(index - 1)
            val next = filteredMessages.getOrNull(index + 1)
            val showDayHeader = prev == null || !isSameCalendarDay(prev.createdAt, msg.createdAt)
            val isFirstInCluster = showDayHeader || !areClusteredMessages(prev, msg)
            val isLastInCluster = next == null ||
                !isSameCalendarDay(msg.createdAt, next.createdAt) ||
                !areClusteredMessages(msg, next)
            val dayHeaderLabel = formatDayHeader(msg.createdAt)
            val isOwn = msg.senderId == viewModel.currentUserId
            val liveSenderProfile = if (isOwn) currentUserProfile else peopleMap[msg.senderId]
            val displayMsg = if (liveSenderProfile != null) {
                val resolvedName = liveSenderProfile.displayName.ifBlank { msg.senderName }
                val resolvedPhoto = liveSenderProfile.photoUrl.ifBlank { msg.senderPhotoUrl }
                if (resolvedName != msg.senderName || resolvedPhoto != msg.senderPhotoUrl) {
                    msg.copy(senderName = resolvedName, senderPhotoUrl = resolvedPhoto)
                } else {
                    msg
                }
            } else {
                msg
            }
            val isRead = conversation?.isMessageReadByOthers(
                message = msg,
                currentUserId = viewModel.currentUserId
            ) == true
            val baseType = when {
                msg.type == Message.TYPE_AUDIO || resolveVoiceAudioUrl(msg) != null -> "audio"
                msg.type == Message.TYPE_IMAGE || !msg.imageBase64.isNullOrBlank() -> "image"
                msg.type == Message.TYPE_POLL -> "poll"
                else -> "text"
            }
            PreparedChatRoomRow(
                key = msg.id.ifBlank { "room_msg_$index" },
                contentType = if (showDayHeader) "${baseType}_hdr" else baseType,
                originalMessage = msg,
                displayMessage = displayMsg,
                isOwnMessage = isOwn,
                isRead = isRead,
                showDayHeader = showDayHeader,
                dayHeaderLabel = dayHeaderLabel,
                isFirstInCluster = isFirstInCluster,
                isLastInCluster = isLastInCluster
            )
        }
    }

    val listState = rememberLazyListState()
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) {
                true
            } else {
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 3 || !listState.canScrollForward
            }
        }
    }
    val showJumpToBottom by remember(preparedRows.size) {
        derivedStateOf {
            preparedRows.isNotEmpty() && listState.canScrollForward && !isNearBottom
        }
    }
    val floatingDateLabel by remember(preparedRows) {
        derivedStateOf {
            if (!listState.isScrollInProgress || preparedRows.size < 8) {
                null
            } else {
                val idx = listState.firstVisibleItemIndex.coerceIn(0, preparedRows.lastIndex)
                preparedRows[idx].dayHeaderLabel
            }
        }
    }

    val beginVoiceRecording: () -> Unit = {
        voicePlayer.stop()
        voiceRecorder.start()
            .onSuccess {
                isTrayOpen = false
                recordingSeconds = 0
                isRecordingAudio = true
            }
            .onFailure { err ->
                isRecordingAudio = false
                Toast.makeText(
                    context,
                    err.localizedMessage ?: "Microphone unavailable",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    val finishAndSendVoiceRecording: () -> Unit = {
        val conv = conversation
        val profile = currentUserProfile
        isRecordingAudio = false
        recordingAmplitude = 0.15f
        val recordedResult = voiceRecorder.stopRecording()
        if (conv != null && profile != null) {
            recordedResult.onSuccess { recorded ->
                viewModel.uploadAndSendVoiceNote(conv, profile, recorded)
            }.onFailure { err ->
                Toast.makeText(
                    context,
                    err.localizedMessage ?: "Voice recording too short",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            recordedResult.getOrNull()?.file?.delete()
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            var elapsedMs = 0L
            while (isRecordingAudio && recordingSeconds < VoiceNoteRecorder.MAX_DURATION_SEC) {
                delay(100L)
                elapsedMs += 100L
                recordingAmplitude = voiceRecorder.currentNormalizedAmplitude()
                recordingSeconds = (elapsedMs / 1000L).toInt()
            }
            if (isRecordingAudio && recordingSeconds >= VoiceNoteRecorder.MAX_DURATION_SEC) {
                finishAndSendVoiceRecording()
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginVoiceRecording()
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice messages.", Toast.LENGTH_SHORT).show()
        }
    }

    val requestStartVoiceRecording: () -> Unit = {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            beginVoiceRecording()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && conversation != null && currentUserProfile != null) {
            scope.launch {
                val base64 = withContext(Dispatchers.IO) {
                    ImageUtils.encodeUriToCompressedBase64(context, uri)
                }
                if (base64 == null) {
                    Toast.makeText(context, "Could not process selected photo.", Toast.LENGTH_SHORT).show()
                } else {
                    val caption = draftText
                    haptics.success()
                    viewModel.sendImage(conversation, currentUserProfile, caption, base64) {
                        draftText = ""
                        isTrayOpen = false
                    }
                }
            }
        }
    }

    LaunchedEffect(editingMessage?.id) {
        editingMessage?.let { draftText = it.text }
    }

    var previousRoomMessageCount by remember(viewModel.conversationId) { mutableIntStateOf(0) }
    LaunchedEffect(conversation?.id, preparedRows.size, conversation?.lastMessageAt, readReceiptsEnabled) {
        if (conversation != null) {
            viewModel.markRead(conversation, sendReadReceipt = readReceiptsEnabled)
        }
        val count = preparedRows.size
        if (count > 0 && !showSearchBar) {
            val lastRow = preparedRows.last()
            val isInitialLoad = previousRoomMessageCount == 0
            val shouldAutoScroll = isInitialLoad || isNearBottom || lastRow.isOwnMessage
            if (shouldAutoScroll) {
                val targetIndex = count - 1
                if (isInitialLoad || abs(targetIndex - listState.firstVisibleItemIndex) > 40) {
                    listState.scrollToItem(targetIndex)
                } else {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        }
        previousRoomMessageCount = count
    }

    val isOtherBlocked = !isGroup && !isSelfChat && otherId != null && otherId in blockedUserIds
    val canCallInThread = conversation != null && !isSelfChat && !isOtherBlocked &&
        conversation.participantIds.any { it != viewModel.currentUserId }
    val title = if (!isGroup && !isSelfChat && !otherProfile?.displayName.isNullOrBlank()) {
        otherProfile!!.displayName
    } else {
        conversation?.displayTitleFor(viewModel.currentUserId) ?: "Chat"
    }
    val isOtherOnline = !isGroup && !isSelfChat && !isOtherBlocked && otherProfile?.isOnline == true

    var typingClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(conversation?.typingBy, viewModel.currentUserId) {
        val hasOthersTyping = conversation?.typingBy?.any { it.key != viewModel.currentUserId } == true
        typingClockMs = System.currentTimeMillis()
        while (hasOthersTyping) {
            delay(1500L)
            typingClockMs = System.currentTimeMillis()
        }
    }
    LaunchedEffect(isOtherBlocked) {
        if (isOtherBlocked) viewModel.clearTypingStatus()
    }

    val activeTypingIds = remember(conversation?.typingBy, blockedUserIds, typingClockMs) {
        conversation?.activeTypingUserIds(
            currentUserId = viewModel.currentUserId,
            blockedUserIds = blockedUserIds,
            nowMillis = typingClockMs
        ).orEmpty()
    }
    val activeTypingNames = remember(activeTypingIds, peopleMap, conversation?.participantNames) {
        activeTypingIds.map { uid ->
            (peopleMap[uid]?.displayName ?: conversation?.participantNames?.get(uid) ?: "Someone")
                .substringBefore(" ")
                .ifBlank { "Someone" }
        }
    }
    val typingLeadPhoto = remember(activeTypingIds, peopleMap, conversation?.participantPhotos) {
        activeTypingIds.firstOrNull()?.let { uid ->
            peopleMap[uid]?.photoUrl ?: conversation?.participantPhotos?.get(uid)
        }
    }
    val typingHeaderSubtitle = remember(activeTypingNames, isGroup) {
        when {
            activeTypingNames.isEmpty() -> null
            !isGroup -> "typing..."
            activeTypingNames.size == 1 -> "${activeTypingNames[0]} is typing..."
            activeTypingNames.size == 2 -> "${activeTypingNames[0]} and ${activeTypingNames[1]} are typing..."
            else -> "${activeTypingNames[0]}, ${activeTypingNames[1]} +${activeTypingNames.size - 2} typing..."
        }
    }

    val subtitle = when {
        conversation == null -> "connecting..."
        isOtherBlocked -> "blocked"
        typingHeaderSubtitle != null -> typingHeaderSubtitle
        isSelfChat -> "encrypted personal notes"
        isGroup -> {
            val base = "${conversation.participantIds.size} members"
            if (conversation.topic.isNotBlank()) "$base, ${conversation.topic}" else base
        }
        otherProfile != null -> {
            val statusMessage = otherProfile.statusVibe.trim()
            when {
                otherProfile.isOnline && statusMessage.isNotBlank() -> "online · $statusMessage"
                otherProfile.isOnline -> "online"
                statusMessage.isNotBlank() -> statusMessage
                otherProfile.lastSeenAt != null -> "last seen ${formatRelativeTime(otherProfile.lastSeenAt)}"
                else -> "last seen recently"
            }
        }
        else -> "last seen recently"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.background(cosmicHeaderBrush),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                onBack()
                            },
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to chats"
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = conversation != null) {
                                haptics.lightTap()
                                when {
                                    isGroup -> showGroupInfoDialog = true
                                    !isSelfChat && otherId != null && onOpenUserProfile != null -> onOpenUserProfile(otherId)
                                    else -> showDirectInfoDialog = true
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        UserAvatar(
                            name = title,
                            photoUrl = if (isGroup || isSelfChat) null else (otherProfile?.photoUrl ?: conversation?.participantPhotos?.get(otherId)),
                            size = 40.dp,
                            isGroup = isGroup,
                            isSavedMessages = isSelfChat,
                            showPresence = !isGroup && !isSelfChat && otherProfile != null,
                            isOnline = isOtherOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isE2eeActive) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "End-to-end encrypted chat",
                                        tint = Color(0xFFB3E5FC),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .testTag("chat_header_e2ee_icon")
                                    )
                                }
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    typingHeaderSubtitle != null || isOtherOnline -> Color(0xFFB3E5FC)
                                    else -> Color.White.copy(alpha = 0.78f)
                                },
                                fontWeight = if (typingHeaderSubtitle != null) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (canCallInThread) {
                        val triggerCall: (Boolean) -> Unit = { isVideo ->
                            val conv = conversation
                            if (conv != null) {
                                if (isGroup) {
                                    pendingGroupCallVideo = isVideo
                                } else if (otherId != null) {
                                    val peerName = otherProfile?.displayName
                                        ?: conv.participantNames[otherId]
                                        ?: title
                                    val peerPhoto = otherProfile?.photoUrl
                                        ?: conv.participantPhotos[otherId]
                                    onStartCall(
                                        CallTarget(
                                            userId = otherId,
                                            displayName = peerName,
                                            photoUrl = peerPhoto,
                                            conversationId = conv.id
                                        ),
                                        isVideo
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                triggerCall(false)
                            },
                            modifier = Modifier.testTag("voice_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Start HD voice call"
                            )
                        }
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                triggerCall(true)
                            },
                            modifier = Modifier.testTag("video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = "Start HD video call"
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                showOverflowMenu = true
                            },
                            modifier = Modifier.testTag("chat_overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (showSearchBar) "Close Search" else "Search") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (showSearchBar) {
                                        showSearchBar = false
                                        inChatSearchQuery = ""
                                    } else {
                                        showSearchBar = true
                                    }
                                }
                            )
                            if (conversation != null) {
                                val isMuted = conversation.isMutedBy(viewModel.currentUserId)
                                DropdownMenuItem(
                                    text = { Text(if (isMuted) "Unmute" else "Mute notifications") },
                                    leadingIcon = {
                                        Icon(
                                            if (isMuted) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onToggleMuteConversation(conversation)
                                    }
                                )
                                if (!isGroup && !isSelfChat && otherId != null && onOpenUserProfile != null) {
                                    DropdownMenuItem(
                                        text = { Text("View Profile") },
                                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onOpenUserProfile(otherId)
                                        }
                                    )
                                }
                                if (isE2eeActive) {
                                    DropdownMenuItem(
                                        text = { Text("Encryption & Safety Number") },
                                        leadingIcon = { Icon(Icons.Rounded.VerifiedUser, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showE2eeSafetyDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (isGroup) "Group Info" else "Chat Info & Media") },
                                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        if (isGroup) showGroupInfoDialog = true else showDirectInfoDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keep wallpaper full-bleed so keyboard animations never resize or redraw it
            TelegramChatWallpaper()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                AnimatedVisibility(visible = showSearchBar) {
                    ChatRoomSearchBar(
                        query = inChatSearchQuery,
                        matchCount = filteredMessages.size,
                        onQueryChange = { inChatSearchQuery = it },
                        onClose = {
                            showSearchBar = false
                            inChatSearchQuery = ""
                        }
                    )
                }

                if (conversation != null && !conversation.pinnedMessageId.isNullOrBlank()) {
                    PinnedMessageBanner(
                        senderName = conversation.pinnedMessageSenderName.orEmpty(),
                        preview = conversation.pinnedMessagePreview.orEmpty(),
                        onJumpToPinned = {
                            val idx = filteredMessages.indexOfFirst { it.id == conversation.pinnedMessageId }
                            if (idx >= 0) {
                                scope.launch { listState.animateScrollToItem(idx) }
                            } else {
                                Toast.makeText(context, "Pinned message is not in current view.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUnpin = {
                            viewModel.pinOrUnpinMessage(conversation, null)
                        }
                    )
                }

                AnimatedVisibility(visible = errorBanner != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorBanner.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearErrorBanner) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss error")
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (val state = messagesState) {
                        is UiState.Loading -> {
                            UniversChatRoomSkeletonLoading()
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        is UiState.Success -> {
                            if (preparedRows.isEmpty()) {
                                ChatRoomEmptyState(
                                    isSearchFiltered = showSearchBar && inChatSearchQuery.isNotBlank(),
                                    searchQuery = inChatSearchQuery
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(vertical = 10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = preparedRows,
                                        key = { row -> row.key },
                                        contentType = { row -> row.contentType }
                                    ) { row ->
                                        val msg = row.originalMessage
                                        if (row.showDayHeader) {
                                            DayDividerPill(label = row.dayHeaderLabel)
                                        }

                                        val bubblePlaybackState = if (voicePlaybackState.activeMessageId == msg.id) {
                                            voicePlaybackState
                                        } else {
                                            IdleVoicePlaybackState
                                        }

                                        MessageBubble(
                                            message = row.displayMessage,
                                            isOwnMessage = row.isOwnMessage,
                                            isGroup = isGroup,
                                            currentUserId = viewModel.currentUserId,
                                            isRead = row.isRead,
                                            isFirstInCluster = row.isFirstInCluster,
                                            isLastInCluster = row.isLastInCluster,
                                            onLongPressOrTapActions = { selectedMessageForActions = msg },
                                            onToggleReaction = { emoji -> viewModel.toggleReaction(msg, emoji) },
                                            onInspectReactions = { reactionDetailsMessageId = msg.id },
                                            onVotePoll = { optionIdx -> viewModel.votePoll(msg, optionIdx) },
                                            onOpenImage = { bmp ->
                                                lightboxState = Triple(bmp, msg.senderName, msg.text)
                                            },
                                            voicePlaybackState = bubblePlaybackState,
                                            onToggleVoicePlayback = { voiceMsg ->
                                                val remoteUrl = resolveVoiceAudioUrl(voiceMsg)
                                                val encoded = voiceMsg.audioBase64
                                                if (!remoteUrl.isNullOrBlank() || !encoded.isNullOrBlank()) {
                                                    voicePlayer.togglePlayback(
                                                        messageId = voiceMsg.id,
                                                        audioUrl = remoteUrl,
                                                        audioBase64 = encoded
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = floatingDateLabel != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                    ) {
                        floatingDateLabel?.let { label ->
                            DayDividerPill(label = label)
                        }
                    }

                    JumpToLatestButton(
                        visible = showJumpToBottom,
                        onClick = {
                            scope.launch {
                                if (preparedRows.isNotEmpty()) {
                                    val targetIdx = preparedRows.lastIndex
                                    if (abs(targetIdx - listState.firstVisibleItemIndex) > 40) {
                                        listState.scrollToItem(targetIdx)
                                    } else {
                                        listState.animateScrollToItem(targetIdx)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 12.dp)
                    )
                }

                AnimatedVisibility(
                    visible = activeTypingNames.isNotEmpty() && !isOtherBlocked,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TypingIndicatorBubble(
                        typerNames = activeTypingNames,
                        typerPhotoUrl = typingLeadPhoto,
                        isGroup = isGroup
                    )
                }

                val bannerTarget = editingMessage ?: replyingTo
                AnimatedVisibility(visible = bannerTarget != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .height(IntrinsicSize.Min),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (editingMessage != null) {
                                            "Edit message"
                                        } else {
                                            "Reply to ${replyingTo?.senderName ?: ""}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TelegramBlue
                                    )
                                    Text(
                                        text = bannerTarget?.text?.ifBlank { "Photo / Voice / Poll" }.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptics.lightTap()
                                        if (editingMessage != null) draftText = ""
                                        viewModel.setEditingTarget(null)
                                        viewModel.setReplyTarget(null)
                                    }
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Cancel reply or edit")
                                }
                            }
                        }
                    }
                }

                if (isOtherBlocked && otherId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "You blocked this user",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                TextButton(
                                    onClick = { onToggleBlockUser(otherId, false) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("UNBLOCK", fontWeight = FontWeight.Bold, color = TelegramBlue)
                                }
                            }
                        }
                    }
                } else {
                    IosQuickInsertTray(
                        visible = isTrayOpen && !isRecordingAudio,
                        onPickPhoto = {
                            isTrayOpen = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCreatePoll = {
                            isTrayOpen = false
                            showPollDialog = true
                        },
                        onRecordVoice = {
                            isTrayOpen = false
                            requestStartVoiceRecording()
                        },
                        onInsertQuickReply = { snippet ->
                            val next = if (draftText.isBlank()) snippet else "$draftText $snippet"
                            draftText = next
                            viewModel.onComposerTextChanged(next, conversation, isOtherBlocked)
                            isTrayOpen = false
                        }
                    )

                    AnimatedVisibility(visible = voiceUploadState !is VoiceUploadState.Idle) {
                        VoiceUploadStatusStrip(
                            state = voiceUploadState,
                            onCancel = {
                                haptics.lightTap()
                                viewModel.cancelVoiceUpload()
                            },
                            onRetry = {
                                val conv = conversation
                                val profile = currentUserProfile
                                if (conv != null && profile != null) {
                                    haptics.lightTap()
                                    viewModel.retryVoiceUpload(conv, profile)
                                }
                            }
                        )
                    }

                    IosMessageComposerBar(
                        draftText = draftText,
                        onDraftChange = { next ->
                            draftText = next
                            viewModel.onComposerTextChanged(next, conversation, isOtherBlocked)
                        },
                        canInteract = conversation != null && currentUserProfile != null,
                        isSending = isSending,
                        isTrayOpen = isTrayOpen,
                        onToggleTray = { isTrayOpen = !isTrayOpen },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onOpenPoll = { showPollDialog = true },
                        onSend = {
                            val conv = conversation ?: return@IosMessageComposerBar
                            val profile = currentUserProfile ?: return@IosMessageComposerBar
                            val trimmed = draftText.trim()
                            if (trimmed.isEmpty()) return@IosMessageComposerBar
                            haptics.success()
                            val encryptedPayload = if (isE2eeActive && peerE2eeKey != null) {
                                SignalProtocolEngine.encryptMessage(
                                    context = context,
                                    conversationId = conv.id,
                                    senderId = viewModel.currentUserId,
                                    peerPublicKeyB64 = peerE2eeKey,
                                    plaintext = trimmed
                                )
                            } else {
                                null
                            }
                            viewModel.sendOrEditText(
                                conversation = conv,
                                senderProfile = profile,
                                text = trimmed,
                                encryptedPayload = encryptedPayload
                            ) {
                                draftText = ""
                                isTrayOpen = false
                            }
                        },
                        isRecordingAudio = isRecordingAudio,
                        recordingSeconds = recordingSeconds,
                        recordingAmplitude = recordingAmplitude,
                        onStartVoiceRecord = requestStartVoiceRecording,
                        onCancelVoiceRecord = {
                            isRecordingAudio = false
                            recordingAmplitude = 0.15f
                            voiceRecorder.cancel()
                        },
                        onSendVoiceRecord = finishAndSendVoiceRecording
                    )
                }
            }
        }
    }

    selectedMessageForActions?.let { msg ->
        val liveMsg = decryptedMessages.firstOrNull { it.id == msg.id } ?: msg
        val isPinned = conversation?.pinnedMessageId == liveMsg.id
        MessageActionsDialog(
            message = liveMsg,
            currentUserId = viewModel.currentUserId,
            isOwnMessage = liveMsg.senderId == viewModel.currentUserId,
            isPinnedInChat = isPinned,
            onDismiss = { selectedMessageForActions = null },
            onReact = { emoji -> viewModel.toggleReaction(liveMsg, emoji) },
            onInspectReactions = { reactionDetailsMessageId = liveMsg.id },
            onReply = { viewModel.setReplyTarget(liveMsg) },
            onEdit = { viewModel.setEditingTarget(liveMsg) },
            onDelete = { viewModel.deleteMessage(liveMsg) },
            onCopy = {
                val copyValue = resolveVoiceAudioUrl(liveMsg) ?: liveMsg.text
                clipboardManager.setText(AnnotatedString(copyValue))
                Toast.makeText(
                    context,
                    if (resolveVoiceAudioUrl(liveMsg) != null) "Audio link copied" else "Message copied",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onForward = if (allConversations.isNotEmpty()) {
                { messageToForward = liveMsg }
            } else null,
            onTogglePin = conversation?.let { conv ->
                {
                    viewModel.pinOrUnpinMessage(conv, if (isPinned) null else liveMsg) { pinned ->
                        Toast.makeText(
                            context,
                            if (pinned) "Message pinned to top" else "Message unpinned",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    reactionDetailsMessageId?.let { targetMsgId ->
        val liveMsg = decryptedMessages.firstOrNull { it.id == targetMsgId }
        if (liveMsg == null || liveMsg.reactions.isEmpty()) {
            reactionDetailsMessageId = null
        } else {
            MessageReactionsDetailDialog(
                message = liveMsg,
                currentUserId = viewModel.currentUserId,
                currentUserProfile = currentUserProfile,
                peopleMap = peopleMap,
                participantNames = conversation?.participantNames.orEmpty(),
                participantPhotos = conversation?.participantPhotos.orEmpty(),
                onToggleReaction = { emoji -> viewModel.toggleReaction(liveMsg, emoji) },
                onDismiss = { reactionDetailsMessageId = null }
            )
        }
    }

    messageToForward?.let { original ->
        val forwardTargets = remember(allConversations, conversation?.id) {
            allConversations.filter { it.id != conversation?.id }
        }
        ForwardMessageDialog(
            conversations = forwardTargets,
            currentUserId = viewModel.currentUserId,
            peopleMap = peopleMap,
            onDismiss = { messageToForward = null },
            onSelectConversation = { targetConv ->
                val profile = currentUserProfile
                messageToForward = null
                if (profile != null) {
                    viewModel.forwardMessage(targetConv, profile, original) {
                        Toast.makeText(
                            context,
                            "Forwarded to ${targetConv.displayTitleFor(viewModel.currentUserId)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    lightboxState?.let { (bitmap, senderName, caption) ->
        PhotoLightboxDialog(
            bitmap = bitmap,
            senderName = senderName,
            caption = caption,
            onDismiss = { lightboxState = null }
        )
    }

    if (showPollDialog && conversation != null && currentUserProfile != null) {
        CreatePollDialog(
            onDismiss = { showPollDialog = false },
            onCreatePoll = { question, options ->
                showPollDialog = false
                viewModel.sendPoll(conversation, currentUserProfile, question, options) {}
            }
        )
    }

    if (showGroupInfoDialog && conversation != null && isGroup) {
        GroupInfoDialog(
            conversation = conversation,
            allPeople = allPeople.filter { it.userId !in blockedUserIds },
            currentUserId = viewModel.currentUserId,
            onDismiss = { showGroupInfoDialog = false },
            onUpdateGroupInfo = { newTitle, newTopic ->
                viewModel.updateGroupInfo(conversation, newTitle, newTopic)
                showGroupInfoDialog = false
            },
            onAddMembers = { members ->
                viewModel.addMembers(conversation, members)
            },
            onLeaveGroup = {
                viewModel.leaveGroup(conversation) {
                    showGroupInfoDialog = false
                    onBack()
                }
            },
            onDeleteChannel = {
                viewModel.deleteConversation(conversation) {
                    showGroupInfoDialog = false
                    onBack()
                }
            },
            photoMessages = photoMessages,
            onOpenPhoto = { bmp, sender, cap ->
                showGroupInfoDialog = false
                lightboxState = Triple(bmp, sender, cap)
            },
            onTogglePin = { onTogglePinConversation(conversation) },
            onToggleMute = { onToggleMuteConversation(conversation) }
        )
    }

    if (showDirectInfoDialog && conversation != null && !isGroup) {
        DirectChatInfoDialog(
            conversation = conversation,
            otherProfile = otherProfile,
            currentUserId = viewModel.currentUserId,
            messageCount = messages.size,
            onDismiss = { showDirectInfoDialog = false },
            onDeleteConversation = {
                viewModel.deleteConversation(conversation) {
                    showDirectInfoDialog = false
                    onBack()
                }
            },
            onStartVoiceCall = if (canCallInThread && otherId != null) {
                {
                    val peerName = otherProfile?.displayName ?: conversation.participantNames[otherId] ?: title
                    val peerPhoto = otherProfile?.photoUrl ?: conversation.participantPhotos[otherId]
                    onStartCall(
                        CallTarget(
                            userId = otherId,
                            displayName = peerName,
                            photoUrl = peerPhoto,
                            conversationId = conversation.id
                        ),
                        false
                    )
                }
            } else null,
            onStartVideoCall = if (canCallInThread && otherId != null) {
                {
                    val peerName = otherProfile?.displayName ?: conversation.participantNames[otherId] ?: title
                    val peerPhoto = otherProfile?.photoUrl ?: conversation.participantPhotos[otherId]
                    onStartCall(
                        CallTarget(
                            userId = otherId,
                            displayName = peerName,
                            photoUrl = peerPhoto,
                            conversationId = conversation.id
                        ),
                        true
                    )
                }
            } else null,
            photoMessages = photoMessages,
            onOpenPhoto = { bmp, sender, cap ->
                showDirectInfoDialog = false
                lightboxState = Triple(bmp, sender, cap)
            },
            onTogglePin = { onTogglePinConversation(conversation) },
            onToggleMute = { onToggleMuteConversation(conversation) },
            isOtherBlocked = isOtherBlocked,
            onToggleBlock = if (!isSelfChat && otherId != null) {
                { onToggleBlockUser(otherId, !isOtherBlocked) }
            } else null
        )
    }

    pendingGroupCallVideo?.let { isVideo ->
        val conv = conversation
        if (conv != null) {
            GroupCallMemberPickerDialog(
                conversation = conv,
                currentUserId = viewModel.currentUserId,
                isVideoCall = isVideo,
                onDismiss = { pendingGroupCallVideo = null },
                onSelectMember = { memberId, memberName, memberPhoto ->
                    pendingGroupCallVideo = null
                    onStartCall(
                        CallTarget(
                            userId = memberId,
                            displayName = memberName,
                            photoUrl = memberPhoto,
                            conversationId = conv.id
                        ),
                        isVideo
                    )
                }
            )
        }
    }

    if (showE2eeSafetyDialog && isE2eeActive && peerE2eeKey != null) {
        val localPubKey = remember(viewModel.currentUserId) {
            E2eeKeyManager.getPublicIdentityKeyBase64(context, viewModel.currentUserId)
        }
        val safetyNumber = remember(localPubKey, peerE2eeKey) {
            E2eeKeyManager.computeSafetyNumber(localPubKey, peerE2eeKey)
        }
        AlertDialog(
            onDismissRequest = { showE2eeSafetyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = TelegramBlue,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "End-to-End Encrypted",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Messages in this conversation are secured with Signal-style X3DH + ephemeral ECDH key exchange and AES-256-GCM. Private keys stay inside Android Keystore on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Safety Number",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = safetyNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("e2ee_safety_number_text")
                        )
                    }
                    Text(
                        text = "Compare this 60-digit number with $title to verify your encryption keys.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(safetyNumber))
                        Toast.makeText(context, "Safety number copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showE2eeSafetyDialog = false }) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun areClusteredMessages(first: Message?, second: Message?): Boolean {
    if (first == null || second == null) return false
    if (first.senderId != second.senderId) return false
    val t1 = first.createdAt?.toDate()?.time ?: return false
    val t2 = second.createdAt?.toDate()?.time ?: return false
    return abs(t2 - t1) <= 180_000L
}
