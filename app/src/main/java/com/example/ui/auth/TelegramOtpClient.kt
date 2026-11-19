package com.example.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

enum class TelegramOtpDeliveryMode(val wireValue: String) {
    SENT_DIRECTLY("sent_directly"),
    OPEN_BOT("open_bot");

    companion object {
        fun fromWire(value: String?): TelegramOtpDeliveryMode = when (value?.lowercase()) {
            "sent_directly" -> SENT_DIRECTLY
            else -> OPEN_BOT
        }
    }
}

data class TelegramOtpRequestResponse(
    val sessionId: String,
    val deliveryMode: TelegramOtpDeliveryMode,
    val botDeepLink: String,
    val expiresInSeconds: Int
)

data class TelegramOtpUser(
    val uid: String,
    val phoneNumber: String,
    val displayName: String,
    val photoUrl: String,
    val isNewUser: Boolean
)

data class TelegramOtpVerifyResponse(
    val sessionToken: String,
    val firebaseCustomToken: String?,
    val user: TelegramOtpUser
)

class TelegramOtpException(
    message: String,
    val httpStatus: Int? = null,
    val isRetryable: Boolean = true,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Handles Telegram Bot OTP verification against our Node.js backend:
 *  - POST {baseUrl}/api/auth/telegram/request
 *  - POST {baseUrl}/api/auth/telegram/verify
 * The Telegram Bot Token never touches the Android client; it lives strictly on the backend.
 */
object TelegramOtpClient {
    const val DEFAULT_BACKEND_URL = "https://univers-chat.onrender.com"
    private const val PLACEHOLDER_BACKEND = "your-backend.example.com"
    private const val PREFS_NAME = "univers_telegram_otp_session"
    private const val KEY_SESSION_JWT = "session_jwt"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Resolves the active HTTPS OTP backend URL, falling back to the deployed Render
     * service when BuildConfig has a blank or placeholder URL.
     */
    fun resolveBackendUrl(rawUrl: String = BuildConfig.TELEGRAM_OTP_BACKEND_URL): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty() || trimmed.contains(PLACEHOLDER_BACKEND, ignoreCase = true)) {
            return DEFAULT_BACKEND_URL
        }
        return trimmed
    }

    fun isBackendConfigured(baseUrl: String = resolveBackendUrl()): Boolean {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return false
        if (trimmed.contains(PLACEHOLDER_BACKEND, ignoreCase = true)) return false
        return trimmed.startsWith("https://", ignoreCase = true)
    }

    /**
     * Sends a lightweight background GET /health ping so Render's free-tier container
     * starts spinning up before the user taps "Get OTP on Telegram Bot".
     */
    suspend fun warmUpBackend(baseUrl: String = resolveBackendUrl()) {
        val resolved = resolveBackendUrl(baseUrl)
        if (!isBackendConfigured(resolved)) return
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$resolved/health")
                    .get()
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(request).execute().close()
            }
        }
    }

    fun buildBotDeepLink(
        botUsername: String = BuildConfig.TELEGRAM_BOT_USERNAME,
        sessionId: String
    ): String {
        val sanitizedBot = botUsername.trim().removePrefix("@")
        val sanitizedSession = sessionId.trim()
        if (sanitizedBot.isBlank() || sanitizedSession.isBlank()) return ""
        return "https://t.me/$sanitizedBot?start=verify_$sanitizedSession"
    }

    suspend fun requestOtp(
        phoneNumberE164: String,
        displayName: String? = null,
        baseUrl: String = resolveBackendUrl()
    ): Result<TelegramOtpRequestResponse> = runCatching {
        val resolvedBase = resolveBackendUrl(baseUrl)
        if (!isBackendConfigured(resolvedBase)) {
            throw TelegramOtpException(
                message = "OTP server URL must be a valid HTTPS endpoint.",
                isRetryable = false
            )
        }

        val endpoint = "$resolvedBase/api/auth/telegram/request"
        val payload = JSONObject().apply {
            put("phoneNumber", phoneNumberE164.trim())
            if (!displayName.isNullOrBlank()) {
                put("displayName", displayName.trim().take(60))
            }
        }

        val responseText = executeJsonPost(endpoint, payload)
        parseRequestResponse(responseText)
    }

    suspend fun verifyOtpAndSignIn(
        context: Context,
        sessionId: String,
        phoneNumberE164: String,
        otpCode: String,
        fallbackDisplayName: String? = null,
        baseUrl: String = resolveBackendUrl()
    ): Result<TelegramOtpVerifyResponse> = runCatching {
        val resolvedBase = resolveBackendUrl(baseUrl)
        if (!isBackendConfigured(resolvedBase)) {
            throw TelegramOtpException(
                message = "Telegram Bot OTP service URL must be a valid HTTPS endpoint.",
                isRetryable = false
            )
        }

        val endpoint = "$resolvedBase/api/auth/telegram/verify"
        val payload = JSONObject().apply {
            put("sessionId", sessionId.trim())
            put("phoneNumber", phoneNumberE164.trim())
            put("otp", otpCode.filter { it.isDigit() }.take(6))
            if (!fallbackDisplayName.isNullOrBlank()) {
                put("displayName", fallbackDisplayName.trim().take(60))
            }
        }

        val responseText = executeJsonPost(endpoint, payload)
        val parsed = parseVerifyResponse(responseText)

        saveSessionJwt(context, parsed.sessionToken)

        val resolvedName = parsed.user.displayName
            .ifBlank { fallbackDisplayName?.trim().orEmpty() }
            .take(60)
            .takeIf { it.isNotBlank() }
        val resolvedPhoto = parsed.user.photoUrl
            .take(500)
            .takeIf { it.isNotBlank() }

        AuthProfilePrefill.pendingDisplayName = resolvedName
        AuthProfilePrefill.pendingPhotoUrl = resolvedPhoto
        AuthProfilePrefill.pendingPhoneNumber = parsed.user.phoneNumber.ifBlank { phoneNumberE164.trim() }

        val customToken = parsed.firebaseCustomToken
        if (customToken.isNullOrBlank()) {
            throw TelegramOtpException(
                message = "Backend verified OTP, but firebaseCustomToken is required to unlock cloud chats.",
                isRetryable = false
            )
        }

        val authResult = Firebase.auth.signInWithCustomToken(customToken).await()
        val firebaseUser = authResult.user
        if (firebaseUser != null && (resolvedName != null || resolvedPhoto != null)) {
            runCatching {
                firebaseUser.updateProfile(
                    userProfileChangeRequest {
                        if (firebaseUser.displayName.isNullOrBlank() && resolvedName != null) {
                            displayName = resolvedName
                        }
                        if (firebaseUser.photoUrl == null && resolvedPhoto != null) {
                            photoUri = Uri.parse(resolvedPhoto)
                        }
                    }
                ).await()
            }
        }
        parsed
    }.onFailure {
        AuthProfilePrefill.pendingDisplayName = null
        AuthProfilePrefill.pendingPhotoUrl = null
        AuthProfilePrefill.pendingPhoneNumber = null
    }

    fun openTelegramBot(context: Context, deepLink: String): Boolean {
        val url = deepLink.trim()
        if (url.isBlank()) return false

        val nativeTgUri = buildNativeTelegramUri(url)
        if (nativeTgUri != null) {
            val nativeIntent = Intent(Intent.ACTION_VIEW, nativeTgUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(nativeIntent) }.isSuccess) {
                return true
            }
        }

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(webIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun buildNativeTelegramUri(httpsDeepLink: String): Uri? {
        val parsed = runCatching { Uri.parse(httpsDeepLink) }.getOrNull() ?: return null
        val host = parsed.host?.lowercase().orEmpty()
        if (host != "t.me" && host != "telegram.me") return null
        val domain = parsed.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val startParam = parsed.getQueryParameter("start")
        val builder = Uri.Builder()
            .scheme("tg")
            .authority("resolve")
            .appendQueryParameter("domain", domain)
        if (!startParam.isNullOrBlank()) {
            builder.appendQueryParameter("start", startParam)
        }
        return builder.build()
    }

    fun clearSavedSessionJwt(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSION_JWT)
            .apply()
    }

    private fun saveSessionJwt(context: Context, jwt: String) {
        if (jwt.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION_JWT, jwt)
            .apply()
    }

    private suspend fun executeJsonPost(endpoint: String, payload: JSONObject): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val serverError = extractErrorMessage(rawBody)
                        val friendlyMessage = when (response.code) {
                            400 -> serverError ?: "Please check your phone number and 6-digit verification code."
                            401 -> serverError ?: "Incorrect verification code. Please check your code and try again."
                            410 -> serverError ?: "This verification code has expired. Tap Resend to get a new code."
                            429 -> serverError ?: "Too many attempts. Please wait a moment and request a new Telegram OTP."
                            502, 503, 504 -> serverError ?: "OTP server is waking up from sleep on Render. Wait a few seconds and tap again."
                            in 500..599 -> serverError ?: "Telegram OTP server returned an error (HTTP ${response.code}). Please try again."
                            else -> serverError ?: "Verification failed (HTTP ${response.code})."
                        }
                        throw TelegramOtpException(
                            message = friendlyMessage,
                            httpStatus = response.code,
                            isRetryable = response.code >= 500 || response.code == 408
                        )
                    }
                    rawBody
                }
            } catch (e: TelegramOtpException) {
                throw e
            } catch (e: SocketTimeoutException) {
                throw TelegramOtpException(
                    message = "The OTP server took too long to respond (Render may still be waking up). Tap again in a few seconds.",
                    isRetryable = true,
                    cause = e
                )
            } catch (e: IOException) {
                throw TelegramOtpException(
                    message = "Network error reaching the Telegram OTP server. Check your internet and retry.",
                    isRetryable = true,
                    cause = e
                )
            }
        }

    internal fun parseRequestResponse(rawJson: String): TelegramOtpRequestResponse {
        val json = runCatching { JSONObject(rawJson) }.getOrElse { err ->
            throw TelegramOtpException("Unexpected response from verification server.", cause = err)
        }
        val sessionId = json.optString("sessionId").trim()
        if (sessionId.isEmpty()) {
            throw TelegramOtpException("Verification server did not return an active session.")
        }
        val deliveryMode = TelegramOtpDeliveryMode.fromWire(json.optString("deliveryMode"))
        val deepLink = json.optString("botDeepLink").trim()
            .ifBlank { buildBotDeepLink(sessionId = sessionId) }
        val expiresIn = json.optInt("expiresInSeconds", 300).coerceIn(30, 1800)

        return TelegramOtpRequestResponse(
            sessionId = sessionId,
            deliveryMode = deliveryMode,
            botDeepLink = deepLink,
            expiresInSeconds = expiresIn
        )
    }

    internal fun parseVerifyResponse(rawJson: String): TelegramOtpVerifyResponse {
        val json = runCatching { JSONObject(rawJson) }.getOrElse { err ->
            throw TelegramOtpException("Unexpected verification response from server.", cause = err)
        }
        val sessionToken = json.optString("sessionToken").trim()
        val firebaseCustomToken = json.optString("firebaseCustomToken").trim().ifBlank { null }
        val userObj = json.optJSONObject("user") ?: JSONObject()

        if (sessionToken.isEmpty() && firebaseCustomToken.isNullOrBlank()) {
            throw TelegramOtpException("Verification succeeded without a valid session token.")
        }

        val firstName = userObj.optString("firstName").trim()
        val lastName = userObj.optString("lastName").trim()
        val displayName = userObj.optString("displayName").trim()
            .ifBlank { listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ") }

        return TelegramOtpVerifyResponse(
            sessionToken = sessionToken,
            firebaseCustomToken = firebaseCustomToken,
            user = TelegramOtpUser(
                uid = userObj.optString("uid").trim(),
                phoneNumber = userObj.optString("phoneNumber").trim(),
                displayName = displayName,
                photoUrl = userObj.optString("photoUrl").trim(),
                isNewUser = userObj.optBoolean("isNewUser", false)
            )
        )
    }

    private fun extractErrorMessage(rawBody: String): String? {
        if (rawBody.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawBody)
            obj.optString("message").ifBlank { obj.optString("error") }.trim().ifBlank { null }
        }.getOrNull()
    }
}
