package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calls.CallAudioController
import com.example.calls.WebRtcCallEngine
import com.example.data.model.CallQualityPreset
import com.example.data.model.CallSession
import com.example.data.model.CallTarget
import com.example.data.model.UserProfile
import com.example.data.repository.CallSignalingRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.ConcurrentHashMap

enum class CallConnectionPhase(val label: String) {
    IDLE("Idle"),
    DIALING("Dialing HD…"),
    RINGING_OUTGOING("Ringing…"),
    CONNECTING("Connecting encrypted stream…"),
    CONNECTED("HD Connected"),
    RECONNECTING("Reconnecting…"),
    ENDED("Call ended")
}

data class ActiveCallUiState(
    val session: CallSession? = null,
    val phase: CallConnectionPhase = CallConnectionPhase.IDLE,
    val isOutgoing: Boolean = true,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val hasRemoteVideo: Boolean = false,
    val isMinimized: Boolean = false,
    val qualityPreset: CallQualityPreset = CallQualityPreset.HD_720P,
    val durationSeconds: Long = 0L,
    val endReasonBanner: String? = null
)

class CallViewModel(
    appContext: Context,
    private val repository: CallSignalingRepository,
    val currentUserId: String
) : AndroidViewModel(appContext.applicationContext as Application) {

    private val audioController = CallAudioController(getApplication())

    private val _incomingCall = MutableStateFlow<CallSession?>(null)
    val incomingCall: StateFlow<CallSession?> = _incomingCall.asStateFlow()

    private val _callState = MutableStateFlow(ActiveCallUiState())
    val callState: StateFlow<ActiveCallUiState> = _callState.asStateFlow()

    private val _callNotice = MutableStateFlow<String?>(null)
    val callNotice: StateFlow<String?> = _callNotice.asStateFlow()

    private data class PendingLocalIceCandidate(
        val callId: String,
        val participantIds: List<String>,
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val candidate: String
    )

    private var rtcEngine: WebRtcCallEngine? = null
    private var callSetupJob: Job? = null
    private var iceSendParentJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var candidatesObserverJob: Job? = null
    private var durationTimerJob: Job? = null
    private var outgoingTimeoutJob: Job? = null

    private val candidateBufferLock = Any()
    private val pendingLocalCandidates = ArrayDeque<PendingLocalIceCandidate>()
    @Volatile
    private var activeCallId: String? = null
    @Volatile
    private var isCallSessionPublished: Boolean = false

    private val appliedCandidateIds = ConcurrentHashMap.newKeySet<String>()
    private var appliedRemoteAnswer = false
    private val blockedCallerIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        observeIncomingCalls()
    }

    fun updateBlockedUserIds(ids: Set<String>) {
        blockedCallerIds.value = ids
        val currentIncoming = _incomingCall.value
        if (currentIncoming != null && currentIncoming.callerId in ids) {
            _incomingCall.value = null
            audioController.stopRinging()
        }
    }

    private fun observeIncomingCalls() {
        viewModelScope.launch {
            repository.observeIncomingRingingCalls(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Incoming call listener error", firestoreError ?: error)
                    _incomingCall.value = null
                    audioController.stopRinging()
                }
                .collect { ringingList ->
                    val currentActiveId = _callState.value.session?.id
                    val blocked = blockedCallerIds.value
                    val candidateCall = ringingList.firstOrNull { call ->
                        call.id != currentActiveId && !call.isTerminal && call.callerId !in blocked
                    }
                    val prevIncoming = _incomingCall.value
                    _incomingCall.value = candidateCall
                    if (candidateCall != null && currentActiveId == null) {
                        if (prevIncoming?.id != candidateCall.id) {
                            audioController.startRinging()
                        }
                    } else {
                        audioController.stopRinging()
                    }
                }
        }
    }

    fun startOutgoingCall(
        callerProfile: UserProfile,
        target: CallTarget,
        isVideoCall: Boolean,
        preferredVideoPreset: CallQualityPreset = CallQualityPreset.HD_720P
    ) {
        if (_callState.value.session != null) {
            _callNotice.value = "Finish your current call before starting another."
            return
        }
        if (target.userId == currentUserId) {
            _callNotice.value = "You cannot start a call with yourself."
            return
        }

        val preset = if (isVideoCall) preferredVideoPreset else CallQualityPreset.HD_VOICE
        val callType = if (isVideoCall) CallSession.TYPE_VIDEO else CallSession.TYPE_VOICE
        val callId = repository.allocateCallId()

        audioController.stopRinging()
        audioController.startCallAudio(speakerOnByDefault = isVideoCall)

        appliedCandidateIds.clear()
        appliedRemoteAnswer = false
        synchronized(candidateBufferLock) {
            pendingLocalCandidates.clear()
            activeCallId = callId
            isCallSessionPublished = false
        }
        iceSendParentJob?.cancel()
        iceSendParentJob = SupervisorJob(viewModelScope.coroutineContext[Job])

        val engine = try {
            createEngine(
                callId = callId,
                participantIds = listOf(currentUserId, target.userId),
                isVideoCall = isVideoCall,
                preset = preset
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize WebRTC engine for outgoing call", e)
            _callNotice.value = "Could not initialize call hardware."
            cleanupCallResources()
            return
        }
        rtcEngine = engine

        val provisionalSession = CallSession(
            id = callId,
            callerId = currentUserId,
            callerName = callerProfile.displayName,
            callerPhotoUrl = callerProfile.photoUrl,
            calleeId = target.userId,
            calleeName = target.displayName,
            calleePhotoUrl = target.photoUrl,
            participantIds = listOf(currentUserId, target.userId),
            conversationId = target.conversationId,
            callType = callType,
            qualityPreset = preset.wireValue,
            status = CallSession.STATUS_RINGING
        )

        _callState.value = ActiveCallUiState(
            session = provisionalSession,
            phase = CallConnectionPhase.DIALING,
            isOutgoing = true,
            isMuted = false,
            isSpeakerOn = isVideoCall,
            isCameraEnabled = isVideoCall,
            isFrontCamera = true,
            hasRemoteVideo = false,
            isMinimized = false,
            qualityPreset = preset,
            durationSeconds = 0L
        )

        callSetupJob?.cancel()
        callSetupJob = viewModelScope.launch {
            try {
                val offerSdp = withContext(Dispatchers.Default) {
                    engine.createOfferSdp()
                }
                if (activeCallId != callId) return@launch
                val created = repository.createCallSession(
                    callId = callId,
                    callerProfile = callerProfile,
                    target = target,
                    callType = callType,
                    qualityPreset = preset,
                    offerSdp = offerSdp
                ).getOrThrow()

                if (activeCallId != callId) {
                    repository.endCall(callId, asMissed = true)
                    return@launch
                }

                markCallSessionPublishedAndFlushCandidates(callId)

                _callState.update {
                    it.copy(
                        session = created,
                        phase = CallConnectionPhase.RINGING_OUTGOING
                    )
                }
                startSignalingObservers(callId)
                startOutgoingNoAnswerTimer(callId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initiate outgoing call", e)
                _callNotice.value = e.localizedMessage ?: "Could not start HD call."
                cleanupCallResources()
            }
        }
    }

    fun acceptIncomingCall(callSession: CallSession) {
        if (_callState.value.session != null) {
            _callNotice.value = "You are already on another call."
            return
        }
        val offerSdp = callSession.offerSdp
        if (offerSdp.isNullOrBlank()) {
            _callNotice.value = "Call offer expired."
            viewModelScope.launch { repository.declineCall(callSession.id) }
            return
        }

        _incomingCall.value = null
        audioController.stopRinging()
        audioController.startCallAudio(speakerOnByDefault = callSession.isVideoCall)

        appliedCandidateIds.clear()
        appliedRemoteAnswer = true // Caller already sent offer; we are answering
        synchronized(candidateBufferLock) {
            pendingLocalCandidates.clear()
            activeCallId = callSession.id
            isCallSessionPublished = false
        }
        iceSendParentJob?.cancel()
        iceSendParentJob = SupervisorJob(viewModelScope.coroutineContext[Job])

        val preset = callSession.qualityEnum()
        val engine = try {
            createEngine(
                callId = callSession.id,
                participantIds = callSession.participantIds,
                isVideoCall = callSession.isVideoCall,
                preset = preset
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize WebRTC engine for incoming call", e)
            _callNotice.value = "Could not initialize call hardware."
            cleanupCallResources()
            return
        }
        rtcEngine = engine

        _callState.value = ActiveCallUiState(
            session = callSession,
            phase = CallConnectionPhase.CONNECTING,
            isOutgoing = false,
            isMuted = false,
            isSpeakerOn = callSession.isVideoCall,
            isCameraEnabled = callSession.isVideoCall,
            isFrontCamera = true,
            hasRemoteVideo = false,
            isMinimized = false,
            qualityPreset = preset,
            durationSeconds = 0L
        )

        callSetupJob?.cancel()
        callSetupJob = viewModelScope.launch {
            try {
                val answerSdp = withContext(Dispatchers.Default) {
                    engine.acceptRemoteOfferAndCreateAnswer(offerSdp)
                }
                if (activeCallId != callSession.id) return@launch
                repository.acceptCall(callSession.id, answerSdp).getOrThrow()
                if (activeCallId != callSession.id) return@launch
                markCallSessionPublishedAndFlushCandidates(callSession.id)
                startSignalingObservers(callSession.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to accept incoming call", e)
                _callNotice.value = e.localizedMessage ?: "Could not connect HD call."
                cleanupCallResources()
            }
        }
    }

    fun declineIncomingCall(callSession: CallSession) {
        _incomingCall.value = null
        audioController.stopRinging()
        viewModelScope.launch {
            repository.declineCall(callSession.id)
        }
    }

    fun hangUp() {
        val current = _callState.value
        val callId = current.session?.id
        val wasPublished = isCallSessionPublished || !current.isOutgoing
        val wasNeverAnswered = current.isOutgoing &&
            (current.phase == CallConnectionPhase.DIALING || current.phase == CallConnectionPhase.RINGING_OUTGOING)

        cleanupCallResources()

        if (!callId.isNullOrBlank() && wasPublished) {
            viewModelScope.launch {
                repository.endCall(callId, asMissed = wasNeverAnswered)
            }
        }
    }

    fun toggleMute() {
        val next = !_callState.value.isMuted
        rtcEngine?.setMicrophoneMuted(next)
        _callState.update { it.copy(isMuted = next) }
    }

    fun toggleSpeaker() {
        val next = !_callState.value.isSpeakerOn
        audioController.setSpeakerphone(next)
        _callState.update { it.copy(isSpeakerOn = next) }
    }

    fun toggleCamera() {
        val session = _callState.value.session ?: return
        if (!session.isVideoCall) return
        val next = !_callState.value.isCameraEnabled
        rtcEngine?.setCameraEnabled(next)
        _callState.update { it.copy(isCameraEnabled = next) }
    }

    fun switchCamera() {
        val session = _callState.value.session ?: return
        if (!session.isVideoCall) return
        rtcEngine?.switchCamera()
    }

    fun cycleVideoQualityPreset() {
        val current = _callState.value
        val session = current.session ?: return
        if (!session.isVideoCall) return
        val next = if (current.qualityPreset == CallQualityPreset.FHD_1080P) {
            CallQualityPreset.HD_720P
        } else {
            CallQualityPreset.FHD_1080P
        }
        rtcEngine?.updateQualityPreset(next)
        _callState.update { it.copy(qualityPreset = next) }
        viewModelScope.launch {
            repository.updateQualityPreset(session.id, next)
        }
    }

    fun setMinimized(minimized: Boolean) {
        _callState.update { it.copy(isMinimized = minimized) }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        rtcEngine?.attachLocalRenderer(renderer)
    }

    fun detachLocalRenderer(renderer: SurfaceViewRenderer) {
        rtcEngine?.detachLocalRenderer(renderer)
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        rtcEngine?.attachRemoteRenderer(renderer)
    }

    fun detachRemoteRenderer(renderer: SurfaceViewRenderer) {
        rtcEngine?.detachRemoteRenderer(renderer)
    }

    fun clearCallNotice() {
        _callNotice.value = null
    }

    private fun enqueueOrSendLocalIceCandidate(
        callId: String,
        participantIds: List<String>,
        sdpMid: String,
        sdpMLineIndex: Int,
        candidate: String
    ) {
        val trimmedCandidate = candidate.trim()
        if (trimmedCandidate.isEmpty()) return

        val pending = PendingLocalIceCandidate(
            callId = callId,
            participantIds = participantIds,
            sdpMid = sdpMid,
            sdpMLineIndex = sdpMLineIndex,
            candidate = trimmedCandidate
        )

        val shouldSendNow = synchronized(candidateBufferLock) {
            if (activeCallId != callId) return
            if (!isCallSessionPublished) {
                if (pendingLocalCandidates.size < MAX_BUFFERED_ICE_CANDIDATES) {
                    pendingLocalCandidates.addLast(pending)
                }
                false
            } else {
                true
            }
        }

        if (shouldSendNow) {
            dispatchLocalIceCandidate(pending)
        }
    }

    private fun markCallSessionPublishedAndFlushCandidates(callId: String) {
        val toFlush = synchronized(candidateBufferLock) {
            if (activeCallId != callId) return
            isCallSessionPublished = true
            val drained = pendingLocalCandidates.toList()
            pendingLocalCandidates.clear()
            drained
        }
        toFlush.forEach { dispatchLocalIceCandidate(it) }
    }

    private fun dispatchLocalIceCandidate(item: PendingLocalIceCandidate) {
        val parentJob = iceSendParentJob ?: return
        if (!parentJob.isActive || activeCallId != item.callId) return
        viewModelScope.launch(parentJob) {
            if (activeCallId != item.callId || !isCallSessionPublished) return@launch
            repository.sendIceCandidate(
                callId = item.callId,
                participantIds = item.participantIds,
                sdpMid = item.sdpMid,
                sdpMLineIndex = item.sdpMLineIndex,
                candidate = item.candidate
            )
        }
    }

    private fun createEngine(
        callId: String,
        participantIds: List<String>,
        isVideoCall: Boolean,
        preset: CallQualityPreset
    ): WebRtcCallEngine {
        return WebRtcCallEngine(
            context = getApplication(),
            isVideoCall = isVideoCall,
            initialQualityPreset = preset,
            onLocalIceCandidate = { sdpMid, sdpMLineIndex, candidate ->
                enqueueOrSendLocalIceCandidate(
                    callId = callId,
                    participantIds = participantIds,
                    sdpMid = sdpMid,
                    sdpMLineIndex = sdpMLineIndex,
                    candidate = candidate
                )
            },
            onConnectionStateChanged = { peerState ->
                viewModelScope.launch {
                    handlePeerConnectionState(peerState)
                }
            },
            onRemoteVideoTrackChanged = { hasVideo ->
                _callState.update { it.copy(hasRemoteVideo = hasVideo) }
            },
            onFacingChanged = { isFront ->
                _callState.update { it.copy(isFrontCamera = isFront) }
            }
        )
    }

    private suspend fun handlePeerConnectionState(peerState: PeerConnection.PeerConnectionState) {
        when (peerState) {
            PeerConnection.PeerConnectionState.CONNECTING -> {
                _callState.update {
                    if (it.phase != CallConnectionPhase.CONNECTED) {
                        it.copy(phase = CallConnectionPhase.CONNECTING)
                    } else it
                }
            }
            PeerConnection.PeerConnectionState.CONNECTED -> {
                outgoingTimeoutJob?.cancel()
                _callState.update { it.copy(phase = CallConnectionPhase.CONNECTED) }
                startDurationTimerIfNeeded()
            }
            PeerConnection.PeerConnectionState.DISCONNECTED -> {
                _callState.update { it.copy(phase = CallConnectionPhase.RECONNECTING) }
            }
            PeerConnection.PeerConnectionState.FAILED -> {
                _callNotice.value = "Connection lost. Ending call."
                hangUp()
            }
            PeerConnection.PeerConnectionState.CLOSED -> {
                cleanupCallResources()
            }
            else -> Unit
        }
    }

    private fun startSignalingObservers(callId: String) {
        sessionObserverJob?.cancel()
        sessionObserverJob = viewModelScope.launch {
            repository.observeCallSession(callId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Call session observer error", firestoreError ?: error)
                    _callNotice.value = "Call connection interrupted."
                    cleanupCallResources()
                }
                .collect { remoteSession ->
                    if (remoteSession == null) {
                        cleanupCallResources()
                        return@collect
                    }
                    _callState.update {
                        it.copy(
                            session = remoteSession,
                            qualityPreset = remoteSession.qualityEnum()
                        )
                    }

                    if (remoteSession.status == CallSession.STATUS_ACCEPTED &&
                        !appliedRemoteAnswer &&
                        !remoteSession.answerSdp.isNullOrBlank()
                    ) {
                        appliedRemoteAnswer = true
                        outgoingTimeoutJob?.cancel()
                        _callState.update { it.copy(phase = CallConnectionPhase.CONNECTING) }
                        val engine = rtcEngine
                        if (engine != null) {
                            runCatching {
                                withContext(Dispatchers.Default) {
                                    engine.applyRemoteAnswer(remoteSession.answerSdp)
                                }
                            }.onFailure { err ->
                                Log.w(TAG, "Failed to apply remote answer", err)
                            }
                        }
                    }

                    if (remoteSession.isTerminal) {
                        val reason = when (remoteSession.status) {
                            CallSession.STATUS_DECLINED -> "${remoteSession.peerNameFor(currentUserId)} declined the call"
                            CallSession.STATUS_MISSED -> "No answer"
                            else -> "Call ended"
                        }
                        if (remoteSession.endedBy != currentUserId) {
                            _callNotice.value = reason
                        }
                        cleanupCallResources()
                    }
                }
        }

        candidatesObserverJob?.cancel()
        candidatesObserverJob = viewModelScope.launch {
            repository.observeRemoteCandidates(callId, currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Remote ICE candidates observer error", firestoreError ?: error)
                }
                .collect { candidates ->
                    val engine = rtcEngine ?: return@collect
                    candidates.forEach { candidate ->
                        if (candidate.id.isNotBlank() && appliedCandidateIds.add(candidate.id)) {
                            engine.addRemoteIceCandidate(candidate)
                        }
                    }
                }
        }
    }

    private fun startDurationTimerIfNeeded() {
        if (durationTimerJob?.isActive == true) return
        durationTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _callState.update { it.copy(durationSeconds = it.durationSeconds + 1L) }
            }
        }
    }

    private fun startOutgoingNoAnswerTimer(callId: String) {
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = viewModelScope.launch {
            delay(45_000L)
            val current = _callState.value
            if (current.session?.id == callId &&
                (current.phase == CallConnectionPhase.DIALING ||
                    current.phase == CallConnectionPhase.RINGING_OUTGOING)
            ) {
                _callNotice.value = "No answer"
                cleanupCallResources()
                repository.endCall(callId, asMissed = true)
            }
        }
    }

    private fun cleanupCallResources() {
        synchronized(candidateBufferLock) {
            activeCallId = null
            isCallSessionPublished = false
            pendingLocalCandidates.clear()
        }
        callSetupJob?.cancel()
        callSetupJob = null
        iceSendParentJob?.cancel()
        iceSendParentJob = null
        outgoingTimeoutJob?.cancel()
        outgoingTimeoutJob = null
        durationTimerJob?.cancel()
        durationTimerJob = null
        sessionObserverJob?.cancel()
        sessionObserverJob = null
        candidatesObserverJob?.cancel()
        candidatesObserverJob = null

        val engineToDispose = rtcEngine
        rtcEngine = null
        if (engineToDispose != null) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { engineToDispose.dispose() }
                    .onFailure { Log.w(TAG, "Error disposing WebRTC engine", it) }
            }
        }
        audioController.stopCallAudio()

        appliedCandidateIds.clear()
        appliedRemoteAnswer = false
        _callState.value = ActiveCallUiState()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupCallResources()
    }

    private companion object {
        const val TAG = "CallViewModel"
        const val MAX_BUFFERED_ICE_CANDIDATES = 64
    }
}
