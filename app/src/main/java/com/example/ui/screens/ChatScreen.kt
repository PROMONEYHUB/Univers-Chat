package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.RecordedVoiceFile
import com.example.audio.VoiceNotePlayer
import com.example.audio.VoiceNoteRecorder
import com.example.audio.formatDurationSec
import com.example.data.model.Message
import com.example.data.repository.ChatPhotoUploader
import com.example.data.repository.ChatVoiceUploader
import com.example.ui.components.TelegramChatWallpaper
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDayHeader
import com.example.ui.components.isSameCalendarDay
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.IosRed
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.PulseBrushes
import com.example.ui.theme.PulseChatTheme
import com.example.ui.theme.TelegramBlueBright
import com.example.ui.viewmodel.VoiceUploadState
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Lightweight item model for standalone/stateless usages of [ChatScreen].
 */
data class ChatMessageItem(
    val id: String,
    val text: String,
    val isSentByMe: Boolean,
    val senderName: String = "",
    val createdAt: Timestamp? = Timestamp.now()
)

private sealed interface PhotoUploadState {
    data object Idle : PhotoUploadState
    data class Uploading(val localUri: Uri, val progress: Float) : PhotoUploadState
    data class Error(val localUri: Uri, val message: String) : PhotoUploadState
}

@Immutable
private data class PreparedChatMessageRow(
    val key: String,
    val contentType: String,
    val message: Message,
    val showDayHeader: Boolean,
    val dayHeaderLabel: String,
    val isFirstInCluster: Boolean,
    val isLastInCluster: Boolean
)

