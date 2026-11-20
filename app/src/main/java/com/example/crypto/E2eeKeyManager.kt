package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Client-side cryptographic identity & session key store for Univers Chat E2EE.
 *
 * Security model:
 * - Generates a long-term EC P-256 (secp256r1) Identity KeyPair per authenticated userId.
 * - Wraps private key material with an Android Keystore AES-256-GCM hardware-backed master key
 *   before persisting to private app storage. Private keys never leave the device.
 * - Exports only the Base64-encoded X.509 Public Identity Key for Firestore `/user_profiles/{uid}`.
 * - Stores Keystore-wrapped per-message ephemeral keys locally so the sender can seamlessly
 *   decrypt their own sent messages while recipients derive the exact same key via ECDH.
 */
object E2eeKeyManager {

    private const val TAG = "E2eeKeyManager"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "univers_chat_e2ee_master_aes_v1"
    private const val PREFS_NAME = "univers_chat_e2ee_keystore_v1"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_LEN = 12
    private const val MAX_STORED_EPHEMERAL_KEYS = 400

    private val lock = Any()
    private val identityCache = HashMap<String, KeyPair>()

    fun getOrCreateIdentityKeyPair(context: Context, userId: String): KeyPair {
        synchronized(lock) {
            identityCache[userId]?.let { return it }

            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pubKeyB64 = prefs.getString(pubPrefKey(userId), null)
            val wrappedPrivB64 = prefs.getString(privPrefKey(userId), null)

            if (!pubKeyB64.isNullOrBlank() && !wrappedPrivB64.isNullOrBlank()) {
                val restored = runCatching {
                    val pubBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
                    val privBytes = unwrapWithMasterKey(Base64.decode(wrappedPrivB64, Base64.NO_WRAP))
                    val kf = KeyFactory.getInstance("EC")
                    val publicKey = kf.generatePublic(X509EncodedKeySpec(pubBytes))
                    val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                    KeyPair(publicKey, privateKey)
                }.onFailure { err ->
                    Log.w(TAG, "Failed to restore E2EE identity keypair; regenerating", err)
                }.getOrNull()

                if (restored != null) {
                    identityCache[userId] = restored
                    return restored
                }
            }

            val fresh = generateEcKeyPair()
            val encodedPub = Base64.encodeToString(fresh.public.encoded, Base64.NO_WRAP)
            runCatching {
                val wrappedPriv = Base64.encodeToString(
                    wrapWithMasterKey(fresh.private.encoded),
                    Base64.NO_WRAP
                )
                prefs.edit()
                    .putString(pubPrefKey(userId), encodedPub)
                    .putString(privPrefKey(userId), wrappedPriv)
                    .apply()
            }.onFailure { err ->
                Log.w(TAG, "Unable to persist wrapped E2EE keypair; keeping in-memory session key", err)
            }

            identityCache[userId] = fresh
            return fresh
        }
    }

    fun getPublicIdentityKeyBase64(context: Context, userId: String): String {
        return runCatching {
            val kp = getOrCreateIdentityKeyPair(context, userId)
            Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        }.onFailure { err ->
            Log.w(TAG, "Failed to derive public E2EE identity key", err)
        }.getOrDefault("")
    }

    fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun decodePublicKey(base64X509: String): PublicKey? {
        if (base64X509.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64X509.trim(), Base64.NO_WRAP)
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
        }.getOrNull()
    }

    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Persists a Keystore-wrapped ephemeral private key indexed by its public key hash so the
     * sender can always decrypt their own sent encrypted messages on this device.
     */
    fun storeSentEphemeralPrivateKey(
        context: Context,
        userId: String,
        ephemeralPublicKeyB64: String,
        ephemeralPrivateKey: PrivateKey
    ) {
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val keyName = ephemeralPrefKey(userId, ephemeralPublicKeyB64)
            val wrapped = Base64.encodeToString(
                wrapWithMasterKey(ephemeralPrivateKey.encoded),
                Base64.NO_WRAP
            )
            val editor = prefs.edit().putString(keyName, wrapped)

            // Keep bounded LRU index of stored ephemeral keys
            val indexKey = "eph_index_$userId"
            val existingList = prefs.getString(indexKey, "")
                .orEmpty()
                .split(',')
                .filter { it.isNotBlank() }
                .toMutableList()
            existingList.remove(keyName)
            existingList.add(keyName)
            while (existingList.size > MAX_STORED_EPHEMERAL_KEYS) {
                val evicted = existingList.removeAt(0)
                editor.remove(evicted)
            }
            editor.putString(indexKey, existingList.joinToString(","))
            editor.apply()
        }.onFailure { err ->
            Log.w(TAG, "Unable to store ephemeral key", err)
        }
    }

    fun loadSentEphemeralPrivateKey(
        context: Context,
        userId: String,
        ephemeralPublicKeyB64: String
    ): PrivateKey? {
        return runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val wrappedB64 = prefs.getString(ephemeralPrefKey(userId, ephemeralPublicKeyB64), null)
                ?: return null
            val rawPriv = unwrapWithMasterKey(Base64.decode(wrappedB64, Base64.NO_WRAP))
            KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(rawPriv))
        }.getOrNull()
    }

    /**
     * Computes a Signal-style 60-digit Safety Number fingerprint from two identity public keys.
     * Both participants independently compute the exact same 12x5-digit code to verify no MITM.
     */
    fun computeSafetyNumber(localPublicKeyB64: String, peerPublicKeyB64: String): String {
        val sorted = listOf(localPublicKeyB64.trim(), peerPublicKeyB64.trim()).sorted()
        val digest = MessageDigest.getInstance("SHA-256")
        var state = "UNIVERS_SIGNAL_SAFETY_V1|${sorted[0]}|${sorted[1]}".toByteArray(Charsets.UTF_8)
        repeat(64) {
            state = digest.digest(state)
        }
        // Expand 32-byte digest into 60 decimal digits (12 blocks of 5 digits)
        val digits = StringBuilder(72)
        var blockState = state
        for (block in 0 until 12) {
            if (block > 0 && block % 4 == 0) {
                blockState = digest.digest(blockState)
            }
            val offset = (block % 4) * 8
            var value = 0L
            for (i in 0 until 8) {
                value = (value shl 8) or (blockState[(offset + i) % blockState.size].toLong() and 0xFFL)
            }
            val fiveDigits = ((value and 0x7FFFFFFFFFFFFFFFL) % 100_000L).toString().padStart(5, '0')
            if (block > 0) digits.append(if (block % 4 == 0) "\n" else " ")
            digits.append(fiveDigits)
        }
        return digits.toString()
    }

    private fun getOrCreateMasterKey(forceRegenerate: Boolean = false): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (forceRegenerate) {
            runCatching { ks.deleteEntry(MASTER_KEY_ALIAS) }
        } else {
            runCatching { ks.getKey(MASTER_KEY_ALIAS, null) as? SecretKey }
                .getOrNull()
                ?.let { return it }
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    private fun wrapWithMasterKey(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        } catch (e: Exception) {
            Log.w(TAG, "Master key init failed; regenerating Keystore master key", e)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey(forceRegenerate = true))
        }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArray(iv.size + ciphertext.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(ciphertext, 0, this, iv.size, ciphertext.size)
        }
    }

    private fun unwrapWithMasterKey(blob: ByteArray): ByteArray {
        require(blob.size > GCM_IV_LEN) { "Corrupted wrapped key blob" }
        val iv = blob.copyOfRange(0, GCM_IV_LEN)
        val ciphertext = blob.copyOfRange(GCM_IV_LEN, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun pubPrefKey(userId: String) = "id_pub_$userId"
    private fun privPrefKey(userId: String) = "id_priv_wrapped_$userId"
    private fun ephemeralPrefKey(userId: String, ephemeralPubB64: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(ephemeralPubB64.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it) }
        return "eph_${userId}_$hash"
    }
}
