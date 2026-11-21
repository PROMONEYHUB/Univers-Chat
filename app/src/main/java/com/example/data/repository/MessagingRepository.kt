package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.contacts.DeviceContactsManager
import com.example.crypto.EncryptedPayload
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.PrivateUser
import com.example.data.model.UserProfile
import com.example.data.model.buildDirectConversationId
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.ui.auth.AuthProfilePrefill
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
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
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

    suspend fun syncCurrentUserProfile(
        firebaseUser: FirebaseUser,
        e2eePublicKey: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid == firebaseUser.uid) { "Authenticated UID mismatch" }

        val pendingName = AuthProfilePrefill.pendingDisplayName?.takeIf { it.isNotBlank() }
        val pendingPhoto = AuthProfilePrefill.pendingPhotoUrl?.takeIf { it.isNotBlank() }
        val pendingPhone = AuthProfilePrefill.pendingPhoneNumber?.takeIf { it.isNotBlank() }
        AuthProfilePrefill.pendingDisplayName = null
        AuthProfilePrefill.pendingPhotoUrl = null
        AuthProfilePrefill.pendingPhoneNumber = null

        val email = firebaseUser.email?.takeIf { it.isNotBlank() }
            ?: pendingPhone
            ?: firebaseUser.phoneNumber?.takeIf { it.isNotBlank() }
            ?: "${uid}@universchat.local"
        val computedPhoneHash = DeviceContactsManager.normalizePhoneToHash(
            pendingPhone ?: firebaseUser.phoneNumber ?: email.takeIf { it.startsWith("+") }
        )
        val cleanE2eeKey = e2eePublicKey?.trim()?.take(256)?.takeIf { it.isNotBlank() }

        val privateRef = db.collection(USERS_COLLECTION).document(uid)
        val profileRef = db.collection(USER_PROFILES_COLLECTION).document(uid)

        try {
            val privateSnap = privateRef.get().await()
            val hasCanonicalPrivateSchema = privateSnap.exists() &&
                privateSnap.getString("userId") == uid &&
                (privateSnap.getString("email")?.length ?: 0) >= 3 &&
                privateSnap.getTimestamp("createdAt") != null &&
                !privateSnap.contains("uid") &&
                !privateSnap.contains("authProvider")

            if (!hasCanonicalPrivateSchema) {
                val existingCreatedAt = privateSnap.getTimestamp("createdAt")
                val existingReadReceipts = privateSnap.getBoolean("readReceiptsEnabled") ?: true
                val existingNotifications = privateSnap.getBoolean("notificationsEnabled") ?: true
                val existingBlocked = (privateSnap.get("blockedUserIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.take(100)
                    ?: emptyList()
                val existingContactHashes = (privateSnap.get("contactHashes") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.take(500)
                    ?: emptyList()
                val existingFcmTokens = (privateSnap.get("fcmTokens") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.take(20)
                    ?: emptyList()
                val existingPhoneHash = privateSnap.getString("phoneHash")?.takeIf { it.isNotBlank() }
                    ?: computedPhoneHash
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "email" to email.take(320),
                    "readReceiptsEnabled" to existingReadReceipts,
                    "notificationsEnabled" to existingNotifications,
                    "blockedUserIds" to existingBlocked,
                    "contactHashes" to existingContactHashes,
                    "fcmTokens" to existingFcmTokens,
                    "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!existingPhoneHash.isNullOrBlank()) {
                    payload["phoneHash"] = existingPhoneHash.take(64)
                }
                privateRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "email" to email.take(320),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!computedPhoneHash.isNullOrBlank()) {
                    updates["phoneHash"] = computedPhoneHash.take(64)
                }
                privateRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path, auth)
            throw e
        }

        try {
            val profileSnap = profileRef.get().await()

            val hasCanonicalProfileSchema = profileSnap.exists() &&
                profileSnap.getString("userId") == uid &&
                !profileSnap.getString("displayName").isNullOrBlank() &&
                (profileSnap.getString("handle")?.length ?: 0) >= 2 &&
                profileSnap.getBoolean("isDiscoverable") != null &&
                profileSnap.getTimestamp("createdAt") != null &&
                !profileSnap.contains("uid") &&
                !profileSnap.contains("phoneNumber") &&
                !profileSnap.contains("lastSeen")

            if (!hasCanonicalProfileSchema) {
                val existingName = profileSnap.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
                val rawName = existingName
                    ?: pendingName
                    ?: firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: (pendingPhone ?: firebaseUser.phoneNumber)?.takeIf { it.isNotBlank() }?.let { "Member ${it.takeLast(4)}" }
                    ?: email.substringBefore("@").ifBlank { "Univers Member" }
                val displayName = rawName.take(60)
                val existingHandle = profileSnap.getString("handle")?.trim()?.lowercase()
                    ?.replace(Regex("[^a-z0-9_]"), "")
                    ?.takeIf { it.length in 2..32 }
                val sanitizedHandle = existingHandle ?: rawName.lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .ifBlank { "user_${uid.take(6)}" }
                    .take(28)
                    .let { if (it.length < 2) "u_${uid.take(6)}" else it }
                val existingPhoto = profileSnap.getString("photoUrl").orEmpty()
                val photoUrl = (existingPhoto.ifBlank { pendingPhoto ?: firebaseUser.photoUrl?.toString().orEmpty() })
                    .take(MAX_PROFILE_PHOTO_LENGTH)
                val existingBio = profileSnap.getString("bio")?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Hey there! I'm on Univers Chat."
                val existingVibe = profileSnap.getString("statusVibe")?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Available"
                val existingDiscoverable = profileSnap.getBoolean("isDiscoverable") ?: true
                val existingCreatedAt = profileSnap.getTimestamp("createdAt")
                val resolvedPhoneHash = computedPhoneHash
                    ?: profileSnap.getString("phoneHash")?.takeIf { it.isNotBlank() }
                val resolvedE2eeKey = cleanE2eeKey
                    ?: profileSnap.getString("e2eeIdentityKey")?.takeIf { it.isNotBlank() }

                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "handle" to sanitizedHandle,
                    "bio" to existingBio.take(160),
                    "photoUrl" to photoUrl,
                    "statusVibe" to existingVibe.take(60),
                    "isOnline" to true,
                    "isDiscoverable" to existingDiscoverable,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!resolvedPhoneHash.isNullOrBlank()) {
                    payload["phoneHash"] = resolvedPhoneHash.take(64)
                }
                if (!resolvedE2eeKey.isNullOrBlank()) {
                    payload["e2eeIdentityKey"] = resolvedE2eeKey.take(256)
                }
                profileRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val existingPhoto = profileSnap.getString("photoUrl").orEmpty()
                val fallbackPhoto = (pendingPhoto ?: firebaseUser.photoUrl?.toString().orEmpty()).take(500)
                if (existingPhoto.isBlank() && fallbackPhoto.isNotBlank()) {
                    updates["photoUrl"] = fallbackPhoto
                }
                if (!computedPhoneHash.isNullOrBlank() && profileSnap.getString("phoneHash").isNullOrBlank()) {
                    updates["phoneHash"] = computedPhoneHash.take(64)
                }
                if (!cleanE2eeKey.isNullOrBlank() && profileSnap.getString("e2eeIdentityKey") != cleanE2eeKey) {
                    updates["e2eeIdentityKey"] = cleanE2eeKey
                }
                profileRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path, auth)
            throw e
        }
        sendAuthBotWelcomeMessageIfNeeded(uid).getOrNull()
    }

    suspend fun syncContactHashes(hashes: List<String>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val sanitized = hashes
            .map { it.trim().lowercase() }
            .filter { it.length == 64 }
            .distinct()
            .take(500)
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) return@runCatching
            docRef.update(
                mapOf(
                    "contactHashes" to sanitized,
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
                        .sortedWith(
                            compareByDescending<Conversation> { it.isPinnedBy(userId) }
                                .thenByDescending {
                                    (it.lastMessageAt ?: it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).toDate().time
                                }
                        )
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
        isDiscoverable: Boolean,
        photoUrl: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(USER_PROFILES_COLLECTION).document(uid)
        val cleanName = displayName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
        val cleanHandle = handle.trim().lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .take(32)
        require(cleanHandle.length >= 2) { "Handle must be at least 2 characters." }
        if (photoUrl != null) {
            require(photoUrl.length <= MAX_PROFILE_PHOTO_LENGTH) { "Profile picture exceeds maximum allowed size." }
        }

        val updates = mutableMapOf<String, Any>(
            "displayName" to cleanName,
            "handle" to cleanHandle,
            "bio" to bio.trim().take(160),
            "statusVibe" to statusVibe.trim().take(60),
            "isOnline" to isOnline,
            "isDiscoverable" to isDiscoverable,
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (photoUrl != null) {
            updates["photoUrl"] = photoUrl
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateProfilePhoto(photoUrl: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(photoUrl.length <= MAX_PROFILE_PHOTO_LENGTH) {
            "Profile picture is too large. Please pick a smaller image."
        }
        val docRef = db.collection(USER_PROFILES_COLLECTION).document(uid)
        try {
            docRef.update(
                mapOf(
                    "photoUrl" to photoUrl,
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

    suspend fun setOnlinePresence(isOnline: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(USER_PROFILES_COLLECTION).document(uid)
        try {
            val snap = docRef.get().await()
            val isReadyForPresenceUpdate = snap.exists() &&
                snap.getString("userId") == uid &&
                (snap.getString("handle")?.length ?: 0) >= 2 &&
                snap.getBoolean("isDiscoverable") != null &&
                snap.getTimestamp("createdAt") != null
            if (!isReadyForPresenceUpdate) {
                return@runCatching
            }
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
            val snap = docRef.get().await()
            if (!snap.exists()) return@runCatching
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

    suspend fun setUserBlocked(
        targetUserId: String,
        blocked: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(targetUserId.isNotBlank() && targetUserId != uid) { "Invalid user to block." }
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                throw IllegalStateException("Account setup is still finishing. Please try again.")
            }
            val currentBlocked = (snap.get("blockedUserIds") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList()
                ?: mutableListOf()
            if (blocked) {
                if (targetUserId !in currentBlocked && currentBlocked.size < 100) {
                    currentBlocked.add(targetUserId)
                }
            } else {
                currentBlocked.removeAll { it == targetUserId }
            }
            docRef.update(
                mapOf(
                    "blockedUserIds" to currentBlocked,
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
                    currentProfile.userId to compactSnapshotPhoto(currentProfile.photoUrl),
                    otherProfile.userId to compactSnapshotPhoto(otherProfile.photoUrl)
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
                    "pinnedBy" to emptyMap<String, Boolean>(),
                    "mutedBy" to emptyMap<String, Boolean>(),
                    "typingBy" to emptyMap<String, Timestamp>(),
                    "lastReadAt" to emptyMap<String, Timestamp>(),
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
        val participantPhotos = allProfiles.associate { it.userId to compactSnapshotPhoto(it.photoUrl) }
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
            "pinnedBy" to emptyMap<String, Boolean>(),
            "mutedBy" to emptyMap<String, Boolean>(),
            "typingBy" to emptyMap<String, Timestamp>(),
            "lastReadAt" to emptyMap<String, Timestamp>(),
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
        replyTo: Message? = null,
        encryptedPayload: EncryptedPayload? = null
    ): Result<Unit> = runCatching {
        val trimmed = text.trim().take(2000)
        require(trimmed.isNotEmpty()) { "Message cannot be empty." }
        val wireText = encryptedPayload?.ciphertextBase64 ?: trimmed
        val preview = if (encryptedPayload != null) "🔒 Encrypted message" else trimmed.take(300)
        sendMessageInternal(
            conversation = conversation,
            senderProfile = senderProfile,
            type = Message.TYPE_TEXT,
            text = wireText,
            previewText = preview,
            imageBase64 = null,
            replyTo = replyTo,
            pollQuestion = null,
            pollOptions = null,
            encryptedPayload = encryptedPayload
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

    suspend fun sendVoiceMessage(
        conversation: Conversation,
        senderProfile: UserProfile,
        audioPayload: String,
        durationSec: Int,
        replyTo: Message? = null
    ): Result<Unit> = runCatching {
        val trimmed = audioPayload.trim()
        val isRemoteUrl = trimmed.startsWith("https://", ignoreCase = true)
        val cleanUrl = if (isRemoteUrl && trimmed.length in 12..2000) trimmed else null
        val cleanBase64 = if (!isRemoteUrl && trimmed.length in 16..95000) trimmed else null
        require(cleanUrl != null || cleanBase64 != null) {
            "Valid voice message audio data is required."
        }
        val safeSec = durationSec.coerceIn(1, 180)
        val formatted = "%d:%02d".format(safeSec / 60, safeSec % 60)
        val preview = "🎤 Voice message ($formatted)"
        sendMessageInternal(
            conversation = conversation,
            senderProfile = senderProfile,
            type = Message.TYPE_AUDIO,
            text = cleanUrl ?: "Voice message ($formatted)",
            previewText = preview,
            imageBase64 = null,
            audioUrl = cleanUrl,
            audioBase64 = cleanBase64,
            audioDurationSec = safeSec,
            replyTo = replyTo,
            pollQuestion = null,
            pollOptions = null
        )
    }

    suspend fun forwardMessage(
        targetConversation: Conversation,
        senderProfile: UserProfile,
        original: Message
    ): Result<Unit> = runCatching {
        when (original.type) {
            Message.TYPE_IMAGE -> {
                val img = original.imageBase64?.takeIf { it.isNotBlank() }
                    ?: error("Photo unavailable to forward.")
                val preview = if (original.text.isNotBlank()) "↪ 📷 ${original.text}".take(300) else "↪ 📷 Forwarded photo"
                sendMessageInternal(
                    conversation = targetConversation,
                    senderProfile = senderProfile,
                    type = Message.TYPE_IMAGE,
                    text = original.text.take(2000),
                    previewText = preview,
                    imageBase64 = img,
                    isForwarded = true
                )
            }
            Message.TYPE_AUDIO -> {
                val remoteUrl = original.audioUrl?.trim()?.takeIf { it.startsWith("https://") && it.length >= 12 }
                val legacyAudio = original.audioBase64?.takeIf { it.length >= 16 }
                if (remoteUrl == null && legacyAudio == null) {
                    error("Voice note unavailable to forward.")
                }
                val safeSec = original.audioDurationSec.coerceIn(1, 180)
                val formatted = "%d:%02d".format(safeSec / 60, safeSec % 60)
                val label = remoteUrl ?: original.text.ifBlank { "Voice message ($formatted)" }
                sendMessageInternal(
                    conversation = targetConversation,
                    senderProfile = senderProfile,
                    type = Message.TYPE_AUDIO,
                    text = label,
                    previewText = "↪ 🎤 Voice message ($formatted)".take(300),
                    imageBase64 = null,
                    audioUrl = remoteUrl,
                    audioBase64 = if (remoteUrl == null) legacyAudio else null,
                    audioDurationSec = safeSec,
                    isForwarded = true
                )
            }
            Message.TYPE_POLL -> {
                val q = original.pollQuestion ?: original.text
                val opts = original.pollOptions
                if (opts.size >= 2) {
                    sendMessageInternal(
                        conversation = targetConversation,
                        senderProfile = senderProfile,
                        type = Message.TYPE_POLL,
                        text = q.take(200),
                        previewText = "↪ 📊 $q".take(300),
                        imageBase64 = null,
                        pollQuestion = q.take(200),
                        pollOptions = opts,
                        isForwarded = true
                    )
                } else {
                    sendMessageInternal(
                        conversation = targetConversation,
                        senderProfile = senderProfile,
                        type = Message.TYPE_TEXT,
                        text = q.take(2000),
                        previewText = "↪ $q".take(300),
                        imageBase64 = null,
                        isForwarded = true
                    )
                }
            }
            else -> {
                val clean = original.text.trim().take(2000)
                require(clean.isNotEmpty()) { "Nothing to forward." }
                sendMessageInternal(
                    conversation = targetConversation,
                    senderProfile = senderProfile,
                    type = Message.TYPE_TEXT,
                    text = clean,
                    previewText = "↪ $clean".take(300),
                    imageBase64 = null,
                    isForwarded = true
                )
            }
        }
    }

    private suspend fun sendMessageInternal(
        conversation: Conversation,
        senderProfile: UserProfile,
        type: String,
        text: String,
        previewText: String,
        imageBase64: String?,
        audioUrl: String? = null,
        audioBase64: String? = null,
        audioDurationSec: Int? = null,
        isForwarded: Boolean = false,
        replyTo: Message? = null,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
        encryptedPayload: EncryptedPayload? = null
    ) {
        val uid = requireUserId()
        require(conversation.id.isNotBlank()) { "Conversation ID is required." }

        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        val msgRef = convRef.collection(MESSAGES_SUBCOLLECTION).document()

        val messagePayload = mutableMapOf<String, Any>(
            "conversationId" to conversation.id,
            "senderId" to uid,
            "senderName" to senderProfile.displayName.take(60).ifBlank { "Member" },
            "senderPhotoUrl" to compactSnapshotPhoto(senderProfile.photoUrl),
            "participantIds" to conversation.participantIds,
            "type" to type,
            "text" to text,
            "reactions" to emptyMap<String, String>(),
            "isEdited" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (encryptedPayload != null) {
            messagePayload["isEncrypted"] = true
            messagePayload["e2eeVersion"] = encryptedPayload.e2eeVersion
            messagePayload["ephemeralPublicKey"] = encryptedPayload.ephemeralPublicKeyBase64.take(256)
            messagePayload["encryptionIv"] = encryptedPayload.ivBase64.take(64)
            messagePayload["senderIdentityKey"] = encryptedPayload.senderIdentityKeyBase64.take(256)
        }
        if (isForwarded) {
            messagePayload["isForwarded"] = true
        }
        if (!imageBase64.isNullOrBlank()) {
            messagePayload["imageBase64"] = imageBase64
        }
        if (!audioUrl.isNullOrBlank()) {
            messagePayload["audioUrl"] = audioUrl.take(2000)
            messagePayload["audioDurationSec"] = (audioDurationSec ?: 1).coerceIn(0, 180)
        }
        if (!audioBase64.isNullOrBlank()) {
            messagePayload["audioBase64"] = audioBase64
            messagePayload["audioDurationSec"] = (audioDurationSec ?: 1).coerceIn(0, 180)
        }
        if (replyTo != null && replyTo.id.isNotBlank()) {
            messagePayload["replyToMessageId"] = replyTo.id.take(128)
            messagePayload["replyToSenderName"] = replyTo.senderName.take(60)
            val replyPreview = when (replyTo.type) {
                Message.TYPE_IMAGE -> if (replyTo.text.isNotBlank()) "📷 ${replyTo.text}" else "📷 Photo"
                Message.TYPE_POLL -> "📊 ${replyTo.pollQuestion ?: replyTo.text}"
                Message.TYPE_AUDIO -> {
                    val sec = replyTo.audioDurationSec.coerceAtLeast(1)
                    "🎤 Voice message (%d:%02d)".format(sec / 60, sec % 60)
                }
                else -> if (replyTo.isEncrypted) "🔒 Encrypted message" else replyTo.text
            }.take(160)
            messagePayload["replyToPreview"] = replyPreview
        }
        if (!pollQuestion.isNullOrBlank() && !pollOptions.isNullOrEmpty()) {
            messagePayload["pollQuestion"] = pollQuestion
            messagePayload["pollOptions"] = pollOptions
            messagePayload["pollVotes"] = emptyMap<String, String>()
        }

        val convUpdatePayload = mutableMapOf<String, Any>(
            "lastMessageText" to previewText.take(300),
            "lastMessageSenderId" to uid,
            "lastMessageSenderName" to senderProfile.displayName.take(60),
            "lastMessageType" to type,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "unreadCounts.$uid" to 0L,
            "typingBy.$uid" to FieldValue.delete(),
            "lastReadAt.$uid" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        conversation.participantIds
            .asSequence()
            .filter { it != uid && it.isNotBlank() }
            .forEach { peerId ->
                convUpdatePayload["unreadCounts.$peerId"] = FieldValue.increment(1L)
            }

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
        newText: String,
        encryptedPayload: EncryptedPayload? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val trimmed = newText.trim().take(2000)
        require(trimmed.isNotEmpty()) { "Edited message cannot be empty." }
        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(messageId)
        val updates = mutableMapOf<String, Any>(
            "text" to (encryptedPayload?.ciphertextBase64 ?: trimmed),
            "isEdited" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (encryptedPayload != null) {
            updates["isEncrypted"] = true
            updates["e2eeVersion"] = encryptedPayload.e2eeVersion
            updates["ephemeralPublicKey"] = encryptedPayload.ephemeralPublicKeyBase64.take(256)
            updates["encryptionIv"] = encryptedPayload.ivBase64.take(64)
            updates["senderIdentityKey"] = encryptedPayload.senderIdentityKeyBase64.take(256)
        }
        try {
            msgRef.update(updates).await()
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
        val cleanEmoji = emoji.trim().take(16)
        require(cleanEmoji.isNotEmpty()) { "Reaction emoji cannot be empty." }

        val fieldValue: Any = if (message.reactions[uid] == cleanEmoji) {
            FieldValue.delete()
        } else {
            cleanEmoji
        }
        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(message.id)
        try {
            msgRef.update(
                mapOf(
                    "reactions.$uid" to fieldValue,
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
        val msgRef = db.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .document(message.id)
        try {
            msgRef.update(
                mapOf(
                    "pollVotes.$uid" to optionIndex.toString(),
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

    suspend fun markConversationRead(
        conversation: Conversation,
        sendReadReceipt: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val hasUnread = (conversation.unreadCounts[uid] ?: 0L) > 0L
        val lastMsgMillis = conversation.lastMessageAt?.toDate()?.time ?: 0L
        val myLastReadMillis = conversation.lastReadAt[uid]?.toDate()?.time ?: 0L
        val needsReadReceiptStamp = sendReadReceipt &&
            conversation.lastMessageSenderId.isNotBlank() &&
            conversation.lastMessageSenderId != uid &&
            lastMsgMillis > myLastReadMillis

        if (!hasUnread && !needsReadReceiptStamp) return@runCatching

        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (hasUnread) {
            updates["unreadCounts.$uid"] = 0L
        }
        if (sendReadReceipt) {
            updates["lastReadAt.$uid"] = FieldValue.serverTimestamp()
        }
        try {
            convRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun toggleMarkUnread(conversation: Conversation): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val currentlyUnread = (conversation.unreadCounts[uid] ?: 0L) > 0L
        val targetCount = if (currentlyUnread) 0L else 1L
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.update(
                mapOf(
                    "unreadCounts.$uid" to targetCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            !currentlyUnread
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun togglePinConversation(conversation: Conversation): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val nowPinned = !conversation.isPinnedBy(uid)
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.update(
                mapOf(
                    "pinnedBy.$uid" to if (nowPinned) true else FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            nowPinned
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun toggleMuteConversation(conversation: Conversation): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val nowMuted = !conversation.isMutedBy(uid)
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        try {
            convRef.update(
                mapOf(
                    "mutedBy.$uid" to if (nowMuted) true else FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            nowMuted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(conversationId.isNotBlank()) { "Conversation ID is required." }
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
        val payload = mapOf<String, Any>(
            "typingBy.$uid" to if (isTyping) FieldValue.serverTimestamp() else FieldValue.delete(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            convRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path, auth)
            throw e
        }
    }

    suspend fun pinMessage(conversationId: String, message: Message?): Result<Unit> = runCatching {
        requireUserId()
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
        val preview = when (message?.type) {
            Message.TYPE_IMAGE -> if (message.text.isNotBlank()) "📷 ${message.text}" else "📷 Photo"
            Message.TYPE_POLL -> "📊 ${message.pollQuestion ?: message.text}"
            Message.TYPE_AUDIO -> {
                val sec = message.audioDurationSec.coerceAtLeast(1)
                "🎤 Voice message (%d:%02d)".format(sec / 60, sec % 60)
            }
            else -> if (message?.isEncrypted == true) "🔒 Encrypted message" else message?.text.orEmpty()
        }.take(200)
        val updates = if (message == null) {
            mapOf<String, Any>(
                "pinnedMessageId" to "",
                "pinnedMessagePreview" to "",
                "pinnedMessageSenderName" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf<String, Any>(
                "pinnedMessageId" to message.id.take(128),
                "pinnedMessagePreview" to preview,
                "pinnedMessageSenderName" to message.senderName.take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        try {
            convRef.update(updates).await()
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
            toAdd.forEach { put(it.userId, compactSnapshotPhoto(it.photoUrl)) }
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
                msgSnapshot.documents.chunked(450).forEach { chunk ->
                    db.runBatch { batch ->
                        chunk.forEach { msgDoc ->
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
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversation.id)
        if (updatedIds.isEmpty()) {
            if (conversation.createdBy == uid) {
                try {
                    convRef.delete().await()
                    return@runCatching
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, convRef.path, auth)
                    throw e
                }
            } else {
                throw IllegalStateException("You are the last member in this group.")
            }
        }
        val updatedNames = conversation.participantNames.toMutableMap().apply { remove(uid) }
        val updatedPhotos = conversation.participantPhotos.toMutableMap().apply { remove(uid) }
        val updatedUnread = conversation.unreadCounts.toMutableMap().apply { remove(uid) }

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
        val isCreator = conversation.createdBy == uid
        val isDirectParticipant = conversation.type == Conversation.TYPE_DIRECT && uid in conversation.participantIds
        require(isCreator || isDirectParticipant) {
            "Only the conversation creator or direct chat participant can delete this thread."
        }
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

    /**
     * Deletes a direct chat or creator-owned channel, or leaves a group channel owned by someone else.
     * Returns true if the conversation was deleted, or false if the user left the group channel.
     */
    suspend fun removeOrDeleteConversation(conversation: Conversation): Result<Boolean> = runCatching {
        val uid = requireUserId()
        require(conversation.id.isNotBlank()) { "Conversation ID is required." }
        if (conversation.type == Conversation.TYPE_GROUP && conversation.createdBy != uid) {
            leaveGroup(conversation).getOrThrow()
            false
        } else {
            deleteConversation(conversation).getOrThrow()
            true
        }
    }

    private fun compactSnapshotPhoto(photoUrl: String): String =
        photoUrl.takeIf { it.length <= 500 }.orEmpty()

    suspend fun ensureSystemAuthBotProfile(): Result<Unit> = runCatching {
        val botRef = db.collection(USER_PROFILES_COLLECTION).document(SYSTEM_AUTH_BOT_ID)
        val snap = botRef.get().await()
        if (!snap.exists()) {
            val botPayload = mapOf(
                "userId" to SYSTEM_AUTH_BOT_ID,
                "displayName" to "Univers Verification Bot",
                "handle" to "univers_auth_bot",
                "bio" to "Official Telegram & App login verification bot",
                "isOnline" to true,
                "isDiscoverable" to true,
                "photoUrl" to "https://api.dicebear.com/7.x/bottts/svg?seed=UniversAuthBot",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            botRef.set(botPayload).await()
        }
    }

    suspend fun sendAuthBotWelcomeMessageIfNeeded(currentUserId: String): Result<Unit> = runCatching {
        ensureSystemAuthBotProfile().getOrThrow()
        val directId = buildDirectConversationId(currentUserId, SYSTEM_AUTH_BOT_ID)
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(directId)
        val convSnap = convRef.get().await()
        val botProfile = UserProfile(
            userId = SYSTEM_AUTH_BOT_ID,
            displayName = "Univers Verification Bot",
            handle = "univers_auth_bot",
            bio = "Official login verification bot",
            isOnline = true,
            isDiscoverable = true,
            photoUrl = "https://api.dicebear.com/7.x/bottts/svg?seed=UniversAuthBot"
        )
        val myProfileSnap = db.collection(USER_PROFILES_COLLECTION).document(currentUserId).get().await()
        val myProfile = myProfileSnap.toObject(UserProfile::class.java) ?: return@runCatching

        if (!convSnap.exists()) {
            getOrCreateDirectConversation(myProfile, botProfile).getOrThrow()
        }
        val conversation = db.collection(CONVERSATIONS_COLLECTION).document(directId).get().await().toObject(Conversation::class.java)
        if (conversation != null) {
            val messagesSnap = db.collection(CONVERSATIONS_COLLECTION).document(directId).collection(MESSAGES_SUBCOLLECTION).limit(1).get().await()
            if (messagesSnap.isEmpty) {
                sendTextMessage(
                    conversation = conversation,
                    senderProfile = botProfile,
                    text = "👋 Welcome to Univers Verification Bot! Security login codes and Telegram bot OTP verification notices for your active sessions will appear here automatically, exactly like Telegram."
                ).getOrThrow()
            }
        }
    }

    suspend fun sendAuthBotOtpMessage(targetUserId: String, otpCode: String): Result<Unit> = runCatching {
        ensureSystemAuthBotProfile().getOrThrow()
        val directId = buildDirectConversationId(targetUserId, SYSTEM_AUTH_BOT_ID)
        val convRef = db.collection(CONVERSATIONS_COLLECTION).document(directId)
        val convSnap = convRef.get().await()
        val botProfile = UserProfile(
            userId = SYSTEM_AUTH_BOT_ID,
            displayName = "Univers Verification Bot",
            handle = "univers_auth_bot",
            bio = "Official login verification bot",
            isOnline = true,
            isDiscoverable = true,
            photoUrl = "https://api.dicebear.com/7.x/bottts/svg?seed=UniversAuthBot"
        )
        if (!convSnap.exists()) {
            val targetProfileSnap = db.collection(USER_PROFILES_COLLECTION).document(targetUserId).get().await()
            val targetProfile = targetProfileSnap.toObject(UserProfile::class.java)
            if (targetProfile != null) {
                getOrCreateDirectConversation(targetProfile, botProfile).getOrThrow()
            }
        }
        val conversation = db.collection(CONVERSATIONS_COLLECTION).document(directId).get().await().toObject(Conversation::class.java)
        if (conversation != null) {
            sendTextMessage(
                conversation = conversation,
                senderProfile = botProfile,
                text = "🔐 Univers Chat Login Code: $otpCode\n\nSomeone requested to sign in to your account from another device. If this was you, enter this code to complete verification. Do not share this code with anyone."
            ).getOrThrow()
        }
    }

    companion object {
        const val SYSTEM_AUTH_BOT_ID = "system_auth_bot"
        const val USERS_COLLECTION = "users"
        const val USER_PROFILES_COLLECTION = "user_profiles"
        const val CONVERSATIONS_COLLECTION = "conversations"
        const val MESSAGES_SUBCOLLECTION = "messages"
        private const val MAX_PROFILE_PHOTO_LENGTH = 60_000
    }
}
