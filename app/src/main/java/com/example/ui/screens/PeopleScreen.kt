package com.example.ui.screens

import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.IosSearchField
import com.example.ui.components.UniversMotion
import com.example.ui.components.UniversPeopleSkeletonLoading
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.rememberIosHaptics
import com.example.ui.theme.LocalIsTelegramDark
import com.example.ui.theme.TelegramGreen

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
                        if (contactCount > 0) {
                            Text(
                                text = "$contactCount contact${if (contactCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                placeholder = "Search by @username or name",
                inputTestTag = "people_search_input",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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

                    val hasAnyContacts = matchedPhoneContacts.isNotEmpty() || connectedContacts.isNotEmpty()
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 2.dp, bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Show subtle inline sync strip ONLY when contacts exist (avoids duplicating with empty state)
                        if (!hasContactsPermission && trimmedQuery.isEmpty() && hasAnyContacts) {
                            item(key = "sync_contacts_banner", contentType = "sync_banner") {
                                SyncContactsInlineRow(
                                    isSyncing = isSyncingContacts,
                                    onSyncClick = onRequestContactsSync
                                )
                            }
                        }

                        if (trimmedQuery.isEmpty()) {
                            if (!hasAnyContacts) {
                                item(key = "empty_private_contacts", contentType = "empty_state") {
                                    PrivateContactsEmptyState(
                                        hasContactsPermission = hasContactsPermission,
                                        isSyncing = isSyncingContacts,
                                        onRequestContactsSync = onRequestContactsSync
                                    )
                                }
                            } else {
                                if (matchedPhoneContacts.isNotEmpty()) {
                                    item(key = "phone_contacts_header", contentType = "section_header") {
                                        ContactsSectionLabel("On Univers Chat")
                                    }
                                    itemsIndexed(
                                        items = matchedPhoneContacts,
                                        key = { _, item -> "phone_${item.userId}" },
                                        contentType = { _, _ -> "contact_row" }
                                    ) { idx, person ->
                                        val savedName = person.phoneHash.takeIf { it.isNotBlank() }?.let { localContactNamesByHash[it] }
                                        ContactListRow(
                                            profile = person,
                                            phonebookLabel = savedName,
                                            showDivider = idx < matchedPhoneContacts.lastIndex,
                                            onRowClick = {
                                                if (onViewProfile != null) onViewProfile(person)
                                                else onMessagePerson(person)
                                            },
                                            onMessageClick = { onMessagePerson(person) },
                                            onVoiceCallClick = { onCallPerson(person, false) },
                                            onVideoCallClick = { onCallPerson(person, true) },
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                                placementSpec = UniversMotion.ListPlacementSpring
                                            )
                                        )
                                    }
                                }
                                if (connectedContacts.isNotEmpty()) {
                                    item(key = "your_contacts_header", contentType = "section_header") {
                                        ContactsSectionLabel("Contacts")
                                    }
                                    itemsIndexed(
                                        items = connectedContacts,
                                        key = { _, item -> item.userId },
                                        contentType = { _, _ -> "contact_row" }
                                    ) { idx, person ->
                                        ContactListRow(
                                            profile = person,
                                            showDivider = idx < connectedContacts.lastIndex,
                                            onRowClick = {
                                                if (onViewProfile != null) onViewProfile(person)
                                                else onMessagePerson(person)
                                            },
                                            onMessageClick = { onMessagePerson(person) },
                                            onVoiceCallClick = { onCallPerson(person, false) },
                                            onVideoCallClick = { onCallPerson(person, true) },
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                                placementSpec = UniversMotion.ListPlacementSpring
                                            )
                                        )
                                    }
                                }
                            }
                        } else if (normalizedQuery.length < 2) {
                            if (connectedMatches.isNotEmpty()) {
                                item(key = "your_contacts_short_header", contentType = "section_header") {
                                    ContactsSectionLabel("Contacts")
                                }
                                itemsIndexed(
                                    items = connectedMatches,
                                    key = { _, item -> item.userId },
                                    contentType = { _, _ -> "contact_row" }
                                ) { idx, person ->
                                    ContactListRow(
                                        profile = person,
                                        showDivider = idx < connectedMatches.lastIndex,
                                        onRowClick = {
                                            if (onViewProfile != null) onViewProfile(person)
                                            else onMessagePerson(person)
                                        },
                                        onMessageClick = { onMessagePerson(person) },
                                        onVoiceCallClick = { onCallPerson(person, false) },
                                        onVideoCallClick = { onCallPerson(person, true) },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                            placementSpec = UniversMotion.ListPlacementSpring
                                        )
                                    )
                                }
                            }
                            item(key = "search_min_chars_hint", contentType = "hint") {
                                Text(
                                    text = "Enter at least 2 characters to search by @username or name.",
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
                                item(key = "search_contacts_header", contentType = "section_header") {
                                    ContactsSectionLabel("Contacts")
                                }
                                itemsIndexed(
                                    items = connectedMatches,
                                    key = { _, item -> "c_${item.userId}" },
                                    contentType = { _, _ -> "contact_row" }
                                ) { idx, person ->
                                    ContactListRow(
                                        profile = person,
                                        showDivider = idx < connectedMatches.lastIndex,
                                        onRowClick = {
                                            if (onViewProfile != null) onViewProfile(person)
                                            else onMessagePerson(person)
                                        },
                                        onMessageClick = { onMessagePerson(person) },
                                        onVoiceCallClick = { onCallPerson(person, false) },
                                        onVideoCallClick = { onCallPerson(person, true) },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                            placementSpec = UniversMotion.ListPlacementSpring
                                        )
                                    )
                                }
                            }

                            if (directoryMatches.isNotEmpty()) {
                                item(key = "search_directory_header", contentType = "section_header") {
                                    ContactsSectionLabel("Global search")
                                }
                                itemsIndexed(
                                    items = directoryMatches,
                                    key = { _, item -> "d_${item.userId}" },
                                    contentType = { _, _ -> "contact_row" }
                                ) { idx, person ->
                                    ContactListRow(
                                        profile = person,
                                        showDivider = idx < directoryMatches.lastIndex,
                                        onRowClick = {
                                            if (onViewProfile != null) onViewProfile(person)
                                            else onMessagePerson(person)
                                        },
                                        onMessageClick = { onMessagePerson(person) },
                                        onVoiceCallClick = { onCallPerson(person, false) },
                                        onVideoCallClick = { onCallPerson(person, true) },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(180, easing = UniversMotion.FluidOutEasing),
                                            placementSpec = UniversMotion.ListPlacementSpring
                                        )
                                    )
                                }
                            }

                            if (connectedMatches.isEmpty() && directoryMatches.isEmpty()) {
                                item(key = "empty_search_people", contentType = "empty_state") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 28.dp, vertical = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PersonSearch,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No results for \"$trimmedQuery\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Check the spelling or search by exact @username.",
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
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SyncContactsInlineRow(
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isSyncing, onClick = onSyncClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("sync_contacts_cta_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sync phone contacts",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "See who you know on Univers Chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSyncing) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                TextButton(onClick = onSyncClick) {
                    Text(
                        text = "Sync",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun PrivateContactsEmptyState(
    hasContactsPermission: Boolean = false,
    isSyncing: Boolean = false,
    onRequestContactsSync: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Contacts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasContactsPermission) "No contacts on Univers Chat yet" else "Find your contacts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasContactsPermission) {
                "Contacts are synced. Search by @username above to message anyone directly."
            } else {
                "Sync your phone contacts to see who is already here, or search by @username above."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!hasContactsPermission) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestContactsSync,
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("sync_contacts_cta_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.background,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Sync contacts", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ContactListRow(
    profile: UserProfile,
    phonebookLabel: String? = null,
    showDivider: Boolean = true,
    onRowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberIosHaptics()
    val isDark = LocalIsTelegramDark.current
    val subtitleLine = remember(profile.isOnline, profile.lastSeenAt, profile.handle, phonebookLabel, profile.displayName) {
        val statusText = when {
            profile.isOnline -> "online"
            profile.lastSeenAt != null -> "last seen ${formatRelativeTime(profile.lastSeenAt)}"
            profile.handle.isNotBlank() -> "@${profile.handle}"
            else -> "offline"
        }
        buildString {
            append(statusText)
            if (profile.handle.isNotBlank() && (profile.isOnline || profile.lastSeenAt != null)) {
                append(" · @${profile.handle}")
            }
            if (!phonebookLabel.isNullOrBlank() &&
                !phonebookLabel.equals(profile.displayName, ignoreCase = true)
            ) {
                append(" · $phonebookLabel")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptics.lightTap()
                onRowClick()
            }
            .testTag("contact_profile_${profile.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (profile.isOnline) TelegramGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onVoiceCallClick()
                    },
                    modifier = Modifier.testTag("voice_call_person_${profile.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Voice call ${profile.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Video call ${profile.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "Message ${profile.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = 0.55.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.72f),
                modifier = Modifier.padding(start = 76.dp, end = 16.dp)
            )
        }
    }
}