/**
 * Displays a full chat conversation using a [LazyColumn] of alternating sent/received
 * bubble-style messages over the app's chat wallpaper, paired with a bottom text input
 * row supporting zero-permission gallery photo picking (`ActivityResultContracts.PickVisualMedia`),
 * Firebase Storage upload, and sending the uploaded photo URL as a message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    chatTitle: String = stringResource(R.string.app_name),
    chatSubtitle: String? = null,
    avatarPhotoUrl: String? = null,
    isGroup: Boolean = false,
    isOnline: Boolean = false,
    showBackButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onPhotoUploaded: ((String) -> Unit)? = null,
    onVoiceUploaded: ((String) -> Unit)? = null,
    onMessageLongPress: (Message) -> Unit = {},
    onToggleReaction: (Message, String) -> Unit = { _, _ -> },
    onVotePoll: (Message, Int) -> Unit = { _, _ -> },
    photoUploader: suspend (
        context: Context,
        imageUri: Uri,
        senderId: String,
        onProgress: (Float) -> Unit
    ) -> Result<String> = { ctx, uri, uid, onProgress ->
        ChatPhotoUploader.uploadChatPhoto(
            context = ctx,
            imageUri = uri,
            senderId = uid,
            onProgress = onProgress
        )
    },
    voiceUploader: suspend (
        audioFile: File,
        senderId: String,
        onProgress: (Float) -> Unit
    ) -> Result<String> = { file, uid, onProgress ->
        ChatVoiceUploader.uploadVoiceAudio(
            audioFile = file,
            senderId = uid,
            onProgress = onProgress
        )
    }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsTelegramDark.current
    val haptics = rememberIosHaptics()
    val headerBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)
    val listState = rememberLazyListState()
    var draftText by rememberSaveable { mutableStateOf("") }
    var lightboxState by remember { mutableStateOf<Triple<Bitmap, String, String>?>(null) }
    var remoteLightboxUrl by remember { mutableStateOf<Pair<String, String>?>(null) }
    var uploadState by remember { mutableStateOf<PhotoUploadState>(PhotoUploadState.Idle) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var voiceUploadState by remember { mutableStateOf<VoiceUploadState>(VoiceUploadState.Idle) }
    var voiceUploadJob by remember { mutableStateOf<Job?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingAmplitude by remember { mutableFloatStateOf(0.18f) }

    val voiceRecorder = remember(context) { VoiceNoteRecorder(context) }
    val voicePlayer = remember(context) { VoiceNotePlayer(context) }
    val voicePlaybackState by voicePlayer.state.collectAsStateWithLifecycle()

    val defaultUploadFailedMsg = stringResource(R.string.chat_upload_photo_failed)
    val defaultVoiceUploadFailedMsg = stringResource(R.string.chat_upload_voice_failed)
    val micPermissionRequiredMsg = stringResource(R.string.chat_voice_permission_required)

    DisposableEffect(Unit) {
        onDispose {
            voiceUploadJob?.cancel()
            voiceRecorder.cancel()
            voicePlayer.release()
        }
    }

    LaunchedEffect(voicePlaybackState.errorMessage) {
        val err = voicePlaybackState.errorMessage
        if (!err.isNullOrBlank()) {
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
    }

    fun startPhotoUpload(uri: Uri) {
        uploadJob?.cancel()
        uploadState = PhotoUploadState.Uploading(localUri = uri, progress = 0f)
        uploadJob = coroutineScope.launch {
            try {
                val result = photoUploader(context, uri, currentUserId) { fraction ->
                    uploadState = PhotoUploadState.Uploading(
                        localUri = uri,
                        progress = fraction.coerceIn(0f, 1f)
                    )
                }
                result.onSuccess { downloadUrl ->
                    haptics.success()
                    uploadState = PhotoUploadState.Idle
                    if (onPhotoUploaded != null) {
                        onPhotoUploaded(downloadUrl)
                    } else {
                        onSendMessage(downloadUrl)
                    }
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                    uploadState = PhotoUploadState.Error(
                        localUri = uri,
                        message = err.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: defaultUploadFailedMsg
                    )
                }
            } catch (_: CancellationException) {
                uploadState = PhotoUploadState.Idle
            }
        }
    }

    fun startVoiceUpload(recorded: RecordedVoiceFile) {
        voiceUploadJob?.cancel()
        voiceUploadState = VoiceUploadState.Uploading(
            progress = 0f,
            durationSec = recorded.durationSec
        )
        voiceUploadJob = coroutineScope.launch {
            try {
                val result = voiceUploader(recorded.file, currentUserId) { fraction ->
                    voiceUploadState = VoiceUploadState.Uploading(
                        progress = fraction.coerceIn(0f, 1f),
                        durationSec = recorded.durationSec
                    )
                }
                result.onSuccess { downloadUrl ->
                    runCatching { recorded.file.delete() }
                    haptics.success()
                    voiceUploadState = VoiceUploadState.Idle
                    if (onVoiceUploaded != null) {
                        onVoiceUploaded(downloadUrl)
                    } else {
                        onSendMessage(downloadUrl)
                    }
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                    voiceUploadState = VoiceUploadState.Error(
                        message = err.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: defaultVoiceUploadFailedMsg,
                        file = recorded.file,
                        durationSec = recorded.durationSec
                    )
                }
            } catch (_: CancellationException) {
                runCatching { recorded.file.delete() }
                voiceUploadState = VoiceUploadState.Idle
            }
        }
    }

    fun beginVoiceRecording() {
        voicePlayer.stop()
        voiceRecorder.start()
            .onSuccess {
                recordingSeconds = 0
                recordingAmplitude = 0.18f
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

    fun finishAndUploadVoiceRecording() {
        isRecordingAudio = false
        recordingAmplitude = 0.15f
        voiceRecorder.stopRecording()
            .onSuccess { recorded -> startVoiceUpload(recorded) }
            .onFailure { err ->
                Toast.makeText(
                    context,
                    err.localizedMessage ?: "Voice recording too short",
                    Toast.LENGTH_SHORT
                ).show()
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
                finishAndUploadVoiceRecording()
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginVoiceRecording()
        } else {
            Toast.makeText(context, micPermissionRequiredMsg, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestStartVoiceRecording() {
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

    // Zero-permission Android Photo Picker compliant with Google Play policies
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            haptics.lightTap()
            startPhotoUpload(selectedUri)
        }
    }

    val preparedRows = remember(messages) {
        messages.mapIndexed { index, message ->
            val prev = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)
            val showDayHeader = prev == null || !isSameCalendarDay(prev.createdAt, message.createdAt)
            val isFirstInCluster = showDayHeader || !areMessagesClustered(prev, message)
            val isLastInCluster = next == null ||
                !isSameCalendarDay(message.createdAt, next.createdAt) ||
                !areMessagesClustered(message, next)
            val dayLabel = formatDayHeader(message.createdAt)
            val resolvedContentType = when {
                message.type == Message.TYPE_AUDIO || resolveVoiceAudioUrl(message) != null -> "audio"
                message.type == Message.TYPE_IMAGE || !message.imageBase64.isNullOrBlank() -> "image"
                message.type == Message.TYPE_POLL -> "poll"
                else -> "text"
            }
            PreparedChatMessageRow(
                key = message.id.ifBlank { "msg_$index" },
                contentType = if (showDayHeader) "${resolvedContentType}_hdr" else resolvedContentType,
                message = message,
                showDayHeader = showDayHeader,
                dayHeaderLabel = dayLabel,
                isFirstInCluster = isFirstInCluster,
                isLastInCluster = isLastInCluster
            )
        }
    }

    // Derived scroll states prevent per-pixel scroll recompositions across long histories
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) {
                true
            } else {
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex >= total - 3 || !listState.canScrollForward
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

    var previousMessageCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(preparedRows.size) {
        val count = preparedRows.size
        if (count > 0) {
            val lastMessage = preparedRows.last().message
            val isInitialLoad = previousMessageCount == 0
            val shouldAutoScroll = isInitialLoad || isNearBottom || lastMessage.senderId == currentUserId
            if (shouldAutoScroll) {
                val lastIndex = count - 1
                val distanceFromEnd = abs(lastIndex - listState.firstVisibleItemIndex)
                if (isInitialLoad || distanceFromEnd > 40) {
                    listState.scrollToItem(lastIndex)
                } else {
                    listState.animateScrollToItem(lastIndex)
                }
            }
        }
        previousMessageCount = count
    }

    val isUploadingPhoto = uploadState is PhotoUploadState.Uploading
    val isUploadingVoice = voiceUploadState is VoiceUploadState.Uploading
    val canSend = draftText.trim().isNotEmpty()
    val defaultSubtitle = if (isOnline) {
        stringResource(R.string.chat_online_status)
    } else {
        stringResource(R.string.app_name)
    }
    val resolvedSubtitle = chatSubtitle ?: defaultSubtitle

    fun submitDraft() {
        val trimmed = draftText.trim()
        if (trimmed.isNotEmpty()) {
            haptics.lightTap()
            onSendMessage(trimmed)
            draftText = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.background(headerBrush),
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
                                onBackClick()
                            },
                            modifier = Modifier.testTag("chat_screen_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.chat_back_desc)
                            )
                        }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            name = chatTitle,
                            photoUrl = avatarPhotoUrl,
                            size = 40.dp,
                            isGroup = isGroup,
                            showPresence = !isGroup,
                            isOnline = isOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = chatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = resolvedSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) {
                                    TelegramBlueBright
                                } else {
                                    Color.White.copy(alpha = 0.78f)
                                },
                                fontWeight = if (isOnline) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
            TelegramChatWallpaper(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (preparedRows.isEmpty()) {
                        ChatEmptyState(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 560.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                                .testTag("chat_messages_list")
                        ) {
                            items(
                                items = preparedRows,
                                key = { row -> row.key },
                                contentType = { row -> row.contentType }
                            ) { row ->
                                val message = row.message
                                if (row.showDayHeader) {
                                    ChatDayDividerPill(
                                        label = row.dayHeaderLabel,
                                        isDark = isDark
                                    )
                                }

                                val bubblePlaybackState = if (voicePlaybackState.activeMessageId == message.id) {
                                    voicePlaybackState
                                } else {
                                    IdleVoicePlaybackState
                                }

                                MessageBubble(
                                    message = message,
                                    isOwnMessage = message.senderId == currentUserId,
                                    isGroup = isGroup,
                                    currentUserId = currentUserId,
                                    isFirstInCluster = row.isFirstInCluster,
                                    isLastInCluster = row.isLastInCluster,
                                    onLongPressOrTapActions = { onMessageLongPress(message) },
                                    onToggleReaction = { emoji -> onToggleReaction(message, emoji) },
                                    onVotePoll = { optionIdx -> onVotePoll(message, optionIdx) },
                                    onOpenImage = { bmp ->
                                        lightboxState = Triple(bmp, message.senderName, message.text)
                                    },
                                    onOpenImageUrl = { url ->
                                        remoteLightboxUrl = url to message.senderName
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

                        androidx.compose.animation.AnimatedVisibility(
                            visible = floatingDateLabel != null,
                            enter = fadeIn(animationSpec = tween(140)),
                            exit = fadeOut(animationSpec = tween(260)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                        ) {
                            floatingDateLabel?.let { label ->
                                ChatDayDividerPill(label = label, isDark = isDark)
                            }
                        }

                        JumpToLatestButton(
                            visible = showJumpToBottom,
                            onClick = {
                                coroutineScope.launch {
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
                                .testTag("chat_screen_jump_to_bottom")
                        )
                    }
            }

            AnimatedVisibility(visible = uploadState !is PhotoUploadState.Idle) {
                PhotoUploadStatusStrip(
                    state = uploadState,
                    onCancel = {
                        haptics.lightTap()
                        uploadJob?.cancel()
                        uploadState = PhotoUploadState.Idle
                    },
                    onRetry = { uri ->
                        haptics.lightTap()
                        startPhotoUpload(uri)
                    }
                )
            }

            AnimatedVisibility(visible = voiceUploadState !is VoiceUploadState.Idle) {
                VoiceUploadStatusStrip(
                    state = voiceUploadState,
                    onCancel = {
                        haptics.lightTap()
                        voiceUploadJob?.cancel()
                        val errFile = (voiceUploadState as? VoiceUploadState.Error)?.file
                        runCatching { errFile?.delete() }
                        voiceUploadState = VoiceUploadState.Idle
                    },
                    onRetry = {
                        val errState = voiceUploadState as? VoiceUploadState.Error
                        if (errState != null) {
                            haptics.lightTap()
                            startVoiceUpload(
                                RecordedVoiceFile(file = errState.file, durationSec = errState.durationSec)
                            )
                        }
                    }
                )
            }

            ChatBottomInputRow(
                text = draftText,
                onTextChange = { draftText = it.take(2000) },
                canSend = canSend,
                isUploadingPhoto = isUploadingPhoto,
                isUploadingVoice = isUploadingVoice,
                isRecordingAudio = isRecordingAudio,
                recordingSeconds = recordingSeconds,
                recordingAmplitude = recordingAmplitude,
                onAttachPhotoClick = {
                    haptics.lightTap()
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onStartVoiceRecord = {
                    haptics.lightTap()
                    requestStartVoiceRecording()
                },
                onCancelVoiceRecord = {
                    haptics.lightTap()
                    isRecordingAudio = false
                    recordingAmplitude = 0.15f
                    voiceRecorder.cancel()
                },
                onSendVoiceRecord = {
                    haptics.success()
                    finishAndUploadVoiceRecording()
                },
                onSend = ::submitDraft
            )
            }
        }
    }

    lightboxState?.let { (bitmap, senderName, caption) ->
        PhotoLightboxDialog(
            bitmap = bitmap,
            senderName = senderName,
            caption = caption,
            onDismiss = { lightboxState = null }
        )
    }

    remoteLightboxUrl?.let { (imageUrl, senderName) ->
        RemotePhotoLightboxDialog(
            imageUrl = imageUrl,
            senderName = senderName,
            onDismiss = { remoteLightboxUrl = null }
        )
    }
}

/**
 * Convenience overload for rendering lightweight [ChatMessageItem] lists with alternating
 * sent/received bubbles, gallery photo attachment upload to Firebase Storage, and a bottom
 * text input row.
 */
