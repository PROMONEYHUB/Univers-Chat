package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.ui.components.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compresses gallery photos into Firestore-ready Base64 strings without requiring
 * an external Firebase Storage bucket.
 */
object ChatPhotoUploader {

    suspend fun uploadChatPhoto(
        context: Context,
        imageUri: Uri,
        senderId: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0.3f)
            val encoded = ImageUtils.encodeUriToCompressedBase64(context, imageUri)
                ?: error("Could not process selected photo.")
            onProgress(1f)
            encoded
        }
    }
}
