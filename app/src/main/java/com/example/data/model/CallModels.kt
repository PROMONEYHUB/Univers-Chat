package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CallQualityPreset(
    val wireValue: String,
    val label: String,
    val shortBadge: String,
    val captureWidth: Int,
    val captureHeight: Int,
    val fps: Int,
    val maxVideoBitrateBps: Int,
    val maxAudioBitrateBps: Int
) {
    HD_720P(
        wireValue = "HD_720P",
        label = "720p · 30fps",
        shortBadge = "720p",
        captureWidth = 1280,
        captureHeight = 720,
        fps = 30,
        maxVideoBitrateBps = 1_500_000,
        maxAudioBitrateBps = 128_000
    ),
    FHD_1080P(
        wireValue = "FHD_1080P",
        label = "1080p · 30fps",
        shortBadge = "1080p",
        captureWidth = 1920,
        captureHeight = 1080,
        fps = 30,
        maxVideoBitrateBps = 2_500_000,
        maxAudioBitrateBps = 128_000
    ),
    HD_VOICE(
        wireValue = "HD_VOICE",
        label = "Voice",
        shortBadge = "Voice",
        captureWidth = 0,
        captureHeight = 0,
        fps = 0,
        maxVideoBitrateBps = 0,
        maxAudioBitrateBps = 128_000
    );

    companion object {
        fun fromWire(value: String?): CallQualityPreset {
            return entries.firstOrNull { it.wireValue == value } ?: HD_720P
        }
    }
}

@Keep
@Immutable
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhotoUrl: String? = null,
    val calleeId: String = "",
    val calleeName: String = "",
    val calleePhotoUrl: String? = null,
    val participantIds: List<String> = emptyList(),
    val conversationId: String = "",
    val callType: String = TYPE_VOICE,
    val qualityPreset: String = CallQualityPreset.HD_VOICE.wireValue,
    val status: String = STATUS_RINGING,
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    val endedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isVideoCall: Boolean get() = callType == TYPE_VIDEO
    val isTerminal: Boolean get() = status in TERMINAL_STATUSES

    fun isOutgoingFor(userId: String): Boolean = callerId == userId

    fun peerIdFor(userId: String): String {
        return if (callerId == userId) calleeId else callerId
    }

    fun peerNameFor(userId: String): String {
        return if (callerId == userId) calleeName.ifBlank { "Member" }
        else callerName.ifBlank { "Member" }
    }

    fun peerPhotoUrlFor(userId: String): String? {
        return if (callerId == userId) calleePhotoUrl else callerPhotoUrl
    }

    fun qualityEnum(): CallQualityPreset = CallQualityPreset.fromWire(qualityPreset)

    companion object {
        const val TYPE_VOICE = "voice"
        const val TYPE_VIDEO = "video"

        const val STATUS_RINGING = "ringing"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
        const val STATUS_ENDED = "ended"
        const val STATUS_MISSED = "missed"

        val TERMINAL_STATUSES = setOf(STATUS_DECLINED, STATUS_ENDED, STATUS_MISSED)
    }
}

@Keep
@Immutable
@IgnoreExtraProperties
data class IceCandidateSignal(
    @DocumentId val id: String = "",
    val callId: String = "",
    val senderId: String = "",
    val participantIds: List<String> = emptyList(),
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Immutable
data class CallTarget(
    val userId: String,
    val displayName: String,
    val photoUrl: String? = null,
    val conversationId: String
)
