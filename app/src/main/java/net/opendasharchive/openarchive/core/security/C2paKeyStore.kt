package net.opendasharchive.openarchive.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import net.opendasharchive.openarchive.services.storacha.util.SecureStorage

/**
 * Secure storage for C2PA signing keys using Android Keystore (AES-GCM).
 * Replaces plaintext storage in SharedPreferences.
 */
class C2paKeyStore(context: Context) {

    private val secureStorage = SecureStorage(context, alias = "C2paKeyStore")

    companion object {
        private const val KEY_SIGNING_KEY = "c2pa_signing_key"
        private const val KEY_PASSPHRASE = "c2pa_passphrase"

        // Legacy SharedPreferences keys used before this class was introduced
        private const val LEGACY_KEY_SIGNING_KEY = "c2pa_signing_key"
        private const val LEGACY_KEY_PASSPHRASE = "c2pa_encrypted_passphrase"
    }

    fun getSigningKey(): String? = secureStorage.getString(KEY_SIGNING_KEY)

    fun putSigningKey(value: String?) = secureStorage.putString(KEY_SIGNING_KEY, value)

    fun getPassphrase(): ByteArray? = secureStorage.getString(KEY_PASSPHRASE)
        ?.let { Base64.decode(it, Base64.DEFAULT) }

    fun putPassphrase(value: ByteArray?) {
        val encoded = value?.let { Base64.encodeToString(it, Base64.DEFAULT) }
        secureStorage.putString(KEY_PASSPHRASE, encoded)
    }

    fun clear() {
        secureStorage.remove(KEY_SIGNING_KEY)
        secureStorage.remove(KEY_PASSPHRASE)
    }

    /**
     * One-time migration: moves C2PA keys from plaintext SharedPreferences to SecureStorage,
     * then removes them from SharedPreferences.
     */
    fun migrateFromPrefsIfNeeded(prefs: SharedPreferences) {
        val legacyKey = prefs.getString(LEGACY_KEY_SIGNING_KEY, null)
        if (legacyKey != null && secureStorage.getString(KEY_SIGNING_KEY) == null) {
            secureStorage.putString(KEY_SIGNING_KEY, legacyKey)
        }
        if (legacyKey != null) {
            prefs.edit().remove(LEGACY_KEY_SIGNING_KEY).apply()
        }

        val legacyPassphrase = prefs.getString(LEGACY_KEY_PASSPHRASE, null)
        if (legacyPassphrase != null && secureStorage.getString(KEY_PASSPHRASE) == null) {
            secureStorage.putString(KEY_PASSPHRASE, legacyPassphrase)
        }
        if (legacyPassphrase != null) {
            prefs.edit().remove(LEGACY_KEY_PASSPHRASE).apply()
        }
    }
}
