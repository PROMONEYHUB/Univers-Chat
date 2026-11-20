package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Immutable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@Immutable
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val readReceiptsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val blockedUserIds: List<String> = emptyList(),
    val phoneHash: String = "",
    val contactHashes: List<String> = emptyList(),
    val fcmTokens: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@Immutable
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val statusVibe: String = "",
    val phoneHash: String = "",
    val e2eeIdentityKey: String = "",
    @get:PropertyName("isOnline") @field:PropertyName("isOnline")
    val isOnline: Boolean = true,
    @get:PropertyName("isDiscoverable") @field:PropertyName("isDiscoverable")
    val isDiscoverable: Boolean = true,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@Immutable
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
    val pinnedBy: Map<String, Boolean> = emptyMap(),
    val mutedBy: Map<String, Boolean> = emptyMap(),
    val pinnedMessageId: String? = null,
    val pinnedMessagePreview: String? = null,
    val pinnedMessageSenderName: String? = null,
    val typingBy: Map<String, Timestamp> = emptyMap(),
    val lastReadAt: Map<String, Timestamp> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayTitleFor(currentUserId: String): String {
        if (type == TYPE_GROUP) return title.ifBlank { "Group" }
        val otherId = participantIds.firstOrNull { it != currentUserId }
        return if (otherId != null) {
            participantNames[otherId]?.takeIf { it.isNotBlank() } ?: "Direct Chat"
        } else {
            "Saved Messages"
        }
    }

    fun otherParticipantId(currentUserId: String): String? {
        return if (type == TYPE_DIRECT) participantIds.firstOrNull { it != currentUserId } else null
    }

    fun unreadCountFor(userId: String): Long {
        return unreadCounts[userId] ?: 0L
    }

    fun isPinnedBy(userId: String): Boolean = pinnedBy[userId] == true

    fun isMutedBy(userId: String): Boolean = mutedBy[userId] == true

    fun isMessageReadByOthers(message: Message, currentUserId: String): Boolean {
        if (message.senderId != currentUserId) return false
        val msgMillis = message.createdAt?.toDate()?.time ?: return false
        val otherIds = participantIds.filter { it != currentUserId }
        if (otherIds.isEmpty()) return true
        return otherIds.any { peerId ->
            val peerReadMillis = lastReadAt[peerId]?.toDate()?.time ?: 0L
            peerReadMillis >= msgMillis
        }
    }

    fun activeTypingUserIds(
        currentUserId: String,
        blockedUserIds: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
        staleAfterMillis: Long = 10_000L
    ): List<String> {
        return typingBy.entries
            .asSequence()
            .filter { (uid, ts) ->
                uid != currentUserId &&
                    uid !in blockedUserIds &&
                    uid in participantIds &&
                    (nowMillis - ts.toDate().time) <= staleAfterMillis
            }
            .sortedByDescending { it.value.toDate().time }
            .map { it.key }
            .toList()
    }

    fun formatTypingSummary(
        currentUserId: String,
        blockedUserIds: Set<String> = emptySet(),
        peopleNames: Map<String, String> = emptyMap(),
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        val activeIds = activeTypingUserIds(currentUserId, blockedUserIds, nowMillis)
        if (activeIds.isEmpty()) return null
        if (type == TYPE_DIRECT) return "typing…"
        val names = activeIds.map { uid ->
            (peopleNames[uid] ?: participantNames[uid] ?: "Someone")
                .substringBefore(" ")
                .ifBlank { "Someone" }
        }
        return when (names.size) {
            1 -> "${names[0]} is typing…"
            2 -> "${names[0]} and ${names[1]} are typing…"
            else -> "${names[0]}, ${names[1]} +${names.size - 2} typing…"
        }
    }

    companion object {
        const val TYPE_DIRECT = "direct"
        const val TYPE_GROUP = "group"
    }
}

@Keep
@Immutable
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
    val audioUrl: String? = null,
    val audioBase64: String? = null,
    val audioDurationSec: Int = 0,
    @get:PropertyName("isForwarded") @field:PropertyName("isForwarded")
    val isForwarded: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val pollQuestion: String? = null,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<String, String> = emptyMap(),
    @get:PropertyName("isEdited") @field:PropertyName("isEdited")
    val isEdited: Boolean = false,
    @get:PropertyName("isEncrypted") @field:PropertyName("isEncrypted")
    val isEncrypted: Boolean = false,
    val e2eeVersion: Int = 0,
    val ephemeralPublicKey: String? = null,
    val encryptionIv: String? = null,
    val senderIdentityKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun reactionSummaries(
        currentUserId: String,
        preferredOrder: List<String> = DEFAULT_TAPBACK_ORDER
    ): List<ReactionSummary> {
        if (reactions.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<String, MutableList<String>>()
        reactions.entries
            .sortedBy { it.key }
            .forEach { (uid, emoji) ->
                val clean = emoji.trim()
                if (clean.isNotEmpty()) {
                    grouped.getOrPut(clean) { mutableListOf() }.add(uid)
                }
            }
        return grouped.entries
            .map { (emoji, uids) ->
                ReactionSummary(
                    emoji = emoji,
                    count = uids.size,
                    userIds = uids,
                    reactedByCurrentUser = currentUserId in uids
                )
            }
            .sortedWith(
                compareByDescending<ReactionSummary> { it.count }
                    .thenBy {
                        val idx = preferredOrder.indexOf(it.emoji)
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }
                    .thenBy { it.emoji }
            )
    }

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_POLL = "poll"
        const val TYPE_AUDIO = "audio"
        val DEFAULT_TAPBACK_ORDER = listOf("❤️", "👍", "👎", "😂", "😮", "😢", "🔥", "🎉")
    }
}

@Immutable
data class ReactionSummary(
    val emoji: String,
    val count: Int,
    val userIds: List<String>,
    val reactedByCurrentUser: Boolean
)

fun buildDirectConversationId(uidA: String, uidB: String): String {
    val sorted = listOf(uidA, uidB).sorted()
    return "dm_${sorted[0]}_${sorted[1]}"
}
