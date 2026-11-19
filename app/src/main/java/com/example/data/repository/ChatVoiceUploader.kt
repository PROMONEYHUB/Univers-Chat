package com.example.data.repository

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Uploads recorded AAC voice messages (`.m4a`) to Firebase Storage and returns the
 * HTTPS download URL to send in chat.
 */
object ChatVoiceUploader {

    suspend fun uploadVoiceAudio(
        audioFile: File,
        senderId: String,
        conversationId: String = "chat",
        storage: FirebaseStorage = Firebase.storage,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(audioFile.exists() && audioFile.length() > 0L) {
                "Recorded audio file is missing or empty."
            }

            val safeOwner = senderId
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .ifBlank { "anonymous" }
            val safeConversation = conversationId
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .ifBlank { "chat" }
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.m4a"

            val storageRef = storage.reference
                .child("voice_messages")
                .child(safeOwner)
                .child(safeConversation)
                .child(fileName)

            val metadata = storageMetadata {
                contentType = "audio/mp4"
            }

            val uploadTask = storageRef.putFile(Uri.fromFile(audioFile), metadata)

            suspendCancellableCoroutine<Unit> { continuation ->
                uploadTask
                    .addOnProgressListener { snapshot ->
                        val total = snapshot.totalByteCount
                        if (total > 0L) {
                            val fraction = (snapshot.bytesTransferred.toFloat() / total.toFloat())
                                .coerceIn(0f, 1f)
                            onProgress(fraction)
                        }
                    }
                    .addOnSuccessListener {
                        onProgress(1f)
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(error))
                        }
                    }

                continuation.invokeOnCancellation {
                    uploadTask.cancel()
                }
            }

            suspendCancellableCoroutine<String> { continuation ->
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        if (continuation.isActive) {
                            continuation.resume(downloadUri.toString())
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(error))
                        }
                    }
            }
        }
    }
}
