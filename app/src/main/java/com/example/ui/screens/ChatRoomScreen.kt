package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ImageUtils
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDayHeader
import com.example.ui.components.isSameCalendarDay
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.OnlinePresenceGreen
import com.example.ui.viewmodel.ChatRoomViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: ChatRoomViewModel,
    currentUserProfile: UserProfile?,
    allPeople: List<UserProfile>,
    readReceiptsEnabled: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberIosHaptics()
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()

    var draftText by rememberSaveable(viewModel.conversationId) { mutableStateOf("") }
    var selectedMessageForActions by remember { mutableStateOf<Message?>(null) }
    var lightboxState by remember { mutableStateOf<Triple<Bitmap, String, String>?>(null) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showDirectInfoDialog by remember { mutableStateOf(false) }
    var showSearchBar by rememberSaveable(viewModel.conversationId) { mutableStateOf(false) }
    var inChatSearchQuery by rememberSaveable(viewModel.conversationId) { mutableStateOf("") }
    var isTrayOpen by remember { mutableStateOf(false) }

    val conversation = (conversationState as? UiState.Success)?.data
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val filteredMessages = remember(messages, showSearchBar, inChatSearchQuery) {
        if (!showSearchBar || inChatSearchQuery.isBlank()) messages
        else messages.filter { msg ->
            msg.text.contains(inChatSearchQuery, ignoreCase = true) ||
                msg.senderName.contains(inChatSearchQuery, ignoreCase = true) ||
                (msg.pollQuestion?.contains(inChatSearchQuery, ignoreCase = true) == true) ||
                msg.pollOptions.any { it.contains(inChatSearchQuery, ignoreCase = true) }
        }
    }

    val listState = rememberLazyListState()
    val showJumpToBottom by remember {
        derivedStateOf { listState.canScrollForward && filteredMessages.isNotEmpty() }
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

    LaunchedEffect(conversation?.id, messages.size, readReceiptsEnabled) {
        if (readReceiptsEnabled && conversation != null) {
            viewModel.markRead(conversation)
        }
        if (messages.isNotEmpty() && !showSearchBar) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val otherId = conversation?.otherParticipantId(viewModel.currentUserId)
    val otherProfile = remember(otherId, allPeople) {
        otherId?.let { id -> allPeople.firstOrNull { it.userId == id } }
    }
    val isGroup = conversation?.type == Conversation.TYPE_GROUP
    val title = conversation?.displayTitleFor(viewModel.currentUserId) ?: "Chat"
    val isOtherOnline = !isGroup && otherProfile?.isOnline == true
    val subtitle = when {
        conversation == null -> "Loading…"
        isGroup -> {
            val base = "${conversation.participantIds.size} members"
            if (conversation.topic.isNotBlank()) "$base · ${conversation.topic}" else base
        }
        otherProfile != null -> {
            if (otherProfile.isOnline) "Active now · ${otherProfile.statusVibe}" else "@${otherProfile.handle}"
        }
        else -> "Direct conversation"
    }

    val chatCanvasColor = if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = chatCanvasColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
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
                                contentDescription = "Back to chats",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = conversation != null) {
                                haptics.lightTap()
                                if (isGroup) showGroupInfoDialog = true
                                else showDirectInfoDialog = true
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        UserAvatar(
                            name = title,
                            photoUrl = if (isGroup) null else (otherProfile?.photoUrl ?: conversation?.participantPhotos?.get(otherId)),
                            size = 40.dp,
                            isGroup = isGroup,
                            showPresence = !isGroup && otherProfile != null,
                            isOnline = isOtherOnline,
                            showStoryRing = isOtherOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOtherOnline) OnlinePresenceGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOtherOnline) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .border(
                                width = 0.6.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    if (showSearchBar) {
                                        showSearchBar = false
                                        inChatSearchQuery = ""
                                    } else {
                                        showSearchBar = true
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("chat_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search conversation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    if (isGroup) showGroupInfoDialog = true
                                    else showDirectInfoDialog = true
                                },
                                enabled = conversation != null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag(if (isGroup) "group_info_button" else "direct_info_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = if (isGroup) "Channel info" else "Contact info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
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
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)

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

            // Crisp iMessage Conversation Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(chatCanvasColor)
            ) {
                when (val state = messagesState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (filteredMessages.isEmpty()) {
                            ChatRoomEmptyState(
                                isSearchFiltered = showSearchBar && inChatSearchQuery.isNotBlank(),
                                searchQuery = inChatSearchQuery
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(filteredMessages, key = { _, msg -> msg.id }) { index, msg ->
                                    val prev = filteredMessages.getOrNull(index - 1)
                                    val next = filteredMessages.getOrNull(index + 1)
                                    val showDayHeader = prev == null || !isSameCalendarDay(prev.createdAt, msg.createdAt)
                                    val isFirstInCluster = showDayHeader || !areClusteredMessages(prev, msg)
                                    val isLastInCluster = next == null ||
                                        !isSameCalendarDay(msg.createdAt, next.createdAt) ||
                                        !areClusteredMessages(msg, next)

                                    if (showDayHeader) {
                                        DayDividerPill(label = formatDayHeader(msg.createdAt))
                                    }

                                    MessageBubble(
                                        message = msg,
                                        isOwnMessage = msg.senderId == viewModel.currentUserId,
                                        isGroup = isGroup,
                                        currentUserId = viewModel.currentUserId,
                                        isFirstInCluster = isFirstInCluster,
                                        isLastInCluster = isLastInCluster,
                                        onLongPressOrTapActions = { selectedMessageForActions = msg },
                                        onToggleReaction = { emoji -> viewModel.toggleReaction(msg, emoji) },
                                        onVotePoll = { optionIdx -> viewModel.votePoll(msg, optionIdx) },
                                        onOpenImage = { bmp ->
                                            lightboxState = Triple(bmp, msg.senderName, msg.text)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                JumpToLatestButton(
                    visible = showJumpToBottom,
                    onClick = {
                        scope.launch {
                            if (filteredMessages.isNotEmpty()) {
                                listState.animateScrollToItem(filteredMessages.lastIndex)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                )
            }

            // Reply / Edit Banner
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
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (editingMessage != null) {
                                        "Editing message"
                                    } else {
                                        "Replying to ${replyingTo?.senderName ?: ""}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = bannerTarget?.text?.ifBlank { "Photo / Poll attachment" }.orEmpty(),
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

            // Expandable iPhone Quick Options Tray
            IosQuickInsertTray(
                visible = isTrayOpen,
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
                onInsertQuickReply = { snippet ->
                    draftText = if (draftText.isBlank()) snippet else "$draftText $snippet"
                    isTrayOpen = false
                }
            )

            // Capsule iOS Composer Bar
            IosMessageComposerBar(
                draftText = draftText,
                onDraftChange = { draftText = it },
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
                    if (draftText.trim().isEmpty()) return@IosMessageComposerBar
                    haptics.success()
                    viewModel.sendOrEditText(conv, profile, draftText) {
                        draftText = ""
                        isTrayOpen = false
                    }
                }
            )
        }
    }

    selectedMessageForActions?.let { msg ->
        MessageActionsDialog(
            message = msg,
            isOwnMessage = msg.senderId == viewModel.currentUserId,
            onDismiss = { selectedMessageForActions = null },
            onReact = { emoji -> viewModel.toggleReaction(msg, emoji) },
            onReply = { viewModel.setReplyTarget(msg) },
            onEdit = { viewModel.setEditingTarget(msg) },
            onDelete = { viewModel.deleteMessage(msg) }
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
                haptics.success()
                viewModel.sendPoll(conversation, currentUserProfile, question, options) {}
            }
        )
    }

    if (showGroupInfoDialog && conversation != null) {
        GroupInfoDialog(
            conversation = conversation,
            allPeople = allPeople,
            currentUserId = viewModel.currentUserId,
            onDismiss = { showGroupInfoDialog = false },
            onUpdateGroupInfo = { newTitle, newTopic ->
                viewModel.updateGroupInfo(conversation, newTitle, newTopic)
            },
            onAddMembers = { members ->
                viewModel.addMembers(conversation, members)
            },
            onLeaveGroup = {
                showGroupInfoDialog = false
                viewModel.leaveGroup(conversation, onLeft = onBack)
            },
            onDeleteChannel = {
                showGroupInfoDialog = false
                viewModel.deleteConversation(conversation, onDeleted = onBack)
            }
        )
    }

    if (showDirectInfoDialog && conversation != null) {
        DirectChatInfoDialog(
            conversation = conversation,
            otherProfile = otherProfile ?: currentUserProfile.takeIf { otherId == null || otherId == viewModel.currentUserId },
            currentUserId = viewModel.currentUserId,
            messageCount = messages.size,
            onDismiss = { showDirectInfoDialog = false },
            onDeleteConversation = {
                showDirectInfoDialog = false
                viewModel.deleteConversation(conversation, onDeleted = onBack)
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
