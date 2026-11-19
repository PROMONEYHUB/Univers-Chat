package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.R
import com.example.contacts.DeviceContactsManager
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.data.model.buildDirectConversationId
import com.example.data.repository.MessagingRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class InAppMessageBanner(
    val conversationId: String,
    val title: String,
    val senderName: String,
    val senderPhotoUrl: String?,
    val previewText: String,
    val isGroup: Boolean,
    val timestampMillis: Long
)

object MessageNotificationCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null
    private var observedUserId: String? = null

    @Volatile
    private var notificationsEnabled: Boolean = true

    @Volatile
    private var blockedUserIds: Set<String> = emptySet()

    @Volatile
    private var appInForeground: Boolean = false

    @Volatile
    private var activeConversationId: String? = null

    @Volatile
    private var contactHashes: Set<String> = emptySet()

    @Volatile
    private var localContactNamesByHash: Map<String, String> = emptyMap()

    // Tracks last processed message key per conversation so initial snapshot & updates never duplicate alerts
    private val lastSeenMessageTimestamps = ConcurrentHashMap<String, Long>()
    private val lastNotifiedEventKeys = ConcurrentHashMap<String, String>()
    private var hasSeededInitialConversations = false

    private val _inAppBanners = MutableSharedFlow<InAppMessageBanner>(extraBufferCapacity = 8)
    val inAppBanners: SharedFlow<InAppMessageBanner> = _inAppBanners.asSharedFlow()

    private val _pendingDeepLinkConversationId = MutableStateFlow<String?>(null)
    val pendingDeepLinkConversationId: StateFlow<String?> = _pendingDeepLinkConversationId.asStateFlow()

    private val _pendingDeepLinkUserId = MutableStateFlow<String?>(null)
    val pendingDeepLinkUserId: StateFlow<String?> = _pendingDeepLinkUserId.asStateFlow()

    fun setAppForegroundState(inForeground: Boolean) {
        appInForeground = inForeground
    }

    fun setActiveConversationId(conversationId: String?) {
        activeConversationId = conversationId
    }

    fun emitDeepLinkConversation(conversationId: String?) {
        if (!conversationId.isNullOrBlank()) {
            _pendingDeepLinkConversationId.value = conversationId
        }
    }

    fun consumeDeepLinkConversation() {
        _pendingDeepLinkConversationId.value = null
    }

    fun emitDeepLinkUser(userId: String?) {
        if (!userId.isNullOrBlank()) {
            _pendingDeepLinkUserId.value = userId
        }
    }

    fun consumeDeepLinkUser() {
        _pendingDeepLinkUserId.value = null
    }

    fun updateLocalContactDirectory(hashToName: Map<String, String>) {
        localContactNamesByHash = hashToName
        if (hashToName.isNotEmpty()) {
            contactHashes = hashToName.keys
        }
    }

    fun startObservingForUser(context: Context, userId: String) {
        val appContext = context.applicationContext
        if (observedUserId == userId && observerJob?.isActive == true) return

        stopObserving()
        observedUserId = userId
        hasSeededInitialConversations = false
        lastSeenMessageTimestamps.clear()
        lastNotifiedEventKeys.clear()

        val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
        val repository = MessagingRepository(db, Firebase.auth)

        observerJob = scope.launch {
            launch {
                repository.observePrivateUser(userId)
                    .catch { err -> Log.w(TAG, "Private user preference listener error", err) }
                    .collect { privateUser ->
                        if (privateUser != null) {
                            notificationsEnabled = privateUser.notificationsEnabled
                            blockedUserIds = privateUser.blockedUserIds.toSet()
                            if (privateUser.contactHashes.isNotEmpty()) {
                                contactHashes = privateUser.contactHashes.toSet()
                            }
                        }
                    }
            }

            launch {
                repository.observeConversations(userId)
                    .catch { err -> Log.w(TAG, "Realtime conversation notification error", err) }
                    .collect { conversations ->
                        handleConversationSnapshot(appContext, userId, conversations)
                    }
            }

            launch {
                repository.observeDiscoverableProfiles()
                    .catch { err -> Log.w(TAG, "Contact-joined profile listener error", err) }
                    .collect { profiles ->
                        handleContactJoinedSnapshot(appContext, userId, profiles)
                    }
            }
        }
    }

    fun stopObserving() {
        observerJob?.cancel()
        observerJob = null
        observedUserId = null
        hasSeededInitialConversations = false
        lastSeenMessageTimestamps.clear()
        lastNotifiedEventKeys.clear()
    }

    private fun handleContactJoinedSnapshot(
        context: Context,
        currentUserId: String,
        profiles: List<UserProfile>
    ) {
        val hashes = contactHashes
        if (hashes.isEmpty()) return

        val matched = profiles.filter { profile ->
            profile.userId != currentUserId &&
                profile.userId !in blockedUserIds &&
                profile.phoneHash.isNotBlank() &&
                profile.phoneHash in hashes
        }
        val currentMatchedIds = matched.map { it.userId }.toSet()
        val previouslyKnown = DeviceContactsManager.getKnownJoinedContactIds(context, currentUserId)

        if (previouslyKnown == null) {
            // Seed existing matched contacts on first sync so only future joins fire notifications
            DeviceContactsManager.saveKnownJoinedContactIds(context, currentUserId, currentMatchedIds)
            return
        }

        val newlyJoined = matched.filter { it.userId !in previouslyKnown }
        if (newlyJoined.isEmpty()) return

        DeviceContactsManager.saveKnownJoinedContactIds(
            context,
            currentUserId,
            previouslyKnown + currentMatchedIds
        )

        if (!notificationsEnabled) return

        for (contact in newlyJoined) {
            val friendlyName = localContactNamesByHash[contact.phoneHash]?.takeIf { it.isNotBlank() }
                ?: contact.displayName.ifBlank { "Your contact" }
            val convId = buildDirectConversationId(currentUserId, contact.userId)

            if (appInForeground) {
                _inAppBanners.tryEmit(
                    InAppMessageBanner(
                        conversationId = convId,
                        title = "$friendlyName joined Univers Chat!",
                        senderName = friendlyName,
                        senderPhotoUrl = contact.photoUrl.takeIf { it.isNotBlank() },
                        previewText = "Your saved contact is now on Univers Chat. Tap to say hi!",
                        isGroup = false,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }

            NotificationHelper.showContactJoinedNotification(
                context = context,
                contactDisplayName = friendlyName,
                joinedUserId = contact.userId
            )
        }
    }

    private fun handleConversationSnapshot(
        context: Context,
        currentUserId: String,
        conversations: List<Conversation>
    ) {
        if (!hasSeededInitialConversations) {
            conversations.forEach { conv ->
                val ts = conv.lastMessageAt?.toDate()?.time ?: 0L
                lastSeenMessageTimestamps[conv.id] = ts
            }
            hasSeededInitialConversations = true
            return
        }

        for (conv in conversations) {
            val latestTs = conv.lastMessageAt?.toDate()?.time ?: continue
            val previousTs = lastSeenMessageTimestamps[conv.id] ?: 0L
            if (latestTs <= previousTs) continue

            lastSeenMessageTimestamps[conv.id] = latestTs

            val senderId = conv.lastMessageSenderId
            if (senderId.isBlank() || senderId == currentUserId) continue
            if (senderId in blockedUserIds) continue
            if (conv.isMutedBy(currentUserId)) continue
            if (conv.unreadCountFor(currentUserId) <= 0L) continue
            if (!notificationsEnabled) continue

            val eventKey = "${conv.id}|$latestTs|$senderId|${conv.lastMessageText}"
            if (lastNotifiedEventKeys[conv.id] == eventKey) continue
            lastNotifiedEventKeys[conv.id] = eventKey

            val isViewingThisChat = appInForeground && activeConversationId == conv.id
            if (isViewingThisChat) continue

            val convTitle = conv.displayTitleFor(currentUserId)
            val senderName = conv.lastMessageSenderName.ifBlank { convTitle }
            val preview = formatNotificationPreview(conv)
            val otherPhoto = if (conv.type == Conversation.TYPE_DIRECT) {
                conv.participantPhotos[senderId]
            } else {
                conv.participantPhotos[senderId]
            }

            if (appInForeground) {
                _inAppBanners.tryEmit(
                    InAppMessageBanner(
                        conversationId = conv.id,
                        title = convTitle,
                        senderName = senderName,
                        senderPhotoUrl = otherPhoto,
                        previewText = preview,
                        isGroup = conv.type == Conversation.TYPE_GROUP,
                        timestampMillis = latestTs
                    )
                )
            }

            // Always post an Android system notification when permission is granted so phone tray & wearables alert too
            NotificationHelper.showIncomingMessageNotification(
                context = context,
                conversation = conv,
                conversationTitle = convTitle,
                senderName = senderName,
                messageText = preview,
                timestampMillis = latestTs
            )
        }
    }

    private fun formatNotificationPreview(conversation: Conversation): String {
        val raw = conversation.lastMessageText.trim()
        if (raw.isNotEmpty()) return raw
        return when (conversation.lastMessageType) {
            Message.TYPE_IMAGE -> "📷 Shared a photo"
            Message.TYPE_POLL -> "📊 Started a live poll"
            Message.TYPE_AUDIO -> "🎤 Sent a voice message"
            else -> "Sent a message"
        }
    }

    private const val TAG = "MsgNotifCoordinator"
}
