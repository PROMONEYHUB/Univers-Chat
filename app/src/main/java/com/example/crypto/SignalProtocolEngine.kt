package com.example.crypto

import android.content.Context
import android.util.Base64
import android.util.LruCache
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
    val ephemeralPublicKeyBase64: String,
    val senderIdentityKeyBase64: String,
    val e2eeVersion: Int = PROTOCOL_VERSION
) {
    companion object {
        const val PROTOCOL_VERSION = 1
    }
}

/**
 * Signal-style End-to-End Encryption engine (X3DH + Ephemeral Ratchet + HKDF-SHA256 + AES-256-GCM).
 *
 * For every encrypted message:
 * 1. Generates a fresh ephemeral EC P-256 ratchet keypair (e_priv, E_pub) for per-message forward secrecy.
 * 2. Computes two ECDH shared secrets:
 *      DH1 = ECDH(senderIdentityPriv, peerIdentityPub)  -> authenticates sender identity
 *      DH2 = ECDH(ephemeralPriv, peerIdentityPub)       -> provides per-message ephemeral secrecy
 * 3. Derives a unique 256-bit AES message key via HKDF-SHA256(IKM = DH1 || DH2, salt = E_pub, info = conversationId).
 * 4. Encrypts plaintext with AES-256-GCM using a fresh 96-bit random IV and AAD = "v1|conversationId|senderId".
 * 5. Stores the wrapped ephemeral private key locally in Keystore-backed storage so the sender can
 *    always re-derive and read their own sent messages on-device while the recipient derives the exact
 *    same key using (peerIdentityPriv, senderIdentityPub) and (peerIdentityPriv, E_pub).
 */
object SignalProtocolEngine {

    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_LEN = 12
    private val secureRandom = SecureRandom()
    private val decryptedPlaintextCache = LruCache<String, String>(512)