@JvmName("ChatScreenWithItems")
@Composable
fun ChatScreen(
    items: List<ChatMessageItem>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    chatTitle: String = stringResource(R.string.app_name),
    chatSubtitle: String? = null,
    isOnline: Boolean = true,
    showBackButton: Boolean = true,
    onBackClick: () -> Unit = {}
) {
    val currentUserId = "local_me"
    val mappedMessages = remember(items) {
        items.map { item ->
            Message(
                id = item.id,
                senderId = if (item.isSentByMe) currentUserId else "peer",
                senderName = item.senderName.ifBlank { if (item.isSentByMe) "You" else chatTitle },
                text = item.text,
                createdAt = item.createdAt
            )
        }
    }
    ChatScreen(
        messages = mappedMessages,
        currentUserId = currentUserId,
        modifier = modifier,
        chatTitle = chatTitle,
        chatSubtitle = chatSubtitle,
        isOnline = isOnline,
        showBackButton = showBackButton,
        onBackClick = onBackClick,
        onSendMessage = onSendMessage
    )
}

@Composable
private fun PhotoUploadStatusStrip(
    state: PhotoUploadState,
    onCancel: () -> Unit,
    onRetry: (Uri) -> Unit
) {
    Surface(
        color = when (state) {
            is PhotoUploadState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when (state) {
                is PhotoUploadState.Uploading -> {
                    val percent = (state.progress * 100f).roundToInt().coerceIn(0, 100)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("chat_screen_upload_progress_strip"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = state.localUri,
                            contentDescription = stringResource(R.string.chat_shared_photo_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (percent > 0) {
                                    stringResource(R.string.chat_uploading_photo_percent, percent)
                                } else {
                                    stringResource(R.string.chat_uploading_photo)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { state.progress.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_screen_cancel_upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.chat_upload_cancel_desc),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is PhotoUploadState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("chat_screen_upload_error_banner"),
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
                            onClick = { onRetry(state.localUri) },
                            modifier = Modifier.testTag("chat_screen_retry_upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.chat_upload_cancel_desc),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                PhotoUploadState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun RemotePhotoLightboxDialog(
    imageUrl: String,
    senderName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .navigationBarsPadding()
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.chat_shared_photo_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = senderName.ifBlank { stringResource(R.string.chat_shared_photo_desc) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close photo",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatDayDividerPill(
    label: String,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (isDark) {
                Color(0xB30D1B2E)
            } else {
                Color(0xCCFFFFFF)
            }
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ChatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.WavingHand,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.chat_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.chat_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatBottomInputRow(
    text: String,
    onTextChange: (String) -> Unit,
    canSend: Boolean,
    isUploadingPhoto: Boolean,
    isUploadingVoice: Boolean,
    isRecordingAudio: Boolean,
    recordingSeconds: Int,
    recordingAmplitude: Float,
    onAttachPhotoClick: () -> Unit,
    onStartVoiceRecord: () -> Unit,
    onCancelVoiceRecord: () -> Unit,
    onSendVoiceRecord: () -> Unit,
    onSend: () -> Unit
) {
    val smoothAmp by animateFloatAsState(
        targetValue = recordingAmplitude.coerceIn(0.1f, 1f),
        animationSpec = tween(durationMillis = 100),
        label = "chat_screen_voice_amp"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (isRecordingAudio) {
                val pulseTransition = rememberInfiniteTransition(label = "chat_screen_rec_dot")
                val dotAlpha by pulseTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(550, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "chat_screen_dot_alpha"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCancelVoiceRecord,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_screen_cancel_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.chat_cancel_voice_desc),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(IosRed.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FiberManualRecord,
                            contentDescription = null,
                            tint = IosRed.copy(alpha = dotAlpha),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${formatDurationSec(recordingSeconds)} / ${formatDurationSec(VoiceNoteRecorder.MAX_DURATION_SEC)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = IosRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                        ) {
                            val factors = listOf(0.42f, 0.72f, 0.56f, 0.94f, 0.65f, 1.0f, 0.78f, 0.48f, 0.86f, 0.62f, 0.90f, 0.50f)
                            factors.forEachIndexed { idx, base ->
                                val wave = ((smoothAmp * base) + if ((recordingSeconds + idx) % 2 == 0) 0.12f else 0.04f)
                                    .coerceIn(0.18f, 1f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(wave)
                                        .clip(CircleShape)
                                        .background(IosRed.copy(alpha = 0.82f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = onSendVoiceRecord,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PulseBrushes.BrandGradient)
                            .testTag("chat_screen_send_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.chat_send_voice_desc),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = onAttachPhotoClick,
                        enabled = !isUploadingPhoto,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_screen_attach_photo_button")
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.chat_attach_photo_desc),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                            .heightIn(min = 44.dp, max = 120.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .border(
                                width = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.chat_message_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { if (canSend) onSend() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_screen_composer_input")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (canSend) {
                        FilledIconButton(
                            onClick = onSend,
                            enabled = true,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PulseBrushes.BrandGradient)
                                .testTag("chat_screen_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.chat_send_message_desc),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        FilledIconButton(
                            onClick = onStartVoiceRecord,
                            enabled = !isUploadingVoice,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_screen_record_voice_button")
                        ) {
                            if (isUploadingVoice) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = stringResource(R.string.chat_record_voice_desc),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun areMessagesClustered(first: Message?, second: Message?): Boolean {
    if (first == null || second == null) return false
    if (first.senderId != second.senderId) return false
    val t1 = first.createdAt?.toDate()?.time ?: return false
    val t2 = second.createdAt?.toDate()?.time ?: return false
    return abs(t2 - t1) <= 180_000L
}

@Preview(showBackground = true, name = "Univers ChatScreen Light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Univers ChatScreen Dark")
@Composable
private fun ChatScreenPreview() {
    val now = Timestamp.now()
    val sampleMessages = listOf(
        Message(
            id = "m1",
            senderId = "alex",
            senderName = "Alex Rivera",
            text = "Hey! Are we still on for the launch review this afternoon?",
            createdAt = now
        ),
        Message(
            id = "m2",
            senderId = "me",
            senderName = "You",
            text = "Yes, everything is ready on my end. Sending over the final notes now.",
            reactions = mapOf("alex" to "👍"),
            createdAt = now
        ),
        Message(
            id = "m3",
            senderId = "me",
            senderName = "You",
            text = "See you at 3 PM!",
            createdAt = now
        ),
        Message(
            id = "m4",
            senderId = "alex",
            senderName = "Alex Rivera",
            text = "Awesome, see you then!",
            createdAt = now
        )
    )
    PulseChatTheme {
        ChatScreen(
            messages = sampleMessages,
            currentUserId = "me",
            chatTitle = "Alex Rivera",
            isOnline = true
        )
    }
}
