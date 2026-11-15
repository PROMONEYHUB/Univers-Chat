package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.PrivateUser
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.CallSignalingRepository
import com.example.data.repository.MessagingRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatFilter(val label: String) {
    ALL("All"),
    DIRECT("Direct"),
    GROUPS("Groups"),
    UNREAD("Unread")
}

class MainMessagingViewModel(
    private val repository: MessagingRepository,
    val currentUserId: String,
    private val callSignalingRepository: CallSignalingRepository? = null
) : ViewModel() {

    val currentUserProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing current user profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val privateUser: StateFlow<PrivateUser?> = repository.observePrivateUser(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing private user", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val blockedUserIds: StateFlow<Set<String>> = privateUser
        .map { it?.blockedUserIds?.toSet().orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    val callHistory: StateFlow<List<CallSession>> =
        (callSignalingRepository?.observeCallHistory(currentUserId) ?: flowOf(emptyList()))
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing call history", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId)
            .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing conversations", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load conversations"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val peopleState: StateFlow<UiState<List<UserProfile>>> =
        repository.observeDiscoverableProfiles()
            .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user profiles", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load people directory"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _chatFilter = MutableStateFlow(ChatFilter.ALL)
    val chatFilter: StateFlow<ChatFilter> = _chatFilter.asStateFlow()

    private val _peopleSearchQuery = MutableStateFlow("")
    val peopleSearchQuery: StateFlow<String> = _peopleSearchQuery.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser != null && firebaseUser.uid == currentUserId) {
                repository.syncCurrentUserProfile(firebaseUser).onFailure { err ->
                    Log.w(TAG, "Failed to sync user profile", err)
                    _actionMessage.value = err.localizedMessage ?: "Failed to sync profile"
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChatFilter(filter: ChatFilter) {
        _chatFilter.value = filter
    }

    fun updatePeopleSearchQuery(query: String) {
        _peopleSearchQuery.value = query
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun startDirectChat(
        otherProfile: UserProfile,
        onConversationReady: (String) -> Unit
    ) {
        val me = currentUserProfile.value
        if (me == null) {
            _actionMessage.value = "Syncing your profile, please try again in a moment."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.getOrCreateDirectConversation(me, otherProfile)
                .onSuccess { convId ->
                    _isBusy.value = false
                    onConversationReady(convId)
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _actionMessage.value = err.localizedMessage ?: "Could not open conversation"
                }
        }
    }

    fun createGroupChat(
        title: String,
        topic: String,
        members: List<UserProfile>,
        onConversationReady: (String) -> Unit
    ) {
        val me = currentUserProfile.value
        if (me == null) {
            _actionMessage.value = "Syncing your profile, please try again in a moment."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.createGroupConversation(title, topic, me, members)
                .onSuccess { convId ->
                    _isBusy.value = false
                    onConversationReady(convId)
                }
                .onFailure { err ->
                    _isBusy.value = false
                    _actionMessage.value = err.localizedMessage ?: "Could not create group"
                }
        }
    }

    fun saveProfile(
        displayName: String,
        handle: String,
        bio: String,
        statusVibe: String,
        isOnline: Boolean,
        isDiscoverable: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateProfile(
                displayName = displayName,
                handle = handle,
                bio = bio,
                statusVibe = statusVibe,
                isOnline = isOnline,
                isDiscoverable = isDiscoverable
            ).onSuccess {
                _isBusy.value = false
                _actionMessage.value = "Profile updated"
            }.onFailure { err ->
                _isBusy.value = false
                _actionMessage.value = err.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun updatePrivacyPreferences(readReceipts: Boolean, notifications: Boolean) {
        viewModelScope.launch {
            repository.updatePrivacyPreferences(readReceipts, notifications)
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update preferences"
                }
        }
    }

    fun deleteOrLeaveConversation(
        conversation: Conversation,
        onResult: (success: Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.removeOrDeleteConversation(conversation)
                .onSuccess { wasDeleted ->
                    _actionMessage.value = if (wasDeleted) "Conversation deleted" else "Left channel"
                    onResult(true)
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to remove conversation"
                    onResult(false)
                }
        }
    }

    fun togglePinConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.togglePinConversation(conversation)
                .onSuccess { pinned ->
                    _actionMessage.value = if (pinned) "Conversation pinned" else "Conversation unpinned"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update pin"
                }
        }
    }

    fun toggleMuteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.toggleMuteConversation(conversation)
                .onSuccess { muted ->
                    _actionMessage.value = if (muted) "Notifications muted" else "Notifications unmuted"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update mute"
                }
        }
    }

    fun toggleMarkUnread(conversation: Conversation) {
        viewModelScope.launch {
            repository.toggleMarkUnread(conversation)
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update read status"
                }
        }
    }

    fun setUserBlocked(targetUserId: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.setUserBlocked(targetUserId, blocked)
                .onSuccess {
                    _actionMessage.value = if (blocked) "Contact blocked" else "Contact unblocked"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to update block setting"
                }
        }
    }

    fun forwardMessageToConversation(
        targetConversation: Conversation,
        original: Message,
        onComplete: () -> Unit = {}
    ) {
        val me = currentUserProfile.value
        if (me == null) {
            _actionMessage.value = "Syncing your profile, please try again in a moment."
            return
        }
        viewModelScope.launch {
            repository.forwardMessage(targetConversation, me, original)
                .onSuccess {
                    _actionMessage.value = "Forwarded to ${targetConversation.displayTitleFor(currentUserId)}"
                    onComplete()
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to forward message"
                }
        }
    }

    fun setOnlineStatus(isOnline: Boolean) {
        if (Firebase.auth.currentUser == null) return
        viewModelScope.launch {
            repository.setOnlinePresence(isOnline)
        }
    }

    private companion object {
        const val TAG = "MainMessagingVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
