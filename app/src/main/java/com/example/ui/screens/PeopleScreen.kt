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
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.IosSearchField
import com.example.ui.components.SavedMessagesAvatar
import com.example.ui.components.UniversPeopleSkeletonLoading
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramGreen

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
    hasContactsPermission: Boolean = false,
    isSyncingContacts: Boolean = false,
    matchedContactUserIds: Set<String> = emptySet(),
    localContactNamesByHash: Map<String, String> = emptyMap(),
    onRequestContactsSync: () -> Unit = {},
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsTelegramDark.current
    val allPeople = (peopleState as? UiState.Success)?.data.orEmpty()
    val visibleContactIds = remember(connectedUserIds, matchedContactUserIds) {
        connectedUserIds + matchedContactUserIds
    }
    val matchedPhoneContacts = remember(allPeople, currentUserId, blockedUserIds, matchedContactUserIds) {
        allPeople
            .filter { it.userId != currentUserId && it.userId !in blockedUserIds && it.userId in matchedContactUserIds }
            .sortedWith(compareByDescending<UserProfile> { it.isOnline }.thenBy { it.displayName.lowercase() })
    }
    val connectedContacts = remember(allPeople, currentUserId, blockedUserIds, connectedUserIds, matchedContactUserIds) {
        allPeople
            .filter {
                it.userId != currentUserId &&
                    it.userId !in blockedUserIds &&
                    it.userId in connectedUserIds &&
                    it.userId !in matchedContactUserIds
            }
            .sortedWith(compareByDescending<UserProfile> { it.isOnline }.thenBy { it.displayName.lowercase() })
    }
    val allVisibleContacts = remember(matchedPhoneContacts, connectedContacts) {
        matchedPhoneContacts + connectedContacts
    }
    val contactCount = allVisibleContacts.size

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (showBackButton) 6.dp else 18.dp,
                            end = 16.dp,
                            top = 10.dp,
                            bottom = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("contacts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contacts",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (contactCount > 0) {
                                "$contactCount contact${if (contactCount == 1) "" else "s"}"
                            } else {
                                "Private contacts"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.7f)
                )
            }
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

                    val connectedMatches = remember(allVisibleContacts, normalizedQuery) {
                        if (normalizedQuery.isEmpty()) {
                            allVisibleContacts
                        } else {
                            allVisibleContacts.filter {
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
                        visibleContactIds
                    ) {
                        if (normalizedQuery.length < 2) {
                            emptyList()
                        } else {
                            peopleState.data
                                .filter {
                                    it.userId != currentUserId &&
                                        it.userId !in blockedUserIds &&
                                        it.userId !in visibleContactIds
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

                        if (!hasContactsPermission && trimmedQuery.isEmpty()) {
                            item(key = "sync_contacts_banner") {
                                SyncContactsBannerCard(
                                    isSyncing = isSyncingContacts,
                                    onSyncClick = onRequestContactsSync
                                )
                            }
                        }

                        if (trimmedQuery.isEmpty()) {
                            if (matchedPhoneContacts.isEmpty() && connectedContacts.isEmpty()) {
                                item(key = "empty_private_contacts") {
                                    PrivateContactsEmptyState(
                                        hasContactsPermission = hasContactsPermission,
                                        onRequestContactsSync = onRequestContactsSync
                                    )
                                }
                            } else {
                                if (matchedPhoneContacts.isNotEmpty()) {
                                    item(key = "phone_contacts_header") {
                                        ContactsSectionLabel("CONTACTS ON UNIVERS")
                                    }
                                    items(items = matchedPhoneContacts, key = { "phone_${it.userId}" }) { person ->
                                        val savedName = person.phoneHash?.let { localContactNamesByHash[it] }
                                        UniversContactCard(
                                            profile = person,
                                            phonebookLabel = savedName,
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
                                if (connectedContacts.isNotEmpty()) {
                                    item(key = "your_contacts_header") {
                                        ContactsSectionLabel("YOUR CONTACTS")
                                    }
                                    items(items = connectedContacts, key = { it.userId }) { person ->
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
private fun SyncContactsBannerCard(
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Contacts,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sync Phone Contacts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Find saved contacts & get notified when they join Univers Chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSyncClick,
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("sync_contacts_cta_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text("Sync", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PrivateContactsEmptyState(
    hasContactsPermission: Boolean = false,
    onRequestContactsSync: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 36.dp),
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
            text = if (hasContactsPermission) {
                "Contacts synced securely. We'll notify you as soon as any of your contacts join Univers Chat — or search by @username above."
            } else {
                "Other accounts are hidden by default. Sync your phone contacts or search by exact @username above to start chatting."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!hasContactsPermission) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onRequestContactsSync,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Contacts,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Allow Contacts Access", fontWeight = FontWeight.Bold)
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
    phonebookLabel: String? = null,
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
                        if (!phonebookLabel.isNullOrBlank() &&
                            !phonebookLabel.equals(profile.displayName, ignoreCase = true)
                        ) {
                            append(" · $phonebookLabel")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.isOnline) TelegramGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (profile.isOnline) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val statusMsg = profile.statusVibe.trim()
                val secondaryText = when {
                    statusMsg.isNotBlank() -> statusMsg
                    profile.bio.isNotBlank() -> profile.bio
                    else -> ""
                }
                if (secondaryText.isNotBlank()) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statusMsg.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (statusMsg.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
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
