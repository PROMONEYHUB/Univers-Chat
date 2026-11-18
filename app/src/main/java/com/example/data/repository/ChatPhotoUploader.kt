package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * Handles compressing and uploading gallery images to Firebase Storage and
 * returning the public HTTPS download URL to send as a chat message.
 */
object ChatPhotoUploader {

    private const val MAX_IMAGE_DIMENSION_PX = 1600
    private const val JPEG_QUALITY = 85

    suspend fun uploadChatPhoto(
        context: Context,
        imageUri: Uri,
        senderId: String,
        storage: FirebaseStorage = Firebase.storage,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val safeOwner = senderId
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
                .ifBlank { "anonymous" }
            val compressedBytes = compressImageToJpegBytes(context, imageUri)
            val extension = if (compressedBytes != null) {
                "jpg"
            } else {
                resolveFileExtension(context, imageUri)
            }
            val contentType = if (compressedBytes != null) {
                "image/jpeg"
            } else {
                context.contentResolver.getType(imageUri) ?: "image/jpeg"
            }

            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
            val storageRef = storage.reference
                .child("chat_attachments")
                .child(safeOwner)
                .child(fileName)

            val metadata = storageMetadata {
                this.contentType = contentType
            }

            val uploadTask = if (compressedBytes != null) {
                storageRef.putBytes(compressedBytes, metadata)
            } else {
                storageRef.putFile(imageUri, metadata)
            }

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

    private fun resolveFileExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"
    }

    private fun compressImageToJpegBytes(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val srcWidth = bounds.outWidth
            val srcHeight = bounds.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var inSampleSize = 1
            while (max(srcWidth / inSampleSize, srcHeight / inSampleSize) > MAX_IMAGE_DIMENSION_PX * 2) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val maxSide = max(decoded.width, decoded.height)
            val scaled = if (maxSide > MAX_IMAGE_DIMENSION_PX) {
                val ratio = MAX_IMAGE_DIMENSION_PX.toFloat() / maxSide.toFloat()
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            scaled.recycle()
            out.toByteArray()
        }.getOrNull()
    }
}
