package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.BadgeCoral
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.LocalUniversAccent
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
    onOpenNotifications: (() -> Unit)? = null,
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
    val accent = LocalUniversAccent.current
    val accentPrimary = accent.primary(isDark)

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
    val activeStoryPeople = remember(peopleList, connectedUserIds, currentUserId, blockedUserIds) {
        peopleList
            .filter { it.userId != currentUserId && it.userId !in blockedUserIds && it.userId in connectedUserIds }
            .sortedWith(compareByDescending<UserProfile> { it.isOnline }.thenBy { it.displayName.lowercase() })
            .take(12)
    }
    val unreadConvCount = remember(allConversations, currentUserId) {
        allConversations.count { it.unreadCountFor(currentUserId) > 0L }
    }
    val hasMissedCallAlert = remember(callHistory, currentUserId) {
        callHistory.take(5).any {
            it.callerId != currentUserId && (it.status == CallSession.STATUS_MISSED || it.status == CallSession.STATUS_DECLINED)
        }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                if (isSearchExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.72f else 0.82f),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptics.lightTap()
                                        isSearchExpanded = false
                                        onSearchQueryChange("")
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("chats_search_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Close search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search chats, people, messages…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(accentPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chats_search_input")
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            onSearchQueryChange("")
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onOpenDrawer != null) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptics.lightTap()
                                        onOpenDrawer()
                                    }
                                    .testTag("drawer_menu_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                UserAvatar(
                                    name = currentUserProfile?.displayName?.ifBlank { "Me" } ?: "Me",
                                    photoUrl = currentUserProfile?.photoUrl,
                                    size = 38.dp,
                                    showPresence = true,
                                    isOnline = currentUserProfile?.isOnline != false,
                                    showStoryRing = false
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (conversationsState is UiState.Loading) {
                                CircularProgressIndicator(
                                    color = accentPrimary,
                                    strokeWidth = 2.2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (conversationsState is UiState.Loading) {
                                    "Connecting…"
                                } else {
                                    "Chats"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenNotifications != null) {
                                HeaderCircleIconButton(
                                    icon = Icons.Rounded.NotificationsNone,
                                    contentDescription = "Notifications center",
                                    showBadgeDot = unreadConvCount > 0 || hasMissedCallAlert,
                                    badgeColor = BadgeCoral,
                                    testTag = "header_notifications_button",
                                    onClick = {
                                        haptics.lightTap()
                                        onOpenNotifications()
                                    }
                                )
                            } else {
                                currentUserProfile?.let { me ->
                                    HeaderCircleIconButton(
                                        icon = Icons.Rounded.BookmarkBorder,
                                        contentDescription = "Saved Messages",
                                        testTag = "header_saved_messages_button",
                                        onClick = {
                                            haptics.lightTap()
                                            onStartDirectChat(me)
                                        }
                                    )
                                }
                            }

                            HeaderCircleIconButton(
                                icon = Icons.Rounded.History,
                                contentDescription = "Call history",
                                testTag = "header_call_history_button",
                                onClick = {
                                    haptics.lightTap()
                                    showCallHistoryDialog = true
                                }
                            )

                            HeaderCircleIconButton(
                                icon = Icons.Rounded.Search,
                                contentDescription = "Search chats",
                                testTag = "chats_search_toggle_button",
                                onClick = {
                                    haptics.lightTap()
                                    isSearchExpanded = true
                                }
                            )
                        }
                    }
                }

                TelegramFolderTabsBar(
                    selectedFilter = selectedFilter,
                    unreadConvCount = unreadConvCount,
                    onSelectFilter = onSelectFilter
                )

                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.7f)
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
                    .size(58.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = accentPrimary,
                        spotColor = accentPrimary
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.brandGradient(isDark))
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

                    val showActiveStrip = !isSearchExpanded &&
                        searchQuery.isBlank() &&
                        selectedFilter == ChatFilter.ALL &&
                        activeStoryPeople.isNotEmpty()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 100.dp)
                    ) {
                        if (showActiveStrip) {
                            item(key = "active_stories_strip") {
                                TelegramStoriesRow(
                                    people = activeStoryPeople,
                                    onPersonClick = onStartDirectChat
                                )
                            }
                        }

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
                            itemsIndexed(filtered, key = { _, conv -> conv.id }) { idx, conversation ->
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
                                        showBottomDivider = idx < filtered.lastIndex,
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

@Composable
private fun HeaderCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    showBadgeDot: Boolean = false,
    badgeColor: Color = BadgeCoral,
    onClick: () -> Unit
) {
    val isDark = LocalIsTelegramDark.current
    Box(
        modifier = Modifier
            .size(42.dp)
            .bouncyPressScale(0.92f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.72f else 0.78f))
            .border(
                width = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.7f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp)
        )
        if (showBadgeDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}
