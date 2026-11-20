package com.example.data.repository

import com.example.audio.VoiceNoteRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Prepares recorded AAC voice messages (`.m4a`) as compact Firestore-ready Base64 payloads
 * so voice notes persist directly in Firestore without requiring a Firebase Storage bucket.
 */
object ChatVoiceUploader {

    suspend fun uploadVoiceAudio(
        audioFile: File,
        senderId: String,
        conversationId: String = "chat",
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        onProgress(0.25f)
        VoiceNoteRecorder.encodeFileToBase64(audioFile).also { result ->
            if (result.isSuccess) {
                onProgress(1f)
            }
        }
    }
}
