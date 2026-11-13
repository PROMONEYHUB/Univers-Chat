package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val readReceiptsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val statusVibe: String = "Available",
    val isOnline: Boolean = true,
    val isDiscoverable: Boolean = true,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = TYPE_DIRECT,
    val title: String = "",
    val topic: String = "",
    val createdBy: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageSenderName: String = "",
    val lastMessageType: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayTitleFor(currentUserId: String): String {
        if (type == TYPE_GROUP) return title.ifBlank { "Group Chat" }
        val otherId = participantIds.firstOrNull { it != currentUserId }
        return if (otherId != null) {
            participantNames[otherId]?.takeIf { it.isNotBlank() } ?: "Direct Chat"
        } else {
            participantNames[currentUserId]?.takeIf { it.isNotBlank() } ?: "Saved Notes"
        }
    }

    fun otherParticipantId(currentUserId: String): String? {
        return if (type == TYPE_DIRECT) participantIds.firstOrNull { it != currentUserId } else null
    }

    fun unreadCountFor(userId: String): Long {
        return unreadCounts[userId] ?: 0L
    }

    companion object {
        const val TYPE_DIRECT = "direct"
        const val TYPE_GROUP = "group"
    }
}

@IgnoreExtraProperties
data class Message(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val type: String = TYPE_TEXT,
    val text: String = "",
    val imageBase64: String? = null,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val pollQuestion: String? = null,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<String, String> = emptyMap(),
    val isEdited: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_POLL = "poll"
    }
}

fun buildDirectConversationId(uidA: String, uidB: String): String {
    val sorted = listOf(uidA, uidB).sorted()
    return "dm_${sorted[0]}_${sorted[1]}"
}
