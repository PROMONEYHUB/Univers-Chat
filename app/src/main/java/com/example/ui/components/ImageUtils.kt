package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {
    const val AVATAR_DATA_URI_PREFIX = "data:image/jpeg;base64,"
    private const val MAX_DIMENSION_PX = 480
    private const val MAX_RAW_BYTES = 54_000
    private const val AVATAR_MAX_DIMENSION_PX = 256
    private const val AVATAR_MAX_RAW_BYTES = 32_000

    fun encodeUriToCompressedBase64(context: Context, uri: Uri): String? {
        return try {
            val original = decodeUriToBitmap(context, uri) ?: return null

            val maxSide = max(original.width, original.height)
            val scaled = if (maxSide > MAX_DIMENSION_PX) {
                val scale = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                val w = (original.width * scale).roundToInt().coerceAtLeast(1)
                val h = (original.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(original, w, h, true)
            } else {
                original
            }

            val bytes = compressJpegToBudget(scaled, MAX_RAW_BYTES)
            if (scaled != original) original.recycle()
            if (bytes == null) return null

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Center-crops the picked image into a 1:1 square avatar, scales to at most 256x256,
     * and encodes it as a compact JPEG data URI suitable for Firestore persistence.
     */
    fun encodeAvatarToDataUri(context: Context, uri: Uri): String? {
        return try {
            val original = decodeUriToBitmap(context, uri) ?: return null
            val side = min(original.width, original.height).coerceAtLeast(1)
            val cropX = ((original.width - side) / 2).coerceAtLeast(0)
            val cropY = ((original.height - side) / 2).coerceAtLeast(0)
            val squared = Bitmap.createBitmap(original, cropX, cropY, side, side)
            if (squared != original) original.recycle()

            val targetSize = min(side, AVATAR_MAX_DIMENSION_PX)
            val scaled = if (side > targetSize) {
                Bitmap.createScaledBitmap(squared, targetSize, targetSize, true)
            } else {
                squared
            }
            if (scaled != squared) squared.recycle()

            val bytes = compressJpegToBudget(scaled, AVATAR_MAX_RAW_BYTES)
            scaled.recycle()
            if (bytes == null) return null

            AVATAR_DATA_URI_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun isDataUri(value: String?): Boolean {
        return !value.isNullOrBlank() && value.startsWith("data:image/")
    }

    fun decodeDataUriToBitmap(dataUri: String): Bitmap? {
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex < 0 || commaIndex >= dataUri.lastIndex) return null
        val payload = dataUri.substring(commaIndex + 1)
        return decodeBase64ToBitmap(payload)
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    }

    private fun compressJpegToBudget(bitmap: Bitmap, maxBytes: Int): ByteArray? {
        var quality = 82
        var bytes: ByteArray
        do {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            bytes = baos.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality >= 22)
        return bytes.takeIf { it.size <= maxBytes }
    }
}
