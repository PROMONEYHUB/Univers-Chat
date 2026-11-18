package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CallSession
import com.example.data.model.CallTarget
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.UniversChatsSkeletonLoading
import com.example.ui.components.UserAvatar
import com.example.ui.components.bouncyPressScale
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.PulseBrushes
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueBright
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramSteelBlue
import com.example.ui.viewmodel.ChatFilter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    conversationsState: UiState<List<Conversation>>,
    peopleState: UiState<List<UserProfile>>,
    searchQuery: String,
    selectedFilter: ChatFilter,
    selectedConversationId: String?,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (ChatFilter) -> Unit,
    onOpenConversation: (String) -> Unit,
    onStartDirectChat: (UserProfile) -> Unit,
    onCreateGroupChat: (title: String, topic: String, members: List<UserProfile>) -> Unit,
    onDeleteConversation: (Conversation, onResult: (Boolean) -> Unit) -> Unit = { _, onResult -> onResult(false) },
    onProfileClick: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    blockedUserIds: Set<String> = emptySet(),
    callHistory: List<CallSession> = emptyList(),
    onTogglePinConversation: (Conversation) -> Unit = {},
    onToggleMuteConversation: (Conversation) -> Unit = {},
    onToggleMarkUnread: (Conversation) -> Unit = {},
    onStartCall: (target: CallTarget, isVideoCall: Boolean) -> Unit = { _, _ -> },
    externalNewChatTrigger: Int = 0,
    externalNewGroupTrigger: Int = 0,
    externalCallHistoryTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val cosmicHeaderBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)

    var showNewChatDialog by remember { mutableStateOf(false) }
    var openDialogInGroupMode by remember { mutableStateOf(false) }
    var showCallHistoryDialog by remember { mutableStateOf(false) }
    var optionsTargetConversation by remember { mutableStateOf<Conversation?>(null) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    val pendingDeleteIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(externalNewChatTrigger) {
        if (externalNewChatTrigger > 0) {
            openDialogInGroupMode = false
            showNewChatDialog = true
        }
    }
    LaunchedEffect(externalNewGroupTrigger) {
        if (externalNewGroupTrigger > 0) {
            openDialogInGroupMode = true
            showNewChatDialog = true
        }
    }
    LaunchedEffect(externalCallHistoryTrigger) {
        if (externalCallHistoryTrigger > 0) {
            showCallHistoryDialog = true
        }
    }

    val peopleList = (peopleState as? UiState.Success)?.data.orEmpty()
    val peopleMap = remember(peopleList) { peopleList.associateBy { it.userId } }
    val peopleNameMap = remember(peopleList) { peopleList.associate { it.userId to it.displayName } }
    val allConversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val connectedUserIds = remember(allConversations, currentUserId, blockedUserIds) {
        allConversations
            .flatMap { it.participantIds }
            .filter { it != currentUserId && it !in blockedUserIds }
            .toSet()
    }
    val unreadConvCount = remember(allConversations, currentUserId) {
        allConversations.count { it.unreadCountFor(currentUserId) > 0L }
    }

    var typingClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(allConversations, currentUserId) {
        val hasAnyTyping = allConversations.any { conv -> conv.typingBy.any { it.key != currentUserId } }
        typingClockMs = System.currentTimeMillis()
        while (hasAnyTyping) {
            delay(1500L)
            typingClockMs = System.currentTimeMillis()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(cosmicHeaderBrush)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        if (isSearchExpanded) {
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    isSearchExpanded = false
                                    onSearchQueryChange("")
                                },
                                modifier = Modifier.testTag("chats_search_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Close search"
                                )
                            }
                        } else if (onOpenDrawer != null) {
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    onOpenDrawer()
                                },
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .testTag("drawer_menu_button")
                            ) {
                                UserAvatar(
                                    name = currentUserProfile?.displayName?.ifBlank { "Me" } ?: "Me",
                                    photoUrl = currentUserProfile?.photoUrl,
                                    size = 34.dp,
                                    showPresence = false,
                                    showStoryRing = false
                                )
                            }
                        }
                    },
                    title = {
                        if (isSearchExpanded) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("chats_search_input")
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (conversationsState is UiState.Loading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = if (conversationsState is UiState.Loading) {
                                        "Connecting…"
                                    } else {
                                        stringResource(R.string.app_name)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        if (isSearchExpanded) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptics.lightTap()
                                        onSearchQueryChange("")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        } else {
                            currentUserProfile?.let { me ->
                                IconButton(
                                    onClick = {
                                        haptics.lightTap()
                                        onStartDirectChat(me)
                                    },
                                    modifier = Modifier.testTag("header_saved_messages_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BookmarkBorder,
                                        contentDescription = "Saved Messages"
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    showCallHistoryDialog = true
                                },
                                modifier = Modifier.testTag("header_call_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "Call history"
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    isSearchExpanded = true
                                },
                                modifier = Modifier.testTag("chats_search_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search chats"
                                )
                            }
                        }
                    }
                )

                TelegramFolderTabsBar(
                    selectedFilter = selectedFilter,
                    unreadConvCount = unreadConvCount,
                    onSelectFilter = onSelectFilter
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.lightTap()
                    openDialogInGroupMode = false
                    showNewChatDialog = true
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .bouncyPressScale(0.92f)
                    .size(56.dp)
                    .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = TelegramBlue, spotColor = TelegramBlue)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PulseBrushes.BrandGradient)
                    .testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.new_chat),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (conversationsState) {
                is UiState.Loading -> {
                    UniversChatsSkeletonLoading()
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversationsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is UiState.Success -> {
                    val filtered = remember(
                        conversationsState.data,
                        searchQuery,
                        selectedFilter,
                        currentUserId,
                        pendingDeleteIds.size
                    ) {
                        conversationsState.data.filter { conv ->
                            if (conv.id in pendingDeleteIds) return@filter false
                            val matchesFilter = when (selectedFilter) {
                                ChatFilter.ALL -> true
                                ChatFilter.DIRECT -> conv.type == Conversation.TYPE_DIRECT
                                ChatFilter.GROUPS -> conv.type == Conversation.TYPE_GROUP
                                ChatFilter.UNREAD -> conv.unreadCountFor(currentUserId) > 0L
                            }
                            val title = conv.displayTitleFor(currentUserId)
                            val matchesSearch = searchQuery.isBlank() ||
                                title.contains(searchQuery, ignoreCase = true) ||
                                conv.lastMessageText.contains(searchQuery, ignoreCase = true) ||
                                conv.topic.contains(searchQuery, ignoreCase = true)
                            matchesFilter && matchesSearch
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            item(key = "empty_chats_card") {
                                EmptyChatsView(
                                    isFiltered = searchQuery.isNotBlank() || selectedFilter != ChatFilter.ALL,
                                    onStartDirect = {
                                        openDialogInGroupMode = false
                                        showNewChatDialog = true
                                    },
                                    onCreateGroup = {
                                        openDialogInGroupMode = true
                                        showNewChatDialog = true
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(filtered, key = { _, conv -> conv.id }) { _, conversation ->
                                val otherId = conversation.otherParticipantId(currentUserId)
                                val otherProfile = otherId?.let { peopleMap[it] }
                                val isPendingDelete = conversation.id in pendingDeleteIds
                                val typingSummary = conversation.formatTypingSummary(
                                    currentUserId = currentUserId,
                                    blockedUserIds = blockedUserIds,
                                    peopleNames = peopleNameMap,
                                    nowMillis = typingClockMs
                                )
                                Box(modifier = Modifier.animateItem()) {
                                    SwipeToDeleteConversationItem(
                                        conversation = conversation,
                                        currentUserId = currentUserId,
                                        otherProfile = otherProfile,
                                        isSelected = conversation.id == selectedConversationId,
                                        isPendingDelete = isPendingDelete,
                                        showBottomDivider = false,
                                        typingText = typingSummary,
                                        onOpen = { onOpenConversation(conversation.id) },
                                        onLongPress = { optionsTargetConversation = conversation },
                                        onDeleteOrLeave = {
                                            if (conversation.id !in pendingDeleteIds) {
                                                pendingDeleteIds.add(conversation.id)
                                            }
                                            onDeleteConversation(conversation) { success ->
                                                if (!success) {
                                                    pendingDeleteIds.remove(conversation.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewConversationDialog(
            people = peopleList.filter { it.userId !in blockedUserIds },
            currentUserId = currentUserId,
            connectedUserIds = connectedUserIds,
            initialGroupMode = openDialogInGroupMode,
            onDismiss = { showNewChatDialog = false },
            onStartDirect = { person ->
                showNewChatDialog = false
                onStartDirectChat(person)
            },
            onCreateGroup = { title, topic, members ->
                showNewChatDialog = false
                onCreateGroupChat(title, topic, members)
            }
        )
    }

    if (showCallHistoryDialog) {
        CallHistoryDialog(
            calls = callHistory,
            currentUserId = currentUserId,
            blockedUserIds = blockedUserIds,
            onDismiss = { showCallHistoryDialog = false },
            onCallBack = onStartCall
        )
    }

    optionsTargetConversation?.let { targetConv ->
        ConversationOptionsDialog(
            conversation = targetConv,
            currentUserId = currentUserId,
            onDismiss = { optionsTargetConversation = null },
            onTogglePin = { onTogglePinConversation(targetConv) },
            onToggleMute = { onToggleMuteConversation(targetConv) },
            onToggleMarkUnread = { onToggleMarkUnread(targetConv) },
            onDeleteOrLeave = {
                if (targetConv.id !in pendingDeleteIds) {
                    pendingDeleteIds.add(targetConv.id)
                }
                onDeleteConversation(targetConv) { success ->
                    if (!success) {
                        pendingDeleteIds.remove(targetConv.id)
                    }
                }
            }
        )
    }
}
