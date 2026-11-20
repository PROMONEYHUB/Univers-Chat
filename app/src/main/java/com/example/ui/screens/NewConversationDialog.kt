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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.components.IosSearchField
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.SegmentedTabItem
import com.example.ui.components.UserAvatar
import com.example.ui.components.rememberIosHaptics


@Composable
fun NewConversationDialog(
    people: List<UserProfile>,
    currentUserId: String,
    connectedUserIds: Set<String> = emptySet(),
    initialGroupMode: Boolean = false,
    onDismiss: () -> Unit,
    onStartDirect: (UserProfile) -> Unit,
    onCreateGroup: (title: String, topic: String, members: List<UserProfile>) -> Unit
) {
    val haptics = rememberIosHaptics()
    var isGroupMode by remember { mutableStateOf(initialGroupMode) }
    var groupTitle by remember { mutableStateOf("") }
    var groupTopic by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    val trimmedSearch = search.trim()
    val normalizedSearch = trimmedSearch.removePrefix("@")

    val allOtherPeople = remember(people, currentUserId) {
        people.filter { it.userId != currentUserId }
            .sortedByDescending { it.isOnline }
    }
    val connectedPeople = remember(allOtherPeople, connectedUserIds) {
        allOtherPeople.filter { it.userId in connectedUserIds }
    }
    val filteredPeople = remember(allOtherPeople, connectedPeople, normalizedSearch, connectedUserIds) {
        if (normalizedSearch.length < 2) {
            if (normalizedSearch.isEmpty()) {
                connectedPeople
            } else {
                connectedPeople.filter {
                    it.displayName.contains(normalizedSearch, ignoreCase = true) ||
                        it.handle.contains(normalizedSearch, ignoreCase = true)
                }
            }
        } else {
            allOtherPeople
                .filter {
                    it.handle.contains(normalizedSearch, ignoreCase = true) ||
                        it.displayName.contains(normalizedSearch, ignoreCase = true)
                }
                .sortedWith(
                    compareByDescending<UserProfile> { it.userId in connectedUserIds }
                        .thenByDescending { it.handle.equals(normalizedSearch, ignoreCase = true) }
                        .thenByDescending { it.handle.startsWith(normalizedSearch, ignoreCase = true) }
                        .thenByDescending { it.isOnline }
                )
                .take(12)
        }
    }
    val selectedPeople = remember(allOtherPeople, selectedIds.size, selectedIds.toList()) {
        allOtherPeople.filter { it.userId in selectedIds }
    }

    val modeTabs = remember {
        listOf(
            SegmentedTabItem(value = false, label = "Direct", testTag = "mode_direct_chip"),
            SegmentedTabItem(value = true, label = "Group", testTag = "mode_group_chip")
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 760.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Sheet Grabber
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        )
                    }

                    // Sheet Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                haptics.lightTap()
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = if (isGroupMode) "New Group" else "New Message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (isGroupMode) {
                            Button(
                                onClick = {
                                    haptics.success()
                                    onCreateGroup(groupTitle, groupTopic, selectedPeople)
                                },
                                enabled = groupTitle.trim().isNotEmpty(),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("create_group_confirm_button")
                            ) {
                                Text("Create", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(68.dp))
                        }
                    }

                    // Sliding Mode Switcher
                    IosSegmentedControl(
                        items = modeTabs,
                        selectedValue = isGroupMode,
                        onSelect = { isGroupMode = it },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )

                    // Group Setup Inset Grouped Card
                    AnimatedVisibility(
                        visible = isGroupMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 0.6.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Groups,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (groupTitle.isEmpty()) {
                                                    Text(
                                                        text = "Group name",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                BasicTextField(
                                                    value = groupTitle,
                                                    onValueChange = { groupTitle = it.take(80) },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("group_title_input")
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(
                                                thickness = 0.6.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (groupTopic.isEmpty()) {
                                                    Text(
                                                        text = "Description (optional)",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                BasicTextField(
                                                    value = groupTopic,
                                                    onValueChange = { groupTopic = it.take(160) },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Selected Members Token Strip
                            AnimatedVisibility(visible = selectedPeople.isNotEmpty()) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(selectedPeople, key = { it.userId }) { member ->
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { selectedIds.remove(member.userId) }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(start = 5.dp, end = 9.dp, top = 4.dp, bottom = 4.dp)
                                                ) {
                                                    UserAvatar(
                                                        name = member.displayName,
                                                        photoUrl = member.photoUrl,
                                                        size = 24.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = member.displayName.substringBefore(" "),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Remove ${member.displayName}",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Search Inside Sheet
                    IosSearchField(
                        query = search,
                        onQueryChange = { search = it },
                        placeholder = "Type @username or name to find someone",
                        showCancelWhenActive = false,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )

                    Text(
                        text = when {
                            isGroupMode && selectedIds.isNotEmpty() -> "${selectedIds.size} selected"
                            normalizedSearch.length >= 2 -> "Search results"
                            else -> "Contacts"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    if (filteredPeople.isEmpty()) {
                        Text(
                            text = when {
                                normalizedSearch.length >= 2 ->
                                    "No account found matching \"$trimmedSearch\"."
                                isGroupMode ->
                                    "No contacts yet. Search by @username above to add members."
                                else ->
                                    "No contacts yet. Search by @username or name above to start a chat."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredPeople, key = { it.userId }) { person ->
                                val isSelected = person.userId in selectedIds
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isGroupMode) {
                                                    haptics.selectionTick()
                                                    if (isSelected) selectedIds.remove(person.userId)
                                                    else selectedIds.add(person.userId)
                                                } else {
                                                    haptics.lightTap()
                                                    onStartDirect(person)
                                                }
                                            }
                                            .padding(horizontal = 18.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            name = person.displayName,
                                            photoUrl = person.photoUrl,
                                            size = 44.dp,
                                            showPresence = true,
                                            isOnline = person.isOnline,
                                            showStoryRing = false
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = person.displayName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (person.handle.isNotBlank()) {
                                                Text(
                                                    text = "@${person.handle}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (isGroupMode) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                                tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        thickness = 0.55.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.padding(start = 74.dp, end = 18.dp)
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
