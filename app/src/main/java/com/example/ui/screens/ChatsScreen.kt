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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.IosSearchField
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.SegmentedTabItem
import com.example.ui.components.UserAvatar
import com.example.ui.theme.OnlinePresenceGreen
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
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showNewChatDialog by remember { mutableStateOf(false) }
    var openDialogInGroupMode by remember { mutableStateOf(false) }

    val peopleList = (peopleState as? UiState.Success)?.data.orEmpty()
    val otherPeople = remember(peopleList, currentUserId) {
        peopleList.filter { it.userId != currentUserId }
            .sortedByDescending { it.isOnline }
    }
    val onlineCount = remember(otherPeople) { otherPeople.count { it.isOnline } }
    val peopleMap = remember(peopleList) { peopleList.associateBy { it.userId } }
    val allConversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val unreadConvCount = remember(allConversations, currentUserId) {
        allConversations.count { it.unreadCountFor(currentUserId) > 0L }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    openDialogInGroupMode = false
                    showNewChatDialog = true
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(56.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // iPhone Dynamic Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Messages",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Dynamic Live Island Pill
                        Surface(
                            shape = CircleShape,
                            color = if (onlineCount > 0) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (onlineCount > 0) OnlinePresenceGreen
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (onlineCount > 0) "$onlineCount live" else stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (onlineCount > 0) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    currentUserProfile?.let { me ->
                        Text(
                            text = me.statusVibe.ifBlank { "Available" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Dynamic iOS Glass Action Capsule + Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.border(
                        width = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                openDialogInGroupMode = true
                                showNewChatDialog = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("header_new_group_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GroupAdd,
                                contentDescription = "Create group channel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        currentUserProfile?.let { me ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onProfileClick)
                            ) {
                                UserAvatar(
                                    name = me.displayName,
                                    photoUrl = me.photoUrl,
                                    size = 36.dp,
                                    showPresence = true,
                                    isOnline = me.isOnline,
                                    showStoryRing = me.isOnline
                                )
                            }
                        }
                    }
                }
            }

            // iOS Spotlight Capsule Search Bar
            IosSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search messages & friends",
                inputTestTag = "chats_search_input",
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Active People Story Strip
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

            // Animated Sliding iOS Segmented Control
            IosSegmentedControl(
                items = segmentedItems,
                selectedValue = selectedFilter,
                onSelect = onSelectFilter,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )

            // Conversations Inset Grouped List
            when (conversationsState) {
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
                            text = conversationsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is UiState.Success -> {
                    val filtered = remember(conversationsState.data, searchQuery, selectedFilter, currentUserId) {
                        conversationsState.data.filter { conv ->
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

                    if (filtered.isEmpty()) {
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
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 104.dp)
                        ) {
                            item(key = "grouped_chats_card") {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .border(
                                            width = 0.6.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        filtered.forEachIndexed { index, conversation ->
                                            val otherId = conversation.otherParticipantId(currentUserId)
                                            val otherProfile = otherId?.let { peopleMap[it] }
                                            GroupedConversationRow(
                                                conversation = conversation,
                                                currentUserId = currentUserId,
                                                otherProfile = otherProfile,
                                                isSelected = conversation.id == selectedConversationId,
                                                showBottomDivider = index < filtered.lastIndex,
                                                onClick = { onOpenConversation(conversation.id) }
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
