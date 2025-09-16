package net.opendasharchive.openarchive.services.storacha.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class DidManager(
    private val context: Context,
) {
    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "storacha_did_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // Legacy prefs for migration
    private val legacyPrefs = context.getSharedPreferences("storacha_prefs", Context.MODE_PRIVATE)
    private val key = "device_did"
    private val keyStorage = KeyStorage(context)

    fun getOrCreateDid(): String {
        // First check if we have a DID in secure storage
        val existingDid = keyStorage.getDid()
        if (existingDid != null) {
            return existingDid
        }
        
        // Check if we have one in old prefs (for migration)
        val legacyDid = legacyPrefs.getString(key, null)
        if (legacyDid != null) {
            // For legacy DIDs without stored keys, generate new ones
            // This will replace the old DID with a new one that has proper keys
            legacyPrefs.edit { remove(key) }
        }
        
        // Generate new DID with keys
        return generateNewDid()
    }

    private fun generateNewDid(): String {
        // Ensure BouncyCastle is registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        // Generate and store new key pair securely
        return keyStorage.generateAndStoreKeyPair()
    }
    
    fun regenerateDid(): String {
        keyStorage.clearKeys()
        legacyPrefs.edit { remove(key) }
        return generateNewDid()
    }
    
    fun hasDid(): Boolean {
        return keyStorage.hasKeys()
    }
}

