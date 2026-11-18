package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.IosSearchField
import com.example.ui.components.SavedMessagesAvatar
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBar
import com.example.ui.theme.TelegramSteelBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    peopleState: UiState<List<UserProfile>>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMessagePerson: (UserProfile) -> Unit,
    onCallPerson: (person: UserProfile, isVideoCall: Boolean) -> Unit = { _, _ -> },
    onViewProfile: ((UserProfile) -> Unit)? = null,
    blockedUserIds: Set<String> = emptySet(),
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    val topBarColor = if (isDark) TelegramDarkBar else TelegramSteelBlue
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val memberCount = remember(allPeople, currentUserId, blockedUserIds) {
        allPeople.count { it.userId != currentUserId && it.userId !in blockedUserIds }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("contacts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Contacts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (memberCount > 0) {
                            Text(
                                text = "$memberCount member${if (memberCount == 1) "" else "s"} on Univers",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
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
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    IosSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = "Search contacts by name or @username…",
                        inputTestTag = "people_search_input",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            when (peopleState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TelegramBlue)
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peopleState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is UiState.Success -> {
                    val others = remember(peopleState.data, searchQuery, currentUserId, blockedUserIds) {
                        peopleState.data
                            .filter { it.userId != currentUserId && it.userId !in blockedUserIds }
                            .filter {
                                searchQuery.isBlank() ||
                                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                                    it.handle.contains(searchQuery, ignoreCase = true) ||
                                    it.statusVibe.contains(searchQuery, ignoreCase = true)
                            }
                            .sortedByDescending { it.isOnline }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (currentUserProfile != null && searchQuery.isBlank()) {
                            item(key = "saved_messages_row") {
                                SavedMessagesContactRow(
                                    profile = currentUserProfile,
                                    onClick = { onMessagePerson(currentUserProfile) }
                                )
                                HorizontalDivider(
                                    thickness = 8.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        }

                        if (others.isEmpty()) {
                            item(key = "empty_people") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 28.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.PersonSearch,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "No contacts match \"$searchQuery\""
                                        } else {
                                            "No contacts online yet"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Contacts appear automatically once other Univers Chat users sign in with their phone number or Google.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            item(key = "contacts_section_header") {
                                Text(
                                    text = "Sorted by online status",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }

                            itemsIndexed(
                                items = others,
                                key = { _, person -> person.userId }
                            ) { index, person ->
                                TelegramContactRow(
                                    profile = person,
                                    showBottomDivider = index < others.lastIndex,
                                    onRowClick = {
                                        if (onViewProfile != null) onViewProfile(person)
                                        else onMessagePerson(person)
                                    },
                                    onMessageClick = { onMessagePerson(person) },
                                    onVoiceCallClick = { onCallPerson(person, false) },
                                    onVideoCallClick = { onCallPerson(person, true) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedMessagesContactRow(
    profile: UserProfile,
    onClick: () -> Unit
) {
    val haptics = rememberIosHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                haptics.lightTap()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("message_person_${profile.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SavedMessagesAvatar(size = 48.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Saved Messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Cloud storage for quick notes, photos & links",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TelegramContactRow(
    profile: UserProfile,
    showBottomDivider: Boolean,
    onRowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    val haptics = rememberIosHaptics()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                haptics.lightTap()
                onRowClick()
            }
            .testTag("contact_profile_${profile.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = profile.displayName,
                photoUrl = profile.photoUrl,
                size = 48.dp,
                showPresence = true,
                isOnline = profile.isOnline,
                showStoryRing = false
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val statusText = when {
                    profile.isOnline -> "online"
                    profile.lastSeenAt != null -> "last seen ${formatRelativeTime(profile.lastSeenAt)}"
                    profile.handle.isNotBlank() -> "@${profile.handle}"
                    else -> "last seen recently"
                }
                Text(
                    text = buildString {
                        append(statusText)
                        if (profile.handle.isNotBlank() && (profile.isOnline || profile.lastSeenAt != null)) {
                            append(" · @${profile.handle}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.isOnline) TelegramBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (profile.isOnline) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onVoiceCallClick()
                    },
                    modifier = Modifier.testTag("voice_call_person_${profile.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Voice call ${profile.displayName}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onVideoCallClick()
                    },
                    modifier = Modifier.testTag("video_call_person_${profile.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = "Video call ${profile.displayName}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onMessageClick()
                    },
                    modifier = Modifier.testTag("message_person_${profile.userId}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Chat with ${profile.displayName}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showBottomDivider) {
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 78.dp)
            )
        }
    }
}
