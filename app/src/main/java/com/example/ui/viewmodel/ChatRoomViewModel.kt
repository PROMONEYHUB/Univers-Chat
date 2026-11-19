package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RecordedVoiceFile
import com.example.crypto.EncryptedPayload
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.ChatVoiceUploader
import com.example.data.repository.MessagingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface VoiceUploadState {
    data object Idle : VoiceUploadState
    data class Uploading(val progress: Float, val durationSec: Int) : VoiceUploadState
    data class Error(
        val message: String,
        val file: File,
        val durationSec: Int
    ) : VoiceUploadState
}

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

    private val _voiceUploadState = MutableStateFlow<VoiceUploadState>(VoiceUploadState.Idle)
    val voiceUploadState: StateFlow<VoiceUploadState> = _voiceUploadState.asStateFlow()

    private var voiceUploadJob: Job? = null
    private var pendingVoiceFile: File? = null

    private var isMarkedTyping = false
    private var lastTypingHeartbeatMs = 0L
    private var typingIdleJob: Job? = null
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    fun onComposerTextChanged(
        text: String,
        conversation: Conversation?,
        isBlocked: Boolean = false
    ) {
        if (conversation == null || isBlocked || conversation.participantIds.distinct().size <= 1) {
            clearTypingStatus()
            return
        }
        if (text.isBlank()) {
            clearTypingStatus()
            return
        }

        typingIdleJob?.cancel()
        typingIdleJob = viewModelScope.launch {
            delay(TYPING_IDLE_CLEAR_MS)
            if (isMarkedTyping) {
                isMarkedTyping = false
                lastTypingHeartbeatMs = 0L
                repository.setTypingStatus(conversationId, isTyping = false)
            }
        }

        val now = System.currentTimeMillis()
        if (!isMarkedTyping || (now - lastTypingHeartbeatMs) >= TYPING_HEARTBEAT_INTERVAL_MS) {
            isMarkedTyping = true
            lastTypingHeartbeatMs = now
            viewModelScope.launch {
                repository.setTypingStatus(conversationId, isTyping = true)
            }
        }
    }

    fun clearTypingStatus() {
        typingIdleJob?.cancel()
        typingIdleJob = null
        if (!isMarkedTyping) return
        isMarkedTyping = false
        lastTypingHeartbeatMs = 0L
        cleanupScope.launch {
            repository.setTypingStatus(conversationId, isTyping = false)
        }
    }

    override fun onCleared() {
        clearTypingStatus()
        cancelVoiceUpload()
        super.onCleared()
    }

    fun markRead(conversation: Conversation, sendReadReceipt: Boolean = true) {
        val hasUnread = conversation.unreadCountFor(currentUserId) > 0L
        val lastMsgMillis = conversation.lastMessageAt?.toDate()?.time ?: 0L
        val myLastReadMillis = conversation.lastReadAt[currentUserId]?.toDate()?.time ?: 0L
        val needsReceipt = sendReadReceipt &&
            conversation.lastMessageSenderId.isNotBlank() &&
            conversation.lastMessageSenderId != currentUserId &&
            lastMsgMillis > myLastReadMillis

        if (!hasUnread && !needsReceipt) return
        viewModelScope.launch {
            repository.markConversationRead(conversation, sendReadReceipt)
        }
    }

    fun sendOrEditText(
        conversation: Conversation,
        senderProfile: UserProfile,
        text: String,
        encryptedPayload: EncryptedPayload? = null,
        onSuccess: () -> Unit
    ) {
        val editing = _editingMessage.value
        val replying = _replyingTo.value
        typingIdleJob?.cancel()
        typingIdleJob = null
        isMarkedTyping = false
        lastTypingHeartbeatMs = 0L
        viewModelScope.launch {
            _isSending.value = true
            val result = if (editing != null) {
                repository.editMessage(conversationId, editing.id, text, encryptedPayload)
            } else {
                repository.sendTextMessage(conversation, senderProfile, text, replying, encryptedPayload)
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

    fun uploadAndSendVoiceNote(
        conversation: Conversation,
        senderProfile: UserProfile,
        recorded: RecordedVoiceFile,
        onSuccess: () -> Unit = {}
    ) {
        val replying = _replyingTo.value
        voiceUploadJob?.cancel()
        if (pendingVoiceFile != null && pendingVoiceFile != recorded.file) {
            runCatching { pendingVoiceFile?.delete() }
        }
        pendingVoiceFile = recorded.file
        _voiceUploadState.value = VoiceUploadState.Uploading(
            progress = 0f,
            durationSec = recorded.durationSec
        )

        voiceUploadJob = viewModelScope.launch {
            _isSending.value = true
            try {
                val uploadResult = ChatVoiceUploader.uploadVoiceAudio(
                    audioFile = recorded.file,
                    senderId = currentUserId,
                    conversationId = conversation.id,
                    onProgress = { fraction ->
                        _voiceUploadState.value = VoiceUploadState.Uploading(
                            progress = fraction.coerceIn(0f, 1f),
                            durationSec = recorded.durationSec
                        )
                    }
                )
                val downloadUrl = uploadResult.getOrElse { err ->
                    if (err is CancellationException) throw err
                    _isSending.value = false
                    _voiceUploadState.value = VoiceUploadState.Error(
                        message = err.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Could not upload voice message",
                        file = recorded.file,
                        durationSec = recorded.durationSec
                    )
                    return@launch
                }

                repository.sendVoiceMessage(
                    conversation = conversation,
                    senderProfile = senderProfile,
                    audioUrl = downloadUrl,
                    durationSec = recorded.durationSec,
                    replyTo = replying
                ).onSuccess {
                    runCatching { recorded.file.delete() }
                    if (pendingVoiceFile == recorded.file) pendingVoiceFile = null
                    _isSending.value = false
                    _voiceUploadState.value = VoiceUploadState.Idle
                    _replyingTo.value = null
                    onSuccess()
                }.onFailure { err ->
                    _isSending.value = false
                    _voiceUploadState.value = VoiceUploadState.Error(
                        message = err.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Failed to send voice message",
                        file = recorded.file,
                        durationSec = recorded.durationSec
                    )
                }
            } catch (_: CancellationException) {
                _isSending.value = false
                _voiceUploadState.value = VoiceUploadState.Idle
            }
        }
    }

    fun retryVoiceUpload(
        conversation: Conversation,
        senderProfile: UserProfile,
        onSuccess: () -> Unit = {}
    ) {
        val errState = _voiceUploadState.value as? VoiceUploadState.Error ?: return
        uploadAndSendVoiceNote(
            conversation = conversation,
            senderProfile = senderProfile,
            recorded = RecordedVoiceFile(file = errState.file, durationSec = errState.durationSec),
            onSuccess = onSuccess
        )
    }

    fun cancelVoiceUpload() {
        voiceUploadJob?.cancel()
        voiceUploadJob = null
        val errFile = (_voiceUploadState.value as? VoiceUploadState.Error)?.file
        runCatching { errFile?.delete() }
        runCatching { pendingVoiceFile?.delete() }
        pendingVoiceFile = null
        _isSending.value = false
        _voiceUploadState.value = VoiceUploadState.Idle
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
        const val TYPING_HEARTBEAT_INTERVAL_MS = 4000L
        const val TYPING_IDLE_CLEAR_MS = 5500L
    }
}
