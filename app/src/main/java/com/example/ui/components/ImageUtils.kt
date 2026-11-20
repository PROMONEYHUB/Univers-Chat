package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    private val imageBitmapCache: LruCache<String, ImageBitmap> = object : LruCache<String, ImageBitmap>(120) {}

    private fun cacheKey(payload: String): String {
        val prefix = if (payload.length > 24) payload.substring(0, 24) else payload
        val suffix = if (payload.length > 24) payload.substring(payload.length - 24) else ""
        return "${payload.length}:${payload.hashCode()}:$prefix:$suffix"
    }

    fun encodeUriToCompressedBase64(context: Context, uri: Uri): String? {
        return try {
            val original = decodeUriToBitmap(context, uri, MAX_DIMENSION_PX) ?: return null

            val maxSide = max(original.width, original.height)
            val scaled = if (maxSide > MAX_DIMENSION_PX) {
                val scale = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                val w = (original.width * scale).roundToInt().coerceAtLeast(1)
                val h = (original.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(original, w, h, true)
            } else {
                original
            }
            if (scaled != original) original.recycle()

            val bytes = compressJpegToBudget(scaled, MAX_RAW_BYTES)
            scaled.recycle()
            if (bytes == null) return null

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Center-crops the picked image into a 1:1 square avatar, scales to at most 256x256,
     * and encodes it as a compact JPEG data URI suitable for Firestore persistence.
     */
    fun encodeAvatarToDataUri(context: Context, uri: Uri): String? {
        return try {
            val original = decodeUriToBitmap(context, uri, AVATAR_MAX_DIMENSION_PX * 2) ?: return null
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
        } catch (_: Throwable) {
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

    fun getCachedDataUriImageBitmap(dataUri: String?): ImageBitmap? {
        if (dataUri.isNullOrBlank()) return null
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex < 0 || commaIndex >= dataUri.lastIndex) return null
        return getCachedBase64ImageBitmap(dataUri.substring(commaIndex + 1))
    }

    fun getCachedBase64ImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return imageBitmapCache.get(cacheKey(base64))
    }

    fun decodeDataUriToImageBitmap(dataUri: String): ImageBitmap? {
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex < 0 || commaIndex >= dataUri.lastIndex) return null
        val payload = dataUri.substring(commaIndex + 1)
        return decodeBase64ToImageBitmap(payload)
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        val key = cacheKey(base64)
        imageBitmapCache.get(key)?.let { return it }
        val bitmap = decodeBase64ToBitmap(base64) ?: return null
        val imageBitmap = bitmap.asImageBitmap()
        imageBitmapCache.put(key, imageBitmap)
        return imageBitmap
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        val key = cacheKey(base64)
        bitmapCache.get(key)?.takeIf { !it.isRecycled }?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also { decoded ->
                bitmapCache.put(key, decoded)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeUriToBitmap(context: Context, uri: Uri, maxTargetPx: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSide = max(info.size.width, info.size.height)
                if (maxSide > maxTargetPx && maxSide > 0) {
                    val scale = maxTargetPx.toFloat() / maxSide.toFloat()
                    val targetW = (info.size.width * scale).roundToInt().coerceAtLeast(1)
                    val targetH = (info.size.height * scale).roundToInt().coerceAtLeast(1)
                    decoder.setTargetSize(targetW, targetH)
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val maxSide = max(bounds.outWidth, bounds.outHeight)
            while (maxSide / (sampleSize * 2) >= maxTargetPx && sampleSize < 32) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }
    }

    private fun compressJpegToBudget(bitmap: Bitmap, maxBytes: Int): ByteArray? {
        var current = bitmap
        var ownsCurrent = false
        try {
            repeat(3) { attempt ->
                var quality = if (attempt == 0) 82 else 68
                var bytes: ByteArray
                do {
                    val baos = ByteArrayOutputStream()
                    current.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    bytes = baos.toByteArray()
                    quality -= 10
                } while (bytes.size > maxBytes && quality >= 22)

                if (bytes.size <= maxBytes) return bytes

                val nextW = (current.width * 0.72f).roundToInt().coerceAtLeast(64)
                val nextH = (current.height * 0.72f).roundToInt().coerceAtLeast(64)
                if (nextW == current.width && nextH == current.height) return null
                val nextBitmap = Bitmap.createScaledBitmap(current, nextW, nextH, true)
                if (ownsCurrent) current.recycle()
                current = nextBitmap
                ownsCurrent = true
            }
            return null
        } finally {
            if (ownsCurrent && !current.isRecycled) {
                current.recycle()
            }
        }
    }
}
