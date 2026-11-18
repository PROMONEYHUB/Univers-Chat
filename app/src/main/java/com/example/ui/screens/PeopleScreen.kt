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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.UniversPeopleSkeletonLoading
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberCosmicAuraHeaderBrush
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramBlue

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
    connectedUserIds: Set<String> = emptySet(),
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    val cosmicHeaderBrush = rememberCosmicAuraHeaderBrush(isDark = isDark)
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val connectedContacts = remember(allPeople, currentUserId, blockedUserIds, connectedUserIds) {
        allPeople
            .filter { it.userId != currentUserId && it.userId !in blockedUserIds && it.userId in connectedUserIds }
            .sortedWith(compareByDescending<UserProfile> { it.isOnline }.thenBy { it.displayName.lowercase() })
    }
    val contactCount = connectedContacts.size

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
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (contactCount > 0) {
                                "$contactCount contact${if (contactCount == 1) "" else "s"}"
                            } else {
                                "Private contacts"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            IosSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Find by @username or name…",
                inputTestTag = "people_search_input",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )

            when (peopleState) {
                is UiState.Loading -> {
                    UniversPeopleSkeletonLoading()
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
                    val trimmedQuery = searchQuery.trim()
                    val normalizedQuery = trimmedQuery.removePrefix("@")

                    val connectedMatches = remember(connectedContacts, normalizedQuery) {
                        if (normalizedQuery.isEmpty()) {
                            connectedContacts
                        } else {
                            connectedContacts.filter {
                                it.displayName.contains(normalizedQuery, ignoreCase = true) ||
                                    it.handle.contains(normalizedQuery, ignoreCase = true)
                            }
                        }
                    }

                    val directoryMatches = remember(
                        peopleState.data,
                        normalizedQuery,
                        currentUserId,
                        blockedUserIds,
                        connectedUserIds
                    ) {
                        if (normalizedQuery.length < 2) {
                            emptyList()
                        } else {
                            peopleState.data
                                .filter {
                                    it.userId != currentUserId &&
                                        it.userId !in blockedUserIds &&
                                        it.userId !in connectedUserIds
                                }
                                .filter {
                                    it.handle.contains(normalizedQuery, ignoreCase = true) ||
                                        it.displayName.contains(normalizedQuery, ignoreCase = true)
                                }
                                .sortedWith(
                                    compareByDescending<UserProfile> { it.handle.equals(normalizedQuery, ignoreCase = true) }
                                        .thenByDescending { it.handle.startsWith(normalizedQuery, ignoreCase = true) }
                                        .thenByDescending { it.isOnline }
                                        .thenBy { it.displayName.lowercase() }
                                )
                                .take(10)
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (currentUserProfile != null && trimmedQuery.isEmpty()) {
                            item(key = "saved_messages_row") {
                                SavedMessagesContactRow(
                                    profile = currentUserProfile,
                                    onClick = { onMessagePerson(currentUserProfile) }
                                )
                            }
                        }

                        if (trimmedQuery.isEmpty()) {
                            if (connectedMatches.isEmpty()) {
                                item(key = "empty_private_contacts") {
                                    PrivateContactsEmptyState()
                                }
                            } else {
                                item(key = "your_contacts_header") {
                                    ContactsSectionLabel("YOUR CONTACTS")
                                }
                                items(items = connectedMatches, key = { it.userId }) { person ->
                                    UniversContactCard(
                                        profile = person,
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
                        } else if (normalizedQuery.length < 2) {
                            if (connectedMatches.isNotEmpty()) {
                                item(key = "your_contacts_short_header") {
                                    ContactsSectionLabel("YOUR CONTACTS")
                                }
                                items(items = connectedMatches, key = { it.userId }) { person ->
                                    UniversContactCard(
                                        profile = person,
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
                            item(key = "search_min_chars_hint") {
                                Text(
                                    text = "Type at least 2 characters to find new people by @username or name.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 18.dp)
                                )
                            }
                        } else {
                            if (connectedMatches.isNotEmpty()) {
                                item(key = "search_contacts_header") {
                                    ContactsSectionLabel("YOUR CONTACTS")
                                }
                                items(items = connectedMatches, key = { "c_${it.userId}" }) { person ->
                                    UniversContactCard(
                                        profile = person,
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

                            if (directoryMatches.isNotEmpty()) {
                                item(key = "search_directory_header") {
                                    ContactsSectionLabel("FIND ON UNIVERS")
                                }
                                items(items = directoryMatches, key = { "d_${it.userId}" }) { person ->
                                    UniversContactCard(
                                        profile = person,
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

                            if (connectedMatches.isEmpty() && directoryMatches.isEmpty()) {
                                item(key = "empty_search_people") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 28.dp, vertical = 44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(62.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PersonSearch,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No accounts match \"$trimmedQuery\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Check the spelling or ask your friend for their Univers Chat @username.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
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

@Composable
private fun ContactsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

@Composable
private fun PrivateContactsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your contacts are private",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Other accounts are hidden by default. Search by exact @username or name above to find someone and start chatting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SavedMessagesContactRow(
    profile: UserProfile,
    onClick: () -> Unit
) {
    val haptics = rememberIosHaptics()
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable {
                haptics.lightTap()
                onClick()
            }
            .testTag("message_person_${profile.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavedMessagesAvatar(size = 46.dp)
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saved Messages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Private cloud notes, photos & links",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UniversContactCard(
    profile: UserProfile,
    onRowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    val haptics = rememberIosHaptics()
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable {
                haptics.lightTap()
                onRowClick()
            }
            .testTag("contact_profile_${profile.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = profile.displayName,
                photoUrl = profile.photoUrl,
                size = 46.dp,
                showPresence = true,
                isOnline = profile.isOnline,
                showStoryRing = false
            )

            Spacer(modifier = Modifier.width(13.dp))

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
    }
}
