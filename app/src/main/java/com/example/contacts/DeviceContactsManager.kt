package com.example.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class DeviceContactEntry(
    val phoneHash: String,
    val displayName: String
)

/**
 * Privacy-preserving device contacts helper.
 *
 * Raw phone numbers are normalized to canonical digits and immediately hashed with SHA-256 on-device
 * before any comparison or upload to Firestore (`contactHashes`).
 */
object DeviceContactsManager {

    private const val TAG = "DeviceContactsManager"
    private const val PREFS_NAME = "univers_contacts_sync_prefs"
    private const val MAX_SYNCED_HASHES = 500

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSeenContactsPrompt(context: Context, userId: String): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("prompted_$userId", false)
    }

    fun markContactsPromptSeen(context: Context, userId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("prompted_$userId", true).apply()
    }

    fun getKnownJoinedContactIds(context: Context, userId: String): Set<String>? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "known_joined_$userId"
        if (!prefs.contains(key)) return null
        return prefs.getStringSet(key, emptySet())?.toSet().orEmpty()
    }

    fun saveKnownJoinedContactIds(context: Context, userId: String, userIds: Set<String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet("known_joined_$userId", userIds.toSet()).apply()
    }

    /**
     * Normalizes a raw phone number (E.164 or national format) into a canonical 64-character
     * lowercase SHA-256 hex hash so contacts match reliably regardless of country-code formatting.
     */
    fun normalizePhoneToHash(rawPhone: String?): String? {
        if (rawPhone.isNullOrBlank()) return null
        val digitsOnly = rawPhone.filter { it.isDigit() }
        if (digitsOnly.length < 7) return null
        val canonical = if (digitsOnly.length >= 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("univers_phone_v1:$canonical".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Reads phone contacts from the Android Contacts provider on an IO dispatcher and returns
     * a map of `phoneHash -> DeviceContactEntry` (capped at 500 entries).
     */
    suspend fun readHashedDeviceContacts(context: Context): Map<String, DeviceContactEntry> =
        withContext(Dispatchers.IO) {
            if (!hasContactsPermission(context)) return@withContext emptyMap()
            val result = LinkedHashMap<String, DeviceContactEntry>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext() && result.size < MAX_SYNCED_HASHES) {
                        val rawNumber = if (numIdx >= 0) cursor.getString(numIdx) else null
                        val hash = normalizePhoneToHash(rawNumber) ?: continue
                        if (!result.containsKey(hash)) {
                            val name = (if (nameIdx >= 0) cursor.getString(nameIdx) else null)
                                ?.trim()
                                ?.take(60)
                                .orEmpty()
                            result[hash] = DeviceContactEntry(
                                phoneHash = hash,
                                displayName = name
                            )
                        }
                    }
                }
            }.onFailure { err ->
                Log.w(TAG, "Failed to read device contacts", err)
            }
            result
        }
}
