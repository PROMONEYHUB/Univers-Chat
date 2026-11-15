package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.MessagingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val repository: MessagingRepository,
    val conversationId: String,
    val currentUserId: String
) : ViewModel() {

    val conversationState: StateFlow<UiState<Conversation?>> =
        repository.observeConversation(conversationId)
            .map<Conversation?, UiState<Conversation?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing conversation $conversationId", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load conversation"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val messagesState: StateFlow<UiState<List<Message>>> =
        repository.observeMessages(conversationId, currentUserId)
            .map<List<Message>, UiState<List<Message>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing messages for $conversationId", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load messages"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    fun setReplyTarget(message: Message?) {
        _editingMessage.value = null
        _replyingTo.value = message
    }

    fun setEditingTarget(message: Message?) {
        _replyingTo.value = null
        _editingMessage.value = message
    }

    fun clearErrorBanner() {
        _errorBanner.value = null
    }

    fun markRead(conversation: Conversation) {
        if (conversation.unreadCountFor(currentUserId) <= 0L) return
        viewModelScope.launch {
            repository.markConversationRead(conversation)
        }
    }

    fun sendOrEditText(
        conversation: Conversation,
        senderProfile: UserProfile,
        text: String,
        onSuccess: () -> Unit
    ) {
        val editing = _editingMessage.value
        val replying = _replyingTo.value
        viewModelScope.launch {
            _isSending.value = true
            val result = if (editing != null) {
                repository.editMessage(conversationId, editing.id, text)
            } else {
                repository.sendTextMessage(conversation, senderProfile, text, replying)
            }
            result.onSuccess {
                _isSending.value = false
                _editingMessage.value = null
                _replyingTo.value = null
                onSuccess()
            }.onFailure { err ->
                _isSending.value = false
                _errorBanner.value = err.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun sendImage(
        conversation: Conversation,
        senderProfile: UserProfile,
        caption: String,
        imageBase64: String,
        onSuccess: () -> Unit
    ) {
        val replying = _replyingTo.value
        viewModelScope.launch {
            _isSending.value = true
            repository.sendImageMessage(conversation, senderProfile, caption, imageBase64, replying)
                .onSuccess {
                    _isSending.value = false
                    _replyingTo.value = null
                    onSuccess()
                }
                .onFailure { err ->
                    _isSending.value = false
                    _errorBanner.value = err.localizedMessage ?: "Failed to send image"
                }
        }
    }

    fun sendPoll(
        conversation: Conversation,
        senderProfile: UserProfile,
        question: String,
        options: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSending.value = true
            repository.sendPollMessage(conversation, senderProfile, question, options)
                .onSuccess {
                    _isSending.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _isSending.value = false
                    _errorBanner.value = err.localizedMessage ?: "Failed to create poll"
                }
        }
    }

    fun sendVoiceNote(
        conversation: Conversation,
        senderProfile: UserProfile,
        audioBase64: String,
        durationSec: Int,
        onSuccess: () -> Unit
    ) {
        val replying = _replyingTo.value
        viewModelScope.launch {
            _isSending.value = true
            repository.sendVoiceMessage(conversation, senderProfile, audioBase64, durationSec, replying)
                .onSuccess {
                    _isSending.value = false
                    _replyingTo.value = null
                    onSuccess()
                }
                .onFailure { err ->
                    _isSending.value = false
                    _errorBanner.value = err.localizedMessage ?: "Failed to send voice message"
                }
        }
    }

    fun pinOrUnpinMessage(
        conversation: Conversation,
        message: Message?,
        onComplete: (pinned: Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.pinMessage(conversation.id, message)
                .onSuccess { onComplete(message != null) }
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to update pinned message"
                }
        }
    }

    fun forwardMessage(
        targetConversation: Conversation,
        senderProfile: UserProfile,
        original: Message,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSending.value = true
            repository.forwardMessage(targetConversation, senderProfile, original)
                .onSuccess {
                    _isSending.value = false
                    onSuccess()
                }
                .onFailure { err ->
                    _isSending.value = false
                    _errorBanner.value = err.localizedMessage ?: "Failed to forward message"
                }
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            repository.deleteMessage(conversationId, message.id)
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to delete message"
                }
        }
    }

    fun toggleReaction(message: Message, emoji: String) {
        viewModelScope.launch {
            repository.toggleReaction(conversationId, message, emoji)
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to update reaction"
                }
        }
    }

    fun votePoll(message: Message, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(conversationId, message, optionIndex)
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to submit vote"
                }
        }
    }

    fun updateGroupInfo(conversation: Conversation, title: String, topic: String) {
        viewModelScope.launch {
            repository.updateGroupInfo(conversation, title, topic)
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to update group info"
                }
        }
    }

    fun addMembers(conversation: Conversation, newMembers: List<UserProfile>) {
        viewModelScope.launch {
            repository.addGroupMembers(conversation, newMembers)
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to add members"
                }
        }
    }

    fun leaveGroup(conversation: Conversation, onLeft: () -> Unit) {
        viewModelScope.launch {
            repository.leaveGroup(conversation)
                .onSuccess { onLeft() }
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to leave group"
                }
        }
    }

    fun deleteConversation(conversation: Conversation, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
                .onSuccess { onDeleted() }
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to delete conversation"
                }
        }
    }

    private companion object {
        const val TAG = "ChatRoomVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
