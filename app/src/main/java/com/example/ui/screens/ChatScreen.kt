package com.example.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Message
import com.example.data.repository.ChatPhotoUploader
import com.example.ui.components.TelegramChatWallpaper
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDayHeader
import com.example.ui.components.isSameCalendarDay
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.PulseBrushes
import com.example.ui.theme.PulseChatTheme
import com.example.ui.theme.TelegramBlueBright
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    val defaultUploadFailedMsg = stringResource(R.string.chat_upload_photo_failed)

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

    // Zero-permission Android Photo Picker compliant with Google Play policies
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            haptics.lightTap()
            startPhotoUpload(selectedUri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val isUploadingPhoto = uploadState is PhotoUploadState.Uploading
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                TelegramChatWallpaper(modifier = Modifier.fillMaxSize())

                if (messages.isEmpty()) {
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
                        itemsIndexed(
                            items = messages,
                            key = { index, msg -> msg.id.ifBlank { "msg_$index" } }
                        ) { index, message ->
                            val prev = messages.getOrNull(index - 1)
                            val next = messages.getOrNull(index + 1)
                            val showDayHeader = prev == null || !isSameCalendarDay(prev.createdAt, message.createdAt)
                            val isFirstInCluster = showDayHeader || !areMessagesClustered(prev, message)
                            val isLastInCluster = next == null ||
                                !isSameCalendarDay(message.createdAt, next.createdAt) ||
                                !areMessagesClustered(message, next)

                            if (showDayHeader) {
                                ChatDayDividerPill(
                                    label = formatDayHeader(message.createdAt),
                                    isDark = isDark
                                )
                            }

                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId,
                                isGroup = isGroup,
                                currentUserId = currentUserId,
                                isFirstInCluster = isFirstInCluster,
                                isLastInCluster = isLastInCluster,
                                onLongPressOrTapActions = { onMessageLongPress(message) },
                                onToggleReaction = { emoji -> onToggleReaction(message, emoji) },
                                onVotePoll = { optionIdx -> onVotePoll(message, optionIdx) },
                                onOpenImage = { bmp ->
                                    lightboxState = Triple(bmp, message.senderName, message.text)
                                },
                                onOpenImageUrl = { url ->
                                    remoteLightboxUrl = url to message.senderName
                                }
                            )
                        }
                    }
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

            ChatBottomInputRow(
                text = draftText,
                onTextChange = { draftText = it.take(2000) },
                canSend = canSend,
                isUploadingPhoto = isUploadingPhoto,
                onAttachPhotoClick = {
                    haptics.lightTap()
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSend = ::submitDraft
            )
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
    onAttachPhotoClick: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (canSend) {
                                Modifier.background(PulseBrushes.BrandGradient)
                            } else {
                                Modifier
                            }
                        )
                        .testTag("chat_screen_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.chat_send_message_desc),
                        modifier = Modifier.size(22.dp)
                    )
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
