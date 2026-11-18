package com.example.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelegramOtpTest {

    @Test
    fun backendConfigGuard_rejectsPlaceholdersAndNonHttps() {
        assertFalse(TelegramOtpClient.isBackendConfigured("https://your-backend.example.com"))
        assertFalse(TelegramOtpClient.isBackendConfigured("http://insecure.universchat.app"))
        assertFalse(TelegramOtpClient.isBackendConfigured(""))
        assertTrue(TelegramOtpClient.isBackendConfigured("https://otp.universchat.app"))
    }

    @Test
    fun buildBotDeepLink_sanitizesAtPrefixAndBuildsStartPayload() {
        val link = TelegramOtpClient.buildBotDeepLink(
            botUsername = "@UniversChatOtpBot",
            sessionId = "otp_abc123"
        )
        assertEquals("https://t.me/UniversChatOtpBot?start=verify_otp_abc123", link)
    }

    @Test
    fun parseRequestResponse_parsesOpenBotAndDirectDeliveryModes() {
        val openBotJson = """
            {
              "sessionId": "otp_9f8e7d",
              "deliveryMode": "open_bot",
              "botDeepLink": "https://t.me/UniversChatOtpBot?start=verify_otp_9f8e7d",
              "expiresInSeconds": 300
            }
        """.trimIndent()
        val parsedOpen = TelegramOtpClient.parseRequestResponse(openBotJson)
        assertEquals("otp_9f8e7d", parsedOpen.sessionId)
        assertEquals(TelegramOtpDeliveryMode.OPEN_BOT, parsedOpen.deliveryMode)
        assertEquals("https://t.me/UniversChatOtpBot?start=verify_otp_9f8e7d", parsedOpen.botDeepLink)
        assertEquals(300, parsedOpen.expiresInSeconds)

        val directJson = """
            {
              "sessionId": "otp_112233",
              "deliveryMode": "sent_directly",
              "botDeepLink": "https://t.me/UniversChatOtpBot",
              "expiresInSeconds": 180
            }
        """.trimIndent()
        val parsedDirect = TelegramOtpClient.parseRequestResponse(directJson)
        assertEquals(TelegramOtpDeliveryMode.SENT_DIRECTLY, parsedDirect.deliveryMode)
        assertEquals(180, parsedDirect.expiresInSeconds)
    }

    @Test
    fun parseVerifyResponse_extractsTokensAndUserProfile() {
        val verifyJson = """
            {
              "sessionToken": "eyJhbGciOiJIUzI1NiJ9.payload.sig",
              "firebaseCustomToken": "fb-custom-token-xyz",
              "user": {
                "uid": "user_tg_42",
                "phoneNumber": "+919876543210",
                "firstName": "Aarav",
                "lastName": "Sharma",
                "photoUrl": "https://cdn.universchat.app/avatar.jpg",
                "isNewUser": true
              }
            }
        """.trimIndent()

        val parsed = TelegramOtpClient.parseVerifyResponse(verifyJson)
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", parsed.sessionToken)
        assertEquals("fb-custom-token-xyz", parsed.firebaseCustomToken)
        assertEquals("user_tg_42", parsed.user.uid)
        assertEquals("+919876543210", parsed.user.phoneNumber)
        assertEquals("Aarav Sharma", parsed.user.displayName)
        assertTrue(parsed.user.isNewUser)
    }
}
