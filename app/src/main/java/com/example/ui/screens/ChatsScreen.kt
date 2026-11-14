package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.IosSearchField
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.SegmentedTabItem
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.PulseBrushes
import com.example.ui.viewmodel.ChatFilter

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
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    var showNewChatDialog by remember { mutableStateOf(false) }
    var openDialogInGroupMode by remember { mutableStateOf(false) }
    val pendingDeleteIds = remember { mutableStateListOf<String>() }

    val peopleList = (peopleState as? UiState.Success)?.data.orEmpty()
    val otherPeople = remember(peopleList, currentUserId) {
        peopleList.filter { it.userId != currentUserId }
            .sortedByDescending { it.isOnline }
    }
    val onlinePeople = remember(otherPeople) { otherPeople.filter { it.isOnline } }
    val peopleMap = remember(peopleList) { peopleList.associateBy { it.userId } }
    val allConversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val unreadConvCount = remember(allConversations, currentUserId) {
        allConversations.count { it.unreadCountFor(currentUserId) > 0L }
    }

    val spotlightThreads = remember(allConversations, currentUserId) {
        allConversations
            .sortedWith(
                compareByDescending<Conversation> { it.unreadCountFor(currentUserId) > 0L }
                    .thenByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
            )
            .take(3)
    }

    val segmentedItems = remember(unreadConvCount) {
        ChatFilter.entries.map { filter ->
            SegmentedTabItem(
                value = filter,
                label = filter.label,
                badgeCount = if (filter == ChatFilter.UNREAD) unreadConvCount else 0,
                testTag = "filter_chip_${filter.name.lowercase()}"
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LiquidGlassBackdrop()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            haptics.lightTap()
                            openDialogInGroupMode = false
                            showNewChatDialog = true
                        }
                        .testTag("new_chat_fab")
                ) {
                    Row(
                        modifier = Modifier
                            .background(PulseBrushes.BrandGradient)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = stringResource(R.string.new_chat),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Chat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                ChatsHeaderIsland(
                    currentUserProfile = currentUserProfile,
                    onlinePeople = onlinePeople,
                    unreadConvCount = unreadConvCount,
                    onNewGroupClick = {
                        openDialogInGroupMode = true
                        showNewChatDialog = true
                    },
                    onPersonClick = onStartDirectChat,
                    onProfileClick = onProfileClick
                )

                IosSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search chats, people & topics",
                    inputTestTag = "chats_search_input",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )

                when (conversationsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassmorphismCard(
                                contentPadding = PaddingValues(20.dp),
                                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = conversationsState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
                            contentPadding = PaddingValues(top = 6.dp, bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "active_orbit_strip") {
                                AnimatedVisibility(
                                    visible = otherPeople.isNotEmpty() && searchQuery.isBlank(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    ActivePeopleStoryStrip(
                                        people = otherPeople,
                                        onNewGroupClick = {
                                            openDialogInGroupMode = true
                                            showNewChatDialog = true
                                        },
                                        onPersonClick = onStartDirectChat
                                    )
                                }
                            }

                            if (searchQuery.isBlank() &&
                                selectedFilter == ChatFilter.ALL &&
                                spotlightThreads.size >= 2
                            ) {
                                item(key = "spotlight_carousel") {
                                    SpotlightCarousel(
                                        conversations = spotlightThreads,
                                        currentUserId = currentUserId,
                                        peopleMap = peopleMap,
                                        onOpenConversation = onOpenConversation
                                    )
                                }
                            }

                            item(key = "filter_segmented_bar") {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    IosSegmentedControl(
                                        items = segmentedItems,
                                        selectedValue = selectedFilter,
                                        onSelect = onSelectFilter,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when {
                                                searchQuery.isNotBlank() -> "SEARCH RESULTS"
                                                selectedFilter == ChatFilter.ALL -> "ALL CONVERSATIONS"
                                                else -> selectedFilter.label.uppercase()
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${filtered.size} ${if (filtered.size == 1) "thread" else "threads"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                                items(filtered, key = { it.id }) { conversation ->
                                    val otherId = conversation.otherParticipantId(currentUserId)
                                    val otherProfile = otherId?.let { peopleMap[it] }
                                    val isPendingDelete = conversation.id in pendingDeleteIds
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 18.dp)
                                            .animateItem()
                                    ) {
                                        SwipeToDeleteConversationItem(
                                            conversation = conversation,
                                            currentUserId = currentUserId,
                                            otherProfile = otherProfile,
                                            isSelected = conversation.id == selectedConversationId,
                                            isPendingDelete = isPendingDelete,
                                            onOpen = { onOpenConversation(conversation.id) },
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
    }

    if (showNewChatDialog) {
        NewConversationDialog(
            people = peopleList,
            currentUserId = currentUserId,
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
}