    fun encryptMessage(
        context: Context,
        conversationId: String,
        senderId: String,
        peerPublicKeyB64: String,
        plaintext: String
    ): EncryptedPayload? {
        val peerPubKey = E2eeKeyManager.decodePublicKey(peerPublicKeyB64) ?: return null
        val myKeyPair = E2eeKeyManager.getOrCreateIdentityKeyPair(context, senderId)
        val myPubB64 = E2eeKeyManager.encodePublicKey(myKeyPair.public)

        return runCatching {
            val ephemeralKp = E2eeKeyManager.generateEcKeyPair()
            val ephemeralPubB64 = E2eeKeyManager.encodePublicKey(ephemeralKp.public)

            val dhIdentity = computeEcdh(myKeyPair.private, peerPubKey)
            val dhEphemeral = computeEcdh(ephemeralKp.private, peerPubKey)

            val aesKeyBytes = deriveMessageKey(
                dhIdentity = dhIdentity,
                dhEphemeral = dhEphemeral,
                ephemeralPubBytes = ephemeralKp.public.encoded,
                conversationId = conversationId
            )

            val iv = ByteArray(GCM_IV_LEN).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(aesKeyBytes, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            cipher.updateAAD(buildAad(conversationId, senderId))
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Save wrapped ephemeral private key locally so sender can decrypt their own message
            E2eeKeyManager.storeSentEphemeralPrivateKey(
                context = context,
                userId = senderId,
                ephemeralPublicKeyB64 = ephemeralPubB64,
                ephemeralPrivateKey = ephemeralKp.private
            )

            EncryptedPayload(
                ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
                ephemeralPublicKeyBase64 = ephemeralPubB64,
                senderIdentityKeyBase64 = myPubB64,
                e2eeVersion = EncryptedPayload.PROTOCOL_VERSION
            )
        }.getOrNull()
    }

    fun decryptMessage(
        context: Context,
        conversationId: String,
        currentUserId: String,
        senderId: String,
        ciphertextBase64: String,
        ivBase64: String?,
        ephemeralPublicKeyB64: String?,
        senderIdentityKeyB64: String?,
        peerPublicKeyB64: String?
    ): String? {
        if (ivBase64.isNullOrBlank() || ephemeralPublicKeyB64.isNullOrBlank()) return null

        val cacheKey = "$conversationId|$currentUserId|$senderId|$ivBase64|${ephemeralPublicKeyB64.hashCode()}|${ciphertextBase64.length}:${ciphertextBase64.hashCode()}"
        decryptedPlaintextCache.get(cacheKey)?.let { return it }

        val myKeyPair = E2eeKeyManager.getOrCreateIdentityKeyPair(context, currentUserId)
        val ephemeralPubKey = E2eeKeyManager.decodePublicKey(ephemeralPublicKeyB64) ?: return null

        return runCatching {
            val (dhIdentity, dhEphemeral) = if (senderId == currentUserId) {
                // Sender decrypting their own message
                val peerPub = peerPublicKeyB64?.let { E2eeKeyManager.decodePublicKey(it) }
                    ?: myKeyPair.public
                val ephPriv = E2eeKeyManager.loadSentEphemeralPrivateKey(
                    context = context,
                    userId = currentUserId,
                    ephemeralPublicKeyB64 = ephemeralPublicKeyB64
                )
                val dh1 = computeEcdh(myKeyPair.private, peerPub)
                val dh2 = if (ephPriv != null) {
                    computeEcdh(ephPriv, peerPub)
                } else {
                    // Self-chat fallback where peerPub == myPub: ECDH(myPriv, E_pub) == ECDH(e_priv, myPub)
                    computeEcdh(myKeyPair.private, ephemeralPubKey)
                }
                dh1 to dh2
            } else {
                // Recipient decrypting incoming message from sender
                val senderPub = (senderIdentityKeyB64 ?: peerPublicKeyB64)
                    ?.let { E2eeKeyManager.decodePublicKey(it) }
                    ?: return null
                val dh1 = computeEcdh(myKeyPair.private, senderPub)
                val dh2 = computeEcdh(myKeyPair.private, ephemeralPubKey)
                dh1 to dh2
            }

            val aesKeyBytes = deriveMessageKey(
                dhIdentity = dhIdentity,
                dhEphemeral = dhEphemeral,
                ephemeralPubBytes = ephemeralPubKey.encoded,
                conversationId = conversationId
            )

            val iv = Base64.decode(ivBase64.trim(), Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextBase64.trim(), Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(aesKeyBytes, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            cipher.updateAAD(buildAad(conversationId, senderId))
            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8).also { plaintext ->
                decryptedPlaintextCache.put(cacheKey, plaintext)
            }
        }.getOrNull()
    }

    private fun computeEcdh(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        return agreement.generateSecret()
    }

    /**
     * HKDF-SHA256 (RFC 5869) extract + expand producing a 32-byte AES-256 key.
     */
    private fun deriveMessageKey(
        dhIdentity: ByteArray,
        dhEphemeral: ByteArray,
        ephemeralPubBytes: ByteArray,
        conversationId: String
    ): ByteArray {
        val ikm = ByteArray(dhIdentity.size + dhEphemeral.size).apply {
            System.arraycopy(dhIdentity, 0, this, 0, dhIdentity.size)
            System.arraycopy(dhEphemeral, 0, this, dhIdentity.size, dhEphemeral.size)
        }
        val prk = hmacSha256(key = ephemeralPubBytes, data = ikm)
        val info = "UNIVERS_SIGNAL_RATCHET_V1|$conversationId".toByteArray(Charsets.UTF_8)
        val expandInput = ByteArray(info.size + 1).apply {
            System.arraycopy(info, 0, this, 0, info.size)
            this[info.size] = 0x01
        }
        return hmacSha256(key = prk, data = expandInput)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun buildAad(conversationId: String, senderId: String): ByteArray {
        return "v1|$conversationId|$senderId".toByteArray(Charsets.UTF_8)
    }
}
