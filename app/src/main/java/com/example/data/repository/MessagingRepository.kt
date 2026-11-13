package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.PrivateUser
import com.example.data.model.UserProfile
import com.example.data.model.buildDirectConversationId
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MessagingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
        throw error
    }

    suspend fun syncCurrentUserProfile(firebaseUser: FirebaseUser): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid == firebaseUser.uid) { "Authenticated UID mismatch" }

        val email = firebaseUser.email?.takeIf { it.isNotBlank() } ?: "${uid}@universchat.local"
        val privateRef = db.collection(USERS_COLLECTION).document(uid)
        val profileRef = db.collection(USER_PROFILES_COLLECTION).document(uid)

        try {
            val privateSnap = privateRef.get().await()
            if (!privateSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "email" to email.take(320),
                    "readReceiptsEnabled" to true,
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(payload).await()
            } else {
                privateRef.update(
                    mapOf(
                        "email" to email.take(320),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path, auth)
            throw e
        }

        try {
            val profileSnap = profileRef.get().await()
            if (!profileSnap.exists()) {
                val rawName = firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").ifBlank { "Univers Member" }
                val displayName = rawName.take(60)
                val sanitizedHandle = rawName.lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .ifBlank { "user_${uid.take(6)}" }
                    .take(28)
                    .let { if (it.length < 2) "u_${uid.take(6)}" else it }
                val photoUrl = firebaseUser.photoUrl?.toString()?.take(500) ?: ""

                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "handle" to sanitizedHandle,
                    "bio" to "Hey there! I'm on Univers Chat.",
                    "photoUrl" to photoUrl,
                    "statusVibe" to "Available",
                    "isOnline" to true,
                    "isDiscoverable" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val existingPhoto = profileSnap.getString("photoUrl").orEmpty()
                val googlePhoto = firebaseUser.photoUrl?.toString()?.take(500).orEmpty()
                if (existingPhoto.isBlank() && googlePhoto.isNotBlank()) {
                    updates["photoUrl"] = googlePhoto
                }
                profileRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path, auth)
            throw e
        }
    }

    fun observePrivateUser(userId: String): Flow<PrivateUser?> = flow {
        val uid = requireUserId()
        val path = "$USERS_COLLECTION/$userId"
        emitAll(
            db.collection(USERS_COLLECTION).document(userId)
                .snapshots()
                .map { it.toObject(PrivateUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "$USER_PROFILES_COLLECTION/$userId"
        emitAll(
            db.collection(USER_PROFILES_COLLECTION).document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeDiscoverableProfiles(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection(USER_PROFILES_COLLECTION)
                .whereEqualTo("isDiscoverable", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<UserProfile> { it.isOnline }
                                .thenBy { it.displayName.lowercase() }
                        )
                }
                .logFirestoreErrors(OperationType.LIST, USER_PROFILES_COLLECTION, uid)
        )
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection(CONVERSATIONS_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending {
                            (it.lastMessageAt ?: it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).toDate().time
                        }
                }
                .logFirestoreErrors(OperationType.LIST, CONVERSATIONS_COLLECTION, uid)
        )
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> = flow {
        val uid = requireUserId()
        val path = "$CONVERSATIONS_COLLECTION/$conversationId"
        emitAll(
            db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
                .snapshots()
                .map { it.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<Message>> = flow {
        val uid = requireUserId()
        val path = "$CONVERSATIONS_COLLECTION/$conversationId/$MESSAGES_SUBCOLLECTION"
        emitAll(
            db.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_SUBCOLLECTION)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Message::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { (it.createdAt ?: Timestamp(Long.MAX_VALUE / 1000, 0)).toDate().time }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        statusVibe: String,
        isOnline: Boolean,
        isDiscoverable: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(USER_PROFILES_COLLECTION).document(uid)
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
        val cleanHandle = handle.trim().lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .take(32)
        require(cleanHandle.length >= 2) { "Handle must be at least 2 characters." }

        val updates = mapOf(
            "displayName" to cleanName,
            "handle" to cleanHandle,
            "bio" to bio.trim().take(160),
            "statusVibe" to statusVibe.trim().take(60),
            "isOnline" to isOnline,
            "isDiscoverable" to isDiscoverable,
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun setOnlinePresence(isOnline: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(USER_PROFILES_COLLECTION).document(uid)
        try {
            docRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updatePrivacyPreferences(
        readReceiptsEnabled: Boolean,
        notificationsEnabled: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        try {
            docRef.update(
                mapOf(
                    "readReceiptsEnabled" to readReceiptsEnabled,
                    "notificationsEnabled" to notificationsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun getOrCreateDirectConversation(
        currentProfile: UserProfile,
        otherProfile: UserProfile
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(uid == currentProfile.userId) { "Authenticated user mismatch" }

        val conversationId = buildDirectConversationId(currentProfile.userId, otherProfile.userId)
        val docRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)

        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val participantIds = listOf(currentProfile.userId, otherProfile.userId).distinct()
                val participantNames = mapOf(
                    currentProfile.userId to currentProfile.displayName,
                    otherProfile.userId to otherProfile.displayName
                )
                val participantPhotos = mapOf(
                    currentProfile.userId to currentProfile.photoUrl,
                    otherProfile.userId to otherProfile.photoUrl
                )
                val unreadCounts = participantIds.associateWith { 0L }

                val payload = mapOf(
                    "type" to Conversation.TYPE_DIRECT,
                    "title" to "",
                    "topic" to "",
                    "createdBy" to uid,
                    "participantIds" to participantIds,
                    "participantNames" to participantNames,
                    "participantPhotos" to participantPhotos,
                    "lastMessageText" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageSenderName" to "",
                    "lastMessageType" to "",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "unreadCounts" to unreadCounts,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            conversationId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    suspend fun createGroupConversation(
        title: String,
        topic: String,
        currentProfile: UserProfile,
        selectedMembers: List<UserProfile>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(uid == currentProfile.userId) { "Authenticated user mismatch" }
        val cleanTitle = title.trim().take(80)
        require(cleanTitle.isNotEmpty()) { "Group name is required." }

        val allProfiles = (listOf(currentProfile) + selectedMembers).distinctBy { it.userId }.take(50)
        val participantIds = allProfiles.map { it.userId }
        val participantNames = allProfiles.associate { it.userId to it.displayName }
        val participantPhotos = allProfiles.associate { it.userId to it.photoUrl }
        val unreadCounts = participantIds.associateWith { 0L }

        val docRef = db.collection(CONVERSATIONS_COLLECTION).document()
        val payload = mapOf(
            "type" to Conversation.TYPE_GROUP,
            "title" to cleanTitle,
            "topic" to topic.trim().take(160),
            "createdBy" to uid,
            "participantIds" to participantIds,
            "participantNames" to participantNames,
            "participantPhotos" to participantPhotos,
            "lastMessageText" to "${currentProfile.displayName} created \"$cleanTitle\"",
            "lastMessageSenderId" to uid,
            "lastMessageSenderName" to currentProfile.displayName,
            "lastMessageType" to Message.TYPE_TEXT,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "unreadCounts" to unreadCounts,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun sendTextMessage(
        conversation: Conversation,
        senderProfile: UserProfile,
        text: String,
        replyTo: Message? = null
    ): Result<Unit> = runCatching {
        val trimmed = text.trim().take(2000)
        require(trimmed.isNotEmpty()) { "Message cannot be empty." }
        sendMessageInternal(
            conversation = conversation,
            senderProfile = senderProfile,
            type = Message.TYPE_TEXT,
            text = trimmed,
            previewText = trimmed.take(300),
            imageBase64 = null,
            replyTo = replyTo,
            pollQuestion = null,
            pollOptions = null
        )
    }

    suspend fun sendImageMessage(
        conversation: Conversation,
        senderProfile: UserProfile,
        caption: String,
        imageBase64: String,
        replyTo: Message? = null
    ): Result<Unit> = runCatching {
        require(imageBase64.isNotBlank() && imageBase64.length <= 80000) {
            "Image size exceeds allowed limit."
        }
        val cleanCaption = caption.trim().take(2000)
        val preview = if (cleanCaption.isNotBlank()) "📷 $cleanCaption".take(300) else "📷 Shared a photo"
        sendMessageInternal(
            conversation = conversation,
            senderProfile = senderProfile,
            type = Message.TYPE_IMAGE,
            text = cleanCaption,
            previewText = preview,
            imageBase64 = imageBase64,
            replyTo = replyTo,
            pollQuestion = null,
            pollOptions = null
        )
    }

    suspend fun sendPollMessage(
        conversation: Conversation,
        senderProfile: UserProfile,
        question: String,
        options: List<String>
    ): Result<Unit> = runCatching {
        val cleanQuestion = question.trim().take(200)
        val cleanOptions = options.map { it.trim().take(80) }.filter { it.isNotEmpty() }.take(6)
        require(cleanQuestion.isNotEmpty()) { "Poll question cannot be empty." }
        require(cleanOptions.size >= 2) { "Provide at least 2 poll options." }

        sendMessageInternal(
            conversation = conversation,
            senderProfile = senderProfile,
            type = Message.TYPE_POLL,
            text = cleanQuestion,
            previewText = "📊 Poll: $cleanQuestion".take(300),
            imageBase64 = null,
            replyTo = null,
            pollQuestion = cleanQuestion,
            pollOptions = cleanOptions
        )
    }

    private suspend fun sendMessageInternal(
        conversation: Conversation,
        senderProfile: UserProfile,
        type: String,
        text: String,
        previewText: String,
        imageBase64: String?,
        replyTo: Message?,
        pollQuestion: String?,
        pollOptions: List<String>?
    ) {
        val uid = requireUserId()
        require(conversation.id.isNotBlank()) { "Conversation ID is required." }

        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        val msgRef = convRef.collection(MESSAGES_SUBCOLLECTION).document()

        val messagePayload = mutableMapOf<String, Any>(
            "conversationId" to conversation.id,
            "senderId" to uid,
            "senderName" to senderProfile.displayName.take(60).ifBlank { "Member" },
            "senderPhotoUrl" to senderProfile.photoUrl.take(500),
            "participantIds" to conversation.participantIds,
            "type" to type,
            "text" to text,
            "reactions" to emptyMap<String, String>(),
            "isEdited" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!imageBase64.isNullOrBlank()) {
            messagePayload["imageBase64"] = imageBase64
        }
        if (replyTo != null && replyTo.id.isNotBlank()) {
            messagePayload["replyToMessageId"] = replyTo.id.take(128)
            messagePayload["replyToSenderName"] = replyTo.senderName.take(60)
            val replyPreview = when (replyTo.type) {
                Message.TYPE_IMAGE -> if (replyTo.text.isNotBlank()) "📷 ${replyTo.text}" else "📷 Photo"
                Message.TYPE_POLL -> "📊 ${replyTo.pollQuestion ?: replyTo.text}"
                else -> replyTo.text
            }.take(160)
            messagePayload["replyToPreview"] = replyPreview
        }
        if (!pollQuestion.isNullOrBlank() && !pollOptions.isNullOrEmpty()) {
            messagePayload["pollQuestion"] = pollQuestion
            messagePayload["pollOptions"] = pollOptions
            messagePayload["pollVotes"] = emptyMap<String, String>()
        }

        val updatedUnread = conversation.participantIds.associateWith { participantId ->
            if (participantId == uid) 0L else (conversation.unreadCounts[participantId] ?: 0L) + 1L
        }

        val convUpdatePayload = mapOf(
            "lastMessageText" to previewText.take(300),
            "lastMessageSenderId" to uid,
            "lastMessageSenderName" to senderProfile.displayName.take(60),
            "lastMessageType" to type,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "unreadCounts" to updatedUnread,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runBatch { batch ->
                batch.set(msgRef, messagePayload)
                batch.update(convRef, convUpdatePayload)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newText: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val trimmed = newText.trim().take(2000)
        require(trimmed.isNotEmpty()) { "Edited message cannot be empty." }
        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(messageId)
        try {
            msgRef.update(
                mapOf(
                    "text" to trimmed,
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(messageId)
        try {
            msgRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun toggleReaction(
        conversationId: String,
        message: Message,
        emoji: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(message.id.isNotBlank()) { "Message ID is required." }
        val updatedReactions = message.reactions.toMutableMap()
        if (updatedReactions[uid] == emoji) {
            updatedReactions.remove(uid)
        } else {
            updatedReactions[uid] = emoji
        }
        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(message.id)
        try {
            msgRef.update(
                mapOf(
                    "reactions" to updatedReactions,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun votePoll(
        conversationId: String,
        message: Message,
        optionIndex: Int
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(message.id.isNotBlank()) { "Message ID is required." }
        val updatedVotes = message.pollVotes.toMutableMap()
        updatedVotes[uid] = optionIndex.toString()

        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(message.id)
        try {
            msgRef.update(
                mapOf(
                    "pollVotes" to updatedVotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path, auth)
            throw e
        }
    }

    suspend fun markConversationRead(conversation: Conversation): Result<Unit> = runCatching {
        val uid = requireUserId()
        if ((conversation.unreadCounts[uid] ?: 0L) == 0L) return@runCatching
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        val updatedUnread = conversation.unreadCounts.toMutableMap().apply {
            this[uid] = 0L
        }
        try {
            convRef.update(
                mapOf(
                    "unreadCounts" to updatedUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun updateGroupInfo(
        conversation: Conversation,
        newTitle: String,
        newTopic: String
    ): Result<Unit> = runCatching {
        requireUserId()
        require(conversation.type == Conversation.TYPE_GROUP) { "Only groups can be updated." }
        val cleanTitle = newTitle.trim().take(80)
        require(cleanTitle.isNotEmpty()) { "Group title cannot be empty." }
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "topic" to newTopic.trim().take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun addGroupMembers(
        conversation: Conversation,
        newMembers: List<UserProfile>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(conversation.type == Conversation.TYPE_GROUP) { "Cannot add members to a direct chat." }
        val toAdd = newMembers.filter { it.userId !in conversation.participantIds }
        if (toAdd.isEmpty()) return@runCatching

        val updatedIds = (conversation.participantIds + toAdd.map { it.userId }).distinct().take(50)
        val updatedNames = conversation.participantNames.toMutableMap().apply {
            toAdd.forEach { put(it.userId, it.displayName) }
        }
        val updatedPhotos = conversation.participantPhotos.toMutableMap().apply {
            toAdd.forEach { put(it.userId, it.photoUrl) }
        }
        val updatedUnread = conversation.unreadCounts.toMutableMap().apply {
            toAdd.forEach { putIfAbsent(it.userId, 0L) }
        }

        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.update(
                mapOf(
                    "participantIds" to updatedIds,
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "unreadCounts" to updatedUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val msgSnapshot = convRef.collection(MESSAGES_SUBCOLLECTION)
                .whereArrayContains("participantIds", uid)
                .get()
                .await()
            if (!msgSnapshot.isEmpty) {
                db.runBatch { batch ->
                    msgSnapshot.documents.forEach { msgDoc ->
                        batch.update(
                            msgDoc.reference,
                            mapOf(
                                "participantIds" to updatedIds,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun leaveGroup(conversation: Conversation): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(conversation.type == Conversation.TYPE_GROUP) { "Cannot leave a direct chat." }
        val updatedIds = conversation.participantIds.filter { it != uid }
        val updatedNames = conversation.participantNames.toMutableMap().apply { remove(uid) }
        val updatedPhotos = conversation.participantPhotos.toMutableMap().apply { remove(uid) }
        val updatedUnread = conversation.unreadCounts.toMutableMap().apply { remove(uid) }

        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.update(
                mapOf(
                    "participantIds" to updatedIds,
                    "participantNames" to updatedNames,
                    "participantPhotos" to updatedPhotos,
                    "unreadCounts" to updatedUnread,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun deleteConversation(conversation: Conversation): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(conversation.id.isNotBlank()) { "Conversation ID is required." }
        require(conversation.createdBy == uid) { "Only the conversation creator can delete this thread." }
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path, auth)
            throw e
        }
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val USER_PROFILES_COLLECTION = "user_profiles"
        const val CONVERSATIONS_COLLECTION = "conversations"
        const val MESSAGES_SUBCOLLECTION = "messages"
    }
}
